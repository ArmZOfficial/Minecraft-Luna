import { createHash, randomBytes } from "node:crypto";
import { readFile } from "node:fs/promises";

export const libraryProducts = JSON.parse(
  await readFile(
    new URL("../lib/library-products.json", import.meta.url),
    "utf8",
  ),
);
const uuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
const hash = (s) => createHash("sha256").update(s).digest("hex");
function fail(status, code) {
  throw Object.assign(new Error(code), { status, code });
}

function local(req) {
  const address = req.socket.remoteAddress;
  if (!["127.0.0.1", "::1", "::ffff:127.0.0.1"].includes(address)) return false;
  try {
    const host = new URL(`http://${req.headers.host}`).hostname;
    return ["127.0.0.1", "localhost", "[::1]"].includes(host);
  } catch {
    return false;
  }
}
async function body(req) {
  if (!req.headers["content-type"]?.startsWith("application/json"))
    fail(415, "JSON_REQUIRED");
  if (req.headers["x-luma-dev"] !== "1") fail(403, "DEV_HEADER_REQUIRED");
  // An exact Origin check also rejects cross-port local sites and DNS rebinding.
  if (req.headers.origin !== `http://${req.headers.host}`)
    fail(403, "ORIGIN_REJECTED");
  let size = 0,
    chunks = [];
  for await (const c of req) {
    size += c.length;
    if (size > 4096) fail(413, "BODY_TOO_LARGE");
    chunks.push(c);
  }
  try {
    const value = JSON.parse(Buffer.concat(chunks).toString("utf8"));
    if (!value || Array.isArray(value) || typeof value !== "object")
      fail(400, "INVALID_JSON");
    return value;
  } catch (e) {
    if (e.status) throw e;
    fail(400, "INVALID_JSON");
  }
}
function sessionHash(req) {
  const token = req.headers.cookie
    ?.split(";")
    .map((s) => s.trim())
    .find((s) => s.startsWith("luma_dev="))
    ?.slice(9);
  if (!token || !/^[a-f0-9]{64}$/.test(token))
    fail(401, "DEV_SESSION_REQUIRED");
  return hash(token);
}
function present(row, receipt) {
  return {
    id: row.id,
    mode: "mock",
    status: row.status,
    amountMinor: row.amount_minor,
    currency: "THB",
    product: row.snapshot,
    expiresAt: row.expires_at,
    createdAt: row.created_at,
    receipt: receipt
      ? {
          state: "simulated-mailbox",
          product: receipt.snapshot,
          gameDelivered: false,
        }
      : null,
    qr: null,
    realMoney: false,
    gameDelivered: false,
  };
}

/** Local-only simulator: never calls a gateway, live order table, or game bridge. */
export async function devPaymentRequest(req, res, route, { pool, mode, json }) {
  if (!route.startsWith("/api/dev/")) return false;
  const enabled =
    mode === "mock" && process.env.NODE_ENV !== "production" && local(req);
  if (route === "/api/dev/status" && req.method === "GET") {
    json(res, 200, {
      enabled,
      mode: enabled ? "mock" : "disabled",
      realMoney: false,
      gameDelivered: false,
    });
    return true;
  }
  if (!enabled) {
    json(res, 404, { error: "DEV_DISABLED" });
    return true;
  }
  if (!pool) {
    json(res, 503, { error: "DATABASE_UNCONFIGURED" });
    return true;
  }
  let client;
  try {
    const input = req.method === "POST" ? await body(req) : null;
    if (route === "/api/dev/session" && req.method === "POST") {
      let existing;
      try {
        existing = sessionHash(req);
      } catch (e) {
        if (e.status !== 401) throw e;
      }
      if (
        existing &&
        (
          await pool.query(
            "SELECT token_hash FROM dev_payment_sessions WHERE token_hash=$1 AND expires_at>now()",
            [existing],
          )
        ).rowCount
      ) {
        json(res, 200, { mode: "mock", realMoney: false });
        return true;
      }
      const token = randomBytes(32).toString("hex");
      await pool.query(
        "INSERT INTO dev_payment_sessions(token_hash) VALUES ($1)",
        [hash(token)],
      );
      res.setHeader(
        "Set-Cookie",
        `luma_dev=${token}; HttpOnly; SameSite=Strict; Path=/api/dev/; Max-Age=86400`,
      );
      json(res, 201, { mode: "mock", realMoney: false });
      return true;
    }
    const identity = sessionHash(req);
    client = await pool.connect();
    await client.query("BEGIN");
    const session = await client.query(
      "SELECT token_hash FROM dev_payment_sessions WHERE token_hash=$1 AND expires_at>now() FOR UPDATE",
      [identity],
    );
    if (!session.rowCount) fail(401, "DEV_SESSION_EXPIRED");
    let order;
    if (route === "/api/dev/checkout" && req.method === "POST") {
      if (!uuid.test(input.idempotencyKey || ""))
        fail(400, "INVALID_IDEMPOTENCY_KEY");
      const product = libraryProducts.find((p) => p.id === input.productId);
      if (!product) fail(404, "PRODUCT_NOT_FOUND");
      const existing = await client.query(
        "SELECT * FROM dev_payment_orders WHERE session_hash=$1 AND idempotency_key=$2 FOR UPDATE",
        [identity, input.idempotencyKey],
      );
      if (existing.rowCount) {
        order = existing.rows[0];
        if (order.product_id !== product.id) fail(409, "IDEMPOTENCY_CONFLICT");
      } else {
        const pending = await client.query(
          "SELECT count(*)::int AS n FROM dev_payment_orders WHERE session_hash=$1 AND status='pending' AND expires_at>now()",
          [identity],
        );
        if (pending.rows[0].n >= 20) fail(429, "TOO_MANY_PENDING_ORDERS");
        order = (
          await client.query(
            "INSERT INTO dev_payment_orders(session_hash,idempotency_key,product_id,amount_minor,snapshot) VALUES ($1,$2,$3,$4,$5) RETURNING *",
            [
              identity,
              input.idempotencyKey,
              product.id,
              product.price * 100,
              JSON.stringify(product),
            ],
          )
        ).rows[0];
      }
    } else {
      const match = /^\/api\/dev\/orders\/([^/]+)(\/outcome)?$/.exec(route);
      if (!match || !uuid.test(match[1])) fail(404, "NOT_FOUND");
      if (
        (match[2] && req.method !== "POST") ||
        (!match[2] && req.method !== "GET")
      )
        fail(405, "METHOD_NOT_ALLOWED");
      const found = await client.query(
        "SELECT * FROM dev_payment_orders WHERE id=$1 AND session_hash=$2 FOR UPDATE",
        [match[1], identity],
      );
      if (!found.rowCount) fail(404, "ORDER_NOT_FOUND");
      order = found.rows[0];
      if (match[2] && !["paid", "failed", "cancelled"].includes(input.outcome))
        fail(400, "INVALID_OUTCOME");
      // Use PostgreSQL time for expiry, including after restart or a delayed callback.
      order = (
        await client.query(
          "UPDATE dev_payment_orders SET status=CASE WHEN status='pending' AND expires_at<=now() THEN 'expired' ELSE status END WHERE id=$1 RETURNING *",
          [order.id],
        )
      ).rows[0];
      if (match[2]) {
        if (order.status !== "pending" && order.status !== input.outcome)
          fail(409, "ORDER_TERMINAL");
        if (order.status === "pending") {
          order = (
            await client.query(
              "UPDATE dev_payment_orders SET status=$2,paid_at=CASE WHEN $2='paid' THEN now() ELSE null END WHERE id=$1 RETURNING *",
              [order.id, input.outcome],
            )
          ).rows[0];
        }
        if (order.status === "paid")
          await client.query(
            "INSERT INTO dev_payment_receipts(order_id,snapshot) VALUES ($1,$2) ON CONFLICT DO NOTHING",
            [order.id, JSON.stringify(order.snapshot)],
          );
      }
    }
    order = (
      await client.query(
        "UPDATE dev_payment_orders SET status=CASE WHEN status='pending' AND expires_at<=now() THEN 'expired' ELSE status END WHERE id=$1 RETURNING *",
        [order.id],
      )
    ).rows[0];
    const receipt = (
      await client.query(
        "SELECT snapshot FROM dev_payment_receipts WHERE order_id=$1",
        [order.id],
      )
    ).rows[0];
    await client.query("COMMIT");
    json(res, 200, present(order, receipt));
  } catch (e) {
    if (client) await client.query("ROLLBACK").catch(() => {});
    json(res, e.status || 503, {
      error: e.code && e.status ? e.code : "DEV_DATABASE_UNAVAILABLE",
    });
  } finally {
    client?.release();
  }
  return true;
}
