import { createPortalServer } from "./api.mjs";
import { createDatabase } from "./database.mjs";
const pool = createDatabase();
const server = createPortalServer({ pool });
const port = Number(process.env.PORT || 4178),
  host = process.env.HOST || "127.0.0.1";
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
