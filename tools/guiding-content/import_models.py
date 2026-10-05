"""Import the current Guiding encounter models, excluding showcase/obsolete revisions."""
import copy
import json
import runpy
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT.parent / "blockbench" / "guiding_light_boss"
ASSETS = ROOT / "src/main/resources/assets/lyycore"
helpers = runpy.run_path(str(ROOT / "tools/research-content/import_models.py"))
sample = runpy.run_path(str(ROOT / "tools/experiment-content/bake_scoop_paths.py"))["sample"]
destination = ASSETS / "textures/entity/guiding"
destination.mkdir(parents=True, exist_ok=True)
for texture in ("white_shell.png", "white_shell_emissive.png", "laser_fx.png"):
    shutil.copyfile(SOURCE / "textures" / texture, destination / texture)

for name in ("guiding_light", "endless_demand", "lost_follower", "fanatic_supporter", "endless_demand_laser_fx"):
    source = json.loads((SOURCE / "source" / f"{name}.bbmodel").read_text(encoding="utf-8"))
    effect = name.endswith("laser_fx")
    texture = "laser_fx.png" if effect else "white_shell.png"
    helpers["bake_mesh"](source, name, [f"lyycore:textures/entity/guiding/{texture}"])
    path = ASSETS / "geometry" / f"{name}.json"
    mesh = json.loads(path.read_text(encoding="utf-8"))
    if effect:
        # Stretch only the beam/aim geometry to the gameplay range; leave rings and sparks round.
        for bone in mesh["bones"]:
            if bone["name"] not in ("beam", "aim_line"):
                continue
            length = max(abs(v[2]) for face in bone["faces"] for v in face["vertices"])
            for face in bone["faces"]:
                for vertex in face["vertices"]:
                    vertex[2] *= 40 / length
            index = str(mesh["bones"].index(bone))
            for clip in mesh["animations"].values():
                for key in clip["tracks"].get(index, {}).get("scale", []):
                    if key[3] > .01: key[3] = 1
        mesh["additive_textures"] = [0]
    else:
        mesh["textures"].append("lyycore:textures/entity/guiding/white_shell_emissive.png")
        mesh["additive_textures"] = [1]
        for bone in mesh["bones"]:
            emission = copy.deepcopy(bone["faces"])
            for face in emission:
                face["texture"] = 1
            bone["faces"].extend(emission)
    if name == "guiding_light":
        # Both source clips start from a closed shell. Blend the second clip's
        # start from the first clip's end, so the shell never snaps shut mid-hatch.
        opening = mesh["animations"]["hatch_absorb"]["tracks"]
        breaking = mesh["animations"]["shell_break"]["tracks"]
        tracks = {}
        for bone in set(opening) | set(breaking):
            channels = {}
            for channel in ("position", "rotation", "scale"):
                default = 1 if channel == "scale" else 0
                first = opening.get(bone, {}).get(channel, [])
                second = breaking.get(bone, {}).get(channel, [])
                values = []
                for tick in range(41):
                    time = tick / 20
                    value = sample(first, min(time, 1) * 2.2, default)
                    if time > 1:
                        blend = min((time - 1) / .15, 1)
                        blend = blend * blend * (3 - 2 * blend)
                        target = sample(second, (time - 1) * 1.2, default)
                        value = [a + (b-a)*blend for a, b in zip(value, target)]
                    values.append([time, *value])
                channels[channel] = values
            tracks[bone] = channels
        mesh["animations"]["hatch_transition"] = {"length": 2, "loop": False, "tracks": tracks}
    helpers["write"](path, mesh)
