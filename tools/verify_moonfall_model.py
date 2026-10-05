"""Offline gate for the exported Moonfall source, rig, atlas and native pose evidence."""
import base64
import hashlib
import json
import math
import struct
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
DIR=ROOT/"output/lobby-concept/assets/models"
NAME="boss_moonfall_guardian_v1"

def check():
    model=json.loads((DIR/(NAME+".bbmodel")).read_text(encoding="utf-8"))
    manifest=json.loads((DIR/(NAME+"-manifest.json")).read_text(encoding="utf-8"))
    contract=json.loads((ROOT/"server/content/dungeons/moonfall/model-contract.json").read_text(encoding="utf-8"))
    assert model["meta"]["model_format"]=="free"
    assert model["resolution"]=={"width":128,"height":128}
    assert not manifest["runtime_tested"] and not contract["enabled"] and not contract["pack"]["compiled"]
    assert len(model["elements"])==manifest["cubes"]==146
    cubes={e["uuid"]:e for e in model["elements"]}
    assert len(cubes)==len(model["elements"])
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
    assert len(groups)==manifest["bones"]==19
    assert len(references)==len(cubes) and set(references)==set(cubes)
    names={item["node"]["name"]:gid for gid,item in groups.items()}
    assert len(names)==len(groups)
    for side in ["right","left"]:
        assert groups[names[side+"_forearm"]]["parent"]==names[side+"_upper_arm"]
        assert groups[names[side+"_hand"]]["parent"]==names[side+"_forearm"]
        assert groups[names[side+"_foot"]]["parent"]==names[side+"_shin"]
    hit=groups[names["hitbox"]]["node"]
    assert hit["origin"]==[0,48,0] and len(hit["children"])==1
    collision=cubes[hit["children"][0]]
    assert collision["from"]==[-12,0,-12] and collision["to"]==[12,54,12]
    assert not collision["visibility"]
    texture_ids={t["uuid"] for t in model["textures"]}
    assert len(texture_ids)==1
    png=(DIR/(NAME+".png")).read_bytes()
    assert png[:8]==b"\x89PNG\r\n\x1a\n" and struct.unpack(">II",png[16:24])==(128,128)
    assert base64.b64decode(model["textures"][0]["source"].split(",",1)[1])==png
    for cube in cubes.values():
        assert all(math.isfinite(v) for v in cube["from"]+cube["to"])
        assert all(a<b for a,b in zip(cube["from"],cube["to"]))
        angles=cube.get("rotation",[0,0,0])
        assert sum(abs(a)>1e-7 for a in angles)<=1 and all(a in [0,22.5,-22.5,45,-45] for a in angles)
        for face in cube.get("faces",{}).values():
            assert face["texture"] in texture_ids or face["texture"]==0
            assert len(face["uv"])==4 and all(math.isfinite(v) and 0<=v<=128 for v in face["uv"])
    animations={a["name"]:a for a in model["animations"]}
    assert set(animations)=={"idle","walk","spawn","attack","slam","enrage","hurt","death"}
    key_count=0
    for name,animation in animations.items():
        assert animation["snapping"]==20 and animation["length"]>0
        assert animation["loop"]==("loop" if name in ["idle","walk"] else "hold" if name=="death" else "once")
        for gid,animator in animation["animators"].items():
            assert gid in groups
            frames=animator.get("keyframes",[])
            assert gid!=names["hitbox"] or not frames
            channels={}
            for key in frames:
                assert 0<=key["time"]<=animation["length"]+1e-7
                assert abs(key["time"]*20-round(key["time"]*20))<1e-6
                values=[float(key["data_points"][0][axis]) for axis in ["x","y","z"]]
                assert all(math.isfinite(v) for v in values)
                channels.setdefault(key["channel"],[]).append(key); key_count+=1
            for channel,keys in channels.items():
                times=[k["time"] for k in keys]
                assert len(set(times))==len(times)
                if animation["loop"]=="loop":
                    ordered=sorted(keys,key=lambda k:k["time"])
                    assert ordered[0]["time"]==0 and abs(ordered[-1]["time"]-animation["length"])<1e-6
                    assert ordered[0]["data_points"]==ordered[-1]["data_points"]
    assert key_count==manifest["timeline"]["keyframes"]==150
    assert manifest["timeline"]["slam_impact_ticks"]==25 and manifest["timeline"]["slam_impact_seconds"]==1.25
    impact=[k for k in animations["slam"]["animators"][names["body"]]["keyframes"] if k["channel"]=="rotation" and k["time"]==1.25]
    assert len(impact)==1 and float(impact[0]["data_points"][0]["x"])==-30
    poses=json.loads((DIR/(NAME+"-pose-checks.json")).read_text(encoding="utf-8"))
    assert set(poses)==set(animations)
    frames=sum(len(f) for f in poses.values())
    assert frames==manifest["native_timeline_frames_checked"]==29
    for name,states in poses.items():
        assert len({tuple(state["max"]) for state in states})>1
        for state in states:
            assert state["selected"]==name and 0<=state["time"]<=animations[name]["length"]+1e-7
            assert all(math.isfinite(v) for v in state["min"]+state["max"])
            assert state["min"][1]>=-1e-6 and state["max"][1]<112
    for path,digest in manifest["sha256"].items():
        assert hashlib.sha256((ROOT/path).read_bytes()).hexdigest()==digest,path
    assert contract["events"]["slam_windup"]["impact_tick"]==25
    assert not contract["ownership"]["animation_keyframes_may_deal_damage"]
    return {"asset":NAME,"cubes":len(cubes),"bones":len(groups),"animations":len(animations),
            "keyframes":key_count,"native_frames":frames,"hashes":len(manifest["sha256"]),"runtime_tested":False}

if __name__=="__main__":
    print(json.dumps(check(),ensure_ascii=False,indent=2))
