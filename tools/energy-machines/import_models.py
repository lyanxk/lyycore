"""Import supplied machines: cached static quads, animated bones, item previews and collision.

Run with Python 3 + Pillow. Original Blockbench files remain outside the project.
Emissive texture alpha is material strength in these sources, not surface transparency.
"""
import itertools
import copy
import json
import math
import runpy
import shutil
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/lyycore"
SOURCE = ROOT.parent / "blockbench"
helpers = runpy.run_path(str(ROOT / "tools/research-content/import_models.py"))
write, rotate, bake = (helpers[k] for k in ("write", "rotate", "bake_mesh"))


def clip_polygon(vertices, axis, boundary, sign):
    result = []
    for a, b in zip(vertices, vertices[1:] + vertices[:1]):
        da, db = sign * (a[axis] - boundary), sign * (b[axis] - boundary)
        if da >= -1e-8: result.append(a)
        if (da > 1e-8 and db < -1e-8) or (da < -1e-8 and db > 1e-8):
            result.append([x + (y - x) * da / (da - db) for x, y in zip(a, b)])
    return result


def import_machine(folder, name, height, source_name=None, oriented=False, width=3, source_offset=(0, 0, 0), gui_scale=0.9,
                   emissive_overlay=None, top_overhang=0, gui_rotation=(20, 30, 0)):
    source = json.loads((SOURCE / folder / "source" / f"{source_name or folder}.bbmodel").read_text(encoding="utf-8"))
    source.setdefault("animations", [])
    paths, texture_map, emissive = [], {}, set()
    for i, texture in enumerate(source["textures"]):
        filename = texture["name"]
        target = ASSETS / "textures/block" / name / filename
        target.parent.mkdir(parents=True, exist_ok=True)
        image = Image.open(SOURCE / folder / "textures" / filename).convert("RGBA")
        if texture.get("render_mode") == "emissive":
            emissive.add(i)
            image.putalpha(255)
        image.save(target)
        paths.append(f"lyycore:textures/block/{name}/{filename}")
        texture_map[str(i)] = f"lyycore:block/{name}/{Path(filename).stem}"
    texture_map["particle"] = texture_map["0"]
    bake(source, name, paths)
    mesh = json.loads((ASSETS / "geometry" / f"{name}.json").read_text(encoding="utf-8"))
    if emissive_overlay:
        assert len(paths) == 1, "An emissive overlay must match the model's single atlas"
        target = ASSETS / "textures/block" / name / emissive_overlay
        shutil.copyfile(SOURCE / folder / "textures" / emissive_overlay, target)
        paths.append(f"lyycore:textures/block/{name}/{emissive_overlay}")
        texture_map["1"] = f"lyycore:block/{name}/{Path(emissive_overlay).stem}"
        mesh["textures"] = paths
        emissive.add(1)
        for bone in mesh["bones"]:
            overlay = copy.deepcopy(bone["faces"])
            for face in overlay: face["texture"] = 1
            bone["faces"].extend(overlay)
    bones, clip = mesh["bones"], next(iter(mesh["animations"].values()), {"tracks": {}})
    moving = set()
    for i, bone in enumerate(bones):
        # A bone can be stationary in the initial pose and move in another clip.
        tracks = [animation["tracks"].get(str(i), {}) for animation in mesh["animations"].values()]
        if bone["parent"] in moving or any(tracks):
            moving.add(i)

    def world(vertex, index, normal=False):
        p = vertex[:3]
        while index >= 0:
            bone = bones[index]
            channels = clip["tracks"].get(str(index), {})
            value = lambda channel, default: channels.get(channel, [[0, *default]])[0][1:]
            p = [v * s for v, s in zip(p, value("scale", [1, 1, 1]))]
            p = rotate(p, [a+b for a, b in zip(bone["rotation"], value("rotation", [0, 0, 0]))])
            if not normal:
                parent = bones[bone["parent"]]["pivot"] if bone["parent"] >= 0 else [0, 0, 0]
                p = [v+(pivot-origin+offset)/16 for v,pivot,origin,offset in zip(p,bone["pivot"],parent,value("position",[0,0,0]))]
            index = bone["parent"]
        return p if normal else [p[0]+0.5-source_offset[0]/16, p[1]-source_offset[1]/16, p[2]+0.5-source_offset[2]/16]

    cell_count = height * width * width
    def cell_origin(part):
        return [part % width-width//2, part//(width*width), part//width%width-width//2]
    def cell_high(origin):
        return [origin[0] + 1, origin[1] + 1 + (top_overhang if origin[1] == height - 1 else 0), origin[2] + 1]
    parts = {(part, layer): [] for part in range(cell_count) for layer in ("body", "glass")}
    item = []
    def face_json(vertices, texture):
        return {"texture": str(texture), "emissive": texture in emissive,
                "vertices": [[round(x, 6) for x in v[:5]] for v in vertices]}
    for index, bone in enumerate(bones):
        for face in bone["faces"]:
            vertices = [world(v, index) + v[3:5] for v in face["vertices"]]
            size = max(height, width)
            icon = [[(v[0]-0.5)/size+0.5, v[1]/size, (v[2]-0.5)/size+0.5, *v[3:]] for v in vertices]
            item.append(face_json(icon, face["texture"]))
            if index in moving: continue
            layer = "glass" if "glass" in paths[face["texture"]] else "body"
            for part in range(cell_count):
                origin = cell_origin(part)
                high = cell_high(origin)
                # Assign a surface on an exact cell boundary to only one of its neighbors.
                limits = [width//2+1, height+top_overhang, width//2+1]
                if any(max(v[a] for v in vertices) < origin[a]-1e-8
                       or min(v[a] for v in vertices) > high[a]+1e-8
                       or (min(v[a] for v in vertices) >= high[a]-1e-8 and high[a] < limits[a]) for a in range(3)): continue
                polygon = vertices
                for axis in range(3):
                    polygon = clip_polygon(polygon, axis, origin[axis], 1)
                    polygon = clip_polygon(polygon, axis, high[axis], -1)
                if len(polygon) < 3: continue
                for k in range(1, len(polygon)-1):
                    tri = [polygon[0], polygon[k], polygon[k+1]]
                    ab = [tri[1][a]-tri[0][a] for a in range(3)]
                    ac = [tri[2][a]-tri[0][a] for a in range(3)]
                    if sum((ab[(a+1)%3]*ac[(a+2)%3]-ab[(a+2)%3]*ac[(a+1)%3])**2 for a in range(3)) < 1e-16: continue
                    local = [[v[a]-origin[a] for a in range(3)]+v[3:] for v in tri]
                    parts[part, layer].append(face_json(local, face["texture"]))
    common = {"loader": "lyycore:machine_mesh", "ambientocclusion": False, "textures": texture_map}
    multipart = []
    for (part, layer), faces in parts.items():
        if not faces: continue
        model = f"block/{name}/{layer}_{part}"
        write(ASSETS / "models" / f"{model}.json", dict(common, faces=faces, render_type="minecraft:translucent" if layer == "glass" else "minecraft:cutout"))
        multipart.append({"when": {"part": str(part)}, "apply": {"model": f"lyycore:{model}"}})
    # A model is also needed for empty reserved cells and command-created out-of-range parts.
    write(ASSETS / "models/block" / f"{name}.json", {"textures": texture_map, "elements": []})
    used = {part for (part, layer), faces in parts.items() if faces}
    for part in range(cell_count if oriented else 100):
        if part not in used:
            multipart.append({"when": {"part": str(part)}, "apply": {"model": f"lyycore:block/{name}"}})
    if oriented:
        multipart = [{"when": dict(entry["when"], facing=facing),
                      "apply": dict(entry["apply"], y=angle)}
                     for entry in multipart for facing, angle in (("north", 0), ("east", 90), ("south", 180), ("west", 270))]
    write(ASSETS / "blockstates" / f"{name}.json", {"multipart": multipart})
    write(ASSETS / "models/item" / f"{name}.json", dict(common, parent="minecraft:block/block", faces=item,
          render_type="minecraft:translucent", display={"gui": {"rotation": list(gui_rotation), "scale": [gui_scale]*3},
          "ground": {"scale": [0.4]*3, "translation": [0, 3, 0]},
          "firstperson_righthand": {"rotation": [0, 45, 0], "scale": [0.65]*3}}))
    # Keep the hierarchy/tracks, but render only moving surfaces each frame.
    for index, bone in enumerate(bones):
        if index not in moving: bone["faces"] = []
    mesh["emissive_textures"] = sorted(emissive)
    write(ASSETS / "geometry" / f"{name}.json", mesh)

    element_bones = {}
    def walk(node, index_counter):
        index = index_counter[0]; index_counter[0] += 1
        for child in node["children"]:
            if isinstance(child, str): element_bones[child] = index
            else: walk(child, index_counter)
    counter = [0]
    for node in source["outliner"]: walk(node, counter)
    boxes = [[] for _ in range(cell_count)]
    for element in source["elements"]:
        if not element.get("export", True): continue
        if element.get("type", "cube") == "cube":
            points = itertools.product(*zip(element["from"], element["to"]))
        elif element.get("type") == "mesh":
            points = [[v+o for v,o in zip(point,element.get("origin",[0,0,0]))]
                      for point in element["vertices"].values()]
        else: continue
        index = element_bones[element["uuid"]]
        origin = element.get("origin", [0,0,0])
        points = [world([(v+o-p)/16 for v,o,p in zip(rotate([v-o for v,o in zip(point,origin)],element.get("rotation",[0,0,0])),origin,bones[index]["pivot"])],index) for point in points]
        low, high = [min(p[a] for p in points) for a in range(3)], [max(p[a] for p in points) for a in range(3)]
        if any(b-a < 0.001 for a,b in zip(low,high)): continue
        # Quantized boxes keep collision shapes compact, even on highly detailed models.
        low = [math.floor(v*16+1e-6)/16 for v in low]; high = [math.ceil(v*16-1e-6)/16 for v in high]
        for part in range(cell_count):
            origin = cell_origin(part)
            a, b = [max(0,v-o) for v,o in zip(low,origin)], [min(limit,v)-o for v,o,limit in zip(high,origin,cell_high(origin))]
            if all(x<y for x,y in zip(a,b)): boxes[part].append(a+b)
    write(ASSETS / "geometry" / f"{name}_shapes.json", boxes)
    print(f"{name}: {len(item)} faces; {sum(len(b['faces']) for b in bones)} animated; {height} blocks high")


if __name__ == "__main__":
    import_machine("imaginary_condensation_beacon", "imaginary_condensing_beacon", 4, gui_scale=0.7, gui_rotation=(20, 150, 0))
    import_machine("spatial_transmission_tower", "spatial_transmission_tower", 8)
