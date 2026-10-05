import { readFile, readdir } from "node:fs/promises";
import path from "node:path";
const exported = path.resolve("out");
let password;
try {
  const local = await readFile(".env.local", "utf8");
  const connection = local
    .split(/\r?\n/)
    .find((l) => l.startsWith("DATABASE_URL="));
  if (connection) password = new URL(connection.slice(13)).password;
} catch {}
let files = 0;
const leaks = [];
async function walk(dir) {
  for (const entry of await readdir(dir, { withFileTypes: true })) {
    const file = path.join(dir, entry.name);
    if (entry.isDirectory()) await walk(file);
    else {
      files++;
      if (password && (await readFile(file)).includes(Buffer.from(password)))
        leaks.push(path.relative(exported, file));
    }
  }
}
await walk(exported);
console.log(
  JSON.stringify({ exportFiles: files, credentialLeaks: leaks }, null, 2),
);
if (leaks.length) process.exitCode = 1;
