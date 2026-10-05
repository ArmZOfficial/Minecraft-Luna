import { createPortalServer } from "./api.mjs";
import { createDatabase } from "./database.mjs";
const pool = createDatabase();
const server = createPortalServer({ pool });
const port = Number(process.env.PORT || 4178),
  host = process.env.HOST || "127.0.0.1";
if (
  process.env.LUMA_PAYMENT_MODE === "mock" &&
  (process.env.NODE_ENV === "production" ||
    !["127.0.0.1", "localhost", "::1"].includes(host))
)
  throw new Error("Mock payments require a local development server.");
server.listen(port, host, () =>
  console.log(`Luma React portal and Node API: http://${host}:${port}`),
);
function stop() {
  server.close(async () => {
    if (pool) await pool.end();
    process.exit(0);
  });
}
process.on("SIGINT", stop);
process.on("SIGTERM", stop);
