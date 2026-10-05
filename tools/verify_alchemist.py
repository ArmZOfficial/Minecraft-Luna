"""Offline gate: actual source, baked UV, rig, clips and 265 native frame probes."""
import base64
import hashlib
import json
import math
from pathlib import Path
from PIL import Image
from build_alchemist import NAME,ROWS,BONES,MATS,ARTS,ANIMATIONS,bake_npc

ROOT=Path(__file__).resolve().parents[1]
DIR=ROOT/"output/lobby-concept/assets/models"

def check():
    model=json.loads((DIR/(NAME+".bbmodel")).read_text(encoding="utf-8"))
    meta=json.loads((DIR/(NAME+"-manifest.json")).read_text(encoding="utf-8"))
    contract=json.loads((ROOT/"server/content/npc-models/lyra-model-contract.json").read_text(encoding="utf-8"))
    assert model["meta"]["model_format"]=="free" and not model["meta"]["box_uv"]
    assert not meta["runtime_tested"] and not contract["enabled"] and not contract["pack"]["compiled"]
    assert not contract["ownership"]["animation_keyframes_may_give_items_or_debit_money"]
    assert not contract["interaction"]["action_implemented"]
    assert contract["placement"]["yaw"]==0 and contract["placement"]["xz"]==[-69.5,68.5]
    cubes={c["uuid"]:c for c in model["elements"]}
    by_name={c["name"]:c for c in cubes.values()}
    assert len(cubes)==len(by_name)==len(ROWS)==meta["cubes"]==206
    groups={};refs=[];defs={g["uuid"]:g for g in model.get("groups",[])}
    def walk(nodes,parent=None):
        for node in nodes:
            if isinstance(node,str):refs.append(node);continue
            node={**defs.get(node["uuid"],{}),**node}
            assert node["uuid"] not in groups
            groups[node["uuid"]]={"node":node,"parent":parent}
            walk(node.get("children",[]),node["uuid"])
    walk(model["outliner"])
    names={g["node"]["name"]:uid for uid,g in groups.items()}
    assert len(groups)==len(names)==len(BONES)==meta["bones"]==21
    assert len(refs)==len(cubes) and set(refs)==set(cubes)
    for name,pivot,parent in BONES:
        g=groups[names[name]]
        assert g["node"]["origin"]==pivot
        assert g["parent"]==(None if parent=="root" else names[parent])
    for bone,_,row in ROWS:
        c=by_name[row["name"]]
        assert all(math.isfinite(v) for v in c["from"]+c["to"])
        assert all(abs(a-b)<1e-6 for a,b in zip(c["from"]+c["to"],row["from"]+row["to"]))
        if "origin" in row:assert all(abs(a-b)<1e-6 for a,b in zip(c["origin"],row["origin"]))
        assert all(a<b for a,b in zip(c["from"],c["to"])) and c["from"][1]>=0
        assert c.get("rotation",[0,0,0])==row.get("rotation",[0,0,0])
        angles=c.get("rotation",[0,0,0])
        assert sum(abs(a)>1e-7 for a in angles)<=1 and all(a in [0,22.5,-22.5,45,-45] for a in angles)
        assert c["uuid"] in groups[names[bone]]["node"]["children"]
        assert c["autouv"]==0
    collision=by_name["collision_proxy"]
    assert not collision["visibility"] and collision["from"]==contract["hitbox_proposal"]["from_units"]
    assert collision["to"]==contract["hitbox_proposal"]["to_units"]
    atlas,uv=bake_npc(ROWS,MATS,ARTS,seed=7381)
    assert model["resolution"]=={"width":512,"height":512} and atlas.size==(512,512)
    textures={t["uuid"] for t in model["textures"]};assert len(textures)==1
    png=(DIR/(NAME+".png")).read_bytes()
    assert base64.b64decode(model["textures"][0]["source"].split(",",1)[1])==png
    assert Image.open(DIR/(NAME+".png")).convert("RGBA").tobytes()==atlas.tobytes()
    rectangles=[]
    for cube in cubes.values():
        for name,face in cube["faces"].items():
            assert face["texture"] in textures or face["texture"]==0
            assert face["uv"]==uv[cube["name"]][name]
            x0,y0,x1,y1=face["uv"]
            assert 0<x0<x1<512 and 0<y0<y1<512
            rectangles.append((x0,y0,x1,y1))
    # No two painted faces may overlap, and gutters remain between their rectangles.
    for i,a in enumerate(rectangles):
        for b in rectangles[i+1:]:
            assert a[2]<=b[0] or b[2]<=a[0] or a[3]<=b[1] or b[3]<=a[1]
    animations={a["name"]:a for a in model["animations"]}
    assert set(animations)=={a[0] for a in ANIMATIONS}
    keys=0
    for name,length,loop,_ in ANIMATIONS:
        a=animations[name]
        assert a["length"]==length and a["snapping"]==20 and a["loop"]==("loop" if loop else "once")
        assert a["override"]==(name!="idle")
        for uid,b in a["animators"].items():
            assert uid in groups
            assert uid not in [names["hitbox"],names["motion_root"],names["brew_station"]] or not b.get("keyframes")
            channels={}
            for k in b.get("keyframes",[]):
                assert 0<=k["time"]<=length and abs(k["time"]*20-round(k["time"]*20))<1e-6
                assert all(math.isfinite(float(k["data_points"][0][xyz])) for xyz in "xyz")
                channels.setdefault(k["channel"],[]).append(k);keys+=1
            for channel in channels.values():
                ordered=sorted(channel,key=lambda k:k["time"])
                assert len({k["time"] for k in channel})==len(channel)
                assert ordered[0]["time"]==0 and ordered[-1]["time"]==length
                assert ordered[0]["data_points"]==ordered[-1]["data_points"]
    assert keys==meta["timeline"]["keyframes"]==75
    poses=json.loads((DIR/(NAME+"-pose-checks.json")).read_text(encoding="utf-8"))
    assert set(poses)==set(animations)
    frame_count=sum(len(s) for s in poses.values())
    assert frame_count==meta["native_timeline_frames_checked"]==265
    first_station=poses["idle"][0]
    for name,length,_,_ in ANIMATIONS:
        states=poses[name]
        assert len(states)==round(length*20)+1
        for tick,s in enumerate(states):
            assert s["selected"]==name and abs(s["time"]-tick/20)<1e-6
            assert s["flask_gap"]<=1e-6 and s["rod_gap"]<=1e-6 and s["braid_head_gap"]<=1e-6
            assert s["tip_inside_liquid"]
            assert s["station_min"]==first_station["station_min"] and s["station_max"]==first_station["station_max"]
            assert all(math.isfinite(v) for v in s["min"]+s["max"]+s["rod_tip"])
            assert abs(s["min"][1])<1e-6 and s["max"][1]<48
            assert s["max"][0]-s["min"][0]<48 and s["max"][2]-s["min"][2]<48
    assert len({tuple(s["rod_tip"]) for s in poses["stir"]})>20
    for path,digest in meta["sha256"].items():assert hashlib.sha256((ROOT/path).read_bytes()).hexdigest()==digest,path
    return {"asset":NAME,"cubes":len(cubes),"bones":len(groups),"painted_faces":len(rectangles),
            "animations":len(animations),"keyframes":keys,"native_frames":frame_count,"hashes":len(meta["sha256"]),"runtime_tested":False}

if __name__=="__main__":print(json.dumps(check(),ensure_ascii=False,indent=2))
