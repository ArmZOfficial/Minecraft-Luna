"""Create runeblade v2, aether halo v2 and the crown-style halo v3 as Java item models through native Blockbench MCP.

Java item rules: elements stay inside -16..32, each element rotates on one axis by 0/22.5/45,
UVs live in 0..16 space, and every display context carries its own transform.
"""
import argparse
import base64
import json
from blockbench_mcp import Client
from build_moonfall_boss import OUT, data, embedded, dispose_view

# 128px atlas, 4x4 slots of 32px; in 0..16 UV space each slot is 4 units wide.
# New slots are appended so earlier items repaint identically.
COLORS = ["#e3e0d6", "#d4a646", "#8f6a33", "#2bd0e0", "#134a55", "#36e2f0", "#ece4d2", "#0e3a44", "#1b1f24", "#d2353c", "#f2cf63", "#f6c93c", "#efbd34", "#ffd84f", "#c98a24"]
SLOTS = {"steel":0, "gold":1, "bronze":2, "rune":3, "grip":4, "gem":5, "ivory":6, "teal":7, "dark":8, "ruby":9, "gold_light":10,
         "crown_gold":11, "crown_band":12, "crown_tip":13, "crown_shade":14}

def build_runeblade():
    rows=[]
    def box(name,a,b,mat,rotation=None,origin=None):
        row={"name":name,"from":a,"to":b,"mat":mat}
        if rotation: row["rotation"]=rotation; row["origin"]=origin
        rows.append(row)
    # Pommel: gold block with a cyan gem on both faces and a collar ring.
    box("pommel",[6.5,-2.5,6.5],[9.5,.5,9.5],"gold")
    box("pommel_gem_front",[7.3,-1.7,6.3],[8.7,-.3,6.5],"gem")
    box("pommel_gem_back",[7.3,-1.7,9.5],[8.7,-.3,9.7],"gem")
    box("pommel_collar",[6.8,.5,6.8],[9.2,1.0,9.2],"bronze")
    # Wrapped grip between two gold rings.
    box("grip",[7,1,7],[9,7,9],"grip")
    box("grip_ring_low",[6.85,1.0,6.85],[9.15,1.5,9.15],"gold")
    box("grip_ring_high",[6.85,6.5,6.85],[9.15,7.0,9.15],"gold")
    # Guard: gem block, stepped wings with raised tips, and a collar under the blade.
    box("guard_core",[6,7,6.5],[10,10,9.5],"gold")
    box("guard_gem_front",[7.2,7.8,6.3],[8.8,9.4,6.5],"gem")
    box("guard_gem_back",[7.2,7.8,9.5],[8.8,9.4,9.7],"gem")
    for side,(w0,w1),(t0,t1),(s0,s1) in [("left",(2.5,6),(1.5,3),(2.2,3.4)),("right",(10,13.5),(13,14.5),(12.6,13.8))]:
        box("guard_wing_"+side,[w0,7.8,7],[w1,9.2,9],"gold")
        box("guard_tip_"+side,[t0,8.4,6.8],[t1,10.6,9.2],"gold")
        box("guard_tip_step_"+side,[s0,7.2,7.2],[s1,8.4,8.8],"bronze")
    box("blade_collar",[6.5,10,7.0],[9.5,10.8,9.0],"bronze")
    # Blade: stepped widths toward a pointed tip, with a cyan rune groove on both faces.
    for name,(x0,x1),(y0,y1),(z0,z1) in [("blade_base",(5.9,10.1),(10.8,13.5),(7.4,8.6)),("blade_mid",(6.2,9.8),(13.5,24),(7.4,8.6)),
                                         ("blade_upper",(6.5,9.5),(24,26.5),(7.4,8.6)),("blade_tip_1",(6.9,9.1),(26.5,28.3),(7.45,8.55)),
                                         ("blade_tip_2",(7.4,8.6),(28.3,29.6),(7.5,8.5)),("blade_point",(7.8,8.2),(29.6,30.4),(7.55,8.45))]:
        box(name,[x0,y0,z0],[x1,y1,z1],"steel")
    box("rune_groove_front",[7.55,11.2,7.25],[8.45,25.6,7.4],"rune")
    box("rune_groove_back",[7.55,11.2,8.6],[8.45,25.6,8.75],"rune")
    return rows

def build_halo():
    rows=[]
    def box(name,a,b,mat): rows.append({"name":name,"from":a,"to":b,"mat":mat})
    # Four ivory bars, each with a teal band on its outer and inner face.
    bars={"north":([3,7.2,1.2],[13,9,2.6]),"south":([3,7.2,13.4],[13,9,14.8]),
          "west":([1.2,7.2,3],[2.6,9,13]),"east":([13.4,7.2,3],[14.8,9,13])}
    for name,(a,b) in bars.items():
        box("bar_"+name,a,b,"ivory")
        if name in ("north","south"):
            for face,(z0,z1) in {"outer":(a[2]-.2,a[2]+.05) if name=="north" else (b[2]-.05,b[2]+.2),
                                 "inner":(b[2]-.05,b[2]+.2) if name=="north" else (a[2]-.2,a[2]+.05)}.items():
                box("band_"+name+"_"+face,[a[0],7.8,z0],[b[0],8.4,z1],"teal")
        else:
            for face,(x0,x1) in {"outer":(a[0]-.2,a[0]+.05) if name=="west" else (b[0]-.05,b[0]+.2),
                                 "inner":(b[0]-.05,b[0]+.2) if name=="west" else (a[0]-.2,a[0]+.05)}.items():
                box("band_"+name+"_"+face,[x0,7.8,a[2]],[x1,8.4,b[2]],"teal")
    # Gold corner housings with a raised step, a top gem and gems on both outer faces.
    for cname,(x,z) in {"nw":(.8,.8),"ne":(12.4,.8),"sw":(.8,12.4),"se":(12.4,12.4)}.items():
        box("corner_"+cname,[x,6.8,z],[x+2.8,9.4,z+2.8],"gold")
        box("corner_step_"+cname,[x+.5,9.4,z+.5],[x+2.3,9.9,z+2.3],"bronze")
        box("corner_gem_top_"+cname,[x+.8,9.9,z+.8],[x+2.0,10.3,z+2.0],"gem")
        gz=(z-.2,z) if z<8 else (z+2.8,z+3.0)
        gx=(x-.2,x) if x<8 else (x+2.8,x+3.0)
        box("corner_gem_z_"+cname,[x+.7,7.4,gz[0]],[x+2.1,8.8,gz[1]],"gem")
        box("corner_gem_x_"+cname,[gx[0],7.4,z+.7],[gx[1],8.8,z+2.1],"gem")
    # Front plaque with a gem; plain plaque on the back, as on the reference.
    box("plaque_front",[6.6,7.0,.8],[9.4,9.2,1.3],"gold")
    box("plaque_front_gem",[7.3,7.5,.6],[8.7,8.9,.8],"gem")
    box("plaque_back",[6.6,7.0,14.7],[9.4,9.2,15.2],"gold")
    return rows

def build_crown():
    """Crown form of the halo: layered gold band, stepped points, rubies on the band, cyan gems near the tips."""
    rows=[]
    def box(name,a,b,mat): rows.append({"name":name,"from":a,"to":b,"mat":mat})
    def ring(name,y0,y1,lo,hi,wall,mat):
        box(name+"_north",[lo,y0,lo],[hi,y1,lo+wall],mat)
        box(name+"_south",[lo,y0,hi-wall],[hi,y1,hi],mat)
        box(name+"_west",[lo,y0,lo+wall],[lo+wall,y1,hi-wall],mat)
        box(name+"_east",[hi-wall,y0,lo+wall],[hi,y1,hi-wall],mat)
    ring("rim_bottom",12,12.8,.3,15.7,1.4,"crown_shade")
    ring("band",12.8,15.4,.5,15.5,1.2,"crown_band")
    ring("lip",15.4,16.2,.7,15.3,1.1,"crown_tip")
    # Each side is described along its own axis t (0.7..15.3); the wall spans d0..d1 across it.
    # Points are thin plates flush with the outer face so the top edge reads as one zigzag.
    sides={"north":(.75,1.55,-1),"south":(14.45,15.25,1),"west":(.75,1.55,-1),"east":(14.45,15.25,1)}
    def place(side,name,t0,t1,y0,y1,mat,out=0):
        d0,d1,sign=sides[side]
        if out:
            d0,d1=(d0-out,d0) if sign<0 else (d1,d1+out)
        if side in ("north","south"): box(side+"_"+name,[t0,y0,d0],[t1,y1,d1],mat)
        else: box(side+"_"+name,[d0,y0,t0],[d1,y1,t1],mat)
    def spike(side,name,center,steps,top_mat="crown_tip"):
        y=16.2
        for i,(width,height) in enumerate(steps):
            place(side,name+"_"+str(i),center-width/2,center+width/2,y,y+height,top_mat if i==len(steps)-1 else "crown_gold")
            y+=height
    for side in sides:
        spike(side,"mid",8,[(3.4,1.4),(2.2,1.4),(1.2,1.4),(.5,.9)])
        place(side,"mid_gem",7.45,8.55,17.75,18.85,"gem",out=.25)
        place(side,"ruby",7.2,8.8,13.2,14.9,"ruby",out=.35)
        for k,c in enumerate([4.7,11.3]):
            spike(side,"small_"+str(k),c,[(3.0,1.0),(1.8,1.0),(.8,.8)])
            place(side,"small_ruby_"+str(k),c-.45,c+.45,16.35,17.15,"ruby",out=.2)
    # Tall corner spikes stand on the four corners with cyan gems on both outer faces.
    for cname,(cx,cz) in {"nw":(1.6,1.6),"ne":(14.4,1.6),"sw":(1.6,14.4),"se":(14.4,14.4)}.items():
        y=16.2
        for i,(width,height) in enumerate([(1.8,1.8),(1.2,1.8),(.8,1.6),(.4,1.0)]):
            box("corner_"+cname+"_"+str(i),[cx-width/2,y,cz-width/2],[cx+width/2,y+height,cz+width/2],"crown_tip" if i==3 else "crown_gold")
            y+=height
        gz=(cz-.8,cz-.55) if cz<8 else (cz+.55,cz+.8)
        gx=(cx-.8,cx-.55) if cx<8 else (cx+.55,cx+.8)
        box("corner_gem_z_"+cname,[cx-.4,18.5,gz[0]],[cx+.4,19.6,gz[1]],"gem")
        box("corner_gem_x_"+cname,[gx[0],18.5,cz-.4],[gx[1],19.6,cz+.4],"gem")
    return rows

# Transforms per display context (rotation, translation, scale), tuned against Blockbench's display previews.
DISPLAY={
 "runeblade":{
  "thirdperson_righthand":{"rotation":[0,-90,-30],"translation":[0,4,.5],"scale":[.5,.5,.5]},
  "thirdperson_lefthand":{"rotation":[0,90,30],"translation":[0,4,.5],"scale":[.5,.5,.5]},
  "firstperson_righthand":{"rotation":[0,-90,25],"translation":[1.13,4.5,1.13],"scale":[.36,.36,.36]},
  "firstperson_lefthand":{"rotation":[0,90,-25],"translation":[1.13,4.5,1.13],"scale":[.36,.36,.36]},
  "gui":{"rotation":[0,0,-45],"translation":[-2.6,-2.6,0],"scale":[.62,.62,.62]},
  "ground":{"rotation":[0,0,0],"translation":[0,2,0],"scale":[.3,.3,.3]},
  "fixed":{"rotation":[0,0,-45],"translation":[-2.6,-2.6,0],"scale":[.62,.62,.62]},
  "head":{"rotation":[0,0,-45],"translation":[0,0,0],"scale":[.6,.6,.6]}},
 "crown":{
  "head":{"rotation":[0,0,0],"translation":[0,0,0],"scale":[1,1,1]},
  "gui":{"rotation":[30,45,0],"translation":[0,-5.45,0],"scale":[.72,.72,.72]},
  "ground":{"rotation":[0,0,0],"translation":[0,-2,0],"scale":[.4,.4,.4]},
  "fixed":{"rotation":[0,0,0],"translation":[0,-5.5,0],"scale":[.75,.75,.75]},
  "thirdperson_righthand":{"rotation":[75,45,0],"translation":[0,0,0],"scale":[.375,.375,.375]},
  "thirdperson_lefthand":{"rotation":[75,45,0],"translation":[0,0,0],"scale":[.375,.375,.375]},
  "firstperson_righthand":{"rotation":[10,45,0],"translation":[0,3,0],"scale":[.3,.3,.3]},
  "firstperson_lefthand":{"rotation":[10,225,0],"translation":[0,3,0],"scale":[.3,.3,.3]}},
 "halo":{
  "head":{"rotation":[0,0,0],"translation":[0,10,0],"scale":[1.15,1.15,1.15]},
  "gui":{"rotation":[35,45,0],"translation":[0,0,0],"scale":[.78,.78,.78]},
  "ground":{"rotation":[0,0,0],"translation":[0,2,0],"scale":[.4,.4,.4]},
  "fixed":{"rotation":[-90,0,0],"translation":[0,0,-4],"scale":[.8,.8,.8]},
  "thirdperson_righthand":{"rotation":[75,45,0],"translation":[0,2.5,0],"scale":[.375,.375,.375]},
  "thirdperson_lefthand":{"rotation":[75,45,0],"translation":[0,2.5,0],"scale":[.375,.375,.375]},
  "firstperson_righthand":{"rotation":[10,45,0],"translation":[0,4,0],"scale":[.35,.35,.35]},
  "firstperson_lefthand":{"rotation":[10,225,0],"translation":[0,4,0],"scale":[.35,.35,.35]}}}

ITEMS={"runeblade":("item_runeblade_v2","runeblade_v2",build_runeblade),
       "halo":("item_aether_halo_v2","aether_halo_v2",build_halo),
       "crown":("item_aether_halo_v3","aether_halo_v3",build_crown)}

PAINT="""(()=>{const t=Texture.all[0];t.edit(canvas=>{
const c=canvas.getContext('2d'),colors=COLORS;let seed=33071;
const rand=()=>{seed=(seed*1664525+1013904223)>>>0;return seed/4294967296};
const rect=(x,y,w,h,col)=>{c.fillStyle=col;c.fillRect(x,y,w,h)};
colors.forEach((col,i)=>{const ox=(i%4)*32,oy=Math.floor(i/4)*32;rect(ox,oy,32,32,col);
for(let y=0;y<32;y++)for(let x=0;x<32;x++){rect(ox+x,oy+y,1,1,rand()>.5?'rgba(255,255,255,.06)':'rgba(0,0,0,.06)')}
if(i===0){rect(ox,oy,3,32,'#fbfaf4');rect(ox+29,oy,3,32,'#b9b6ab');
 for(let j=0;j<8;j++)rect(ox+4+Math.floor(rand()*24),oy+3+Math.floor(rand()*26),4,1,'rgba(255,255,255,.35)');}
if([1,2].includes(i)){const hi=i===1?'#f7dc8f':'#b98d4e',lo=i===1?'#8c6720':'#5a3f1d';
 rect(ox,oy,32,2,hi);rect(ox,oy,2,32,hi);rect(ox+30,oy,2,32,lo);rect(ox,oy+30,32,2,lo);
 for(let j=0;j<10;j++)rect(ox+3+Math.floor(rand()*25),oy+3+Math.floor(rand()*25),3,1,'rgba(255,240,190,.3)');}
if(i===3){rect(ox,oy,32,32,'#1fb9cc');rect(ox+10,oy,12,32,'#45e8f6');rect(ox+14,oy,4,32,'#c6fdff');
 for(let y=2;y<32;y+=6){rect(ox+6,oy+y,20,2,'#0d8a9c');rect(ox+14,oy+y-2,4,6,'#0d8a9c');rect(ox+15,oy+y,2,2,'#e8ffff');}}
if(i===4){for(let k=-32;k<32;k+=6)for(let y=0;y<32;y++){const x=k+y;if(x>=0&&x<32){rect(ox+x,oy+y,2,1,'#0a2e36');rect(ox+(x+2)%32,oy+y,1,1,'#2a7584');}}}
if(i===5){rect(ox,oy,32,32,'#1ec3d6');rect(ox+3,oy+3,26,26,'#4cecf8');rect(ox+8,oy+8,16,16,'#a5f9ff');rect(ox+8,oy+8,6,6,'#ffffff');
 rect(ox,oy+29,32,3,'#0e8796');rect(ox+29,oy,3,32,'#0e8796');}
if(i===6){rect(ox,oy,32,3,'#fffaf0');rect(ox,oy+29,32,3,'#c3b89f');}
if(i===7){rect(ox,oy,32,2,'#2f7380');rect(ox,oy+30,32,2,'#071f25');}
if(i===9){rect(ox+3,oy+3,26,26,'#e85a5f');rect(ox+8,oy+8,16,16,'#ff9a9c');rect(ox+8,oy+8,6,6,'#ffe2e2');
 rect(ox,oy+29,32,3,'#8c1c24');rect(ox+29,oy,3,32,'#8c1c24');}
if(i===11||i===13){for(let j=0;j<14;j++)rect(ox+2+Math.floor(rand()*28),oy+2+Math.floor(rand()*28),2,2,i===11?'rgba(255,240,170,.35)':'rgba(255,255,230,.5)');}
if(i===12){rect(ox,oy,32,3,'#ffe48a');rect(ox,oy+13,32,4,'#fbe07a');rect(ox,oy+27,32,5,'#c98a24');
 for(let j=0;j<10;j++)rect(ox+2+Math.floor(rand()*28),oy+4+Math.floor(rand()*20),3,1,'rgba(255,250,210,.5)');}
if(i===10){rect(ox,oy,32,2,'#fff1b0');rect(ox,oy,2,32,'#fff1b0');rect(ox+30,oy,2,32,'#b08a2c');rect(ox,oy+30,32,2,'#b08a2c');
 for(let j=0;j<8;j++)rect(ox+3+Math.floor(rand()*25),oy+3+Math.floor(rand()*25),3,1,'rgba(255,255,230,.45)');}
});},{edit_name:'Luma item v2 atlas'});return true})()"""

def uv_for(mat):
    i=SLOTS[mat]; u=(i%4)*4; v=(i//4)*4
    return [u+.05,v+.05,u+3.95,v+3.95]

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("item",choices=list(ITEMS))
    parser.add_argument("--project-uuid")
    parser.add_argument("--dry-run",action="store_true")
    args=parser.parse_args()
    name,texture,builder=ITEMS[args.item]
    rows=builder()
    assert len({r["name"] for r in rows})==len(rows)
    for r in rows:
        assert all(a<b for a,b in zip(r["from"],r["to"])),r["name"]
        assert all(-16<=v<=32 for v in r["from"]+r["to"]),r["name"]
    summary={"asset":name,"cubes":len(rows),"display":sorted(DISPLAY[args.item])}
    if args.dry_run: print(json.dumps(summary)); return
    target=OUT/(name+".bbmodel")
    if target.exists(): raise RuntimeError("Revision exists; do not overwrite earlier work")
    client=Client()
    def guard():
        info=data(client.call("get_project_info",{})); project=info["project"]
        if project["uuid"]!=args.project_uuid or project["name"]!=name: raise RuntimeError("Active project changed")
        return info
    def call(tool,arguments): guard(); return client.call(tool,arguments)
    if any(guard()["counts"].get(key,0) for key in ["cubes","meshes","groups","textures"]): raise RuntimeError("Project must be empty")
    call("set_mode",{"mode_id":"edit"})
    call("create_texture",{"name":texture+".png","width":128,"height":128,"fill_color":COLORS[0]})
    call("risky_eval",{"code":PAINT.replace("COLORS",json.dumps(COLORS))})
    # Pack path luma:item/<texture>, so the exported JSON references the namespaced texture.
    call("risky_eval",{"code":"(()=>{const t=Texture.all[0];t.namespace='luma';t.folder='item';return t.name})()"})
    call("add_group",{"name":"item","origin":[8,8,8],"parent":"root"})
    for mat in SLOTS:
        batch=[{k:v for k,v in r.items() if k!="mat"} for r in rows if r["mat"]==mat]
        if batch:
            call("place_cube",{"elements":batch,"group":"item","texture":texture+".png",
                              "faces":[{"face":f,"uv":uv_for(mat)} for f in ["north","south","east","west","up","down"]]})
    display=json.dumps(DISPLAY[args.item])
    call("risky_eval",{"code":"(()=>{const d="+display+";for(const [k,v] of Object.entries(d)){Project.display_settings[k]=new DisplaySlot(k,v)}return Object.keys(Project.display_settings)})()"})
    OUT.mkdir(parents=True,exist_ok=True)
    embedded(call("export_model",{"codec_id":"project","result_format":"embedded","max_content_length":2000000}),target)
    java=OUT/(name+".json")
    embedded(call("export_model",{"codec_id":"java_block","result_format":"embedded","max_content_length":2000000}),java)
    # The manifest hashes this file and .gitattributes stores JSON as LF, so write LF here too.
    java.write_bytes(java.read_bytes().replace(b"\r\n",b"\n"))
    model=json.loads(target.read_text(encoding="utf-8"))
    (OUT/(name+".png")).write_bytes(base64.b64decode(model["textures"][0]["source"].split(",",1)[1]))
    call("create_offscreen_view",{"id":"item_qa","width":1200,"height":1200,"copy_view":"none"})
    try:
        # Blockbench draws Java models shifted by -8 on X and Z.
        center={"runeblade":[0,14,0],"halo":[0,8,0],"crown":[0,16,0]}[args.item]
        zoom={"runeblade":.8,"halo":1.2,"crown":1.1}[args.item]
        for view,offset in {"preview":[50,35,-60],"front":[0,0,-100],"right":[-100,0,0],"top":[0,100,.01]}.items():
            camera=[center[i]+offset[i] for i in range(3)]
            result=call("set_camera_angle",{"view":"item_qa","position":camera,"target":center,"projection":"orthographic","zoom":zoom})
            image=next(b for b in result["content"] if b.get("type")=="image")
            (OUT/(name+"-"+view+".png")).write_bytes(base64.b64decode(image["data"]))
    finally: dispose_view(client,"item_qa")
    print(json.dumps({"exported":str(target),**summary}),flush=True)

if __name__=="__main__": main()
