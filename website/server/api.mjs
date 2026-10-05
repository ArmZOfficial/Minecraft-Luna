import { createServer } from "node:http";
import { readFile, stat } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { createDatabase } from "./database.mjs";
import { devPaymentRequest, libraryProducts } from "./dev-payments.mjs";

const output = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  "../out",
);
const types = {
  ".html": "text/html; charset=utf-8",
  ".js": "text/javascript; charset=utf-8",
  ".css": "text/css; charset=utf-8",
  ".json": "application/json; charset=utf-8",
  ".webp": "image/webp",
  ".svg": "image/svg+xml",
  ".txt": "text/plain; charset=utf-8",
  ".woff2": "font/woff2",
  ".ico": "image/x-icon",
  ".png": "image/png",
};
function json(res, status, body) {
  res.writeHead(status, {
    "Content-Type": "application/json; charset=utf-8",
    "Cache-Control": "no-store",
    "X-Content-Type-Options": "nosniff",
  });
  res.end(JSON.stringify(body));
}
function unavailable(res, code = "DATABASE_UNCONFIGURED") {
  json(res, 503, { error: code, message: "บริการนี้ยังไม่พร้อมใช้งาน" });
}
export function createPortalServer({
  pool = createDatabase(),
  staticRoot = output,
  paymentMode = process.env.LUMA_PAYMENT_MODE || "disabled",
} = {}) {
  const root = path.resolve(staticRoot);
  return createServer(async (req, res) => {
    try {
      const url = new URL(req.url, "http://localhost");
      const route = url.pathname;
      if (route.startsWith("/api/")) {
        if (
          await devPaymentRequest(req, res, route, {
            pool,
            mode: paymentMode,
            json,
          })
        )
          return;
        if (route === "/api/library/catalog" && req.method === "GET")
          return json(res, 200, {
            items: libraryProducts,
            mode: "draft",
            livePayment: false,
          });
        if (route === "/api/health" && req.method === "GET") {
          let database = pool ? "unavailable" : "unconfigured";
          if (pool)
            try {
              await pool.query("SELECT 1");
              database = "ready";
            } catch {}
          return json(res, 200, {
            database,
            game: "unconfigured",
            payment: "unconfigured",
            delivery: "unconfigured",
            updatedAt: new Date().toISOString(),
          });
        }
        if (
          [
            "/api/auth/link",
            "/api/payments/checkout",
            "/api/payments/webhook",
            "/api/bridge/deliveries",
          ].includes(route)
        )
          return unavailable(res, "INTEGRATION_UNCONFIGURED");
        if (
          ["/api/account", "/api/orders", "/api/mailbox", "/api/admin"].some(
            (p) => route === p || route.startsWith(p + "/"),
          )
        )
          return json(res, 401, { error: "AUTH_REQUIRED" });
        if (req.method !== "GET")
          return json(res, 405, { error: "METHOD_NOT_ALLOWED" });
        if (!["/api/news", "/api/catalog"].includes(route))
          return json(res, 404, { error: "NOT_FOUND" });
        if (!pool) return unavailable(res);
        try {
          const result =
            route === "/api/news"
              ? await pool.query(
                  "SELECT id,title,intro,body,category,image,to_char(published_at AT TIME ZONE 'Asia/Bangkok','DD/MM/YYYY') AS date FROM news_posts WHERE published = $1 ORDER BY published_at DESC LIMIT 50",
                  [true],
                )
              : await pool.query(
                  "SELECT id,name,description,type,(price_minor::numeric/100) AS price,image FROM products WHERE active = $1 AND cosmetic_only = $2 ORDER BY id LIMIT 50",
                  [true, true],
                );
          return json(res, 200, { items: result.rows });
        } catch {
          return unavailable(res, "DATABASE_UNAVAILABLE");
        }
      }
      if (req.method !== "GET" && req.method !== "HEAD")
        return json(res, 405, { error: "METHOD_NOT_ALLOWED" });
      const decoded = decodeURIComponent(route);
      if (decoded.includes("\0") || decoded.includes("\\"))
        return json(res, 400, { error: "INVALID_PATH" });
      let filename = path.resolve(root, "." + decoded);
      if (filename !== root && !filename.startsWith(root + path.sep))
        return json(res, 403, { error: "FORBIDDEN" });
      let code = 200;
      try {
        if ((await stat(filename)).isDirectory())
          filename = path.join(filename, "index.html");
        await stat(filename);
      } catch {
        filename = path.join(root, "404.html");
        code = 404;
      }
      const body = await readFile(filename);
      const ext = path.extname(filename);
      res.writeHead(code, {
        "Content-Type": types[ext] || "application/octet-stream",
        "Cache-Control": route.startsWith("/_next/static/")
          ? "public,max-age=31536000,immutable"
          : "no-cache",
        "X-Content-Type-Options": "nosniff",
        "Referrer-Policy": "strict-origin-when-cross-origin",
      });
      res.end(req.method === "HEAD" ? undefined : body);
    } catch {
      if (!res.headersSent) json(res, 500, { error: "SERVER_UNAVAILABLE" });
      else res.end();
    }
  });
}
