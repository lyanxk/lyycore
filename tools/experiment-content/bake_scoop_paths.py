"""Bake six collision paths from the same bone transforms used by the scoop renderer."""
import json
import math
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
BONES = [f"{side}_primary_{index:02}" for side, index in (
    ("upper_left", 2), ("upper_left", 8), ("lower_left", 4),
    ("upper_right", 2), ("upper_right", 8), ("lower_right", 4))]


def sample(keys, time, default):
    if not keys:
        return [default] * 3
    if time <= keys[0][0]:
        return keys[0][1:4]
    for index in range(len(keys) - 1):
        a, b = keys[index:index + 2]
        if time > b[0]:
            continue
        t = (time - a[0]) / (b[0] - a[0])
        if len(a) > 4 and a[4] == 1 and time < b[0]:
            return a[1:4]
        if (len(a) > 4 and a[4] == 2) or (len(b) > 4 and b[4] == 2):
            previous, following = keys[max(0, index - 1)], keys[min(len(keys) - 1, index + 2)]
            return [.5 * (2*a[i] + (-previous[i]+b[i])*t
                         + (2*previous[i]-5*a[i]+4*b[i]-following[i])*t*t
                         + (-previous[i]+3*a[i]-3*b[i]+following[i])*t*t*t) for i in range(1, 4)]
        return [a[i] + (b[i] - a[i]) * t for i in range(1, 4)]
    return keys[-1][1:4]


def transform(point, bone, parent, track, time):
    scale = sample(track.get("scale", []), time, 1)
    x, y, z = [point[i] * scale[i] for i in range(3)]
    angles = sample(track.get("rotation", []), time, 0)
    rx, ry, rz = [math.radians(bone["rotation"][i] + angles[i]) for i in range(3)]
    # PoseStack applies T * Rz * Ry * Rx * S.
    y, z = y*math.cos(rx)-z*math.sin(rx), y*math.sin(rx)+z*math.cos(rx)
    x, z = x*math.cos(ry)+z*math.sin(ry), -x*math.sin(ry)+z*math.cos(ry)
    x, y = x*math.cos(rz)-y*math.sin(rz), x*math.sin(rz)+y*math.cos(rz)
    offset = sample(track.get("position", []), time, 0)
    return [v + (bone["pivot"][i] - parent[i] + offset[i])/16 for i, v in enumerate((x, y, z))]


def bake_paths():
    mesh = json.loads((ROOT / "src/main/resources/assets/lyycore/geometry/white_aegis_four_wings.json").read_text(encoding="utf-8"))
    bones = mesh["bones"]
    clip = mesh["animations"]["scoop_attack"]
    paths = []
    for name in BONES:
        index = next(i for i, bone in enumerate(bones) if bone["name"] == name)
        vertices = [v for face in bones[index]["faces"] for v in face["vertices"]]
        center = [(min(v[i] for v in vertices) + max(v[i] for v in vertices))/2 for i in range(3)]
        path = []
        for tick in range(41):
            point, current = center, index
            while current >= 0:
                bone = bones[current]
                parent = bones[bone["parent"]]["pivot"] if bone["parent"] >= 0 else [0, 0, 0]
                point = transform(point, bone, parent, clip["tracks"].get(str(current), {}), tick/40*clip["length"])
                current = bone["parent"]
            # Same Z reflection as WingsScoopRenderer; runtime applies current player yaw.
            path.append([round(point[0], 6), round(point[1], 6), round(-point[2], 6)])
        paths.append(path)
    target = ROOT / "src/main/resources/data/lyycore/wings/scoop_paths.json"
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps({"bones": BONES, "paths": paths}, separators=(",", ":")) + "\n", encoding="utf-8")


if __name__ == "__main__":
    bake_paths()
