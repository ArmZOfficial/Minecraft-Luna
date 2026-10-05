"""Create rune smith v2 through native Blockbench MCP, preserving earlier revisions."""
import argparse
import base64
import json
from collections import defaultdict
from blockbench_mcp import Client
from build_moonfall_boss import OUT, data, embedded, dispose_view
from build_luma_props import load_atlas
from npc_bake import bake_npc, uv_js, portrait, panel, embroidery, ledger_cover, coin_face, key_emblem, rune_apron, coat_motif, warden_emblem, tabard, robe_hem, mage_emblem

NAME = "npc_rune_smith_v2"
# 0 tunic, 1 trousers, 2 skin, 3 gold, 4 bronze, 5 hand skin, 6 hair, 7 leather,
# 8 iron, 9 cyan, 10 portrait, 11 apron, 12 steel, 13 ivory plate, 14 wood, 15 strap teal
COLORS = ["#2b2d33", "#24262b", "#d39a74", "#d4b044", "#8f6a33",
          "#c98d68", "#5a3826", "#5b3b28", "#26282c", "#39d7e9",
          "#d39a74", "#1d5b5e", "#8c939b", "#e3ddcf", "#6b4429", "#164a4d"]
BONES = [("motion_root", [0,0,0], "root"), ("body", [0,13,0], "motion_root"),
         ("waist", [0,13,0], "body"), ("apron_flap", [0,13.2,-3.1], "waist"),
         ("hi_head", [0,26,0], "body")]
for side, sign in [("right",-1),("left",1)]:
    BONES += [(side+"_upper_arm", [6.8*sign,25,0], "body"),
              (side+"_forearm", [6.8*sign,19.5,0], side+"_upper_arm"),
              (side+"_hand", [6.8*sign,15.5,-.3], side+"_forearm"),
              (side+"_leg", [2.3*sign,13,0], "motion_root")]
BONES += [("hammer", [-6.8,14.3,-.3], "right_hand"), ("tongs", [5.3,12.4,-1.25], "waist"),
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

def frame(bone, name, x0, y0, x1, y1, z0, z1, edge, color):
    """Hollow square of four bars, used for buckles and goggle rims."""
    box(bone,name+"_top",[x0,y1-edge,z0],[x1,y1,z1],color)
    box(bone,name+"_bottom",[x0,y0,z0],[x1,y0+edge,z1],color)
    box(bone,name+"_left",[x0,y0+edge,z0],[x0+edge,y1-edge,z1],color)
    box(bone,name+"_right",[x1-edge,y0+edge,z0],[x1,y1-edge,z1],color)

# Sleeveless forge tunic with a leather bib and straps that cross on the back.
box("body","tunic_torso",[-4.8,13,-2.6],[4.8,26,2.6],0)
box("body","tunic_collar",[-2.2,25.4,-2.75],[2.2,26.05,2.75],8)
box("body","apron_bib",[-3.4,17.4,-2.95],[3.4,24.2,-2.6],11)
box("body","bib_gold_edge",[-3.5,24.0,-3.05],[3.5,24.35,-2.55],3)
for sign in [-1,1]:
    x0,x1=sorted([2.55*sign,3.45*sign])
    box("body","front_strap_"+str(sign),[x0,24.2,-2.95],[x1,26.1,-2.6],15)
    box("body","shoulder_strap_"+str(sign),[x0,25.75,-2.95],[x1,26.15,2.95],15)
    b0,b1=sorted([2.4*sign,3.6*sign])
    frame("body","strap_buckle_"+str(sign),b0,23.0,b1,24.15,-3.15,-2.9,.3,3)
    box("body","back_strap_"+str(sign),[-.4,14.6,2.6],[.4,25.8,2.85],15,[0,0,22.5*sign],[0,20.2,2.7])
frame("body","back_buckle",-.75,19.45,.75,20.95,2.85,3.1,.3,3)
box("body","back_buckle_tongue",[-.12,19.75,2.95],[.12,20.65,3.15],4)
for y in [18.4,21.2]:
    for sign in [-1,1]: box("body","bib_rivet_"+str(sign)+"_"+str(y),[sign*2.8-.2,y,-3.08],[sign*2.8+.2,y+.4,-2.9],4)

# Wide belt, square buckle, pouches, back apron and the notched lower apron.
box("waist","tunic_skirt",[-4.9,8,-2.75],[4.9,12.7,2.75],0)
box("waist","work_belt",[-5,12.6,-2.95],[5,14.1,2.95],7)
frame("waist","belt_buckle",-1.1,12.4,1.1,14.3,-3.5,-2.9,.38,3)
box("waist","belt_buckle_pin",[-.12,12.75,-3.45],[.12,13.95,-3.0],4)
for x in [-3.6,-2.2,2.2,3.6]: box("waist","belt_rivet_"+str(x),[x-.17,13.15,-3.08],[x+.17,13.5,-2.92],3)
box("waist","pouch_right",[-5.75,10.2,-2.3],[-4.6,13.3,.7],7)
box("waist","pouch_right_flap",[-5.85,12.3,-2.45],[-4.5,13.45,.85],4)
box("waist","pouch_right_stud",[-5.95,12.5,-.95],[-5.8,12.95,-.5],3)
box("waist","pouch_right_gem",[-5.98,11.1,-1.0],[-5.82,11.6,-.45],9)
box("waist","tong_loop",[4.85,11.55,-1.85],[5.95,12.45,-.65],7)
box("waist","tong_loop_rivet",[5.95,11.8,-1.45],[6.1,12.2,-1.05],3)
box("waist","apron_back",[-4.3,6,2.6],[4.3,12.6,2.95],15)
box("waist","apron_back_hem",[-4.4,5.7,2.55],[4.4,6.05,3.05],3)
box("apron_flap","apron_front",[-3.6,5.2,-3.25],[3.6,13.2,-2.95],11)
box("apron_flap","apron_notch",[-1.4,4.2,-3.25],[1.4,5.2,-2.95],11)
for sign in [-1,1]:
    x0,x1=sorted([3.4*sign,3.75*sign])
    box("apron_flap","apron_side_gold_"+str(sign),[x0,5.2,-3.35],[x1,13.2,-2.9],3)
    h0,h1=sorted([1.4*sign,3.75*sign])
    box("apron_flap","apron_hem_gold_"+str(sign),[h0,4.9,-3.35],[h1,5.25,-2.9],3)
    s0,s1=sorted([1.4*sign,1.75*sign])
    box("apron_flap","apron_notch_gold_"+str(sign),[s0,3.9,-3.35],[s1,5.25,-2.9],3)
box("apron_flap","apron_notch_hem",[-1.4,3.9,-3.35],[1.4,4.25,-2.9],3)

# Iron tongs hang from the hip loop with a riveted pivot and bent jaws.
for i,x in enumerate([5.05,5.55]):
    box("tongs","tong_arm_"+str(i),[x-.14,5.2,-1.4],[x+.14,12.6,-1.1],12)
    box("tongs","tong_jaw_"+str(i),[x-.14+(-.3 if i==0 else .3),4.4,-1.4],[x+.14+(-.3 if i==0 else .3),5.3,-1.1],12)
box("tongs","tong_pivot",[4.95,9.6,-1.55],[5.65,10.2,-.95],3)
box("tongs","tong_grip_wrap",[4.88,11.0,-1.5],[5.72,11.5,-1.0],7)

for side,sign in [("right",-1),("left",1)]:
    m=lambda bone,name,a,b,color,angle=0,pivot=None: mirrored(side,sign,bone,name,a,b,color,angle,pivot)
    # Bare, heavy upper arms; leather bracers with gold bands and a set gem.
    m("upper_arm","bicep",[4.8,19.5,-2],[8.8,26,2],2)
    m("upper_arm","shoulder_cap",[4.9,25.9,-1.9],[8.7,26.4,1.9],2)
    m("forearm","forearm_skin",[5,15.5,-1.8],[8.6,19.5,1.8],2)
    m("forearm","bracer",[4.85,15.9,-1.95],[8.75,19.15,1.95],7)
    m("forearm","bracer_gold_top",[4.75,18.7,-2.05],[8.85,19.25,2.05],3)
    m("forearm","bracer_gold_bottom",[4.75,15.8,-2.05],[8.85,16.3,2.05],3)
    m("forearm","bracer_lace",[6.55,16.4,-2.1],[7.05,18.6,-1.95],4)
    m("forearm","bracer_gem_frame",[8.75,16.85,-.65],[8.95,18.15,.65],3)
    m("forearm","bracer_gem",[8.9,17.15,-.35],[9.05,17.85,.35],9)
    # Closed working fist with separate knuckles and thumb.
    m("hand","fist",[5.3,13,-1.6],[8.3,15.6,1.4],5)
    for i in range(4):
        x=5.35+i*.73
        m("hand","finger_"+str(i),[x,13.2,-2.1],[x+.66,14.6,-1.55],5)
    m("hand","thumb",[4.95,13.9,-1.95],[5.6,15.2,-.6],5)
    # Layered boots: shaft, bronze strap, gold cuff, iron toe cap and sole.
    m("leg","trousers",[.4,5.5,-1.9],[4.3,13,1.9],1)
    m("leg","trouser_fold",[.35,6.4,-1.98],[4.35,6.9,1.98],0)
    m("leg","boot_shaft",[.25,1,-2.1],[4.45,6.5,2.1],7)
    m("leg","boot_cuff_gold",[.15,5.9,-2.2],[4.55,6.6,2.2],3)
    m("leg","boot_strap",[.2,3.4,-2.2],[4.5,4.1,2.2],4)
    m("leg","boot_toe",[.3,.5,-3.4],[4.4,2.6,-2]  ,7)
    m("leg","toe_cap",[.3,.8,-3.6],[4.4,2.2,-3.35],12)
    m("leg","sole",[.2,0,-3.7],[4.5,.6,2.2],8)
    m("leg","heel",[.2,.6,1.4],[4.5,1.2,2.2],8)
    m("leg","boot_buckle",[4.5,3.25,-.55],[4.62,4.25,.55],3)

# Only the left shoulder carries the ivory forge pauldron, as on the reference.
box("left_upper_arm","pauldron",[4.5,22.6,-2.4],[9.3,26.6,2.4],13)
box("left_upper_arm","pauldron_crown",[4.8,26.55,-2],[9,27.1,2],13)
box("left_upper_arm","pauldron_rim",[4.4,22.4,-2.5],[9.4,22.9,2.5],3)
box("left_upper_arm","pauldron_outer_rim",[9.25,22.9,-2.5],[9.45,26.6,2.5],3)
frame("left_upper_arm","pauldron_gem_frame",6.2,23.8,7.6,25.2,-2.62,-2.4,.3,3)
box("left_upper_arm","pauldron_gem",[6.5,24.1,-2.7],[7.3,24.9,-2.5],9)
for z in [-1.4,1.4]: box("left_upper_arm","pauldron_rivet_"+str(z),[9.4,24.4,z-.2],[9.55,24.8,z+.2],4)
box("left_upper_arm","arm_strap",[4.6,21.1,-2.15],[9,21.7,2.15],7)
box("left_upper_arm","arm_strap_buckle",[8.95,21.0,-.35],[9.1,21.8,.35],3)

# Forging hammer: wrapped handle through the right fist, banded steel head.
box("hammer","hammer_handle",[-7.25,8.2,-.75],[-6.35,16.0,.15],14)
box("hammer","hammer_grip",[-7.35,12.6,-.85],[-6.25,13.0,.25],7)
box("hammer","hammer_pommel",[-7.4,15.9,-.9],[-6.2,16.45,.3],3)
box("hammer","hammer_collar",[-7.45,8.3,-.95],[-6.15,8.9,.35],3)
box("hammer","hammer_head",[-8.1,6.1,-2.4],[-5.5,8.4,1.8],12)
for z in [-1.0,.4]: box("hammer","hammer_band_"+str(z),[-8.2,6.0,z],[-5.4,8.5,z+.4],3)
box("hammer","hammer_face_front",[-7.95,6.25,-2.55],[-5.65,8.25,-2.38],8)
box("hammer","hammer_face_back",[-7.95,6.25,1.78],[-5.65,8.25,1.95],8)
box("hammer","hammer_rune",[-8.25,6.9,-.35],[-8.12,7.6,.3],9)

# Head: portrait UV, projecting nose and ears, full beard, forehead goggles.
box("hi_head","head_skin",[-4,26,-4],[4,34.2,3.6],10)
box("hi_head","nose",[-.6,28.7,-4.75],[.6,30.4,-3.95],2)
box("hi_head","hair_top",[-4.25,33.3,-4.25],[4.25,35.2,3.85],6)
box("hi_head","hair_back",[-4.25,26,3.55],[4.25,34,4.15],6)
box("hi_head","hair_crown_tuft",[-2.6,35.15,-2.4],[2.4,35.6,2.2],6)
for sign in [-1,1]:
    x=sign*4.0
    lo,hi=sorted([sign*3.7,sign*4.65])
    box("hi_head","ear_"+str(sign),[lo,28.8,-.6],[hi,31,.9],2)
    box("hi_head","hair_temple_"+str(sign),[x-.3,31,-3.9],[x+.3,34,-.8],6)
    box("hi_head","hair_behind_ear_"+str(sign),[x-.3,28,.95],[x+.3,34,3.9],6)
    box("hi_head","beard_jaw_"+str(sign),[x-.4,25.8,-3.95],[x+.4,29.3,.85],6)
    box("hi_head","brow_"+str(sign),[sign*1.95-.95,31.1,-4.22],[sign*1.95+.95,31.55,-3.95],6)
    box("hi_head","moustache_"+str(sign),[min(.05*sign,2.4*sign),27.9,-4.75],[max(.05*sign,2.4*sign),28.75,-4.3],6,[0,0,-22.5*sign],[sign*1.2,28.3,-4.5])
    l0,l1=sorted([.45*sign,3.35*sign])
    frame("hi_head","goggle_rim_"+str(sign),l0,31.7,l1,33.75,-4.7,-4.3,.38,3)
    g0,g1=sorted([.8*sign,3.0*sign])
    box("hi_head","goggle_lens_"+str(sign),[g0,32.05,-4.6],[g1,33.4,-4.35],9)
box("hi_head","beard_main",[-3.4,24.6,-4.55],[3.4,28.2,-3.4],6)
box("hi_head","beard_chin_tip",[-2,23.8,-4.45],[2,24.7,-3.5],6)
box("hi_head","mouth",[-.9,27.45,-4.62],[.9,27.85,-4.5],7)
box("hi_head","goggle_bridge",[-.5,32.45,-4.6],[.5,32.95,-4.3],4)
box("hi_head","goggle_strap",[-4.35,32.2,-4.3],[4.35,33.1,4.25],7)
frame("hi_head","goggle_strap_buckle",-.7,31.95,.7,33.35,4.2,4.45,.3,3)
box("hitbox","collision_proxy",[-4.8,0,-4.8],[4.8,35.6,4.8],8)

def rot(values): return [{"time":t,"rotation":v} for t,v in values]
def pos(values): return [{"time":t,"position":v} for t,v in values]
idle={"body":pos([(0,[0,0,0]),(2,[0,.1,0]),(4,[0,0,0])]),
      "hi_head":rot([(0,[0,0,0]),(1.2,[1,3,0]),(2.8,[-1,-3,0]),(4,[0,0,0])]),
      "apron_flap":rot([(0,[0,0,0]),(2,[-1.5,0,0]),(4,[0,0,0])])}
for side,sign in [("right",-1),("left",1)]:
    idle[side+"_upper_arm"]=rot([(0,[0,0,0]),(2,[1,0,sign*.8]),(4,[0,0,0])])
greet={"hi_head":rot([(0,[0,0,0]),(.35,[-6,0,0]),(.8,[1,0,0]),(1.6,[0,0,0])]),
       "left_upper_arm":rot([(0,[0,0,0]),(.35,[70,0,-12]),(1.2,[70,0,-12]),(1.6,[0,0,0])]),
       "left_forearm":rot([(0,[0,0,0]),(.35,[30,0,0]),(1.2,[30,0,0]),(1.6,[0,0,0])]),
       "left_hand":rot([(0,[0,0,0]),(.4,[0,0,16]),(.6,[0,0,-16]),(.8,[0,0,16]),(1,[0,0,-16]),(1.2,[0,0,0]),(1.6,[0,0,0])])}
# Two strikes; impacts land on ticks 11 and 25 so a server-side sound can sync to them.
hammer={"hi_head":rot([(0,[0,0,0]),(.3,[-12,0,0]),(1.7,[-12,0,0]),(2,[0,0,0])]),
        "body":rot([(0,[0,0,0]),(.35,[-3,0,0]),(.55,[4,0,0]),(.95,[-3,0,0]),(1.25,[4,0,0]),(1.6,[0,0,0]),(2,[0,0,0])]),
        "right_upper_arm":rot([(0,[0,0,0]),(.35,[120,0,22]),(.55,[55,0,-4]),(.95,[120,0,22]),(1.25,[55,0,-4]),(1.6,[30,0,-2]),(2,[0,0,0])]),
        "right_forearm":rot([(0,[0,0,0]),(.35,[40,0,0]),(.55,[25,0,0]),(.95,[40,0,0]),(1.25,[25,0,0]),(1.6,[15,0,0]),(2,[0,0,0])]),
        "left_upper_arm":rot([(0,[0,0,0]),(.3,[35,0,6]),(1.7,[35,0,6]),(2,[0,0,0])]),
        "left_forearm":rot([(0,[0,0,0]),(.3,[25,0,0]),(1.7,[25,0,0]),(2,[0,0,0])]),
        "apron_flap":rot([(0,[0,0,0]),(.55,[-4,0,0]),(.8,[0,0,0]),(1.25,[-4,0,0]),(1.5,[0,0,0]),(2,[0,0,0])])}
success={"hi_head":rot([(0,[0,0,0]),(.5,[10,0,0]),(1.8,[10,0,0]),(2.4,[0,0,0])]),
         "body":pos([(0,[0,0,0]),(.4,[0,.4,0]),(.6,[0,0,0]),(2.4,[0,0,0])]),
         "right_upper_arm":rot([(0,[0,0,0]),(.5,[150,0,28]),(1.8,[150,0,28]),(2.4,[0,0,0])]),
         "right_forearm":rot([(0,[0,0,0]),(.5,[10,0,0]),(1.8,[10,0,0]),(2.4,[0,0,0])]),
         "hammer":rot([(0,[0,0,0]),(.5,[0,0,0]),(1.0,[0,90,0]),(1.4,[0,0,0]),(2.4,[0,0,0])]),
         "left_upper_arm":rot([(0,[0,0,0]),(.5,[0,0,-16]),(1.8,[0,0,-16]),(2.4,[0,0,0])]),
         "left_forearm":rot([(0,[0,0,0]),(.5,[35,0,0]),(1.8,[35,0,0]),(2.4,[0,0,0])])}
ANIMATIONS=[("idle",4,True,idle),("greet",1.6,False,greet),
            ("hammer",2,False,hammer),("craft_success",2.4,False,success)]

# Everything above is authored with the character's right on -X. The model faces north (-Z),
# so its right is +X: mirror once so the left pauldron and right-hand hammer match the reference.
# A mirror across X keeps X rotations and negates Y/Z rotations and X positions.
for _,_,row in ROWS:
    row["from"],row["to"]=[-row["to"][0],*row["from"][1:]],[-row["from"][0],*row["to"][1:]]
    if "origin" in row: row["origin"]=[-row["origin"][0],*row["origin"][1:]]
    if "rotation" in row: row["rotation"]=[row["rotation"][0],-row["rotation"][1],-row["rotation"][2]]
BONES=[(name,[-pivot[0],*pivot[1:]],parent) for name,pivot,parent in BONES]
for _,_,_,bones in ANIMATIONS:
    for keys in bones.values():
        for key in keys:
            if "rotation" in key: key["rotation"]=[key["rotation"][0],-key["rotation"][1],-key["rotation"][2]]
            if "position" in key: key["position"]=[-key["position"][0],*key["position"][1:]]

# Paint the 16-slot atlas: material noise, edge bevels, portrait, rune apron and wood grain.
# Baked materials per colour index (see COLORS), plus face-sized art.
MATS=["cloth_charcoal","cloth_dark","skin_tan","gold","bronze","skin_tan","hair_brown","leather","iron","gem",
      "portrait","teal_leather","steel","ivory","wood","teal_leather"]
OVERRIDES={"apron_front":"apron_art"}
ARTS={"portrait":portrait("skin_tan",(.43,.52),(.25,.71),.09,[(70,140,205),(47,110,180),(18,40,70),(220,240,255)]),
      "apron_art":panel("teal_leather",rune_apron)}


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
    atlas,face_uv=bake_npc(ROWS,MATS,ARTS,OVERRIDES,seed=sum(map(ord,NAME)))
    size=atlas.size[0]
    call("create_texture",{"name":NAME+".png","width":size,"height":size,"uv_width":size,"uv_height":size,"fill_color":"#000000"})
    call("risky_eval",{"code":"(()=>{Undo.initEdit({uv_mode:true});Project.texture_width=%d;Project.texture_height=%d;Undo.finishEdit('NPC baked UV size');return true})()"%(size,size)})
    load_atlas(call,atlas)
    for name,pivot,parent in BONES: call("add_group",{"name":name,"origin":pivot,"parent":parent})
    by_bone=defaultdict(list)
    for bone,_,row in ROWS: by_bone[bone].append(row)
    for bone,rows in by_bone.items():
        call("place_cube",{"elements":rows,"group":bone,"texture":NAME+".png",
                          "faces":[{"face":f,"uv":[0,0,1,1]} for f in ["north","south","east","west","up","down"]]})
    call("risky_eval",{"code":uv_js(face_uv)})
    print(json.dumps({"geometry":summary}),flush=True)
    call("set_mode",{"mode_id":"animate"})
    for name,length,loop,bones in ANIMATIONS: call("create_animation",{"name":name,"animation_length":length,"loop":loop,"bones":bones})
    call("risky_eval",{"code":"(()=>{Undo.initEdit({animations:Animation.all.slice()});for(const a of Animation.all){a.name=a.name.replace(/^animation\\./,'');a.snapping=20;a.override=a.name!=='idle'}Undo.finishEdit('Smith state names 20 FPS');return Animation.all.map(a=>({name:a.name,length:a.length}))})()"})
    call("animation_timeline",{"animation_id":"idle","action":"stop"})
    call("set_mode",{"mode_id":"edit"})
    OUT.mkdir(parents=True,exist_ok=True)
    embedded(call("export_model",{"codec_id":"project","result_format":"embedded","max_content_length":2000000}),target)
    model=json.loads(target.read_text(encoding="utf-8"))
    (OUT/(NAME+".png")).write_bytes(base64.b64decode(model["textures"][0]["source"].split(",",1)[1]))
    call("create_offscreen_view",{"id":"smith_qa","width":1400,"height":1400,"copy_view":"none"})
    try:
        for view,camera in {"preview":[60,40,-90],"front":[0,17,-100],"right":[-100,17,0],"back":[0,17,100]}.items():
            result=call("set_camera_angle",{"view":"smith_qa","position":camera,"target":[0,17.5,0],"projection":"orthographic","zoom":.75})
            image=next(b for b in result["content"] if b.get("type")=="image")
            (OUT/(NAME+"-"+view+".png")).write_bytes(base64.b64decode(image["data"]))
    finally: dispose_view(client,"smith_qa")
    print(json.dumps({"exported":str(target),**summary}),flush=True)

if __name__=="__main__": main()
