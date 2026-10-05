"""Create portal mage v2 through native Blockbench MCP, preserving earlier revisions.

Authored with the character's right on +X (the model faces north, -Z). The hood is a child
bone of hi_head and the staff sits in a socket bone under the right hand. Every pose must stay
under the 2.8-block (44.8 px) working clearance from the handoff ticket.
"""
import argparse
import base64
import json
from collections import defaultdict
from blockbench_mcp import Client
from build_moonfall_boss import OUT, data, embedded, dispose_view

NAME = "npc_portal_mage_v2"
# 0 robe teal, 1 dark cloth, 2 ivory, 3 gold, 4 bronze, 5 skin, 6 white hair, 7 deep teal,
# 8 boot black, 9 cyan, 10 portrait, 11 tabard motif, 12 robe back hem, 13 inner hood shadow, 14 staff shaft, 15 back emblem
COLORS = ["#134a55", "#1d2026", "#ece4d2", "#d1a542", "#8a6a30",
          "#e8cbb6", "#e9eef0", "#0e3640", "#1a1c20", "#36e2f0",
          "#e8cbb6", "#ece4d2", "#134a55", "#0b2a31", "#164c57", "#134a55"]
BONES = [("motion_root", [0,0,0], "root"), ("body", [0,12.5,0], "motion_root"),
         ("waist", [0,12.5,0], "body"), ("hi_head", [0,25.5,0], "body"),
         ("hood", [0,25.5,0], "hi_head")]
for side, sign in [("right",1),("left",-1)]:
    BONES += [(side+"_upper_arm", [5.8*sign,24.8,0], "body"),
              (side+"_forearm", [5.8*sign,18.5,0], side+"_upper_arm"),
              (side+"_hand", [5.8*sign,13.4,-.3], side+"_forearm"),
              (side+"_leg", [2*sign,12.5,0], "motion_root"),
              (side+"_robe", [2.4*sign,12.5,0], "waist")]
BONES += [("staff_socket", [5.8,12.35,-2.6], "right_hand"), ("staff_crystal", [5.8,37.6,-2.6], "staff_socket"),
          ("hitbox", [0,30,0], "root")]
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

def frame(bone, name, x0, y0, x1, y1, z0, z1, edge, color, rotation=None, origin=None):
    """Hollow square of four bars; rotated 45 degrees it becomes a diamond."""
    box(bone,name+"_top",[x0,y1-edge,z0],[x1,y1,z1],color,rotation,origin)
    box(bone,name+"_bottom",[x0,y0,z0],[x1,y0+edge,z1],color,rotation,origin)
    box(bone,name+"_left",[x0,y0+edge,z0],[x0+edge,y1-edge,z1],color,rotation,origin)
    box(bone,name+"_right",[x1-edge,y0+edge,z0],[x1,y1-edge,z1],color,rotation,origin)

# Teal outer robe over an ivory inner robe; gold-edged opening and a shoulder mantle.
box("body","outer_robe",[-4.25,12.5,-2.55],[4.25,25.5,2.55],0)
box("body","inner_robe_front",[-1.6,12.5,-2.7],[1.6,23.5,-2.55],2)
for sign in [-1,1]:
    e0,e1=sorted([1.6*sign,1.95*sign])
    box("body","robe_gold_edge_"+str(sign),[e0,12.5,-2.78],[e1,23.5,-2.5],3)
    m0,m1=sorted([1.2*sign,4.6*sign])
    box("body","mantle_side_"+str(sign),[m0,21.8,-2.85],[m1,25.7,1.2],0)
    box("body","mantle_trim_"+str(sign),[min(1.2*sign,4.7*sign),21.6,-2.95],[max(1.2*sign,4.7*sign),22.1,1.2],3)
    v0,v1=sorted([1.2*sign,1.55*sign])
    box("body","mantle_front_edge_"+str(sign),[v0,21.8,-2.97],[v1,25.7,-2.8],3)
box("body","mantle_back",[-4.6,21.8,1.2],[4.6,25.7,2.85],0)
box("body","mantle_back_trim",[-4.7,21.6,1.2],[4.7,22.1,2.95],3)
box("body","mantle_back_stripe",[-.35,22.1,2.85],[.35,25.7,2.97],3)
box("body","back_emblem",[-1.8,13.6,2.55],[1.8,21.5,2.72],15)

# Belt with a large gem buckle and an embroidered ivory tabard hanging to the shins.
box("waist","belt",[-4.45,13.2,-2.75],[4.45,14.8,2.75],1)
frame("waist","belt_buckle",-1.3,12.9,1.3,15.1,-3.0,-2.72,.45,3)
box("waist","belt_gem",[-.6,13.55,-3.1],[.6,14.45,-2.92],9)
box("waist","inner_robe_skirt",[-1.6,3.0,-2.6],[1.6,12.5,-2.3],2)
box("waist","tabard",[-1.3,4.0,-2.95],[1.3,13.2,-2.7],11)
for sign in [-1,1]:
    t0,t1=sorted([1.3*sign,1.6*sign])
    box("waist","tabard_gold_"+str(sign),[t0,4.0,-3.0],[t1,13.2,-2.65],3)
box("waist","tabard_point",[-.7,3.2,-2.95],[.7,4.05,-2.7],3)

for side,sign in [("right",1),("left",-1)]:
    m=lambda bone,name,a,b,color,angle=0,pivot=None: mirrored(side,sign,bone,name,a,b,color,angle,pivot)
    # Ivory bell sleeves with a teal mantle cap, gold trims and a shoulder gem.
    m("upper_arm","sleeve",[4.1,18,-1.8],[7.5,25,1.8],2)
    m("upper_arm","mantle_cap",[4.0,21.6,-2.0],[7.65,25.3,2.0],0)
    m("upper_arm","mantle_cap_trim",[3.95,21.4,-2.05],[7.7,21.9,2.05],3)
    m("upper_arm","shoulder_gem_frame",[7.65,22.6,-.7],[7.8,24.0,.7],3)
    m("upper_arm","shoulder_gem",[7.75,22.9,-.4],[7.92,23.7,.4],9)
    m("forearm","bell_sleeve",[3.9,14,-2.0],[7.7,18.5,2.0],2)
    m("forearm","cuff_band",[3.8,13.6,-2.1],[7.8,15.2,2.1],0)
    m("forearm","cuff_gold_top",[3.75,15.0,-2.15],[7.85,15.4,2.15],3)
    m("forearm","cuff_gold_bottom",[3.75,13.4,-2.15],[7.85,13.8,2.15],3)
    m("hand","palm",[4.5,11.2,-1.3],[7.1,13.5,1.3],5)
    for i in range(4):
        x=4.55+i*.62
        m("hand","finger_"+str(i),[x,10.7,-1.75],[x+.55,12.2,-1.25],5)
    m("hand","thumb",[4.1,11.6,-1.6],[4.65,12.8,-.4],5)
    # Split robe tails: teal front with gold edge, embroidered back hem, gold hem band.
    m("robe","front_panel",[1.6,1.6,-2.75],[4.6,12.5,-2.1],0)
    m("robe","front_gold_edge",[1.6,1.6,-2.9],[2.0,12.5,-2.6],3)
    m("robe","side_panel",[3.95,1.6,-2.1],[4.65,12.5,2.5],0)
    m("robe","back_panel",[.05,1.6,2.0],[4.65,12.5,2.75],12)
    m("robe","hem_gold",[.05,1.3,-2.85],[4.75,2.0,2.85],3)
    # Dark trousers and black boots with gold toe caps and cuff bands.
    m("leg","trousers",[.3,2,-1.7],[3.7,12.5,1.7],1)
    m("leg","boot",[.2,0,-1.9],[3.8,3.4,1.9],8)
    m("leg","boot_toe",[.2,0,-3.1],[3.8,1.8,-1.9],8)
    m("leg","toe_cap",[.2,.3,-3.3],[3.8,1.4,-3.05],3)
    m("leg","boot_band",[.1,3.0,-2.0],[3.9,3.5,2.0],3)
    m("leg","sole",[.1,0,-3.35],[3.9,.35,2.0],1)

# Elder face: portrait UV, long layered beard, swept moustache, bushy brows and temple hair.
box("hi_head","head_skin",[-3.8,25.5,-3.8],[3.8,33.3,3.4],10)
box("hi_head","nose",[-.5,27.8,-4.5],[.5,29.7,-3.75],5)
box("hi_head","beard_main",[-2.9,22.0,-4.4],[2.9,28.6,-3.4],6)
box("hi_head","beard_lower",[-1.8,18.6,-4.2],[1.8,22.2,-3.3],6)
box("hi_head","beard_tip",[-.9,17.2,-4.0],[.9,18.8,-3.3],6)
for sign in [-1,1]:
    box("hi_head","moustache_"+str(sign),[min(.1*sign,2.6*sign),27.4,-4.6],[max(.1*sign,2.6*sign),28.2,-4.2],6,[0,0,-22.5*sign],[sign*1.3,27.8,-4.4])
    s0,s1=sorted([3.0*sign,3.85*sign])
    box("hi_head","sideburn_"+str(sign),[s0,25.6,-3.95],[s1,29.5,-2.0],6)
    b0,b1=sorted([.8*sign,3.0*sign])
    box("hi_head","brow_"+str(sign),[b0,30.4,-4.15],[b1,31.1,-3.85],6)
    f0,f1=sorted([2.6*sign,3.8*sign])
    box("hi_head","temple_hair_"+str(sign),[f0,29.6,-4.0],[f1,32.6,-3.6],6)

# Hood (child of hi_head): pointed shell, back drape, gold face trim with brow gem, back stripe.
box("hood","hood_top",[-4.4,33.2,-4.3],[4.4,34.8,3.9],0)
box("hood","hood_back",[-4.4,25.2,3.4],[4.4,33.3,4.2],0)
box("hood","hood_peak",[-3.2,34.7,-2.5],[3.2,35.8,3.6],0)
box("hood","hood_point",[-2.0,35.7,-1.0],[2.0,36.8,3.4],0)
box("hood","hood_drape",[-3.6,21.8,2.6],[3.6,25.6,4.4],0)
for sign in [-1,1]:
    h0,h1=sorted([3.8*sign,4.5*sign])
    box("hood","hood_side_"+str(sign),[h0,25.2,-4.3],[h1,33.3,3.9],0)
    g0,g1=sorted([3.7*sign,4.55*sign])
    box("hood","hood_trim_side_"+str(sign),[g0,24.8,-4.6],[g1,32.6,-4.25],3)
    i0,i1=sorted([3.75*sign,3.82*sign])
    box("hood","hood_inner_shadow_"+str(sign),[i0,25.2,-4.25],[i1,32.6,2.0],13)
box("hood","hood_trim_top",[-4.55,32.5,-4.6],[4.55,33.4,-4.25],3)
box("hood","hood_gem_frame",[-.8,32.3,-4.78],[.8,33.9,-4.55],3)
box("hood","hood_gem",[-.5,32.6,-4.88],[.5,33.6,-4.7],9)
box("hood","hood_back_stripe",[-.35,25.2,4.2],[.35,33.3,4.35],3)
for name,y0,y1,z in [("top",33.3,34.8,3.9),("peak",34.8,35.8,3.6),("point",35.8,36.8,3.4)]:
    box("hood","hood_stripe_"+name,[-.35,y0,z],[.35,y1,z+.15],3)
box("hood","hood_back_gem",[-.5,29.4,4.3],[.5,30.4,4.45],9)

# Staff in the right-hand socket: banded shaft, collar, gold diamond cage and floating crystal.
box("staff_socket","staff_shaft",[5.45,.6,-.65],[6.15,34.0,.05],14)
for y in [10.5,14.6,26.0]: box("staff_socket","staff_band_"+str(y),[5.35,y,-.75],[6.25,y+.5,.15],3)
box("staff_socket","staff_ferrule",[5.35,.3,-.75],[6.25,1.3,.15],3)
box("staff_socket","staff_collar",[5.25,33.6,-.85],[6.35,34.6,.25],3)
frame("staff_socket","staff_cage",3.8,35.5,7.8,39.5,-.6,0.0,.5,3,[0,0,45],[5.8,37.5,-.3])
box("staff_crystal","crystal_core",[5.2,36.6,-.9],[6.4,38.6,.3],9,[0,45,0],[5.8,37.6,-.3])
box("staff_crystal","crystal_tip_top",[5.5,38.6,-.6],[6.1,39.3,0.0],9)
box("staff_crystal","crystal_tip_bottom",[5.5,35.9,-.6],[6.1,36.6,0.0],9)
# The staff stands in front of the sleeve, clasped by a closed right fist, never through the arm.
for bone,_,row in ROWS:
    if bone in ("staff_socket","staff_crystal"):
        row["from"]=[row["from"][0],row["from"][1],row["from"][2]-2.3]
        row["to"]=[row["to"][0],row["to"][1],row["to"][2]-2.3]
        if "origin" in row: row["origin"]=[row["origin"][0],row["origin"][1],row["origin"][2]-2.3]
box("right_hand","staff_grip",[4.55,11.0,-3.15],[7.05,12.6,-1.7],5)
box("hitbox","collision_proxy",[-4.6,0,-4.6],[4.6,36.8,4.6],8)

def rot(values): return [{"time":t,"rotation":v} for t,v in values]
def pos(values): return [{"time":t,"position":v} for t,v in values]
# Below shoulder height a positive Z rotation opens a +X arm outward (negative for -X).
idle={"body":pos([(0,[0,0,0]),(2,[0,.08,0]),(4,[0,0,0])]),
      "hi_head":rot([(0,[0,0,0]),(1.4,[1,2,0]),(2.8,[-1,-2,0]),(4,[0,0,0])]),
      "staff_crystal":pos([(0,[0,0,0]),(2,[0,.45,0]),(4,[0,0,0])])}
for side,sign in [("right",1),("left",-1)]:
    idle[side+"_robe"]=rot([(0,[0,0,0]),(2,[1,0,sign*.4]),(4,[0,0,0])])
greet={"hi_head":rot([(0,[0,0,0]),(.35,[-7,0,0]),(.8,[1,0,0]),(1.6,[0,0,0])]),
       "left_upper_arm":rot([(0,[0,0,0]),(.35,[70,0,-12]),(1.2,[70,0,-12]),(1.6,[0,0,0])]),
       "left_forearm":rot([(0,[0,0,0]),(.35,[30,0,0]),(1.2,[30,0,0]),(1.6,[0,0,0])]),
       "left_hand":rot([(0,[0,0,0]),(.4,[0,0,16]),(.6,[0,0,-16]),(.8,[0,0,16]),(1,[0,0,-16]),(1.2,[0,0,0]),(1.6,[0,0,0])])}
cast={"body":pos([(0,[0,0,0]),(.6,[0,.2,0]),(2.4,[0,.2,0]),(3,[0,0,0])]),
      "hi_head":rot([(0,[0,0,0]),(.6,[6,0,0]),(2.4,[6,0,0]),(3,[0,0,0])]),
      "right_upper_arm":rot([(0,[0,0,0]),(.6,[28,0,6]),(2.4,[28,0,6]),(3,[0,0,0])]),
      "right_forearm":rot([(0,[0,0,0]),(.6,[18,0,0]),(2.4,[18,0,0]),(3,[0,0,0])]),
      "left_upper_arm":rot([(0,[0,0,0]),(.6,[70,0,-8]),(2.4,[70,0,-8]),(3,[0,0,0])]),
      "left_forearm":rot([(0,[0,0,0]),(.6,[15,0,0]),(2.4,[15,0,0]),(3,[0,0,0])]),
      "staff_crystal":rot([(0,[0,0,0]),(.6,[0,0,0]),(1.5,[0,180,0]),(2.4,[0,0,0]),(3,[0,0,0])])}
portal={"hi_head":rot([(0,[0,0,0]),(.5,[8,0,0]),(1.9,[8,0,0]),(2.4,[0,0,0])]),
        "right_upper_arm":rot([(0,[0,0,0]),(.5,[12,0,32]),(1.9,[12,0,32]),(2.4,[0,0,0])]),
        # Counter-rotate the socket so the staff stays upright instead of swinging into the hood.
        "staff_socket":rot([(0,[0,0,0]),(.5,[0,0,-32]),(1.9,[0,0,-32]),(2.4,[0,0,0])]),
        "left_upper_arm":rot([(0,[0,0,0]),(.5,[12,0,-48]),(1.9,[12,0,-48]),(2.4,[0,0,0])]),
        "left_forearm":rot([(0,[0,0,0]),(.5,[20,0,0]),(1.9,[20,0,0]),(2.4,[0,0,0])]),
        "right_robe":rot([(0,[0,0,0]),(.5,[-4,0,3]),(1.9,[-4,0,3]),(2.4,[0,0,0])]),
        "left_robe":rot([(0,[0,0,0]),(.5,[-4,0,-3]),(1.9,[-4,0,-3]),(2.4,[0,0,0])]),
        "staff_crystal":pos([(0,[0,0,0]),(.5,[0,.6,0]),(1.9,[0,.6,0]),(2.4,[0,0,0])])}
ANIMATIONS=[("idle",4,True,idle),("greet",1.6,False,greet),
            ("cast",3,False,cast),("open_portal",2.4,False,portal)]

PAINT="""(()=>{const t=Texture.all[0];t.edit(canvas=>{
const c=canvas.getContext('2d'),colors=COLORS;let seed=91207;
const rand=()=>{seed=(seed*1664525+1013904223)>>>0;return seed/4294967296};
const rect=(x,y,w,h,col)=>{c.fillStyle=col;c.fillRect(x,y,w,h)};
colors.forEach((col,i)=>{const ox=(i%4)*64,oy=Math.floor(i/4)*64;
rect(ox,oy,64,64,col);
for(let y=2;y<62;y++)for(let x=2;x<62;x++){
 const a=[5,10].includes(i)?.025:.065;
 rect(ox+x,oy+y,1,1,rand()>.5?'rgba(255,255,255,'+a+')':'rgba(0,0,0,'+a+')');}
if([0,12,15].includes(i)){for(let y=5;y<60;y+=4)for(let x=5;x<60;x+=4)rect(ox+x,oy+y,1,1,'rgba(120,200,210,.10)');
 rect(ox+3,oy+3,1,58,'#2a6a76');rect(ox+60,oy+3,1,58,'#08262d');}
if(i===2||i===11){for(let x=6;x<60;x+=7){rect(ox+x,oy+3,1,57,'rgba(120,100,70,.10)');rect(ox+x+1,oy+3,1,57,'rgba(255,255,245,.18)');}}
if([3,4].includes(i)){const hi=i===3?'#f4d785':'#b68b4c',lo=i===3?'#8a6622':'#5c4220';
 rect(ox+2,oy+2,60,2,hi);rect(ox+2,oy+4,2,57,hi);rect(ox+60,oy+4,2,57,lo);rect(ox+4,oy+60,56,2,lo);}
if(i===6){for(let x=4;x<60;x+=3){rect(ox+x,oy+3,1,58,'#c4ccd0');rect(ox+x+1,oy+3,1,58,'#ffffff');}}
if(i===9){rect(ox+3,oy+3,58,4,'#c9fdff');rect(ox+3,oy+7,5,52,'#8af4ff');rect(ox+48,oy+9,12,50,'#15a4b5');}
if(i===10){rect(ox,oy,64,64,'#e8cbb6');rect(ox+5,oy+8,54,48,'#efd5c2');
 for(let x of [20,44]){rect(ox+x-5,oy+26,10,7,'#f6f2ea');rect(ox+x-2,oy+26,5,7,'#1fb6c8');rect(ox+x-1,oy+28,3,3,'#0b4f5a');
  rect(ox+x-1,oy+27,1,1,'#e6ffff');rect(ox+x-6,oy+25,12,1,'#8f7363');rect(ox+x-5,oy+33,10,1,'#c9a690');}
 for(let y of [14,18])rect(ox+12,oy+y,40,1,'rgba(140,100,80,.22)');}
if(i===11){const g='#d1a542';rect(ox+29,oy+6,6,28,g);rect(ox+24,oy+12,16,4,g);
 rect(ox+22,oy+40,20,4,g);rect(ox+26,oy+44,12,4,g);rect(ox+30,oy+48,4,6,g);rect(ox+30,oy+40,4,4,'#36e2f0');}
if(i===12){const g='#d1a542';rect(ox+3,oy+52,58,3,g);rect(ox+14,oy+40,8,8,g);rect(ox+16,oy+42,4,4,'#36e2f0');
 rect(ox+42,oy+40,8,8,g);rect(ox+44,oy+42,4,4,'#36e2f0');}
if(i===13){rect(ox+2,oy+2,60,60,'#0b2a31');}
if(i===14){for(let y=6;y<60;y+=10)rect(ox+3,oy+y,58,2,'#0e3640');rect(ox+3,oy+3,3,58,'#2a6f7c');}
if(i===15){const g='#d1a542';rect(ox+30,oy+3,4,58,g);
 for(let k=0;k<10;k++){rect(ox+32-k,oy+18+k,2,2,g);rect(ox+30+k,oy+18+k,2,2,g);rect(ox+22+k,oy+28+k,2,2,g);rect(ox+40-k,oy+28+k,2,2,g);}
 rect(ox+29,oy+26,6,6,'#36e2f0');}
});},{edit_name:'Mage robe hood staff portrait 256 atlas'});return {texture:t.name,size:[t.width,t.height]};})()"""

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
    call("risky_eval",{"code":"(()=>{Undo.initEdit({uv_mode:true});Project.texture_width=256;Project.texture_height=256;Undo.finishEdit('Mage UV resolution');return true})()"})
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
const d=cube.to.map((v,k)=>v-cube.from[k]),motif=[11,12,15].includes(i);
for(const [face,axis] of Object.entries({north:[0,1],south:[0,1],east:[2,1],west:[2,1],up:[0,2],down:[0,2]})){
const w=Math.max(1,Math.min(56,Math.round(d[axis[0]]*4))),h=Math.max(1,Math.min(56,Math.round(d[axis[1]]*4)));
cube.faces[face].uv=[ox+4,oy+4,ox+4+Math.min(w,motif?18:56),oy+4+Math.min(h,motif?18:56)];}
if(motif)for(const f of ['north','south'])cube.faces[f].uv=[ox+4,oy+4,ox+60,oy+60];
if(cube.name==='head_skin')cube.faces.north.uv=[132,132,188,188];
if(cube.name==='collision_proxy')cube.visibility=false;
cube.preview_controller.updateUV(cube);cube.preview_controller.updateVisibility(cube);}
const hit=Group.all.find(g=>g.name==='hitbox');hit.visibility=false;hit.preview_controller.updateVisibility(hit);
Undo.finishEdit('Mage proportional UV and dedicated portrait');return {cubes:elements.length,bones:Group.all.length}})()""".replace("MATERIALS",json.dumps(materials))
    call("risky_eval",{"code":uv})
    print(json.dumps({"geometry":summary}),flush=True)
    call("set_mode",{"mode_id":"animate"})
    for name,length,loop,bones in ANIMATIONS: call("create_animation",{"name":name,"animation_length":length,"loop":loop,"bones":bones})
    call("risky_eval",{"code":"(()=>{Undo.initEdit({animations:Animation.all.slice()});for(const a of Animation.all){a.name=a.name.replace(/^animation\\./,'');a.snapping=20;a.override=a.name!=='idle'}Undo.finishEdit('Mage state names 20 FPS');return Animation.all.map(a=>({name:a.name,length:a.length}))})()"})
    call("animation_timeline",{"animation_id":"idle","action":"stop"})
    call("set_mode",{"mode_id":"edit"})
    OUT.mkdir(parents=True,exist_ok=True)
    embedded(call("export_model",{"codec_id":"project","result_format":"embedded","max_content_length":2000000}),target)
    model=json.loads(target.read_text(encoding="utf-8"))
    (OUT/(NAME+".png")).write_bytes(base64.b64decode(model["textures"][0]["source"].split(",",1)[1]))
    call("create_offscreen_view",{"id":"mage_qa","width":1400,"height":1400,"copy_view":"none"})
    try:
        for view,camera in {"preview":[60,44,-90],"front":[0,20,-100],"right":[-100,20,0],"back":[0,20,100]}.items():
            result=call("set_camera_angle",{"view":"mage_qa","position":camera,"target":[0,20,0],"projection":"orthographic","zoom":.66})
            image=next(b for b in result["content"] if b.get("type")=="image")
            (OUT/(NAME+"-"+view+".png")).write_bytes(base64.b64decode(image["data"]))
    finally: dispose_view(client,"mage_qa")
    print(json.dumps({"exported":str(target),**summary}),flush=True)

if __name__=="__main__": main()
