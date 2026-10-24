"""Apply selective emission without rebaking geometry or changing display transforms.

Run with the bundled Python + Pillow. BB emission alpha 3 is almost fullbright;
alpha 0 stays cut out. The companion opacity atlas restores game transparency
on subsequent import_machine exports.
"""
import base64
import copy
import io
import json
import shutil
import uuid
from datetime import datetime
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/lyycore"
SOURCE = Path("F:/misc/BlockBench/phantom_matrix")
MODEL = SOURCE / "source/phantom_matrix.bbmodel"
NAME = "phantom_matrix_emission"


def luminous(name):
    return name.startswith(("bed_crystal_", "dominant_core_", "lower_core_",
        "left_core_shard_", "right_core_shard_", "floating_crystal_")) or name in {
        "pink_base_seam", "energy_x", "energy_z", "light_x", "light_z",
        "pink_rib_inlay", "corner_pink_lens", "side_pink_lens", "output_gem",
        "output_highlight", "output_top_lens"}


def rectangle(uv):
    return min(uv[0], uv[2]), min(uv[1], uv[3]), max(uv[0], uv[2]), max(uv[1], uv[3])


def main():
    source = json.loads(MODEL.read_text(encoding="utf-8"))
    before_source = copy.deepcopy(source)
    rects = {True: set(), False: set()}
    for element in source["elements"]:
        for face in element["faces"].values():
            if face.get("texture") is not None:
                rects[luminous(element["name"])].add(rectangle(face["uv"]))
    assert not rects[True] & rects[False], "Mixed material UVs require a full rebake"

    def emission(face):
        # Clipped cell faces retain interpolated UVs inside the source rectangle.
        points = [(v[3] * 1024, v[4] * 512) for v in face["vertices"]]
        found = {kind for kind, rectangles in rects.items() for x0, y0, x1, y1 in rectangles
                 if all(x0-.002 <= u <= x1+.002 and y0-.002 <= v <= y1+.002 for u,v in points)}
        assert len(found) == 1, ("Unclassified/ambiguous UV", points, found)
        return found.pop()

    original_atlas = Image.open(SOURCE / "textures/phantom_matrix.png").convert("RGBA")
    opacity = original_atlas.getchannel("A")
    bb_atlas = original_atlas.copy()
    bb_atlas.putalpha(opacity.point(lambda a: 3 if a else 0))
    stream = io.BytesIO()
    bb_atlas.save(stream, format="PNG")
    index = next((i for i,t in enumerate(source["textures"]) if t["name"] == NAME+".png"), len(source["textures"]))
    tex = copy.deepcopy(source["textures"][0])
    tex.update(name=NAME+".png", id=str(index), uuid=str(uuid.uuid5(uuid.NAMESPACE_URL, NAME)),
               render_mode="emissive", source="data:image/png;base64,"+base64.b64encode(stream.getvalue()).decode())
    if index == len(source["textures"]):
        source["textures"].append(tex)
    else:
        source["textures"][index] = tex
    for element in source["elements"]:
        if luminous(element["name"]):
            for face in element["faces"].values():
                if face.get("texture") is not None:
                    face["texture"] = index

    files = [ASSETS / "models/item/phantom_matrix.json",
             *sorted((ASSETS / "models/block/phantom_matrix").glob("*.json"))]
    staged = {}
    counts = {}
    for path in files:
        model = json.loads(path.read_text(encoding="utf-8"))
        prior = copy.deepcopy(model)
        emitted = 0
        for face in model.get("faces", []):
            face["emissive"] = emission(face)
            face["texture"] = str(index) if face["emissive"] else "0"
            emitted += face["emissive"]
        if emitted:
            model["textures"][str(index)] = f"lyycore:block/phantom_matrix/{NAME}"
        assert model.get("display") == prior.get("display")
        assert [f["vertices"] for f in model.get("faces",[])] == [f["vertices"] for f in prior.get("faces",[])]
        counts[str(path.relative_to(ASSETS))] = emitted
        if model != prior:
            staged[path] = model

    path = ASSETS / "geometry/phantom_matrix.json"
    mesh = json.loads(path.read_text(encoding="utf-8"))
    prior_mesh = copy.deepcopy(mesh)
    texture_path = f"lyycore:textures/block/phantom_matrix/{NAME}.png"
    if texture_path not in mesh["textures"]:
        assert len(mesh["textures"]) == index
        mesh["textures"].append(texture_path)
    animated = 0
    for bone in mesh["bones"]:
        for face in bone["faces"]:
            if emission(face):
                face["texture"] = index
                animated += 1
    mesh["emissive_textures"] = sorted(set(mesh.get("emissive_textures", [])) | {index})
    assert mesh["animations"] == prior_mesh["animations"]
    assert [[f["vertices"] for f in b["faces"]] for b in mesh["bones"]] == [[f["vertices"] for f in b["faces"]] for b in prior_mesh["bones"]]
    staged[path] = mesh
    staged[MODEL] = source
    assert source.get("display") == before_source.get("display")
    assert source["animations"] == before_source["animations"]
    for old, new in zip(before_source["elements"], source["elements"]):
        assert {k:v for k,v in old.items() if k != "faces"} == {k:v for k,v in new.items() if k != "faces"}
        for key, face in old["faces"].items():
            assert {k:v for k,v in face.items() if k != "texture"} == {k:v for k,v in new["faces"][key].items() if k != "texture"}
            if face.get("texture") is None: assert new["faces"][key].get("texture") is None

    backup = SOURCE / "revisions" / ("before_selective_emission_"+datetime.now().strftime("%Y%m%d_%H%M%S"))
    for path in staged:
        dest = backup / ("source.bbmodel" if path == MODEL else path.relative_to(ASSETS))
        dest.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(path, dest)
    (SOURCE / "textures" / (NAME+".png")).write_bytes(stream.getvalue())
    opacity.save(SOURCE / "textures" / (NAME+".opacity.png"))
    game_texture = ASSETS / f"textures/block/phantom_matrix/{NAME}.png"
    original_atlas.save(game_texture)
    assert Image.open(game_texture).tobytes() == original_atlas.tobytes()
    for path, data in staged.items():
        path.write_text(json.dumps(data, ensure_ascii=False, separators=(",", ":"))+"\n", encoding="utf-8")
    report = {"changed_files": len(staged), "animated_emissive_faces": animated,
              "emissive_faces": counts, "geometry_animation_display_unchanged": True,
              "rgb_and_cutout_preserved": True, "backup": str(backup)}
    (SOURCE / "source/emission_validation.json").write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
