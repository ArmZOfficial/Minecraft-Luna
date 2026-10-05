"""Repair the open guide's hair in place, preserving its rig, animations and non-hair atlas pixels.

Requires an explicit active-project UUID. Backs up the live source (including user edits),
uses native undoable Blockbench edits, exports embedded textures and binds the saved file.
"""
import argparse
import base64
import io
import json
from pathlib import Path

from PIL import Image
from blockbench_mcp import Client
from build_crystal_guide import NAME, ROWS, guide_hair
from build_moonfall_boss import OUT, data, embedded, dispose_view
from build_luma_props import load_atlas
from texture_bake import Canvas, face_size

ROOT=Path(__file__).resolve().parents[1]
RENAMES={"cowlick":"hair_crown_sweep_right","cowlick_tip":"hair_crown_sweep_left"}
HAIR={row["name"]:row for bone,col,row in ROWS if bone=="hi_head" and col==5}

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--project-uuid',required=True)
    args=parser.parse_args()
    client=Client()
    def call(name,arguments):
        p=data(client.call('get_project_info',{}))['project']
        if p['uuid']!=args.project_uuid or p['name']!=NAME:
            raise RuntimeError('Active project changed; no edit or save performed')
        if name=='risky_eval':
            check="if(Project.uuid!==%s||Project.name!==%s)throw new Error('Active project changed');" % (json.dumps(args.project_uuid),json.dumps(NAME))
            arguments={'code':arguments['code'].replace('(()=>{','(()=>{'+check,1).replace('(async()=>{','(async()=>{'+check,1)}
        return client.call(name,arguments)

    backup=ROOT/'.cache'/f'{NAME}-before-hair-{args.project_uuid}.bbmodel'
    if backup.exists():
        raise RuntimeError('Live backup already exists; inspect it before reapplying the repair')
    embedded(call('export_model',{'codec_id':'project','result_format':'embedded','max_content_length':2000000}),backup)
    before=json.loads(backup.read_text(encoding='utf-8'))
    oldhair={e['name']:e for e in before['elements'] if RENAMES.get(e['name'],e['name']) in HAIR}
    if set(RENAMES.get(n,n) for n in oldhair)!=set(HAIR):
        raise RuntimeError('Hair pieces differ from this repair; inspect the live model first')
    original_png=base64.b64decode(before['textures'][0]['source'].split(',',1)[1])
    atlas=Image.open(io.BytesIO(original_png)).convert('RGBA')
    occupied=max(max(f['uv'][1],f['uv'][3]) for e in before['elements'] for f in e['faces'].values())
    tiles=[]
    import random
    rng=random.Random(20261006)
    for name,row in HAIR.items():
        for face in ('north','south','east','west','up','down'):
            w,h=face_size(row,face,4)
            c=Canvas(w,h); guide_hair(c,face,rng)
            tiles.append((name,face,w,h,c))
    x=0; y=int(occupied)+2; shelf=0; uv={}
    for name,face,w,h,c in sorted(tiles,key=lambda t:(-t[3],-t[2])):
        if x+w+2>atlas.width:
            x=0; y+=shelf; shelf=0
        if y+h+2>atlas.height:
            raise RuntimeError('No unused atlas space for the new hair; no mutation performed')
        ox,oy=x+1,y+1
        for yy in range(-1,h+1):
            for xx in range(-1,w+1):
                atlas.putpixel((ox+xx,oy+yy),c.px[min(h-1,max(0,yy))][min(w-1,max(0,xx))]+(255,))
        uv.setdefault(name,{})[face]=[ox,oy,ox+w,oy+h]
        x+=w+2; shelf=max(shelf,h+2)

    # Never overwrite changes the user made after the live backup was taken.
    check=ROOT/'.cache'/f'{NAME}-hair-check.bbmodel'
    embedded(call('export_model',{'codec_id':'project','result_format':'embedded','max_content_length':2000000}),check)
    latest=json.loads(check.read_text(encoding='utf-8'))
    for key in ('elements','groups','outliner','textures','animations'):
        if latest.get(key)!=before.get(key):
            raise RuntimeError('Live model changed during preparation; retry after inspecting it')
    call('animation_timeline',{'animation_id':'idle','action':'stop'})
    call('set_mode',{'mode_id':'edit'})
    code="""(()=>{const names=%s,rows=%s,uv=%s;
const hair=Cube.all.filter(c=>rows[names[c.name]||c.name]);
if(hair.length!==Object.keys(rows).length||hair.some(c=>c.parent?.name!=='hi_head'))throw new Error('Hair rig mismatch');
Undo.initEdit({elements:hair,outliner:true});
for(const c of hair){const n=names[c.name]||c.name,r=rows[n];c.name=n;c.from=r.from.slice();c.to=r.to.slice();c.rotation=[0,0,0];c.autouv=0;
for(const k in uv[n])c.faces[k].uv=uv[n][k].slice();
c.preview_controller.updateGeometry(c);c.preview_controller.updateTransform(c);c.preview_controller.updateUV(c)}
Undo.finishEdit('Guide: clean side-parted hair and fitted UV');Canvas.updateAll();
for(const c of hair)if(c.mesh)c.mesh.geometry.computeBoundingBox();return {hair:hair.length,cubes:Cube.all.length}})()""" % (json.dumps(RENAMES),json.dumps(HAIR),json.dumps(uv))
    print(json.dumps(data(call('risky_eval',{'code':code}))),flush=True)
    load_atlas(call,atlas)

    # Re-export through the native project codec; do not synthesize a .bbmodel.
    target=OUT/f'{NAME}.bbmodel'
    embedded(call('export_model',{'codec_id':'project','result_format':'embedded','max_content_length':2000000}),target)
    after=json.loads(target.read_text(encoding='utf-8'))
    originals={e['uuid']:e for e in before['elements'] if e['name'] not in oldhair}
    assert all(e==originals[e['uuid']] for e in after['elements'] if e['uuid'] in originals)
    assert len(before['elements'])==len(after['elements'])
    assert before['animations']==after['animations']
    assert before.get('groups')==after.get('groups') and before['outliner']==after['outliner']
    new_png=base64.b64decode(after['textures'][0]['source'].split(',',1)[1])
    exported=Image.open(io.BytesIO(new_png)).convert('RGBA')
    old_atlas=Image.open(io.BytesIO(original_png)).convert('RGBA')
    assert exported.size==old_atlas.size
    assert exported.crop((0,0,exported.width,int(occupied)+1)).tobytes()==old_atlas.crop((0,0,old_atlas.width,int(occupied)+1)).tobytes()
    (OUT/f'{NAME}.png').write_bytes(new_png)
    saved=data(call('risky_eval',{'code':"(()=>{Codecs.project.afterSave(%s);return {saved:Project.saved,path:Project.save_path,name:Project.name}})()" % json.dumps(str(target).replace('\\','/'))}))
    assert saved['saved'] and Path(saved['path'])==target

    call('create_offscreen_view',{'id':'hair_after','width':1400,'height':1400,'copy_view':'none'})
    try:
        cameras={'preview':([60,40,-90],[0,17.5,0],.74),
                 'front':([0,17.5,-100],[0,17.5,0],.74),
                 'right':([100,17.5,0],[0,17.5,0],.74),
                 'back':([0,17.5,100],[0,17.5,0],.74),
                 'hair-detail':([26,40,-90],[0,27.8,0],1.5),
                 'hair-front':([0,28,-100],[0,28,0],1.65),
                 'hair-top':([0,120,-.01],[0,29,0],1.8)}
        for suffix,(position,look,zoom) in cameras.items():
            r=call('set_camera_angle',{'view':'hair_after','position':position,'target':look,'projection':'orthographic','zoom':zoom})
            im=next(b for b in r['content'] if b.get('type')=='image')
            (OUT/f'{NAME}-{suffix}.png').write_bytes(base64.b64decode(im['data']))
    finally:
        dispose_view(client,'hair_after')
    print(json.dumps({'saved':saved,'backup':str(backup),'hair_pieces':len(HAIR),'atlas':exported.size,
                     'preserved_non_hair_cubes':len(originals),'preserved_animations':len(after['animations'])}),flush=True)

if __name__=='__main__':
    main()
