"""Create the 12 Luma station props as Java item models through native Blockbench MCP.

Same rules as build_luma_items_v2: elements inside -16..32, one-axis 22.5/45 rotations, 0..16 UVs.
GUI and item-frame transforms are fitted from the real geometry, so every icon stays inside its slot.
"""
import argparse
import base64
import json
import math
from blockbench_mcp import Client
from build_moonfall_boss import OUT, data, embedded, dispose_view

COLORS = ["#d4a646", "#8d95a0", "#8f6a33", "#8a5a35", "#4e3220", "#efe6cf", "#1d5b5e", "#36e2f0",
          "#d2353c", "#cfe9ee", "#9aa3ab", "#5b3b28", "#1b1f24", "#e8dcb8", "#173e46", "#7d8691"]
SLOTS = {"gold":0, "stone":1, "bronze":2, "wood":3, "wood_dark":4, "paper":5, "teal":6, "gem":7,
         "ruby":8, "ice":9, "steel":10, "leather":11, "dark":12, "map":13, "sign":14, "rune_stone":15}

class Prop:
    def __init__(self): self.rows=[]
    def box(self,name,a,b,mat,axis=None,angle=0,origin=None):
        row={"name":name,"from":list(a),"to":list(b),"mat":mat}
        if axis:
            row["rotation"]=[angle if axis=="x" else 0,angle if axis=="y" else 0,angle if axis=="z" else 0]
            row["origin"]=list(origin)
        self.rows.append(row)

def bank_ledger():
    p=Prop()
    p.box("cover_bottom",[2,0,3.5],[14,.6,12.5],"teal")
    p.box("pages",[2.5,.6,3.9],[13.6,2.4,12.1],"paper")
    p.box("cover_top",[2,2.4,3.5],[14,3.0,12.5],"teal")
    p.box("spine",[1.5,0,3.5],[2.5,3.0,12.5],"leather")
    for z in [5,7.6,10.2]: p.box("spine_band_"+str(z),[1.3,0,z],[2.6,3.2,z+.6],"gold")
    for x in [2.4,12.6]:
        for z in [3.5,11.6]: p.box("corner_"+str(x)+"_"+str(z),[x,3.0,z],[x+1.2,3.25,z+.9],"gold")
    p.box("seal_frame",[6.6,3.0,6.6],[9.4,3.3,9.4],"gold","y",45,[8,3.15,8])
    p.box("seal_gem",[7.3,3.3,7.3],[8.7,3.55,8.7],"gem","y",45,[8,3.4,8])
    p.box("clasp",[13.8,.8,7.3],[14.4,2.6,8.7],"gold")
    p.box("clasp_gem",[14.4,1.3,7.7],[14.55,2.1,8.3],"gem")
    p.box("ribbon",[9.6,.9,12.1],[10.3,1.2,14.2],"ruby")
    return p

def vault_key():
    p=Prop()
    for name,a,b in [("top",[5.2,14.6,7.4],[10.8,15.8,8.6]),("bottom",[5.2,9.6,7.4],[10.8,10.8,8.6]),
                     ("left",[5.2,10.8,7.4],[6.4,14.6,8.6]),("right",[9.6,10.8,7.4],[10.8,14.6,8.6])]:
        p.box("bow_"+name,a,b,"gold")
    p.box("bow_gem",[7.2,11.8,7.6],[8.8,13.4,8.4],"gem")
    p.box("bow_strut_left",[6.4,12.35,7.8],[7.2,12.85,8.2],"bronze")
    p.box("bow_strut_right",[8.8,12.35,7.8],[9.6,12.85,8.2],"bronze")
    p.box("finial",[7.3,15.8,7.6],[8.7,16.6,8.4],"bronze")
    p.box("collar_upper",[6.8,8.6,7.2],[9.2,9.6,8.8],"bronze")
    p.box("shaft",[7.4,1.2,7.6],[8.6,8.6,8.4],"gold")
    p.box("collar_lower",[7.1,6.2,7.4],[8.9,6.7,8.6],"bronze")
    p.box("rune_line",[7.85,2.5,7.5],[8.15,6.0,7.6],"gem")
    p.box("tooth_1",[8.6,1.2,7.7],[10.8,2.2,8.3],"gold")
    p.box("tooth_2",[8.6,2.2,7.7],[9.6,3.0,8.3],"gold")
    p.box("tooth_3",[8.6,3.0,7.7],[10.4,4.0,8.3],"gold")
    p.box("tip",[7.6,.6,7.7],[8.4,1.2,8.3],"gold")
    return p

def coin_stack():
    p=Prop()
    for sname,(x,z,n) in {"a":(5,5,5),"b":(10.5,6,3),"c":(7.5,10.5,7)}.items():
        for i in range(n):
            y=i*.55
            mat="bronze" if i%3==1 else "gold"
            # Two crossed slabs give the cut-corner outline of a pixel-art coin.
            p.box("coin_"+sname+str(i),[x-1.8,y,z-1.1],[x+1.8,y+.55,z+1.1],mat)
            p.box("coin_"+sname+str(i)+"_cross",[x-1.1,y+.01,z-1.8],[x+1.1,y+.54,z+1.8],mat)
        p.box("stamp_"+sname,[x-.6,n*.55,z-.6],[x+.6,n*.55+.12,z+.6],"bronze")
    p.box("leaning_coin",[10.6,.6,11.0],[14.0,2.8,11.55],"gold","x",-22.5,[12.3,0,11.3])
    p.box("leaning_coin_cross",[11.2,0,11.01],[13.4,3.4,11.54],"gold","x",-22.5,[12.3,0,11.3])
    p.box("leaning_coin_gem",[11.8,1.2,10.85],[12.8,2.2,11.0],"gem","x",-22.5,[12.3,0,11.3])
    return p

def forge_hammer():
    p=Prop()
    p.box("pommel",[7.1,0,7.1],[8.9,.9,8.9],"gold")
    p.box("handle",[7.4,.8,7.4],[8.6,11.6,8.6],"wood")
    p.box("grip",[7.25,1.5,7.25],[8.75,5.5,8.75],"leather")
    p.box("collar",[7.1,11.3,7.1],[8.9,12,8.9],"gold")
    p.box("head",[3.5,12,6.2],[12.5,15.6,9.8],"steel")
    for x in [5.6,9.8]: p.box("band_"+str(x),[x,11.9,6.1],[x+.6,15.7,9.9],"gold")
    p.box("face_left",[3.3,12.2,6.4],[3.5,15.4,9.6],"dark")
    p.box("face_right",[12.5,12.2,6.4],[12.7,15.4,9.6],"dark")
    p.box("rune_front",[7.4,12.6,6.05],[8.6,15.0,6.2],"gem")
    p.box("rune_back",[7.4,12.6,9.8],[8.6,15.0,9.95],"gem")
    return p

def tongs():
    p=Prop()
    for side,angle in [("a",22.5),("b",-22.5)]:
        p.box("arm_"+side,[7.6,0,7.6],[8.4,13,8.4],"steel","z",angle,[8,8,8])
        p.box("handle_wrap_"+side,[7.45,.2,7.45],[8.55,3.8,8.55],"leather","z",angle,[8,8,8])
    p.box("pivot",[7.3,7.3,7.0],[8.7,8.7,9.0],"gold")
    p.box("jaw_a",[6.0,12.2,7.5],[7.3,13.2,8.5],"steel")
    p.box("jaw_b",[8.7,12.2,7.5],[10.0,13.2,8.5],"steel")
    p.box("rune_ingot",[7.0,12.3,6.8],[9.0,13.5,9.2],"gem")
    return p

def quest_scroll():
    p=Prop()
    p.box("roll",[2.6,0,6.5],[13.4,2.9,9.5],"paper")
    p.box("roll_oct",[2.6,0,6.5],[13.4,2.9,9.5],"paper","x",45,[8,1.45,8])
    for side,(x0,x1),(k0,k1) in [("left",(1.8,2.6),(1.2,1.8)),("right",(13.4,14.2),(14.2,14.8))]:
        p.box("cap_"+side,[x0,-.05,6.3],[x1,3.0,9.7],"gold")
        p.box("knob_"+side,[k0,.9,7.4],[k1,2.1,8.6],"bronze")
    p.box("ribbon",[7.2,-.05,6.4],[8.8,3.0,9.6],"teal")
    p.box("ribbon_tail_a",[7.4,0,9.6],[8.0,.3,11.4],"teal")
    p.box("ribbon_tail_b",[8.1,0,9.6],[8.7,.3,11.0],"teal")
    p.box("wax_seal",[7.3,.9,9.6],[8.7,2.3,9.85],"ruby")
    p.box("seal_mark",[7.75,1.35,9.85],[8.25,1.85,9.95],"gold")
    return p

def map_table():
    p=Prop()
    p.box("table_top",[.5,11,.5],[15.5,12.5,15.5],"wood")
    p.box("table_apron",[.8,10.2,.8],[15.2,11,15.2],"wood_dark")
    for x in [1.2,13]:
        for z in [1.2,13]: p.box("leg_"+str(x)+"_"+str(z),[x,0,z],[x+1.8,10.2,z+1.8],"wood_dark")
    p.box("stretcher_north",[3,2,1.6],[13,3,2.6],"wood")
    p.box("stretcher_south",[3,2,13.4],[13,3,14.4],"wood")
    p.box("map_sheet",[2.5,12.5,3],[13.5,12.7,13],"map","y",22.5,[8,12.6,8])
    p.box("compass_case",[10.2,12.7,4.2],[11.8,13.1,5.8],"gold")
    p.box("compass_face",[10.5,13.1,4.5],[11.5,13.2,5.5],"gem")
    p.box("pin_a",[5,12.7,8],[5.5,13.6,8.5],"ruby")
    p.box("pin_b",[8.7,12.7,10],[9.2,13.4,10.5],"ruby")
    p.box("ink_pot",[3.2,12.5,12.6],[4.6,13.8,14],"dark")
    p.box("quill",[3.8,13.2,13.1],[4.0,16,13.5],"paper","z",22.5,[3.9,13.2,13.3])
    return p

def mailbox():
    p=Prop()
    p.box("base_plate",[5.5,0,5.5],[10.5,.6,10.5],"stone")
    p.box("post",[7,.6,7],[9,8,9],"wood_dark")
    p.box("box_body",[3.5,8,4],[12.5,13,12],"teal")
    p.box("roof_trim",[3.2,12.9,3.7],[12.8,13.3,12.3],"gold")
    p.box("roof_low",[3.7,13.3,4],[12.3,14.1,12],"teal")
    p.box("roof_high",[4.6,14.1,4.2],[11.4,14.8,11.8],"teal")
    for name,a,b in [("top",[4.3,12.0,3.75],[11.7,12.4,4.0]),("bottom",[4.3,8.6,3.75],[11.7,9.0,4.0]),
                     ("left",[4.3,9.0,3.75],[4.7,12.0,4.0]),("right",[11.3,9.0,3.75],[11.7,12.0,4.0])]:
        p.box("door_frame_"+name,a,b,"gold")
    p.box("letter_slot",[5.5,10.9,3.8],[10.5,11.5,4.0],"dark")
    p.box("letter",[6.2,11.05,3.3],[9.4,13.4,3.7],"paper")
    p.box("emblem_gem",[7.4,9.2,3.75],[8.6,10.4,4.0],"gem")
    p.box("flag_pole",[12.5,9,8.6],[12.9,14.5,9.0],"steel")
    p.box("flag",[12.6,12.6,9.0],[12.8,14.5,11.4],"ruby")
    return p

def shop_sign():
    p=Prop()
    p.box("wall_post",[7,0,13.5],[9,16,15.5],"wood_dark")
    p.box("bracket_arm",[7.4,14.2,3],[8.6,15.2,13.5],"steel")
    p.box("bracket_brace",[7.6,11.6,10.6],[8.4,15.0,11.4],"steel","x",45,[8,13.3,11])
    p.box("arm_finial",[7.2,14.0,2.4],[8.8,15.4,3.0],"gold")
    for z in [4.6,11.1]: p.box("chain_"+str(z),[7.85,11,z],[8.15,14.2,z+.3],"steel")
    p.box("board",[7.4,3.2,4.0],[8.6,10.8,12.0],"sign")
    p.box("frame_top",[7.3,10.8,3.6],[8.7,11.3,12.4],"gold")
    p.box("frame_bottom",[7.3,2.7,3.6],[8.7,3.2,12.4],"gold")
    p.box("frame_front",[7.3,3.2,3.6],[8.7,10.8,4.0],"gold")
    p.box("frame_back",[7.3,3.2,12.0],[8.7,10.8,12.4],"gold")
    return p

def fish_crate():
    p=Prop()
    p.box("crate_floor",[1,0,2],[15,1,14],"wood_dark")
    for x in [1,13.8]:
        for z in [2,12.8]: p.box("corner_post_"+str(x)+"_"+str(z),[x,0,z],[x+1.2,8,z+1.2],"wood_dark")
    for y0,y1 in [(1,3.6),(4.4,7.2)]:
        p.box("plank_north_"+str(y0),[2.2,y0,2.1],[13.8,y1,2.9],"wood")
        p.box("plank_south_"+str(y0),[2.2,y0,13.1],[13.8,y1,13.9],"wood")
        p.box("plank_west_"+str(y0),[1.1,y0,3.2],[1.9,y1,12.8],"wood")
        p.box("plank_east_"+str(y0),[14.1,y0,3.2],[14.9,y1,12.8],"wood")
    p.box("ice_bed",[2.2,1,3.2],[13.8,6.4,12.8],"ice")
    for name,(z0,z1),(x0,x1),body,tail,angle in [("a",(4.4,6.2),(3.0,10.6),"steel","teal",0),
                                                 ("b",(7.4,9.2),(5.0,12.6),"teal","steel",22.5),
                                                 ("c",(10.0,11.6),(3.6,9.8),"steel","teal",-22.5)]:
        cz=(z0+z1)/2
        origin=[(x0+x1)/2,7,cz]
        axis="y" if angle else None
        p.box("fish_"+name,[x0,6.4,z0],[x1,8.0,z1],body,axis,angle,origin)
        p.box("fish_tail_"+name,[x1,6.6,z0-.3],[x1+1.4,7.8,z1+.3],tail,axis,angle,origin)
        p.box("fish_eye_"+name,[x0+.5,7.4,z0-.1],[x0+.9,7.8,z1+.1],"dark",axis,angle,origin)
        p.box("fish_fin_"+name,[(x0+x1)/2-.8,8.0,cz-.2],[(x0+x1)/2+.8,8.6,cz+.2],tail,axis,angle,origin)
    return p

def portal_focus():
    p=Prop()
    p.box("base",[3,0,3],[13,1.5,13],"stone")
    p.box("base_trim",[2.8,1.5,2.8],[13.2,2,13.2],"gold")
    p.box("pillar",[6,2,6],[10,5,10],"stone")
    p.box("cup",[4.5,5,4.5],[11.5,6,11.5],"gold")
    for name,a,b,c,d in [("west",[4.5,6,7.4],[5.3,12.5,8.6],[5.3,12,7.4],[6.3,12.8,8.6]),
                         ("east",[10.7,6,7.4],[11.5,12.5,8.6],[9.7,12,7.4],[10.7,12.8,8.6]),
                         ("north",[7.4,6,4.5],[8.6,12.5,5.3],[7.4,12,5.3],[8.6,12.8,6.3]),
                         ("south",[7.4,6,10.7],[8.6,12.5,11.5],[7.4,12,9.7],[8.6,12.8,10.7])]:
        p.box("cage_arm_"+name,a,b,"gold")
        p.box("cage_curl_"+name,c,d,"bronze")
    p.box("crystal_core",[6.5,7.5,6.5],[9.5,11.5,9.5],"gem","y",45,[8,9.5,8])
    p.box("crystal_upper",[7.1,11.5,7.1],[8.9,13,8.9],"gem","y",45,[8,12.25,8])
    p.box("crystal_point",[7.6,13,7.6],[8.4,13.8,8.4],"gem")
    p.box("crystal_lower",[7.1,6.5,7.1],[8.9,7.5,8.9],"gem","y",45,[8,7,8])
    for name,a,b in [("north",[5.8,9.3,5.8],[10.2,9.7,6.2]),("south",[5.8,9.3,9.8],[10.2,9.7,10.2]),
                     ("west",[5.8,9.3,6.2],[6.2,9.7,9.8]),("east",[9.8,9.3,6.2],[10.2,9.7,9.8])]:
        p.box("orbit_"+name,a,b,"gold")
    return p

def rune_plinth():
    p=Prop()
    p.box("base",[1.5,0,1.5],[14.5,2,14.5],"stone")
    p.box("base_trim",[1.3,1.8,1.3],[14.7,2.2,14.7],"bronze")
    p.box("body",[3,2.2,3],[13,11.8,13],"rune_stone")
    p.box("top_trim",[1.9,11.8,1.9],[14.1,12.1,14.1],"gold")
    p.box("top_slab",[2,12.1,2],[14,13.6,14],"stone")
    for x in [2.6,12.4]:
        for z in [2.6,12.4]: p.box("corner_stud_"+str(x)+"_"+str(z),[x,2.2,z],[x+1,11.8,z+1],"stone")
    p.box("gem_plate",[6.5,13.6,6.5],[9.5,14.2,9.5],"gold")
    p.box("gem_cradle",[6.9,13.6,6.9],[9.1,14.4,9.1],"bronze","y",45,[8,14,8])
    p.box("gem",[7.2,14.2,7.2],[8.8,16,8.8],"gem","y",45,[8,15.1,8])
    return p

PROPS={"bank_ledger":bank_ledger,"vault_key":vault_key,"coin_stack":coin_stack,"forge_hammer":forge_hammer,
       "tongs":tongs,"quest_scroll":quest_scroll,"map_table":map_table,"mailbox":mailbox,"shop_sign":shop_sign,
       "fish_crate":fish_crate,"portal_focus":portal_focus,"rune_plinth":rune_plinth}
ITEMS={key:("prop_"+key,"prop_"+key,fn) for key,fn in PROPS.items()}

def rotate(v,angles):
    """Java applies display rotation as Rx * Ry * Rz (rotationXYZ)."""
    x,y,z=v; rx,ry,rz=(math.radians(a) for a in angles)
    x,y=x*math.cos(rz)-y*math.sin(rz),x*math.sin(rz)+y*math.cos(rz)
    x,z=x*math.cos(ry)+z*math.sin(ry),-x*math.sin(ry)+z*math.cos(ry)
    y,z=y*math.cos(rx)-z*math.sin(rx),y*math.sin(rx)+z*math.cos(rx)
    return x,y,z

def corners(row):
    """World corners of a Blockbench row or Java element, after its own single-axis rotation."""
    if "rotation" in row and isinstance(row["rotation"],dict):
        rot=row["rotation"]; angles=[rot["angle"] if rot["axis"]==ax else 0 for ax in "xyz"]; origin=rot["origin"]
    else:
        angles=row.get("rotation",[0,0,0]); origin=row.get("origin",[8,8,8])
    for cx in (row["from"][0],row["to"][0]):
        for cy in (row["from"][1],row["to"][1]):
            for cz in (row["from"][2],row["to"][2]):
                x,y,z=rotate([cx-origin[0],cy-origin[1],cz-origin[2]],angles)
                yield x+origin[0],y+origin[1],z+origin[2]

def extent(rows,rotation):
    """Screen extent (x0,x1,y0,y1) of all corners around the model centre at scale 1."""
    xs=[];ys=[]
    for r in rows:
        for cx,cy,cz in corners(r):
            x,y,_=rotate([cx-8,cy-8,cz-8],rotation); xs.append(x); ys.append(y)
    return min(xs),max(xs),min(ys),max(ys)

def fitted(rows,rotation,limit,cap):
    """Scale and centre a view so the model fills at most +-limit units."""
    x0,x1,y0,y1=extent(rows,rotation)
    s=round(min(cap,2*limit/(x1-x0),2*limit/(y1-y0)),3)
    return {"rotation":rotation,"translation":[round(-(x0+x1)/2*s,3),round(-(y0+y1)/2*s,3),0],"scale":[s,s,s]}

def display_for(rows):
    size=max(max(r["to"][i] for r in rows)-min(r["from"][i] for r in rows) for i in range(3))
    k=round(min(1,16/size),3)
    return {"gui":fitted(rows,[30,225,0],7.4,.9),"fixed":fitted(rows,[0,0,0],7.0,.9),
            "ground":{"rotation":[0,0,0],"translation":[0,3,0],"scale":[.25*k]*3},
            # Head space puts the top of the head at +8; lift the prop so its base rests there.
            "head":{"rotation":[0,0,0],"translation":[0,round(8+(8-min(r["from"][1] for r in rows))*.6*k,3),0],"scale":[.6*k]*3},
            "thirdperson_righthand":{"rotation":[75,45,0],"translation":[0,2.5,0],"scale":[.375*k]*3},
            "thirdperson_lefthand":{"rotation":[75,45,0],"translation":[0,2.5,0],"scale":[.375*k]*3},
            "firstperson_righthand":{"rotation":[0,45,0],"translation":[0,0,0],"scale":[.4*k]*3},
            "firstperson_lefthand":{"rotation":[0,225,0],"translation":[0,0,0],"scale":[.4*k]*3}}

DISPLAY={key:display_for(fn().rows) for key,fn in PROPS.items()}

PAINT="""(()=>{const t=Texture.all[0];t.edit(canvas=>{
const c=canvas.getContext('2d'),colors=COLORS;let seed=61121;
const rand=()=>{seed=(seed*1664525+1013904223)>>>0;return seed/4294967296};
const rect=(x,y,w,h,col)=>{c.fillStyle=col;c.fillRect(x,y,w,h)};
colors.forEach((col,i)=>{const ox=(i%4)*32,oy=Math.floor(i/4)*32;rect(ox,oy,32,32,col);
for(let y=0;y<32;y++)for(let x=0;x<32;x++){rect(ox+x,oy+y,1,1,rand()>.5?'rgba(255,255,255,.06)':'rgba(0,0,0,.06)')}
if([0,2,10].includes(i)){const hi={0:'#f7dc8f',2:'#b98d4e',10:'#d5dbe0'}[i],lo={0:'#8c6720',2:'#5a3f1d',10:'#5d646b'}[i];
 rect(ox,oy,32,2,hi);rect(ox,oy,2,32,hi);rect(ox+30,oy,2,32,lo);rect(ox,oy+30,32,2,lo);}
if(i===1||i===15){rect(ox,oy,32,2,'#a9b1ba');rect(ox,oy+30,32,2,'#666d76');
 for(let j=0;j<6;j++){const x=2+Math.floor(rand()*26),y=2+Math.floor(rand()*26);rect(ox+x,oy+y,4,1,'#6f7781');rect(ox+x+3,oy+y+1,1,2,'#6f7781');}}
if(i===3||i===4){for(let y=0;y<32;y+=8){rect(ox,oy+y,32,1,i===3?'#5e3b21':'#2d1c11');}
 for(let j=0;j<20;j++)rect(ox+Math.floor(rand()*30),oy+1+Math.floor(rand()*30),3,1,i===3?'#a2704a':'#694430');}
if(i===5){for(let y=4;y<30;y+=4)rect(ox+3,oy+y,26,1,'rgba(140,120,90,.25)');}
if(i===6||i===14){rect(ox,oy,32,2,'#2f7f82');rect(ox,oy+30,32,2,'#0d3134');}
if(i===7||i===8){const a=i===7?['#1ec3d6','#4cecf8','#a5f9ff','#0e8796']:['#b0262d','#e85a5f','#ff9a9c','#7a161c'];
 rect(ox,oy,32,32,a[0]);rect(ox+3,oy+3,26,26,a[1]);rect(ox+8,oy+8,16,16,a[2]);rect(ox+8,oy+8,6,6,'#ffffff');
 rect(ox,oy+29,32,3,a[3]);rect(ox+29,oy,3,32,a[3]);}
if(i===9){for(let j=0;j<18;j++)rect(ox+Math.floor(rand()*28),oy+Math.floor(rand()*28),4,2,'rgba(255,255,255,.45)');}
if(i===11){for(let y=2;y<32;y+=3){rect(ox+2,oy+y,1,1,'#a07a52');rect(ox+29,oy+y,1,1,'#a07a52');}}
if(i===13){rect(ox+1,oy+1,30,30,'#e8dcb8');rect(ox+3,oy+4,12,10,'#9cc39a');rect(ox+15,oy+6,6,5,'#9cc39a');rect(ox+18,oy+16,10,12,'#9cc39a');
 rect(ox+3,oy+18,9,9,'#7fb3cf');rect(ox+20,oy+3,9,8,'#7fb3cf');
 for(let k=0;k<8;k++)rect(ox+6+k*2,oy+14+(k%2),1,1,'#b0262d');rect(ox+22,oy+20,3,1,'#b0262d');rect(ox+23,oy+19,1,3,'#b0262d');
 rect(ox,oy,32,1,'#b39c6a');rect(ox,oy+31,32,1,'#b39c6a');rect(ox,oy,1,32,'#b39c6a');rect(ox+31,oy,1,32,'#b39c6a');}
if(i===14){rect(ox+3,oy+3,26,26,'#d4a646');rect(ox+5,oy+5,22,22,'#173e46');
 rect(ox+10,oy+9,12,14,'#d4a646');rect(ox+9,oy+11,14,10,'#d4a646');rect(ox+13,oy+12,6,8,'#36e2f0');rect(ox+14,oy+13,2,2,'#ffffff');}
if(i===15){const g='#45e8f6';for(let k=0;k<6;k++){rect(ox+15-k,oy+4+k,2,1,g);rect(ox+15+k,oy+4+k,2,1,g);rect(ox+10+k,oy+10+k,2,1,g);rect(ox+20-k,oy+10+k,2,1,g);}
 rect(ox+15,oy+16,2,9,g);rect(ox+14,oy+26,4,3,'#c6fdff');rect(ox+15,oy+9,2,3,'#c6fdff');}
});},{edit_name:'Luma props atlas'});return true})()"""

def uv_for(mat):
    i=SLOTS[mat]; u=(i%4)*4; v=(i//4)*4
    return [u+.05,v+.05,u+3.95,v+3.95]

def build(key,uuid,client):
    name,texture,fn=ITEMS[key]
    rows=fn().rows
    target=OUT/(name+".bbmodel")
    if target.exists(): raise RuntimeError("Revision exists; do not overwrite earlier work")
    def guard():
        info=data(client.call("get_project_info",{})); project=info["project"]
        if project["uuid"]!=uuid or project["name"]!=name: raise RuntimeError("Active project changed")
        return info
    def call(tool,arguments): guard(); return client.call(tool,arguments)
    if any(guard()["counts"].get(k,0) for k in ["cubes","meshes","groups","textures"]): raise RuntimeError("Project must be empty")
    call("set_mode",{"mode_id":"edit"})
    call("create_texture",{"name":texture+".png","width":128,"height":128,"fill_color":COLORS[0]})
    call("risky_eval",{"code":PAINT.replace("COLORS",json.dumps(COLORS))})
    call("risky_eval",{"code":"(()=>{const t=Texture.all[0];t.namespace='luma';t.folder='item';return t.name})()"})
    call("add_group",{"name":"prop","origin":[8,8,8],"parent":"root"})
    for mat in SLOTS:
        batch=[{k:v for k,v in r.items() if k!="mat"} for r in rows if r["mat"]==mat]
        if batch:
            call("place_cube",{"elements":batch,"group":"prop","texture":texture+".png",
                              "faces":[{"face":f,"uv":uv_for(mat)} for f in ["north","south","east","west","up","down"]]})
    call("risky_eval",{"code":"(()=>{const d="+json.dumps(DISPLAY[key])+";for(const [k,v] of Object.entries(d)){Project.display_settings[k]=new DisplaySlot(k,v)}return true})()"})
    embedded(call("export_model",{"codec_id":"project","result_format":"embedded","max_content_length":2000000}),target)
    java=OUT/(name+".json")
    embedded(call("export_model",{"codec_id":"java_block","result_format":"embedded","max_content_length":2000000}),java)
    # Manifest hashes this file and .gitattributes stores JSON as LF.
    java.write_bytes(java.read_bytes().replace(b"\r\n",b"\n"))
    model=json.loads(target.read_text(encoding="utf-8"))
    (OUT/(name+".png")).write_bytes(base64.b64decode(model["textures"][0]["source"].split(",",1)[1]))
    lo=[min(r["from"][i] for r in rows) for i in range(3)]; hi=[max(r["to"][i] for r in rows) for i in range(3)]
    # Blockbench draws Java models shifted by -8 on X and Z.
    center=[(lo[0]+hi[0])/2-8,(lo[1]+hi[1])/2,(lo[2]+hi[2])/2-8]
    zoom=round(min(1.6,20/max(h-l for h,l in zip(hi,lo))),2)
    call("create_offscreen_view",{"id":"prop_qa","width":900,"height":900,"copy_view":"none"})
    try:
        for view,offset in {"preview":[50,35,-60],"front":[0,0,-100]}.items():
            result=call("set_camera_angle",{"view":"prop_qa","position":[center[i]+offset[i] for i in range(3)],"target":center,"projection":"orthographic","zoom":zoom})
            image=next(b for b in result["content"] if b.get("type")=="image")
            (OUT/(name+"-"+view+".png")).write_bytes(base64.b64decode(image["data"]))
    finally: dispose_view(client,"prop_qa")
    return {"asset":name,"cubes":len(rows)}

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("props",nargs="*",help="prop keys; default all")
    parser.add_argument("--dry-run",action="store_true")
    args=parser.parse_args()
    keys=args.props or list(PROPS)
    for key in keys:
        rows=PROPS[key]().rows
        assert len({r["name"] for r in rows})==len(rows),key
        for r in rows:
            assert all(a<b for a,b in zip(r["from"],r["to"])),(key,r["name"])
            assert all(-16<=v<=32 for v in r["from"]+r["to"]),(key,r["name"])
    if args.dry_run:
        print(json.dumps({k:len(PROPS[k]().rows) for k in keys})); return
    client=Client()
    import re
    for key in keys:
        name=ITEMS[key][0]
        client.call("risky_eval",{"code":"(()=>{for(const p of ModelProject.all.filter(p=>p.name==='"+name+"')){p.select();p.saved=true;p.close(true)}return true})()"})
        text=client.call("create_project",{"name":name,"format":"java_block"})["content"][0]["text"]
        uuid=re.search(r"UUID: ([0-9a-f-]+)",text).group(1)
        print(json.dumps({**build(key,uuid,client),"project_uuid":uuid}),flush=True)

if __name__=="__main__": main()
