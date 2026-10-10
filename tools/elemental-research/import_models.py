"""Import the artist's final experiment table and hail models; never rebuild artist sources."""
import copy
import itertools
import json
import math
import runpy
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/lyycore"
SOURCE = Path("F:/misc/BlockBench")
helpers = runpy.run_path(str(ROOT / "tools/research-content/import_models.py"))
bake, rotate, write = (helpers[k] for k in ("bake_mesh", "rotate", "write"))


def import_mesh(folder, source_name, name, category):
    source = json.loads((SOURCE / folder / "source" / f"{source_name}.bbmodel").read_text(encoding="utf-8"))
    # Large flowers share embedded texture names with the small flower, but use their own expanded atlas.
    paths = []
    for suffix in ("", "_emissive"):
        filename = source_name + suffix + ".png"
        target = ASSETS / "textures" / category / filename
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(SOURCE / folder / "textures" / filename, target)
        paths.append(f"lyycore:textures/{category}/{filename}")
    assert len(source["textures"]) == 1
    bake(source, name, paths[:1])
    mesh = json.loads((ASSETS / "geometry" / f"{name}.json").read_text())
    mesh["textures"] = paths
    mesh["emissive_textures"] = [1]
    for bone in mesh["bones"]:
        extra = copy.deepcopy(bone["faces"])
        for face in extra:
            face["texture"] = 1
        bone["faces"].extend(extra)
    return source, mesh


def table():
    name = "experiment_table"
    source, mesh = import_mesh("arcane_experiment_table", "arcane_experiment_table", name, "block/experiment_table")
    bones = mesh["bones"]
    clip = mesh["animations"]["idle"]
    moving = set()
    for index, bone in enumerate(bones):
        tracks = clip["tracks"].get(str(index), {})
        if bone["parent"] in moving or any(any(k[1:4] != keys[0][1:4] for k in keys[1:]) for keys in tracks.values()):
            moving.add(index)

    def world(point, index):
        while index >= 0:
            bone = bones[index]
            channels = clip["tracks"].get(str(index), {})
            value = lambda channel, default: channels.get(channel, [[0, *default]])[0][1:4]
            point = [v * s for v, s in zip(point, value("scale", [1, 1, 1]))]
            point = rotate(point, [a + b for a, b in zip(bone["rotation"], value("rotation", [0, 0, 0]))])
            parent = bones[bone["parent"]]["pivot"] if bone["parent"] >= 0 else [0, 0, 0]
            point = [v + (p - o + t) / 16 for v, p, o, t in zip(point, bone["pivot"], parent, value("position", [0, 0, 0]))]
            index = bone["parent"]
        return [point[0] + .5, point[1], point[2] + .5]

    textures = {str(i): path.replace("textures/", "").removesuffix(".png") for i, path in enumerate(mesh["textures"])}
    textures["particle"] = textures["0"]
    common = {"loader": "lyycore:machine_mesh", "ambientocclusion": False, "render_type": "minecraft:cutout", "textures": textures}
    static, item = [], []
    for index, bone in enumerate(bones):
        for face in bone["faces"]:
            converted = {"texture": str(face["texture"]), "emissive": face["texture"] == 1,
                         "vertices": [[round(v, 6) for v in world(vertex[:3], index) + vertex[3:5]] for vertex in face["vertices"]]}
            item.append(converted)
            if index not in moving:
                static.append(converted)
    write(ASSETS / f"models/block/{name}.json", dict(common, faces=static))
    write(ASSETS / f"models/item/{name}.json", dict(common, faces=item, parent="minecraft:block/block"))
    write(ASSETS / f"blockstates/{name}.json", {"variants": {f"facing={f}": {"model": f"lyycore:block/{name}", "y": a}
          for f, a in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})

    # Quantize actual cube bounds to one model pixel and cache all four rotated shapes.
    element_bones = {}
    counter = itertools.count()
    def visit(node):
        index = next(counter)
        for child in node["children"]:
            if isinstance(child, str): element_bones[child] = index
            else: visit(child)
    for node in source["outliner"]: visit(node)
    boxes = [[] for _ in range(4)]
    for element in source["elements"]:
        index = element_bones[element["uuid"]]
        if index in moving or not element.get("export", True): continue
        origin = element.get("origin", [0, 0, 0])
        points = []
        for point in itertools.product(*zip(element["from"], element["to"])):
            turned = rotate([v - o for v, o in zip(point, origin)], element.get("rotation", [0, 0, 0]))
            points.append(world([(v + o - p) / 16 for v, o, p in zip(turned, origin, bones[index]["pivot"])], index))
        if any(max(p[a] for p in points) - min(p[a] for p in points) < .002 for a in range(3)): continue
        for facing in range(4):
            turned = [rotate([p[0] - .5, p[1], p[2] - .5], [0, -90 * facing, 0]) for p in points]
            low = [math.floor((min(p[a] for p in turned) + (.5 if a != 1 else 0)) * 16 + 1e-5) / 16 for a in range(3)]
            high = [math.ceil((max(p[a] for p in turned) + (.5 if a != 1 else 0)) * 16 - 1e-5) / 16 for a in range(3)]
            boxes[facing].append(low + high)
    write(ASSETS / f"geometry/{name}_shapes.json", boxes)
    for index, bone in enumerate(bones):
        if index not in moving: bone["faces"] = []
    write(ASSETS / f"geometry/{name}.json", mesh)
    print(f"{name}: {len(static)} cached faces, {sum(len(b['faces']) for b in bones)} animated faces")


def hail():
    for name in ("hail_flower", "hail_flower_large", "hail_projectile"):
        _, mesh = import_mesh("hail", name, name, "entity/hail")
        write(ASSETS / f"geometry/{name}.json", mesh)
    write(ASSETS / "models/item/hail.json", {"parent": "minecraft:block/block",
        "textures": {"particle": "lyycore:entity/hail/hail_flower"}, "elements": [],
        "display": {"gui": {"rotation": [35, 25, 0], "scale": [.55] * 3},
                    "ground": {"scale": [.35] * 3, "translation": [0, 3, 0]},
                    "fixed": {"rotation": [90, 0, 0], "scale": [.5] * 3},
                    "thirdperson_righthand": {"rotation": [0, 0, 0], "scale": [.45] * 3},
                    "firstperson_righthand": {"rotation": [10, 0, 0], "scale": [.5] * 3}}})


if __name__ == "__main__":
    table()
    hail()
