"""Capture native Blockbench poses for mage v2: staff stays in the right fist and every pose clears 2.8 blocks."""
import argparse
import base64
import json
from blockbench_mcp import Client
from build_moonfall_boss import OUT, data, dispose_view
from build_portal_mage import NAME

CAPTURES={"idle":[0,2,4],"greet":[0,.35,.6,.8,1.6],"cast":[0,.6,1.5,2.4,3],
          "open_portal":[0,.5,1.2,2.4]}
SHOTS={("idle",2):"idle",("greet",.6):"greet",("cast",1.5):"cast",("open_portal",1.2):"open-portal"}
# Props must touch their holding hand in every frame; distance between world boxes, in pixels.
# staff_head_hits samples a 3x3x3 grid on each staff cube and tests it inside each head cube's own
# rotated frame, so 45-degree cage bars do not report false overlaps the way world AABBs would.
PROBE="""(()=>{const box=n=>{const b=new THREE.Box3();for(const c of Cube.all){if(!c.mesh||!c.visibility)continue;
let g=c.parent;while(g&&g!=='root'&&g.name!==n)g=g.parent;if(!g||g==='root')continue;
c.mesh.updateWorldMatrix(true,true);b.union(new THREE.Box3().setFromObject(c.mesh))}return b};
const own=(n,skip)=>{const b=new THREE.Box3();for(const c of Cube.all){if(!c.mesh||c.parent?.name!==n)continue;
c.mesh.updateWorldMatrix(true,true);b.union(new THREE.Box3().setFromObject(c.mesh))}return b};
const gap=(a,b)=>{let d=0;for(const k of ['x','y','z'])d=Math.max(d,a.min[k]-b.max[k],b.min[k]-a.max[k]);return Math.max(0,d)};
const all=new THREE.Box3();for(const c of Cube.all){if(c.name==='collision_proxy'||!c.mesh||!c.visibility)continue;all.union(new THREE.Box3().setFromObject(c.mesh))}
return {time:Timeline.time,selected:Animation.selected?.name,min:all.min.toArray(),max:all.max.toArray(),
staff_gap:gap(box('staff_socket'),own('right_hand')),hood_gap:gap(box('hood'),own('hi_head')),
staff_head_hits:(()=>{const under=(cu,n)=>{let g=cu.parent;while(g&&g!=='root'){if(g.name===n)return true;g=g.parent}return false};
const staff=Cube.all.filter(cu=>under(cu,'staff_socket')),head=Cube.all.filter(cu=>under(cu,'hi_head'));
for(const cu of [...staff,...head]){cu.mesh.updateWorldMatrix(true,true);cu.mesh.geometry.computeBoundingBox()}
let hits=0;for(const a of staff){const g=a.mesh.geometry.boundingBox;for(const b of head){const h=b.mesh.geometry.boundingBox.clone().expandByScalar(-.05);
let hit=false;for(let i=0;i<3&&!hit;i++)for(let j=0;j<3&&!hit;j++)for(let k=0;k<3&&!hit;k++){
const q=new THREE.Vector3(g.min.x+(g.max.x-g.min.x)*i/2,g.min.y+(g.max.y-g.min.y)*j/2,g.min.z+(g.max.z-g.min.z)*k/2).applyMatrix4(a.mesh.matrixWorld);
if(h.containsPoint(b.mesh.worldToLocal(q)))hit=true}if(hit)hits++}}return hits})()}})()"""

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
    call("create_offscreen_view",{"id":"mage_pose","width":1200,"height":1200,"copy_view":"none"})
    report={}
    try:
        call("set_camera_angle",{"view":"mage_pose","position":[70,50,-90],"target":[0,21,0],"projection":"orthographic","zoom":.6})
        for name,times in CAPTURES.items():
            report[name]=[]
            for time in times:
                call("animation_timeline",{"animation_id":name,"action":"set_time","time":time})
                state=data(call("risky_eval",{"code":PROBE}))
                if state.get("selected")!=name or abs(state.get("time",-1)-time)>1e-6: raise RuntimeError("Timeline did not select requested frame")
                report[name].append(state)
                if (name,time) in SHOTS:
                    image=next(b for b in call("capture_screenshot",{"view":"mage_pose","format":"png"})["content"] if b.get("type")=="image")
                    (OUT/(NAME+"-"+SHOTS[(name,time)]+".png")).write_bytes(base64.b64decode(image["data"]))
            print(json.dumps({"animation":name,"max_staff_gap":max(s["staff_gap"] for s in report[name]),
                              "max_hood_gap":max(s["hood_gap"] for s in report[name]),"max_top_px":max(s["max"][1] for s in report[name])}),flush=True)
        call("animation_timeline",{"animation_id":"idle","action":"stop"})
        (OUT/(NAME+"-pose-checks.json")).write_text(json.dumps(report,indent=2),encoding="utf-8",newline="\n")
        call("set_mode",{"mode_id":"edit"})
    finally:
        dispose_view(client,"mage_pose")

if __name__=="__main__": main()
