"""Build the Crystal Plaza guide (zone 01 spawn_guide) with native Blockbench MCP tools.

A young wayfinder in the plaza's heraldry: royal-blue tabard with the white banner cross, short
cape with a compass-rose back, a brass-caged crystal staff in the right fist and a folding map in
the left. Anatomical right is +X; the model faces north (-Z). The map flap hinges open on its own bone.
"""
import argparse
import base64
import json
import math
from collections import defaultdict
from blockbench_mcp import Client
from build_moonfall_boss import OUT, data, embedded, dispose_view
from build_luma_props import load_atlas
from npc_bake import bake_npc, uv_js, portrait, panel, gold_border, diamond, fill, RAMPS, KIND, ramp

NAME="npc_crystal_guide"
# 0 royal cloth, 1 ivory tunic, 2 gold, 3 bronze, 4 skin, 5 hair, 6 dark leather, 7 sole, 8 crystal,
# 9 portrait, 10 tabard (cross), 11 staff wood, 12 paper edge, 13 map art, 14 cape (compass rose),
# 15 leather, 16 trousers, 17 steel, 18 violet crystal, 19 tabard hem, 20 compass dial, 21 satchel flap
MATS=["royal","cloth_ivory","gold","bronze","skin_mid","hair_chestnut","leather_dark","dark","gem",
      "portrait","tabard","wood","paper","map","cape_rose","leather","cloth_brown","steel","violet_gem",
      "tabard_hem","compass_dial","satchel_flap"]
RAMPS.update({"royal":ramp("#0a1236","#13205a","#1c2f7a","#27409a","#3655b5","#4f72cc","#7898e0"),
              "hair_chestnut":ramp("#2a1208","#3f1c0c","#572812","#70361a","#8a4723","#a65d31","#c27a46"),
              "leaf":ramp("#1b2e12","#2a4519","#3a5e22","#4d772c","#64903a","#83ab50","#a8c76e")})
KIND.update({"royal":"cloth","hair_chestnut":"hair","leaf":"cloth"})
SX=6.2
BONES=[("motion_root",[0,0,0],"root"),("body",[0,12,0],"motion_root"),
       ("waist",[0,12,0],"body"),("hi_head",[0,24,0],"body"),("cape",[0,23.6,3.0],"body")]
for side,sign in [("right",1),("left",-1)]:
    BONES += [(side+"_upper_arm",[SX*sign,23,0],"body"),
              (side+"_forearm",[SX*sign,17.7,0],side+"_upper_arm"),
              (side+"_hand",[SX*sign,13.3,-.3],side+"_forearm"),
              (side+"_leg",[2.1*sign,12,0],"motion_root")]
# The staff pivots at the grip so it can be re-aimed inside the fist; the map pivots where it is pinched.
BONES += [("staff",[SX,12.4,-2.65],"right_hand"),("staff_crystal",[SX,30.2,-2.65],"staff"),
          ("map",[-SX,12.4,-1.8],"left_hand"),("map_flap",[-6.35,9.2,-6.4],"map"),
          ("hitbox",[0,28,0],"root")]
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

# Torso: ivory tunic, heraldic tabard front and back, collar and a crystal cape clasp.
box("body","tunic_torso",[-4.2,12,-2.5],[4.2,23.6,2.5],1)
box("body","tabard_front",[-2.9,12.9,-2.85],[2.9,22.6,-2.5],10)
box("body","tabard_back",[-2.9,12.9,2.5],[2.9,22.6,2.85],0)
box("body","tabard_shoulder_front",[-3.3,22.4,-2.95],[3.3,23.1,-2.45],2)
box("body","collar",[-2.7,23.2,-2.8],[2.7,24.5,2.75],1)
box("body","collar_trim",[-2.8,23.15,-2.95],[2.8,23.5,-2.75],2)
box("body","mantle",[-4.6,21.2,-2.9],[4.6,24.1,3.1],0)
box("body","mantle_trim_front",[-4.65,21.0,-3.0],[4.65,21.4,-2.75],2)
for sign in [-1,1]:
    x0,x1=sorted([1.0*sign,2.0*sign])
    box("body","clasp_disc_"+str(sign),[x0,22.55,-3.25],[x1,23.55,-2.9],2)
    x0,x1=sorted([1.25*sign,1.75*sign])
    box("body","clasp_inset_"+str(sign),[x0,22.8,-3.38],[x1,23.3,-3.2],8)
box("body","clasp_chain",[-1.05,22.95,-3.12],[1.05,23.15,-2.95],3)
box("body","clasp_crystal",[-.45,22.55,-3.5],[.45,23.45,-3.05],8,[0,0,45],[0,23,-3.3])
for y in [14.2,16.4]: box("body","tunic_side_lace_"+str(y),[4.2,y,-.4],[4.35,y+.9,.4],3)

# Short cape on its own bone (sways in idle); hood lies folded at the nape.
box("cape","cape_panel",[-4.4,7.2,2.95],[4.4,23.4,3.6],14)
box("cape","cape_hem",[-4.5,6.8,2.9],[4.5,7.3,3.7],2)
for sign in [-1,1]:
    x0,x1=sorted([4.25*sign,4.55*sign])
    box("cape","cape_edge_"+str(sign),[x0,7.3,2.9],[x1,23.2,3.7],2)
box("cape","hood_roll",[-3.6,22.6,3.05],[3.6,25.0,4.7],0)
box("cape","hood_lining",[-3.2,24.7,3.2],[3.2,25.2,4.5],1)
box("cape","hood_point",[-1.7,18.6,3.55],[1.7,22.7,4.45],0)
box("cape","hood_tip",[-.8,17.6,3.6],[.8,18.7,4.35],0)
box("cape","hood_tassel",[-.35,16.6,3.75],[.35,17.7,4.2],2)
box("cape","hood_roll_piping",[-3.65,22.45,4.6],[3.65,22.8,4.8],2)
for sign in [-1,1]:
    x0,x1=sorted([1.65*sign,1.85*sign])
    box("cape","hood_piping_"+str(sign),[x0,18.6,4.4],[x1,22.5,4.6],2)

# Belt, tabard skirt, hip compass, scroll case and a map satchel.
box("waist","belt",[-4.5,11.45,-2.8],[4.5,12.85,2.8],6)
frame("waist","belt_buckle",-1,11.25,1,13.1,-3.2,-2.75,.3)
box("waist","buckle_pin",[-.1,11.55,-3.15],[.1,12.8,-2.95],3)
box("waist","tabard_skirt",[-2.9,7.0,-3.0],[2.9,11.5,-2.6],19)
box("waist","tabard_skirt_back",[-2.9,7.6,2.6],[2.9,11.5,2.95],0)
for x in [-3.7,3.2]: box("waist","belt_stud_"+str(x),[x,11.75,-2.98],[x+.5,12.55,-2.78],2)
box("waist","compass_chain",[-3.75,10.9,-3.12],[-3.55,11.5,-2.92],2)
box("waist","compass_case",[-4.35,9.4,-3.35],[-2.95,10.95,-2.85],17)
box("waist","compass_rim",[-4.45,10.85,-3.3],[-2.85,11.05,-2.9],2)
box("waist","compass_dial",[-4.15,9.6,-3.45],[-3.15,10.75,-3.33],20)
box("waist","compass_bow",[-3.85,11.0,-3.2],[-3.45,11.35,-3.0],2)
box("waist","scroll_case",[4.15,7.3,-1.0],[5.35,12.2,.3],15)
for y in [7.1,11.9]: box("waist","scroll_case_cap_"+str(y),[4.05,y,-1.1],[5.45,y+.5,.4],3)
box("waist","scroll_case_strap",[4.1,9.3,-1.05],[5.4,9.8,.35],6)
box("waist","scroll_rolled",[4.4,12.4,-.75],[5.1,13.1,.05],12)
box("waist","satchel",[-5.0,7.6,-1.2],[-3.6,11.2,2.2],15)
box("waist","satchel_flap",[-5.15,9.2,-1.35],[-4.9,11.35,2.35],21)
box("waist","satchel_button",[-5.35,9.35,.25],[-5.12,9.95,.75],8)

for side,sign in [("right",1),("left",-1)]:
    m=lambda bone,name,a,b,col:mirror(side,sign,bone,name,a,b,col)
    m("upper_arm","sleeve",[4.25,17.7,-1.85],[8,23.6,1.85],1)
    m("upper_arm","capelet",[4.15,20.9,-2.15],[8.2,24.1,2.15],0)
    m("upper_arm","capelet_trim",[4.05,20.7,-2.2],[8.25,21.05,2.2],2)
    m("upper_arm","capelet_stud",[7.95,22.2,-.45],[8.35,23.1,.45],2)
    m("upper_arm","elbow_band",[4.2,17.9,-1.95],[8.05,18.6,1.95],0)
    m("forearm","lower_sleeve",[4.6,14.3,-1.65],[7.8,17.85,1.65],1)
    m("forearm","bracer",[4.45,13.3,-1.95],[7.95,15.5,1.95],6)
    m("forearm","bracer_trim",[4.35,15.3,-2.05],[8.05,15.6,2.05],2)
    m("forearm","bracer_buckle",[7.9,13.75,-.5],[8.15,14.75,.5],2)
    m("forearm","bracer_gem",[8.1,14.0,-.25],[8.3,14.5,.25],8)
    m("hand","glove",[4.95,11.6,-1.3],[7.45,13.3,1.15],6)
    # Curled fingers stack in Y and wrap the front of whatever the fist holds (staff or map edge).
    for i in range(4):
        y=11.25+i*.5
        m("hand","finger_"+str(i),[5.05,y,-3.15],[7.3,y+.44,-1.25],4)
    m("hand","thumb",[4.6,12.35,-3.0],[5.3,13.25,-1.0],4)
    m("hand","glove_cuff",[4.85,12.9,-1.4],[7.55,13.35,1.25],15)
    m("leg","trousers",[.65,3,-1.5],[3.55,12,1.5],16)
    m("leg","boot",[.55,.7,-1.7],[3.65,6.4,1.8],6)
    m("leg","boot_cuff",[.35,5.9,-1.9],[3.85,7.0,2.0],15)
    m("leg","boot_strap",[.4,3.55,-1.94],[3.8,4.2,-1.7],6)
    m("leg","boot_buckle",[1.55,3.65,-2.08],[2.65,4.25,-1.9],2)
    m("leg","toe",[.4,.45,-3.05],[3.8,2.3,-1.55],6)
    m("leg","toe_trim",[.45,.95,-3.15],[3.75,1.3,-3],2)
    m("leg","sole",[.3,0,-3.2],[3.9,.55,2],7)

# Head: painted face, neat side-parted chestnut hair, small crystal stud in one ear.
box("hi_head","head",[-3.8,24,-3.75],[3.8,31.65,3.05],9)
box("hi_head","nose",[-.35,26.7,-4.15],[.35,28.2,-3.73],4)
box("hi_head","hair_cap",[-4.05,30.35,-3.9],[4.05,32.25,3.45],5)
box("hi_head","hair_crown",[-3.65,32.18,-3.2],[3.45,32.85,2.95],5)
box("hi_head","hair_back",[-4.05,25.55,2.7],[4.05,31.2,3.65],5)
box("hi_head","hair_nape",[-3.45,25.28,2.85],[3.45,25.85,3.5],5)
box("hi_head","hair_crown_sweep_right",[-3.25,32.75,-2.8],[-.55,33.2,2.2],5)
box("hi_head","hair_crown_sweep_left",[-.65,32.70,-2.4],[2.6,33.0,2.5],5)
for sign in [-1,1]:
    x=sign*3.85
    box("hi_head","temple_"+str(sign),[x-.4,28.1,-3.85],[x+.4,31.75,3.0],5)
    x0,x1=sorted([3.75*sign,4.25*sign])
    box("hi_head","ear_"+str(sign),[x0,26.6,-.7],[x1,28.5,.7],4)
    box("hi_head","sideburn_"+str(sign),[sign*3.85-.3,27.0,-2.9],[sign*3.85+.3,28.3,-1.2],5)
box("hi_head","ear_crystal",[-4.45,26.55,-.2],[-4.1,27.05,.25],8)
# Adjacent locks meet at their X edges; they never overlap on a coplanar north face.
# Staggered tips open the brow and define a gentle part rather than hiding the eyes.
for i,(x0,x1,y,z,top) in enumerate([(-4.0,-2.65,29.55,-4.24,31.8),
                                  (-2.65,-1.2,29.9,-4.22,31.95),
                                  (-1.2,.1,30.2,-4.16,32.02),
                                  (.1,1.5,29.9,-4.25,32.0),
                                  (1.5,2.85,29.6,-4.30,31.9),
                                  (2.85,4.0,29.45,-4.23,31.65)]):
    box("hi_head","fringe_"+str(i),[x0,y,z],[x1,top,-3.8],5)

# Crystal staff: wrapped grip, bronze bands, brass cage holding a glowing cyan crystal.
box("staff","shaft_low",[5.9,1.2,-2.1],[6.5,10.6,-1.5],11)
box("staff","shaft_high",[5.9,14.2,-2.1],[6.5,27.3,-1.5],11)
box("staff","grip_wrap",[5.8,10.6,-2.2],[6.6,14.2,-1.4],15)
for y in [10.4,14.1]: box("staff","grip_ring_"+str(y),[5.75,y,-2.25],[6.65,y+.3,-1.35],3)
for y in [5.0,20.4,24.6]: box("staff","shaft_band_"+str(y),[5.8,y,-2.2],[6.6,y+.5,-1.4],3)
box("staff","ferrule",[5.8,.4,-2.2],[6.6,1.3,-1.4],3)
box("staff","ferrule_tip",[5.95,0,-2.05],[6.45,.45,-1.55],17)
box("staff","cage_collar",[5.55,27.2,-2.45],[6.85,28.0,-1.15],2)
for i,(x,z) in enumerate([(5.45,-2.55),(6.65,-2.55),(5.45,-1.35),(6.65,-1.35)]):
    box("staff","cage_prong_"+str(i),[x,28,z],[x+.3,32.3,z+.3],2)
box("staff","cage_ring",[5.45,29.9,-2.55],[6.95,30.2,-1.05],3)
box("staff","cage_cap",[5.55,32.2,-2.45],[6.85,32.8,-1.15],2)
box("staff","cage_finial",[5.95,32.8,-2.05],[6.45,33.7,-1.55],2,[0,45,0],[6.2,33.25,-1.8])
box("staff_crystal","crystal_body",[5.72,28.5,-2.28],[6.68,31.9,-1.32],8,[0,45,0],[6.2,30.2,-1.8])
box("staff_crystal","crystal_shard",[6.05,29.0,-2.0],[6.75,30.9,-1.6],18,[0,0,22.5],[6.4,30,-1.8])
# Authored on the z=-1.8 column, then moved forward so the staff clears the capelet and bracer.
for _bone,_col,_row in ROWS:
    if _bone in ("staff","staff_crystal"):
        _row["from"][2]-=.85; _row["to"][2]-=.85
        if "origin" in _row: _row["origin"][2]-=.85

# Folding map pinched at its back corner in the left fist; the flap hinges on the far (front) edge,
# so opening it swings the flap away from the hand instead of through the wrist.
box("map","map_leaf",[-6.35,5.6,-6.4],[-6.15,12.8,-1.6],13)
box("map","map_leaf_edge",[-6.38,12.5,-6.45],[-6.12,12.85,-1.55],12)
box("map_flap","map_flap_leaf",[-6.55,5.6,-6.4],[-6.35,12.8,-1.6],13)
box("map_flap","map_seal",[-6.75,8.4,-4.5],[-6.55,9.3,-3.6],2)
box("map_flap","map_ribbon",[-6.62,5.2,-4.2],[-6.5,8.5,-3.9],3)
box("hitbox","collision_proxy",[-4.8,0,-4.8],[4.8,34.4,4.8],7)

def tabard(c,face,rng):
    """Front: gold-ruled royal blue with the plaza's white banner cross and a crystal at the crossing."""
    gold_border(c,1)
    if face!="north": return
    iv=RAMPS["cloth_ivory"]; ro=RAMPS["royal"]; gem=RAMPS["gem"]
    bw=max(3,c.w//6); cx=c.w//2; cy=int(c.h*.38)
    def bar(x0,y0,x1,y1):
        for y in range(y0-1,y1+1):
            for x in range(x0-1,x1+1): c.set(x,y,ro[1])
        for y in range(y0,y1):
            for x in range(x0,x1): c.set(x,y,iv[5] if x==x0 else (iv[3] if x==x1-1 or y==y1-1 else iv[4]))
    bar(cx-bw//2,3,cx-bw//2+bw,c.h-3)
    bar(3,cy-bw//2,c.w-3,cy-bw//2+bw)
    for y in range(cy-bw//2,cy-bw//2+bw):
        for x in range(cx-bw//2,cx-bw//2+bw): c.set(x,y,iv[4])
    diamond(c,cx,cy,max(2,bw//2+1),gem[2],gem[4]); c.set(cx-1,cy-1,gem[6])

def tabard_skirt(c,face,rng):
    """Skirt front: the cross's foot continues down to a gold diamond above a fringed gold hem."""
    if face!="north": return
    iv=RAMPS["cloth_ivory"]; ro=RAMPS["royal"]; g=RAMPS["gold"]
    bw=max(3,c.w//6); cx=c.w//2; hem=c.h-3
    for y in range(0,hem-4):
        for x in range(cx-bw//2-1,cx-bw//2+bw+1): c.set(x,y,ro[1])
        for x in range(cx-bw//2,cx-bw//2+bw): c.set(x,y,iv[5] if x==cx-bw//2 else iv[4])
    diamond(c,cx,hem-4,2,g[5],g[3])
    for x in range(c.w): c.set(x,hem,g[4]); c.set(x,hem+1,g[2]); c.set(x,hem+2,g[4] if x%2==0 else ro[1])
    for y in range(hem): c.set(1,y,g[3]); c.set(c.w-2,y,g[3])

def cape_rose(c,face,rng):
    """Cape back: compass rose (ivory cardinal points, gold ring, crystal centre) above a gold hem."""
    if face!="south": return
    iv=RAMPS["cloth_ivory"]; g=RAMPS["gold"]; gem=RAMPS["gem"]; ro=RAMPS["royal"]
    for x in range(c.w): c.set(x,c.h-2,g[4]); c.set(x,c.h-3,g[2])
    cx,cy=(c.w-1)/2,c.h*.6; R=min(c.w,c.h)*.34
    for y in range(c.h):
        for x in range(c.w):
            dx,dy=x-cx,y-cy; d=math.hypot(dx,dy)
            if R*.78<=d<=R*.78+1.1: c.set(x,y,g[4] if dy<0 else g[3])
            diag=abs(dx+dy)/R/.24+abs(dx-dy)/R/.95<=1 or abs(dx-dy)/R/.24+abs(dx+dy)/R/.95<=1
            if diag: c.set(x,y,ro[5])
            if abs(dx)/(R*.22)+abs(dy)/(R*1.2)<=1: c.set(x,y,iv[5] if dx<0 else iv[3])      # north/south point
            elif abs(dx)/(R*1.2)+abs(dy)/(R*.22)<=1: c.set(x,y,iv[5] if dy<0 else iv[3])    # east/west point
    diamond(c,int(cx),int(cy),max(2,int(R*.2)),gem[2],gem[4]); c.set(int(cx)-1,int(cy)-1,gem[6])
    nx=int(cx)
    for y in range(int(cy-R*1.42),int(cy-R*1.25)): c.set(nx,y,g[5])

def map_art(c,face,rng):
    """Parchment with a coast, forest dots, a dashed route and the crystal plaza marked in cyan."""
    fill(c,"paper",face,rng)
    if c.w<10 or c.h<10: return
    p=RAMPS["paper"]; sea=RAMPS["ice"]; ru=RAMPS["ruby"]; lf=RAMPS["leaf"]; st=RAMPS["stone"]; gem=RAMPS["gem"]
    for x in range(c.w): c.set(x,1,p[1]); c.set(x,c.h-2,p[1])
    for y in range(c.h): c.set(1,y,p[1]); c.set(c.w-2,y,p[1])
    for y in range(2,c.h-2):
        coast=int(c.w*.24+math.sin(y*.45)*1.6)
        for x in range(2,coast): c.set(x,y,sea[2] if (x+y)%5 else sea[1])
        c.set(coast,y,sea[0])
    for fx,fy in [(.62,.2),(.72,.26),(.66,.32),(.8,.18)]:
        x,y=int(fx*c.w),int(fy*c.h); c.set(x,y,lf[4]); c.set(x+1,y,lf[3]); c.set(x,y+1,lf[2])
    for mx,my in [(.75,.7),(.86,.66)]:
        x,y=int(mx*c.w),int(my*c.h)
        for k in range(3): c.set(x-k,y+k,st[4]); c.set(x+k,y+k,st[1])
    tx,ty=int(c.w*.55),int(c.h*.45)
    pts=[(int(c.w*.4),c.h-4),(int(c.w*.48),int(c.h*.72)),(int(c.w*.38),int(c.h*.58)),(tx,ty)]
    for (x0,y0),(x1,y1) in zip(pts,pts[1:]):
        n=max(abs(x1-x0),abs(y1-y0),1)
        for i in range(n):
            if i%3!=2: c.set(round(x0+(x1-x0)*i/n),round(y0+(y1-y0)*i/n),ru[3])
    diamond(c,tx,ty,2,gem[2],gem[4]); c.set(tx,ty,gem[6])

def compass_dial(c,face,rng):
    fill(c,"steel",face,rng)
    if face!="north": return
    iv=RAMPS["cloth_ivory"]; ru=RAMPS["ruby"]; g=RAMPS["gold"]
    cx,cy=(c.w-1)/2,(c.h-1)/2
    for y in range(c.h):
        for x in range(c.w):
            if ((x-cx)/(c.w/2))**2+((y-cy)/(c.h/2))**2<=.8: c.set(x,y,iv[5])
    ix,iy=int(round(cx)),int(round(cy))
    for y in range(1,iy): c.set(ix,y,ru[4])
    for y in range(iy+1,c.h-1): c.set(ix,y,RAMPS["steel"][2])
    c.set(ix,iy,g[5])

def satchel_flap(c,face,rng):
    fill(c,"leather",face,rng)
    if face!="west" or c.w<8: return
    g=RAMPS["gold"]
    for x in range(1,c.w-1): c.set(x,c.h-2,g[3])
    for y in range(1,c.h-2): c.set(1,y,g[3]); c.set(c.w-2,y,g[3])

base_face=portrait("skin_mid",(.45,.58),(.29,.71),.1,
                   [(92,182,226),(38,112,170),(16,38,66),(240,252,255)],young=True,mouth=.79)
def guide_face(c,face,rng):
    """Portrait plus chestnut brows and a few freckles across the nose."""
    base_face(c,face,rng)
    if face!="north": return
    h=RAMPS["hair_chestnut"]; s=RAMPS["skin_mid"]; w=c.w; y0=int(.45*c.h)
    for cxf,out in ((.29,-1),(.71,1)):
        cx=int(cxf*w); hw=max(3,int(.1*w))
        for x in range(cx-hw,cx+hw+1): c.set(x,y0-3,h[3])
        c.set(cx+out*(hw+1),y0-2,h[3])
    for fx,fy in [(.36,.66),(.41,.69),(.59,.69),(.64,.66),(.39,.72),(.62,.72)]: c.set(int(fx*w),int(fy*c.h),s[2])

def guide_hair(c,face,rng):
    """Broad chestnut locks with restrained strand highlights, not alternating bright stripes."""
    r=RAMPS["hair_chestnut"]
    base=4 if face=="up" else (2 if face=="down" else 3)
    for y in range(c.h):
        for x in range(c.w):
            # Front locks gently sweep away from the part; crown grain runs front to back.
            shift=(c.h-1-y)//4 if face in ("north","south") else 0
            band=(x+shift)%9
            tone=base+(1 if band in (2,3) else (-1 if band==8 else 0))
            if face not in ("up","down") and y>=c.h-2:
                tone-=1
            c.set(x,y,r[max(0,min(6,tone))])

ARTS={"hair_chestnut":guide_hair,"portrait":guide_face,"tabard":panel("royal",tabard),"tabard_hem":panel("royal",tabard_skirt),
      "cape_rose":panel("royal",cape_rose),"map":map_art,"compass_dial":compass_dial,"satchel_flap":satchel_flap}

def rot(items):return [{"time":t,"rotation":v} for t,v in items]
def pos(items):return [{"time":t,"position":v} for t,v in items]
# Rotation signs measured in native Blockbench: +X pitches a bone's top backward (arms swing forward,
# head looks up), +Y turns the face toward the character's left (-X), +Z opens the right arm outward.
idle={"body":pos([(0,[0,0,0]),(2,[0,.08,0]),(4,[0,0,0])]),
      "hi_head":rot([(0,[0,0,0]),(1.2,[1,4,0]),(2.8,[-1,-4,0]),(4,[0,0,0])]),
      "cape":rot([(0,[0,0,0]),(2,[-3,0,0]),(4,[0,0,0])]),
      "staff_crystal":pos([(0,[0,0,0]),(2,[0,.3,0]),(4,[0,0,0])]),
      "left_upper_arm":rot([(0,[0,0,0]),(2,[1,0,-.8]),(4,[0,0,0])])}
GREET_ARM=[0,0,-140]
greet={"hi_head":rot([(0,[0,0,0]),(.4,[2,8,4]),(1.3,[2,8,4]),(1.8,[0,0,0])]),
       "left_upper_arm":rot([(0,[0,0,0]),(.4,GREET_ARM),(1.3,GREET_ARM),(1.8,[0,0,0])]),
       "left_forearm":rot([(0,[0,0,0]),(.4,[0,0,0]),(.6,[0,0,-22]),(.85,[0,0,14]),(1.1,[0,0,-22]),(1.3,[0,0,0]),(1.8,[0,0,0])]),
       "cape":rot([(0,[0,0,0]),(.4,[-4,0,0]),(1.3,[-4,0,0]),(1.8,[0,0,0])])}
POINT_ARM=[80,0,-25]
point={"hi_head":rot([(0,[0,0,0]),(.5,[0,22,0]),(1.9,[0,22,0]),(2.4,[0,0,0])]),
       "body":rot([(0,[0,0,0]),(.5,[0,8,0]),(1.9,[0,8,0]),(2.4,[0,0,0])]),
       "left_upper_arm":rot([(0,[0,0,0]),(.5,POINT_ARM),(1.9,POINT_ARM),(2.4,[0,0,0])]),
       "left_forearm":rot([(0,[0,0,0]),(.5,[8,0,0]),(1.9,[8,0,0]),(2.4,[0,0,0])]),
       "map":rot([(0,[0,0,0]),(.5,[0,0,0]),(1.9,[0,0,0]),(2.4,[0,0,0])]),
       "staff_crystal":pos([(0,[0,0,0]),(.5,[0,.35,0]),(1.9,[0,.35,0]),(2.4,[0,0,0])])}
# Native grid search (map-to-body point tests at 9 transition frames all zero): the open map stands
# upright in front of the chest, spans X and faces the eyes (|normal.to_eye| 0.89).
SHOW_ARM=[60,0,10]; SHOW_FORE=[20,0,0]; SHOW_MAP=[-75,15,90]
show={"hi_head":rot([(0,[0,0,0]),(.5,[-14,6,0]),(2.3,[-14,6,0]),(2.8,[0,0,0])]),
      "left_upper_arm":rot([(0,[0,0,0]),(.5,SHOW_ARM),(2.3,SHOW_ARM),(2.8,[0,0,0])]),
      "left_forearm":rot([(0,[0,0,0]),(.5,SHOW_FORE),(2.3,SHOW_FORE),(2.8,[0,0,0])]),
      "map":rot([(0,[0,0,0]),(.5,SHOW_MAP),(2.3,SHOW_MAP),(2.8,[0,0,0])]),
      "map_flap":rot([(0,[0,0,0]),(.5,[0,0,0]),(.9,[0,-180,0]),(1.9,[0,-180,0]),(2.3,[0,0,0]),(2.8,[0,0,0])])}
# The staff swings out to the side and is counter-rotated in the fist so it stays upright while the crystal turns.
WEL_ARM=[15,0,35]; WEL_STAFF=[-15,0,-35]
welcome={"body":rot([(0,[0,0,0]),(.5,[3,0,0]),(1.5,[3,0,0]),(2.2,[0,0,0])]),
         "hi_head":rot([(0,[0,0,0]),(.5,[6,0,0]),(1.5,[6,0,0]),(2.2,[0,0,0])]),
         "right_upper_arm":rot([(0,[0,0,0]),(.5,WEL_ARM),(1.5,WEL_ARM),(2.2,[0,0,0])]),
         "staff":rot([(0,[0,0,0]),(.5,WEL_STAFF),(1.5,WEL_STAFF),(2.2,[0,0,0])]),
         "staff_crystal":rot([(0,[0,0,0]),(.5,[0,45,0]),(1.0,[0,90,0]),(1.5,[0,45,0]),(2.2,[0,0,0])]),
         "left_upper_arm":rot([(0,[0,0,0]),(.5,[10,0,-35]),(1.5,[10,0,-35]),(2.2,[0,0,0])]),
         "cape":rot([(0,[0,0,0]),(.5,[-6,0,0]),(1.5,[-6,0,0]),(2.2,[0,0,0])])}
ANIMATIONS=[("idle",4,True,idle),("greet",1.8,False,greet),("point_direction",2.4,False,point),
            ("show_map",2.8,False,show),("welcome",2.2,False,welcome)]

def main():
    parser=argparse.ArgumentParser();parser.add_argument("--project-uuid");parser.add_argument("--dry-run",action="store_true");parser.add_argument("--atlas-out")
    args=parser.parse_args()
    assert len({row["name"] for _,_,row in ROWS})==len(ROWS)
    assert all(all(a<b for a,b in zip(row["from"],row["to"])) for _,_,row in ROWS)
    assert {bone for bone,_,_ in ROWS}<={b[0] for b in BONES}
    atlas,face_uv=bake_npc(ROWS,MATS,ARTS,seed=sum(map(ord,NAME)));size=atlas.size[0]
    summary={"asset":NAME,"cubes":len(ROWS),"bones":len(BONES),"atlas":[size,size],"animations":[a[0] for a in ANIMATIONS]}
    if args.dry_run:
        if args.atlas_out: atlas.save(args.atlas_out)
        print(json.dumps(summary));return
    if not args.project_uuid:parser.error("--project-uuid is required")
    target=OUT/(NAME+".bbmodel")
    if target.exists():raise RuntimeError("Revision exists; create a new revision instead")
    client=Client()
    def guard():
        info=data(client.call("get_project_info",{}));p=info["project"]
        if p["name"]!=NAME or p["uuid"]!=args.project_uuid:raise RuntimeError("Active project changed")
        return info
    def call(name,arguments):guard();return client.call(name,arguments)
    if any(guard()["counts"].get(k,0) for k in ["cubes","meshes","groups","textures"]):raise RuntimeError("Project must be empty")
    call("set_mode",{"mode_id":"edit"})
    call("create_texture",{"name":NAME+".png","width":size,"height":size,"uv_width":size,"uv_height":size,"fill_color":"#000000"})
    call("risky_eval",{"code":"(()=>{Undo.initEdit({uv_mode:true});Project.texture_width=%d;Project.texture_height=%d;Undo.finishEdit('Guide UV size');return true})()"%(size,size)})
    load_atlas(call,atlas)
    for name,pivot,parent in BONES:call("add_group",{"name":name,"origin":pivot,"parent":parent})
    batches=defaultdict(list)
    for bone,_,row in ROWS:batches[bone].append(row)
    for bone,rows in batches.items():
        call("place_cube",{"elements":rows,"group":bone,"texture":NAME+".png",
                          "faces":[{"face":f,"uv":[0,0,1,1]} for f in ["north","south","east","west","up","down"]]})
    call("risky_eval",{"code":uv_js(face_uv)})
    print(json.dumps({"geometry":summary}),flush=True)
    call("set_mode",{"mode_id":"animate"})
    for name,length,loop,bones in ANIMATIONS:call("create_animation",{"name":name,"animation_length":length,"loop":loop,"bones":bones})
    call("risky_eval",{"code":"(()=>{Undo.initEdit({animations:Animation.all.slice()});for(const a of Animation.all){a.name=a.name.replace(/^animation\\./,'');a.snapping=20;a.override=a.name!=='idle'}Undo.finishEdit('Guide state names 20 FPS');return true})()"})
    call("animation_timeline",{"animation_id":"idle","action":"stop"});call("set_mode",{"mode_id":"edit"})
    embedded(call("export_model",{"codec_id":"project","result_format":"embedded","max_content_length":2000000}),target)
    model=json.loads(target.read_text(encoding="utf-8"))
    (OUT/(NAME+".png")).write_bytes(base64.b64decode(model["textures"][0]["source"].split(",",1)[1]))
    call("create_offscreen_view",{"id":"guide_qa","width":1400,"height":1400,"copy_view":"none"})
    try:
        for view,camera in {"preview":[60,40,-90],"front":[0,17.5,-100],"right":[100,17.5,0],"back":[0,17.5,100]}.items():
            result=call("set_camera_angle",{"view":"guide_qa","position":camera,"target":[0,17.5,0],"projection":"orthographic","zoom":.74})
            image=next(b for b in result["content"] if b.get("type")=="image")
            (OUT/(NAME+"-"+view+".png")).write_bytes(base64.b64decode(image["data"]))
    finally:dispose_view(client,"guide_qa")
    print(json.dumps({"exported":str(target),**summary}),flush=True)

if __name__=="__main__":main()
