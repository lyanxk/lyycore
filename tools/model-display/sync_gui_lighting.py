"""Sync/check GUI lighting only; run after saving BB sources, before packaging.

Usage: python tools/model-display/sync_gui_lighting.py --apply
       python tools/model-display/sync_gui_lighting.py --check
Does not change world shading, meshes, textures or display transforms.
"""
import argparse
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MANIFEST = Path(__file__).with_name('gui_lighting_manifest.json')


def read(path):
    return json.loads(path.read_text(encoding='utf-8-sig'))


def desired_updates(manifest):
    bb = Path(manifest['bb_root'])
    policy = manifest['gui_light']
    for entry in manifest['models']:
        source_path = bb / entry['source']
        source = read(source_path)
        actual = 'front' if source.get('front_gui_light', False) else 'side'
        if actual != policy:
            raise ValueError(f'{source_path}: BB GUI light is {actual}; save it as {policy} first')
        if entry.get('game_item'):
            path = ROOT / 'src/main/resources/assets/lyycore/models/item' / (entry['game_item'] + '.json')
            yield path, {'gui_light': actual}
        for path in entry.get('java_exports', []):
            yield bb / path, {'gui_light': actual}
        for path in entry.get('bb_exports', []):
            yield bb / path, {'front_gui_light': actual == 'front'}
        if entry.get('display_contract'):
            yield bb / entry['display_contract'], {'gui_light': actual, 'display': source.get('display', {})}


def run(apply=False):
    manifest = read(MANIFEST)
    results = []
    failures = []
    for path, updates in desired_updates(manifest):
        before = read(path) if path.exists() else {}
        after = {**before, **updates}
        changed = before != after
        if changed and apply:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(json.dumps(after, ensure_ascii=False, separators=(',', ':')) + '\n', encoding='utf-8')
        elif changed:
            failures.append(str(path))
        results.append({'path': str(path), 'changed': changed, 'expected_gui_light': manifest['gui_light']})
    print(json.dumps({'files': results, 'skipped': manifest['skip_game_items']}, ensure_ascii=False, indent=2))
    if failures:
        raise SystemExit('GUI light mismatch: ' + ', '.join(failures))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument('--apply', action='store_true')
    mode.add_argument('--check', action='store_true')
    args = parser.parse_args()
    run(apply=args.apply)
