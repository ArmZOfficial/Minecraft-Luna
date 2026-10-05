"""Build Lyra, the zone 06 alchemist, using native Blockbench MCP tools.

Anatomical right is +X; front is north/-Z. The stirring rod and flask follow
their respective hands. The floor burner is a separate, unanimated root bone.
"""
import argparse
import base64
import json
from collections import defaultdict
from blockbench_mcp import Client
from build_moonfall_boss import OUT, data, embedded, dispose_view
from build_luma_props import load_atlas
from npc_bake import bake_npc, uv_js, portrait, panel, gold_border, RAMPS, KIND, ramp

NAME="npc_lyra_alchemist"
MATS=["sage","cloth_ivory","gold","bronze","skin_fair","hair_white","leather_dark","dark",
      "violet_gem","ruby","gem","portrait","botanical_sage","botanical_violet","paper",
      "stone","glass_violet","glass_ruby","glass_cyan","liquid","cork"]
RAMPS.update({"sage":ramp("#102b29","#1a3d38","#295248","#3a685c","#527f70","#739b83","#a4c2a0"),
              "apron_violet":ramp("#200d2f","#352042","#4b3158","#654772","#846291","#a986b3","#d1b5d7"),
              "glass_violet":ramp("#392649","#564064","#795a8c","#987aa9","#b49cc5","#d5c9e3","#f9f0ff"),
              "glass_ruby":ramp("#572832","#79414b","#a66a75","#c58a94","#e4bac1","#f1dce0","#fff9fa"),
              "glass_cyan":ramp("#244d59","#407380","#6596a6","#89b6c4","#b5d8e3","#def0f5","#ffffff"),
              "liquid":ramp("#30043b","#57086f","#851797","#b139c6","#d86fe4","#f3bcf8","#fff2ff"),
              "cork":ramp("#3b2213","#624021","#886034","#ac844e","#c8a96a","#e0c794","#f4e4bd")})
KIND.update({"sage":"cloth","apron_violet":"leather","glass_violet":"gem","glass_ruby":"gem",
             "glass_cyan":"gem","liquid":"gem","cork":"wood"})
BONES=[("motion_root",[0,0,0],"root"),("body",[0,12,0],"motion_root"),
       ("waist",[0,12,0],"body"),("hi_head",[0,24,0],"body"),
       ("goggles",[0,30.7,-3.8],"hi_head"),("braid",[-3.5,26,-2.9],"hi_head")]
for side,sign in [("right",1),("left",-1)]:
    BONES += [(side+"_upper_arm",[6.2*sign,23,0],"body"),
              (side+"_forearm",[6.2*sign,17.7,0],side+"_upper_arm"),
              (side+"_hand",[6.2*sign,13.3,-.3],side+"_forearm"),
              (side+"_leg",[2.1*sign,12,0],"motion_root"),
              (side+"_coattail",[2.4*sign,12,2.1],"waist")]
BONES += [("flask",[-6.2,12.6,-2.3],"left_hand"),("stirring_rod",[6.2,12.7,-1.8],"right_hand"),
          ("brew_station",[6.2,0,-1.8],"motion_root"),("brew_liquid",[6.2,7.4,-1.8],"brew_station"),
          ("hitbox",[0,28.4,0],"root")]
ROWS=[]

def box(bone,name,a,b,color,rotation=None,origin=None):
    row={"name":name,"from":a,"to":b}
    if rotation is not None: row["rotation"]=rotation
    if origin is not None: row["origin"]=origin
    ROWS.append((bone,color,row))

def mirror(side,sign,bone,name,a,b,color):
    x0,x1=sorted([a[0]*sign,b[0]*sign])
    box(side+"_"+bone,side+"_"+name,[x0,a[1],a[2]],[x1,b[1],b[2]],color)

def frame(bone,name,x0,y0,x1,y1,z0,z1,edge,color=2):
    for part,a,b in [("top",[x0,y1-edge,z0],[x1,y1,z1]),("bottom",[x0,y0,z0],[x1,y0+edge,z1]),
                     ("left",[x0,y0+edge,z0],[x0+edge,y1-edge,z1]),("right",[x1-edge,y0+edge,z0],[x1,y1-edge,z1])]:
        box(bone,name+"_"+part,a,b,color)

# Layered coat and short botanical apron, with true lapels, clasps and piping.
box("body","coat_torso",[-4.2,12,-2.5],[4.2,23.6,2.5],0)
box("body","shirt_front",[-2.4,14.1,-2.8],[2.4,23.9,-2.45],1)
box("body","shirt_collar_back",[-3.2,23.3,1.5],[3.2,24.7,2.8],1)
for sign in [-1,1]:
    x=sign*3.2
    box("body","lapel_"+str(sign),[x-.75,18.9,-3.15],[x+.75,24,-2.5],12,[0,0,-22.5*sign],[x,23,-2.8])
    box("body","lapel_piping_"+str(sign),[x-.13,18.9,-3.3],[x+.13,24,-3.1],2,[0,0,-22.5*sign],[x,23,-2.8])
    x0,x1=sorted([.3*sign,1.55*sign])
    box("body","collar_tip_"+str(sign),[x0,22.6,-3.08],[x1,24.1,-2.7],1,[0,0,-22.5*sign],[sign,23.4,-2.9])
for y in [15.8,18.1,20.4,22]: box("body","shirt_button_"+str(y),[-.2,y,-3.05],[.2,y+.4,-2.8],2)
box("body","apron_bib",[-2.6,13.2,-3.02],[2.6,18,-2.76],13)
for x in [-2.2,2.2]:
    box("body","apron_strap_"+str(x),[x-.23,17,-3.17],[x+.23,22.8,-2.85],6)
    frame("body","apron_clasp_"+str(x),x-.45,17.3,x+.45,18.2,-3.35,-3.05,.18)
box("body","back_botanical_panel",[-3.5,13.5,2.51],[3.5,23.2,2.65],12)
box("body","brooch_star",[1.5,21,-3.5],[2.6,22.1,-3.13],2,[0,0,45],[2.05,21.55,-3.3])
box("body","brooch_gem",[1.8,21.3,-3.67],[2.3,21.8,-3.46],8,[0,0,45],[2.05,21.55,-3.6])

# Belt: framed buckle, three glass vials, individually corked and labelled.
box("waist","belt",[-4.5,11.45,-2.8],[4.5,12.85,2.8],6)
frame("waist","belt_buckle",-1,11.25,1,13.1,-3.2,-2.75,.3)
box("waist","buckle_pin",[-.1,11.55,-3.15],[.1,12.8,-2.95],3)
box("waist","apron_lower",[-2.8,7.5,-3],[2.8,11.9,-2.6],13)
box("waist","vial_rack",[-1.9,9.6,-3.6],[2.6,10.15,-3.05],6)
for i,x in enumerate([-1.35,.35,2.05]):
    glass=[17,18,16][i]
    box("waist","belt_vial_"+str(i),[x-.55,10.15,-3.62],[x+.55,11.4,-2.92],glass)
    box("waist","belt_vial_liquid_"+str(i),[x-.36,10.25,-3.73],[x+.36,10.95,-3.6],[9,10,8][i])
    box("waist","belt_vial_neck_"+str(i),[x-.26,11.35,-3.48],[x+.26,11.85,-3.07],glass)
    box("waist","belt_vial_cork_"+str(i),[x-.3,11.85,-3.52],[x+.3,12.23,-3.03],20)
    box("waist","belt_vial_label_"+str(i),[x-.18,10.48,-3.83],[x+.18,10.92,-3.69],14)
box("waist","herb_pouch",[4.1,9,-1.6],[5.4,11.65,1.2],6)
box("waist","pouch_flap",[4.15,10.9,-1.85],[5.45,11.95,-1.58],0)
box("waist","pouch_button",[4.6,11.05,-2],[5,11.45,-1.8],2)

for side,sign in [("right",1),("left",-1)]:
    m=lambda bone,name,a,b,col:mirror(side,sign,bone,name,a,b,col)
    m("upper_arm","sleeve",[4.25,17.7,-1.85],[8,23.6,1.85],0)
    m("upper_arm","shoulder_overlay",[4.15,22.1,-2.05],[8.15,24,2.05],12)
    m("upper_arm","shoulder_trim",[4.1,23.75,-2.15],[8.2,24.05,2.15],2)
    m("upper_arm","sleeve_patch",[5.1,19.05,-2],[7.2,21.3,-1.8],12)
    for y in [19.4,20.8]: m("upper_arm","sleeve_clasp_"+str(y),[7.95,y,-.6],[8.15,y+.4,.6],2)
    m("forearm","lower_sleeve",[4.6,14.3,-1.65],[7.8,17.85,1.65],0)
    m("forearm","cuff",[4.45,13.45,-1.95],[7.95,14.85,1.95],1)
    m("forearm","cuff_gold",[4.35,14.6,-2.05],[8.05,14.9,2.05],2)
    m("forearm","bracer",[4.75,13,-1.7],[7.65,13.8,1.7],6)
    m("forearm","bracer_buckle",[5.75,13.1,-1.88],[6.65,13.75,-1.65],2)
    m("hand","glove",[4.95,11.6,-1.3],[7.45,13.3,1.15],6)
    for i in range(4):
        x=5.02+i*.6
        reach=-2.5 if side=="left" else -1.95
        m("hand","finger_"+str(i),[x,11.25,reach],[x+.51,12.6,-1.22],4)
    m("hand","thumb",[4.55,12,-2.1],[5.3,13.15,-.8],4)
    m("hand","glove_plate",[5.35,12.05,1.16],[7.05,13.15,1.35],3)
    m("coattail","front_tail",[2.5,4.6,-2.75],[4.5,12.2,-1.7],12)
    m("coattail","back_tail",[.2,3.9,1.8],[4.45,12.1,2.75],12)
    m("coattail","side_tail",[3.75,4.5,-1.75],[4.55,12.1,1.8],0)
    m("coattail","front_hem",[.3,4.45,-2.85],[4.6,4.75,-1.65],2)
    m("coattail","back_hem",[.25,3.75,1.75],[4.55,4.05,2.85],2)
    m("leg","trousers",[.65,3,-1.5],[3.55,12,1.5],1)
    m("coattail","ivory_underskirt",[.35,4.7,-2.3],[2.5,12,-1.6],1)
    m("leg","boot",[.55,.7,-1.7],[3.65,5.4,1.8],6)
    m("leg","boot_cuff",[.35,4.95,-1.9],[3.85,5.7,2],6)
    m("leg","boot_strap",[.4,3.55,-1.94],[3.8,4.2,-1.7],6)
    m("leg","boot_buckle",[1.55,3.65,-2.08],[2.65,4.25,-1.9],2)
    m("leg","buckle_pin",[2,3.8,-2.17],[2.2,4.1,-2.05],3)
    m("leg","toe",[.4,.45,-3.05],[3.8,2.3,-1.55],6)
    m("leg","toe_trim",[.45,.95,-3.15],[3.75,1.3,-3],2)
    m("leg","sole",[.3,0,-3.2],[3.9,.55,2],7)

# Face is its own artwork; stepped hair locks and a separate swinging braid.
box("hi_head","head",[-3.8,24,-3.75],[3.8,31.65,3.05],11)
box("hi_head","nose",[-.35,26.7,-4.15],[.35,28.2,-3.73],4)
box("hi_head","hair_cap",[-4.05,30.4,-3.5],[4.05,32.7,3.45],5)
box("hi_head","hair_crown",[-2.5,32.4,-2.5],[2.5,33.15,2.4],5)
box("hi_head","hair_back",[-4.05,25.2,2.7],[4.05,31.2,3.85],5)
for sign in [-1,1]:
    x=sign*3.85
    box("hi_head","temple_"+str(sign),[x-.5,27.4,-2.7],[x+.5,31.2,2.65],5)
    box("hi_head","ear_base_"+str(sign),[x-.35,27,-.8],[x+.35,28.9,.9],4)
    # Pointed elf ears use two straight, stepped cuboids.
    x0,x1=sorted([4*sign,5.2*sign])
    box("hi_head","ear_long_"+str(sign),[x0,27.6,-.45],[x1,28.65,.65],4)
    x0,x1=sorted([5.1*sign,5.85*sign])
    box("hi_head","ear_tip_"+str(sign),[x0,28,-.35],[x1,28.6,.5],4)
    for j in range(3):
        hx=sign*(3.7-j*.3)
        box("hi_head","hair_side_lock_"+str(sign)+"_"+str(j),[hx-.45,24.4+j*1.15,-1.3],[hx+.45,26.8+j*1.15,1.35],5)
    box("hi_head","earring_chain_"+str(sign),[sign*4.7-.1,26.1,.05],[sign*4.7+.1,27.8,.25],2)
    box("hi_head","earring_gem_"+str(sign),[sign*4.7-.28,25.8,-.12],[sign*4.7+.28,26.36,.42],8,[0,0,45],[sign*4.7,26.08,.15])
for i,(x,y) in enumerate([(-2.8,30.1),(-1.65,29.6),(-.5,29.2),(1.2,30.15),(2.65,29.6)]):
    box("hi_head","fringe_"+str(i),[x-.72,y,-4.12],[x+.72,31.05,-3.4],5)
for i,(x,y,z) in enumerate([(-3.5,24.9,-3),(-3.7,23.9,-3.05),(-3.45,22.9,-3),(-3.65,21.9,-3.05)]):
    box("braid","braid_segment_"+str(i),[x-.55,y,z-.7],[x+.55,y+1.25,z+.7],5)
box("braid","braid_tie",[-4.15,21.7,-3.7],[-3.05,22.1,-2.4],2)
box("braid","braid_tip",[-4,20.9,-3.55],[-3.25,21.75,-2.5],5)
# Asymmetric diagonal fringe below the goggles; one long lock gives the face depth.
box("hi_head","fringe_diagonal",[-3.35,28.95,-4.45],[-.65,30.05,-3.85],5,[0,0,22.5],[-2,29.5,-4.1])
box("hi_head","fringe_point",[-3.7,28.1,-4.3],[-2.6,29.45,-3.85],5,[0,0,22.5],[-3.15,28.8,-4.1])

# Forehead goggles: hollow gold rims, inset purple lenses and a leather back strap.
box("goggles","goggle_strap_back",[-4.2,30.05,3.45],[4.2,31.05,3.75],6)
for sign in [-1,1]:
    x0,x1=sorted([.4*sign,3.65*sign])
    frame("goggles","goggle_frame_"+str(sign),x0,30.05,x1,32.2,-4.5,-3.85,.3)
    box("goggles","goggle_lens_"+str(sign),[x0+.3,30.35,-4.24],[x1-.3,31.9,-3.88],16)
    box("goggles","goggle_strap_side_"+str(sign),[sign*4.05-.17,30.05,-3.95],[sign*4.05+.17,31.05,3.6],6)
    box("goggles","goggle_hinge_"+str(sign),[sign*3.9-.2,30.3,-4.08],[sign*3.9+.2,30.9,-3.7],3)
box("goggles","goggle_bridge",[-.6,30.8,-4.32],[.6,31.25,-3.9],3)

# Flask neck meets the fingertips; visible liquid is below the neck, under the cork.
box("flask","flask_glass",[-7.3,9.4,-3.2],[-5.1,11.8,-1.4],16)
box("flask","flask_liquid",[-7.1,9.55,-3.3],[-5.3,10.85,-3.15],19)
box("flask","flask_shoulder",[-7.05,11.75,-2.95],[-5.35,12.15,-1.65],16)
box("flask","flask_neck",[-6.6,12,-2.65],[-5.8,13.3,-1.95],16)
box("flask","flask_neck_ring",[-6.7,13,-2.75],[-5.7,13.35,-1.85],2)
box("flask","flask_cork",[-6.62,13.3,-2.67],[-5.78,13.8,-1.93],20)
box("flask","flask_label",[-6.65,10.1,-3.45],[-5.75,11.1,-3.25],14)
box("stirring_rod","rod_shaft",[6.03,7.3,-1.97],[6.37,13.2,-1.63],3)
box("stirring_rod","rod_grip",[5.92,12.4,-2.08],[6.48,13.4,-1.52],6)
box("stirring_rod","rod_finial",[5.85,13.4,-2.15],[6.55,14.1,-1.45],2,[0,0,45],[6.2,13.75,-1.8])
box("stirring_rod","rod_paddle",[5.92,7.2,-2.08],[6.48,8.3,-1.52],2)

# Root-anchored floor burner and hollow kettle. No fire entity or damaging block.
box("brew_station","burner_base",[4.1,0,-3.9],[8.3,.8,.3],15)
for x in [4.4,7.4]:
    box("brew_station","burner_column_"+str(x),[x,.7,-3.4],[x+.6,3.3,-.2],15)
box("brew_station","burner_back",[4.4,.7,-.9],[8,3.3,-.2],15)
box("brew_station","burner_top",[4.3,3.3,-3.7],[8.1,4.1,.1],15)
box("brew_station","flame_base",[5.3,.8,-3],[7.1,1.3,-1],2)
box("brew_station","flame_core",[5.7,1.3,-2.7],[6.7,2.65,-1.45],10)
for x,z in [(4.75,-3.0),(7.3,-3.0),(6,-.5)]:
    box("brew_station","kettle_leg_"+str(x),[x,4,z],[x+.35,6.75,z+.35],3)
box("brew_station","kettle_bottom",[4.3,6.4,-3.7],[8.1,6.9,.1],3)
for name,a,b in [("front",[4.3,6.9,-3.8],[8.1,8.7,-3.3]),("back",[4.3,6.9,-.3],[8.1,8.7,.2]),
                 ("left",[4.15,6.9,-3.3],[4.65,8.7,-.3]),("right",[7.75,6.9,-3.3],[8.25,8.7,-.3])]:
    box("brew_station","kettle_wall_"+name,a,b,16)
for name,a,b in [("front",[4.2,8.45,-3.9],[8.2,8.85,-3.3]),("back",[4.2,8.45,-.3],[8.2,8.85,.3]),
                 ("left",[4.05,8.45,-3.3],[4.65,8.85,-.3]),("right",[7.75,8.45,-3.3],[8.35,8.85,-.3])]:
    box("brew_station","kettle_rim_"+name,a,b,2)
for x in [4.05,7.9]: box("brew_station","kettle_handle_"+str(x),[x,7.1,-2.5],[x+.45,8.2,-1.1],3)
box("brew_liquid","kettle_liquid",[4.7,6.9,-3.25],[7.7,7.65,-.35],19)
box("brew_liquid","liquid_glint",[5,7.66,-2.9],[5.55,7.75,-2.3],8)
box("hitbox","collision_proxy",[-4.7,0,-4.7],[4.7,33.2,4.7],7)

def botanical(c,face,rng):
    if face not in ("north","south"): return
    gold_border(c,1); g=RAMPS["gold"];cx=c.w//2
    for y in range(3,c.h-3): c.set(cx,y,g[3]); c.set(cx+1,y,g[2])
    for j,y in enumerate(range(5,c.h-4,5)):
        sign=1 if j%2 else -1
        for k in range(1,min(c.w//3,5)):
            c.set(cx+sign*k,y-k//2,g[4]);c.set(cx+sign*k,y-k//2-1,g[5])
            if k>1:c.set(cx+sign*k,y-k//2+1,g[3])
    if c.h>14:
        for x in range(cx-2,cx+3):c.set(x,3,g[4])

ARTS={"portrait":portrait("skin_fair",(.44,.59),(.28,.72),.105,
                          [(174,98,226),(115,52,161),(50,24,72),(255,241,255)],young=True,mouth=.78),
      "botanical_sage":panel("sage",botanical),"botanical_violet":panel("apron_violet",botanical)}

def rot(items):return [{"time":t,"rotation":v} for t,v in items]
def pos(items):return [{"time":t,"position":v} for t,v in items]
idle={"body":pos([(0,[0,0,0]),(2,[0,.08,0]),(4,[0,0,0])]),
      "hi_head":rot([(0,[0,0,0]),(1,[1,3,0]),(3,[-1,-3,0]),(4,[0,0,0])]),
      "braid":rot([(0,[0,0,0]),(2,[0,0,3]),(4,[0,0,0])])}
greet={"hi_head":rot([(0,[0,0,0]),(.5,[5,0,0]),(1.1,[0,0,0]),(1.8,[0,0,0])]),
       "left_upper_arm":rot([(0,[0,0,0]),(.5,[40,0,-7]),(1.2,[40,0,-7]),(1.8,[0,0,0])]),
       "left_forearm":rot([(0,[0,0,0]),(.5,[35,0,0]),(1.2,[35,0,0]),(1.8,[0,0,0])]),
       "flask":rot([(0,[0,0,0]),(.5,[-75,0,7]),(1.2,[-75,0,7]),(1.8,[0,0,0])])}
stir={"hi_head":rot([(0,[0,0,0]),(.4,[10,12,0]),(2.6,[10,12,0]),(3,[0,0,0])]),
      "right_hand":pos([(0,[0,0,0]),(.4,[.35,0,0]),(.75,[0,0,.35]),(1.1,[-.35,0,0]),
                        (1.45,[0,0,-.35]),(1.8,[.35,0,0]),(2.15,[0,0,.35]),(2.5,[-.35,0,0]),(3,[0,0,0])])}
offer={"hi_head":rot([(0,[0,0,0]),(.5,[-3,-8,0]),(1.8,[-3,-8,0]),(2.4,[0,0,0])]),
       "left_upper_arm":rot([(0,[0,0,0]),(.5,[55,0,-5]),(1.8,[55,0,-5]),(2.4,[0,0,0])]),
       "left_forearm":rot([(0,[0,0,0]),(.5,[20,0,0]),(1.8,[20,0,0]),(2.4,[0,0,0])]),
       "flask":rot([(0,[0,0,0]),(.5,[-75,0,5]),(1.8,[-75,0,5]),(2.4,[0,0,0])])}
success={"hi_head":rot([(0,[0,0,0]),(.35,[-6,0,0]),(.8,[3,0,0]),(1.8,[0,0,0])]),
         "left_upper_arm":rot([(0,[0,0,0]),(.4,[20,0,-10]),(1.15,[20,0,-10]),(1.8,[0,0,0])]),
         "left_forearm":rot([(0,[0,0,0]),(.4,[35,0,0]),(1.15,[35,0,0]),(1.8,[0,0,0])]),
         "flask":rot([(0,[0,0,0]),(.4,[-55,0,10]),(1.15,[-55,0,10]),(1.8,[0,0,0])]),
         "brew_liquid":pos([(0,[0,0,0]),(.4,[0,.22,0]),(.8,[0,0,0]),(1.8,[0,0,0])])}
ANIMATIONS=[("idle",4,True,idle),("greet",1.8,False,greet),("stir",3,False,stir),
            ("offer_potion",2.4,False,offer),("brew_success",1.8,False,success)]

def main():
    parser=argparse.ArgumentParser();parser.add_argument("--project-uuid");parser.add_argument("--dry-run",action="store_true")
    args=parser.parse_args()
    assert len({row["name"] for _,_,row in ROWS})==len(ROWS)
    assert all(all(a<b for a,b in zip(row["from"],row["to"])) for _,_,row in ROWS)
    assert {bone for bone,_,_ in ROWS}<={b[0] for b in BONES}
    atlas,face_uv=bake_npc(ROWS,MATS,ARTS,seed=7381);size=atlas.size[0]
    summary={"asset":NAME,"cubes":len(ROWS),"bones":len(BONES),"atlas":[size,size],"animations":[a[0] for a in ANIMATIONS]}
    if args.dry_run:print(json.dumps(summary));return
    if not args.project_uuid:parser.error("--project-uuid is required")
    target=OUT/(NAME+".bbmodel")
    if target.exists():raise RuntimeError("Revision exists; create a new revision instead")
    client=Client()
    def guard():
        info=data(client.call("get_project_info",{}));p=info["project"]
        if p["name"]!=NAME or p["uuid"]!=args.project_uuid:raise RuntimeError("Active project changed")
        return info
    def call(name,arguments):guard();return client.call(name,arguments)
    first=guard()
    if any(first["counts"].get(k,0) for k in ["cubes","meshes","groups","textures"]):raise RuntimeError("Project must be empty")
    call("set_mode",{"mode_id":"edit"})
    call("create_texture",{"name":NAME+".png","width":size,"height":size,"uv_width":size,"uv_height":size,"fill_color":"#000000"})
    call("risky_eval",{"code":"(()=>{Undo.initEdit({uv_mode:true});Project.texture_width=%d;Project.texture_height=%d;Undo.finishEdit('Lyra UV size');return true})()"%(size,size)})
    load_atlas(call,atlas)
    for name,pivot,parent in BONES:call("add_group",{"name":name,"origin":pivot,"parent":parent})
    batches=defaultdict(list)
    for bone,_,row in ROWS:batches[bone].append(row)
    for bone,rows in batches.items():call("place_cube",{"elements":rows,"group":bone,"texture":NAME+".png","faces":True})
    call("risky_eval",{"code":uv_js(face_uv)})
    print(json.dumps({"geometry":summary}),flush=True)
    call("set_mode",{"mode_id":"animate"})
    for name,length,loop,bones in ANIMATIONS:call("create_animation",{"name":name,"animation_length":length,"loop":loop,"bones":bones})
    call("risky_eval",{"code":"(()=>{Undo.initEdit({animations:Animation.all.slice()});for(const a of Animation.all){a.name=a.name.replace(/^animation\\./,'');a.snapping=20;a.override=a.name!=='idle'}Undo.finishEdit('Lyra state names 20 FPS');return true})()"})
    call("animation_timeline",{"animation_id":"idle","action":"stop"});call("set_mode",{"mode_id":"edit"})
    embedded(call("export_model",{"codec_id":"project","result_format":"embedded","max_content_length":2000000}),target)
    model=json.loads(target.read_text(encoding="utf-8"))
    (OUT/(NAME+".png")).write_bytes(base64.b64decode(model["textures"][0]["source"].split(",",1)[1]))
    call("create_offscreen_view",{"id":"lyra_qa","width":1400,"height":1400,"copy_view":"none"})
    try:
        for view,camera in {"preview":[60,40,-90],"front":[0,17,-100],"right":[100,17,0],"back":[0,17,100]}.items():
            result=call("set_camera_angle",{"view":"lyra_qa","position":camera,"target":[0,17,0],"projection":"orthographic","zoom":.78})
            image=next(b for b in result["content"] if b.get("type")=="image")
            (OUT/(NAME+"-"+view+".png")).write_bytes(base64.b64decode(image["data"]))
    finally:dispose_view(client,"lyra_qa")
    print(json.dumps({"exported":str(target),**summary}),flush=True)

if __name__=="__main__":main()
