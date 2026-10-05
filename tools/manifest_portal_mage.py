"""Record mage v2 in the model manifest with hashes of its exported evidence."""
import hashlib
import json
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
MODELS=ROOT/"output/lobby-concept/assets/models"
NAME="npc_portal_mage_v2"

def rig(model):
    definitions={g["uuid"]:g for g in model.get("groups",[])}
    def walk(nodes,parent):
        for node in nodes:
            if isinstance(node,dict):
                node={**definitions.get(node["uuid"],{}),**node}
                yield {"name":node["name"],"parent":parent,"origin":node.get("origin",[0,0,0])}
                yield from walk(node.get("children",[]),node["name"])
    return list(walk(model["outliner"],None))

def main():
    model=json.loads((MODELS/(NAME+".bbmodel")).read_text(encoding="utf-8"))
    poses=json.loads((MODELS/(NAME+"-pose-checks.json")).read_text(encoding="utf-8"))
    bones=rig(model)
    keyframes=sum(len(a.get("keyframes",[])) for anim in model["animations"] for a in anim["animators"].values())
    entry={"id":NAME,"format":"free","status":"model-exported-awaiting-runtime-qa",
           "animations":[a["name"] for a in model["animations"]],"cubes":len(model["elements"]),
           "bones":len(bones),"height_target":2.3,"runtime_tested":False,
           "reference":"assets/references/npc-portal-mage.png","texture":[256,256],
           "native_timeline_frames_checked":sum(len(f) for f in poses.values()),"supersedes":"npc_portal_mage"}
    path=MODELS/"manifest.json"
    manifest=[e for e in json.loads(path.read_text(encoding="utf-8")) if e["id"]!=NAME]
    manifest.append(entry)
    path.write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+"\n",encoding="utf-8",newline="\n")
    files=[MODELS/(NAME+".bbmodel"),MODELS/(NAME+".png"),MODELS/(NAME+"-pose-checks.json"),
           *sorted(p for p in MODELS.glob(NAME+"-*.png"))]
    frames=[f for states in poses.values() for f in states]
    detail={**entry,"revision":2,"built_at":"2026-10-06",
            "build_method":"Blockbench 5.2.1 / local MCP 1.10.0 native geometry, paint and animation tools",
            "builder":"tools/build_portal_mage.py","pose_capture":"tools/preview_portal_mage.py",
            "direction":"north (-Z)","units_per_block":16,"skeleton":bones,
            "held_props":{"staff_socket":"right_hand","hood":"hi_head",
                          "max_gap_px":max(max(f["staff_gap"],f["hood_gap"]) for f in frames),
                          "staff_head_intersections":max(f["staff_head_hits"] for f in frames)},
            "handedness":"character right is +X (model faces north/-Z); staff socket under the right hand",
            "working_clearance":{"limit_blocks":2.8,"max_pose_top_blocks":round(max(f["max"][1] for f in frames)/16,4)},
            "hitbox_proposal":{"bone":"hitbox","from":[-4.6,0,-4.6],"to":[4.6,36.8,4.6],"animated":False,"runtime_verified":False},
            "timeline":{"fps":20,"keyframes":keyframes},
            "rendered_bounds_blocks":{"max_height":round(max(f["max"][1] for f in frames)/16,4),
                                     "max_width":round(max(f["max"][0]-f["min"][0] for f in frames)/16,4)},
            "sha256":{str(f.relative_to(ROOT)).replace("\\","/"):hashlib.sha256(f.read_bytes()).hexdigest() for f in files},
            "known_limits":["No ModelEngine/Citizens runtime or client evidence yet",
                            "Pose samples are review evidence, not a collision-free proof across every interpolated frame",
                            "v1 (npc_portal_mage) is kept unchanged for comparison"]}
    (MODELS/(NAME+"-manifest.json")).write_text(json.dumps(detail,ensure_ascii=False,indent=2)+"\n",encoding="utf-8",newline="\n")
    print(json.dumps({"asset":entry,"keyframes":keyframes,"bounds":detail["rendered_bounds_blocks"]}))

if __name__=="__main__": main()
