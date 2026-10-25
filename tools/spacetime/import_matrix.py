"""Bake the three supplied matrix stages; never modify the source Blockbench files.

Usage: python tools/spacetime/import_matrix.py [spacetime_matrix asset directory]
The shared foundation is chunk geometry; only the two authored idle rings animate.
The client draws the star separately, while inventory models retain its authored mesh.
"""
import copy
import json
import runpy
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/lyycore"
SOURCE = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("F:/misc/BlockBench/spacetime_matrix")
helpers = runpy.run_path(str(ROOT / "tools/research-content/import_models.py"))
write, rotate, bake = (helpers[k] for k in ("write", "rotate", "bake_mesh"))
NAME = "spacetime_matrix"


def clip_polygon(vertices, axis, boundary, sign):
    """Clip position and UV together, using the energy-machine importer's cell convention."""
    result = []
    for a, b in zip(vertices, vertices[1:] + vertices[:1]):
        da, db = sign * (a[axis] - boundary), sign * (b[axis] - boundary)
        if da >= -1e-8:
            result.append(a)
        if (da > 1e-8 and db < -1e-8) or (da < -1e-8 and db > 1e-8):
            result.append([x + (y - x) * da / (da - db) for x, y in zip(a, b)])
    return result


def face_json(vertices, texture):
    return {"texture": str(texture), "emissive": texture in (1, 2),
            "vertices": [[round(v, 6) for v in point[:5]] for point in vertices]}


def import_stage(stage):
    name = f"{NAME}_tier_{stage}"
    source = json.loads((SOURCE / "source" / f"{name}.bbmodel").read_text(encoding="utf-8"))
    paths, texture_map = [], {}
    for index, texture in enumerate(source["textures"]):
        filename = f"{Path(texture['name']).stem}_game.png"
        target = ASSETS / "textures/block" / NAME / filename
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(SOURCE / "textures" / filename, target)
        paths.append(f"lyycore:textures/block/{NAME}/{filename}")
        texture_map[str(index)] = f"lyycore:block/{NAME}/{Path(filename).stem}"
    texture_map["particle"] = texture_map["0"]
    bake(source, name, paths)
    mesh = json.loads((ASSETS / "geometry" / f"{name}.json").read_text(encoding="utf-8"))
    bones, tracks = mesh["bones"], mesh["animations"]["idle"]["tracks"]
    moving = set()
    for index, bone in enumerate(bones):
        if tracks.get(str(index)) or bone["parent"] in moving:
            moving.add(index)

    def world(vertex, index):
        point = vertex[:3]
        while index >= 0:
            bone = bones[index]
            channels = tracks.get(str(index), {})
            value = lambda channel, default: channels.get(channel, [[0, *default]])[0][1:4]
            point = [v * s for v, s in zip(point, value("scale", [1, 1, 1]))]
            point = rotate(point, [r + a for r, a in zip(bone["rotation"], value("rotation", [0, 0, 0]))])
            parent = bones[bone["parent"]]["pivot"] if bone["parent"] >= 0 else [0, 0, 0]
            point = [v + (p - q + d) / 16 for v, p, q, d in
                     zip(point, bone["pivot"], parent, value("position", [0, 0, 0]))]
            index = bone["parent"]
        return [point[0] + .5, point[1], point[2] + .5]

    common = {"loader": "lyycore:machine_mesh", "ambientocclusion": False,
              "textures": texture_map, "render_type": "minecraft:cutout"}
    parts, item = [[] for _ in range(9)], []
    for index, bone in enumerate(bones):
        for face in bone["faces"]:
            points = [world(v, index) + v[3:5] for v in face["vertices"]]
            # Display transforms are authored for the original full-size model.
            item.append(face_json(points, face["texture"]))
            if index in moving:
                continue
            for part in range(9):
                origin = [part % 3 - 1, 0, part // 3 - 1]
                if any(min(p[a] for p in points) >= origin[a] + 1 - 1e-8 and origin[a] < 1 for a in (0, 2)):
                    continue
                polygon = points
                for axis in range(3):
                    polygon = clip_polygon(polygon, axis, origin[axis], 1)
                    polygon = clip_polygon(polygon, axis, origin[axis] + 1, -1)
                for k in range(1, len(polygon) - 1):
                    tri = [polygon[0], polygon[k], polygon[k + 1]]
                    ab = [tri[1][a] - tri[0][a] for a in range(3)]
                    ac = [tri[2][a] - tri[0][a] for a in range(3)]
                    if sum((ab[(a+1)%3]*ac[(a+2)%3] - ab[(a+2)%3]*ac[(a+1)%3])**2 for a in range(3)) < 1e-16:
                        continue
                    parts[part].append(face_json([[v[a] - origin[a] for a in range(3)] + v[3:] for v in tri], face["texture"]))
    if stage == 1:
        for part, faces in enumerate(parts):
            write(ASSETS / "models/block" / NAME / f"base_{part}.json", dict(common, faces=faces))
        write(ASSETS / "models/block" / f"{NAME}.json", {"textures": texture_map, "elements": []})
        write(ASSETS / "blockstates" / f"{NAME}.json", {"multipart": [
            {"when": {"part": str(part)}, "apply": {"model": f"lyycore:block/{NAME}/base_{part}" if part < 9 else f"lyycore:block/{NAME}"}}
            for part in range(100)]})
        # Compact foundation/pylon collision. Floating machinery is visual, not reserved cells.
        shapes = []
        for part in range(9):
            ox, oz = part % 3 - 1, part // 3 - 1
            boxes = [[max(0, -.5 - ox), 0, max(0, -.5 - oz),
                      min(1, 1.5 - ox), 1.9 / 16, min(1, 1.5 - oz)]]
            for x, z in ((-12.2, -12.2), (-12.2, 12.2), (12.2, -12.2), (12.2, 12.2)):
                low = [max(0, .5 + (x - 2.5) / 16 - ox), 1.9 / 16, max(0, .5 + (z - 2.5) / 16 - oz)]
                high = [min(1, .5 + (x + 2.5) / 16 - ox), 10.4 / 16, min(1, .5 + (z + 2.5) / 16 - oz)]
                if low[0] < high[0] and low[2] < high[2]:
                    boxes.append(low + high)
            shapes.append(boxes)
        write(ASSETS / "geometry" / f"{NAME}_shapes.json", shapes)
        write(ASSETS / "models/item" / f"{NAME}.json", dict(common, parent="minecraft:block/block",
              faces=item, gui_light="front", display=copy.deepcopy(source["display"])))
    for index, bone in enumerate(bones):
        if index not in moving or bone["name"] in ("stellar_core", "corona_orbit", "corona_cross"):
            bone["faces"] = []
    mesh["emissive_textures"] = [1, 2]
    write(ASSETS / "geometry" / f"{name}.json", mesh)
    return parts


if __name__ == "__main__":
    foundation = import_stage(1)
    for tier in (2, 3):
        assert import_stage(tier) == foundation, "Stage foundations must be identical"
    print("Matrix: 32-unit foundation across 3x3 partial cells, 3 idle ring meshes, static inventory star")
