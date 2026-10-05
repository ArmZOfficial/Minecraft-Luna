"""Create quest warden v2 through native Blockbench MCP, preserving earlier revisions.

Authored directly with the character's right on +X (the model faces north, -Z).
"""
import argparse
import base64
import json
from collections import defaultdict
from blockbench_mcp import Client
from build_moonfall_boss import OUT, data, embedded, dispose_view

NAME = "npc_quest_warden_v2"
# 0 coat teal, 1 under-tunic, 2 ivory fur, 3 gold, 4 bronze, 5 hand skin, 6 hair, 7 leather,
# 8 sole, 9 cyan, 10 portrait, 11 coat panel motifs, 12 boot leather, 13 paper, 14 light teal, 15 back emblem
COLORS = ["#1f4d5e", "#2d2622", "#efe6d2", "#d4a646", "#8f6a33",
          "#f2c9a8", "#e6dccb", "#4e3426", "#2a221d", "#3ee4f0",
          "#f2c9a8", "#1f4d5e", "#3e2a20", "#f3ead6", "#2f8f95", "#1f4d5e"]
BONES = [("motion_root", [0,0,0], "root"), ("body", [0,12,0], "motion_root"),
         ("waist", [0,12,0], "body"), ("hi_head", [0,24,0], "body")]
for side, sign in [("right",1),("left",-1)]:
    BONES += [(side+"_upper_arm", [5.7*sign,23.5,0], "body"),
              (side+"_forearm", [5.7*sign,18,0], side+"_upper_arm"),
              (side+"_hand", [5.7*sign,13,-.3], side+"_forearm"),
              (side+"_leg", [2*sign,12,0], "motion_root"),
              (side+"_coattail", [2.4*sign,12.2,0], "waist")]
BONES += [("scroll", [-5.7,11.85,-1.3], "left_hand"), ("compass", [4.2,12.2,-2.7], "waist"),
          ("hitbox", [0,28,0], "root")]
ROWS = []

def box(bone, name, a, b, color, rotation=None, origin=None):
    row={"name":name,"from":a,"to":b}
    if rotation is not None: row["rotation"]=rotation
    if origin is not None: row["origin"]=origin
    ROWS.append((bone,color,row))

def mirrored(side, sign, bone, name, a, b, color, angle=0, pivot=None):
    lo,hi=sorted([a[0]*sign,b[0]*sign])
    rotation=[0,0,angle*sign] if angle else None
    origin=[pivot[0]*sign,pivot[1],pivot[2]] if pivot else None
    box(side+"_"+bone,side+"_"+name,[lo,a[1],a[2]],[hi,b[1],b[2]],color,rotation,origin)

def frame(bone, name, x0, y0, x1, y1, z0, z1, edge, color):
    """Hollow square of four bars, used for buckles and gem settings."""
    box(bone,name+"_top",[x0,y1-edge,z0],[x1,y1,z1],color)
    box(bone,name+"_bottom",[x0,y0,z0],[x1,y0+edge,z1],color)
    box(bone,name+"_left",[x0,y0+edge,z0],[x0+edge,y1-edge,z1],color)
    box(bone,name+"_right",[x1-edge,y0+edge,z0],[x1,y1-edge,z1],color)

# Slim long coat: dark under-tunic in the opening, ivory fur lapels and a high fur collar.
box("body","coat_torso",[-4.1,12,-2.4],[4.1,24,2.4],0)
box("body","under_tunic",[-1.15,12.4,-2.55],[1.15,23.2,-2.35],1)
box("body","back_emblem_panel",[-1.7,15.2,2.4],[1.7,23.4,2.55],15)
box("body","collar_back",[-3.3,23.2,1.3],[3.3,25.4,2.75],2)
for sign in [-1,1]:
    x0,x1=sorted([1.1*sign,2.35*sign])
    box("body","fur_lapel_"+str(sign),[x0,12.5,-2.75],[x1,24.2,-2.35],2)
    c0,c1=sorted([2.3*sign,3.35*sign])
    box("body","collar_side_"+str(sign),[c0,23.2,-2.7],[c1,25.2,1.3],2)
    s0,s1=sorted([3.9*sign,4.15*sign])
    box("body","side_seam_"+str(sign),[s0,12.5,-1.2],[s1,23.5,1.2],14)
box("body","brooch_frame",[-.85,21.95,-3.0],[.85,23.65,-2.6],3,[0,0,45],[0,22.8,-2.8])
box("body","brooch_gem",[-.45,22.35,-3.15],[.45,23.25,-2.95],9,[0,0,45],[0,22.8,-3.05])
for y in [16.2,18.6]: box("body","tunic_clasp_"+str(y),[-.3,y,-2.7],[.3,y+.5,-2.5],3)

# Belt with framed buckle; pouch on the left hip, compass chain on the right hip.
box("waist","belt",[-4.4,11.6,-2.6],[4.4,13.0,2.6],7)
frame("waist","belt_buckle",-.95,11.4,.95,13.2,-2.95,-2.6,.35,3)
box("waist","belt_buckle_pin",[-.12,11.75,-2.9],[.12,12.85,-2.7],4)
box("waist","pouch",[-5.1,9.6,-2.0],[-4.1,12.6,.7],7)
box("waist","pouch_flap",[-5.2,11.7,-2.15],[-4.0,12.75,.85],4)
box("waist","pouch_clasp",[-5.3,11.85,-.95],[-5.15,12.35,-.45],3)
box("compass","compass_chain",[4.1,10.9,-2.95],[4.3,12.2,-2.75],3)
box("compass","compass_bail",[3.9,10.5,-2.98],[4.5,11.0,-2.72],3)
box("compass","compass_case",[3.4,8.8,-3.05],[5.0,10.5,-2.65],3)
box("compass","compass_face",[3.7,9.1,-3.15],[4.7,10.2,-2.95],1)
box("compass","compass_needle_v",[4.12,9.2,-3.22],[4.28,10.1,-3.1],9)
box("compass","compass_needle_h",[3.8,9.57,-3.22],[4.6,9.73,-3.1],9)

for side,sign in [("right",1),("left",-1)]:
    m=lambda bone,name,a,b,color,angle=0,pivot=None: mirrored(side,sign,bone,name,a,b,color,angle,pivot)
    # Slim sleeves end in deep ivory cuffs with a gem clasp on the outer side.
    m("upper_arm","sleeve",[4.0,17.5,-1.7],[7.4,24,1.7],0)
    m("upper_arm","fur_shoulder",[3.85,23.4,-1.85],[7.55,24.35,1.85],2)
    m("forearm","sleeve_lower",[4.1,15,-1.6],[7.3,18,1.6],0)
    m("forearm","fur_cuff",[3.9,13.8,-1.9],[7.5,16.6,1.9],2)
    m("forearm","cuff_gold_line",[3.85,16.3,-1.95],[7.55,16.65,1.95],3)
    m("forearm","cuff_clasp_top",[7.5,15.7,-.7],[7.7,16.0,.7],3)
    m("forearm","cuff_clasp_bottom",[7.5,14.3,-.7],[7.7,14.6,.7],3)
    m("forearm","cuff_clasp_front",[7.5,14.6,-.7],[7.7,15.7,-.4],3)
    m("forearm","cuff_clasp_back",[7.5,14.6,.4],[7.7,15.7,.7],3)
    m("forearm","cuff_gem",[7.55,14.65,-.38],[7.8,15.65,.38],9)
    m("forearm","under_sleeve",[4.3,12.6,-1.45],[7.1,13.9,1.45],1)
    m("hand","palm",[4.45,10.9,-1.3],[6.95,12.9,1.3],5)
    for i in range(4):
        x=4.5+i*.6
        m("hand","finger_"+str(i),[x,10.4,-1.75],[x+.52,11.9,-1.25],5)
    m("hand","thumb",[4.05,11.3,-1.6],[4.6,12.5,-.4],5)
    # Coat skirt splits into left/right tails, each with fur front edge and gold-hem motif panels.
    m("coattail","front_panel",[1.0,2.5,-2.6],[4.5,12.2,-1.9],11)
    m("coattail","fur_front_edge",[.9,2.4,-2.8],[1.9,12.2,-2.5],2)
    m("coattail","side_panel",[3.9,2.5,-1.9],[4.55,12.2,2.4],0)
    m("coattail","back_panel",[.12,2.5,1.9],[4.55,12.2,2.6],11)
    m("coattail","fur_hem",[.1,2.15,-2.7],[4.65,2.95,2.7],2)
    m("coattail","gold_hem_line",[.1,2.95,-2.72],[4.65,3.25,2.72],3)
    # Tall boots with a gold cuff band and a framed cyan gem on the shin.
    m("leg","trousers",[.3,6,-1.7],[3.7,12,1.7],1)
    m("leg","boot_shaft",[.2,.8,-1.9],[3.8,6.6,1.9],12)
    m("leg","boot_cuff_gold",[.1,6.0,-2.0],[3.9,6.5,2.0],3)
    m("leg","boot_toe",[.2,.4,-3.0],[3.8,2.2,-1.9],12)
    m("leg","sole",[.1,0,-3.1],[3.9,.5,2.0],8)
    m("leg","shin_gem_frame_top",[1.25,5.75,-2.08],[2.75,6.0,-1.9],3)
    m("leg","shin_gem_frame_bottom",[1.25,4.55,-2.08],[2.75,4.8,-1.9],3)
    m("leg","shin_gem",[1.55,4.8,-2.12],[2.45,5.75,-1.92],9)

# Elf head: portrait UV, pointed ears through split side hair, layered bangs and side locks.
box("hi_head","head_skin",[-3.8,24,-3.8],[3.8,31.4,3.4],10)
box("hi_head","nose",[-.35,26.3,-4.15],[.35,27.3,-3.75],5)
box("hi_head","hair_top",[-4.2,30.4,-4.2],[4.2,33.0,3.9],6)
# Uneven crown and flicked ends break the boxy silhouette into a fluffy cut.
box("hi_head","hair_crown_left",[-3.6,32.9,-3.2],[.4,33.6,2.2],6)
box("hi_head","hair_crown_right",[.2,32.9,-2.4],[3.4,33.35,2.9],6)
for sign in [-1,1]:
    f0,f1=sorted([3.9*sign,4.6*sign])
    box("hi_head","hair_flick_"+str(sign),[f0,24.6,-3.6],[f1,25.8,-2.2],6)
box("hi_head","hair_back_flick_left",[-3.6,23.6,3.1],[-1.0,24.1,4.2],6)
box("hi_head","hair_back_flick_right",[.6,23.75,3.1],[3.4,24.1,4.2],6)
box("hi_head","hair_back",[-4.2,24,3.0],[4.2,31,4.35],6)
for x0,x1,y0 in [(-3.9,-1.7,28.9),(-1.9,.3,28.4),(.1,2.1,29.1),(1.9,3.9,28.6)]:
    box("hi_head","bang_"+str(x0),[x0,y0,-4.45],[x1,30.6,-3.8],6)
for sign in [-1,1]:
    x=sign*4.0
    box("hi_head","hair_side_front_"+str(sign),[x-.4,25.6,-3.9],[x+.4,30.6,-.75],6)
    box("hi_head","hair_side_back_"+str(sign),[x-.4,24.8,.75],[x+.4,30.6,3.9],6)
    l0,l1=sorted([2.95*sign,4.1*sign])
    box("hi_head","face_lock_"+str(sign),[l0,24.9,-4.3],[l1,29.0,-3.6],6)
    e0,e1=sorted([3.7*sign,4.75*sign])
    box("hi_head","ear_"+str(sign),[e0,26.9,-.55],[e1,28.5,.55],5)
    t0,t1=sorted([4.5*sign,5.9*sign])
    box("hi_head","ear_tip_"+str(sign),[t0,27.9,-.4],[t1,28.65,.4],5,[0,0,22.5*sign],[4.6*sign,28.2,0])

# Sealed scroll: paper roll through the left fist, gold caps, teal ribbon with a gem seal.
box("scroll","scroll_roll",[-6.3,11.25,-4.4],[-5.1,12.45,1.8],13)
for name,z0,z1 in [("front",-4.75,-4.35),("back",1.75,2.15)]:
    box("scroll","scroll_cap_"+name,[-6.45,11.1,z0],[-4.95,12.6,z1],3)
box("scroll","scroll_ribbon",[-6.4,11.15,-3.0],[-5.0,12.55,-2.55],15)
box("scroll","scroll_ribbon_tail",[-5.95,9.9,-2.95],[-5.45,11.15,-2.6],15)
box("scroll","scroll_seal",[-6.6,11.45,-3.05],[-6.38,12.25,-2.5],3)
box("scroll","scroll_seal_gem",[-6.7,11.65,-2.95],[-6.55,12.05,-2.6],9)
box("hitbox","collision_proxy",[-4.4,0,-4.4],[4.4,33.6,4.4],8)

def rot(values): return [{"time":t,"rotation":v} for t,v in values]
def pos(values): return [{"time":t,"position":v} for t,v in values]
idle={"body":pos([(0,[0,0,0]),(2,[0,.08,0]),(4,[0,0,0])]),
      "hi_head":rot([(0,[0,0,0]),(1.2,[1,-3,0]),(2.8,[-1,3,0]),(4,[0,0,0])])}
for side,sign in [("right",1),("left",-1)]:
    # Below shoulder height a positive Z rotation opens a +X arm outward; above it the sign flips.
    idle[side+"_upper_arm"]=rot([(0,[0,0,0]),(2,[1,0,sign*.6]),(4,[0,0,0])])
    idle[side+"_coattail"]=rot([(0,[0,0,0]),(2,[1.2,0,sign*.4]),(4,[0,0,0])])
greet={"hi_head":rot([(0,[0,0,0]),(.35,[-6,0,0]),(.8,[1,0,0]),(1.6,[0,0,0])]),
       "right_upper_arm":rot([(0,[0,0,0]),(.35,[70,0,12]),(1.2,[70,0,12]),(1.6,[0,0,0])]),
       "right_forearm":rot([(0,[0,0,0]),(.35,[30,0,0]),(1.2,[30,0,0]),(1.6,[0,0,0])]),
       "right_hand":rot([(0,[0,0,0]),(.4,[0,0,-16]),(.6,[0,0,16]),(.8,[0,0,-16]),(1,[0,0,16]),(1.2,[0,0,0]),(1.6,[0,0,0])])}
# The scroll turns across the body (local Y 90) so it is presented horizontally.
offer={"hi_head":rot([(0,[0,0,0]),(.5,[-10,0,0]),(2.2,[-10,0,0]),(2.8,[0,0,0])]),
       "left_upper_arm":rot([(0,[0,0,0]),(.5,[55,0,-6]),(2.2,[55,0,-6]),(2.8,[0,0,0])]),
       "left_forearm":rot([(0,[0,0,0]),(.5,[30,0,0]),(2.2,[30,0,0]),(2.8,[0,0,0])]),
       "scroll":rot([(0,[0,0,0]),(.5,[0,90,0]),(2.2,[0,90,0]),(2.8,[0,0,0])]),
       "right_upper_arm":rot([(0,[0,0,0]),(.5,[8,0,0]),(2.2,[8,0,0]),(2.8,[0,0,0])])}
point={"hi_head":rot([(0,[0,0,0]),(.45,[0,-25,0]),(1.9,[0,-25,0]),(2.4,[0,0,0])]),
       "right_upper_arm":rot([(0,[0,0,0]),(.45,[82,0,28]),(1.9,[82,0,28]),(2.4,[0,0,0])]),
       "right_forearm":rot([(0,[0,0,0]),(.45,[6,0,0]),(1.9,[6,0,0]),(2.4,[0,0,0])]),
       "left_upper_arm":rot([(0,[0,0,0]),(.45,[-6,0,-4]),(1.9,[-6,0,-4]),(2.4,[0,0,0])])}
ANIMATIONS=[("idle",4,True,idle),("greet",1.6,False,greet),
            ("offer_scroll",2.8,False,offer),("point_direction",2.4,False,point)]

PAINT="""(()=>{const t=Texture.all[0];t.edit(canvas=>{
const c=canvas.getContext('2d'),colors=COLORS;let seed=40613;
const rand=()=>{seed=(seed*1664525+1013904223)>>>0;return seed/4294967296};
const rect=(x,y,w,h,col)=>{c.fillStyle=col;c.fillRect(x,y,w,h)};
colors.forEach((col,i)=>{const ox=(i%4)*64,oy=Math.floor(i/4)*64;
rect(ox,oy,64,64,col);
for(let y=2;y<62;y++)for(let x=2;x<62;x++){
 const a=[5,10,13].includes(i)?.025:.065;
 rect(ox+x,oy+y,1,1,rand()>.5?'rgba(255,255,255,'+a+')':'rgba(0,0,0,'+a+')');}
if([0,11,15].includes(i)){for(let y=5;y<60;y+=4)for(let x=5;x<60;x+=4)rect(ox+x,oy+y,1,1,'rgba(150,210,220,.10)');
 rect(ox+3,oy+3,1,58,'#3f7488');rect(ox+60,oy+3,1,58,'#123544');}
if(i===2){for(let j=0;j<160;j++){const x=4+Math.floor(rand()*56),y=4+Math.floor(rand()*54);rect(ox+x,oy+y,1,3,rand()>.5?'#fffaf0':'#cfc3a8');}}
if([3,4].includes(i)){const hi=i===3?'#f7d98a':'#b68b4c',lo=i===3?'#8f6a22':'#5c4220';
 rect(ox+2,oy+2,60,2,hi);rect(ox+2,oy+4,2,57,hi);rect(ox+60,oy+4,2,57,lo);rect(ox+4,oy+60,56,2,lo);}
if(i===6){for(let x=4;x<60;x+=3){rect(ox+x,oy+3,1,58,'#c9bca5');rect(ox+x+1,oy+3,1,58,'#fbf5e9');}
 for(let j=0;j<12;j++)rect(ox+4+Math.floor(rand()*54),oy+4+Math.floor(rand()*50),2,6,'#d8ccb6');}
if(i===7||i===12){for(let y=5;y<60;y+=3){rect(ox+4,oy+y,1,1,'#8a6448');rect(ox+59,oy+y,1,1,'#8a6448');}}
if(i===9){rect(ox+3,oy+3,58,4,'#c4fdff');rect(ox+3,oy+7,5,52,'#86f3ff');rect(ox+48,oy+9,12,50,'#1aa6b8');}
if(i===10){rect(ox,oy,64,64,'#f2c9a8');rect(ox+5,oy+10,54,46,'#f7d3b5');
 rect(ox+8,oy+44,7,3,'#f0a99a');rect(ox+49,oy+44,7,3,'#f0a99a');
 for(let x of [20,44]){rect(ox+x-6,oy+30,12,11,'#ffffff');rect(ox+x-4,oy+30,8,11,'#1aa9c4');
  rect(ox+x-3,oy+33,6,6,'#0d5e70');rect(ox+x-4,oy+38,8,3,'#53e6f4');rect(ox+x-3,oy+31,2,2,'#e9ffff');
  rect(ox+x-7,oy+29,14,2,'#4a3a33');rect(ox+x-5,oy+25,10,2,'#d9c9b0');}
 rect(ox+30,oy+44,4,2,'#e3ad8f');rect(ox+28,oy+51,8,1,'#c97a72');}
if(i===11){const g='#d9ad4c';rect(ox+3,oy+52,58,2,g);
 for(const cx of [16,40]){rect(ox+cx-2,oy+38,6,12,g);rect(ox+cx-6,oy+42,14,4,g);rect(ox+cx,oy+40,2,8,'#a57d2c');}
 rect(ox+28,oy+30,6,6,g);}
if(i===13){for(let y=6;y<60;y+=5)rect(ox+5,oy+y,54,1,'rgba(150,130,100,.25)');}
if(i===15){const g='#d9ad4c';rect(ox+28,oy+6,8,8,g);rect(ox+30,oy+8,4,4,'#3ee4f0');
 rect(ox+30,oy+14,4,8,g);rect(ox+22,oy+22,20,4,g);rect(ox+22,oy+38,20,4,g);rect(ox+22,oy+26,4,12,g);rect(ox+38,oy+26,4,12,g);
 rect(ox+29,oy+29,6,6,'#3ee4f0');rect(ox+30,oy+42,4,12,g);}
});},{edit_name:'Warden coat fur hair portrait scroll 256 atlas'});return {texture:t.name,size:[t.width,t.height]};})()"""

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("--project-uuid")
    parser.add_argument("--dry-run",action="store_true")
    args=parser.parse_args()
    assert len({r[2]["name"] for r in ROWS})==len(ROWS)
    assert all(all(a<b for a,b in zip(row["from"],row["to"])) for _,_,row in ROWS)
    assert {bone for bone,_,_ in ROWS}<={b[0] for b in BONES}
    summary={"asset":NAME,"cubes":len(ROWS),"bones":len(BONES),"atlas":[256,256],"animations":[a[0] for a in ANIMATIONS]}
    if args.dry_run: print(json.dumps(summary)); return
    if not args.project_uuid: parser.error("--project-uuid is required")
    target=OUT/(NAME+".bbmodel")
    if target.exists(): raise RuntimeError("Revision exists; do not overwrite earlier work")
    client=Client()
    def guard():
        info=data(client.call("get_project_info",{})); project=info["project"]
        if project["uuid"]!=args.project_uuid or project["name"]!=NAME: raise RuntimeError("Active project changed")
        return info
    def call(name,arguments): guard(); return client.call(name,arguments)
    if any(guard()["counts"].get(key,0) for key in ["cubes","meshes","groups","textures"]): raise RuntimeError("Project must be empty")
    call("set_mode",{"mode_id":"edit"})
    call("create_texture",{"name":NAME+".png","width":256,"height":256,"uv_width":256,"uv_height":256,"fill_color":COLORS[0]})
    call("risky_eval",{"code":"(()=>{Undo.initEdit({uv_mode:true});Project.texture_width=256;Project.texture_height=256;Undo.finishEdit('Warden UV resolution');return true})()"})
    call("risky_eval",{"code":PAINT.replace("COLORS",json.dumps(COLORS))})
    for name,pivot,parent in BONES: call("add_group",{"name":name,"origin":pivot,"parent":parent})
    batches=defaultdict(list)
    for bone,color,row in ROWS: batches[(bone,color)].append(row)
    for (bone,color),rows in batches.items():
        ox=(color%4)*64;oy=(color//4)*64
        call("place_cube",{"elements":rows,"group":bone,"texture":NAME+".png",
                          "faces":[{"face":face,"uv":[ox+4,oy+4,ox+60,oy+60]} for face in ["north","south","east","west","up","down"]]})
    materials={row["name"]:color for _,color,row in ROWS}
    uv="""(()=>{const materials=MATERIALS,elements=Cube.all.slice();Undo.initEdit({elements,uv_only:true,outliner:true});
for(const cube of elements){const i=materials[cube.name],ox=(i%4)*64,oy=Math.floor(i/4)*64;
const d=cube.to.map((v,k)=>v-cube.from[k]);
for(const [face,axis] of Object.entries({north:[0,1],south:[0,1],east:[2,1],west:[2,1],up:[0,2],down:[0,2]})){
const w=Math.max(1,Math.min(56,Math.round(d[axis[0]]*4))),h=Math.max(1,Math.min(56,Math.round(d[axis[1]]*4)));
cube.faces[face].uv=[ox+4,oy+4,ox+4+Math.min(w,[11,15].includes(i)?20:56),oy+4+Math.min(h,[11,15].includes(i)?20:56)];}
if([11,15].includes(i))for(const f of ['north','south'])cube.faces[f].uv=[ox+4,oy+4,ox+60,oy+60];
if(cube.name==='head_skin')cube.faces.north.uv=[132,132,188,188];
if(cube.name==='collision_proxy')cube.visibility=false;
cube.preview_controller.updateUV(cube);cube.preview_controller.updateVisibility(cube);}
const hit=Group.all.find(g=>g.name==='hitbox');hit.visibility=false;hit.preview_controller.updateVisibility(hit);
Undo.finishEdit('Warden proportional UV and dedicated portrait');return {cubes:elements.length,bones:Group.all.length}})()""".replace("MATERIALS",json.dumps(materials))
    call("risky_eval",{"code":uv})
    print(json.dumps({"geometry":summary}),flush=True)
    call("set_mode",{"mode_id":"animate"})
    for name,length,loop,bones in ANIMATIONS: call("create_animation",{"name":name,"animation_length":length,"loop":loop,"bones":bones})
    call("risky_eval",{"code":"(()=>{Undo.initEdit({animations:Animation.all.slice()});for(const a of Animation.all){a.name=a.name.replace(/^animation\\./,'');a.snapping=20;a.override=a.name!=='idle'}Undo.finishEdit('Warden state names 20 FPS');return Animation.all.map(a=>({name:a.name,length:a.length}))})()"})
    call("animation_timeline",{"animation_id":"idle","action":"stop"})
    call("set_mode",{"mode_id":"edit"})
    OUT.mkdir(parents=True,exist_ok=True)
    embedded(call("export_model",{"codec_id":"project","result_format":"embedded","max_content_length":2000000}),target)
    model=json.loads(target.read_text(encoding="utf-8"))
    (OUT/(NAME+".png")).write_bytes(base64.b64decode(model["textures"][0]["source"].split(",",1)[1]))
    call("create_offscreen_view",{"id":"warden_qa","width":1400,"height":1400,"copy_view":"none"})
    try:
        for view,camera in {"preview":[60,40,-90],"front":[0,16.5,-100],"right":[-100,16.5,0],"back":[0,16.5,100]}.items():
            result=call("set_camera_angle",{"view":"warden_qa","position":camera,"target":[0,16.8,0],"projection":"orthographic","zoom":.78})
            image=next(b for b in result["content"] if b.get("type")=="image")
            (OUT/(NAME+"-"+view+".png")).write_bytes(base64.b64decode(image["data"]))
    finally: dispose_view(client,"warden_qa")
    print(json.dumps({"exported":str(target),**summary}),flush=True)

if __name__=="__main__": main()
