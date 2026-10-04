"""Split the laboratory's exported quarter-scale meshes into cached block models.

Run from any directory after updating body.json or glass.json. Moving parts and
the item model keep their existing meshes. Cuboid cuts preserve UV coordinates
and never introduce faces inside the original solid.
"""

import copy
import itertools
import json
import math
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2] / "src/main/resources/assets/lyycore"
MODELS = ROOT / "models/block/production_lab"
EPSILON = 1e-7
# Surface normal axis/sign, then texture U and V axes/signs (vanilla face UV order).
FACES = {
    "down": (1, -1, 0, 1, 2, -1),
    "up": (1, 1, 0, 1, 2, 1),
    "north": (2, -1, 0, -1, 1, -1),
    "south": (2, 1, 0, 1, 1, -1),
    "west": (0, -1, 2, 1, 1, -1),
    "east": (0, 1, 2, -1, 1, -1),
}


def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + "\n", encoding="utf-8")


def full_size(point):
    return [round((value - 8) * 4 + (0 if axis == 1 else 8), 6)
            for axis, value in enumerate(point)]


def local(point, cell):
    return [round(value - coordinate * 16, 6) for value, coordinate in zip(point, cell)]


def part_index(cell):
    x, y, z = cell
    assert -2 <= x <= 2 and 0 <= y <= 2 and -1 <= z <= 1, cell
    return y * 15 + (z + 1) * 5 + x + 2


def crop_uv(uv, source_from, source_to, clipped_from, clipped_to, axes):
    result = [0.0] * 4
    for channel, (axis, sign) in enumerate(zip(axes[::2], axes[1::2])):
        length = source_to[axis] - source_from[axis]
        start = (clipped_from[axis] - source_from[axis]) / length
        end = (clipped_to[axis] - source_from[axis]) / length
        if sign < 0:
            start, end = 1 - end, 1 - start
        span = uv[channel + 2] - uv[channel]
        result[channel] = round(uv[channel] + span * start, 6)
        result[channel + 2] = round(uv[channel] + span * end, 6)
    return result


def split(element):
    start, end = full_size(element["from"]), full_size(element["to"])
    if element.get("rotation", {}).get("angle", 0):
        # The tilted control panel fits in one block model, including its small overhang.
        pivot = full_size(element["rotation"]["origin"])
        cell = tuple(math.floor(value / 16) for value in pivot)
        piece = copy.deepcopy(element)
        piece["from"], piece["to"] = local(start, cell), local(end, cell)
        piece["rotation"]["origin"] = local(pivot, cell)
        assert all(-16 <= value <= 32 for point in [piece["from"], piece["to"]] for value in point)
        yield cell, piece
        return

    cells = [range(math.floor(low / 16), math.ceil(high / 16)) for low, high in zip(start, end)]
    for cell in itertools.product(*cells):
        low = [max(value, coordinate * 16) for value, coordinate in zip(start, cell)]
        high = [min(value, (coordinate + 1) * 16) for value, coordinate in zip(end, cell)]
        if any(b - a <= EPSILON for a, b in zip(low, high)):
            continue
        faces = {}
        for name, face in element["faces"].items():
            axis, sign, *uv_axes = FACES[name]
            boundary, original = (low, start) if sign < 0 else (high, end)
            if abs(boundary[axis] - original[axis]) > EPSILON:
                continue
            assert face.get("rotation", 0) == 0, "Handle rotated UVs before changing the export."
            faces[name] = dict(face, uv=crop_uv(face["uv"], start, end, low, high, uv_axes))
        if faces:
            yield cell, {"from": local(low, cell), "to": local(high, cell), "faces": faces}


def generate():
    from clean_model_surfaces import clean

    multipart = []
    for name, render_type in [("body", "minecraft:cutout"), ("glass", "minecraft:translucent")]:
        source = json.loads((MODELS / f"{name}.json").read_text(encoding="utf-8"))
        # Resolve coplanar trim/structure overdraw before cutting into block cells.
        source["elements"], _, _ = clean(source["elements"])
        cells = [[] for _ in range(45)]
        for element in source["elements"]:
            for cell, piece in split(element):
                cells[part_index(cell)].append(piece)
        for part, elements in enumerate(cells):
            if not elements and name == "glass":
                continue
            model = f"static/{name}_{part}"
            write(MODELS / f"{model}.json", {
                "ambientocclusion": False,
                "render_type": render_type,
                "textures": source["textures"],
                "elements": elements,
            })
            for facing, rotation in [("south", 0), ("west", 90), ("north", 180), ("east", 270)]:
                apply = {"model": f"lyycore:block/production_lab/{model}"}
                if rotation:
                    apply["y"] = rotation
                multipart.append({"when": {"facing": facing, "part": str(part)}, "apply": apply})
        print(f"{name}: {len(source['elements'])} source cuboids -> {sum(map(len, cells))} block fragments")
    write(ROOT / "blockstates/production_lab.json", {"multipart": multipart})


if __name__ == "__main__":
    generate()
