"""Compare machine geometry with artist sources without resetting GUI or animations.

Requires Pillow. Defaults to an audit; --apply updates only faces and associated
collision bounds. Sources are never modified. Backups/report live under build/.
"""
import argparse
import contextlib
import copy
import io
import json
import runpy
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / 'src/main/resources/assets/lyycore'
CASES = [
    ('imaginary_condensation_beacon', 'imaginary_condensing_beacon', 4, {}),
    ('spatial_transmission_tower', 'spatial_transmission_tower', 8, {}),
    ('mind_control_beacon', 'mind_control_beacon', 6, {}),
    ('pure_smelting_plant', 'pure_smelting_plant', 3, {'oriented': True}),
    ('pure_smelting_plant', 'pure_smelting_plant_shell', 3,
     {'source_name': 'pure_smelting_plant_inactive', 'oriented': True}),
    ('phantom_matrix', 'phantom_matrix', 4, {'width': 5}),
    ('imaginary_summoning_altar', 'summoning_altar', 1,
     {'source_name': 'summoning_core', 'width': 1, 'source_offset': (8, 0, 8)}),
    ('imaginary_summoning_altar', 'summoning_pedestal', 1,
     {'source_name': 'summoning_pedestal', 'width': 1, 'source_offset': (8, 0, 8)}),
    ('otherworldly_chest', 'otherworld_chest', 1,
     {'width': 1, 'oriented': True, 'emissive_overlay': 'otherworldly_chest_emissive.png'}),
    ('erosion_factory', 'erosion_factory', 3,
     {'oriented': True, 'emissive_overlay': 'erosion_factory_emissive.png', 'top_overhang': 2.5/16}),
    ('imaginary_dragon_nest', 'imaginary_dragon_nest', 3, {'oriented': True}),
]


def read(path):
    return json.loads(path.read_text(encoding='utf-8'))


def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, separators=(',', ':')) + '\n', encoding='utf-8')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', type=Path, default=Path('F:/misc/BlockBench'))
    parser.add_argument('--apply', action='store_true')
    args = parser.parse_args()
    work = ROOT / 'build/model-coordinate-audit'
    stage = work / 'generated'
    stage.mkdir(parents=True, exist_ok=True)
    helpers = runpy.run_path(str(ROOT / 'tools/energy-machines/import_models.py'))
    machine = helpers['import_machine']
    machine.__globals__['SOURCE'] = args.source
    machine.__globals__['ASSETS'] = stage
    helpers['bake'].__globals__['ASSETS'] = stage
    reports, updates = [], {}
    for folder, name, height, options in CASES:
        with contextlib.redirect_stdout(io.StringIO()):
            machine(folder, name, height, **options)
        report = {'model': name, 'bones': [], 'static_parts': [], 'item': False, 'shapes': False}
        relative = Path('geometry') / f'{name}.json'
        old, new = read(ASSETS / relative), read(stage / relative)
        assert len(old['bones']) == len(new['bones']), name
        patched = copy.deepcopy(old)
        for i, (a, b) in enumerate(zip(old['bones'], new['bones'])):
            for key in ('name', 'parent', 'pivot', 'rotation'):
                assert a[key] == b[key], (name, key, a['name'])
            if a['faces'] != b['faces']:
                report['bones'].append(a['name'])
                patched['bones'][i]['faces'] = b['faces']
        if patched != old:
            assert patched['animations'] == old['animations']
            updates[relative] = patched

        # Keep existing blockstate mappings, render types, textures and GUI transforms.
        # Missing/new occupied cells require manual review instead of silently remapping them.
        static = Path('models/block') / name
        for relative in sorted({p.relative_to(stage) for p in (stage/static).glob('*.json')} |
                               {p.relative_to(ASSETS) for p in (ASSETS/static).glob('*.json')}):
            assert (ASSETS/relative).exists(), ('new occupied cell', relative)
            old = read(ASSETS/relative)
            if 'faces' not in old:
                continue
            new_faces = read(stage/relative)['faces'] if (stage/relative).exists() else []
            if old['faces'] != new_faces:
                old['faces'] = new_faces
                updates[relative] = old
                report['static_parts'].append(relative.name)
        relative = Path('models/item') / f'{name}.json'
        old, new = read(ASSETS/relative), read(stage/relative)
        # Some pedestal items deliberately inherit the Java block model.
        if 'faces' in old and old['faces'] != new['faces']:
            old['faces'] = new['faces']
            updates[relative] = old
            report['item'] = True
        if report['bones'] or report['static_parts'] or report['item']:
            relative = Path('geometry') / f'{name}_shapes.json'
            old, new = read(ASSETS/relative), read(stage/relative)
            if old != new:
                updates[relative] = new
                report['shapes'] = True
        reports.append(report)
        print(json.dumps(report, ensure_ascii=False))
    # Check the single-block table using its own static/moving partition rules.
    table = runpy.run_path(str(ROOT/'tools/elemental-research/import_models.py'))
    table['table'].__globals__['ASSETS'] = stage
    table['table'].__globals__['SOURCE'] = args.source
    table['bake'].__globals__['ASSETS'] = stage
    with contextlib.redirect_stdout(io.StringIO()):
        table['table']()
    table_changes = []
    for rel in ['geometry/experiment_table.json', 'models/block/experiment_table.json', 'models/item/experiment_table.json']:
        old, new = read(ASSETS/rel), read(stage/rel)
        keys = ['bones'] if 'bones' in old else ['faces']
        for key in keys:
            if old[key] != new[key]:
                table_changes.append(rel)
    assert not table_changes, ('Table needs separate review', table_changes)
    reports.append({'model': 'experiment_table', 'changes': table_changes})
    if args.apply:
        for relative, value in updates.items():
            backup = work / 'before' / relative
            if not backup.exists():
                backup.parent.mkdir(parents=True, exist_ok=True)
                backup.write_bytes((ASSETS/relative).read_bytes())
            write(ASSETS/relative, value)
    write(work/'report.json', {'applied':args.apply, 'models':reports, 'files':[str(p) for p in updates]})
    print(f'{len(reports)} models checked; {len(updates)} files ' + ('updated' if args.apply else 'need updates'))


if __name__ == '__main__':
    main()
