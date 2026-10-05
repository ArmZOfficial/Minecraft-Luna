"""Inventory local packs, repair known duplicate keys in copies, produce a Luma catalogue.

No supplied executable is run. Originals and paid asset files stay out of Git.
Run from the repository root with Python 3.11+, Java and the existing Gradle cache.
"""
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import re
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'All for module'
WORK = ROOT / '.cache/library'
DEST = ROOT / 'server/content/library'
WEB = ROOT / 'website/public/assets/library'

# Prices are cosmetic proposals in THB, never payment authorization.
SETS = [
 ('azureset.zip','azure','ธาราฟ้า','เริ่มต้น',149,'gem','bank'),
 ('beatsset.zip','beats','จังหวะดารา','กลาง',149,'bard','tavern'),
 ('Beserkset.zip','berserker','พยัคฆ์คลั่ง','กลาง',179,'berserker','forge'),
 ('Demon_commander pack.zip','commander','จอมทัพเงารัตติกาล','ปลาย',199,'demon','guild'),
 ('Demonic_Killeffect_legacy.zip','killfx','ประกายอสูร','ตกแต่ง',79,'spell','arena'),
 ('draculaset.zip','dracula','ขุนนางจันทราเลือด','ปลาย',199,'vampire','guild'),
 ('fiendskullset.zip','fiendskull','กระโหลกผนึกวิญญาณ','กลาง',179,'necromancer','quest'),
 ('Ifritset.zip','ifrit','อิฟริทเพลิงอรุณ','ปลาย',199,'elemental','forge'),
 ('infernoset.zip','inferno','อัคคีล้างเงา','ปลาย',199,'volcano','forge'),
 ('littledragonset.zip','little-dragon','ผู้พิทักษ์มังกรน้อย','เริ่มต้น',149,'dragon','pets'),
 ('lunardragon_set.zip','lunar-dragon','มังกรจันทรา','ปลาย',219,'celestial','guild'),
 ('Madhatterset.zip','madhatter','นักมายาหมวกพิศวง','กลาง',179,'mage','market'),
 ('Medieval-Kitchen-Furniture_Pack.zip','kitchen','ครัวรูนแสนอุ่น','เฟอร์นิเจอร์',99,'chef','tavern'),
 ('Medieval-Tavern-Furniture_Pack.zip','tavern','โรงเตี๊ยมแสงจันทร์','เฟอร์นิเจอร์',129,'tavern','tavern'),
 ('MystiCrates _ keys animated V.2.0.0.zip','keys','กุญแจห้าดารา','เควส',0,'treasure_cave','quest'),
 ('Nogs Plushies [Dinosaurs].zip','dinosaurs','เพื่อนจิ๋วยุคดึกดำบรรพ์','ตุ๊กตา',79,'pet','pets'),
 ('Nogs Plushies [Superheroes].zip','heroes','ผู้พิทักษ์ตัวจิ๋ว','ตุ๊กตา',79,'hero','pets'),
 ('nora-limited_set.zip','nora','นภาผู้เดินทาง','กลาง',179,'explorer','market'),
 ('OnePunchMan_set.zip','onepunch','หมัดดาวตก','กลาง',179,'monk','arena'),
 ('radiance_collection.zip','radiance','รุ่งอรุณศักดิ์สิทธิ์','ปลาย',219,'holy','guild'),
 ('sanpatric_set.zip','clover','โคลเวอร์โชคสี่แฉก','เริ่มต้น',149,'blessed','market'),
 ('Skelton_overload set.zip','skeleton','ราชันกระดูกนิรันดร์','ปลาย',199,'skeleton','quest'),
 ('starlight_set.zip','starlight','แสงดาวผู้พิทักษ์','กลาง',179,'ethereal','guild'),
 ('Essential Icons Vol.1.zip','chamby-icons','สัญลักษณ์เมืองลูม่า','เมนู',0,'town','menus'),
 ('EssentialIconsVol1.zip','crystal-icons-1','สัญลักษณ์บริการชุดหนึ่ง','เมนู',0,'tool','menus'),
 ('EssentialIconsVol2.zip','crystal-icons-2','สัญลักษณ์บริการชุดสอง','เมนู',0,'item','menus'),
]
ICONS = {'gem':'gem','chest':'bank','sword':'weapon','book':'scroll','pick':'miner',
         'banner':'guild','portal':'portal_room','bank':'bank','repair':'blacksmith',
         'craft':'forge','quest':'scroll','shop':'marketplace','furniture':'furniture',
         'pets':'pet','admin':'admin','mail':'treasure_cave','enchant':'enchanter',
         'currency':'coin', **{r[5]:r[5] for r in SETS}}
PICTOGRAMS = {'gem':'diamond','chest':'chest','sword':'sword','book':'book','pick':'cave',
 'banner':'flag','portal':'wizard_tower','bank':'coins','repair':'anvil','craft':'anvil',
 'quest':'scroll','shop':'money','furniture':'house','pets':'heart','admin':'cog',
 'mail':'chest','enchant':'purple_orb','currency':'coins','bard':'star','berserker':'sword',
 'demon':'skull','spell':'mana','vampire':'moon','necromancer':'skull','elemental':'fire',
 'volcano':'fire','dragon':'shield','celestial':'moon','mage':'wizard_tower','chef':'chef_hat',
 'tavern':'house','treasure_cave':'key','pet':'heart','hero':'shield','explorer':'compass',
 'monk':'level_up','holy':'sun','blessed':'clover','skeleton':'skull','ethereal':'star',
 'town':'house','tool':'cog','item':'chest'}

def write_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')

def repair(text, archive, entry):
    """Only modify these known two-space item keys; reject unknown conflicting keys."""
    lines = text.replace('\r\r\n','\n').replace('\r\n','\n').splitlines(True)
    if archive not in ('Essential Icons Vol.1.zip','lunardragon_set.zip','OnePunchMan_set.zip') or not any(s.strip() in ('items:','font_images:') for s in lines):
        return ''.join(lines),[]
    matches = [(i,re.match(r'^  ([\w]+):\s*$',s).group(1)) for i,s in enumerate(lines)
               if re.match(r'^  ([\w]+):\s*$',s)]
    seen, edits, report = {}, [], []
    for k,(start,key) in enumerate(matches):
        end = matches[k+1][0] if k+1 < len(matches) else len(lines)
        block = ''.join(lines[start+1:end]).strip()
        if key not in seen:
            seen[key] = block
            continue
        # Colour icon repetitions have identical definitions; collapse only exact matches.
        if block == seen[key]:
            edits.append((start,end,'')); action='collapse-identical'
        elif archive == 'lunardragon_set.zip' and key == 'lunardragonset_tail_cosmetics_self' and 'model_path: helmet' in block:
            new='lunardragonset_helmet_cosmeticscore'
            assert new not in {key for _,key in matches}
            edits.append((start,start+1,'  '+new+':\n')); action='rename-to:'+new
        elif archive == 'OnePunchMan_set.zip' and key == 'onepunchman_greatsword' and 'model_path: onepunchman_greatsword' in block:
            new='onepunchman_greatsword_alternate'
            assert new not in {key for _,key in matches}
            edits.append((start,start+1,'  '+new+':\n')); action='rename-to:'+new
        else:
            raise ValueError(f'Unreviewed duplicate: {archive}/{entry}: {key}')
        report.append({'archive':archive,'entry':entry,'key':key,'line':start+1,'action':action})
    for start,end,replacement in reversed(edits): lines[start:end] = [replacement] if replacement else []
    text=''.join(lines)
    if archive=='OnePunchMan_set.zip' and 'resourcepack' not in entry and re.search(r'^\s+model_path: greatsword\s*$',text,re.M):
        text=re.sub(r'^(\s+model_path:) greatsword\s*$',r'\1 onepunchman_greatsword',text,flags=re.M)
        report.append({'archive':archive,'entry':entry,'key':'onepunchman_greatsword',
                       'action':'fix-missing-model-path:onepunchman_greatsword'})
    return text,report

def prepare():
    WORK.mkdir(parents=True,exist_ok=True)
    rows,fixes,inventory = [],[],[]
    for f in sorted(SOURCE.iterdir()):
        if f.suffix.lower() not in ('.zip','.jar'): continue
        with zipfile.ZipFile(f) as z:
            names=z.namelist()
            for n in names:
                p=PurePosixPath(n.replace('\\','/'))
                if '..' in p.parts or p.is_absolute() or re.match(r'^[A-Za-z]:',n):
                    raise ValueError(f'Unsafe archive path: {f.name}: {n}')
            item={'file':f.name,'sha256':hashlib.sha256(f.read_bytes()).hexdigest(),
                  'entries':len(names),'bytes':f.stat().st_size,'yaml':0,'models':0,'textures':0}
            if f.suffix.lower()=='.zip':
                for n in names:
                    if n.lower().endswith(('.yml','.yaml')):
                        raw=z.read(n).decode('utf-8-sig')
                        text,report=repair(raw,f.name,n)
                        out=WORK/'yaml'/f'{len(rows):04d}.yml'; out.parent.mkdir(parents=True,exist_ok=True)
                        out.write_text(text,encoding='utf-8'); fixes+=report
                        rows.append({'file':str(out.relative_to(ROOT)),'archive':f.name,'entry':n})
                        item['yaml']+=1
                    item['models']+=int(n.endswith(('.json','.bbmodel')))
                    item['textures']+=int(n.endswith('.png'))
            else:
                # Inspect metadata only; no Java bytecode is executed.
                for name in ('plugin.yml','paper-plugin.yml'):
                    if name in names:
                        text=z.read(name).decode('utf-8',errors='replace')
                        item['plugin_metadata']={key:match.group(1).strip() for key in ('name','version','api-version')
                            if (match:=re.search(r'^'+key+r':\s*(.+)$',text,re.M))}
            inventory.append(item)
    write_json(WORK/'index.json',rows)
    write_json(DEST/'source-inventory.json',inventory)
    write_json(DEST/'repairs.json',fixes)
    java=Path(os.environ.get('JAVA_HOME',''))/'bin/java.exe'
    if not java.is_file():
        java=next((ROOT/'.tools/jdk25').glob('*/bin/java.exe'))
    cache=ROOT/'.cache/gradle/caches/modules-2/files-2.1'
    cp=os.pathsep.join(str(next(cache.glob(f'{group}/{artifact}/*/*/*.jar')))
                       for group,artifact in [('org.yaml','snakeyaml'),('com.google.code.gson','gson')])
    subprocess.run([str(java),'-cp',cp,str(ROOT/'tools/ParseModuleYaml.java'),
                    str(WORK/'index.json'),str(WORK/'parsed.json')],check=True,cwd=ROOT)

def role(key, data):
    resource=data.get('resource',{})
    if data.get('behaviours',{}).get('furniture'): return 'furniture'
    if 'cosmetic' in key or any(s in key for s in ('wings','tail','hat','headgear')): return 'appearance'
    material=resource.get('material','')
    if any(material.endswith('_'+r) for r in ('BOOTS','CHESTPLATE','LEGGINGS','HELMET')): return 'armor'
    if any(s in key for s in ('chest','opening','key','scroll','plushie')): return 'quest-prop'
    if material in ('BOW','CROSSBOW','TRIDENT') or material.endswith(('_SWORD','_AXE')): return 'weapon'
    if material.endswith(('_PICKAXE','_SHOVEL','_HOE')) or material=='FISHING_ROD': return 'tool'
    return 'appearance'

def model_entries(z):
    models={}
    for n in z.namelist():
        m=re.search(r'(?:assets/|resourcepack/)([\w.-]+)/models/(.+)\.json$',n)
        if m: models.setdefault(m[1]+':'+m[2],n)
    return models

def check_models(z, archive, issues):
    models=model_entries(z)
    textures=set()
    for n in z.namelist():
        m=re.search(r'(?:assets/|resourcepack/)([\w.-]+)/textures/(.+)\.png$',n)
        if m: textures.add(m[1]+':'+m[2])
    for ident,entry in models.items():
        data=json.loads(z.read(entry))
        for value in data.get('textures',{}).values():
            if ':' in value and not value.startswith('minecraft:') and value not in textures:
                issues.append({'id':ident,'archive':archive,'issue':'texture-unresolved','texture':value})
        parent=data.get('parent','minecraft:item/generated')
        if ':' in parent and not parent.startswith('minecraft:') and parent not in models:
            issues.append({'id':ident,'archive':archive,'issue':'parent-model-unresolved','parent':parent})
    return len(models)

def build():
    parsed=json.loads((WORK/'parsed.json').read_text(encoding='utf-8'))
    assert not any('error' in r for r in parsed)
    sets,items,issues=[],[],[]
    checked_models=0
    for archive,sid,name,tier,price,icon,zone in SETS:
        metadata={'id':sid,'archive':archive,'name':name,'tier':tier,'price':price,
                  'icon':f'library/{icon}.png','zone':zone,'live':False}
        sets.append(metadata)
        if tier=='เมนู': continue
        with zipfile.ZipFile(SOURCE/archive) as z:
            models=model_entries(z)
            checked_models+=check_models(z,archive,issues)
            known={}
            for row in parsed:
                d=row.get('data')
                if row['archive']!=archive or not isinstance(d,dict) or not isinstance(d.get('items'),dict) or 'info' not in d: continue
                ns=d['info'].get('namespace')
                for key,value in d['items'].items():
                    if not isinstance(value,dict): continue
                    ident=ns+':'+key
                    if ident in known:
                        if value!=known[ident]: issues.append({'id':ident,'issue':'conflicting-item-definition','entry':row['entry']})
                        continue
                    known[ident]=value
                    resource=value.get('resource',{})
                    path=resource.get('model_path')
                    model_id=(path if path and ':' in path else ns+':'+path) if path else None
                    model=models.get(model_id)
                    if path and not model: issues.append({'id':ident,'issue':'model-path-unresolved','model':model_id})
                    kind=role(key,value)
                    profile={'เริ่มต้น':'adventurer','กลาง':'veteran','ปลาย':'master'}.get(tier)
                    words={'axe':'ขวาน','bow':'ธนู','crossbow':'หน้าไม้','sword':'ดาบ','greatsword':'ดาบใหญ่',
                           'pickaxe':'พลั่วขุด','shovel':'พลั่ว','hoe':'จอบ','staff':'คทา','shield':'โล่',
                           'trident':'ตรีศูล','hammer':'ค้อน','scythe':'เคียว','spear':'หอก',
                           'helmet':'หมวก','chestplate':'เกราะอก','leggings':'กางเกงเกราะ','boots':'รองเท้า',
                           'wings':'ปีก','key':'กุญแจ','chest':'หีบ','fishing':'เบ็ด','tail':'หาง'}
                    label=next((th for token,th in sorted(words.items(),key=lambda v:-len(v[0])) if token in key),
                               {'furniture':'เฟอร์นิเจอร์','quest-prop':'ของสะสม','appearance':'เครื่องประดับ'}.get(kind,'อุปกรณ์'))
                    items.append({'id':ident,'setId':sid,'sourceEntry':row['entry'],
                        'originalName':value.get('display_name',key),'name':label+' '+name,
                        'material':resource.get('material'),'role':kind,'modelEntry':model,
                        'modelId':model_id,'customModelData':resource.get('model_id'),
                        'balanceProfile':profile if kind in ('weapon','armor','tool') else None,
                        'gameplaySource':'quest/craft' if kind in ('weapon','armor','tool') else 'cosmetic/quest',
                        'moneyPurchase':'appearance-only' if price else 'disabled',
                        'runtimeStatus':'not-tested'})
    ids=[i['id'] for i in items]
    assert len(ids)==len(set(ids)), 'Cross-pack namespace collision'
    # Resolve explicit custom model numbers across all item config variants (including icons).
    numbers={}
    for row in parsed:
        d=row.get('data')
        if not isinstance(d,dict) or 'info' not in d or not isinstance(d.get('items'),dict): continue
        for key,val in d['items'].items():
            if not isinstance(val,dict): continue
            r=val.get('resource',{}); n=r.get('model_id')
            if n is None: continue
            pair=(r.get('material'),n); ident=d['info']['namespace']+':'+key
            if pair in numbers and numbers[pair]!=ident:
                issues.append({'id':ident,'issue':'custom-model-data-collision','with':numbers[pair],'material':pair[0],'number':n})
            numbers[pair]=ident
    write_json(DEST/'catalog.json',{'version':1,'sets':sets,'items':items,
               'checkedCustomModels':checked_models,'issues':issues})
    translations={i['id']:i['name'] for i in items}
    for row in parsed:
        d=row.get('data')
        if isinstance(d,dict) and 'info' in d and isinstance(d.get('items'),dict):
            for key,value in d['items'].items():
                if isinstance(value,dict) and d['info']['namespace']+':'+key in translations:
                    value['display_name']=translations[d['info']['namespace']+':'+key]
        # JSON is a YAML-compatible representation. Each original path is retained.
        target=DEST/'local/normalized'/Path(row['archive']).stem/row['entry']
        write_json(target,d)
    WEB.mkdir(parents=True,exist_ok=True)
    icon_sources=[]
    for target,original in sorted(PICTOGRAMS.items()):
        for archive in ('EssentialIconsVol1.zip','EssentialIconsVol2.zip'):
            with zipfile.ZipFile(SOURCE/archive) as z:
                entry=next((n for n in z.namelist() if '/itemsadder/' in n and n.endswith('/'+original+'.png')),None)
                if not entry: continue
                data=z.read(entry); (WEB/f'{target}.png').write_bytes(data)
                icon_sources.append({'name':target,'path':f'library/{target}.png','archive':archive,
                                     'entry':entry,'sha256':hashlib.sha256(data).hexdigest(),'creator':'Crystal Creations'})
                break
        else: raise ValueError('Missing pictogram: '+original)
    with zipfile.ZipFile(SOURCE/'Essential Icons Vol.1.zip') as z:
        for tag in ('player','admin','guild','rune','vip','mythic','bank'):
            entry=f'Essential Icons Vol.1/All Images/Icon Images Color 1/{tag}_color_1.png'
            data=z.read(entry); (WEB/f'tag-{tag}.png').write_bytes(data)
            icon_sources.append({'name':'tag-'+tag,'path':f'library/tag-{tag}.png','archive':'Essential Icons Vol.1.zip',
                                 'entry':entry,'sha256':hashlib.sha256(data).hexdigest(),'creator':'Chamby'})
    write_json(DEST/'icons.json',icon_sources)
    # Semantic icons identify the set; they are deliberately not claimed as 3D item renders.
    offers=[{'id':'library-'+s['id'],'name':s['name'],'type': 'เฟอร์นิเจอร์' if s['tier']=='เฟอร์นิเจอร์' else 'รูปลักษณ์',
             'price':s['price'],'image':s['icon'],'setId':s['id'],'previewKind':'category-icon',
             'description':'รูปลักษณ์ชุด '+s['name']+' · ไม่มีโบนัสโจมตีจากการซื้อด้วยเงินจริง',
             'itemCount':sum(i['setId']==s['id'] for i in items), 'draft':True,
             'fulfillment':{'kind':'cosmetic-entitlement','setId':s['id'],'version':1,'combatStats':False}}
            for s in sets if s['price']>0]
    write_json(ROOT/'website/lib/library-products.json',offers)
    print(f'Sets: {len(sets)}, items: {len(items)}, icons: {len(icon_sources)}, draft offers: {len(offers)}, issues: {len(issues)}')
    if issues: raise SystemExit('Unresolved catalogue issues; do not deploy these packs.')

if __name__=='__main__':
    prepare()
    build()
