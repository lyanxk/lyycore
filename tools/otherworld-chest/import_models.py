"""Import the supplied chest, including its lid, motes and emissive atlas."""
import json
import runpy
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/lyycore"
helpers = runpy.run_path(str(ROOT / "tools/energy-machines/import_models.py"))
machine = helpers["import_machine"]
machine.__globals__["SOURCE"] = Path("F:/misc/BlockBench")
machine("otherworldly_chest", "otherworld_chest", 1, width=1, oriented=True,
        emissive_overlay="otherworldly_chest_emissive.png", gui_scale=0.6, gui_rotation=(20, -150, 0))
path = ASSETS / "blockstates/otherworld_chest.json"
states = json.loads(path.read_text(encoding="utf-8"))
for entry in states["multipart"]:
    del entry["when"]["part"]
helpers["write"](path, states)
path = ASSETS / "models/item/otherworld_chest.json"
item = json.loads(path.read_text(encoding="utf-8"))
item["render_type"] = "minecraft:cutout"
helpers["write"](path, item)
path = ASSETS / "geometry/otherworld_chest.json"
mesh = json.loads(path.read_text(encoding="utf-8"))
mesh["animations"].pop("preview_cycle", None)
helpers["write"](path, mesh)
