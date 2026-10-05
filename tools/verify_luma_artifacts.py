"""Check deliverable links and the exported NPC rig/animation references."""
import json
import re
import sys
from pathlib import Path
from urllib.parse import unquote

sys.stdout.reconfigure(encoding='utf-8')

ROOT=Path(__file__).resolve().parents[1]
CONCEPT=ROOT/'output/lobby-concept'
errors=[]
link_count=0
for file in [ROOT/'README.md',ROOT/'website/README-th.md',ROOT/'fantasycore/README-th.md',ROOT/'fantasycore/EXCHANGE-th.md',ROOT/'server/README-th.md',*CONCEPT.rglob('*.md')]:
    text=file.read_text(encoding='utf-8-sig')
    if text.count('```')%2: errors.append(f'Unbalanced code fence: {file.relative_to(ROOT)}')
    for match in re.finditer(r'!?\[[^\]\n]*\]\(([^)\n]+)\)',text):
        target=match.group(1).strip()
        if re.match(r'^(?:[a-z][a-z0-9+.-]*:|#)',target,re.I): continue
        target=target.split('#',1)[0].strip('<>')
        if not target: continue
        link_count+=1
        if not (file.parent/unquote(target)).exists(): errors.append(f'{file.relative_to(ROOT)} → {target}')

manifest=json.loads((CONCEPT/'assets/models/manifest.json').read_text(encoding='utf-8'))
def group_ids(nodes):
    found=set()
    for node in nodes:
        if isinstance(node,dict):
            found.add(node['uuid'])
            found.update(group_ids(node.get('children',[])))
    return found

model_report=[]
for entry in manifest:
    model=json.loads((CONCEPT/f"assets/models/{entry['id']}.bbmodel").read_text(encoding='utf-8'))
    assert len(model['elements'])==entry['cubes'],entry['id']
    assert all(tex.get('source','').startswith('data:image/png;base64,') for tex in model['textures']),entry['id']
    animations=model.get('animations',[])
    assert sorted(a['name'] for a in animations)==sorted(entry['animations']),entry['id']
    bones=group_ids(model.get('outliner',[]))
    keys=0
    for animation in animations:
        assert animation['length']>0,(entry['id'],animation['name'])
        for bone,animator in animation['animators'].items():
            assert bone in bones,(entry['id'],bone)
            for key in animator.get('keyframes',[]):
                assert 0<=key['time']<=animation['length']+1e-6,(entry['id'],key['time'])
                keys+=1
    model_report.append({'id':entry['id'],'animations':len(animations),'keyframes':keys})

for page in ['index.html','app.js','portal.js']:
    source=(ROOT/'website/dist'/page).read_text(encoding='utf-8')
    for asset in re.findall(r'(?:src=\"|image:\x27)(assets/[^\"\x27]+)',source):
        if '${' not in asset and not (ROOT/'website/dist'/asset).exists(): errors.append(f'{page} → {asset}')
report={'markdown_links_checked':link_count,'models':model_report,'errors':errors}
print(json.dumps(report,ensure_ascii=False,indent=2))
if errors: raise SystemExit(1)
