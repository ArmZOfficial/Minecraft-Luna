import test from "node:test";
import assert from "node:assert/strict";
import { randomUUID } from "node:crypto";
import { once } from "node:events";
import { createDatabase } from "./database.mjs";
import { createPortalServer } from "./api.mjs";

test(
  "PostgreSQL enforces visibility and duplicate payment protection",
  { skip: !process.env.DATABASE_URL },
  async () => {
    const pool = createDatabase();
    const client = await pool.connect();
    const prefix = `qa-${randomUUID()}`;
    let server;
    try {
      await client.query("BEGIN");
      await client.query(
        "INSERT INTO news_posts(id,title,intro,body,category,image,published,published_at) VALUES ($1,'draft','draft','[]','ระบบ','test.webp',false,null),($2,'published','published','[]','ระบบ','test.webp',true,now())",
        [prefix + "-draft", prefix + "-public"],
      );
      server = createPortalServer({ pool: client });
      server.listen(0, "127.0.0.1");
      await once(server, "listening");
      const result = await fetch(
        `http://127.0.0.1:${server.address().port}/api/news`,
      );
      const body = await result.json();
      assert.equal(
        body.items.some((i) => i.id === prefix + "-draft"),
        false,
      );
      assert.equal(
        body.items.some((i) => i.id === prefix + "-public"),
        true,
      );
      await client.query(
        "INSERT INTO payment_events(provider,provider_event_id,payload_digest) VALUES ('qa',$1,'digest')",
        [prefix],
      );
      await client.query("SAVEPOINT duplicate_test");
      await assert.rejects(
        client.query(
          "INSERT INTO payment_events(provider,provider_event_id,payload_digest) VALUES ('qa',$1,'digest')",
          [prefix],
        ),
        (e) => e.code === "23505",
      );
      await client.query("ROLLBACK TO SAVEPOINT duplicate_test");
    } finally {
      if (server) await new Promise((resolve) => server.close(resolve));
      await client.query("ROLLBACK");
      client.release();
      await pool.end();
    }
  },
);
