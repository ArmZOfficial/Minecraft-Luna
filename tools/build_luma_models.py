"""Build the first Luma assets through the connected Blockbench MCP (no offline fake exports)."""
import base64
import json
from pathlib import Path
from blockbench_mcp import Client

OUT = Path('output/lobby-concept/assets/models').resolve()
OUT.mkdir(parents=True, exist_ok=True)
COLORS = ['#124653','#e8dfcb','#c99739','#47ced6','#c89775','#343334','#6b4932','#17252b']
client = Client()
manifest = []

def call(name, args):
    return client.call(name, args)

def content(result):
    for block in result.get('content', []):
        if block.get('type') == 'text':
            try:
                return json.loads(block['text'])
            except ValueError:
                continue
    return result.get('structuredContent', {})

def texture(name):
    call('create_texture', {'name':name,'width':128,'height':128,'fill_color':COLORS[0]})
    code = "(()=>{const t=Texture.all[0];t.edit((canvas)=>{const c=canvas.getContext('2d');const colors="+json.dumps(COLORS)+";colors.forEach((color,i)=>{c.fillStyle=color;c.fillRect(i*16,0,16,128);c.fillStyle='rgba(255,255,255,0.06)';for(let y=0;y<128;y+=8){c.fillRect(i*16+(y%16),y,3,3)}})},{edit_name:'Luma pixel palette'});return {texture:t.name,painted:true}})()"
    call('risky_eval', {'code':code})

def group(name, origin, parent='root'):
    call('add_group', {'name':name,'origin':origin,'parent':parent})

def cube(name, a, b, color):
    return (name,a,b,color)

def cubes(bone, rows, tex):
    by_color = {}
    for name,a,b,color in rows:
        by_color.setdefault(color,[]).append({'name':name,'from':a,'to':b})
    for color, elements in by_color.items():
        uv = [color*2+0.1,0.1,color*2+1.9,1.9]
        call('place_cube', {'elements':elements,'group':bone,'texture':tex,
            'faces':[{'face':face,'uv':uv} for face in ['north','south','east','west','up','down']]})

def save_export(codec, file):
    result = call('export_model', {'codec_id':codec,'result_format':'embedded','max_content_length':2000000})
    for block in result.get('content', []):
        if block.get('type') == 'resource':
            resource = block['resource']
            if 'text' in resource:
                file.write_text(resource['text'], encoding='utf-8')
                return
            if 'blob' in resource:
                file.write_bytes(base64.b64decode(resource['blob']))
                return
    info = content(result)
    raw = info.get('content')
    if not raw:
        raise RuntimeError('No complete model export returned')
    file.write_text(raw if isinstance(raw,str) else json.dumps(raw,ensure_ascii=False),encoding='utf-8')

def snapshot(name, height=32):
    call('set_mode', {'mode_id':'edit'})
    result = call('set_camera_angle', {'position':[48,35,-60],'target':[0,height/2,0],
        'projection':'orthographic','zoom':0.75,'max_size':1200})
    block = next(b for b in result['content'] if b.get('type') == 'image')
    (OUT/(name+'-preview.png')).write_bytes(base64.b64decode(block['data']))

def build_npc(name, variant, height):
    if name != 'npc_arcane_banker':
        call('create_project', {'name':name,'format':'free'})
    else:
        # The first empty project was created in the connection check; preserve it.
        if content(call('get_project_info', {})).get('elements_count',0):
            raise RuntimeError('First project is no longer empty')
    if name != 'npc_arcane_banker':
        texture(name)
    else:
        code="(()=>{const t=Texture.all[0];t.edit((canvas)=>{const c=canvas.getContext('2d');const colors="+json.dumps(COLORS)+";colors.forEach((v,i)=>{c.fillStyle=v;c.fillRect(i*16,0,16,128)})},{edit_name:'Luma palette'});return {painted:true}})()"
        call('risky_eval', {'code':code})
    call('set_mode', {'mode_id':'edit'})
    for bone,pivot,parent in [('root',[0,0,0],'root'),('body',[0,12,0],'root'),
        ('hi_head',[0,24,0],'body'),('right_arm',[-6,22,0],'body'),
        ('right_forearm',[-6,16,0],'right_arm'),('left_arm',[6,22,0],'body'),
        ('left_forearm',[6,16,0],'left_arm'),('right_leg',[-2,12,0],'root'),
        ('left_leg',[2,12,0],'root')]:
        group(bone,pivot,parent)
    cubes('body',[
        cube('coat',[-4,12,-2],[4,24,2],0),cube('belt',[-4.3,12,-2.3],[4.3,14,2.3],6),
        cube('lapel_left',[-3.3,14,-2.4],[-2.1,24,-2.05],1),
        cube('lapel_right',[2.1,14,-2.4],[3.3,24,-2.05],1),
        cube('clasp',[-1,20,-2.8],[1,22,-2.4],2),cube('clasp_gem',[-.5,20.5,-3],[.5,21.5,-2.8],3),
        cube('coat_tail_r',[-4.2,5,-2.3],[-.4,12,2.3],0),cube('coat_tail_l',[.4,5,-2.3],[4.2,12,2.3],0),
        cube('trim_r',[-4.3,5,-2.5],[-3.5,12,-2.2],2),cube('trim_l',[3.5,5,-2.5],[4.3,12,-2.2],2)],name)
    hair = 6 if variant=='smith' else 1
    head_rows=[cube('face',[-4,24,-4],[4,32,4],4),cube('hair_top',[-4.2,30.5,-4.2],[4.2,33,4.2],hair),
        cube('hair_back',[-4.2,24,3.5],[4.2,31,4.3],hair),
        cube('eye_r',[-2.7,27.4,-4.1],[-1.6,28.4,-4],3),cube('eye_l',[1.6,27.4,-4.1],[2.7,28.4,-4],3),
        cube('nose',[-.6,25.7,-4.7],[.6,27.1,-4],4)]
    if variant in ['banker','smith','mage']:
        head_rows += [cube('beard',[-2.6,23,-4.6],[2.6,25.9,-4],hair),cube('beard_tip',[-1.5,21.5,-4.5],[1.5,23,-4],hair)]
    if variant in ['banker','smith']:
        gy=30 if variant=='smith' else 27.2
        head_rows += [cube('spectacles_r',[-3.3,gy,-4.4],[-.8,gy+1.8,-4.15],2),
            cube('lens_r',[-2.9,gy+.3,-4.5],[-1.2,gy+1.5,-4.4],3),
            cube('spectacles_l',[.8,gy,-4.4],[3.3,gy+1.8,-4.15],2),
            cube('lens_l',[1.2,gy+.3,-4.5],[2.9,gy+1.5,-4.4],3)]
    if variant=='mage':
        head_rows += [cube('hood_top',[-4.8,32,-4.5],[4.8,35,4.8],0),
            cube('hood_r',[-4.8,24,-4.5],[-4,32,4.8],0),cube('hood_l',[4,24,-4.5],[4.8,32,4.8],0)]
    cubes('hi_head',head_rows,name)
    for side,sign in [('right',-1),('left',1)]:
        lo,hi=sorted([sign*4,sign*8])
        cubes(side+'_arm',[cube(side+'_sleeve',[lo,16,-2],[hi,23,2],4 if variant=='smith' else 0),
            cube(side+'_shoulder',[lo-.4,22,-2.4],[hi+.4,24,2.4],2)],name)
        cubes(side+'_forearm',[cube(side+'_forearm',[lo,11,-2],[hi,16,2],4),
            cube(side+'_cuff',[lo-.2,14,-2.2],[hi+.2,16,2.2],1)],name)
        l,h=sorted([sign*.3,sign*3.7])
        cubes(side+'_leg',[cube(side+'_leg',[l,1,-1.8],[h,12,1.8],5),
            cube(side+'_boot',[l-.2,0,-2.5],[h+.2,3,2],7)],name)
    if variant=='smith':
        cubes('body',[cube('apron',[-3.2,6,-2.8],[3.2,18,-2.4],7),cube('apron_rune',[-.5,10,-3],[.5,15,-2.8],3)],name)
        cubes('right_forearm',[cube('hammer_grip',[-6.5,7,-1],[-5.5,16,0],6),
            cube('hammer_head',[-9,15,-2],[-3,18,1],5)],name)
    elif variant=='banker':
        cubes('left_forearm',[cube('ledger',[4.5,10,-3],[9,13,-.5],0),cube('ledger_edges',[4.3,12.7,-3.2],[9.2,13.2,-.3],2)],name)
    elif variant=='warden':
        cubes('left_forearm',[cube('scroll',[4.5,10,-3],[8.5,12,-1],1),cube('scroll_seal',[6,9.8,-3.2],[7,12.2,-3],2)],name)
    else:
        cubes('left_forearm',[cube('staff',[6.5,0,-1],[7.5,26,0],6),cube('staff_cap',[5.5,26,-2],[8.5,27,1],2),
            cube('staff_crystal',[6,27,-1.5],[8,31,.5],3)],name)
    idle={'body':[{'time':0,'position':[0,0,0]},{'time':2,'position':[0,.25,0]},{'time':4,'position':[0,0,0]}],
        'hi_head':[{'time':0,'rotation':[0,-2,0]},{'time':2,'rotation':[0,2,0]},{'time':4,'rotation':[0,-2,0]}]}
    greet={'right_arm':[{'time':0,'rotation':[0,0,0]},{'time':.3,'rotation':[-35,0,0]},
        {'time':.7,'rotation':[-30,0,-8]},{'time':1.2,'rotation':[0,0,0]}]}
    action = {'banker':'count_coins','smith':'hammer','warden':'offer_scroll','mage':'cast'}[variant]
    work={'right_arm':[{'time':0,'rotation':[0,0,0]},{'time':.35,'rotation':[-65,0,0]},
        {'time':.65,'rotation':[-15,0,0]},{'time':1.4,'rotation':[0,0,0]}]}
    if variant!='smith':
        work={'left_arm':[{'time':0,'rotation':[0,0,0]},{'time':.4,'rotation':[-35,0,0]},
            {'time':1,'rotation':[-35,0,0]},{'time':1.4,'rotation':[0,0,0]}]}
    for aname,length,loop,bones in [('idle',4,True,idle),('greet',1.2,False,greet),(action,1.4,False,work)]:
        call('create_animation', {'name':aname,'loop':loop,'animation_length':length,'bones':bones})
    # Native animation creation prefixes names; normalize for ModelEngine using a scoped Undo edit.
    call('risky_eval', {'code':"(()=>{Undo.initEdit({animations:Animation.all.slice()});for(const a of Animation.all)a.name=a.name.replace(/^animation\\./,'');Undo.finishEdit('ModelEngine animation names');return Animation.all.map(a=>a.name)})()"})
    call('list_export_formats', {'only_current_format':True})
    save_export('project',OUT/(name+'.bbmodel'))
    snapshot(name)
    model=json.loads((OUT/(name+'.bbmodel')).read_text(encoding='utf-8'))
    for tex in model.get('textures',[]):
        source=tex.get('source','')
        if source.startswith('data:image/png;base64,'):
            (OUT/(name+'.png')).write_bytes(base64.b64decode(source.split(',',1)[1]))
    manifest.append({'id':name,'format':'free','status':'pilot-model-exported','animations':['idle','greet',action],
        'cubes':len(model.get('elements',[])),'height_target':height,'runtime_tested':False})
    print(json.dumps(manifest[-1]),flush=True)

for entry in [('npc_arcane_banker','banker',2.0),('npc_rune_smith','smith',2.2),
              ('npc_quest_warden','warden',2.1),('npc_portal_mage','mage',2.3)]:
    build_npc(*entry)

for name in ['item_aether_halo','item_runeblade']:
    call('create_project',{'name':name,'format':'java_block'})
    texture(name)
    group('item',[8,8,8])
    if name=='item_aether_halo':
        rows=[cube('ring_north',[1,8,1],[15,9,2],2),cube('ring_south',[1,8,14],[15,9,15],2),
            cube('ring_west',[1,8,2],[2,9,14],2),cube('ring_east',[14,8,2],[15,9,14],2)]
        for i,(x,z) in enumerate([(1,1),(13,1),(1,13),(13,13)]):
            rows += [cube('corner_'+str(i),[x,7.5,z],[x+2,9.5,z+2],2),
                cube('gem_'+str(i),[x+.4,9.5,z+.4],[x+1.6,10,z+1.6],3)]
    else:
        rows=[cube('blade',[6.5,8,7.5],[9.5,26,8.5],1),cube('tip',[7,26,7.5],[9,28,8.5],1),
            cube('rune',[7.6,9,7.3],[8.4,25,7.5],3),cube('guard',[3,7,7],[13,9,9],2),
            cube('grip',[7,1,7],[9,7,9],0),cube('pommel',[6.5,-1,6.5],[9.5,1,9.5],2)]
    cubes('item',rows,name)
    call('list_export_formats',{'only_current_format':True})
    save_export('project',OUT/(name+'.bbmodel'))
    save_export('java_block',OUT/(name+'.json'))
    # Items contain no skeleton animations; animated texture/state mapping is a pack task.
    snapshot(name,20)
    model=json.loads((OUT/(name+'.bbmodel')).read_text(encoding='utf-8'))
    source=model['textures'][0]['source']
    (OUT/(name+'.png')).write_bytes(base64.b64decode(source.split(',',1)[1]))
    manifest.append({'id':name,'format':'java_block','status':'pilot-model-exported','animations':[],
        'cubes':len(model.get('elements',[])),'runtime_tested':False})
    print(json.dumps(manifest[-1]),flush=True)

(OUT/'manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2),encoding='utf-8')
