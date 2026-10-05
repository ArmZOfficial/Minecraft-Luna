import { readFile } from "node:fs/promises";
import { createDatabase } from "./database.mjs";
const pool = createDatabase();
if (!pool) {
  console.error("Set DATABASE_URL in .env.local before running migrations.");
  process.exit(1);
}
try {
  const sql = await readFile(
    new URL("../database/001_initial.sql", import.meta.url),
    "utf8",
  );
  await pool.query(sql);
  console.log(
    "PostgreSQL migration 001_initial completed. No players, paid orders, or sample balances were created.",
  );
} catch {
  console.error(
    "Migration failed. Check the PostgreSQL connection and database permissions.",
  );
  process.exitCode = 1;
} finally {
  await pool.end();
}
