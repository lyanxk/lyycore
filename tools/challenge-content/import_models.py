"""Import the supplied wings and independent Life Revel models.

Run from any directory: python tools/challenge-content/import_models.py
The editable originals stay in the workspace's blockbench/ directory, alongside lyycore/;
only baked runtime data is shipped.
"""
import json
import runpy
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/lyycore"
SOURCE = ROOT.parent / "blockbench"
bake = runpy.run_path(str(ROOT / "tools/research-content/import_models.py"))["bake_mesh"]

for folder, source_name, name in [
    ("white_aegis_wings", "white_aegis_wings", "white_aegis_wings"),
    ("white_aegis_wings", "white_feather_projectile", "white_feather_projectile"),
    ("life_revel", "life_revel_v2", "life_revel"),
    ("life_revel", "life_revel_blindness", "revel_blind"),
    ("life_revel", "life_revel_dancer", "revel_dancer"),
]:
    source = json.loads((SOURCE / folder / "source" / f"{source_name}.bbmodel").read_text(encoding="utf-8"))
    source["animations"] = [a for a in source["animations"] if not a["name"].endswith(".attack_showcase")]
    destination = ASSETS / "textures/entity" / folder
    destination.mkdir(parents=True, exist_ok=True)
    paths = []
    for texture in source["textures"]:
        filename = Path(texture["name"]).name
        shutil.copyfile(SOURCE / folder / "textures" / filename, destination / filename)
        paths.append(f"lyycore:textures/entity/{folder}/{filename}")
    # The attack's body translation previews a dash that the server already moves in world space.
    bake(source, name, paths, strip_motion={("body", "attack")} if name == "revel_dancer" else ())
