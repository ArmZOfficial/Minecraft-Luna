"""Focused invariant checks for the authored asset catalogue and balance design."""
import hashlib
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def read(name): return json.loads((ROOT/name).read_text(encoding='utf-8'))
catalog=read('server/content/library/catalog.json')
balance=read('server/content/library/balance.json')
offers=read('website/lib/library-products.json')
icons=read('server/content/library/icons.json')
assert catalog['issues']==[]
assert len({i['id'] for i in catalog['items']})==len(catalog['items'])
sets={s['id'] for s in catalog['sets']}
assert all(i['setId'] in sets and i['runtimeStatus']=='not-tested' for i in catalog['items'])
assert balance['enabled'] is False and balance['rules']['cashPurchasesGrantCombatStats'] is False
assert balance['rules']['maxActiveSetTraits']==1
traits=balance['setTraits']
assert len({t['setId'] for t in traits})==len(traits)
for t in traits:
    assert t['setId'] in sets and t['cooldownSeconds']>=18
    assert t.get('healHp',0)<=4 and t.get('absorptionHp',0)<=4 and t.get('maxTargets',1)<=3
    assert t.get('damageBonusFraction',0)<=0.15
    assert t.get('damageHp',0)<=4
for p in balance['profiles'].values():
    assert p['traitSlots']<=1
    for role in ('sword','axe','bow','crossbow','trident','armor','bootsExtra','tool','fishing'):
        assert all(k in balance['enchantmentNames'] and 1<=v<=balance["rules"]["nativeEnchantLevelCap"] for k,v in p[role].items())
        assert 'minecraft:mending' not in p[role]
for o in offers:
    assert o['setId'] in sets and o['draft'] and o['fulfillment']['combatStats'] is False
    assert o['price']*100==int(o['price']*100) and 20<=o['price']<=500
    assert o['setId']!='keys'
for icon in icons:
    p=ROOT/'website/public/assets'/icon['path']
    assert p.is_file(),f'Regenerate supplied icons: {p}'
    assert hashlib.sha256(p.read_bytes()).hexdigest()==icon['sha256']
print(f"Library verified: {len(catalog['items'])} item definitions, {len(icons)} icons, {len(offers)} draft offers, {len(traits)} planned traits.")
