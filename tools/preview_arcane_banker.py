"""Capture native Blockbench poses for banker v2 and check held props stay in hand."""
import argparse
import base64
import json
from blockbench_mcp import Client
from build_moonfall_boss import OUT, data, dispose_view
from build_arcane_banker import NAME

CAPTURES={"idle":[0,2,4],"greet":[0,.35,.6,.8,1.6],"count_coins":[0,.4,1.1,2.3,3.2],
          "inspect_ledger":[0,.5,1.2,2.4]}
SHOTS={("idle",2):"idle",("greet",.6):"greet",("count_coins",1.1):"count-coins",("inspect_ledger",1.2):"inspect-ledger"}
# Props must touch their holding hand in every frame; distance between world boxes, in pixels.
PROBE="""(()=>{const box=n=>{const b=new THREE.Box3();for(const c of Cube.all){if(!c.mesh||!c.visibility)continue;
let g=c.parent;while(g&&g!=='root'&&g.name!==n)g=g.parent;if(!g||g==='root')continue;
c.mesh.updateWorldMatrix(true,true);b.union(new THREE.Box3().setFromObject(c.mesh))}return b};
const own=(n,skip)=>{const b=new THREE.Box3();for(const c of Cube.all){if(!c.mesh||c.parent?.name!==n)continue;
c.mesh.updateWorldMatrix(true,true);b.union(new THREE.Box3().setFromObject(c.mesh))}return b};
const gap=(a,b)=>{let d=0;for(const k of ['x','y','z'])d=Math.max(d,a.min[k]-b.max[k],b.min[k]-a.max[k]);return Math.max(0,d)};
const all=new THREE.Box3();for(const c of Cube.all){if(c.name==='collision_proxy'||!c.mesh||!c.visibility)continue;all.union(new THREE.Box3().setFromObject(c.mesh))}
return {time:Timeline.time,selected:Animation.selected?.name,min:all.min.toArray(),max:all.max.toArray(),
ledger_gap:gap(box('ledger'),own('left_hand')),coin_gap:gap(box('coin'),own('right_hand'))}})()"""

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
    call("create_offscreen_view",{"id":"banker_pose","width":1200,"height":1200,"copy_view":"none"})
    report={}
    try:
        call("set_camera_angle",{"view":"banker_pose","position":[70,40,-90],"target":[0,16,0],"projection":"orthographic","zoom":.8})
        for name,times in CAPTURES.items():
            report[name]=[]
            for time in times:
                call("animation_timeline",{"animation_id":name,"action":"set_time","time":time})
                state=data(call("risky_eval",{"code":PROBE}))
                if state.get("selected")!=name or abs(state.get("time",-1)-time)>1e-6: raise RuntimeError("Timeline did not select requested frame")
                report[name].append(state)
                if (name,time) in SHOTS:
                    image=next(b for b in call("capture_screenshot",{"view":"banker_pose","format":"png"})["content"] if b.get("type")=="image")
                    (OUT/(NAME+"-"+SHOTS[(name,time)]+".png")).write_bytes(base64.b64decode(image["data"]))
            print(json.dumps({"animation":name,"max_ledger_gap":max(s["ledger_gap"] for s in report[name]),
                              "max_coin_gap":max(s["coin_gap"] for s in report[name])}),flush=True)
        call("animation_timeline",{"animation_id":"idle","action":"stop"})
        (OUT/(NAME+"-pose-checks.json")).write_text(json.dumps(report,indent=2),encoding="utf-8",newline="\n")
        call("set_mode",{"mode_id":"edit"})
    finally:
        dispose_view(client,"banker_pose")

if __name__=="__main__": main()
