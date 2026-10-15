"""Import the supplied beacon and link the existing prism item texture."""
import json
import runpy
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / 'src/main/resources/assets/lyycore'
SOURCE = Path('F:/misc/BlockBench')
helpers = runpy.run_path(str(ROOT / 'tools/energy-machines/import_models.py'))
machine, write = helpers['import_machine'], helpers['write']
machine.__globals__['SOURCE'] = SOURCE
machine('creature_summoning_beacon', 'summoning_beacon', 4)

source = json.loads((SOURCE / 'creature_summoning_beacon/source/creature_summoning_beacon.bbmodel').read_text(encoding='utf8'))
path = ASSETS / 'models/item/summoning_beacon.json'
item = json.loads(path.read_text(encoding='utf8'))
# The shared importer normalizes large item geometry by four. Undo that scale
# before applying the artist's actual display, including its single X mirror.
for face in item['faces']:
    for v in face['vertices']:
        v[:3] = [(v[0] - .5) * 4 + .5, v[1] * 4, (v[2] - .5) * 4 + .5]
item['display'] = source['display']
item['gui_light'] = 'front' if source.get('front_gui_light', False) else 'side'
item['render_type'] = 'minecraft:cutout'
write(path, item)

# The prism already has its own supplied pixel texture in the project.
write(ASSETS / 'models/item/crystal_prism.json', {
    'parent': 'minecraft:item/generated',
    'textures': {'layer0': 'lyycore:item/crystal_prism'}
})
