import pg from "pg";
export function createDatabase(connectionString = process.env.DATABASE_URL) {
  if (!connectionString) return null;
  const pool = new pg.Pool({
    connectionString,
    max: 5,
    connectionTimeoutMillis: 2500,
    idleTimeoutMillis: 30000,
  });
  pool.on("error", () =>
    console.error(
      "PostgreSQL idle connection lost. Credentials are not logged.",
    ),
  );
  return pool;
}
