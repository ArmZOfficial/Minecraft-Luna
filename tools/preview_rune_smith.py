"""Capture native Blockbench poses for smith v2 and check the hammer stays in the right fist."""
import argparse
import base64
import json
from blockbench_mcp import Client
from build_moonfall_boss import OUT, data, dispose_view
from build_rune_smith import NAME

CAPTURES={"idle":[0,2,4],"greet":[0,.35,.6,.8,1.6],"hammer":[0,.35,.55,.95,1.25,2],
          "craft_success":[0,.5,1,1.8,2.4]}
SHOTS={("idle",2):"idle",("greet",.6):"greet",("hammer",.35):"hammer-windup",("hammer",.55):"hammer-impact",("craft_success",1):"craft-success"}
# Props must touch their holding hand in every frame; distance between world boxes, in pixels.
PROBE="""(()=>{const box=n=>{const b=new THREE.Box3();for(const c of Cube.all){if(!c.mesh||!c.visibility)continue;
let g=c.parent;while(g&&g!=='root'&&g.name!==n)g=g.parent;if(!g||g==='root')continue;
c.mesh.updateWorldMatrix(true,true);b.union(new THREE.Box3().setFromObject(c.mesh))}return b};
const own=(n,skip)=>{const b=new THREE.Box3();for(const c of Cube.all){if(!c.mesh||c.parent?.name!==n)continue;
c.mesh.updateWorldMatrix(true,true);b.union(new THREE.Box3().setFromObject(c.mesh))}return b};
const gap=(a,b)=>{let d=0;for(const k of ['x','y','z'])d=Math.max(d,a.min[k]-b.max[k],b.min[k]-a.max[k]);return Math.max(0,d)};
const all=new THREE.Box3();for(const c of Cube.all){if(c.name==='collision_proxy'||!c.mesh||!c.visibility)continue;all.union(new THREE.Box3().setFromObject(c.mesh))}
return {time:Timeline.time,selected:Animation.selected?.name,min:all.min.toArray(),max:all.max.toArray(),
hammer_gap:gap(box('hammer'),own('right_hand')),tongs_gap:gap(box('tongs'),own('waist'))}})()"""

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("--project-uuid",required=True)
    args=parser.parse_args()
    client=Client()
    def call(name,arguments):
        project=data(client.call("get_project_info",{}))["project"]
        if project["uuid"]!=args.project_uuid or project["name"]!=NAME: raise RuntimeError("Active project changed")
        return client.call(name,arguments)
    call("set_mode",{"mode_id":"animate"})
    call("create_offscreen_view",{"id":"smith_pose","width":1200,"height":1200,"copy_view":"none"})
    report={}
    try:
        call("set_camera_angle",{"view":"smith_pose","position":[70,48,-90],"target":[0,21,0],"projection":"orthographic","zoom":.62})
        for name,times in CAPTURES.items():
            report[name]=[]
            for time in times:
                call("animation_timeline",{"animation_id":name,"action":"set_time","time":time})
                state=data(call("risky_eval",{"code":PROBE}))
                if state.get("selected")!=name or abs(state.get("time",-1)-time)>1e-6: raise RuntimeError("Timeline did not select requested frame")
                report[name].append(state)
                if (name,time) in SHOTS:
                    image=next(b for b in call("capture_screenshot",{"view":"smith_pose","format":"png"})["content"] if b.get("type")=="image")
                    (OUT/(NAME+"-"+SHOTS[(name,time)]+".png")).write_bytes(base64.b64decode(image["data"]))
            print(json.dumps({"animation":name,"max_hammer_gap":max(s["hammer_gap"] for s in report[name]),
                              "max_tongs_gap":max(s["tongs_gap"] for s in report[name])}),flush=True)
        call("animation_timeline",{"animation_id":"idle","action":"stop"})
        (OUT/(NAME+"-pose-checks.json")).write_text(json.dumps(report,indent=2),encoding="utf-8",newline="\n")
        call("set_mode",{"mode_id":"edit"})
    finally:
        dispose_view(client,"smith_pose")

if __name__=="__main__": main()
