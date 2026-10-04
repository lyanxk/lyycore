import copy
import unittest

from clean_model_surfaces import clean, subtract


def cube(start, end, texture="#0"):
    return {"from": start, "to": end,
            "faces": {"up": {"uv": [0, 0, 16, 16], "texture": texture}}}


class SurfaceCleanupTest(unittest.TestCase):
    def test_partial_overlay_preserves_union_and_uv(self):
        base = cube([0, 0, 0], [16, 1, 16])
        trim = cube([4, 0, 4], [12, 1, 12], "#1")
        result, _, stats = clean([base, trim])
        self.assertEqual(stats["removed_area"], 64)
        self.assertEqual(len(result), 5)
        self.assertEqual(sum((e["to"][0] - e["from"][0]) * (e["to"][2] - e["from"][2])
                             for e in result), 256)
        for element in result[:-1]:
            self.assertEqual(element["faces"]["up"]["uv"],
                             [element["from"][0], element["from"][2],
                              element["to"][0], element["to"][2]])
        self.assertEqual(clean(result)[0], result)

    def test_animation_partitions_keep_rest_pose_surfaces(self):
        elements = [cube([0, 0, 0], [16, 1, 16]), cube([0, 0, 0], [16, 1, 16])]
        self.assertEqual(clean(elements, ["static", "moving"])[0], elements)
        self.assertEqual(len(clean(elements)[0]), 1)

    def test_non_coplanar_and_rotated_faces_are_kept(self):
        base = cube([0, 0, 0], [16, 1, 16])
        offset = cube([0, 0, 0], [16, 1.0001, 16])
        rotated = copy.deepcopy(base)
        rotated["rotation"] = {"origin": [0, 0, 0], "axis": "z", "angle": 45}
        self.assertEqual(clean([base, offset, rotated])[0], [base, offset, rotated])

    def test_touching_edges_are_not_overlap(self):
        self.assertEqual(subtract((0, 0, 1, 1), (1, 0, 2, 1)), [(0, 0, 1, 1)])

    def test_reversed_uvs(self):
        base = cube([0, 0, 0], [16, 1, 16])
        base["faces"]["up"]["uv"] = [16, 16, 0, 0]
        result, _, _ = clean([base, cube([8, 0, 0], [16, 1, 16])])
        self.assertEqual(result[0]["faces"]["up"]["uv"], [16, 16, 8, 0])


if __name__ == "__main__":
    unittest.main()
