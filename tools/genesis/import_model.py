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
source = json.loads((SOURCE / "origin_of_all/source/origin_of_all.bbmodel").read_text(encoding="utf-8"))
# Bedrock's BB Display applies the artist's transform around (0, 0, 0).
# The shared block importer already adds half a block to X/Z, but leaves Y
# ground-relative. ItemRenderer subtracts (.5, .5, .5), so compensate Y here
# as well. This is item-only: placed geometry and lid pivots must stay intact.
for face in item["faces"]:
    for vertex in face["vertices"]:
        vertex[1] = round(vertex[1] + 0.5, 6)
item["display"] = source.get("display", {})
item["gui_light"] = "front" if source.get("front_gui_light", False) else "side"
item["render_type"] = "minecraft:cutout"
write(path, item)

path = ASSETS / "geometry/genesis.json"
mesh = json.loads(path.read_text(encoding="utf-8"))
mesh["animations"].pop("preview_cycle", None)
write(path, mesh)
