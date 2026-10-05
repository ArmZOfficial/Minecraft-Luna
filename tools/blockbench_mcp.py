"""Connect to the user's local Blockbench MCP through a fresh HTTP session."""
import argparse
import base64
import json
from pathlib import Path
from urllib.request import Request, urlopen

ENDPOINT = "http://localhost:3000/bb-mcp"

class Client:
    def __init__(self):
        self.session = None
        self.request_id = 0
        self.server = self.send("initialize", {
            "protocolVersion": "2025-03-26", "capabilities": {},
            "clientInfo": {"name": "FantasyLobbyPlanner", "version": "1.0.0"}})
        self.send("notifications/initialized", None, notification=True)

    def send(self, method, params=None, notification=False):
        body = {"jsonrpc": "2.0", "method": method}
        if params is not None:
            body["params"] = params
        if not notification:
            self.request_id += 1
            body["id"] = self.request_id
        headers = {"Content-Type": "application/json",
                   "Accept": "application/json, text/event-stream",
                   "MCP-Protocol-Version": "2025-03-26"}
        if self.session:
            headers["Mcp-Session-Id"] = self.session
        with urlopen(Request(ENDPOINT, json.dumps(body).encode(), headers), timeout=90) as response:
            self.session = response.headers.get("Mcp-Session-Id", self.session)
            raw = response.read().decode()
        if not raw:
            return None
        if raw.startswith("event:") or raw.startswith("data:"):
            payloads = [json.loads(line[5:].strip()) for line in raw.splitlines() if line.startswith("data:")]
            result = next(p for p in payloads if p.get("id") == body.get("id"))
        else:
            result = json.loads(raw)
        if "error" in result:
            raise RuntimeError(json.dumps(result["error"], ensure_ascii=False))
        return result.get("result")

    def call(self, name, arguments):
        result = self.send("tools/call", {"name": name, "arguments": arguments})
        if result.get("isError"):
            raise RuntimeError(json.dumps(result, ensure_ascii=False))
        return result

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("tool", nargs="?", default="tools/list")
    parser.add_argument("--args", default="{}")
    parser.add_argument("--args-file")
    parser.add_argument("--out")
    parser.add_argument("--image-out")
    args = parser.parse_args()
    client = Client()
    arguments = json.loads(Path(args.args_file).read_text(encoding="utf-8-sig") if args.args_file else args.args)
    result = client.send("tools/list", {}) if args.tool == "tools/list" else client.call(args.tool, arguments)
    if args.image_out:
        block = next(c for c in result.get("content", []) if c.get("type") == "image")
        Path(args.image_out).write_bytes(base64.b64decode(block["data"]))
        print(json.dumps({"image": args.image_out}))
    elif args.out:
        Path(args.out).write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
        print(json.dumps({"saved": args.out, "tools": len(result.get("tools", []))}))
    else:
        print(json.dumps(result, ensure_ascii=False, indent=2))

if __name__ == "__main__":
    main()
