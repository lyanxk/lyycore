"""Import the final factory mesh, preserving its 3x3x3 footprint and all four facings."""
import json
import runpy
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/lyycore"
helpers = runpy.run_path(str(ROOT / "tools/energy-machines/import_models.py"))
machine = helpers["import_machine"]
machine.__globals__["SOURCE"] = Path("F:/misc/BlockBench")
machine("erosion_factory", "erosion_factory", 3, oriented=True,
        emissive_overlay="erosion_factory_emissive.png", top_overhang=2.5/16)

# SquareMachineBlock part IDs describe world-space cells, not cells rotated with the machine.
# Move each rotated source mesh to the correct physical cell; keep saved part IDs unchanged.
path = ASSETS / "blockstates/erosion_factory.json"
states = json.loads(path.read_text(encoding="utf-8"))
for entry in states["multipart"]:
    part = int(entry["when"]["part"])
    x, y, z = part % 3 - 1, part // 9, part // 3 % 3 - 1
    for _ in range(entry["apply"]["y"] // 90): x, z = -z, x
    entry["when"]["part"] = str(x + 1 + (z + 1) * 3 + y * 9)
states["multipart"].append({"when": {"part": "|".join(map(str, range(27, 100)))},
                            "apply": {"model": "lyycore:block/erosion_factory"}})
helpers["write"](path, states)
path = ASSETS / "models/item/erosion_factory.json"
item = json.loads(path.read_text(encoding="utf-8"))
item["render_type"] = "minecraft:cutout"
helpers["write"](path, item)
