"""Bake the supplied bow, beam and three boss bodies without modifying artist sources."""
import copy
import json
import runpy
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/lyycore"
SOURCE = Path("F:/misc/BlockBench")
bake = runpy.run_path(str(ROOT / "tools/research-content/import_models.py"))["bake_mesh"]


def import_model(folder, source_name, name, texture_folder, hidden=(), idle=None, additive=False, emissive=False):
    source = SOURCE / folder
    model = json.loads((source / "source" / f"{source_name}.bbmodel").read_text(encoding="utf-8"))
    if idle:
        clip = copy.deepcopy(next(a for a in model["animations"] if a["name"].endswith("."+idle)))
        clip["name"] = "animation."+name+".idle"
        model["animations"].append(clip)
    model["animations"] = [a for a in model["animations"] if not a["name"].endswith((".display", ".preview_draw_release", ".preview", ".preview_cycle"))]
    paths = []
    for texture in model["textures"]:
        destination = ASSETS / "textures" / texture_folder / texture["name"]
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(source / "textures" / texture["name"], destination)
        paths.append(f"lyycore:textures/{texture_folder}/{texture['name']}")
    bake(model, name, paths)
    path = ASSETS / "geometry" / f"{name}.json"
    mesh = json.loads(path.read_text())
    if additive:
        mesh["additive_textures"] = list(range(len(paths)))
    if emissive:
        texture = source / "textures" / f"{source_name}_emissive.png"
        destination = ASSETS / "textures" / texture_folder / texture.name
        shutil.copyfile(texture, destination)
        index = len(mesh["textures"])
        mesh["textures"].append(f"lyycore:textures/{texture_folder}/{texture.name}")
        mesh["emissive_textures"] = [index]
        for bone in mesh["bones"]:
            extra = copy.deepcopy(bone["faces"])
            for face in extra: face["texture"] = index
            bone["faces"].extend(extra)
    original = copy.deepcopy(mesh)
    hidden_bones = set()
    for i, bone in enumerate(mesh["bones"]):
        if bone["name"] in hidden or bone["parent"] in hidden_bones:
            hidden_bones.add(i); bone["faces"] = []
    path.write_text(json.dumps(mesh, separators=(",", ":")), encoding="utf-8")
    return original


def extract_spell(mesh, name, roots, clip):
    """Keep only the effect bones and their ancestors, with compact animation indices."""
    mesh = copy.deepcopy(mesh)
    visible = set()
    for i, bone in enumerate(mesh["bones"]):
        if bone["name"] in roots or bone["parent"] in visible:
            visible.add(i)
    keep = set(visible)
    for i in visible:
        parent = mesh["bones"][i]["parent"]
        while parent >= 0:
            keep.add(parent); parent = mesh["bones"][parent]["parent"]
    indices = {old: new for new, old in enumerate(sorted(keep))}
    for i in keep:
        bone = mesh["bones"][i]
        bone["parent"] = indices.get(bone["parent"], -1)
        if i not in visible: bone["faces"] = []
    mesh["bones"] = [mesh["bones"][i] for i in sorted(keep)]
    animation = mesh["animations"][clip]
    animation["tracks"] = {str(indices[int(i)]): track for i, track in animation["tracks"].items() if int(i) in keep}
    mesh["animations"] = {clip: animation}
    if name == "life_spell_missile":
        mesh["animations"] = {"idle": {"length": 1, "loop": True, "tracks": {}}}
    else:
        # Art previews place effects along a fixed path. Gameplay centers them on the locked target.
        spike_positions = [(-8, -8), (8, -8), (0, 0), (-8, 8), (8, 8)]
        for i, bone in enumerate(mesh["bones"]):
            if bone["name"].startswith("ground_spike_"):
                x, z = spike_positions[int(bone["name"].rsplit("_", 1)[1])]
                bone["pivot"] = [x, 0, z]
            elif bone["name"] in ("spell_ground_circle", "spell_impact", "spell_sky_circle"):
                bone["pivot"] = [0, 192 if bone["name"] == "spell_sky_circle" else bone["pivot"][1], 0]
                animation["tracks"].get(str(i), {}).pop("position", None)
                if name == "life_spell_meteor_fx":
                    # Ground warning covers the real 5x5 impact area; the sky ring fits the meteor.
                    diameter = 3.2 if bone["name"] == "spell_sky_circle" else 5
                    width = max(v[0] for f in bone["faces"] for v in f["vertices"]) - min(v[0] for f in bone["faces"] for v in f["vertices"])
                    for face in bone["faces"]:
                        for vertex in face["vertices"]:
                            vertex[0] *= diameter / width
                            vertex[2] *= diameter / width
    (ASSETS / "geometry" / f"{name}.json").write_text(json.dumps(mesh, separators=(",", ":")), encoding="utf-8")


def import_bosses():
    import_model("twin_phase_sovereign", "white_gold_knight", "life_defender", "entity/sovereign", hidden=("thrown_sword",), emissive=True)
    import_sword()
    mage = import_model("twin_phase_sovereign", "violet_blue_mage", "life_usurper", "entity/sovereign", idle="display",
                        hidden=("spell_fx", "fx_magic_circle_preview"), emissive=True)
    import_model("twin_phase_sovereign", "white_phase_cocoon", "life_cocoon", "entity/sovereign", idle="dormant")
    import_model("pink_tracking_prism", "pink_tracking_prism", "life_tracking_prism", "entity/sovereign", emissive=True)
    extract_spell(mage, "life_spell_missile", ("spell_projectile",), "cast_projectile")
    extract_spell(mage, "life_spell_spikes", ("spell_spikes", "spell_ground_circle"), "cast_ground_spikes")
    extract_spell(mage, "life_spell_meteor_fx", ("spell_ground_circle", "spell_sky_circle", "spell_impact"), "cast_meteor")


def import_sword():
    folder = SOURCE / "twin_phase_sovereign"
    model = json.loads((folder / "source/knight_thrown_greatsword.bbmodel").read_text(encoding="utf-8"))
    knight = json.loads((folder / "source/white_gold_knight.bbmodel").read_text(encoding="utf-8"))
    # The detached mesh predates the atlas expansion; pixel UVs still refer to the original region.
    for texture, current in zip(model["textures"], knight["textures"]):
        texture["uv_width"], texture["uv_height"] = current["uv_width"], current["uv_height"]
    model["animations"] = [{"name": "animation.defender_sword.idle", "length": 1, "loop": "loop", "animators": {}}]
    paths = ["lyycore:textures/entity/sovereign/white_gold_knight.png"]
    bake(model, "defender_sword", paths)
    path = ASSETS / "geometry/defender_sword.json"
    mesh = json.loads(path.read_text())
    mesh["textures"].append("lyycore:textures/entity/sovereign/white_gold_knight_emissive.png")
    mesh["emissive_textures"] = [1]
    for bone in mesh["bones"]:
        extra = copy.deepcopy(bone["faces"])
        for face in extra:
            face["texture"] = 1
        bone["faces"].extend(extra)
    path.write_text(json.dumps(mesh, separators=(",", ":")), encoding="utf-8")


if __name__ == "__main__":
    import_model("alien_dragon_might", "alien_dragon_might", "dragon_might", "item/dragon_might",
                 hidden=tuple(f"energy_bolt_{i}_preview" for i in range(1, 5)))
    import_model("alien_dragon_might/laser_fx", "alien_dragon_laser", "dragon_laser", "entity/dragon_laser", additive=True)
    import_bosses()
