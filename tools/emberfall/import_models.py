"""Import the artist's final Emberfall model without changing its source or regenerating it."""
import base64
import copy
import io
import json
import runpy
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / 'src/main/resources/assets/lyycore'
SOURCE = Path('F:/misc/BlockBench/jiejin_sword/source/Emberfall.bbmodel')
helpers = runpy.run_path(str(ROOT / 'tools/research-content/import_models.py'))
write = helpers['write']
source = json.loads(SOURCE.read_text(encoding='utf8'))
# The authored blade has convex n-gons; the existing runtime consumes triangles/quads.
for element in source['elements']:
    if element.get('type') != 'mesh': continue
    faces = {}
    for key, face in element['faces'].items():
        vertices = face['vertices']
        if len(vertices) <= 4: faces[key] = face; continue
        for i in range(1,len(vertices)-1):
            triangle = [vertices[0],vertices[i],vertices[i+1]]
            faces[f'{key}_{i}'] = dict(face,vertices=triangle,uv={v:face['uv'][v] for v in triangle})
    element['faces'] = faces
paths = []
for texture in source['textures']:
    name = texture['name']
    target = ASSETS / 'textures/item/emberfall' / name
    target.parent.mkdir(parents=True, exist_ok=True)
    data = base64.b64decode(texture['source'].split(',', 1)[1])
    if texture.get('render_mode') == 'emissive':
        # BB uses emissive alpha as glow strength, not surface transparency.
        # Runtime emissive faces are full-bright cutouts; preserve their visible RGB.
        with Image.open(io.BytesIO(data)) as image:
            image = image.convert('RGBA')
            image.putalpha(255)
            image.save(target)
    else:
        target.write_bytes(data)
    paths.append('lyycore:textures/item/emberfall/' + name)
helpers['bake_mesh'](source, 'emberfall', paths)
original = json.loads((ASSETS / 'geometry/emberfall.json').read_text(encoding='utf8'))
original['additive_textures'] = [i for i,t in enumerate(source['textures']) if t.get('render_mode') == 'additive']
original['emissive_textures'] = [i for i,t in enumerate(source['textures']) if t.get('render_mode') == 'emissive']

def subtree(names, filename, remove_positions=(), remove_animations=()):
    mesh = copy.deepcopy(original)
    keep = set()
    for i,b in enumerate(mesh['bones']):
        if b['name'] in names or b['parent'] in keep: keep.add(i)
    mapping = {old:new for new,old in enumerate(sorted(keep))}
    mesh['bones'] = [b for i,b in enumerate(mesh['bones']) if i in keep]
    for b in mesh['bones']: b['parent'] = mapping.get(b['parent'], -1)
    for name, clip in mesh['animations'].items():
        clip['tracks'] = {str(mapping[int(i)]): t for i,t in clip['tracks'].items() if int(i) in keep}
        for i,track in clip['tracks'].items():
            bone = mesh['bones'][int(i)]['name']
            if bone in remove_positions: track.pop('position',None)
    for name in remove_animations: mesh['animations'].pop(name,None)
    write(ASSETS / f'geometry/{filename}.json',mesh)
    return mesh

previews = ('throw_cycle_preview','block_preview')
subtree({'handheld_attack_basis'},'emberfall',remove_animations=previews)
subtree({'single_throw_path'},'emberfall_thrown',remove_positions={'single_throw_path'},remove_animations=previews)
subtree({'vfx_slash_projectile'},'emberfall_slash',remove_positions={'vfx_slash_projectile'})
subtree({'vfx_impact_fissure','vfx_shock_ring','vfx_embers'},'emberfall_ring',
        remove_positions={'vfx_impact_fissure','vfx_shock_ring','vfx_embers'})

# Match Blockbench Display's idle pose and origin; normalizing the bounding box
# would invalidate the artist's GUI/fixed display adjustments.
mesh = json.loads((ASSETS / 'geometry/emberfall.json').read_text(encoding='utf8'))
bones = mesh['bones']
idle = mesh['animations']['idle']['tracks']

def idle_value(index, channel, default):
    keys = idle.get(str(index), {}).get(channel)
    return keys[0][1:4] if keys else [default]*3

def visible(index):
    while index >= 0:
        if any(abs(v) < 1e-6 for v in idle_value(index, 'scale', 1)): return False
        index = bones[index]['parent']
    return True
def point_world(point,index):
    while index >= 0:
        b = bones[index]
        point = [v*s for v,s in zip(point,idle_value(index,'scale',1))]
        point = helpers['rotate'](point,[r+a for r,a in zip(b['rotation'],idle_value(index,'rotation',0))])
        parent = bones[b['parent']]['pivot'] if b['parent']>=0 else [0,0,0]
        point = [v+(p-q+a)/16 for v,p,q,a in zip(point,b['pivot'],parent,idle_value(index,'position',0))]
        index=b['parent']
    # ItemRenderer subtracts (.5,.5,.5); the free-model center is (0,8,0).
    return [point[0]+.5, point[1], point[2]+.5]
faces=[{'texture':str(face['texture']),'emissive':face['texture'] in original['emissive_textures'],
        'vertices':[point_world(v[:3],i)+v[3:5] for v in face['vertices']]}
       for i,b in enumerate(bones) if visible(i) for face in b['faces']]
display=copy.deepcopy(source['display']);display.setdefault('ground',{'scale':[.5]*3})
textures={str(i):p.replace('textures/','').removesuffix('.png') for i,p in enumerate(paths)};textures['particle']=textures['0']
write(ASSETS/'models/item/emberfall.json',{'loader':'lyycore:machine_mesh','parent':'minecraft:block/block','render_type':'minecraft:cutout','ambientocclusion':False,'textures':textures,'faces':faces,'display':display})
