"""Review every 20 FPS timeline frame in native Blockbench, including held props."""
import argparse
import base64
import json
from blockbench_mcp import Client
from build_moonfall_boss import OUT,data,dispose_view
from build_alchemist import NAME,ANIMATIONS

SHOTS={("idle",2):"idle",("greet",.75):"greet",("stir",.75):"stir",
       ("offer_potion",1.2):"offer-potion",("brew_success",.4):"brew-success"}
PROBE="""(()=>{const own=n=>{const b=new THREE.Box3();for(const c of Cube.all){if(c.parent?.name!==n||!c.mesh)continue;
c.mesh.updateWorldMatrix(true,true);b.union(new THREE.Box3().setFromObject(c.mesh))}return b};
const cube=n=>{const c=Cube.all.find(c=>c.name===n);c.mesh.updateWorldMatrix(true,true);return new THREE.Box3().setFromObject(c.mesh)};
const gap=(a,b)=>{let d=0;for(const k of ['x','y','z'])d=Math.max(d,a.min[k]-b.max[k],b.min[k]-a.max[k]);return Math.max(0,d)};
const all=new THREE.Box3();for(const c of Cube.all){if(c.name==='collision_proxy'||!c.visibility||!c.mesh)continue;
c.mesh.updateWorldMatrix(true,true);all.union(new THREE.Box3().setFromObject(c.mesh))}
const tip=cube('rod_paddle'),liquid=cube('kettle_liquid'),point=[(tip.min.x+tip.max.x)/2,tip.min.y,(tip.min.z+tip.max.z)/2];
const station=own('brew_station');
return {time:Timeline.time,selected:Animation.selected?.name,min:all.min.toArray(),max:all.max.toArray(),
flask_gap:gap(cube('flask_neck'),own('left_hand')),rod_gap:gap(cube('rod_grip'),own('right_hand')),
braid_head_gap:gap(cube('braid_segment_0'),own('hi_head')),
rod_tip:point,liquid_min:liquid.min.toArray(),liquid_max:liquid.max.toArray(),
tip_inside_liquid:point.every((v,i)=>v>=liquid.min.toArray()[i]-1e-6&&v<=liquid.max.toArray()[i]+1e-6),
station_min:station.min.toArray(),station_max:station.max.toArray()}})()"""

def main():
    parser=argparse.ArgumentParser();parser.add_argument("--project-uuid",required=True);args=parser.parse_args()
    client=Client()
    def call(name,arguments):
        p=data(client.call("get_project_info",{}))["project"]
        if p["uuid"]!=args.project_uuid or p["name"]!=NAME:raise RuntimeError("Active project changed")
        return client.call(name,arguments)
    call("set_mode",{"mode_id":"animate"})
    call("create_offscreen_view",{"id":"lyra_pose","width":1400,"height":1400,"copy_view":"none"})
    report={}
    try:
        call("set_camera_angle",{"view":"lyra_pose","position":[60,40,-90],"target":[0,17,0],"projection":"orthographic","zoom":.76})
        for name,length,_,_ in ANIMATIONS:
            report[name]=[]
            for tick in range(round(length*20)+1):
                time=round(tick/20,3)
                call("animation_timeline",{"animation_id":name,"action":"set_time","time":time})
                state=data(call("risky_eval",{"code":PROBE}))
                assert state["selected"]==name and abs(state["time"]-time)<1e-6
                report[name].append(state)
                if (name,time) in SHOTS:
                    r=call("capture_screenshot",{"view":"lyra_pose","format":"png"})
                    im=next(b for b in r["content"] if b.get("type")=="image")
                    (OUT/(NAME+"-"+SHOTS[(name,time)]+".png")).write_bytes(base64.b64decode(im["data"]))
            print(json.dumps({"animation":name,"frames":len(report[name]),
                              "max_flask_gap":max(s["flask_gap"] for s in report[name]),
                              "max_rod_gap":max(s["rod_gap"] for s in report[name]),
                              "tip_outside_liquid":sum(not s["tip_inside_liquid"] for s in report[name])}),flush=True)
        call("animation_timeline",{"animation_id":"idle","action":"stop"})
        (OUT/(NAME+"-pose-checks.json")).write_text(json.dumps(report,indent=2)+"\n",encoding="utf-8",newline="\n")
        call("set_mode",{"mode_id":"edit"})
    finally:dispose_view(client,"lyra_pose")

if __name__=="__main__":main()
