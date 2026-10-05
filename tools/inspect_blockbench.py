import json
from pathlib import Path

data = json.loads(Path('output/lobby-concept/blockbench-capabilities.json').read_text(encoding='utf-8'))
for block in data.get('content', []):
    if block.get('type') == 'text':
        try:
            payload = json.loads(block['text'])
            print(json.dumps({k:v for k,v in payload.items() if k != 'tools'}, ensure_ascii=False, indent=2))
        except ValueError:
            print(block['text'][:4500])
