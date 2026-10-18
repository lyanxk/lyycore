"""Bake the authored trial assets into the existing mesh format.

Usage: python tools/spacetime-trial/import_models.py <spatial_cube_creature directory>
Material alpha in the author's core is emission data, not surface transparency.
"""
import copy
import json
import runpy
import shutil
import sys
import math
import bisect
import numpy as np
from pathlib import Path
from PIL import Image, ImageChops

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/lyycore"
SOURCE = Path(sys.argv[1])
helpers = runpy.run_path(str(ROOT / "tools/research-content/import_models.py"))
DEST = ASSETS / "textures/entity/spacetime_trial"
DEST.mkdir(parents=True, exist_ok=True)


def material(albedo, emission=None):
    shutil.copyfile(albedo, DEST / albedo.name)
    result = [f"lyycore:textures/entity/spacetime_trial/{albedo.name}", None]
    if emission:
        # The renderer's additive pass expects colored emission, not a grayscale mask.
        base = Image.open(albedo).convert("RGB")
        mask = Image.open(emission).convert("RGB")
        name = albedo.stem + "_glow.png"
        ImageChops.multiply(base, mask).save(DEST / name)
        result[1] = f"lyycore:textures/entity/spacetime_trial/{name}"
    return result


def bake(source, name, materials):
    helpers["bake_mesh"](source, name, [m[0] for m in materials])
    path = ASSETS / "geometry" / f"{name}.json"
    mesh = json.loads(path.read_text(encoding="utf-8"))
    mesh["additive_textures"] = []
    for original, (_, glow) in enumerate(materials):
        if not glow:
            continue
        index = len(mesh["textures"])
        mesh["textures"].append(glow)
        mesh["additive_textures"].append(index)
        for bone in mesh["bones"]:
            faces = [copy.deepcopy(f) for f in bone["faces"] if f["texture"] == original]
            for face in faces:
                face["texture"] = index
            bone["faces"].extend(faces)
    if name == "trial_ground_spike":
        glass = Image.open(DEST / Path(materials[0][0]).name).convert("RGBA")
        glass.putalpha(150)
        glass.save(DEST / "trial_spike_glass.png")
        mesh["textures"][0] = "lyycore:textures/entity/spacetime_trial/trial_spike_glass.png"
        mesh["translucent_textures"] = [0]
    helpers["write"](path, mesh)


def hitboxes():
    """Sample bounds offline, so server collision follows animation without skinning meshes."""
    mesh = json.loads((ASSETS / "geometry/trial_guide.json").read_text(encoding="utf-8"))
    bones = mesh["bones"]
    vertices = [np.array([v[:3]+[1] for face in b["faces"] for v in face["vertices"]]).T for b in bones]

    def sample(keys, time, fallback):
        if not keys:
            return np.array([fallback]*3, dtype=float)
        high = bisect.bisect_right([k[0] for k in keys], time)
        if high == 0 or high == len(keys):
            return np.array(keys[0 if high == 0 else -1][1:4])
        a, b = keys[high-1], keys[high]
        if len(a)>4 and a[4] == 1:
            return np.array(a[1:4])
        t = (time-a[0])/(b[0]-a[0])
        if len(a)>4 and a[4] == 2 or len(b)>4 and b[4] == 2:
            p0,p1,p2,p3 = (np.array(k[1:4]) for k in [keys[max(0,high-2)], a,b,keys[min(len(keys)-1,high+1)]])
            return .5*(2*p1+(-p0+p2)*t+(2*p0-5*p1+4*p2-p3)*t*t+(-p0+3*p1-3*p2+p3)*t*t*t)
        return np.array(a[1:4])*(1-t)+np.array(b[1:4])*t

    result = {}
    for name in ["idle", "execution", "fold", "depart"]:
        clip = mesh["animations"][name]
        frames = []
        for tick in range(round(clip["length"]*20)+1):
            time = tick/20
            if clip["loop"]: time %= clip["length"]
            transforms, points = [], []
            for i,bone in enumerate(bones):
                track = clip["tracks"].get(str(i), {})
                position = sample(track.get("position"),time,0)
                rotation = sample(track.get("rotation"),time,0)+bone["rotation"]
                scale = sample(track.get("scale"),time,1)
                parent = bone["parent"]
                local = np.identity(4)
                local[:3,3] = (np.array(bone["pivot"])-(np.array(bones[parent]["pivot"]) if parent>=0 else 0)+position)/16
                for axis in [2,1,0]:
                    turn = np.identity(4); a,b = (axis+1)%3,(axis+2)%3
                    c,s = math.cos(math.radians(rotation[axis])),math.sin(math.radians(rotation[axis]))
                    turn[a,a]=turn[b,b]=c; turn[a,b]=-s; turn[b,a]=s
                    local = local @ turn
                local = local @ np.diag([*scale,1])
                world = transforms[parent] @ local if parent>=0 else local
                transforms.append(world)
                if vertices[i].size: points.append((world @ vertices[i])[:3].T)
            values = np.concatenate(points); values[:,1] -= .2875
            frames.append([round(float(v),4) for v in [*values.min(axis=0),*values.max(axis=0)]])
        result[name] = frames
        print(name, "maximum animated span", np.max(np.array(frames)[:,3:]-np.array(frames)[:,:3],axis=0).round(3))
    helpers["write"](ASSETS / "shapes/trial_guide.json", result)


body = json.loads((SOURCE / "source/spatial_cube_creature.bbmodel").read_text(encoding="utf-8"))
bake(body, "trial_guide", [material(SOURCE / "textures/spatial_cube_palette.png"),
                           material(SOURCE / "textures/spatial_core_albedo64.png", SOURCE / "textures/spatial_core_emission64.png")])
hitboxes()
manifest = json.loads((SOURCE / "assets/manifest.json").read_text(encoding="utf-8"))
for entry in manifest["assets"]:
    directory = SOURCE / "assets" / entry["directory"]
    source = json.loads((directory / entry["model"]).read_text(encoding="utf-8"))
    bake(source, "trial_" + entry["id"], [material(directory / m["albedo"], directory / m["emission"]) for m in entry["materials"]])
