"""Import Origin of All using the shared static-body / animated-lid pipeline."""
import json
import runpy
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/lyycore"
SOURCE = Path("F:/misc/BlockBench")
helpers = runpy.run_path(str(ROOT / "tools/energy-machines/import_models.py"))
machine, write = helpers["import_machine"], helpers["write"]
machine.__globals__["SOURCE"] = SOURCE
machine("origin_of_all", "genesis", 1, width=1, oriented=True, clip_surfaces=False)

path = ASSETS / "blockstates/genesis.json"
states = json.loads(path.read_text(encoding="utf-8"))
for entry in states["multipart"]:
    del entry["when"]["part"]
write(path, states)

path = ASSETS / "models/item/genesis.json"
item = json.loads(path.read_text(encoding="utf-8"))
item.update(json.loads((SOURCE / "origin_of_all/source/origin_of_all.display.json").read_text(encoding="utf-8")))
item["render_type"] = "minecraft:cutout"
write(path, item)

path = ASSETS / "geometry/genesis.json"
mesh = json.loads(path.read_text(encoding="utf-8"))
mesh["animations"].pop("preview_cycle", None)
write(path, mesh)
