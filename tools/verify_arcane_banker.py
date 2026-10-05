"""Offline gate for the exported banker v2 source, rig, atlas, held props and pose evidence."""
import base64
import hashlib
import json
import math
import struct
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
DIR=ROOT/"output/lobby-concept/assets/models"
NAME="npc_arcane_banker_v2"
ANIMATIONS={"idle":"loop","greet":"once","count_coins":"once","inspect_ledger":"once"}

def check():
    model=json.loads((DIR/(NAME+".bbmodel")).read_text(encoding="utf-8"))
    manifest=json.loads((DIR/(NAME+"-manifest.json")).read_text(encoding="utf-8"))
    assert model["meta"]["model_format"]=="free"
    assert model["resolution"]=={"width":256,"height":256}
    assert not manifest["runtime_tested"]
    cubes={e["uuid"]:e for e in model["elements"]}
    assert len(cubes)==len(model["elements"])==manifest["cubes"]
    assert len({c["name"] for c in cubes.values()})==len(cubes)
    groups={}; references=[]
    definitions={g["uuid"]:g for g in model.get("groups",[])}
    def visit(nodes,parent=None):
        for node in nodes:
            if isinstance(node,str): references.append(node); continue
            node={**definitions.get(node["uuid"],{}),**node}
            assert node["uuid"] not in groups
            groups[node["uuid"]]={"node":node,"parent":parent}
            visit(node.get("children",[]),node["uuid"])
    visit(model["outliner"])
    assert len(groups)==manifest["bones"]==17
    assert len(references)==len(cubes) and set(references)==set(cubes)
    names={item["node"]["name"]:gid for gid,item in groups.items()}
    assert len(names)==len(groups)
    parent=lambda n:groups[groups[names[n]]["parent"]]["node"]["name"]
    for side in ["right","left"]:
        assert parent(side+"_forearm")==side+"_upper_arm" and parent(side+"_hand")==side+"_forearm"
        assert parent(side+"_coattail")=="waist"
    # Props must be parented to the hand that holds them, so every animation carries them.
    assert parent("ledger")=="left_hand" and parent("coin")=="right_hand"
    hit=groups[names["hitbox"]]["node"]
    assert groups[names["hitbox"]]["parent"] is None and len(hit["children"])==1
    collision=cubes[hit["children"][0]]
    assert collision["from"]==[-4.5,0,-4.5] and collision["to"]==[4.5,32.75,4.5] and not collision["visibility"]
    texture_ids={t["uuid"] for t in model["textures"]}
    assert len(texture_ids)==1
    png=(DIR/(NAME+".png")).read_bytes()
    assert png[:8]==b"\x89PNG\r\n\x1a\n" and struct.unpack(">II",png[16:24])==(256,256)
    assert base64.b64decode(model["textures"][0]["source"].split(",",1)[1])==png
    for cube in cubes.values():
        assert all(math.isfinite(v) for v in cube["from"]+cube["to"])
        assert all(a<b for a,b in zip(cube["from"],cube["to"]))
        angles=cube.get("rotation",[0,0,0])
        assert sum(abs(a)>1e-7 for a in angles)<=1 and all(a in [0,22.5,-22.5,45,-45] for a in angles)
        assert cube["from"][1]>=0
        for face in cube.get("faces",{}).values():
            assert face["texture"] in texture_ids or face["texture"]==0
            assert len(face["uv"])==4 and all(math.isfinite(v) and 0<=v<=256 for v in face["uv"])
    animations={a["name"]:a for a in model["animations"]}
    assert set(animations)==set(ANIMATIONS)
    key_count=0
    for name,animation in animations.items():
        assert animation["snapping"]==20 and animation["length"]>0
        assert animation["loop"]==ANIMATIONS[name] and animation["override"]==(name!="idle")
        for gid,animator in animation["animators"].items():
            assert gid in groups
            assert gid!=names["hitbox"] or not animator.get("keyframes")
            channels={}
            for key in animator.get("keyframes",[]):
                assert 0<=key["time"]<=animation["length"]+1e-7
                assert abs(key["time"]*20-round(key["time"]*20))<1e-6
                assert all(math.isfinite(float(key["data_points"][0][a])) for a in "xyz")
                channels.setdefault(key["channel"],[]).append(key); key_count+=1
            for keys in channels.values():
                ordered=sorted(keys,key=lambda k:k["time"])
                assert len({k["time"] for k in keys})==len(keys)
                # Every clip returns to the rest pose so the next state starts clean.
                assert ordered[0]["time"]==0 and abs(ordered[-1]["time"]-animation["length"])<1e-6
                assert ordered[0]["data_points"]==ordered[-1]["data_points"]
    assert key_count==manifest["timeline"]["keyframes"]
    poses=json.loads((DIR/(NAME+"-pose-checks.json")).read_text(encoding="utf-8"))
    assert set(poses)==set(animations)
    frames=sum(len(f) for f in poses.values())
    assert frames==manifest["native_timeline_frames_checked"]
    for name,states in poses.items():
        assert len({tuple(s["max"]) for s in states})>1 or name=="idle"
        for s in states:
            assert s["selected"]==name and 0<=s["time"]<=animations[name]["length"]+1e-7
            assert s["ledger_gap"]==0 and s["coin_gap"]==0
            assert s["min"][1]>=-1e-6 and s["max"][1]<40
    for path,digest in manifest["sha256"].items():
        assert hashlib.sha256((ROOT/path).read_bytes()).hexdigest()==digest,path
    return {"asset":NAME,"cubes":len(cubes),"bones":len(groups),"animations":len(animations),
            "keyframes":key_count,"native_frames":frames,"hashes":len(manifest["sha256"]),"runtime_tested":False}

if __name__=="__main__":
    print(json.dumps(check(),ensure_ascii=False,indent=2))
