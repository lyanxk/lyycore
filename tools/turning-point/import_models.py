"""Import the supplied turning-point machines and dragon; keep editor sources external."""
import json
import runpy
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SOURCE = Path("F:/misc/BlockBench")
ASSETS = ROOT / "src/main/resources/assets/lyycore"
helpers = runpy.run_path(str(ROOT / "tools/energy-machines/import_models.py"))
helpers["import_machine"].__globals__["SOURCE"] = SOURCE
helpers["import_machine"]("pure_smelting_plant", "pure_smelting_plant", 3, oriented=True, gui_scale=0.6)
helpers["import_machine"]("pure_smelting_plant", "pure_smelting_plant_shell", 3,
                          source_name="pure_smelting_plant_inactive", oriented=True, gui_scale=0.6)
helpers["import_machine"]("imaginary_dragon_nest", "imaginary_dragon_nest", 3, oriented=True)
name = "imaginary_dragon_adult"
model = json.loads((SOURCE / name / "source" / f"{name}.bbmodel").read_text(encoding="utf-8"))
model["animations"] = [a for a in model["animations"] if not a["name"].endswith(".display")]
paths = []
for texture in model["textures"]:
    target = ASSETS / "textures/entity" / name / texture["name"]
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(SOURCE / name / "textures" / texture["name"], target)
    paths.append(f"lyycore:textures/entity/{name}/{texture['name']}")
helpers["bake"](model, name, paths)
