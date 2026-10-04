"""Remove coplanar overdraw without moving geometry or stretching its UVs.

Later elements own shared surfaces (trim over structure). Only faces with the
same normal and transform are compared. Clean animated bones separately: a
surface covered in the rest pose may become visible during animation.
"""

import copy
import json
import runpy
import uuid
from pathlib import Path

helpers = runpy.run_path(str(Path(__file__).with_name("generate_static_models.py")))
FACES, crop_uv = helpers["FACES"], helpers["crop_uv"]
EPSILON = 1e-7


def transform(element):
    rotation = element.get("rotation", {})
    if isinstance(rotation, list):
        return (tuple(rotation), tuple(element.get("origin", [0, 0, 0]))) if any(rotation) else None
    return json.dumps(rotation, sort_keys=True) if rotation.get("angle", 0) else None


def visible(face):
    return face.get("texture") is not None


def rectangle(element, axes):
    return tuple(element[edge][axis] for edge in ("from", "to") for axis in axes)


def subtract(rect, cut):
    """Disjoint rectangles covering rect minus cut, including partial coverage."""
    x0, y0, x1, y1 = rect
    a, b, c, d = max(x0, cut[0]), max(y0, cut[1]), min(x1, cut[2]), min(y1, cut[3])
    if c - a <= EPSILON or d - b <= EPSILON:
        return [rect]
    return [r for r in [(x0, y0, a, y1), (c, y0, x1, y1),
                        (a, y0, c, b), (a, d, c, y1)]
            if r[2] - r[0] > EPSILON and r[3] - r[1] > EPSILON]


def clean(elements, partitions=None, blockbench=False):
    partitions = partitions or [None] * len(elements)
    replacements, result = {}, []
    stats = {"clipped_faces": 0, "removed_faces": 0, "removed_area": 0.0}
    for index, element in enumerate(elements):
        if element.get("export", True) is False:
            result.append(copy.deepcopy(element))
            continue
        original = copy.deepcopy(element)
        fragments = []
        for name, face in element["faces"].items():
            if not visible(face):
                continue
            axis, sign, *uv_axes = FACES[name]
            axes = [i for i in range(3) if i != axis]
            edge = "from" if sign < 0 else "to"
            rect = rectangle(element, axes)
            pieces = [rect]
            for other_index in range(index + 1, len(elements)):
                other = elements[other_index]
                if (partitions[index] != partitions[other_index]
                        or other.get("export", True) is False
                        or transform(element) != transform(other)
                        or not visible(other["faces"].get(name, {}))
                        or abs(element[edge][axis] - other[edge][axis]) > EPSILON):
                    continue
                pieces = [part for piece in pieces for part in subtract(piece, rectangle(other, axes))]
                if not pieces:
                    break
            if pieces == [rect]:
                continue
            assert face.get("rotation", 0) == 0, "Rotated UV clipping is not implemented."
            stats["clipped_faces" if pieces else "removed_faces"] += 1
            area = lambda r: (r[2] - r[0]) * (r[3] - r[1])
            stats["removed_area"] += area(rect) - sum(map(area, pieces))
            if blockbench:
                original["faces"][name]["texture"] = None
            else:
                del original["faces"][name]
            for piece_index, rect_piece in enumerate(pieces):
                fragment = copy.deepcopy(element)
                for k, a in enumerate(axes):
                    fragment["from"][a], fragment["to"][a] = rect_piece[k], rect_piece[k + 2]
                cropped = dict(face, uv=crop_uv(face["uv"], element["from"], element["to"],
                                              fragment["from"], fragment["to"], uv_axes))
                if blockbench:
                    for f in fragment["faces"].values():
                        f["texture"] = None
                    fragment["name"] = f"{element['name']} / {name} {piece_index + 1}"
                    fragment["uuid"] = str(uuid.uuid5(uuid.UUID(element["uuid"]), f"surface:{name}:{piece_index}"))
                    fragment["autouv"] = 0
                else:
                    fragment["faces"] = {}
                fragment["faces"][name] = cropped
                fragments.append(fragment)
        pieces = ([original] if any(visible(f) for f in original["faces"].values()) else []) + fragments
        result.extend(pieces)
        if blockbench:
            replacements[element["uuid"]] = [p["uuid"] for p in pieces]
    stats["removed_area"] = round(stats["removed_area"], 6)
    return result, replacements, stats


def clean_project(project):
    animated = {key for animation in project.get("animations", [])
                for key, animator in animation["animators"].items() if animator.get("keyframes")}
    partitions = {}

    def visit(nodes, partition=None):
        for node in nodes:
            if isinstance(node, str):
                partitions[node] = partition
            else:
                visit(node["children"], node["uuid"] if node["uuid"] in animated else partition)

    visit(project["outliner"])
    project["elements"], replacements, stats = clean(
        project["elements"], [partitions.get(e["uuid"]) for e in project["elements"]], True)

    def replace(nodes):
        result = []
        for node in nodes:
            if isinstance(node, str):
                result.extend(replacements.get(node, [node]))
            else:
                node["children"] = replace(node["children"])
                result.append(node)
        return result

    project["outliner"] = replace(project["outliner"])
    return stats


def clean_file(path):
    data = json.loads(path.read_text(encoding="utf-8"))
    if path.suffix == ".bbmodel":
        stats = clean_project(data)
    else:
        data["elements"], _, stats = clean(data["elements"])
    if stats["clipped_faces"] or stats["removed_faces"]:
        path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return stats


if __name__ == "__main__":
    import sys
    for argument in sys.argv[1:]:
        path = Path(argument)
        print(path.name, clean_file(path))
