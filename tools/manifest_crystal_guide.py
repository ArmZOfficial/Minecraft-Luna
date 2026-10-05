"""Record the crystal guide in the model manifest with hashes of its exported evidence."""
import hashlib
import json
from pathlib import Path
from manifest_market_merchant import rig
from build_crystal_guide import NAME

ROOT=Path(__file__).resolve().parents[1]
MODELS=ROOT/"output/lobby-concept/assets/models"

def main():
    model=json.loads((MODELS/(NAME+".bbmodel")).read_text(encoding="utf-8"))
    poses=json.loads((MODELS/(NAME+"-pose-checks.json")).read_text(encoding="utf-8"))
    frames=[s for states in poses.values() for s in states]
    entry={"id":NAME,"format":"free","status":"model-exported-awaiting-runtime-qa",
           "animations":[a["name"] for a in model["animations"]],"cubes":len(model["elements"]),
           "bones":len(rig(model)),"height_target":2.1,"runtime_tested":False,"reference":None,
           "texture":[model["resolution"]["width"],model["resolution"]["height"]],
           "native_timeline_frames_checked":len(frames)}
    path=MODELS/"manifest.json"
    entries=[e for e in json.loads(path.read_text(encoding="utf-8")) if e["id"]!=NAME]+[entry]
    path.write_text(json.dumps(entries,ensure_ascii=False,indent=2)+"\n",encoding="utf-8",newline="\n")
    files=[MODELS/(NAME+".bbmodel"),MODELS/(NAME+".png"),MODELS/(NAME+"-pose-checks.json"),
           *sorted(MODELS.glob(NAME+"-*.png"))]
    detail={**entry,"revision":1,"built_at":"2026-10-06","units_per_block":16,"direction":"north (-Z)",
            "handedness":"character right is +X; crystal staff in the right fist, folding map in the left",
            "skeleton":rig(model),"zone":"01 crystal spawn plaza (spawn_guide, navigation.main)",
            "build_method":"Blockbench 5.2.1 / local MCP 1.10.0 native geometry, imported per-face pixel atlas and native animation",
            "builder":"tools/build_crystal_guide.py","pose_capture":"tools/preview_crystal_guide.py",
            "timeline":{"fps":20,"sampling":"every tick including both endpoints",
                        "keyframes":sum(len(b.get("keyframes",[])) for a in model["animations"] for b in a["animators"].values())},
            "held_props":{"staff":"right_hand","map":"left_hand",
                          "max_staff_grip_gap_units":max(s["staff_gap"] for s in frames),
                          "max_map_edge_gap_units":max(s["map_gap"] for s in frames)},
            "clearance":{"method":"27 sample points per prop cube tested inside every other cube's local frame",
                         "staff_head_hits":sum(s["staff_head_hits"] for s in frames),
                         "staff_body_hits":sum(s["staff_body_hits"] for s in frames),
                         "map_body_hits":sum(s["map_body_hits"] for s in frames)},
            "hitbox_proposal":{"bone":"hitbox","from":[-4.8,0,-4.8],"to":[4.8,34.4,4.8],"animated":False,"runtime_verified":False},
            "bounds_blocks":{"max_height":round(max(s["max"][1] for s in frames)/16,4),
                             "max_width":round(max(s["max"][0]-s["min"][0] for s in frames)/16,4),
                             "max_depth":round(max(s["max"][2]-s["min"][2] for s in frames)/16,4)},
            "sha256":{str(f.relative_to(ROOT)).replace("\\","/"):hashlib.sha256(f.read_bytes()).hexdigest() for f in files},
            "known_limits":["No ModelEngine/Citizens renderer integration or compiled resource pack yet",
                            "Point sampling (corners, edge and face midpoints) catches overlaps of a prop into the rig, not thin slivers between samples",
                            "No ChatGPT reference sheet; designed from the zone 01 ticket, the plaza banners and the NPC family",
                            "149 cubes is above the 40-70 town NPC budget; fine for the single spawn guide, crowds need a LOD variant"]}
    (MODELS/(NAME+"-manifest.json")).write_text(json.dumps(detail,ensure_ascii=False,indent=2)+"\n",encoding="utf-8",newline="\n")
    print(json.dumps({"asset":entry,"keyframes":detail["timeline"]["keyframes"],"bounds":detail["bounds_blocks"]}))

if __name__=="__main__":main()
