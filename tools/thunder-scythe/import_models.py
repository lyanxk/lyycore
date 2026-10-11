"""Bake the original mesh model and clips; never regenerate or edit the source."""
import json
import math
import runpy
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SOURCE = Path("F:/misc/BlockBench/thunder_greatscythe")
ASSETS = ROOT / "src/main/resources/assets/lyycore"
helpers = runpy.run_path(str(ROOT / "tools/research-content/import_models.py"))
write, rotate = helpers["write"], helpers["rotate"]
# The editable source now contains the Bedrock conversion; use the original backup.
source = json.loads((SOURCE / "revisions/before_bedrock_conversion.bbmodel").read_text(encoding="utf-8"))
groups = {g["uuid"]: g for g in source["groups"]}
source["outliner"] = [node for node in source["outliner"] if groups[node["uuid"]].get("export", True)]

# The runtime interpolates baked keys. Sample authored Bezier handles here rather
# than discarding their easing or adding a new per-frame curve evaluator.
def bezier(a, b, c, d, t):
    return (1-t)**3*a + 3*(1-t)**2*t*b + 3*(1-t)*t*t*c + t**3*d

for animation in source["animations"]:
    for animator in animation["animators"].values():
        keys = animator.get("keyframes", [])
        for channel in {k["channel"] for k in keys if k.get("interpolation") == "bezier"}:
            curve = sorted((k for k in keys if k["channel"] == channel), key=lambda k:k["time"])
            assert all(k.get("interpolation") == "bezier" for k in curve)
            sampled = []
            for a,b in zip(curve,curve[1:]):
                steps = max(1, math.ceil((b["time"]-a["time"])*60))
                for step in range(steps):
                    time = a["time"]+(b["time"]-a["time"])*step/steps
                    value = {}
                    for axis,name in enumerate("xyz"):
                        lo,hi = 0.,1.
                        for _ in range(24):
                            t=(lo+hi)/2
                            x=bezier(a["time"], a["time"]+a["bezier_right_time"][axis], b["time"]+b["bezier_left_time"][axis], b["time"], t)
                            if x < time: lo=t
                            else: hi=t
                        start,end=float(a["data_points"][0][name]),float(b["data_points"][0][name])
                        value[name]=bezier(start,start+a["bezier_right_value"][axis],end+b["bezier_left_value"][axis],end,(lo+hi)/2)
                    sampled.append({"channel":channel,"time":time,"interpolation":"linear","data_points":[value]})
            sampled.append(dict(curve[-1],interpolation="linear"))
            keys=[k for k in keys if k["channel"] != channel]+sampled
        animator["keyframes"]=keys
texture = "thunder_greatscythe.png"
texture_id = f"lyycore:item/thunder_scythe/{Path(texture).stem}"
target = ASSETS / "textures/item/thunder_scythe" / texture
target.parent.mkdir(parents=True, exist_ok=True)
shutil.copyfile(SOURCE / "textures" / texture, target)
helpers["bake_mesh"](source, "thunder_scythe", [f"lyycore:textures/item/thunder_scythe/{texture}"])

# The handheld animation is anchored at the player's feet. Bake a compact static
# preview separately, so inventory/JEI/dropped items do not use player-space offsets.
mesh = json.loads((ASSETS / "geometry/thunder_scythe.json").read_text(encoding="utf-8"))
bones, clip = mesh["bones"], mesh["animations"]["held_idle"]
def world(point, index):
    while index >= 0:
        bone = bones[index]
        channels = clip["tracks"].get(str(index), {})
        def value(channel, default): return channels.get(channel, [[0, *default]])[0][1:4]
        point = [v*s for v,s in zip(point, value("scale", [1,1,1]))]
        point = rotate(point, [a+b for a,b in zip(bone["rotation"], value("rotation", [0,0,0]))])
        parent = bones[bone["parent"]]["pivot"] if bone["parent"] >= 0 else [0,0,0]
        point = [v+(p-o+t)/16 for v,p,o,t in zip(point, bone["pivot"], parent, value("position", [0,0,0]))]
        index = bone["parent"]
    return point
faces = [{"texture":"0", "emissive":False, "vertices":[world(v[:3], index)+v[3:5] for v in face["vertices"]]}
         for index,bone in enumerate(bones) for face in bone["faces"]]
points = [v for face in faces for v in face["vertices"]]
low = [min(p[a] for p in points) for a in range(3)]
high = [max(p[a] for p in points) for a in range(3)]
size = max(b-a for a,b in zip(low,high)) / .9
for face in faces:
    face["vertices"] = [[round((v[a]-(low[a]+high[a])/2)/size+.5,6) for a in range(3)]+v[3:5] for v in face["vertices"]]
write(ASSETS / "models/item/thunder_scythe.json", {
    "loader":"lyycore:machine_mesh", "parent":"minecraft:block/block", "render_type":"minecraft:cutout",
    "ambientocclusion":False, "textures":{"0":texture_id, "particle":texture_id},
    "faces":faces, "display":{"gui":{"rotation":[0,0,0], "scale":[-.8,.8,.8]}, "ground":{"scale":[.5]*3}}})
