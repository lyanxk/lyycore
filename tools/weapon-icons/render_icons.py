"""Run inside Blender. Render the existing baked GUI geometry, without remodeling it."""
import bpy
import json
import math
from pathlib import Path
from mathutils import Vector, Euler, Matrix

ROOT = Path('F:/CodeMisc/Java/lyycore')
ASSETS = ROOT / 'src/main/resources/assets/lyycore'
OUTPUT = Path('F:/misc/BlockBench/weapon_inventory_renders')
OUTPUT.mkdir(parents=True, exist_ok=True)

def render_icon(name):
    source = json.loads((ASSETS / f'models/item/{name}.json').read_text(encoding='utf8'))
    scene = bpy.data.scenes.new('Icon_' + name)
    scene.render.engine = 'BLENDER_EEVEE'
    scene.render.resolution_x = scene.render.resolution_y = 256
    scene.render.resolution_percentage = 100
    scene.render.film_transparent = True
    scene.render.image_settings.file_format = 'PNG'
    scene.render.image_settings.color_mode = 'RGBA'
    scene.view_settings.view_transform = 'Standard'
    scene.view_settings.look = 'None'
    scene.view_settings.exposure = 0
    scene.view_settings.gamma = 1
    world = bpy.data.worlds.new(name + '_studio')
    world.use_nodes = True
    world.node_tree.nodes['Background'].inputs['Color'].default_value = (.8, .87, 1, 1)
    world.node_tree.nodes['Background'].inputs['Strength'].default_value = .35
    scene.world = world

    display = source.get('display', {}).get('gui', {})
    # BB/ItemTransform uses XYZ quaternion composition, i.e. R_x R_y R_z.
    r = [math.radians(v) for v in display.get('rotation', [0,0,0])]
    transform = (Matrix.Rotation(r[0], 4, 'X') @ Matrix.Rotation(r[1], 4, 'Y') @ Matrix.Rotation(r[2], 4, 'Z')
                 @ Matrix.Diagonal((*display.get('scale', [1,1,1]), 1)))
    all_points = [transform @ Vector([v[i]-.5 for i in range(3)]) for f in source['faces'] for v in f['vertices']]
    low = Vector([min(p[i] for p in all_points) for i in range(3)])
    high = Vector([max(p[i] for p in all_points) for i in range(3)])
    center = (low + high) * .5
    fit = 1.82 / max(high.x-low.x, high.y-low.y)

    materials = {}
    for face in source['faces']:
        key = (face['texture'], face.get('emissive', False))
        if key in materials:
            continue
        path = source['textures'][key[0]].split(':',1)[1]
        image = bpy.data.images.load(str(ASSETS / ('textures/' + path + '.png')), check_existing=True)
        mat = bpy.data.materials.new(name + '_' + key[0] + ('_glow' if key[1] else ''))
        mat.use_nodes = True
        nodes = mat.node_tree.nodes
        bsdf = nodes.get('Principled BSDF')
        image_node = nodes.new('ShaderNodeTexImage')
        image_node.image = image
        image_node.interpolation = 'Closest'
        links = mat.node_tree.links
        links.new(image_node.outputs['Color'], bsdf.inputs['Base Color'])
        links.new(image_node.outputs['Alpha'], bsdf.inputs['Alpha'])
        links.new(image_node.outputs['Color'], bsdf.inputs['Emission Color'])
        bsdf.inputs['Emission Strength'].default_value = .8 if key[1] else .18
        bsdf.inputs['Roughness'].default_value = .5
        bsdf.inputs['Metallic'].default_value = .08
        materials[key] = mat

    verts, polygons, uvs, mat_indices = [], [], [], []
    material_keys = list(materials)
    for face in source['faces']:
        data = list(face['vertices'])
        if transform.determinant() < 0:
            data.reverse()
        indices = []
        for vertex in data:
            p = (transform @ Vector([vertex[i]-.5 for i in range(3)]) - center) * fit
            # Minecraft X/Y/Z -> Blender X/-Z/Y; camera looks along +Y.
            verts.append((p.x, -p.z, p.y))
            indices.append(len(verts)-1)
            uvs.append((vertex[3], 1-vertex[4]))
        polygons.append(indices)
        mat_indices.append(material_keys.index((face['texture'],face.get('emissive',False))))
    mesh = bpy.data.meshes.new(name + '_baked_gui')
    mesh.from_pydata(verts, [], polygons)
    mesh.update()
    obj = bpy.data.objects.new(name, mesh)
    scene.collection.objects.link(obj)
    for mat in materials.values():
        mesh.materials.append(mat)
    uv_layer = mesh.uv_layers.new(name='UVMap')
    for poly, mat_index in zip(mesh.polygons, mat_indices):
        poly.material_index = mat_index
        for index in poly.loop_indices:
            uv_layer.data[index].uv = uvs[mesh.loops[index].vertex_index]
    camera_data = bpy.data.cameras.new(name + '_camera')
    camera = bpy.data.objects.new(name + '_camera', camera_data)
    scene.collection.objects.link(camera)
    camera.location = (0,-6,0)
    camera.rotation_euler = (-camera.location).to_track_quat('-Z','Y').to_euler()
    camera_data.type = 'ORTHO'
    camera_data.ortho_scale = 2
    scene.camera = camera
    for label, pos, energy, size in [('key',(-3,-4,4),420,4),('fill',(4,-3,1),300,5),('rim',(0,2,3),350,3)]:
        data = bpy.data.lights.new(name+'_'+label, 'AREA')
        data.energy = energy * .45
        data.shape = 'DISK'
        data.size = size
        light = bpy.data.objects.new(data.name, data)
        scene.collection.objects.link(light)
        light.location = pos
        light.rotation_euler = (-light.location).to_track_quat('-Z','Y').to_euler()
    scene.render.filepath = str(OUTPUT / (name + '.png'))
    bpy.ops.render.render(write_still=True, scene=scene.name)
    return {'name':name,'scene':scene.name,'path':scene.render.filepath,'polygons':len(polygons)}

results = [render_icon(name) for name in ['emberfall','thunder_scythe']]
bpy.ops.wm.save_as_mainfile(filepath=str(OUTPUT/'weapon_icons.blend'))
result = {'icons':results,'blend':str(OUTPUT/'weapon_icons.blend')}
