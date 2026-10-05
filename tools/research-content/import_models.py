"""Import the supplied gate and both animated dragons. No runtime Blockbench parser required.

Usage: python tools/research-content/import_models.py [BlockBench asset directory]
The gate is split into cached block meshes. Dragon cuboids/triangles are baked
into bone-local vertices; only hierarchy and sampled animation tracks run in game.
"""
import copy
import itertools
import json
import math
import runpy
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/lyycore"
SOURCE = Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT.parent / "blockbench"
uv_helpers = runpy.run_path(str(ROOT / "tools/production-lab/generate_static_models.py"))
FACES, crop_uv = uv_helpers["FACES"], uv_helpers["crop_uv"]


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, separators=(",", ":")) + "\n", encoding="utf-8")


def load(name):
    return json.loads((SOURCE / name / "source" / f"{name}.bbmodel").read_text(encoding="utf-8"))


def textures(name, category):
    destination = ASSETS / "textures" / category / name
    destination.mkdir(parents=True, exist_ok=True)
    for path in (SOURCE / name / "textures").iterdir():
        if path.suffix in (".png", ".mcmeta"):
            shutil.copyfile(path, destination / path.name)


def gate():
    name = "advanced_imaginary_gate"
    source = load(name)
    textures(name, "block")
    names = [Path(t["name"]).stem for t in source["textures"]]
    texture_map = {str(i): f"lyycore:block/{name}/{n}" for i, n in enumerate(names)}
    texture_map["particle"] = f"lyycore:block/{name}/white"
    parts = {(x, y, layer): [] for x in range(5) for y in range(5) for layer in ("body", "portal")}
    item = []
    for element in source["elements"]:
        a, b = element["from"], element["to"]
        faces = {face: {"uv": [round(v * 16 / source["textures"][f["texture"]]["uv_width"], 6) for v in f["uv"]],
                        "texture": f'#{f["texture"]}', **({"rotation": f["rotation"]} if f.get("rotation") else {})}
                 for face, f in element["faces"].items() if f.get("texture") is not None}
        rotation = element.get("rotation", [0, 0, 0])
        piece = {"from": a, "to": b, "faces": faces}
        if any(rotation):
            axes = [i for i, angle in enumerate(rotation) if angle]
            assert len(axes) == 1
            piece["rotation"] = {"origin": element["origin"], "axis": "xyz"[axes[0]], "angle": rotation[axes[0]]}
        icon = copy.deepcopy(piece)
        shrink = lambda point: [round((v - center) / 5 + 8, 6) for v, center in zip(point, (40, 40, 8))]
        icon["from"], icon["to"] = shrink(a), shrink(b)
        if "rotation" in icon: icon["rotation"]["origin"] = shrink(icon["rotation"]["origin"])
        item.append(icon)
        layer = "portal" if any(names[int(f["texture"][1:])] in ("portal", "crystal", "emitter") for f in faces.values()) else "body"
        if any(rotation):
            x, y = [min(4, max(0, math.floor(v / 16))) for v in element["origin"][:2]]
            local = lambda p: [round(p[0] - x * 16, 6), round(p[1] - y * 16, 6), round(p[2], 6)]
            part = copy.deepcopy(piece)
            part["from"], part["to"], part["rotation"]["origin"] = local(a), local(b), local(element["origin"])
            assert all(-16 <= v <= 32 for point in (part["from"], part["to"]) for v in point)
            parts[x, y, layer].append(part)
            continue
        for x, y in itertools.product(range(max(0, math.floor(a[0] / 16)), min(5, math.ceil(b[0] / 16))),
                                       range(max(0, math.floor(a[1] / 16)), min(5, math.ceil(b[1] / 16)))):
            low = [max(a[0], x * 16), max(a[1], y * 16), a[2]]
            high = [min(b[0], (x + 1) * 16), min(b[1], (y + 1) * 16), b[2]]
            cropped = {}
            for face, uv in faces.items():
                axis, sign, *uv_axes = FACES[face]
                edge, original = (low, a) if sign < 0 else (high, b)
                if abs(edge[axis] - original[axis]) > 1e-6: continue
                assert not uv.get("rotation")
                cropped[face] = dict(uv, uv=crop_uv(uv["uv"], a, b, low, high, uv_axes))
            if cropped:
                local = lambda p: [round(p[0] - x * 16, 6), round(p[1] - y * 16, 6), round(p[2], 6)]
                parts[x, y, layer].append({"from": local(low), "to": local(high), "faces": cropped})
    multipart = []
    for (x, y, layer), elements in parts.items():
        if not elements: continue
        model = f"block/{name}/{layer}_{x}_{y}"
        write(ASSETS / "models" / f"{model}.json", {"ambientocclusion": False,
              "render_type": "minecraft:translucent" if layer == "portal" else "minecraft:cutout",
              "textures": texture_map, "elements": elements})
        for facing, angle in [("north", 0), ("east", 90), ("south", 180), ("west", 270)]:
            multipart.append({"when": {"facing": facing, "column": str(x), "row": str(y)}, "apply": {"model": f"lyycore:{model}", "y": angle}})
    write(ASSETS / "blockstates" / f"{name}.json", {"multipart": multipart})
    write(ASSETS / "models/item" / f"{name}.json", {"parent": "minecraft:block/block", "ambientocclusion": False,
          "render_type": "minecraft:translucent", "textures": texture_map, "elements": item,
          "display": {"gui": {"rotation": [20, 25, 0], "scale": [0.85] * 3},
                      "ground": {"scale": [0.4] * 3, "translation": [0, 3, 0]},
                      "thirdperson_righthand": {"rotation": [75, 45, 0], "scale": [0.4] * 3},
                      "firstperson_righthand": {"rotation": [0, 45, 0], "scale": [0.65] * 3}}})
    print(f"Gate: {sum(bool(p) for p in parts.values())} cached models, {sum(map(len, parts.values()))} elements")


def rotate(point, rotation):
    x, y, z = point
    for axis, angle in enumerate(rotation):
        s, c = math.sin(math.radians(angle)), math.cos(math.radians(angle))
        if axis == 0: y, z = c * y - s * z, s * y + c * z
        elif axis == 1: x, z = c * x + s * z, -s * x + c * z
        else: x, y = c * x - s * y, s * x + c * y
    return [x, y, z]


def dragon(name):
    source = load(name)
    textures(name, "entity")
    names = [Path(t["name"]).stem for t in source["textures"]]
    bake_mesh(source, name, [f"lyycore:textures/entity/{name}/{n}.png" for n in names])


def bake_mesh(source, name, texture_paths, strip_motion=()):
    """Bake Blockbench geometry once; the client only samples bone transforms."""
    groups = {g["uuid"]: g for g in source["groups"]}
    elements = {e["uuid"]: e for e in source["elements"]}
    bones, indexes = [], {}

    def add_face(bone, element, points, uvs, texture):
        origin, rotation = element.get("origin", [0, 0, 0]), element.get("rotation", [0, 0, 0])
        vertices = []
        for point in points:
            turned = rotate([v - o for v, o in zip(point, origin)], rotation)
            vertices.append([(v + o - p) / 16 for v, o, p in zip(turned, origin, bone["pivot"])])
        a, b, c = vertices[:3]
        ab, ac = [y-x for x,y in zip(a,b)], [y-x for x,y in zip(a,c)]
        normal = [ab[1]*ac[2]-ab[2]*ac[1], ab[2]*ac[0]-ab[0]*ac[2], ab[0]*ac[1]-ab[1]*ac[0]]
        length = math.sqrt(sum(v*v for v in normal))
        if length < 1e-9: return
        normal = [v/length for v in normal]
        atlas = source["textures"][texture]
        size = [atlas.get("uv_width", 16), atlas.get("uv_height", 16)]
        rows = [[round(v, 6) for v in point + [u/s for u,s in zip(uv, size)] + normal] for point,uv in zip(vertices,uvs)]
        if len(rows) == 3: rows.append(rows[-1])
        assert len(rows) == 4
        bone["faces"].append({"texture": texture, "vertices": rows})

    def add_element(bone, element):
        if element.get("export", True) is False: return
        if element.get("type", "cube") not in ("cube", "mesh"): return
        if element.get("type") == "mesh":
            for face in element["faces"].values():
                if face.get("texture") is not None:
                    add_face(bone, element, [element["vertices"][v] for v in face["vertices"]], [face["uv"][v] for v in face["vertices"]], face["texture"])
            return
        x0,y0,z0 = element["from"]; x1,y1,z1 = element["to"]
        corners = {
            "north": [[x1,y0,z0],[x0,y0,z0],[x0,y1,z0],[x1,y1,z0]],
            "south": [[x0,y0,z1],[x1,y0,z1],[x1,y1,z1],[x0,y1,z1]],
            "west": [[x0,y0,z0],[x0,y0,z1],[x0,y1,z1],[x0,y1,z0]],
            "east": [[x1,y0,z1],[x1,y0,z0],[x1,y1,z0],[x1,y1,z1]],
            "up": [[x0,y1,z1],[x1,y1,z1],[x1,y1,z0],[x0,y1,z0]],
            "down": [[x0,y0,z0],[x1,y0,z0],[x1,y0,z1],[x0,y0,z1]]}
        for face, uv in element["faces"].items():
            if uv.get("texture") is None: continue
            u0,v0,u1,v1 = uv["uv"]
            uvs = [[u0,v1],[u1,v1],[u1,v0],[u0,v0]]
            shift = int(uv.get("rotation", 0) / 90)
            uvs = uvs[shift:] + uvs[:shift]
            add_face(bone, element, corners[face], uvs, uv["texture"])

    def visit(node, parent=-1):
        group = groups[node["uuid"]]
        index = len(bones); indexes[node["uuid"]] = index
        bone = {"name": group["name"], "parent": parent, "pivot": group["origin"], "rotation": group.get("rotation", [0,0,0]), "faces": []}
        bones.append(bone)
        for child in node["children"]:
            if isinstance(child, str): add_element(bone, elements[child])
            else: visit(child, index)
    for node in source["outliner"]: visit(node)
    animations = {}
    for animation in source["animations"]:
        kind = animation["name"].rsplit(".", 1)[-1]
        tracks = {}
        for uuid, animator in animation["animators"].items():
            if uuid not in indexes: continue
            channels = {}
            for key in animator.get("keyframes", []):
                if key["channel"] not in ("position", "rotation", "scale"): continue
                assert key.get("interpolation", "linear") in ("linear", "step", "catmullrom")
                value = [float(key["data_points"][0][axis]) for axis in "xyz"]
                channels.setdefault(key["channel"], []).append([key["time"], *value, *({"step": [1], "catmullrom": [2]}.get(key.get("interpolation"), []))])
            for values in channels.values(): values.sort(key=lambda v:v[0])
            if (bones[indexes[uuid]]["name"], kind) in strip_motion:
                channels.pop("position", None)
            if bones[indexes[uuid]]["name"] == "root" and kind in ("fly", "glide") and "position" in channels:
                # The source's hover altitude is a preview offset, not world movement.
                baseline = channels["position"][0][2]
                for key in channels["position"]: key[2] -= baseline
            tracks[str(indexes[uuid])] = channels
        animations[kind] = {"length": animation["length"], "loop": animation.get("loop") == "loop", "tracks": tracks}
    write(ASSETS / "geometry" / f"{name}.json", {"textures": texture_paths, "bones": bones, "animations": animations})
    print(f"{name}: {len(bones)} bones, {sum(len(b['faces']) for b in bones)} faces, {len(animations)} clips")


if __name__ == "__main__":
    gate()
    dragon("ender_dragon_hatchling")
    dragon("ender_dragon_juvenile")
