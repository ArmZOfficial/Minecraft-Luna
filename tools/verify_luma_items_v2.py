"""Offline gate for item v2 Java models; with --write-manifest it records hashes first."""
import argparse
import base64
import hashlib
import json
import math
import struct
from pathlib import Path
from build_luma_items_v2 import DISPLAY as ITEM_DISPLAY, ITEMS as ITEM_BUILDERS
from build_luma_props import DISPLAY as PROP_DISPLAY, ITEMS as PROP_BUILDERS, corners

DISPLAY={**ITEM_DISPLAY,**PROP_DISPLAY}
BUILDER_ITEMS={**ITEM_BUILDERS,**PROP_BUILDERS}

ROOT=Path(__file__).resolve().parents[1]
DIR=ROOT/"output/lobby-concept/assets/models"
ITEMS={"item_runeblade_v2":{"texture":"luma:item/runeblade_v2","supersedes":"item_runeblade","reference":"assets/references/item-runeblade.png"},
       "item_aether_halo_v2":{"texture":"luma:item/aether_halo_v2","supersedes":"item_aether_halo","reference":"assets/references/item-aether-halo.png"},
       # Crown form requested by the user on 2026-10-06; v2 stays as the square-ring variant.
       "item_aether_halo_v3":{"texture":"luma:item/aether_halo_v3","supersedes":"item_aether_halo_v2","reference":"user-supplied crown photo (not stored)"},
       # Station props from the catalog's 12-prop slice; no reference sheets exist, they follow the NPC v2 palette.
       **{name:{"texture":"luma:item/"+name,"supersedes":None,"reference":"NPC v2 palette (no prop reference sheet)"}
          for name,_,_ in PROP_BUILDERS.values()}}
CONTEXTS={"thirdperson_righthand","thirdperson_lefthand","firstperson_righthand","firstperson_lefthand","gui","head","ground","fixed"}

def rotate(v,angles):
    """Java applies display rotation as Rx * Ry * Rz (rotationXYZ)."""
    x,y,z=v; rx,ry,rz=(math.radians(a) for a in angles)
    x,y=x*math.cos(rz)-y*math.sin(rz),x*math.sin(rz)+y*math.cos(rz)
    x,z=x*math.cos(ry)+z*math.sin(ry),-x*math.sin(ry)+z*math.cos(ry)
    y,z=y*math.cos(rx)-z*math.sin(rx),y*math.sin(rx)+z*math.cos(rx)
    return x,y,z

def footprint(elements,slot):
    """Screen extent (x,y) of the model after a display transform, in model units around the slot centre."""
    scale=slot.get("scale",[1,1,1]); move=slot.get("translation",[0,0,0]); xs=[]; ys=[]
    for e in elements:
        for corner in corners(e):
            v=[(c-8)*s for c,s in zip(corner,scale)]
            x,y,_=rotate(v,slot.get("rotation",[0,0,0]))
            xs.append(x+move[0]); ys.append(y+move[1])
    return min(xs),max(xs),min(ys),max(ys)

def check(name,spec):
    java=json.loads((DIR/(name+".json")).read_text(encoding="utf-8"))
    model=json.loads((DIR/(name+".bbmodel")).read_text(encoding="utf-8"))
    assert model["meta"]["model_format"]=="java_block"
    assert java["textures"]["0"]==spec["texture"]
    elements=java["elements"]
    assert len(elements)==len(model["elements"])
    assert len({e["name"] for e in elements})==len(elements)
    for e in elements:
        assert all(-16<=v<=32 for v in e["from"]+e["to"]),e["name"]
        assert all(a<b for a,b in zip(e["from"],e["to"])),e["name"]
        if "rotation" in e: assert e["rotation"]["angle"] in (-45,-22.5,0,22.5,45)
        for face in e["faces"].values():
            assert face["texture"]=="#0" and all(0<=u<=16 for u in face["uv"])
    display=java["display"]
    # Every context matches the builder's tuned value; Blockbench omits a context whose value is the
    # identity transform, which Minecraft also treats as identity, so absence is allowed only then.
    identity={"rotation":[0,0,0],"translation":[0,0,0],"scale":[1,1,1]}
    intended=DISPLAY[next(key for key,(asset,_,_) in BUILDER_ITEMS.items() if asset==name)]
    assert set(intended)==CONTEXTS and set(display)<=CONTEXTS
    for context,want in intended.items():
        got={**identity,**display.get(context,{})}
        # Blockbench writes angles in -180..180 (225 becomes -135), so rotations compare modulo 360.
        assert all(abs((a-b+180)%360-180)<1e-6 for a,b in zip(got["rotation"],want["rotation"])),(name,context)
        assert all(abs(a-b)<1e-6 for k in ("translation","scale") for a,b in zip(got[k],want[k])),(name,context)
    display={context:{**identity,**display.get(context,{})} for context in CONTEXTS}
    for slot in display.values():
        assert all(-80<=t<=80 for t in slot.get("translation",[0,0,0]))
        assert all(0<s<=4 for s in slot.get("scale",[1,1,1]))
    # The inventory icon must sit inside the 16x16 slot (8 units each way from centre).
    x0,x1,y0,y1=footprint(elements,display["gui"])
    assert -8.01<=x0 and x1<=8.01 and -8.01<=y0 and y1<=8.01,(name,x0,x1,y0,y1)
    png=(DIR/(name+".png")).read_bytes()
    assert png[:8]==b"\x89PNG\r\n\x1a\n" and struct.unpack(">II",png[16:24])==(128,128)
    assert base64.b64decode(model["textures"][0]["source"].split(",",1)[1])==png
    assert (DIR/(name+"-display.png")).exists()
    return {"asset":name,"elements":len(elements),"contexts":len(display),"gui_extent":[round(v,2) for v in (x0,x1,y0,y1)]}

def files(name):
    required=[DIR/(name+ext) for ext in [".json",".bbmodel",".png","-display.png","-preview.png","-front.png"]]
    optional=[DIR/(name+ext) for ext in ["-right.png","-top.png"]]
    return required+[f for f in optional if f.exists()]

def write_manifest(name,spec,result):
    entry={"id":name,"format":"java_block","status":"model-exported-awaiting-runtime-qa","animations":[],
           "cubes":result["elements"],"runtime_tested":False,"reference":spec["reference"],"texture":[128,128],
           "display_contexts":sorted(CONTEXTS),"supersedes":spec["supersedes"]}
    path=DIR/"manifest.json"
    manifest=[e for e in json.loads(path.read_text(encoding="utf-8")) if e["id"]!=name]+[entry]
    path.write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+"\n",encoding="utf-8",newline="\n")
    detail={**entry,"revision":2,"built_at":"2026-10-06","builder":"tools/build_luma_items_v2.py",
            "display_capture":"tools/preview_luma_items_v2.py","pack_texture":spec["texture"],"gui_extent_units":result["gui_extent"],
            "sha256":{str(f.relative_to(ROOT)).replace("\\","/"):hashlib.sha256(f.read_bytes()).hexdigest() for f in files(name)},
            "known_limits":["Not loaded in a real client/resource pack yet","Legacy 1.16.5 CustomModelData mapping still untested",
                            "No animated/emissive texture; glow needs a pack-side solution","Earlier revisions are kept unchanged for comparison"]}
    (DIR/(name+"-manifest.json")).write_text(json.dumps(detail,ensure_ascii=False,indent=2)+"\n",encoding="utf-8",newline="\n")

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("--write-manifest",action="store_true")
    args=parser.parse_args()
    out=[]
    for name,spec in ITEMS.items():
        result=check(name,spec)
        if args.write_manifest: write_manifest(name,spec,result)
        detail=json.loads((DIR/(name+"-manifest.json")).read_text(encoding="utf-8"))
        assert not detail["runtime_tested"] and detail["cubes"]==result["elements"]
        for path,digest in detail["sha256"].items():
            assert hashlib.sha256((ROOT/path).read_bytes()).hexdigest()==digest,path
        out.append({**result,"hashes":len(detail["sha256"])})
    print(json.dumps(out,ensure_ascii=False,indent=2))

if __name__=="__main__": main()
