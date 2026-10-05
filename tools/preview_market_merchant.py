"""Capture native Blockbench poses for the market merchant and check the balance stays in the left fist."""
import argparse
import base64
import json
from blockbench_mcp import Client
from build_moonfall_boss import OUT, data, dispose_view
from build_market_merchant import NAME

CAPTURES={"idle":[0,1,2,3,4],"greet":[0,.4,.6,1.0,1.8],"show_wares":[0,.5,1.2,2.6],
          "weigh_goods":[0,.6,.9,1.3,1.7,2.4,3],"sale_success":[0,.3,.45,1.1,1.6]}
SHOTS={("idle",2):"idle",("greet",.6):"greet-hat-tip",("show_wares",1.2):"show-wares",
       ("weigh_goods",.9):"weigh-goods",("sale_success",.3):"sale-success"}
# Props must touch their holding hand in every frame; distances between world boxes, in pixels.
PROBE="""(()=>{const box=n=>{const b=new THREE.Box3();for(const c of Cube.all){if(!c.mesh||!c.visibility)continue;
let g=c.parent;while(g&&g!=='root'&&g.name!==n)g=g.parent;if(!g||g==='root')continue;
c.mesh.updateWorldMatrix(true,true);b.union(new THREE.Box3().setFromObject(c.mesh))}return b};
const own=n=>{const b=new THREE.Box3();for(const c of Cube.all){if(!c.mesh||c.parent?.name!==n)continue;
c.mesh.updateWorldMatrix(true,true);b.union(new THREE.Box3().setFromObject(c.mesh))}return b};
const gap=(a,b)=>{let d=0;for(const k of ['x','y','z'])d=Math.max(d,a.min[k]-b.max[k],b.min[k]-a.max[k]);return Math.max(0,d)};
const all=new THREE.Box3();for(const c of Cube.all){if(c.name==='collision_proxy'||!c.mesh||!c.visibility)continue;all.union(new THREE.Box3().setFromObject(c.mesh))}
const pans=['front','back'].map(n=>{const c=Cube.all.find(c=>c.name==='pan_'+n);c.mesh.updateWorldMatrix(true,true);return new THREE.Box3().setFromObject(c.mesh)});
return {time:Timeline.time,selected:Animation.selected?.name,min:all.min.toArray(),max:all.max.toArray(),
scale_gap:gap(box('scale'),own('left_hand')),hand_hat_gap:gap(own('right_hand'),own('hat')),
pan_tilt:pans.map(b=>+(b.max.y-b.min.y).toFixed(3))}})()"""

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
    call("create_offscreen_view",{"id":"merchant_pose","width":1200,"height":1200,"copy_view":"none"})
    report={}
    try:
        call("set_camera_angle",{"view":"merchant_pose","position":[70,46,-90],"target":[0,19,0],"projection":"orthographic","zoom":.66})
        for name,times in CAPTURES.items():
            report[name]=[]
            for time in times:
                call("animation_timeline",{"animation_id":name,"action":"set_time","time":time})
                state=data(call("risky_eval",{"code":PROBE}))
                if state.get("selected")!=name or abs(state.get("time",-1)-time)>1e-6: raise RuntimeError("Timeline did not select requested frame")
                report[name].append(state)
                if (name,time) in SHOTS:
                    image=next(b for b in call("capture_screenshot",{"view":"merchant_pose","format":"png"})["content"] if b.get("type")=="image")
                    (OUT/(NAME+"-"+SHOTS[(name,time)]+".png")).write_bytes(base64.b64decode(image["data"]))
            print(json.dumps({"animation":name,"max_scale_gap":max(s["scale_gap"] for s in report[name]),
                              "min_hand_hat_gap":min(s["hand_hat_gap"] for s in report[name]),
                              "max_pan_height":max(max(s["pan_tilt"]) for s in report[name])}),flush=True)
        call("animation_timeline",{"animation_id":"idle","action":"stop"})
        (OUT/(NAME+"-pose-checks.json")).write_text(json.dumps(report,indent=2),encoding="utf-8",newline="\n")
        call("set_mode",{"mode_id":"edit"})
    finally:
        dispose_view(client,"merchant_pose")

if __name__=="__main__": main()
