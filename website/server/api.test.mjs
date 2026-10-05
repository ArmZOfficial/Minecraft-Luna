import test from "node:test";
import assert from "node:assert/strict";
import { mkdtemp, writeFile, rm } from "node:fs/promises";
import { tmpdir } from "node:os";
import path from "node:path";
import { once } from "node:events";
import { createPortalServer } from "./api.mjs";

test("API gates integrations and serves only exported public files", async (t) => {
  const dir = await mkdtemp(path.join(tmpdir(), "luma-api-"));
  await writeFile(path.join(dir, "index.html"), "<h1>Luma</h1>");
  await writeFile(path.join(dir, "404.html"), "<h1>Not found</h1>");
  const server = createPortalServer({ pool: null, staticRoot: dir });
  server.listen(0, "127.0.0.1");
  await once(server, "listening");
  const base = `http://127.0.0.1:${server.address().port}`;
  try {
    await t.test(
      "health distinguishes unconfigured database from zero data",
      async () => {
        const res = await fetch(base + "/api/health");
        assert.equal(res.status, 200);
        assert.equal((await res.json()).database, "unconfigured");
      },
    );
    await t.test(
      "catalog does not return fake purchasable inventory",
      async () => {
        const res = await fetch(base + "/api/catalog");
        assert.equal(res.status, 503);
        assert.equal((await res.json()).error, "DATABASE_UNCONFIGURED");
      },
    );
    await t.test("payments and account routes remain gated", async () => {
      assert.equal(
        (await fetch(base + "/api/payments/checkout", { method: "POST" }))
          .status,
        503,
      );
      assert.equal((await fetch(base + "/api/orders")).status, 401);
    });
    await t.test(
      "public root works and private paths cannot escape export root",
      async () => {
        assert.equal((await fetch(base)).status, 200);
        assert.equal((await fetch(base + "/%2e%2e%2f.env.local")).status, 403);
        assert.equal(
          (await fetch(base + "/database/001_initial.sql")).status,
          404,
        );
      },
    );
  } finally {
    await new Promise((resolve) => server.close(resolve));
    await rm(dir, { recursive: true, force: true });
  }
});
