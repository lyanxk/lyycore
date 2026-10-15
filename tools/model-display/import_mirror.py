"""Keep the Java/BB item intact, but clip world faces into their actual light-sampling cells."""
import json
import math
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / 'src/main/resources/assets/lyycore'
SOURCE = Path('F:/misc/BlockBench/oval_mirror/export/oval_mirror.json')
FACE_CORNERS = {
    'down': [(0,0,1),(0,0,0),(1,0,0),(1,0,1)],
    'up': [(0,1,0),(0,1,1),(1,1,1),(1,1,0)],
    'north': [(1,1,0),(1,0,0),(0,0,0),(0,1,0)],
    'south': [(0,1,1),(0,0,1),(1,0,1),(1,1,1)],
    'west': [(0,1,0),(0,0,0),(0,0,1),(0,1,1)],
    'east': [(1,1,1),(1,0,1),(1,0,0),(1,1,0)],
}


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, separators=(',', ':')) + '\n', encoding='utf-8')


def point(element, corner):
    v = [element['to' if hi else 'from'][axis] for axis, hi in enumerate(corner)]
    rot = element.get('rotation')
    if rot and rot['angle']:
        axis = 'xyz'.index(rot['axis'])
        origin = rot.get('origin', [8,8,8])
        angle = math.radians(rot['angle'])
        c, s = math.cos(angle), math.sin(angle)
        v = [a-b for a,b in zip(v, origin)]
        if rot.get('rescale', False):
            v = [n if i == axis else n/c for i,n in enumerate(v)]
        a, b = (axis+1)%3, (axis+2)%3
        v[a], v[b] = c*v[a]-s*v[b], s*v[a]+c*v[b]
        v = [a+b for a,b in zip(v, origin)]
    return [n/16 for n in v]


def faces(model):
    for element in model['elements']:
        for side, face in element['faces'].items():
            uv = face['uv']
            corners = [(uv[0],uv[1]),(uv[0],uv[3]),(uv[2],uv[3]),(uv[2],uv[1])]
            shift = face.get('rotation', 0)//90
            vertices = [point(element, c) + [n/16 for n in corners[(i+shift)%4]]
                        for i,c in enumerate(FACE_CORNERS[side])]
            yield {'texture': face['texture'].lstrip('#'), 'emissive': False, 'vertices': vertices}


def clip(vertices, boundary, sign):
    output = []
    for a,b in zip(vertices, vertices[1:]+vertices[:1]):
        da,db = sign*(a[1]-boundary),sign*(b[1]-boundary)
        if da >= -1e-9: output.append(a)
        if (da > 1e-9 and db < -1e-9) or (da < -1e-9 and db > 1e-9):
            t = da/(da-db)
            output.append([x+(y-x)*t for x,y in zip(a,b)])
    return output


def area(vertices):
    a = vertices[0]
    total = 0
    for b,c in zip(vertices[1:], vertices[2:]):
        u,v = [b[i]-a[i] for i in range(3)], [c[i]-a[i] for i in range(3)]
        cross = [u[1]*v[2]-u[2]*v[1],u[2]*v[0]-u[0]*v[2],u[0]*v[1]-u[1]*v[0]]
        total += math.sqrt(sum(n*n for n in cross))/2
    return total


def generate(model):
    source_faces = list(faces(model))
    result = [[],[]]
    for f in source_faces:
        parts = []
        for cell in range(2):
            # Faces exactly on the seam are emitted once.
            if cell == 0 and all(abs(v[1]-1)<1e-9 for v in f['vertices']): continue
            polygon = clip(clip(f['vertices'], cell, 1), cell+1, -1)
            if len(polygon)<3 or area(polygon)<1e-10: continue
            parts.append(polygon)
            polys = [polygon] if len(polygon)<=4 else [[polygon[0],polygon[i],polygon[i+1]] for i in range(1,len(polygon)-1)]
            for poly in polys:
                result[cell].append({**f, 'vertices': [[round(v[0],9),round(v[1]-cell,9),round(v[2],9),v[3],v[4]] for v in poly]})
        if not math.isclose(sum(area(p) for p in parts),area(f['vertices']),rel_tol=1e-6,abs_tol=1e-7):
            raise ValueError('Clipping lost part of a mirror surface')
    return result


def main():
    model = json.loads(SOURCE.read_text(encoding='utf-8-sig'))
    lower,upper = generate(model)
    for name,part in [('mirror',lower),('mirror_upper',upper)]:
        write(ASSETS / f'models/block/{name}.json', {
            'loader':'lyycore:machine_mesh', 'ambientocclusion':False,
            'render_type':'minecraft:cutout', 'textures':model['textures'], 'faces':part,
        })
    # An inventory icon still needs both halves and the user's original display.
    write(ASSETS/'models/item/mirror_full.json', model)
    write(ASSETS/'models/item/mirror.json', {'parent':'lyycore:item/mirror_full','gui_light':model.get('gui_light','front')})
    print(json.dumps({'lower_faces':len(lower),'upper_faces':len(upper),'surface_area_preserved':True,'item_unchanged':True}))


if __name__ == '__main__':
    main()
