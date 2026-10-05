"""Bake the authored pink rope rig for AnimatedMeshModel (no glTF dependency)."""
import copy
import json
import runpy
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT.parent / "blockbench/pink_energy_rope"
ASSETS = ROOT / "src/main/resources/assets/lyycore"


def main():
    helpers = runpy.run_path(str(ROOT / "tools/research-content/import_models.py"))
    source = json.loads((SOURCE / "source/pink_energy_rope.bbmodel").read_text(encoding="utf-8"))
    source = copy.deepcopy(source)
    # Blockbench mesh vertices are relative to the mesh origin. The shared baker
    # accepts absolute points; notably, every segmented tether has its own origin.
    for element in source["elements"]:
        if element.get("type") == "mesh":
            origin = element.get("origin", [0, 0, 0])
            element["vertices"] = {key: [v + o for v, o in zip(point, origin)]
                                   for key, point in element["vertices"].items()}
    texture = ASSETS / "textures/entity/guiding/pink_energy.png"
    texture.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(SOURCE / "textures/pink_energy.png", texture)
    helpers["bake_mesh"](source, "pink_energy_rope", ["lyycore:textures/entity/guiding/pink_energy.png"])
    path = ASSETS / "geometry/pink_energy_rope.json"
    mesh = json.loads(path.read_text(encoding="utf-8"))
    mesh["emissive_textures"] = [0]
    assert set(mesh["animations"]) == {"grab_and_pull", "grab_extend", "grab_wrap", "grab_pull", "bound_idle", "release"}
    assert sum(b["name"].startswith("coil_") for b in mesh["bones"]) == 64
    assert sum(b["name"].startswith("tether_") and b["name"] != "tether_lead" for b in mesh["bones"]) == 24
    helpers["write"](path, mesh)


if __name__ == "__main__":
    main()
