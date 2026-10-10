"""Import supplied altar, furnace, matrix and raid crystals without modifying editor sources."""
import json
import runpy
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SOURCE = Path('F:/misc/BlockBench')
ASSETS = ROOT / 'src/main/resources/assets/lyycore'
helpers = runpy.run_path(str(ROOT / 'tools/energy-machines/import_models.py'))
machine, bake, write = (helpers[k] for k in ('import_machine', 'bake', 'write'))
machine.__globals__['SOURCE'] = SOURCE
machine('phantom_matrix', 'phantom_matrix', 4, width=5)
machine('imaginary_summoning_altar', 'summoning_altar', 1, source_name='summoning_core', width=1, source_offset=(8,0,8))
machine('imaginary_summoning_altar', 'summoning_pedestal', 1, source_name='summoning_pedestal', width=1, source_offset=(8,0,8))
write(ASSETS/'blockstates/summoning_altar.json', {'variants': {'': {'model': 'lyycore:block/summoning_altar/body_0'}}})

for folder, name in [('imaginary_summoning_altar', 'summoning_pedestal'), ('fission_furnace', 'fission_furnace'), ('fission_furnace', 'fission_furnace_active')]:
    model = json.loads((SOURCE/folder/'models'/f'{name}.json').read_text(encoding='utf-8'))
    model['render_type'] = 'minecraft:cutout'
    write(ASSETS/'models/block'/f'{name}.json', model)
    for texture in (SOURCE/folder/'textures').glob('*.png'):
        shutil.copyfile(texture, ASSETS/'textures/block'/texture.name)
    if name == 'summoning_pedestal':
        write(ASSETS/'blockstates'/f'{name}.json', {'variants': {'': {'model': f'lyycore:block/{name}'}}})
    if name != 'fission_furnace_active':
        write(ASSETS/'models/item'/f'{name}.json', {'parent': f'lyycore:block/{name}'})
write(ASSETS/'blockstates/fission_furnace.json', {'variants': {
    f'active={str(active).lower()},facing={facing}': {'model': 'lyycore:block/fission_furnace'+('_active' if active else ''), 'y': angle}
    for active in [False,True] for facing,angle in [('north',0),('east',90),('south',180),('west',270)]}})

for name in ['recon_crystal', 'assault_crystal']:
    model = json.loads((SOURCE/'crystal_troops/source'/f'{name}.bbmodel').read_text(encoding='utf-8'))
    paths = []
    for texture in model['textures']:
        target = ASSETS/'textures/entity/crystal_troops'/texture['name']; target.parent.mkdir(parents=True,exist_ok=True)
        shutil.copyfile(SOURCE/'crystal_troops/textures'/texture['name'], target)
        paths.append(f'lyycore:textures/entity/crystal_troops/{texture["name"]}')
    # The ranged beam is rendered to the actual hit point by the entity renderer.
    # Remove the authored fixed-length beam, retaining the emitter and all body animation.
    bake(model, name, paths)
    if name == 'recon_crystal':
        path = ASSETS/'geometry'/f'{name}.json'
        mesh = json.loads(path.read_text(encoding='utf-8'))
        for bone in mesh['bones']:
            if bone['name'] == 'laser_pulse_preview': bone['faces'] = []
        write(path, mesh)
for name,parent in [('jump_potion','blue_potion'),('propulsion_potion','control_enhancement_potion')]:
    model = {'parent':f'lyycore:item/{parent}'}
    if name == 'jump_potion': model['overrides'] = [{'predicate': {'custom_model_data': 1}, 'model': 'lyycore:item/jump_boost_icon'}]
    write(ASSETS/'models/item'/f'{name}.json', model)
write(ASSETS/'models/item/jump_boost_icon.json', {'parent':'minecraft:item/generated','textures':{'layer0':'minecraft:mob_effect/jump_boost'}})
