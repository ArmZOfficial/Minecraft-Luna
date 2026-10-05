import test from "node:test";
import assert from "node:assert/strict";
import { createHash, randomUUID } from "node:crypto";
import { once } from "node:events";
import { request as httpRequest } from "node:http";
import { createDatabase } from "./database.mjs";
import { createPortalServer } from "./api.mjs";
import { libraryProducts } from "./dev-payments.mjs";

test(
  "DEV PromptPay uses PostgreSQL, freezes prices and grants one simulated receipt",
  { skip: !process.env.DATABASE_URL },
  async (t) => {
    const pool = createDatabase(),
      sessions = [];
    let server, base;
    async function boot() {
      server = createPortalServer({ pool, paymentMode: "mock" });
      server.listen(0, "127.0.0.1");
      await once(server, "listening");
      base = `http://127.0.0.1:${server.address().port}`;
    }
    async function call(route, body, cookie, overrides = {}) {
      const res = await fetch(base + "/api/dev/" + route, {
        method: body ? "POST" : "GET",
        headers: {
          Origin: base,
          "Content-Type": "application/json",
          "X-Luma-Dev": "1",
          ...(cookie ? { Cookie: cookie } : {}),
          ...overrides,
        },
        body: body ? JSON.stringify(body) : undefined,
      });
      return {
        status: res.status,
        data: await res.json(),
        cookie: res.headers.get("set-cookie")?.split(";")[0],
      };
    }
    async function session() {
      const r = await call("session", {});
      assert.equal(r.status, 201);
      sessions.push(
        createHash("sha256").update(r.cookie.slice(9)).digest("hex"),
      );
      return r.cookie;
    }
    const product = libraryProducts[0];
    const create = (cookie, key = randomUUID()) =>
      call(
        "checkout",
        {
          productId: product.id,
          idempotencyKey: key,
          amountMinor: 1,
          quantity: 99,
        },
        cookie,
      );
    try {
      await boot();
      const liveBefore = (
        await pool.query(
          "SELECT (SELECT count(*) FROM orders)::int AS orders,(SELECT count(*) FROM delivery_outbox)::int AS deliveries",
        )
      ).rows[0];
      await t.test("Origin, host, and session boundaries", async () => {
        assert.equal(
          (
            await call("session", {}, null, {
              Origin: "https://attacker.invalid",
            })
          ).status,
          403,
        );
        const foreignHost = await new Promise((resolve, reject) => {
          const req = httpRequest(
            base + "/api/dev/status",
            { headers: { Host: "attacker.invalid" } },
            (res) => {
              let data = "";
              res.on("data", (c) => (data += c));
              res.on("end", () => resolve(JSON.parse(data)));
            },
          );
          req.on("error", reject);
          req.end();
        });
        assert.equal(foreignHost.enabled, false);
        assert.equal((await create()).status, 401);
      });
      const cookie = await session(),
        cookie2 = await session();
      await t.test(
        "opening another product keeps the existing DEV identity",
        async () => {
          const reused = await call("session", {}, cookie);
          assert.equal(reused.status, 200);
          assert.equal(reused.cookie, undefined);
        },
      );
      const key = randomUUID();
      let order;
      await t.test(
        "concurrent checkout uses frozen server prices and one idempotency key",
        async () => {
          const list = await Promise.all(
            Array.from({ length: 8 }, () => create(cookie, key)),
          );
          assert.ok(list.every((r) => r.status === 200));
          assert.equal(new Set(list.map((r) => r.data.id)).size, 1);
          order = list[0].data;
          assert.equal(order.amountMinor, product.price * 100);
          assert.equal(order.realMoney, false);
          assert.equal(order.qr, null);
          assert.equal(
            (
              await call(
                "checkout",
                { productId: libraryProducts[1].id, idempotencyKey: key },
                cookie,
              )
            ).status,
            409,
          );
        },
      );
      await t.test(
        "another local session cannot read or resolve an order",
        async () => {
          assert.equal(
            (await call(`orders/${order.id}`, null, cookie2)).status,
            404,
          );
          assert.equal(
            (
              await call(
                `orders/${order.id}/outcome`,
                { outcome: "paid" },
                cookie2,
              )
            ).status,
            404,
          );
        },
      );
      await t.test(
        "duplicate success is atomic and never touches live delivery",
        async () => {
          const results = await Promise.all(
            Array.from({ length: 8 }, () =>
              call(`orders/${order.id}/outcome`, { outcome: "paid" }, cookie),
            ),
          );
          assert.ok(
            results.every(
              (r) => r.status === 200 && r.data.receipt.gameDelivered === false,
            ),
          );
          assert.equal(
            (
              await pool.query(
                "SELECT count(*)::int AS n FROM dev_payment_receipts WHERE order_id=$1",
                [order.id],
              )
            ).rows[0].n,
            1,
          );
          assert.deepEqual(
            (
              await pool.query(
                "SELECT (SELECT count(*) FROM orders)::int AS orders,(SELECT count(*) FROM delivery_outbox)::int AS deliveries",
              )
            ).rows[0],
            liveBefore,
          );
          assert.equal(
            (
              await call(
                `orders/${order.id}/outcome`,
                { outcome: "failed" },
                cookie,
              )
            ).status,
            409,
          );
        },
      );
      await t.test(
        "failed, cancelled and expired orders cannot be paid later",
        async () => {
          for (const outcome of ["failed", "cancelled", "expired"]) {
            const other = (await create(cookie)).data;
            if (outcome === "expired")
              await pool.query(
                "UPDATE dev_payment_orders SET expires_at=now()-interval '1 second' WHERE id=$1",
                [other.id],
              );
            else
              assert.equal(
                (await call(`orders/${other.id}/outcome`, { outcome }, cookie))
                  .status,
                200,
              );
            assert.equal(
              (
                await call(
                  `orders/${other.id}/outcome`,
                  { outcome: "paid" },
                  cookie,
                )
              ).status,
              409,
            );
            const state = (await call(`orders/${other.id}`, null, cookie)).data;
            assert.equal(state.status, outcome);
            assert.equal(state.receipt, null);
          }
        },
      );
      await t.test("status and receipt survive a Node restart", async () => {
        await new Promise((r) => server.close(r));
        await boot();
        const found = (await call(`orders/${order.id}`, null, cookie)).data;
        assert.equal(found.status, "paid");
        assert.equal(found.receipt.product.id, product.id);
      });
      await t.test("production mode disables the simulator", async () => {
        const old = process.env.NODE_ENV;
        try {
          process.env.NODE_ENV = "production";
          assert.equal((await call("status")).data.enabled, false);
          assert.equal((await create(cookie)).status, 404);
        } finally {
          if (old === undefined) delete process.env.NODE_ENV;
          else process.env.NODE_ENV = old;
        }
      });
    } finally {
      if (server) await new Promise((r) => server.close(r));
      await pool.query(
        "DELETE FROM dev_payment_receipts WHERE order_id IN (SELECT id FROM dev_payment_orders WHERE session_hash=ANY($1::text[]))",
        [sessions],
      );
      await pool.query(
        "DELETE FROM dev_payment_orders WHERE session_hash=ANY($1::text[])",
        [sessions],
      );
      await pool.query(
        "DELETE FROM dev_payment_sessions WHERE token_hash=ANY($1::text[])",
        [sessions],
      );
      await pool.end();
    }
  },
);
