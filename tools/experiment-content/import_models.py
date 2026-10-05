"""Import the supplied cauldron, control beacon and four-wing assets."""
import json
import runpy
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/lyycore"
SOURCE = Path("F:/misc/BlockBench")
helpers = runpy.run_path(str(ROOT / "tools/energy-machines/import_models.py"))
write = helpers["write"]


def cauldron():
    source = SOURCE / "alloy_cauldron"
    target = ASSETS / "textures/block/alloy_cauldron/cauldron_alloy.png"
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(source / "textures/cauldron_alloy.png", target)
    multipart = []
    for part in ("alloy_cauldron_shell", "water_supply", "liquid_blue", "liquid_pink", "liquid_silver", "liquid_strange", "liquid_water"):
        model = json.loads((source / "exports" / f"{'liquid_blue' if part == 'liquid_water' else part}.json").read_text(encoding="utf-8"))
        model["render_type"] = "minecraft:cutout" if part.endswith("shell") else "minecraft:translucent"
        if part == "liquid_water": model["textures"]["basin_liquid"] = "minecraft:block/water_still"
        for element in model["elements"]:
            for face in element["faces"].values(): face.pop("tintindex", None)
            if part == "liquid_water":
                for face in element["faces"].values(): face["tintindex"] = 0
        write(ASSETS / "models/block/alloy_cauldron" / f"{part}.json", model)
        for facing, angle in (("north",0),("east",90),("south",180),("west",270)):
            condition = {"facing": facing}
            if part.startswith("liquid_"): condition["liquid"] = part.removeprefix("liquid_")
            multipart.append({"when":condition,"apply":{"model":f"lyycore:block/alloy_cauldron/{part}","y":angle}})
    write(ASSETS / "blockstates/alloy_cauldron.json", {"multipart":multipart})
    model = json.loads((source / "exports/alloy_cauldron_blue.json").read_text(encoding="utf-8"))
    model["render_type"] = "minecraft:translucent"
    write(ASSETS / "models/item/alloy_cauldron.json", model)


def wings():
    name = "white_aegis_four_wings"
    folder = SOURCE / name
    source = json.loads((folder / "source" / f"{name}.bbmodel").read_text(encoding="utf-8"))
    # Showcases and player/target references are editor-only.
    source["animations"] = [a for a in source["animations"] if not a["name"].endswith("_showcase")]
    paths = []
    for texture in source["textures"]:
        target = ASSETS / "textures/entity" / texture["name"]
        shutil.copyfile(folder / "textures" / texture["name"], target)
        paths.append("lyycore:textures/entity/" + texture["name"])
    helpers["bake"](source, name, paths)
    path = ASSETS / "geometry" / f"{name}.json"
    mesh = json.loads(path.read_text(encoding="utf-8"))
    elements = {e["uuid"]: e for e in source["elements"]}
    locators = {}
    index = 0
    def walk(node):
        nonlocal index
        bone = index
        index += 1
        for child in node["children"]:
            if isinstance(child, dict): walk(child)
            elif elements[child].get("type") == "locator":
                locator = elements[child]
                locators[locator["name"]] = {"bone":bone,"position":[(x-p)/16 for x,p in zip(locator["position"],mesh["bones"][bone]["pivot"])]}
    for node in source["outliner"]: walk(node)
    mesh["locators"] = locators
    mesh["attack_feathers"] = json.loads((folder / "source/attack_handoff.json").read_text(encoding="utf-8"))["feathers"]
    write(path, mesh)
    runpy.run_path(str(ROOT / "tools/experiment-content/bake_scoop_paths.py"))["bake_paths"]()


if __name__ == "__main__":
    cauldron()
    helpers["import_machine"]("mind_control_beacon", "mind_control_beacon", 6)
    wings()
