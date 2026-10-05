"""Capture actual Blockbench animation poses without changing model geometry."""
import argparse
import base64
import json
from pathlib import Path
from blockbench_mcp import Client
from build_moonfall_boss import NAME, OUT, data, dispose_view

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("--project-uuid",required=True)
    args=parser.parse_args()
    client=Client()
    def call(name,arguments):
        project=data(client.call("get_project_info",{}))["project"]
        if project["uuid"]!=args.project_uuid or project["name"]!=NAME: raise RuntimeError("Active project changed")
        return client.call(name,arguments)
    call("set_mode",{"mode_id":"edit"})
    call("create_offscreen_view",{"id":"moonfall_review","width":1400,"height":1400,"copy_view":"none"})
    try:
        for view,position in {"preview":[88,62,-120],"front":[0,27,-150],"right":[-150,27,0],"back":[0,27,150]}.items():
            result=call("set_camera_angle",{"view":"moonfall_review","position":position,"target":[0,27,0],"projection":"orthographic","zoom":.48})
            image=next(b for b in result["content"] if b.get("type")=="image")
            (OUT/(NAME+"-"+view+".png")).write_bytes(base64.b64decode(image["data"]))
        call("set_mode",{"mode_id":"animate"})
        report={}
        captures={"idle":[0,2,4],"walk":[0,.4,.8,1.6],"spawn":[0,1,2],"attack":[0,.25,.45,.9],
                  "slam":[0,.5,1.1,1.25,1.55,2.2],"enrage":[0,.5,1.6],"hurt":[0,.15,.5],"death":[0,.8,2]}
        selected={("slam",1.1):"slam-windup",("slam",1.25):"slam-impact",("attack",.45):"attack",
                  ("enrage",.5):"enrage",("walk",.4):"walk",("spawn",0):"spawn",("death",2):"death"}
        call("set_camera_angle",{"view":"moonfall_review","position":[100,75,-135],"target":[0,34,0],"projection":"orthographic","zoom":.38})
        for name,times in captures.items():
            report[name]=[]
            for time in times:
                call("animation_timeline",{"animation_id":name,"action":"set_time","time":time})
                bounds=data(call("risky_eval",{"code":"(()=>{const b=new THREE.Box3();for(const c of Cube.all){if(c.name==='collision_proxy'||!c.visibility||!c.mesh)continue;c.mesh.updateWorldMatrix(true,true);b.union(new THREE.Box3().setFromObject(c.mesh))}return {time:Timeline.time,selected:Animation.selected?.name,min:b.min.toArray(),max:b.max.toArray()}})()"}))
                if bounds.get("selected")!=name or abs(bounds.get("time",-1)-time)>1e-6: raise RuntimeError("Native timeline did not select requested frame")
                report[name].append(bounds)
                if (name,time) in selected:
                    result=call("capture_screenshot",{"view":"moonfall_review","format":"png"})
                    image=next(b for b in result["content"] if b.get("type")=="image")
                    (OUT/(NAME+"-"+selected[(name,time)]+".png")).write_bytes(base64.b64decode(image["data"]))
            print(json.dumps({"animation":name,"frames_checked":len(times)}),flush=True)
        call("animation_timeline",{"animation_id":"idle","action":"stop"})
        (OUT/(NAME+"-pose-checks.json")).write_text(json.dumps(report,indent=2),encoding="utf-8",newline="\n")
        call("set_mode",{"mode_id":"edit"})
    finally:
        dispose_view(client,"moonfall_review")
    print(json.dumps({"pose_report":str(OUT/(NAME+"-pose-checks.json"))}),flush=True)

if __name__=="__main__": main()
