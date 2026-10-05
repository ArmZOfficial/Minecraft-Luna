import { randomBytes } from "node:crypto";
import { writeFile } from "node:fs/promises";
const password = randomBytes(24).toString("hex");
try {
  await writeFile(
    ".env.local",
    `DATABASE_URL=postgresql://luma:${password}@127.0.0.1:5432/luma\nPOSTGRES_PASSWORD=${password}\nHOST=127.0.0.1\nPORT=4178\nNEXT_PUBLIC_API_BASE=\n`,
    { flag: "wx", mode: 0o600 },
  );
  console.log(
    "Created ignored .env.local with a random local database password. Credentials are not printed.",
  );
} catch (error) {
  if (error.code === "EEXIST") console.log("Existing .env.local preserved.");
  else throw error;
}
