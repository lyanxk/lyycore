"""Bake the final meteor and its emissive mask without changing artist sources."""
import copy
import json
import runpy
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/lyycore"
SOURCE = Path("F:/misc/BlockBench/crystal_meteor")
DESTINATION = ASSETS / "textures/entity/crystal_meteor"

model = json.loads((SOURCE / "source/crystal_meteor.bbmodel").read_text(encoding="utf-8"))
model["animations"] = [clip for clip in model["animations"] if clip["name"].endswith(".flight")]
DESTINATION.mkdir(parents=True, exist_ok=True)
texture_paths = []
for name in ("crystal_meteor.png", "crystal_meteor_emissive.png"):
    shutil.copyfile(SOURCE / "textures" / name, DESTINATION / name)
    texture_paths.append(f"lyycore:textures/entity/crystal_meteor/{name}")

bake = runpy.run_path(str(ROOT / "tools/research-content/import_models.py"))["bake_mesh"]
bake(model, "crystal_meteor", texture_paths[:1])
path = ASSETS / "geometry/crystal_meteor.json"
mesh = json.loads(path.read_text(encoding="utf-8"))
mesh["textures"] = texture_paths
mesh["emissive_textures"] = [1]
for bone in mesh["bones"]:
    overlay = copy.deepcopy(bone["faces"])
    for face in overlay:
        face["texture"] = 1
    bone["faces"].extend(overlay)
path.write_text(json.dumps(mesh, separators=(",", ":")) + "\n", encoding="utf-8")
