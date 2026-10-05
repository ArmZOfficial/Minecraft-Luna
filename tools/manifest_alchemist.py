"""Record exported Lyra files and native frame evidence without enabling runtime."""
import hashlib
import json
from pathlib import Path
from manifest_market_merchant import rig
from build_alchemist import NAME

ROOT=Path(__file__).resolve().parents[1]
MODELS=ROOT/"output/lobby-concept/assets/models"

def main():
    model=json.loads((MODELS/(NAME+".bbmodel")).read_text(encoding="utf-8"))
    poses=json.loads((MODELS/(NAME+"-pose-checks.json")).read_text(encoding="utf-8"))
    frames=[s for states in poses.values() for s in states]
    entry={"id":NAME,"format":"free","status":"model-exported-awaiting-runtime-qa",
           "animations":[a["name"] for a in model["animations"]],"cubes":len(model["elements"]),
           "bones":len(rig(model)),"height_target":2.1,"runtime_tested":False,
           "reference":"assets/references/npc-lyra-alchemist-turnaround.png",
           "texture":[model["resolution"]["width"],model["resolution"]["height"]],
           "native_timeline_frames_checked":len(frames)}
    path=MODELS/"manifest.json"
    entries=[e for e in json.loads(path.read_text(encoding="utf-8")) if e["id"]!=NAME]+[entry]
    path.write_text(json.dumps(entries,ensure_ascii=False,indent=2)+"\n",encoding="utf-8",newline="\n")
    files=[MODELS/(NAME+".bbmodel"),MODELS/(NAME+".png"),MODELS/(NAME+"-pose-checks.json"),
           *sorted(MODELS.glob(NAME+"-*.png")),
           ROOT/"output/lobby-concept/assets/references/npc-lyra-alchemist-turnaround.png",
           ROOT/"server/content/npc-models/lyra-model-contract.json"]
    detail={**entry,"revision":1,"built_at":"2026-10-06","units_per_block":16,"direction":"north (-Z)",
            "handedness":"right +X / left -X","skeleton":rig(model),
            "build_method":"AI reference -> Blockbench 5.2.1 / MCP 1.10.0 native geometry, imported new per-face pixel atlas and native animation",
            "builder":"tools/build_alchemist.py","pose_capture":"tools/preview_alchemist.py",
            "timeline":{"fps":20,"sampling":"every tick including both endpoints",
                        "keyframes":sum(len(b.get("keyframes",[])) for a in model["animations"] for b in a["animators"].values())},
            "held_props":{"flask":"left_hand","stirring_rod":"right_hand",
                          "max_flask_neck_gap_units":max(s["flask_gap"] for s in frames),
                          "max_rod_grip_gap_units":max(s["rod_gap"] for s in frames)},
            "station":{"parent":"motion_root","animated":False,"tip_outside_liquid_frames":sum(not s["tip_inside_liquid"] for s in frames)},
            "bounds_blocks":{"max_height":round(max(s["max"][1] for s in frames)/16,4),
                             "max_width":round(max(s["max"][0]-s["min"][0] for s in frames)/16,4),
                             "max_depth":round(max(s["max"][2]-s["min"][2] for s in frames)/16,4)},
            "sha256":{str(f.relative_to(ROOT)).replace("\\","/"):hashlib.sha256(f.read_bytes()).hexdigest() for f in files},
            "known_limits":["Native frame bounds are axis-aligned review evidence, not an oriented collision proof or a Minecraft test",
                            "No ModelEngine/Citizens renderer integration or compiled legacy/modern resource pack yet",
                            "market.main is a planned Core action; buying and brewing services are not implemented by this model",
                            "206 cubes and 21 bones are a high-detail single shopkeeper; crowd and client performance still require staging"]}
    (MODELS/(NAME+"-manifest.json")).write_text(json.dumps(detail,ensure_ascii=False,indent=2)+"\n",encoding="utf-8",newline="\n")
    print(json.dumps({"asset":entry,"keyframes":detail["timeline"]["keyframes"],"bounds":detail["bounds_blocks"]}))

if __name__=="__main__":main()
