"""Import the finished Java factory and Riftcleaver mesh without changing artist sources."""
import json
import runpy
import shutil
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/lyycore"
SOURCE = Path("F:/misc/BlockBench")
helpers = runpy.run_path(str(ROOT / "tools/research-content/import_models.py"))
write, rotate = helpers["write"], helpers["rotate"]


def factory():
    source = SOURCE / "spacetime_processing_factory_java"
    exported = source / "export/assets/lyycore"
    for kind in ("block", "item"):
        text = (exported / f"models/{kind}/spacetime_processing_factory.json").read_text(encoding="utf-8")
        write(ASSETS / f"models/{kind}/spacetime_factory.json", json.loads(text.replace("spacetime_processing_factory", "spacetime_factory")))
    target = ASSETS / "textures/block/spacetime_factory"
    target.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(source / "textures/factory_palette.png", target / "factory_palette.png")
    write(ASSETS / "blockstates/spacetime_factory.json", {"variants": {"": {"model": "lyycore:block/spacetime_factory"}}})


def riftbreaker():
    folder = SOURCE / "riftcleaver"
    source = json.loads((folder / "source/riftcleaver.bbmodel").read_text(encoding="utf-8"))
    source.setdefault("animations", [])
    paths, textures, emissive = [], {}, set()
    target = ASSETS / "textures/item/riftbreaker"
    target.mkdir(parents=True, exist_ok=True)
    for i, texture in enumerate(source["textures"]):
        name = texture["name"]
        texture_source = folder / "textures" / name
        if texture.get("render_mode") == "emissive":
            # BB encodes emission strength in alpha. This solid energy atlas
            # needs opaque pixels in Java cutout; emission is set on the faces.
            with Image.open(texture_source) as image:
                image = image.convert("RGBA")
                image.putalpha(255)
                image.save(target / name)
        else:
            shutil.copyfile(texture_source, target / name)
        paths.append(f"lyycore:textures/item/riftbreaker/{name}")
        textures[str(i)] = f"lyycore:item/riftbreaker/{Path(name).stem}"
        if texture.get("render_mode") == "emissive": emissive.add(i)
    textures["particle"] = textures["0"]
    helpers["bake_mesh"](source, "riftbreaker", paths)
    mesh_path = ASSETS / "geometry/riftbreaker.json"
    bones = json.loads(mesh_path.read_text(encoding="utf-8"))["bones"]

    def world(point, index):
        while index >= 0:
            bone = bones[index]
            point = rotate(point, bone["rotation"])
            parent = bones[bone["parent"]]["pivot"] if bone["parent"] >= 0 else [0, 0, 0]
            point = [v + (p - o) / 16 for v, p, o in zip(point, bone["pivot"], parent)]
            index = bone["parent"]
        # BB Display offsets the source by (0, -8, 0); Java item rendering
        # subtracts (.5, .5, .5). Add half a block only on X/Z so both agree.
        return [round(v + offset, 6) for v, offset in zip(point, (.5, 0, .5))]

    faces = [{"texture": str(face["texture"]), "emissive": face["texture"] in emissive,
              "vertices": [world(v[:3], i) + v[3:5] for v in face["vertices"]]}
             for i, bone in enumerate(bones) for face in bone["faces"]]
    write(ASSETS / "models/item/riftbreaker.json", {
        "loader": "lyycore:machine_mesh", "parent": "minecraft:block/block",
        "render_type": "minecraft:cutout", "ambientocclusion": False,
        "gui_light": "front" if source.get("front_gui_light") else "side",
        "textures": textures, "faces": faces, "display": source.get("display", {})})
    # Static item: the intermediate skeleton is not needed by the game.
    mesh_path.unlink()


if __name__ == "__main__":
    factory()
    riftbreaker()
