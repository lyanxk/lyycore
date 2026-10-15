"""Validate clipped surfaces, cell-local coordinates, UVs, and unchanged full item."""
import json
import math
from pathlib import Path
from import_mirror import ASSETS, SOURCE, faces, area, generate

source=json.loads(SOURCE.read_text(encoding='utf-8-sig'))
expected=generate(source)
for i,name in enumerate(['mirror','mirror_upper']):
    actual=json.loads((ASSETS/f'models/block/{name}.json').read_text(encoding='utf-8'))
    assert actual['faces']==expected[i]
    assert actual['faces'] and actual['ambientocclusion'] is False
    for f in actual['faces']:
        assert 3<=len(f['vertices'])<=4
        assert area(f['vertices'])>1e-10
        assert all(-1e-8<=v[1]<=1+1e-8 for v in f['vertices'])
        assert all(math.isfinite(x) for v in f['vertices'] for x in v)
original=list(faces(source))
assert any(v[1]>1 for f in original for v in f['vertices']), 'Fixture no longer spans both cells'
assert math.isclose(sum(area(f['vertices']) for f in original),sum(area(f['vertices']) for cell in expected for f in cell),rel_tol=1e-7)
item=json.loads((ASSETS/'models/item/mirror_full.json').read_text(encoding='utf-8'))
assert item==source, 'Original Java item geometry or artist display changed'
states=json.loads((ASSETS/'blockstates/mirror.json').read_text(encoding='utf-8'))
assert len(states['variants'])==8
print('PASS: upper/lower cell lighting bounds, no missing surfaces, full Java inventory model preserved, all four facing variants')
