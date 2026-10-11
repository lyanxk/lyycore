"""Regression checks for mesh-local origins versus absolute cuboid corners."""
import contextlib
import io
import runpy
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


class CoordinatesTest(unittest.TestCase):
    def bake(self, element):
        source = {
            'groups': [{'uuid': 'bone', 'name': 'crystal', 'origin': [4, 10, 6]}],
            'elements': [dict(element, uuid='element')],
            'outliner': [{'uuid': 'bone', 'children': ['element']}],
            'textures': [{'uv_width': 16, 'uv_height': 16}], 'animations': []
        }
        bake = runpy.run_path(str(ROOT/'tools/research-content/import_models.py'))['bake_mesh']
        output = []
        bake.__globals__['write'] = lambda path, model: output.append(model)
        with contextlib.redirect_stdout(io.StringIO()):
            bake(source, 'origin_regression', ['test:texture.png'])
        return output[0]['bones'][0]['faces'][0]['vertices']

    def test_offset_mesh_rotates_about_its_own_origin(self):
        vertices = self.bake({
            'type': 'mesh', 'origin': [2, 20, 3], 'rotation': [0, 0, 90],
            'vertices': {'a': [1, 0, 0], 'b': [0, 1, 0], 'c': [0, 0, 1]},
            'faces': {'face': {'vertices': ['a', 'b', 'c'],
                      'uv': {'a': [0, 0], 'b': [1, 0], 'c': [0, 1]}, 'texture': 0}}
        })
        # World points after a quarter-turn: (2,21,3), (1,20,3), (2,20,4).
        expected = [[-2/16, 11/16, -3/16], [-3/16, 10/16, -3/16], [-2/16, 10/16, -2/16]]
        self.assertEqual([v[:3] for v in vertices[:3]], expected)

    def test_cube_coordinates_are_absolute(self):
        vertices = self.bake({
            'type': 'cube', 'origin': [2, 20, 3], 'rotation': [0, 0, 90],
            'from': [2, 20, 3], 'to': [3, 21, 4],
            'faces': {'north': {'texture': 0, 'uv': [0, 0, 1, 1]}}
        })
        expected = [[-2/16, 11/16, -3/16], [-2/16, 10/16, -3/16],
                    [-3/16, 10/16, -3/16], [-3/16, 11/16, -3/16]]
        self.assertEqual([v[:3] for v in vertices], expected)


if __name__ == '__main__':
    unittest.main()
