"""Sample every 20 FPS frame of the crystal guide in native Blockbench and capture the held poses.

Checks per frame: the staff stays in the right fist and clear of the head, the map stays pinched in
the left fist, nothing dips below the floor; records where the face points and how wide the map opens.
"""
import argparse
import base64
import json
from blockbench_mcp import Client
from build_moonfall_boss import OUT, data, dispose_view
from build_crystal_guide import NAME, ANIMATIONS

SHOTS={("idle",2):"idle",("greet",.85):"greet",("point_direction",1.2):"point-direction",
       ("show_map",1.4):"show-map",("welcome",1.0):"welcome"}
PROBE="""(()=>{const box=n=>{const b=new THREE.Box3();for(const c of Cube.all){if(!c.mesh||!c.visibility)continue;
let g=c.parent;while(g&&g!=='root'&&g.name!==n)g=g.parent;if(!g||g==='root')continue;
c.mesh.updateWorldMatrix(true,true);b.union(new THREE.Box3().setFromObject(c.mesh))}return b};
const own=n=>{const b=new THREE.Box3();for(const c of Cube.all){if(!c.mesh||c.parent?.name!==n)continue;
c.mesh.updateWorldMatrix(true,true);b.union(new THREE.Box3().setFromObject(c.mesh))}return b};
const cube=n=>{const c=Cube.all.find(c=>c.name===n);c.mesh.updateWorldMatrix(true,true);return new THREE.Box3().setFromObject(c.mesh)};
const ctr=b=>b.getCenter(new THREE.Vector3()).toArray().map(v=>+v.toFixed(3));
const gap=(a,b)=>{let d=0;for(const k of ['x','y','z'])d=Math.max(d,a.min[k]-b.max[k],b.min[k]-a.max[k]);return +Math.max(0,d).toFixed(4)};
const all=new THREE.Box3();for(const c of Cube.all){if(c.name==='collision_proxy'||!c.mesh||!c.visibility)continue;
c.mesh.updateWorldMatrix(true,true);all.union(new THREE.Box3().setFromObject(c.mesh))}
const head=cube('head'),nose=cube('nose'),map=box('map'),leaf=cube('map_leaf'),flap=cube('map_flap_leaf');
const hc=head.getCenter(new THREE.Vector3()),nc=nose.getCenter(new THREE.Vector3()),f=nc.sub(hc).normalize();
const pts=c=>{c.mesh.updateWorldMatrix(true,true);const bb=c.mesh.geometry.boundingBox||(c.mesh.geometry.computeBoundingBox(),c.mesh.geometry.boundingBox);
const o=[];for(const i of [0,.5,1])for(const j of [0,.5,1])for(const k of [0,.5,1])o.push(new THREE.Vector3(bb.min.x+(bb.max.x-bb.min.x)*i,bb.min.y+(bb.max.y-bb.min.y)*j,bb.min.z+(bb.max.z-bb.min.z)*k).applyMatrix4(c.mesh.matrixWorld));return o};
const under=(c,n)=>{let g=c.parent;while(g&&g!=='root'&&g.name!==n)g=g.parent;return g&&g!=='root'};
const hits=(a,b,skip=[])=>{let h=0;const A=Cube.all.filter(c=>c.mesh&&c.visibility&&under(c,a)),B=Cube.all.filter(c=>c.mesh&&c.visibility&&c.name!=='collision_proxy'&&under(c,b)&&!under(c,a)&&!skip.some(s=>under(c,s)));
for(const q of B){q.mesh.updateWorldMatrix(true,true);const inv=q.mesh.matrixWorld.clone().invert();const bb=q.mesh.geometry.boundingBox||(q.mesh.geometry.computeBoundingBox(),q.mesh.geometry.boundingBox);
for(const c of A)for(const p of pts(c)){const l=p.clone().applyMatrix4(inv);if(l.x>bb.min.x+1e-3&&l.x<bb.max.x-1e-3&&l.y>bb.min.y+1e-3&&l.y<bb.max.y-1e-3&&l.z>bb.min.z+1e-3&&l.z<bb.max.z-1e-3)h++}}return h};
const size=b=>b.getSize(new THREE.Vector3()).toArray().map(v=>+v.toFixed(3));
return {time:Timeline.time,selected:Animation.selected?.name,min:all.min.toArray(),max:all.max.toArray(),
staff_gap:gap(cube('grip_wrap'),own('right_hand')),map_gap:gap(cube('map_leaf_edge'),own('left_hand')),
staff_head_gap:gap(box('staff'),own('hi_head')),staff_head_hits:hits('staff','hi_head'),map_body_hits:hits('map','motion_root',['left_hand']),
staff_body_hits:hits('staff','motion_root',['right_hand','hi_head']),
face_dir:f.toArray().map(v=>+v.toFixed(3)),left_hand:ctr(own('left_hand')),crystal:ctr(cube('crystal_body')),
map_size:size(map),map_center:ctr(map),leaf_flap_gap:gap(leaf,flap),
staff_min_y:+box('staff').min.y.toFixed(3)}})()"""

def main():
    parser=argparse.ArgumentParser();parser.add_argument("--project-uuid",required=True);args=parser.parse_args()
    client=Client()
    def call(name,arguments):
        p=data(client.call("get_project_info",{}))["project"]
        if p["uuid"]!=args.project_uuid or p["name"]!=NAME:raise RuntimeError("Active project changed")
        return client.call(name,arguments)
    call("set_mode",{"mode_id":"animate"})
    call("create_offscreen_view",{"id":"guide_pose","width":1400,"height":1400,"copy_view":"none"})
    report={}
    try:
        call("set_camera_angle",{"view":"guide_pose","position":[60,40,-90],"target":[0,18,0],"projection":"orthographic","zoom":.7})
        for name,length,_,_ in ANIMATIONS:
            report[name]=[]
            for tick in range(round(length*20)+1):
                time=round(tick/20,3)
                call("animation_timeline",{"animation_id":name,"action":"set_time","time":time})
                state=data(call("risky_eval",{"code":PROBE}))
                if state.get("selected")!=name or abs(state.get("time",-1)-time)>1e-6:raise RuntimeError("Timeline did not select requested frame")
                report[name].append(state)
                if (name,time) in SHOTS:
                    r=call("capture_screenshot",{"view":"guide_pose","format":"png"})
                    im=next(b for b in r["content"] if b.get("type")=="image")
                    (OUT/(NAME+"-"+SHOTS[(name,time)]+".png")).write_bytes(base64.b64decode(im["data"]))
            s=report[name]
            print(json.dumps({"animation":name,"frames":len(s),"max_staff_gap":max(x["staff_gap"] for x in s),
                              "max_map_gap":max(x["map_gap"] for x in s),"staff_head_hits":sum(x["staff_head_hits"] for x in s),"map_body_hits":sum(x["map_body_hits"] for x in s),"staff_body_hits":sum(x["staff_body_hits"] for x in s),
                              "min_y":min(x["min"][1] for x in s),"max_y":max(x["max"][1] for x in s)}),flush=True)
        call("animation_timeline",{"animation_id":"idle","action":"stop"})
        (OUT/(NAME+"-pose-checks.json")).write_text(json.dumps(report,indent=2)+"\n",encoding="utf-8",newline="\n")
        call("set_mode",{"mode_id":"edit"})
    finally:dispose_view(client,"guide_pose")

if __name__=="__main__":main()
