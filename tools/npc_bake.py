"""Per-face baked textures for the NPC v2 rigs (free format, pixel UVs).

Each builder keeps its geometry and colour indices; MATS maps an index to a baked material or art,
OVERRIDES maps single cubes, and bake_npc() returns the atlas plus pixel UVs per cube face.
Density is 4 texels per unit (64px per block) because NPCs are viewed close up.
"""
from texture_bake import RAMPS, KIND, ramp, bake, paint_face, shade

DENSITY = 4

RAMPS.update({
 "skin_mid":ramp("#6e3f2c","#8f5a40","#b07656","#c99070","#dcaa8a","#ecc4a6","#f7dccb"),
 "skin_pale":ramp("#7c5444","#9e7262","#bf917f","#d9ae9b","#e8c7b5","#f3dccf","#fbefe7"),
 "skin_tan":ramp("#5e3420","#7d4a30","#9c6343","#b97c58","#d39a74","#e5b593","#f2d0b5"),
 "skin_fair":ramp("#8a5a48","#ad7a64","#cf9c82","#e8b99c","#f2c9a8","#f8dcc4","#fff0e2"),
 "hair_white":ramp("#5d676d","#7f8a90","#a3adb2","#c3cbcf","#d9e0e3","#ebf0f2","#ffffff"),
 "hair_brown":ramp("#1f120a","#311d10","#452918","#5a3826","#73492f","#8d5d3d","#a8764f"),
 "hair_ivory":ramp("#8c8170","#aca08c","#c9bea9","#dfd5c2","#ece4d4","#f6f1e6","#ffffff"),
 "cloth_charcoal":ramp("#0d0e11","#16181c","#202329","#2b2e35","#373b44","#454a55","#575d6a"),
 "cloth_dark":ramp("#0a0b0d","#121317","#1a1c21","#24262b","#2e3137","#3a3e46","#4a4f59"),
 "cloth_brown":ramp("#0e0b09","#17120f","#211b17","#2d2622","#3a312b","#4a3f37","#5d5047"),
 "teal_deep":ramp("#03141a","#06202a","#0a2c36","#0e3640","#14485a","#1c5c6c","#2a7480"),
 "teal_light":ramp("#0f3d40","#16585c","#1f7378","#2f8f95","#45a9ad","#66c3c4","#94dbd8"),
 "leather_dark":ramp("#120a06","#1d110b","#2a1a11","#3e2a20","#52382b","#684837","#7f5b46"),
 "iron":ramp("#0b0c0e","#141518","#1d1f23","#26282c","#33363c","#43474f","#585d66"),
})
RAMPS["cloth_ivory"]=RAMPS["ivory"]; RAMPS["fur_ivory"]=RAMPS["hair_ivory"]; RAMPS["shadow"]=RAMPS["teal_deep"]
for name in ("skin_mid","skin_pale","skin_tan","skin_fair"): KIND[name]="skin"
for name in ("hair_white","hair_brown","hair_ivory"): KIND[name]="hair"
for name in ("cloth_charcoal","cloth_dark","cloth_brown","teal_deep","teal_light","cloth_ivory"): KIND[name]="cloth"
KIND.update({"fur_ivory":"fur","leather_dark":"leather","iron":"metal","shadow":"plain"})

# Moonfall boss: dark blue stone, armour plates, ash, violet crystal and the void core.
RAMPS.update({
 "boss_stone":ramp("#15181f","#222731","#2f3542","#414858","#555d6e","#6c7486","#8c94a5"),
 "boss_stone_light":ramp("#3a404c","#4d5462","#626a7a","#777d8a","#8f96a3","#a9afba","#c7ccd4"),
 "boss_armor":ramp("#1d2530","#2a3442","#3a4656","#536376","#687a90","#8195ab","#a3b4c6"),
 "ash":ramp("#5d5953","#7a756e","#9a958c","#b3aea4","#c2bdb0","#d6d2c7","#ece9e1"),
 "violet_gem":ramp("#24093f","#3c1468","#5a2496","#7140d2","#9b6ff0","#c7a8ff","#f3eaff"),
 "void":ramp("#07030c","#0f0718","#170c24","#241436","#331e4a","#45295f","#5a3778"),
})
KIND.update({"boss_stone":"slab","boss_stone_light":"slab","boss_armor":"slab","ash":"slab",
             "violet_gem":"gem","void":"plain"})

def fill(c,mat,face,rng):
    c.px=paint_face(mat,face,c.w,c.h,rng).px

def portrait(skin,eye_rows,eye_centers,eye_half,iris,old=False,young=False,mouth=None):
    """Face art for the head's north face, laid out in fractions so it scales with the face."""
    def art(c,face,rng):
        fill(c,skin,face,rng)
        if face!="north": return
        r=RAMPS[skin]; w,h=c.w,c.h
        for y in range(h):
            for x in range(w):
                if x<w*.07 or x>=w*.93: c.set(x,y,r[2])                      # cheek turn shading
        y0=int(eye_rows[0]*h); y1=max(y0+3,int(eye_rows[1]*h))
        for cxf in eye_centers:
            cx=int(cxf*w); hw=max(3,int(eye_half*w))
            for y in range(y0,y1):
                for x in range(cx-hw,cx+hw): c.set(x,y,(242,238,230))       # sclera
            iw=max(2,hw-1)
            for y in range(y0,y1):
                for x in range(cx-iw//2-1,cx+iw//2+1): c.set(x,y,iris[0] if y==y0 else iris[1])
            for y in range(y0+1,y1):
                for x in range(cx-1,cx+1): c.set(x,y,iris[2])                 # pupil
            c.set(cx-iw//2-1,y0,iris[3])                                     # catch-light
            lash=(58,42,38) if young else r[0]
            for x in range(cx-hw-1,cx+hw+1): c.set(x,y0-1,lash)              # upper lid
            if young:
                c.set(cx+hw,y0-2,lash)
                for x in range(cx-hw,cx+hw-1): c.set(x,y1+2,(236,160,150))   # blush
            if old:
                for x in range(cx-hw,cx+hw): c.set(x,y1,r[2])                 # under-eye bag
                c.set(cx+hw,y0+1,r[2]); c.set(cx+hw+1,y0+2,r[2])              # crow's feet
        if old:
            for fy in (.12,.2):
                for x in range(int(w*.28),int(w*.72)):
                    if x%5: c.set(x,int(fy*h),r[2])
        if mouth is not None:
            my=int(mouth*h)
            for x in range(int(w*.42),int(w*.58)): c.set(x,my,(178,96,92))
            c.set(int(w*.42)-1,my-1,(178,96,92)); c.set(int(w*.58),my-1,(178,96,92))
    return art

def panel(base,motif):
    """Cloth panel: base material on every face, motif(c,rng) drawn on the big north/south faces."""
    def art(c,face,rng):
        fill(c,base,face,rng)
        if face in ("north","south") and c.w>=6 and c.h>=6: motif(c,face,rng)
    return art

def gold_border(c,inset=1):
    g=RAMPS["gold"]
    for x in range(inset,c.w-inset): c.set(x,inset,g[5]); c.set(x,c.h-1-inset,g[2])
    for y in range(inset,c.h-inset): c.set(inset,y,g[4]); c.set(c.w-1-inset,y,g[2])

def diamond(c,cx,cy,s,col,fillcol=None):
    for k in range(s+1):
        for x,y in ((cx-k,cy-s+k),(cx+k,cy-s+k),(cx-k,cy+s-k),(cx+k,cy+s-k)): c.set(x,y,col)
    if fillcol:
        for k in range(1,s):
            for x in range(cx-k+1,cx+k): c.set(x,cy-s+k,fillcol); c.set(x,cy+s-k,fillcol)
        for x in range(cx-s+1,cx+s): c.set(x,cy,fillcol)

def embroidery(c,face,rng):
    """Waistcoat/coat embroidery: gold side rules and a column of small gem diamonds."""
    g=RAMPS["gold"]; gem=RAMPS["gem"]
    for y in range(c.h):
        c.set(1,y,g[4]); c.set(c.w-2,y,g[3])
    step=max(6,c.h//5)
    for cy in range(step//2+1,c.h-2,step): diamond(c,c.w//2,cy,2,g[4],gem[3])

def ledger_cover(c,face,rng):
    gold_border(c,1); g=RAMPS["gold"]; gem=RAMPS["gem"]
    for x in range(c.w//4,c.w-c.w//4):
        c.set(x,c.h//5,g[3]); c.set(x,c.h-1-c.h//5,g[3])
    diamond(c,c.w//2,c.h//2,max(2,min(c.w,c.h)//6),g[5],gem[4])

def coin_face(c,face,rng):
    g=RAMPS["gold"]; cx,cy=(c.w-1)/2,(c.h-1)/2
    for y in range(c.h):
        for x in range(c.w):
            e=max(abs(x-cx)/(c.w/2),abs(y-cy)/(c.h/2))
            c.set(x,y,g[1] if e>.86 else (g[5] if e>.7 else (g[2] if e>.58 else g[4])))
    c.set(int(cx),int(cy),g[6]); c.set(1,1,g[6])

def key_emblem(c,face,rng):
    """Back panel: inset gold frame and a vault key with a gem in its bow."""
    if face!="south": return
    gold_border(c,1); g=RAMPS["gold"]; gem=RAMPS["gem"]
    cx=c.w//2; bow=max(3,c.w//6)
    top=c.h//6
    for k in range(-bow,bow+1):
        for x,y in ((cx+k,top),(cx+k,top+2*bow),(cx-bow,top+bow+k),(cx+bow,top+bow+k)): c.set(x,y,g[5])
    diamond(c,cx,top+bow,max(1,bow//2),gem[4],gem[3])
    for y in range(top+2*bow,int(c.h*.85)): c.set(cx,y,g[4]); c.set(cx+1,y,g[2])
    for y,l in ((int(c.h*.72),bow),(int(c.h*.8),bow-1)):
        for x in range(cx+2,cx+2+l): c.set(x,y,g[4])

def rune_apron(c,face,rng):
    """Smith apron: gold-ruled leather with a glowing rune sized to the front face."""
    if face!="north": return
    gold_border(c,0); glow=RAMPS["gem"]
    cx=c.w//2; s=max(3,c.w//7); top=int(c.h*.15)
    pts=set()
    for k in range(s+1):
        for x,y in ((cx-k,top+k),(cx+k,top+k),(cx-k,top+2*s-k),(cx+k,top+2*s-k)): pts.add((x,y))
    for y in range(top+2*s,int(c.h*.75)): pts.add((cx,y))
    for x,y in pts:
        for dx,dy in ((1,0),(-1,0),(0,1),(0,-1)):
            if (x+dx,y+dy) not in pts: c.set(x+dx,y+dy,glow[1])
    for x,y in pts: c.set(x,y,glow[4])
    for dy in range(3):
        for dx in range(-1,2): c.set(cx+dx,int(c.h*.82)+dy,glow[5])

def coat_motif(c,face,rng):
    """Warden coat tails: gold hem rule and two cross motifs near the bottom."""
    g=RAMPS["gold"]
    hem=c.h-max(3,c.h//10)
    for x in range(c.w): c.set(x,hem,g[4]); c.set(x,hem+1,g[2])
    for cx in (c.w//4,3*c.w//4):
        cy=int(c.h*.72)
        for d in range(-3,4): c.set(cx,cy+d,g[4]); c.set(cx+d,cy,g[4])
        c.set(cx,cy,g[6])

def warden_emblem(c,face,rng):
    if face!="south": return
    g=RAMPS["gold"]; gem=RAMPS["gem"]; cx=c.w//2
    for y in range(c.h//8,c.h-c.h//8): c.set(cx,y,g[3])
    diamond(c,cx,c.h//2,max(3,c.w//4),g[5],None)
    diamond(c,cx,c.h//2,max(1,c.w//10),gem[4],gem[3])
    diamond(c,cx,c.h//6,2,g[5],gem[4])

def tabard(c,face,rng):
    g=RAMPS["gold"]; gem=RAMPS["gem"]; cx=c.w//2
    for y in range(2,int(c.h*.55)): c.set(cx,y,g[4]); c.set(cx+1,y,g[2])
    for x in range(cx-3,cx+5): c.set(x,int(c.h*.18),g[4])
    diamond(c,cx,int(c.h*.72),max(3,c.w//4),g[5],None)
    diamond(c,cx,int(c.h*.72),1,gem[5],gem[4])

def robe_hem(c,face,rng):
    g=RAMPS["gold"]; gem=RAMPS["gem"]
    hem=c.h-max(3,c.h//9)
    for x in range(c.w): c.set(x,hem,g[4]); c.set(x,hem+1,g[3])
    for cx in (c.w//4,3*c.w//4):
        diamond(c,cx,hem-5,2,g[4],gem[4])

def mage_emblem(c,face,rng):
    if face!="south": return
    g=RAMPS["gold"]; gem=RAMPS["gem"]; cx=c.w//2
    for y in range(1,c.h-1): c.set(cx,y,g[4])
    diamond(c,cx,int(c.h*.4),max(3,c.w//3),g[5],None)
    diamond(c,cx,int(c.h*.4),1,gem[5],gem[4])

def boss_rune(c,face,rng):
    """Boss rune panel: dark stone with a glowing cyan sigil on its front face."""
    fill(c,"boss_stone",face,rng)
    if face!="north": return
    glow=RAMPS["gem"]; cx,cy=c.w//2,c.h//2; s=max(2,min(c.w,c.h)//3)
    for k in range(-s,s+1):
        c.set(cx+k,cy,glow[4]); c.set(cx,cy+k,glow[4])
    diamond(c,cx,cy,max(1,s//2),glow[5],glow[3])
    c.set(cx,cy,glow[6])

def moon_panel(c,face,rng):
    """Back plate: armour stone with a violet crescent and cyan star on the outward (south) face."""
    fill(c,"boss_armor",face,rng)
    if face!="south": return
    vio=RAMPS["violet_gem"]; gem=RAMPS["gem"]
    cx,cy,r=c.w/2,c.h*.45,min(c.w,c.h)*.32
    for y in range(c.h):
        for x in range(c.w):
            d1=((x-cx)**2+(y-cy)**2)**.5; d2=((x-cx-r*.45)**2+(y-cy+r*.2)**2)**.5
            if d1<=r and d2>r*.82: c.set(x,y,vio[4] if d1<r-1.5 else vio[2])
    sx,sy=int(cx+r*.55),int(cy-r*.15)
    for k in range(-2,3): c.set(sx+k,sy,gem[5]); c.set(sx,sy+k,gem[5])
    c.set(sx,sy,gem[6])

def bake_npc(rows,mats,arts,overrides=None,seed=7):
    """rows: [(bone,color,row)] -> (atlas, {cube:{face:[x0,y0,x1,y1]}}) in pixels at DENSITY."""
    overrides=overrides or {}
    flat=[{**row,"mat":overrides.get(row["name"],mats[color])} for _,color,row in rows]
    return bake(flat,DENSITY,arts,seed=seed)

def uv_js(face_uv):
    """JS that writes pixel UVs per cube face and hides the collision proxy and hitbox group."""
    import json
    return ("(()=>{const m="+json.dumps(face_uv)+";Undo.initEdit({elements:Cube.all.slice(),uv_only:true,outliner:true});"
            "for(const c of Cube.all){const f=m[c.name];if(f){c.autouv=0;for(const k in f)c.faces[k].uv=f[k]}"
            "if(c.name==='collision_proxy')c.visibility=false;c.preview_controller.updateUV(c);c.preview_controller.updateVisibility(c)}"
            "const hit=Group.all.find(g=>g.name==='hitbox');if(hit){hit.visibility=false;hit.preview_controller.updateVisibility(hit)}"
            "Undo.finishEdit('Baked per-face UV');return Cube.all.length})()")

# Market merchant: wine waistcoat, felt hat, teal plume, rolled carpet and a balance-scale crest.
RAMPS.update({
 "wine":ramp("#1f060c","#350b16","#4e1222","#691a2e","#84263c","#a0384e","#bd5466"),
 "plume":ramp("#0c3d44","#13606a","#1d8790","#36aab0","#62c8c9","#9fe2dd","#e8fbf6"),
 "felt":ramp("#160d09","#24160f","#352116","#472d1e","#5a3a27","#704a33","#8a5e42"),
})
KIND.update({"wine":"cloth","plume":"hair","felt":"leather"})

def vest_damask(c,face,rng):
    """Waistcoat front: tonal diamond lattice with gold pips and a gold hem rule."""
    r=RAMPS["wine"]; g=RAMPS["gold"]
    for y in range(2,c.h-3):
        for x in range(1,c.w-1):
            if (x+y)%6==0 or (x-y)%6==0: c.set(x,y,r[2])
    for y in range(3,c.h-4):
        for x in range(1,c.w-1):
            if (x+y)%6==3 and (x-y)%6==3 and ((x+y)//6)%2==0: c.set(x,y,g[4])   # pip at alternate diamond centres
    for x in range(c.w): c.set(x,c.h-3,g[4]); c.set(x,c.h-2,g[2])

def carpet_roll(c,face,rng):
    """Rolled rug: wine weave with gold/teal bands around the roll and a spiral on the ends."""
    fill(c,"wine",face,rng)
    g=RAMPS["gold"]; t=RAMPS["teal_light"]; r=RAMPS["wine"]
    if face in ("east","west"):
        cx,cy=(c.w-1)/2,(c.h-1)/2
        for y in range(c.h):
            for x in range(c.w):
                d=((x-cx)**2+(y-cy)**2)**.5
                c.set(x,y,(r[4],r[2],g[3],r[3])[int(d)%4])
        c.set(int(cx),int(cy),r[1]); return
    for x in range(c.w):
        k=x%14
        if k in (0,1): col=g[4] if k==0 else g[2]
        elif k in (5,9): col=t[4]
        elif k==7: col=g[5]
        else: continue
        for y in range(c.h): c.set(x,y,col)

def scarf_tail(c,face,rng):
    fill(c,"teal",face,rng)
    g=RAMPS["gold"]
    if face in ("up","down"): return
    for y in range(max(0,c.h-3),c.h):
        for x in range(c.w): c.set(x,y,g[4] if x%2==0 else RAMPS["teal"][1])
    for x in range(c.w): c.set(x,max(0,c.h-4),g[3])

def merchant_crest(c,face,rng):
    """Pack flap: gold ring around a balance scale, stitched on the outward (south) face."""
    fill(c,"leather",face,rng)
    if face!="south" or min(c.w,c.h)<12: return
    g=RAMPS["gold"]; gem=RAMPS["gem"]
    cx,cy=(c.w-1)/2,(c.h-1)/2; R=min(c.w,c.h)*.42
    for y in range(c.h):
        for x in range(c.w):
            d=((x-cx)**2+(y-cy)**2)**.5
            if R-1.2<=d<=R: c.set(x,y,g[4] if y<cy else g[3])
    ix,iy=int(cx),int(cy); s=int(R*.6)
    for y in range(iy-s,iy+s+1): c.set(ix,y,g[5])
    for x in range(ix-s,ix+s+1): c.set(x,iy-s+1,g[5])
    for px in (ix-s,ix+s):
        for k in range(3):
            for x in range(px-k,px+k+1): c.set(x,iy+k,g[4])
    for x in range(ix-2,ix+3): c.set(x,iy+s,g[4])
    c.set(ix,iy-s-1,gem[5])
