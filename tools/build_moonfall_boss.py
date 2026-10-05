"""Build the Moonfall boss in a new, explicitly selected Blockbench MCP project.

Native tools make the geometry/animation; the .bbmodel is exported by Blockbench,
not fabricated offline. Existing projects and prior model revisions are preserved.
"""
import argparse
import base64
import json
import math
from collections import defaultdict
from pathlib import Path
from blockbench_mcp import Client

ROOT = Path(__file__).resolve().parents[1]
NAME = "boss_moonfall_guardian_v1"
OUT = ROOT / "output/lobby-concept/assets/models"
COLORS = ["#414858", "#777d8a", "#171c27", "#ab7c42",
          "#dfb475", "#25cbdc", "#c0f8ff", "#7140d2",
          "#b974ff", "#241436", "#c2bdb0", "#375e68",
          "#536376", "#463057", "#78552d", "#976dcd"]
BONES = [
    ("motion_root", [0,0,0], "root"),
    ("hips", [0,23,0], "motion_root"),
    ("body", [0,26,0], "hips"),
    ("hi_head", [0,44,0], "body"),
    ("core", [0,34,-8], "body"),
    ("backplate", [0,34,6], "body"),
]
for side, sign in [("right",-1),("left",1)]:
    BONES += [(side+"_upper_arm",[13*sign,40,0],"body"),
              (side+"_forearm",[18*sign,29,0],side+"_upper_arm"),
              (side+"_hand",[20*sign,19,0],side+"_forearm"),
              (side+"_thigh",[5.8*sign,23,0],"hips"),
              (side+"_shin",[5.8*sign,12,0],side+"_thigh"),
              (side+"_foot",[5.8*sign,4,0],side+"_shin")]
BONES += [("hitbox",[0,48,0],"root")]
ROWS = []

def box(bone,name,a,b,color,rotation=None,origin=None):
    row={"name":name,"from":a,"to":b}
    if rotation is not None: row["rotation"]=rotation
    if origin is not None: row["origin"]=origin
    ROWS.append((bone,color,row))

def mirror_box(side,sign,bone,name,a,b,color,rotation=None,origin=None):
    lo,hi=sorted([a[0]*sign,b[0]*sign])
    aa=[lo,a[1],a[2]]; bb=[hi,b[1],b[2]]
    if rotation is not None: rotation=[rotation[0],rotation[1]*sign,rotation[2]*sign]
    if origin is not None: origin=[origin[0]*sign,origin[1],origin[2]]
    box(side+"_"+bone,side+"_"+name,aa,bb,color,rotation,origin)

# Torso has a recessed center, rather than painting a core on a flat block.
box("hips","hip_joint",[-8,21,-4],[8,27,4],2)
box("hips","hip_belt",[-10,24,-5.3],[10,27,5.3],3)
box("hips","pelvis_front",[-7,20,-6],[7,24,-4],0)
box("hips","pelvis_rune",[-2,20.5,-6.3],[2,23.5,-6.05],11)
box("body","torso_backing",[-11,27,-3],[11,43,6],0)
box("body","left_chest_plate",[-11,28,-7],[-5.5,40,-3],12)
box("body","right_chest_plate",[5.5,28,-7],[11,40,-3],12)
box("body","chest_upper",[-10,40,-7],[10,43,-3],0)
box("body","chest_lower",[-9,25.5,-7],[9,28,-3],0)
box("body","neck_socket",[-5,42,-3],[5,45,3],2)
box("body","chest_recess",[-5.5,28.5,-7.3],[5.5,39.5,-5],9)
for sign in [-1,1]:
    box("body","collar_"+str(sign),[min(7*sign,10.5*sign),40,-7.5],[max(7*sign,10.5*sign),42.5,-7],3)
    box("body","torso_bolt_"+str(sign),[9*sign-.5,29.5,-7.6],[9*sign+.5,30.5,-7.05],4)
    box("body","waist_trim_"+str(sign),[min(4*sign,8*sign),25,-7.5],[max(4*sign,8*sign),26,-7],3)
# Eight separate bronze frame segments around the purple crystal.
for name,a,b in [
    ("ring_top",[-3,40,-9],[3,42,-6]),
    ("ring_bottom",[-3,26,-9],[3,28,-6]),
    ("ring_left",[-8,31,-9],[-6,37,-6]),
    ("ring_right",[6,31,-9],[8,37,-6])]:
    box("body",name,a,b,3)
for x,y,angle in [(-4.95,38.95,45),(4.95,38.95,-45),(-4.95,29.05,-45),(4.95,29.05,45)]:
    box("body","ring_diagonal_"+str(x)+"_"+str(y),[x-3,y-1,-9],[x+3,y+1,-6],4,[0,0,angle],[x,y,-7.5])
for x,y in [(0,41),(0,27),(-7,34),(7,34)]:
    box("body","ring_rivet_"+str(x)+"_"+str(y),[x-.6,y-.6,-9.4],[x+.6,y+.6,-9],1)
box("core","core_outer",[-3.7,30.3,-8.7],[3.7,37.7,-7.3],7,[0,0,45],[0,34,-8])
box("core","core_inner",[-2.35,31.65,-9.3],[2.35,36.35,-8.6],8,[0,0,45],[0,34,-8])
box("core","core_void",[-1.3,32.7,-9.65],[1.3,35.3,-9.25],9,[0,0,45],[0,34,-8])
for x,y in [(-3,34),(3,34),(0,31),(0,37)]:
    box("core","core_facet_"+str(x)+"_"+str(y),[x-.6,y-.6,-9.05],[x+.6,y+.6,-8.75],15)

# Head/crown: stone face, cyan eyes, recessed sockets and uneven crown stones.
box("hi_head","head_stone",[-6,44,-5],[6,52,5],0)
box("hi_head","face_mask",[-4.5,44.5,-5.7],[4.5,50.7,-5],1)
box("hi_head","brow",[-5.5,49.8,-6.1],[5.5,51.8,-5.3],0)
box("hi_head","nose",[-.8,46,-6.4],[.8,49,-5.5],12)
box("hi_head","chin",[-3.6,43.5,-5.9],[3.6,45.5,-5.1],12)
box("hi_head","mouth_rune",[-1.9,45.7,-5.99],[1.9,46.35,-5.75],5)
for sign in [-1,1]:
    box("hi_head","eye_socket_"+str(sign),[min(1.3*sign,4.3*sign),47.3,-6.2],[max(1.3*sign,4.3*sign),49.5,-5.5],2)
    box("hi_head","eye_"+str(sign),[min(1.7*sign,3.9*sign),47.7,-6.35],[max(1.7*sign,3.9*sign),48.8,-6.15],6)
    box("hi_head","temple_gold_"+str(sign),[min(5.8*sign,6.6*sign),45.5,-3],[max(5.8*sign,6.6*sign),50.5,1],3)
    box("hi_head","cheek_guard_"+str(sign),[min(4.4*sign,5.5*sign),44.5,-6],[max(4.4*sign,5.5*sign),48.5,-5.4],0)
    box("hi_head","crown_side_"+str(sign),[min(3*sign,6*sign),51.5,-4.8],[max(3*sign,6*sign),54,3.3],12)
box("hi_head","crown_center",[-1.6,51,-5.5],[1.6,54,-4],3)
box("hi_head","crown_rune",[-.55,50.2,-5.8],[.55,53.8,-5.5],6)
box("hi_head","crown_back",[-3,51.6,2.8],[3,53.2,5],0)
box("backplate","back_frame",[-6,27.8,6],[6,42.5,7.2],3)
box("backplate","back_moon_panel",[-4.9,29,7.21],[4.9,41.5,7.7],13)

for side,sign in [("right",-1),("left",1)]:
    mb=lambda bone,name,a,b,col,rot=None,pivot=None: mirror_box(side,sign,bone,name,a,b,col,rot,pivot)
    mb("upper_arm","shoulder_joint",[10.5,36,-3],[16,41,3],2)
    mb("upper_arm","pauldron",[13,35,-6],[24,44,6],0,[0,0,-22.5],[15,40,0])
    mb("upper_arm","pauldron_top",[13,42.5,-6.5],[24,44.5,6.5],3,[0,0,-22.5],[15,40,0])
    mb("upper_arm","pauldron_border",[23,35,-6.5],[25,43,6.5],3,[0,0,-22.5],[15,40,0])
    mb("upper_arm","shoulder_face",[13.5,36,-6.7],[23,43,-6.2],12,[0,0,-22.5],[15,40,0])
    mb("upper_arm","shoulder_rune_vertical",[18,37,-7.15],[19.1,41.7,-6.7],5,[0,0,-22.5],[15,40,0])
    mb("upper_arm","shoulder_rune_cross",[16.5,38.8,-7.2],[20.8,39.9,-6.8],6,[0,0,-22.5],[15,40,0])
    mb("upper_arm","upper_arm_stone",[14,29.5,-4],[20.5,37,4],12)
    mb("upper_arm","upper_arm_trim",[13.6,29,-4.5],[21,30.4,4.5],3)
    mb("forearm","elbow_joint",[15.5,27,-3.3],[21.5,30,3.3],2)
    mb("forearm","forearm_guard",[16,19,-5],[24,28,5],0)
    mb("forearm","forearm_trim_top",[15.7,26.8,-5.5],[24.3,28.3,5.5],3)
    mb("forearm","forearm_trim_bottom",[15.7,18.8,-5.5],[24.3,20.2,5.5],3)
    mb("forearm","forearm_plate",[16.4,20.5,-5.7],[23.6,26.5,-5],12)
    mb("forearm","forearm_rune_vertical",[19.1,21,-6.05],[20.2,26,-5.7],5)
    mb("forearm","forearm_rune_cross",[17.5,23,-6.1],[21.8,24.1,-5.8],6)
    mb("forearm","forearm_rivet_a",[16.8,21,-6],[17.8,22,-5.7],4)
    mb("forearm","forearm_rivet_b",[22.2,25,-6],[23.2,26,-5.7],4)
    mb("hand","wrist",[17.5,17.5,-3],[22.5,20,3],2)
    mb("hand","palm",[17,13.5,-4.2],[24,18.7,4],0)
    mb("hand","hand_knuckle_plate",[16.8,16.5,-5],[24.3,18.8,-4.2],12)
    for finger in range(3):
        x=17+finger*2.4
        mb("hand","finger_"+str(finger),[x,12,-4.9],[x+2.1,16.5,-2.5],1)
        mb("hand","finger_joint_"+str(finger),[x+.15,13.3,-5.15],[x+1.95,14,-4.9],2)
    mb("hand","thumb",[15.4,13.8,-2],[17.3,17,1.5],12)
    mb("thigh","thigh_socket",[2.5,20,-3.2],[9,24,3.2],2)
    mb("thigh","thigh_stone",[2.6,12.8,-4],[9.2,22,4],0)
    mb("thigh","thigh_front_plate",[3,14,-4.65],[8.8,21,-4],12)
    mb("thigh","thigh_side_rune",[8.9,16,-3],[9.35,19.5,-1.8],5)
    mb("shin","knee_joint",[3.1,10.7,-3.3],[8.7,13.3,3.3],2)
    mb("shin","shin_stone",[2.8,4.3,-4.2],[9.1,11.8,4.2],0)
    mb("shin","kneecap",[2.5,9.4,-5],[9.4,13.3,-3.6],3)
    mb("shin","knee_face",[3.3,9.9,-5.4],[8.6,12.8,-5],12)
    mb("shin","knee_rune_vertical",[5.4,9.7,-5.8],[6.5,13,-5.4],5)
    mb("shin","knee_rune_cross",[4,10.8,-5.9],[7.9,11.9,-5.5],6)
    mb("shin","shin_front",[3.5,4.5,-4.8],[8.3,9.4,-4.2],1)
    mb("foot","ankle_joint",[3.8,3,-3],[8,5,3],2)
    mb("foot","boot",[2.1,0,-7],[9.9,4.3,4.6],0)
    mb("foot","toe_guard",[1.9,0,-7.5],[10.1,2.5,-6.6],3)
    mb("foot","boot_front",[2.8,.4,-7.8],[9.2,3,-7.5],12)
    mb("foot","boot_rune",[5.4,.5,-8],[6.5,2.9,-7.8],5)

box("hitbox","collision_proxy",[-12,0,-12],[12,54,12],2)

def keys(channel,points):
    return [{"time":t,channel:value} for t,value in points]
def rot(points): return keys("rotation",points)
def pos(points): return keys("position",points)
def scale(points): return keys("scale",points)

idle={
    "body":pos([(0,[0,0,0]),(2,[0,.3,0]),(4,[0,0,0])]),
    "hi_head":rot([(0,[0,-2,0]),(2,[0,2,0]),(4,[0,-2,0])]),
    "core":scale([(0,[1,1,1]),(1,[1.05,1.05,1.05]),(2,[1,1,1]),(3,[.97,.97,.97]),(4,[1,1,1])]),
}
walk={"hips":pos([(0,[0,.4,0]),(.4,[0,.8,0]),(.8,[0,.4,0]),(1.2,[0,.8,0]),(1.6,[0,.4,0])]),
      "body":rot([(0,[0,-3,0]),(.8,[0,3,0]),(1.6,[0,-3,0])])}
for side,sign in [("right",-1),("left",1)]:
    idle[side+"_upper_arm"]=rot([(0,[0,0,0]),(2,[1.5,0,sign*1]),(4,[0,0,0])])
    walk[side+"_thigh"]=rot([(0,[sign*18,0,0]),(.8,[-sign*18,0,0]),(1.6,[sign*18,0,0])])
    walk[side+"_shin"]=rot([(0,[sign*4,0,0]),(.8,[-sign*4,0,0]),(1.6,[sign*4,0,0])])
    walk[side+"_foot"]=rot([(0,[-sign*22,0,0]),(.8,[sign*22,0,0]),(1.6,[-sign*22,0,0])])
    walk[side+"_upper_arm"]=rot([(0,[-sign*12,0,0]),(.8,[sign*12,0,0]),(1.6,[-sign*12,0,0])])
spawn={"body":pos([(0,[0,-3,0]),(.8,[0,-1,0]),(2,[0,0,0])]),
       "hi_head":rot([(0,[20,0,0]),(1,[5,0,0]),(2,[0,0,0])]),
       "core":scale([(0,[.25,.25,.25]),(.8,[1.12,1.12,1.12]),(2,[1,1,1])])}
attack={"body":rot([(0,[0,0,0]),(.25,[0,-12,0]),(.45,[-8,12,0]),(.9,[0,0,0])]),
        "right_upper_arm":rot([(0,[0,0,0]),(.25,[100,-10,-12]),(.45,[40,5,0]),(.9,[0,0,0])]),
        "right_forearm":rot([(0,[0,0,0]),(.25,[35,0,0]),(.45,[-5,0,0]),(.9,[0,0,0])])}
slam={"body":rot([(0,[0,0,0]),(.4,[6,0,0]),(1.1,[8,0,0]),(1.25,[-30,0,0]),(1.55,[-20,0,0]),(2.2,[0,0,0])])
      +pos([(0,[0,0,0]),(1.1,[0,0,0]),(1.25,[0,-5,-1]),(1.55,[0,-3,-1]),(2.2,[0,0,0])]),
      "core":scale([(0,[1,1,1]),(.6,[1.13,1.13,1.13]),(1.1,[1.13,1.13,1.13]),(1.25,[.92,.92,.92]),(2.2,[1,1,1])])}
for side,sign in [("right",-1),("left",1)]:
    slam[side+"_upper_arm"]=rot([(0,[0,0,0]),(.5,[145,0,sign*8]),(1.1,[155,0,sign*8]),(1.25,[25,0,0]),(1.55,[10,0,0]),(2.2,[0,0,0])])
    slam[side+"_forearm"]=rot([(0,[0,0,0]),(.5,[25,0,0]),(1.1,[30,0,0]),(1.25,[0,0,0]),(2.2,[0,0,0])])
    slam[side+"_hand"]=rot([(0,[0,0,0]),(.5,[15,0,0]),(1.25,[0,0,0]),(2.2,[0,0,0])])
enrage={"body":rot([(0,[0,0,0]),(.5,[-10,0,0]),(1.1,[2,0,0]),(1.6,[0,0,0])]),
        "core":scale([(0,[1,1,1]),(.5,[1.18,1.18,1.18]),(1,[1.12,1.12,1.12]),(1.6,[1,1,1])])}
for side,sign in [("right",-1),("left",1)]:
    enrage[side+"_upper_arm"]=rot([(0,[0,0,0]),(.5,[45,0,sign*25]),(1.1,[15,0,sign*10]),(1.6,[0,0,0])])
hurt={"body":rot([(0,[0,0,0]),(.15,[-5,0,-3]),(.5,[0,0,0])]),
      "hi_head":rot([(0,[0,0,0]),(.15,[-8,0,0]),(.5,[0,0,0])])}
death={"body":rot([(0,[0,0,0]),(.8,[-30,0,0]),(2,[-55,0,0])])
       +pos([(0,[0,0,0]),(.8,[0,-6,0]),(2,[0,-10,0])]),
       "hi_head":rot([(0,[0,0,0]),(.8,[-15,0,0]),(2,[-25,0,0])]),
       "core":scale([(0,[1,1,1]),(.8,[.6,.6,.6]),(2,[.05,.05,.05])])}
ANIMATIONS=[("idle",4,True,idle),("walk",1.6,True,walk),("spawn",2,False,spawn),
            ("attack",.9,False,attack),("slam",2.2,False,slam),
            ("enrage",1.6,False,enrage),("hurt",.5,False,hurt),("death",2,False,death)]

# Procedural painting is performed in Blockbench's native texture canvas.
PAINT = """(()=>{
const t=Texture.all[0];
t.edit(canvas=>{
 const c=canvas.getContext('2d'),colors=COLORS;
 let seed=71337;
 const rand=()=>{seed=(seed*1664525+1013904223)>>>0;return seed/4294967296};
 colors.forEach((color,i)=>{
  const ox=(i%4)*32,oy=Math.floor(i/4)*32;
  c.fillStyle=color;c.fillRect(ox,oy,32,32);
  for(let y=0;y<32;y++)for(let x=0;x<32;x++){
    const alpha=rand()*.16;
    c.fillStyle=rand()>.5?'rgba(255,255,255,'+alpha+')':'rgba(0,0,0,'+alpha+')';c.fillRect(ox+x,oy+y,1,1);
  }
  if([0,1,12].includes(i)){
    c.fillStyle='rgba(0,0,0,.4)';
    c.fillRect(ox,oy+15,32,1);c.fillRect(ox+10,oy,1,16);c.fillRect(ox+23,oy+16,1,16);
    c.fillStyle='rgba(255,255,255,.11)';c.fillRect(ox,oy+16,32,1);
    for(let j=0;j<3;j++){let x=3+j*9,y=4+j*5;c.fillStyle='rgba(12,15,23,.35)';
      for(let k=0;k<6;k++){c.fillRect(ox+x,oy+y+k,1,2);x+=(k%2?1:-1);}}
  }
  if([3,4,14].includes(i)){
    c.fillStyle='rgba(255,238,189,.32)';c.fillRect(ox+1,oy+1,30,2);c.fillRect(ox+1,oy+3,2,28);
    c.fillStyle='rgba(49,27,13,.36)';c.fillRect(ox+29,oy+3,2,28);c.fillRect(ox+3,oy+29,28,2);
    for(let j=0;j<18;j++){c.fillStyle='rgba(72,38,17,.25)';c.fillRect(ox+Math.floor(rand()*30),oy+Math.floor(rand()*30),2,1);}
  }
  if([7,8,15].includes(i)){
    for(let y=0;y<32;y+=4){c.fillStyle=y%8?'rgba(237,183,255,.21)':'rgba(28,8,74,.2)';c.fillRect(ox+y/2,oy+y,24-y/2,2);}
  }
  if(i===11){
    c.fillStyle=colors[6];c.fillRect(ox+14,oy+4,4,24);c.fillRect(ox+5,oy+14,22,4);
    c.fillStyle=colors[5];c.fillRect(ox+9,oy+9,14,2);c.fillRect(ox+9,oy+21,14,2);
  }
  if(i===13){
    c.fillStyle=colors[4];c.fillRect(ox+4,oy+1,24,2);c.fillRect(ox+4,oy+29,24,2);
    c.beginPath();c.arc(ox+16,oy+16,9,0,Math.PI*2);c.fill();
    c.fillStyle=colors[13];c.beginPath();c.arc(ox+20,oy+13,8,0,Math.PI*2);c.fill();
    c.fillStyle=colors[4];c.fillRect(ox+15,oy+3,2,5);c.fillRect(ox+13,oy+5,6,1);
    c.fillRect(ox+15,oy+25,2,5);c.fillRect(ox+13,oy+27,6,1);
  }
 });
},{edit_name:'Moonfall stone bronze rune atlas'});
return {texture:t.name,size:[t.width,t.height]};
})()"""

def data(result):
    if result.get("structuredContent"): return result["structuredContent"]
    for block in result.get("content",[]):
        if block.get("type")=="text":
            try: return json.loads(block["text"])
            except ValueError: continue
    return {}

def embedded(result,path):
    for block in result.get("content",[]):
        if block.get("type")=="resource":
            resource=block["resource"]
            if "text" in resource: path.write_text(resource["text"],encoding="utf-8"); return
            if "blob" in resource: path.write_bytes(base64.b64decode(resource["blob"])); return
    raise RuntimeError("Blockbench did not return a complete export")

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("--project-uuid")
    parser.add_argument("--dry-run",action="store_true")
    args=parser.parse_args()
    assert len(ROWS)<=240 and len(BONES)<=24,(len(ROWS),len(BONES))
    assert len({r[2]["name"] for r in ROWS})==len(ROWS)
    assert len({b[0] for b in BONES})==len(BONES)
    summary={"asset":NAME,"cubes":len(ROWS),"bones":len(BONES),"animations":[a[0] for a in ANIMATIONS],"atlas":[128,128]}
    if args.dry_run: print(json.dumps(summary)); return
    if not args.project_uuid: parser.error("--project-uuid is required")
    target=OUT/(NAME+".bbmodel")
    if target.exists(): raise RuntimeError("Revision already exported; create a new revision instead of overwriting")
    client=Client()
    def guard():
        info=data(client.call("get_project_info",{}))
        project=info.get("project",{})
        if project.get("uuid")!=args.project_uuid or project.get("name")!=NAME: raise RuntimeError("Active project changed; stop without modifying it")
        return info
    def call(name,arguments):
        guard(); return client.call(name,arguments)
    first=guard()
    if any(first.get("counts",{}).get(key,0) for key in ["cubes","meshes","groups","textures"]): raise RuntimeError("Boss project is not empty")
    call("set_mode",{"mode_id":"edit"})
    call("create_texture",{"name":NAME+".png","width":128,"height":128,"uv_width":128,"uv_height":128,"fill_color":COLORS[0]})
    call("risky_eval",{"code":"(()=>{Undo.initEdit({uv_mode:true});Project.texture_width=128;Project.texture_height=128;Undo.finishEdit('Moonfall UV resolution');return {width:Project.texture_width,height:Project.texture_height}})()"})
    call("risky_eval",{"code":PAINT.replace("COLORS",json.dumps(COLORS))})
    for name,pivot,parent in BONES: call("add_group",{"name":name,"origin":pivot,"parent":parent})
    batches=defaultdict(list)
    for bone,color,row in ROWS: batches[(bone,color)].append(row)
    for (bone,color),rows in batches.items():
        ox=(color%4)*32; oy=(color//4)*32
        call("place_cube",{"elements":rows,"group":bone,"texture":NAME+".png",
                          "faces":[{"face":face,"uv":[ox+2,oy+2,ox+30,oy+30]} for face in ["north","south","east","west","up","down"]]})
    colors={row["name"]:color for _,color,row in ROWS}
    uv_code="""(()=>{
const materials=MATERIALS,elements=Cube.all.slice();
Undo.initEdit({elements,uv_only:true,outliner:true});
for(const cube of elements){
 const i=materials[cube.name],ox=(i%4)*32,oy=Math.floor(i/4)*32;
 const d=cube.to.map((v,k)=>v-cube.from[k]);
 for(const [face,axis] of Object.entries({north:[0,1],south:[0,1],east:[2,1],west:[2,1],up:[0,2],down:[0,2]})){
   let w=Math.max(1,Math.min(28,Math.round(d[axis[0]]*1.8))),h=Math.max(1,Math.min(28,Math.round(d[axis[1]]*1.8)));
   if(i===13||i===11){w=28;h=28}
   cube.faces[face].uv=[ox+2,oy+2,ox+2+w,oy+2+h];
 }
 if(cube.name==='collision_proxy'){cube.visibility=false}
 cube.preview_controller.updateUV(cube);cube.preview_controller.updateVisibility(cube);
}
const hit=Group.all.find(g=>g.name==='hitbox');hit.visibility=false;hit.preview_controller.updateVisibility(hit);
Undo.finishEdit('Moonfall per-face atlas UV and hidden hitbox');return {cubes:elements.length,bones:Group.all.length};
})()""".replace("MATERIALS",json.dumps(colors))
    call("risky_eval",{"code":uv_code})
    print(json.dumps({"geometry":summary}),flush=True)
    call("set_mode",{"mode_id":"animate"})
    for name,length,loop,bones in ANIMATIONS:
        call("create_animation",{"name":name,"animation_length":length,"loop":loop,"bones":bones})
    call("risky_eval",{"code":"(()=>{Undo.initEdit({animations:Animation.all.slice()});for(const a of Animation.all){a.name=a.name.replace(/^animation\\./,'');a.snapping=20;a.override=!['idle','walk'].includes(a.name);if(a.name==='death')a.loop='hold'}Undo.finishEdit('Moonfall native state names and 20 FPS');return Animation.all.map(a=>({name:a.name,length:a.length,loop:a.loop}))})()"})
    call("list_export_formats",{"only_current_format":True})
    OUT.mkdir(parents=True,exist_ok=True)
    embedded(call("export_model",{"codec_id":"project","result_format":"embedded","max_content_length":2000000}),target)
    model=json.loads(target.read_text(encoding="utf-8"))
    texture=model["textures"][0]["source"]
    (OUT/(NAME+".png")).write_bytes(base64.b64decode(texture.split(",",1)[1]))
    call("animation_timeline",{"animation_id":"idle","action":"stop"})
    call("set_mode",{"mode_id":"edit"})
    call("create_offscreen_view",{"id":"moonfall_qa","width":1400,"height":1400,"copy_view":"none"})
    cameras={
        "preview":([88,62,-120],[0,27,0]),
        "front":([0,27,-150],[0,27,0]),
        "right":([-150,27,0],[0,27,0]),
        "back":([0,27,150],[0,27,0]),
    }
    for view,(position,look) in cameras.items():
        result=call("set_camera_angle",{"view":"moonfall_qa","position":position,"target":look,"projection":"orthographic","zoom":.48,"max_size":1400})
        image=next(b for b in result["content"] if b.get("type")=="image")
        (OUT/(NAME+"-"+view+".png")).write_bytes(base64.b64decode(image["data"]))
    dispose_view(client,"moonfall_qa")
    print(json.dumps({"exported":str(target),"cubes":len(model["elements"]),"animations":len(model.get("animations",[]))}),flush=True)

def dispose_view(client,view):
    try:
        client.call("delete_offscreen_view",{"view":view})
    except RuntimeError as error:
        # MCP 1.10.0 may throw after removing the view; verify it really is gone.
        remaining=data(client.call("list_views",{})).get("views",[])
        if "onContextMenu is not defined" not in str(error) or any(v["id"]==view for v in remaining):
            raise
        print(json.dumps({"cleanup_warning":"MCP 1.10.0 context-menu error after verified viewport disposal"}),flush=True)

if __name__=="__main__": main()
