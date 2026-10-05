import { readFile, readdir } from "node:fs/promises";
import { createDatabase } from "./database.mjs";
const pool = createDatabase();
if (!pool) {
  console.error("Set DATABASE_URL in .env.local before running migrations.");
  process.exit(1);
}
try {
  const directory = new URL("../database/", import.meta.url);
  for (const file of (await readdir(directory))
    .filter((f) => /^\d+_.*\.sql$/.test(f))
    .sort()) {
    await pool.query(await readFile(new URL(file, directory), "utf8"));
    console.log(`PostgreSQL migration ${file} completed.`);
  }
} catch {
  console.error(
    "Migration failed. Check the PostgreSQL connection and database permissions.",
  );
  process.exitCode = 1;
} finally {
  await pool.end();
}
