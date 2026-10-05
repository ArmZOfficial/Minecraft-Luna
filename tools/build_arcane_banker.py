"""Create banker v2 through native Blockbench MCP, preserving earlier revisions."""
import argparse
import base64
import json
from collections import defaultdict
from blockbench_mcp import Client
from build_moonfall_boss import OUT, data, embedded, dispose_view

NAME = "npc_arcane_banker_v2"
COLORS = ["#195354", "#0e333b", "#eee1bd", "#dbb354", "#957138",
          "#d6aa87", "#d5dedb", "#674638", "#272c31", "#47ded4",
          "#d6aa87", "#16494c", "#173e46", "#ded6b8", "#dbb354", "#11373f"]
BONES = [("motion_root", [0,0,0], "root"), ("body", [0,13,0], "motion_root"),
         ("waist", [0,12,0], "body"), ("hi_head", [0,24,0], "body")]
for side, sign in [("right",-1),("left",1)]:
    BONES += [(side+"_upper_arm", [6.3*sign,22,0], "body"),
              (side+"_forearm", [6.3*sign,17,0], side+"_upper_arm"),
              (side+"_hand", [6.3*sign,13.3,-.8], side+"_forearm"),
              (side+"_leg", [2.1*sign,12,0], "motion_root"),
              (side+"_coattail", [2.4*sign,12,2.5], "waist")]
BONES += [("ledger", [7.6,14.5,-3.4], "left_hand"),
          ("coin", [-6.3,12.7,-3.1], "right_hand"), ("hitbox", [0,28.4,0], "root")]
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

# Tailored coat, inset waistcoat, turned lapels and separate shoulder hardware.
box("body","coat_torso",[-4.2,12,-2.3],[4.2,23.3,2.4],0)
box("body","waistcoat",[-2.7,13,-2.65],[2.7,22.5,-2.28],11)
box("body","collar_back",[-3.4,22.7,1.5],[3.4,24.2,2.8],2)
box("body","shirt_neck",[-1.6,22.1,-2.8],[1.6,24.5,1.5],2)
for sign in [-1,1]:
    x=sign*2.9
    box("body","ivory_lapel_"+str(sign),[x-.65,18.4,-3],[x+.65,23.6,-2.35],2,[0,0,-22.5*sign],[x,22,-2.5])
    box("body","lapel_gold_seam_"+str(sign),[x-.12,18.3,-3.16],[x+.12,23.7,-2.95],3,[0,0,-22.5*sign],[x,22,-2.5])
    box("body","side_panel_"+str(sign),[sign*3.4-.7,13,-2.65],[sign*3.4+.7,18.3,-2.25],11)
for y in [14.2,16.1,18]:
    box("body","vest_button_"+str(y),[-.25,y,-2.95],[.25,y+.5,-2.6],3)
for x,y,angle in [(-1.8,22,22.5),(-1.2,21.4,22.5),(-.6,20.8,22.5),(1.8,22,-22.5),(1.2,21.4,-22.5),(.6,20.8,-22.5)]:
    box("body","chain_"+str(x),[x-.22,y-.25,-3.22],[x+.22,y+.25,-2.92],3,[0,0,angle],[x,y,-3.1])
box("body","medallion_frame",[-.85,19.5,-3.4],[.85,21.2,-2.9],4,[0,0,45],[0,20.35,-3.1])
box("body","medallion_gold",[-.65,19.7,-3.55],[.65,21,-3.35],3,[0,0,45],[0,20.35,-3.45])
box("body","medallion_gem",[-.3,20,-3.75],[.3,20.6,-3.5],9,[0,0,45],[0,20.3,-3.65])
box("body","back_embroidered_panel",[-3.4,13.3,2.41],[3.4,22.5,2.55],15)

# Belt, framed crystal buckle, leather coin pouch and an actual vault key.
box("waist","leather_belt",[-4.5,11.8,-2.75],[4.5,13.2,2.75],7)
for name,a,b in [("top",[-1.1,12.85,-3.15],[1.1,13.25,-2.7]),("bottom",[-1.1,11.65,-3.15],[1.1,12.05,-2.7]),
                 ("left",[-1.1,12,-3.15],[-.7,12.9,-2.7]),("right",[.7,12,-3.15],[1.1,12.9,-2.7])]:
    box("waist","buckle_"+name,a,b,3)
box("waist","buckle_crystal",[-.65,12.08,-3.3],[.65,12.8,-2.9],9)
box("waist","coin_pouch",[-5.25,9.6,-2.2],[-3.9,12.1,-.2],7)
box("waist","pouch_flap",[-5.35,11.2,-2.45],[-3.8,12.2,-2.1],1)
box("waist","pouch_clasp",[-4.8,11.25,-2.6],[-4.3,11.9,-2.35],3)
for name,a,b in [("bow_top",[3.3,11.5,-3.2],[4.3,11.75,-2.95]),("bow_left",[3.3,10.8,-3.2],[3.55,11.5,-2.95]),
                 ("bow_right",[4.05,10.8,-3.2],[4.3,11.5,-2.95]),("bow_bottom",[3.3,10.6,-3.2],[4.3,10.85,-2.95]),
                 ("stem",[3.7,8.5,-3.2],[3.95,10.7,-2.95]),("tooth",[3.95,8.6,-3.2],[4.55,9,-2.95])]:
    box("waist","vault_key_"+name,a,b,3)

for side,sign in [("right",-1),("left",1)]:
    m=lambda bone,name,a,b,color,angle=0,pivot=None: mirrored(side,sign,bone,name,a,b,color,angle,pivot)
    # Sleeves retain a clean silhouette; small metal pieces sit above cloth.
    m("upper_arm","sleeve",[4.45,17,-1.9],[8.1,22.8,1.9],0)
    m("upper_arm","shoulder_ivory",[4.3,21.1,-2.2],[8.4,23.4,2.2],2)
    m("upper_arm","pauldron_inset",[4.55,21.35,-2.5],[8.15,23.1,-2.18],4)
    m("upper_arm","pauldron_rim_top",[4.4,23.05,-2.7],[8.3,23.4,2.4],3)
    m("upper_arm","pauldron_rim_bottom",[4.4,21.05,-2.7],[8.3,21.4,-2.4],3)
    m("upper_arm","pauldron_rim_side",[8.1,21.35,-2.7],[8.45,23.1,2.4],3)
    m("upper_arm","pauldron_crystal",[5.85,21.75,-2.9],[6.9,22.8,-2.5],9,45,[6.375,22.275,-2.7])
    for x in [4.8,7.7]: m("upper_arm","shoulder_rivet_"+str(x),[x-.15,22.15,-2.82],[x+.15,22.45,-2.65],3)
    m("upper_arm","sleeve_seam",[7.85,17.3,-1.6],[8.12,20.8,1.6],11)
    m("forearm","sleeve_lower",[4.75,13.5,-1.7],[7.9,17.1,1.7],0)
    m("forearm","ivory_cuff",[4.55,13.1,-2],[8.1,14.6,2],2)
    m("forearm","cuff_top_gold",[4.45,14.4,-2.1],[8.2,14.75,2.1],3)
    m("forearm","cuff_bottom_gold",[4.5,13,-2.05],[8.15,13.3,2.05],3)
    m("forearm","cuff_button",[6,13.6,-2.25],[6.6,14.2,-2],3)
    m("hand","palm",[5.15,11.9,-1.8],[7.55,13.2,1.05],5)
    for i in range(4):
        x=5.25+i*.57
        m("hand","finger_"+str(i),[x,11.25,-2.5],[x+.48,12.5,-1.65],5)
    m("hand","thumb",[4.8,12.1,-2.55],[5.5,13,-1.25],5)
    m("hand","ring",[5.85,11.65,-2.65],[6.35,12.05,-2.45],3)
# The left thumb wraps over the ledger cover so the grip reads from the front.
box("left_hand","left_thumb_over_ledger",[5.75,12.65,-4.5],[6.55,13.45,-4.22],5)
for side,sign in [("right",-1),("left",1)]:
    m=lambda bone,name,a,b,color,angle=0,pivot=None: mirrored(side,sign,bone,name,a,b,color,angle,pivot)
    # Split tails have their own pivots, so they never stretch a painted skirt.
    m("coattail","coat_front",[.35,5,-2.6],[4.3,12.1,-1.8],0)
    m("coattail","front_embroidery",[.5,5.4,-2.75],[3.95,11.7,-2.6],11)
    m("coattail","front_ivory_facing",[.5,5.15,-2.85],[1.5,12.1,-2.62],2)
    m("coattail","front_gold_edge",[.15,5,-2.9],[.5,12.1,-2.55],3)
    m("coattail","front_gold_hem",[.45,4.8,-2.85],[4.45,5.15,-1.7],3)
    m("coattail","coat_back",[.25,4.8,1.75],[4.35,12.2,2.6],1)
    m("coattail","back_gold_hem",[.3,4.6,1.65],[4.5,4.95,2.75],3)
    m("coattail","coat_side",[3.7,5,-1.8],[4.4,12.1,1.85],0)
    # Layered boots with soles, turned cuffs, toe caps, straps and square buckles.
    m("leg","trousers",[.65,3.4,-1.5],[3.55,12,1.5],1)
    m("leg","boot_shaft",[.5,.8,-1.7],[3.7,5.3,1.8],7)
    m("leg","boot_cuff",[.35,4.6,-1.9],[3.85,5.55,2],7)
    m("leg","boot_cuff_gold",[.35,5.25,-1.98],[3.85,5.55,2.05],3)
    m("leg","boot_toe",[.4,.5,-3.05],[3.8,2.3,-1.6],7)
    m("leg","toe_cap",[.4,.85,-3.25],[3.8,1.75,-2.95],4)
    m("leg","sole",[.3,0,-3.3],[3.9,.6,2],8)
    m("leg","boot_strap",[.4,3,-1.95],[3.8,3.6,-1.65],1)
    for name,a,b in [("top",[1.55,3.55,-2.15],[2.65,3.8,-1.9]),("bottom",[1.55,2.8,-2.15],[2.65,3.05,-1.9]),
                     ("left",[1.55,3,-2.15],[1.8,3.6,-1.9]),("right",[2.4,3,-2.15],[2.65,3.6,-1.9])]:
        m("leg","boot_buckle_"+name,a,b,3)

# Elderly face: portrait UV, projecting nose, hollow spectacles and sculpted hair.
box("hi_head","head_skin",[-4,24,-4],[4,31.7,3.2],5)
box("hi_head","nose",[-.55,26.6,-4.75],[.55,28.65,-3.95],5)
box("hi_head","hair_cap",[-4.25,30.6,-3.75],[4.25,32.5,3.65],6)
box("hi_head","hair_back",[-4.2,24,2.85],[4.2,31.5,3.9],6)
box("hi_head","hair_crown_step",[-2.8,32.1,-2.6],[2.8,32.75,2.6],6)
for sign in [-1,1]:
    x=sign*4.0
    # Hair parts around a protruding ear; a jaw beard closes the bare side of the head.
    lo,hi=sorted([sign*3.7,sign*4.7])
    box("hi_head","ear_"+str(sign),[lo,26.9,-.55],[hi,29.1,1],5)
    box("hi_head","hair_temple_"+str(sign),[x-.55,27.5,-3.4],[x+.55,31.3,-.75],6)
    box("hi_head","hair_behind_ear_"+str(sign),[x-.55,24.6,1.15],[x+.55,31.3,3],6)
    box("hi_head","beard_jaw_"+str(sign),[x-.45,24,-3.95],[x+.45,26.8,1.15],6)
    box("hi_head","sideburn_"+str(sign),[x-.45,25.5,-3.95],[x+.4,28,-2.8],6)
    box("hi_head","hair_front_lock_"+str(sign),[sign*2.7-.75,29.9,-4.35],[sign*2.7+.75,31.3,-3.6],6)
    # Lens area stays open; eyes remain visible through the frame.
    a,b=sorted([.55*sign,3.65*sign])
    for name,lo,hi in [("top",[a,29.1,-4.5],[b,29.35,-4.25]),("bottom",[a,27.5,-4.5],[b,27.75,-4.25]),
                       ("outer",[a,27.7,-4.5],[a+.25,29.15,-4.25]),("inner",[b-.25,27.7,-4.5],[b,29.15,-4.25])]:
        box("hi_head","spectacle_"+str(sign)+"_"+name,lo,hi,3)
    box("hi_head","spectacle_arm_"+str(sign),[x-.13,28.7,-4.35],[x+.13,28.95,.7],4)
    box("hi_head","brow_"+str(sign),[sign*2-.95,29.6,-4.25],[sign*2+.95,30,-3.98],6)
    box("hi_head","moustache_"+str(sign),[min(.1*sign,2.5*sign),25.9,-4.65],[max(.1*sign,2.5*sign),26.65,-4.1],6,[0,0,-22.5*sign],[sign*1.2,26.25,-4.4])
    box("hi_head","beard_cheek_"+str(sign),[sign*3-.65,24,-4.15],[sign*3+.65,26.8,-3.2],6)
    box("hi_head","beard_side_"+str(sign),[sign*2-.75,23.2,-4.35],[sign*2+.75,25.6,-3.3],6)
box("hi_head","spectacle_bridge",[-.7,28.55,-4.62],[.7,28.85,-4.3],3)
box("hi_head","beard_middle",[-1.65,22.65,-4.6],[1.65,25.9,-3.15],6)
box("hi_head","beard_tip",[-1.05,22.15,-4.4],[1.05,23.15,-3.25],6)
box("hi_head","beard_clasp",[-.8,22.85,-4.7],[.8,23.25,-4.35],3)

# Ledger is fully parented to the holding hand; covers, pages and binding are real.
box("ledger","ledger_pages",[5.55,11.35,-3.95],[9.85,17.65,-2.85],13)
box("ledger","ledger_cover_front",[5.35,11.15,-4.25],[10.05,17.85,-3.95],12)
box("ledger","ledger_cover_back",[5.35,11.15,-2.85],[10.05,17.85,-2.55],12)
box("ledger","ledger_spine",[5.25,11.15,-4.25],[5.65,17.85,-2.55],7)
for y in [12.2,14.4,16.6]: box("ledger","binding_band_"+str(y),[5.15,y,-4.35],[5.7,y+.35,-2.5],3)
for x in [5.5,9.4]:
    for y in [11.3,17.2]: box("ledger","ledger_corner_"+str(x)+"_"+str(y),[x,y,-4.42],[x+.55,y+.5,-4.18],3)
box("ledger","ledger_seal_frame",[7.05,13.9,-4.48],[8.25,15.1,-4.2],3,[0,0,45],[7.65,14.5,-4.35])
box("ledger","ledger_seal",[7.28,14.13,-4.65],[8.02,14.87,-4.45],9,[0,0,45],[7.65,14.5,-4.55])
box("ledger","ledger_clasp",[9.5,14.1,-4.55],[10.2,14.7,-2.6],4)
box("ledger","ledger_ribbon",[8.8,10.65,-3.9],[9.15,12.3,-3.6],9)
# The octagonal coin is pinched between the right fingers, not floating nearby.
box("coin","coin_body",[-7.05,12.05,-2.95],[-5.55,13.55,-2.55],4)
box("coin","coin_facets",[-6.9,12.2,-3.05],[-5.7,13.4,-2.65],3,[0,0,45],[-6.3,12.8,-2.85])
box("coin","coin_face",[-6.8,12.3,-3.18],[-5.8,13.3,-3.02],14)
box("coin","coin_edge_glint",[-6.95,13.15,-3.1],[-5.65,13.4,-2.95],3)
box("hitbox","collision_proxy",[-4.5,0,-4.5],[4.5,32.75,4.5],8)

def rot(values): return [{"time":t,"rotation":v} for t,v in values]
def pos(values): return [{"time":t,"position":v} for t,v in values]
idle={"body":pos([(0,[0,0,0]),(2,[0,.08,0]),(4,[0,0,0])]),
      "hi_head":rot([(0,[0,0,0]),(1,[1,-2,0]),(3,[-1,2,0]),(4,[0,0,0])])}
for side,sign in [("right",-1),("left",1)]:
    idle[side+"_upper_arm"]=rot([(0,[0,0,0]),(2,[1,0,sign*.6]),(4,[0,0,0])])
    idle[side+"_coattail"]=rot([(0,[0,0,0]),(2,[1,0,sign*.4]),(4,[0,0,0])])
greet={"hi_head":rot([(0,[0,0,0]),(.35,[-5,0,0]),(.8,[1,0,0]),(1.6,[0,0,0])]),
       "right_upper_arm":rot([(0,[0,0,0]),(.35,[70,0,-12]),(1.2,[70,0,-12]),(1.6,[0,0,0])]),
       "right_forearm":rot([(0,[0,0,0]),(.35,[30,0,0]),(1.2,[30,0,0]),(1.6,[0,0,0])]),
       "right_hand":rot([(0,[0,0,0]),(.4,[0,0,-16]),(.6,[0,0,16]),(.8,[0,0,-16]),(1,[0,0,16]),(1.2,[0,0,0]),(1.6,[0,0,0])])}
count={"hi_head":rot([(0,[0,0,0]),(.4,[-10,14,0]),(2.8,[-10,14,0]),(3.2,[0,0,0])]),
       "right_upper_arm":rot([(0,[0,0,0]),(.4,[20,0,-4]),(2.8,[20,0,-4]),(3.2,[0,0,0])]),
       "right_forearm":rot([(0,[0,0,0]),(.4,[38,0,0]),(1.1,[48,0,0]),(1.7,[38,0,0]),(2.3,[48,0,0]),(2.8,[38,0,0]),(3.2,[0,0,0])]),
       "right_hand":rot([(0,[0,0,0]),(.4,[0,0,-8]),(1.1,[0,0,8]),(1.7,[0,0,-8]),(2.3,[0,0,8]),(2.8,[0,0,-8]),(3.2,[0,0,0])]),
       "coin":rot([(0,[0,0,0]),(.4,[0,0,0]),(1.1,[0,40,0]),(1.7,[0,0,0]),(2.3,[0,40,0]),(2.8,[0,0,0]),(3.2,[0,0,0])])}
inspect={"hi_head":rot([(0,[0,0,0]),(.5,[-16,-20,0]),(1.8,[-16,-20,0]),(2.4,[0,0,0])]),
         "body":rot([(0,[0,0,0]),(.5,[-3,0,0]),(1.8,[-3,0,0]),(2.4,[0,0,0])]),
         "left_upper_arm":rot([(0,[0,0,0]),(.5,[12,0,0]),(1.8,[12,0,0]),(2.4,[0,0,0])]),
         "left_forearm":rot([(0,[0,0,0]),(.5,[42,0,0]),(1.8,[42,0,0]),(2.4,[0,0,0])]),
         "left_hand":rot([(0,[0,0,0]),(.5,[0,-12,0]),(1.8,[0,-12,0]),(2.4,[0,0,0])])}
ANIMATIONS=[("idle",4,True,idle),("greet",1.6,False,greet),
            ("count_coins",3.2,False,count),("inspect_ledger",2.4,False,inspect)]

# Paint new pixels on the native texture canvas, including dedicated face/book art.
PAINT="""(()=>{const t=Texture.all[0];t.edit(canvas=>{
const c=canvas.getContext('2d'),colors=COLORS;let seed=54131;
const rand=()=>{seed=(seed*1664525+1013904223)>>>0;return seed/4294967296};
const rect=(x,y,w,h,col)=>{c.fillStyle=col;c.fillRect(x,y,w,h)};
colors.forEach((col,i)=>{const ox=(i%4)*64,oy=Math.floor(i/4)*64;
rect(ox,oy,64,64,col);
for(let y=2;y<62;y++)for(let x=2;x<62;x++){
 const a=(i===5||i===10)?.025:.07;
 rect(ox+x,oy+y,1,1,rand()>.5?'rgba(255,255,255,'+a+')':'rgba(0,0,0,'+a+')');}
if([0,1,11,15].includes(i)){
 for(let y=4;y<60;y+=4)for(let x=4;x<60;x+=4)rect(ox+x,oy+y,1,1,'rgba(164,215,201,.12)');
 rect(ox+3,oy+3,1,58,'#527d74');rect(ox+60,oy+3,1,58,'#082b32');
 for(let y=5;y<59;y+=3){rect(ox+5,oy+y,1,1,'#6b9185');rect(ox+58,oy+y,1,1,'#6b9185');}}
if([3,4,14].includes(i)){
 rect(ox+2,oy+2,60,2,'#f9dea0');rect(ox+2,oy+4,2,57,'#ebc775');
 rect(ox+60,oy+4,2,57,'#77532b');rect(ox+4,oy+60,56,2,'#866135');
 for(let j=0;j<22;j++)rect(ox+5+Math.floor(rand()*50),oy+5+Math.floor(rand()*50),3,1,'rgba(255,228,166,.24)');}
if(i===2){for(let x=6;x<60;x+=8){rect(ox+x,oy+3,1,57,'rgba(109,94,65,.11)');rect(ox+x+1,oy+3,1,57,'rgba(255,255,235,.16)');}}
if(i===6){for(let x=4;x<60;x+=4){rect(ox+x,oy+3,1,58,'#a8bcb8');rect(ox+x+1,oy+3,1,58,'#eaf0e7');}rect(ox+3,oy+58,58,3,'#acbdb8');}
if(i===7){for(let y=5;y<60;y+=3){rect(ox+4,oy+y,1,1,'#b39065');rect(ox+59,oy+y,1,1,'#b39065');}rect(ox+3,oy+3,58,1,'#946d4b');}
if(i===9){rect(ox+3,oy+3,58,4,'#b1fff0');rect(ox+3,oy+7,5,52,'#71f0dd');
rect(ox+48,oy+9,12,50,'#179ba0');for(let x=12;x<48;x+=8)rect(ox+x,oy+8,2,42-x/2,'rgba(196,255,246,.25)');}
if(i===10){rect(ox,oy,64,64,'#d6aa87');rect(ox+4,oy+8,56,44,'#dfb591');
 rect(ox+6,oy+16,4,29,'#c99778');rect(ox+54,oy+16,4,29,'#c99778');
 for(let x of [16,44]){rect(ox+x-3,oy+23,11,2,'#a9b5a8');rect(ox+x-2,oy+26,10,5,'#f0dfc1');
 rect(ox+x+1,oy+26,4,5,'#548c85');rect(ox+x+2,oy+27,2,4,'#243d3b');rect(ox+x+2,oy+26,1,1,'#d5ffed');
 rect(ox+x-3,oy+33,10,1,'#bd9073');rect(ox+x-5,oy+38,13,2,'#d0a181');}
 rect(ox+27,oy+43,10,2,'#ba7c6c');rect(ox+29,oy+36,6,3,'#e5bd99');}
if(i===11){rect(ox+6,oy+4,2,56,'#c49c52');rect(ox+56,oy+4,2,56,'#c49c52');
for(let y=10;y<55;y+=12){rect(ox+28,oy+y,8,2,'#a48446');rect(ox+31,oy+y-3,2,8,'#a48446');
rect(ox+29,oy+y-1,6,4,'#407e77');}}
if(i===12){rect(ox+5,oy+5,54,2,'#d3ac55');rect(ox+5,oy+57,54,2,'#d3ac55');
rect(ox+5,oy+7,2,50,'#a68543');rect(ox+57,oy+7,2,50,'#a68543');
rect(ox+12,oy+12,40,1,'#376569');rect(ox+12,oy+51,40,1,'#376569');
for(let y of [18,23,42,47]){rect(ox+20,oy+y,24,1,'#a99559');rect(ox+29,oy+y-2,6,1,'#819a78');}}
if(i===13){for(let y=3;y<62;y+=3){rect(ox+2,oy+y,60,1,'#b4ab8e');rect(ox+2,oy+y+1,60,1,'#ece3c6');}}
if(i===14){rect(ox+8,oy+8,48,48,'#c69540');rect(ox+12,oy+12,40,40,'#efd074');
rect(ox+27,oy+20,6,25,'#a27035');rect(ox+22,oy+23,5,5,'#a27035');rect(ox+22,oy+38,17,5,'#a27035');}
if(i===15){rect(ox+4,oy+4,56,2,'#bc9c52');rect(ox+4,oy+58,56,2,'#bc9c52');
rect(ox+4,oy+4,2,56,'#bc9c52');rect(ox+58,oy+4,2,56,'#bc9c52');
rect(ox+22,oy+9,20,18,'#c3a45b');rect(ox+26,oy+13,12,10,'#11373f');rect(ox+30,oy+16,4,4,'#67c8b9');
rect(ox+30,oy+27,4,26,'#c3a45b');rect(ox+34,oy+43,7,3,'#c3a45b');rect(ox+34,oy+48,5,3,'#c3a45b');
rect(ox+23,oy+9,18,1,'#e3c77c');rect(ox+31,oy+27,1,26,'#e3c77c');}
});},{edit_name:'Banker tailored cloth gold face ledger 256 atlas'});return {texture:t.name,size:[t.width,t.height]};})()"""

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("--project-uuid")
    parser.add_argument("--dry-run",action="store_true")
    args=parser.parse_args()
    assert len({r[2]["name"] for r in ROWS})==len(ROWS)
    assert all(all(a<b for a,b in zip(row["from"],row["to"])) for _,_,row in ROWS)
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
    call("risky_eval",{"code":"(()=>{Undo.initEdit({uv_mode:true});Project.texture_width=256;Project.texture_height=256;Undo.finishEdit('Banker UV resolution');return true})()"})
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
cube.faces[face].uv=[ox+4,oy+4,ox+4+w,oy+4+h];}
if([11,12,14,15].includes(i))for(const f of ['north','south'])cube.faces[f].uv=[ox+4,oy+4,ox+60,oy+60];
if(cube.name==='head_skin')cube.faces.north.uv=[132,132,188,188];
if(cube.name==='collision_proxy')cube.visibility=false;
cube.preview_controller.updateUV(cube);cube.preview_controller.updateVisibility(cube);}
const hit=Group.all.find(g=>g.name==='hitbox');hit.visibility=false;hit.preview_controller.updateVisibility(hit);
Undo.finishEdit('Banker proportional UV and dedicated portrait');return {cubes:elements.length,bones:Group.all.length}})()""".replace("MATERIALS",json.dumps(materials))
    call("risky_eval",{"code":uv})
    print(json.dumps({"geometry":summary}),flush=True)
    call("set_mode",{"mode_id":"animate"})
    for name,length,loop,bones in ANIMATIONS: call("create_animation",{"name":name,"animation_length":length,"loop":loop,"bones":bones})
    call("risky_eval",{"code":"(()=>{Undo.initEdit({animations:Animation.all.slice()});for(const a of Animation.all){a.name=a.name.replace(/^animation\\./,'');a.snapping=20;a.override=a.name!=='idle'}Undo.finishEdit('Banker state names 20 FPS');return Animation.all.map(a=>({name:a.name,length:a.length}))})()"})
    call("animation_timeline",{"animation_id":"idle","action":"stop"})
    call("set_mode",{"mode_id":"edit"})
    OUT.mkdir(parents=True,exist_ok=True)
    embedded(call("export_model",{"codec_id":"project","result_format":"embedded","max_content_length":2000000}),target)
    model=json.loads(target.read_text(encoding="utf-8"))
    (OUT/(NAME+".png")).write_bytes(base64.b64decode(model["textures"][0]["source"].split(",",1)[1]))
    call("create_offscreen_view",{"id":"banker_qa","width":1400,"height":1400,"copy_view":"none"})
    try:
        for view,camera in {"preview":[60,40,-90],"front":[0,16,-100],"right":[-100,16,0],"back":[0,16,100]}.items():
            result=call("set_camera_angle",{"view":"banker_qa","position":camera,"target":[0,16,0],"projection":"orthographic","zoom":.8})
            image=next(b for b in result["content"] if b.get("type")=="image")
            (OUT/(NAME+"-"+view+".png")).write_bytes(base64.b64decode(image["data"]))
    finally: dispose_view(client,"banker_qa")
    print(json.dumps({"exported":str(target),**summary}),flush=True)

if __name__=="__main__": main()
