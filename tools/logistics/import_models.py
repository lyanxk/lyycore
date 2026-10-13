"""Import the supplied logistics assets without modifying their Blockbench sources."""
import json
import runpy
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/lyycore"
SOURCE = Path("F:/misc/BlockBench")
helpers = runpy.run_path(str(ROOT / "tools/energy-machines/import_models.py"))
machine, write = helpers["import_machine"], helpers["write"]
machine.__globals__["SOURCE"] = SOURCE
machine("logistics_matrix", "logistics_matrix", 4, width=5, gui_scale=0.75)
item_path = ASSETS / "models/item/logistics_matrix.json"
item = json.loads(item_path.read_text(encoding="utf-8"))
item["render_type"] = "minecraft:cutout"
write(item_path, item)

for name, folder, source_name in (
    ("matrix_input", "matrix_ports", "matrix_input"),
    ("matrix_output", "matrix_ports", "matrix_output"),
    ("matrix_collector", "directional_collector", "directional_collector"),
):
    model = json.loads((SOURCE / folder / "models" / f"{source_name}.json").read_text(encoding="utf-8"))
    for key, value in model["textures"].items():
        if not value.startswith("#"):
            model["textures"][key] = f"lyycore:block/{name}"
    write(ASSETS / "models/block" / f"{name}.json", model)
    write(ASSETS / "models/item" / f"{name}.json", {"parent": f"lyycore:block/{name}"})
    shutil.copyfile(SOURCE / folder / "textures" / f"{source_name}.png", ASSETS / "textures/block" / f"{name}.png")
    variants = {"": {"model": f"lyycore:block/{name}"}}
    if name == "matrix_collector":
        variants = {f"facing={facing}": dict(model=f"lyycore:block/{name}", x=x, y=y)
                    for facing, x, y in (("north", 0, 0), ("east", 0, 90), ("south", 0, 180),
                                         ("west", 0, 270), ("up", 270, 0), ("down", 90, 0))}
    write(ASSETS / "blockstates" / f"{name}.json", {"variants": variants})
