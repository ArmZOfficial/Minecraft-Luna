"""Create the market merchant through native Blockbench MCP (zone 06 shops A-D).

A travelling trader: wine waistcoat over a rolled-sleeve shirt, teal scarf, plumed felt hat,
curled moustache, a pack with a rolled rug and lantern, and a hand balance in the left fist.
Authored with the character's right on +X (the model faces north, -Z).
"""
import argparse
import base64
import json
from collections import defaultdict
from blockbench_mcp import Client
from build_moonfall_boss import OUT, data, embedded, dispose_view
from build_luma_props import load_atlas
from npc_bake import bake_npc, uv_js, portrait, panel, vest_damask, carpet_roll, scarf_tail, merchant_crest

NAME = "npc_market_merchant"
# 0 wine, 1 shirt, 2 gold, 3 bronze, 4 skin, 5 hair, 6 felt hat, 7 leather, 8 sole, 9 cyan glass,
# 10 portrait, 11 vest front, 12 boot leather, 13 teal scarf, 14 scarf tail, 15 plume,
# 16 trousers, 17 pack frame wood, 18 rug, 19 pack crest, 20 ruby glass, 21 paper
COLORS = ["#691a2e", "#ebe2c8", "#d4a646", "#8f6a33", "#c48a68", "#3a2416", "#472d1e", "#583321",
          "#2a221d", "#3ee4f0", "#c48a68", "#691a2e", "#3e2a20", "#1f4d5e", "#1f4d5e", "#36aab0",
          "#3a312b", "#4d2d19", "#691a2e", "#583321", "#bd2d38", "#ece3c7"]
SX = 6.2  # shoulder/hand column
BONES = [("motion_root", [0,0,0], "root"), ("body", [0,12,0], "motion_root"),
         ("waist", [0,12,0], "body"), ("hi_head", [0,24,0], "body"),
         ("hat", [0,31,0], "hi_head"), ("backpack", [0,18,2.8], "body")]
for side, sign in [("right",1),("left",-1)]:
    BONES += [(side+"_upper_arm", [SX*sign,23.5,0], "body"),
              (side+"_forearm", [SX*sign,18,0], side+"_upper_arm"),
              (side+"_hand", [SX*sign,13,-.3], side+"_forearm"),
              (side+"_leg", [2.1*sign,12,0], "motion_root")]
# Hand balance hangs from the left fist: scale (counter-tilt) > scale_turn (yaw) > beam (tip) > pans.
BONES += [("scale", [-SX,10.2,0], "left_hand"), ("scale_turn", [-SX,10.2,0], "scale"),
          ("scale_beam", [-SX,8.4,0], "scale_turn"),
          ("scale_pan_front", [-SX,8.4,-2.9], "scale_beam"), ("scale_pan_back", [-SX,8.4,2.9], "scale_beam"),
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
    """Hollow square of four bars, used for buckles."""
    box(bone,name+"_top",[x0,y1-edge,z0],[x1,y1,z1],color)
    box(bone,name+"_bottom",[x0,y0,z0],[x1,y0+edge,z1],color)
    box(bone,name+"_left",[x0,y0+edge,z0],[x0+edge,y1-edge,z1],color)
    box(bone,name+"_right",[x1-edge,y0+edge,z0],[x1,y1-edge,z1],color)

# Torso: wine waistcoat shell, laced shirt front, damask front panels with gold piping and buttons.
box("body","vest_torso",[-4.5,12,-2.6],[4.5,24,2.6],0)
box("body","shirt_front",[-.95,12.6,-2.75],[.95,23.6,-2.6],1)
for i,y in enumerate([14.2,16.4,18.6,20.8]):
    box("body","shirt_lace_"+str(i),[-.8,y,-2.85],[.8,y+.3,-2.7],7)
for sign in [-1,1]:
    s=str(sign)
    x0,x1=sorted([.95*sign,4.45*sign])
    box("body","vest_front_"+s,[x0,12.3,-2.9],[x1,23.3,-2.6],11)
    p0,p1=sorted([.85*sign,1.2*sign])
    box("body","vest_piping_"+s,[p0,12.3,-2.98],[p1,23.3,-2.85],2)
    for y in [14.6,17.0,19.4]:
        b0,b1=sorted([1.45*sign,2.05*sign])
        box("body","vest_button_"+s+"_"+str(y),[b0,y,-3.05],[b1,y+.6,-2.85],2)
    q0,q1=sorted([2.2*sign,3.4*sign])
    box("body","pocket_welt_"+s,[q0,13.6,-3.0],[q1,13.95,-2.85],2)
    # Pack straps run over each shoulder and down the front of the waistcoat.
    t0,t1=sorted([2.4*sign,3.4*sign])
    box("body","strap_front_"+s,[t0,15.2,-3.08],[t1,23.6,-2.9],7)
    box("body","strap_shoulder_"+s,[t0,23.6,-3.08],[t1,24.35,2.9],7)
    box("body","strap_buckle_"+s,[t0-.05,16.4,-3.2],[t1+.05,17.2,-3.0],2)
# Teal scarf knotted at the throat, one fringed tail falling over the right chest.
box("body","scarf_wrap",[-3.5,23.0,-2.95],[3.5,24.7,2.95],13)
box("body","scarf_knot",[.6,22.2,-3.3],[1.9,23.6,-2.9],13)
box("body","scarf_tail",[.9,17.6,-3.25],[2.1,22.3,-2.95],14,[0,0,-22.5],[1.5,22.3,-3.1])

# Waist: sash belt with framed buckle, coin purse (right hip), three potion vials (left hip).
box("waist","belt",[-4.75,11.3,-2.85],[4.75,12.9,2.85],7)
frame("waist","belt_buckle",-1.0,11.1,1.0,13.1,-3.15,-2.8,.35,2)
box("waist","belt_buckle_pin",[-.12,11.45,-3.1],[.12,12.75,-2.9],3)
box("waist","vest_skirt_back",[-4.6,9.4,2.0],[4.6,12.2,2.75],0)
for sign in [-1,1]:
    x0,x1=sorted([1.1*sign,4.6*sign])
    box("waist","vest_skirt_front_"+str(sign),[x0,9.4,-2.85],[x1,12.2,-2.45],11)
    s0,s1=sorted([4.25*sign,4.65*sign])
    box("waist","vest_skirt_side_"+str(sign),[s0,9.4,-2.45],[s1,12.2,2.0],0)
box("waist","purse",[4.35,8.6,-1.9],[5.65,11.4,.9],7)
box("waist","purse_neck",[4.5,11.2,-1.6],[5.5,11.8,.6],3)
box("waist","purse_tie",[4.3,11.0,-2.0],[5.7,11.3,1.0],2)
box("waist","purse_coin",[5.65,9.2,-1.3],[5.85,10.4,-.1],2)
for i,(z,col) in enumerate([(-1.9,20),(-.75,9),(.4,20)]):
    box("waist","vial_glass_"+str(i),[-5.55,9.2,z],[-4.65,10.9,z+.85],col)
    box("waist","vial_cork_"+str(i),[-5.45,10.9,z+.1],[-4.75,11.4,z+.75],3)
box("waist","vial_holder",[-5.7,9.6,-2.1],[-4.6,10.2,1.45],7)

for side,sign in [("right",1),("left",-1)]:
    m=lambda bone,name,a,b,color,angle=0,pivot=None: mirrored(side,sign,bone,name,a,b,color,angle,pivot)
    # Puffed shirt sleeve rolled to the elbow, bare forearm with a studded leather bracer.
    m("upper_arm","sleeve",[4.5,17.6,-1.85],[7.9,24,1.85],1)
    m("upper_arm","sleeve_seam",[4.45,22.6,-1.9],[7.95,22.9,1.9],1)
    m("upper_arm","sleeve_garter",[4.4,19.7,-1.95],[8.0,20.35,1.95],0)
    m("upper_arm","garter_clasp",[7.95,19.6,-.4],[8.12,20.45,.4],2)
    m("forearm","sleeve_roll",[4.35,16.5,-2.0],[8.05,18.1,2.0],1)
    m("forearm","forearm_skin",[4.75,13.6,-1.5],[7.65,16.6,1.5],4)
    m("forearm","bracer",[4.6,13.7,-1.68],[7.8,15.9,1.68],7)
    m("forearm","bracer_band_top",[4.55,15.55,-1.75],[7.85,15.85,1.75],2)
    m("forearm","bracer_band_bottom",[4.55,13.75,-1.75],[7.85,14.05,1.75],2)
    m("forearm","bracer_stud_front",[5.85,14.5,-1.85],[6.55,15.1,-1.68],2)
    m("forearm","bracer_stud_out",[7.8,14.5,-.35],[7.98,15.1,.35],2)
    m("hand","palm",[4.95,10.9,-1.3],[7.45,13.0,1.3],4)
    for i in range(4):
        x=5.0+i*.6
        m("hand","finger_"+str(i),[x,10.4,-1.75],[x+.52,11.9,-1.25],4)
    m("hand","thumb",[4.55,11.3,-1.6],[5.1,12.5,-.4],4)
    # Baggy trousers into tall boots with fold-over cuffs and a gold strap buckle.
    m("leg","trousers",[.25,6.2,-1.85],[3.95,12,1.85],16)
    m("leg","trouser_puff",[.15,6.6,-1.95],[4.05,7.6,1.95],16)
    m("leg","boot_shaft",[.25,.8,-1.95],[3.95,6.4,1.95],12)
    m("leg","boot_cuff",[.1,5.6,-2.15],[4.1,7.2,2.15],7)
    m("leg","boot_strap",[.2,3.0,-2.08],[4.0,3.5,2.08],7)
    m("leg","boot_buckle",[1.6,2.9,-2.2],[2.6,3.6,-2.05],2)
    m("leg","boot_toe",[.25,.4,-3.15],[3.95,2.2,-1.95],12)
    m("leg","toe_cap",[.35,1.4,-3.25],[3.85,2.25,-2.9],12)
    m("leg","sole",[.1,0,-3.3],[4.1,.5,2.1],8)
    m("leg","heel",[.2,0,1.0],[4.0,.9,2.15],8)

# Head: tanned portrait, broad nose, brows, curled moustache, goatee, sideburns and hair under the hat.
box("hi_head","head_skin",[-3.8,24,-3.8],[3.8,31.2,3.4],10)
box("hi_head","nose",[-.55,26.3,-4.3],[.55,27.7,-3.75],4)
box("hi_head","nose_tip",[-.45,26.1,-4.45],[.45,26.7,-4.2],4)
box("hi_head","moustache",[-1.45,25.6,-4.15],[1.45,26.3,-3.75],5)
box("hi_head","goatee",[-.9,24.0,-3.98],[.9,25.05,-3.75],5)
box("hi_head","goatee_point",[-.45,23.55,-3.95],[.45,24.05,-3.75],5)
box("hi_head","hair_back",[-4.1,24.6,2.9],[4.1,30.8,3.85],5)
box("hi_head","hair_nape",[-3.4,23.9,3.0],[3.4,24.7,3.7],5)
for sign in [-1,1]:
    s=str(sign)
    m0,m1=sorted([1.35*sign,2.55*sign])
    box("hi_head","moustache_wing_"+s,[m0,25.75,-4.1],[m1,26.4,-3.75],5)
    c0,c1=sorted([2.35*sign,2.95*sign])
    box("hi_head","moustache_curl_"+s,[c0,26.2,-4.05],[c1,27.05,-3.75],5)
    w0,w1=sorted([1.25*sign,2.95*sign])
    box("hi_head","brow_"+s,[w0,28.45,-3.98],[w1,28.95,-3.75],5)
    h0,h1=sorted([3.7*sign,4.15*sign])
    box("hi_head","hair_side_"+s,[h0,25.6,-1.1],[h1,30.8,3.0],5)
    box("hi_head","sideburn_"+s,[h0,25.0,-2.4],[h1,29.6,-1.05],5)
    e0,e1=sorted([3.85*sign,4.6*sign])
    box("hi_head","ear_"+s,[e0,26.7,-.9],[e1,28.4,.1],4)

# Wide felt hat: brim, two-step crown with a pinch, wine band and gold buckle, teal plume on the left.
box("hat","hat_brim",[-5.7,30.5,-5.9],[5.7,31.25,5.5],6)
box("hat","hat_brim_front_lip",[-4.6,30.35,-6.25],[4.6,31.0,-5.85],6)
box("hat","hat_crown",[-3.9,31.2,-3.7],[3.9,34.0,3.6],6)
box("hat","hat_crown_top",[-3.3,34.0,-2.6],[3.3,34.8,3.0],6)
box("hat","hat_pinch",[-.6,34.4,-3.2],[.6,34.9,2.4],6)
box("hat","hat_band",[-4.0,31.2,-3.8],[4.0,32.25,3.7],0)
frame("hat","hat_buckle",-.9,31.05,.9,32.4,-4.05,-3.75,.3,2)
# Plume sweeps back (+X rotation tips +Y toward +Z); a shorter side feather splays outward.
box("hat","plume_shaft",[-4.35,32.0,1.4],[-4.05,37.2,1.7],2,[22.5,0,0],[-4.2,32.0,1.55])
box("hat","plume_vane",[-4.7,32.6,.45],[-3.9,37.6,1.45],15,[22.5,0,0],[-4.2,32.0,1.55])
box("hat","plume_vane_tip",[-4.6,36.4,2.9],[-4.0,39.0,3.65],15,[45,0,0],[-4.2,36.4,3.25])
box("hat","plume_side",[-4.55,32.3,.9],[-3.95,35.9,1.6],15,[0,0,22.5],[-4.2,32.2,1.25])
box("hat","plume_clasp",[-4.65,31.5,1.0],[-3.85,32.5,2.0],9)

# Pack: wooden frame, leather body with crest flap, rolled rug on top, lantern and scroll case.
for sign in [-1,1]:
    f0,f1=sorted([3.0*sign,3.6*sign])
    box("backpack","frame_post_"+str(sign),[f0,10.6,2.75],[f1,27.4,3.35],17)
box("backpack","frame_bar_top",[-3.6,26.6,2.75],[3.6,27.2,3.35],17)
box("backpack","frame_bar_low",[-3.6,11.2,2.75],[3.6,11.8,3.35],17)
box("backpack","pack_body",[-3.3,13.2,3.35],[3.3,23.6,6.4],7)
box("backpack","pack_flap",[-3.45,17.6,6.4],[3.45,23.9,6.75],19)
box("backpack","pack_flap_top",[-3.45,23.6,3.3],[3.45,23.95,6.75],7)
box("backpack","pack_buckle",[-.5,17.3,6.7],[.5,18.3,6.95],2)
box("backpack","pack_pocket",[-2.6,13.8,6.4],[2.6,16.8,7.3],7)
box("backpack","pack_pocket_stitch",[-2.6,16.55,7.3],[2.6,16.8,7.4],2)
box("backpack","rug_roll",[-4.9,24.0,3.5],[4.9,26.4,5.9],18)
for sign in [-1,1]:
    t0,t1=sorted([2.0*sign,2.5*sign])
    box("backpack","rug_tie_"+str(sign),[t0,23.85,3.35],[t1,26.55,6.05],7)
box("backpack","scroll_case",[-2.9,24.2,6.0],[-1.9,29.2,7.0],7,[0,0,22.5],[-2.4,24.2,6.5])
box("backpack","scroll_case_cap",[-3.0,29.2,5.9],[-1.8,29.8,7.1],2,[0,0,22.5],[-2.4,24.2,6.5])
box("backpack","scroll_paper",[-2.75,29.8,6.15],[-2.05,30.6,6.85],21,[0,0,22.5],[-2.4,24.2,6.5])
box("backpack","lantern_hook",[3.6,22.0,4.4],[4.3,22.4,4.9],3)
box("backpack","lantern_bail",[4.05,20.9,4.45],[4.25,22.0,4.85],3)
box("backpack","lantern_top",[3.45,20.5,3.95],[4.85,20.95,5.35],2)
box("backpack","lantern_glass",[3.6,18.5,4.1],[4.7,20.5,5.2],9)
box("backpack","lantern_bottom",[3.45,18.1,3.95],[4.85,18.55,5.35],2)
for i,(x,z) in enumerate([(3.45,3.95),(4.6,3.95),(3.45,5.1),(4.6,5.1)]):
    box("backpack","lantern_bar_"+str(i),[x,18.55,z],[x+.25,20.5,z+.25],2)
box("backpack","pan_hang",[-4.4,19.6,4.3],[-3.3,20.0,4.7],3)
box("backpack","pan_dish",[-4.6,16.4,3.6],[-3.4,19.6,5.6],3)
box("backpack","pan_handle",[-4.3,19.6,4.45],[-3.7,22.6,4.75],17)

# Hand balance under the left fist: ring, chain, beam with end caps, strings and two pans.
box("scale","scale_ring",[-6.6,10.0,-.45],[-5.8,11.0,.45],2)
box("scale_turn","scale_chain",[-6.3,8.8,-.15],[-6.1,10.0,.15],3)
box("scale_beam","beam",[-6.35,8.25,-3.1],[-6.05,8.6,3.1],2)
box("scale_beam","beam_pivot",[-6.55,8.0,-.35],[-5.85,8.9,.35],3)
box("scale_beam","beam_pointer",[-6.3,8.9,-.1],[-6.1,9.7,.1],2)
for z,bone in [(-2.9,"scale_pan_front"),(2.9,"scale_pan_back")]:
    n=bone.split("_")[-1]
    box(bone,"pan_string_"+n,[-6.3,5.7,z-.1],[-6.1,8.25,z+.1],3)
    box(bone,"pan_"+n,[-7.3,5.2,z-1.0],[-5.1,5.7,z+1.0],2)
    box(bone,"pan_rim_"+n,[-7.4,5.6,z-1.1],[-5.0,5.8,z+1.1],3)
box("scale_pan_front","pan_gem",[-6.55,5.7,-3.15],[-5.85,6.3,-2.65],9)
box("hitbox","collision_proxy",[-4.8,0,-4.8],[4.8,34.8,4.8],8)

def rot(values): return [{"time":t,"rotation":v} for t,v in values]
def pos(values): return [{"time":t,"position":v} for t,v in values]
idle={"body":pos([(0,[0,0,0]),(2,[0,.1,0]),(4,[0,0,0])]),
      "hi_head":rot([(0,[0,0,0]),(1.2,[1,4,0]),(2.8,[-1,-3,0]),(4,[0,0,0])]),
      "backpack":rot([(0,[0,0,0]),(2,[-1,0,0]),(4,[0,0,0])]),
      "scale_beam":rot([(0,[0,0,0]),(1,[4,0,0]),(2,[0,0,0]),(3,[-4,0,0]),(4,[0,0,0])])}
for pan in ["scale_pan_front","scale_pan_back"]:
    idle[pan]=rot([(0,[0,0,0]),(1,[-4,0,0]),(2,[0,0,0]),(3,[4,0,0]),(4,[0,0,0])])
for side,sign in [("right",1),("left",-1)]:
    # Below shoulder height a positive Z rotation opens a +X arm outward; above it the sign flips.
    idle[side+"_upper_arm"]=rot([(0,[0,0,0]),(2,[1,0,sign*.8]),(4,[0,0,0])])
# Hat tip: the right hand rises to the brim and the hat lifts and dips with a small bow.
greet={"body":rot([(0,[0,0,0]),(.5,[6,0,0]),(1.2,[6,0,0]),(1.8,[0,0,0])]),
       "hi_head":rot([(0,[0,0,0]),(.5,[8,0,0]),(1.2,[8,0,0]),(1.8,[0,0,0])]),
       # Angles from a native grid search: the fist grips the right side of the brim, so the arm never crosses the face.
       "right_upper_arm":rot([(0,[0,0,0]),(.4,[125,0,-5]),(1.2,[125,0,-5]),(1.8,[0,0,0])]),
       "right_forearm":rot([(0,[0,0,0]),(.4,[60,0,0]),(1.2,[60,0,0]),(1.8,[0,0,0])]),
       "hat":pos([(0,[0,0,0]),(.4,[0,0,0]),(.6,[0,1.6,-.5]),(1.0,[0,1.6,-.5]),(1.25,[0,0,0]),(1.8,[0,0,0])])}
# Present wares: right palm forward and up, left arm lifts the balance a little.
show={"hi_head":rot([(0,[0,0,0]),(.5,[-4,-10,0]),(2.0,[-4,-10,0]),(2.6,[0,0,0])]),
      "right_upper_arm":rot([(0,[0,0,0]),(.5,[55,0,-10]),(2.0,[55,0,-10]),(2.6,[0,0,0])]),
      "right_forearm":rot([(0,[0,0,0]),(.5,[30,0,0]),(2.0,[30,0,0]),(2.6,[0,0,0])]),
      "right_hand":rot([(0,[0,0,0]),(.5,[0,0,-70]),(2.0,[0,0,-70]),(2.6,[0,0,0])]),
      "left_upper_arm":rot([(0,[0,0,0]),(.5,[20,0,0]),(2.0,[20,0,0]),(2.6,[0,0,0])]),
      "scale":rot([(0,[0,0,0]),(.5,[-20,0,0]),(2.0,[-20,0,0]),(2.6,[0,0,0])])}
# Weigh goods: the balance comes up in front, turns across the body and settles after a few tips.
weigh={"hi_head":rot([(0,[0,0,0]),(.6,[14,0,0]),(2.4,[14,0,0]),(3,[0,0,0])]),
       "left_upper_arm":rot([(0,[0,0,0]),(.6,[50,0,-10]),(2.4,[50,0,-10]),(3,[0,0,0])]),
       "left_forearm":rot([(0,[0,0,0]),(.6,[40,0,0]),(2.4,[40,0,0]),(3,[0,0,0])]),
       "scale":rot([(0,[0,0,0]),(.6,[-90,0,0]),(2.4,[-90,0,0]),(3,[0,0,0])]),
       "scale_turn":rot([(0,[0,0,0]),(.6,[0,90,0]),(2.4,[0,90,0]),(3,[0,0,0])]),
       "scale_beam":rot([(0,[0,0,0]),(.6,[0,0,0]),(.9,[14,0,0]),(1.3,[-9,0,0]),(1.7,[5,0,0]),(2.1,[0,0,0]),(3,[0,0,0])]),
       "right_upper_arm":rot([(0,[0,0,0]),(.6,[35,0,-12]),(2.4,[35,0,-12]),(3,[0,0,0])]),
       "right_forearm":rot([(0,[0,0,0]),(.6,[45,0,0]),(2.4,[45,0,0]),(3,[0,0,0])])}
for pan in ["scale_pan_front","scale_pan_back"]:
    weigh[pan]=rot([(0,[0,0,0]),(.6,[0,0,0]),(.9,[-14,0,0]),(1.3,[9,0,0]),(1.7,[-5,0,0]),(2.1,[0,0,0]),(3,[0,0,0])])
# Sale made: a little hop, arms open wide, hat bounces.
sale={"body":pos([(0,[0,0,0]),(.3,[0,.8,0]),(.6,[0,0,0]),(1.6,[0,0,0])]),
      "hi_head":rot([(0,[0,0,0]),(.3,[-10,0,0]),(.7,[6,0,0]),(1.1,[0,0,0]),(1.6,[0,0,0])]),
      "right_upper_arm":rot([(0,[0,0,0]),(.3,[15,0,35]),(1.1,[15,0,35]),(1.6,[0,0,0])]),
      "left_upper_arm":rot([(0,[0,0,0]),(.3,[15,0,-28]),(1.1,[15,0,-28]),(1.6,[0,0,0])]),
      "right_forearm":rot([(0,[0,0,0]),(.3,[25,0,0]),(1.1,[25,0,0]),(1.6,[0,0,0])]),
      "scale":rot([(0,[0,0,0]),(.3,[-15,0,28]),(1.1,[-15,0,28]),(1.6,[0,0,0])]),
      "hat":pos([(0,[0,0,0]),(.3,[0,0,0]),(.45,[0,.7,0]),(.65,[0,0,0]),(1.6,[0,0,0])])}
ANIMATIONS=[("idle",4,True,idle),("greet",1.8,False,greet),("show_wares",2.6,False,show),
            ("weigh_goods",3,False,weigh),("sale_success",1.6,False,sale)]

MATS=["wine","cloth_ivory","gold","bronze","skin_tan","hair_brown","felt","leather","dark","gem",
      "portrait","vest_front","leather_dark","teal","scarf_tail","plume","cloth_brown","wood_dark",
      "carpet_roll","merchant_crest","ruby","paper"]
OVERRIDES={}
ARTS={"portrait":portrait("skin_tan",(.46,.6),(.29,.71),.1,[(214,160,72),(150,96,40),(44,26,14),(255,250,230)]),
      "vest_front":panel("wine",vest_damask),"carpet_roll":carpet_roll,"scarf_tail":scarf_tail,
      "merchant_crest":merchant_crest}


def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("--project-uuid")
    parser.add_argument("--dry-run",action="store_true")
    args=parser.parse_args()
    assert len({r[2]["name"] for r in ROWS})==len(ROWS)
    assert all(all(a<b for a,b in zip(row["from"],row["to"])) for _,_,row in ROWS)
    assert {bone for bone,_,_ in ROWS}<={b[0] for b in BONES}
    atlas,face_uv=bake_npc(ROWS,MATS,ARTS,OVERRIDES,seed=sum(map(ord,NAME)))
    size=atlas.size[0]
    summary={"asset":NAME,"cubes":len(ROWS),"bones":len(BONES),"atlas":[size,size],"animations":[a[0] for a in ANIMATIONS]}
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
    call("risky_eval",{"code":"(()=>{Undo.initEdit({animations:Animation.all.slice()});for(const a of Animation.all){a.name=a.name.replace(/^animation\\./,'');a.snapping=20;a.override=a.name!=='idle'}Undo.finishEdit('Merchant state names 20 FPS');return Animation.all.map(a=>({name:a.name,length:a.length}))})()"})
    call("animation_timeline",{"animation_id":"idle","action":"stop"})
    call("set_mode",{"mode_id":"edit"})
    OUT.mkdir(parents=True,exist_ok=True)
    embedded(call("export_model",{"codec_id":"project","result_format":"embedded","max_content_length":2000000}),target)
    model=json.loads(target.read_text(encoding="utf-8"))
    (OUT/(NAME+".png")).write_bytes(base64.b64decode(model["textures"][0]["source"].split(",",1)[1]))
    call("create_offscreen_view",{"id":"merchant_qa","width":1400,"height":1400,"copy_view":"none"})
    try:
        for view,camera in {"preview":[60,40,-90],"front":[0,17.5,-100],"right":[-100,17.5,0],"back":[0,17.5,100]}.items():
            result=call("set_camera_angle",{"view":"merchant_qa","position":camera,"target":[0,18.5,0],"projection":"orthographic","zoom":.74})
            image=next(b for b in result["content"] if b.get("type")=="image")
            (OUT/(NAME+"-"+view+".png")).write_bytes(base64.b64decode(image["data"]))
    finally: dispose_view(client,"merchant_qa")
    print(json.dumps({"exported":str(target),**summary}),flush=True)

if __name__=="__main__": main()
