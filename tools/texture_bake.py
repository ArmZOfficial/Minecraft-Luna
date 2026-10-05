"""Bake a per-face pixel-art atlas: every cube face gets its own region at a fixed texel density.

No swatch is stretched across faces of different sizes. Each material paints from a hue-shifted
shade ramp (cool shadows, warm highlights) with its own pattern, so a 0.3-unit trim and a 16-unit
table top both read at the same pixel scale, like hand-made packs on large servers.
"""
import random
from PIL import Image

def hexrgb(h): return tuple(int(h[i:i+2],16) for i in (1,3,5))
def ramp(*hexes): return [hexrgb(h) for h in hexes]

# Dark -> light; shadows lean cool/saturated, highlights warm/desaturated.
RAMPS={
 "gold":ramp("#4a2a10","#7a4a17","#a96f22","#d39a32","#efc350","#fbe58c","#fffbd8"),
 "bronze":ramp("#2e1a0e","#4b2c16","#6b4220","#8c5a2c","#ad763c","#cc9756","#e6bd80"),
 "steel":ramp("#1f242c","#353d48","#4f5864","#6c7682","#8e98a3","#b5bec7","#e3e9ee"),
 "wood":ramp("#2c180c","#432614","#5c3720","#77492b","#925d36","#ad7545","#c79161"),
 "wood_dark":ramp("#1a0e07","#28160b","#3a2112","#4d2d19","#613a21","#77492b","#8e5c38"),
 "stone":ramp("#2d3138","#40454e","#555b65","#6c737d","#858c96","#a1a8b1","#c3c8ce"),
 "paper":ramp("#8d7a55","#ab9870","#c7b78f","#ddd0ad","#ece3c7","#f6f0dc","#fffcf0"),
 "teal":ramp("#071c22","#0c2c34","#123f49","#19535e","#226a75","#2f828c","#4fa3a8"),
 "leather":ramp("#1e1009","#2f1a0f","#432617","#583321","#6e432d","#86563b","#a06e4f"),
 "dark":ramp("#0a0b0e","#121419","#1a1d23","#22262e","#2c313a","#3a404b","#4b525e"),
 "ice":ramp("#5f9fb0","#7fb9c8","#9fcfda","#bde2ea","#d6eff4","#ebf8fa","#ffffff"),
 "gem":ramp("#063c47","#0b6676","#1495a6","#24c3d3","#5ee6f2","#a8f6fb","#ffffff"),
 "ruby":ramp("#3a070d","#621019","#8f1b26","#bd2d38","#e2525b","#f78c90","#ffe0e0"),
}
RAMPS["teal_metal"]=RAMPS["teal"]; RAMPS["teal_leather"]=RAMPS["teal"]
# Which pattern each material uses.
KIND={"gold":"metal","bronze":"metal","steel":"metal","wood":"wood","wood_dark":"wood","stone":"stone",
      "paper":"paper","teal":"cloth","leather":"leather","dark":"plain","ice":"ice","gem":"gem","ruby":"gem",
      "teal_metal":"metal","teal_leather":"leather"}

def face_size(row,face,density):
    d=[b-a for a,b in zip(row["from"],row["to"])]
    w,h={"north":(d[0],d[1]),"south":(d[0],d[1]),"east":(d[2],d[1]),"west":(d[2],d[1]),"up":(d[0],d[2]),"down":(d[0],d[2])}[face]
    return max(1,round(w*density)),max(1,round(h*density))

class Canvas:
    def __init__(self,w,h): self.w,self.h=w,h; self.px=[[None]*w for _ in range(h)]
    def set(self,x,y,c):
        if 0<=x<self.w and 0<=y<self.h: self.px[y][x]=c
    def rect(self,x,y,w,h,c):
        for yy in range(y,y+h):
            for xx in range(x,x+w): self.set(xx,yy,c)

def shade(r,i): return r[max(0,min(len(r)-1,i))]

def paint_face(mat,face,w,h,rng,art=None):
    """Return a Canvas of w*h pixels for one face of one material."""
    c=Canvas(w,h)
    if art: art(c,face,rng); return c
    r=RAMPS[mat]; kind=KIND[mat]
    # Engine shading already darkens sides/bottom; keep only a gentle top lift.
    base=3+(1 if face=="up" else 0)-(1 if face=="down" else 0)
    for y in range(h):
        for x in range(w):
            v=base
            if face not in ("up","down") and h>=4:
                v+= 1 if y<h*.2 else (-1 if y>h*.8 else 0)   # soft top light / bottom occlusion
            c.set(x,y,shade(r,v))
    if min(w,h)<=2 and kind not in ("gem",):
        # Trim-sized faces: patterns turn into noise at 1-2px, so keep a clean tone with a lit edge.
        for x in range(w): c.set(x,0,shade(r,base+1))
        return c
    if kind=="metal":
        for y in range(h):
            for x in range(w):
                if abs((x+y)-(w+h)*.3)<max(1,(w+h)*.07): c.set(x,y,shade(r,base+1))   # one soft specular band
        for _ in range(max(1,w*h//40)):
            c.set(rng.randrange(w),rng.randrange(h),shade(r,base-1))                  # wear specks
        bevel(c,r,base,hard=True)
        if w>=3 and h>=3: c.set(1,1,shade(r,6))                                       # glint
    elif kind=="wood":
        along=w>=h
        n=h if along else w; L=w if along else h
        for i in range(n):
            tone=base+(rng.choice([0,0,0,-1,1]))
            for j in range(L):
                x,y=(j,i) if along else (i,j)
                t=tone
                if rng.random()<.08: t-=1
                c.set(x,y,shade(r,t))
            if i%4==3:                                                                # grain line
                for j in range(L):
                    if rng.random()<.8:
                        x,y=(j,i) if along else (i,j); c.set(x,y,shade(r,base-2))
        if w*h>40:
            kx,ky=rng.randrange(w),rng.randrange(h); c.set(kx,ky,shade(r,base-3))       # knot
        bevel(c,r,base,hard=False)
    elif kind=="stone":
        bw,bh=8,4
        for y in range(h):
            row=y//bh; off=(bw//2)*(row%2)
            for x in range(w):
                bx=(x+off)//bw; tone=base+((bx*7+row*3)%3)-1
                if y%bh==bh-1 or (x+off)%bw==bw-1: tone=base-2                           # mortar
                elif y%bh==0 or (x+off)%bw==0: tone+=1                                   # brick top/left light
                c.set(x,y,shade(r,tone))
        for _ in range(max(1,w*h//60)):
            x,y=rng.randrange(w),rng.randrange(h); c.set(x,y,shade(r,base-2)); c.set(x+1,y+1,shade(r,base-2))
        bevel(c,r,base,hard=True)
    elif kind=="cloth":
        for y in range(h):
            for x in range(w):
                if (x+y)%2==0 and (x//2+y//2)%2==0: c.set(x,y,shade(r,base+1 if face!="down" else base))
        if w>=6 and h>=6:
            for x in range(1,w-1,2): c.set(x,1,shade(r,base+2)); c.set(x,h-2,shade(r,base+2))  # stitched hem
        bevel(c,r,base,hard=False)
    elif kind=="leather":
        for _ in range(max(1,w*h//25)):
            x,y=rng.randrange(w),rng.randrange(h); c.rect(x,y,2,1,shade(r,base-1))
        if w>=5 and h>=5:
            for x in range(1,w-1,2): c.set(x,1,shade(r,base+2)); c.set(x,h-2,shade(r,base+2))
        bevel(c,r,base,hard=False)
    elif kind=="paper":
        for y in range(2,h-1,3):
            for x in range(1,w-1):
                if rng.random()<.85: c.set(x,y,shade(r,base-1))
        for _ in range(max(1,w*h//50)): c.set(rng.randrange(w),rng.randrange(h),shade(r,base-2))
        for x in range(w): c.set(x,0,shade(r,base-1)); c.set(x,h-1,shade(r,base-2))
    elif kind=="ice":
        for y in range(h):
            for x in range(w):
                if (x-y)%6==0: c.set(x,y,shade(r,6))
        bevel(c,r,base,hard=True)
    elif kind=="gem":
        gem(c,r)
    elif kind=="plain":
        for _ in range(max(1,w*h//20)): c.set(rng.randrange(w),rng.randrange(h),shade(r,base+1))
    return c

def bevel(c,r,base,hard):
    if c.w<3 or c.h<3: return
    up,dn=(2,-2) if hard else (1,-1)
    for x in range(c.w): c.set(x,0,shade(r,base+up)); c.set(x,c.h-1,shade(r,base+dn))
    for y in range(1,c.h-1): c.set(0,y,shade(r,base+up-1)); c.set(c.w-1,y,shade(r,base+dn))

def gem(c,r):
    """Cut-gem look: bright top facet, lit left, dark right/bottom, table with a white glint."""
    w,h=c.w,c.h
    for y in range(h):
        for x in range(w):
            u=(x+.5)/w-.5; v=(y+.5)/h-.5
            if abs(u)<.22 and abs(v)<.22: t=5
            elif abs(v)>=abs(u): t=5 if v<0 else 2
            else: t=4 if u<0 else 3
            c.set(x,y,shade(r,t))
    if w>=4 and h>=4:
        for x in range(w): c.set(x,h-1,shade(r,0))
        for y in range(h): c.set(w-1,y,shade(r,0))
        c.set(int(w*.32),int(h*.32),shade(r,6)); c.set(int(w*.32)+1,int(h*.32),shade(r,6))
        c.set(int(w*.7),int(h*.68),shade(r,6))
    else:
        c.set(0,0,shade(r,6))

def bake(rows,density,arts=None,seed=7):
    """rows: [{name,from,to,mat}] -> (Image, {name:{face:[x0,y0,x1,y1] px}}). arts: {mat: fn(canvas,face,rng)}."""
    arts=arts or {}
    rng=random.Random(seed)
    tiles=[]
    for row in rows:
        for face in ("north","south","east","west","up","down"):
            w,h=face_size(row,face,density)
            tiles.append((row["name"],face,w,h,paint_face(row["mat"],face,w,h,rng,arts.get(row["mat"]))))
    # Shelf-pack tallest first with a 1px gutter, growing the square power-of-two atlas until it fits.
    order=sorted(tiles,key=lambda t:(-t[3],-t[2]))
    size=32
    while True:
        x=y=shelf=0; spots={}; ok=True
        for name,face,w,h,_ in order:
            if x+w+2>size: x=0; y+=shelf; shelf=0
            if y+h+2>size: ok=False; break
            spots[(name,face)]=(x+1,y+1); x+=w+2; shelf=max(shelf,h+2)
        if ok: break
        size*=2
    img=Image.new("RGBA",(size,size),(0,0,0,0)); uv={}
    for name,face,w,h,canvas in tiles:
        ox,oy=spots[(name,face)]
        for yy in range(-1,h+1):
            for xx in range(-1,w+1):
                col=canvas.px[min(h-1,max(0,yy))][min(w-1,max(0,xx))]   # gutter repeats the edge pixel
                img.putpixel((ox+xx,oy+yy),col+(255,))
        uv.setdefault(name,{})[face]=[ox,oy,ox+w,oy+h]
    return img,uv
