"""Generate the three research entries and item/data resources for the life ritual."""
import json
import shutil
from PIL import Image
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "src/main/resources"
ASSETS = RES / "assets/lyycore"


def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2)+"\n", encoding="utf-8")


def research(name, icon, rarity, prerequisites, xp=0, materials=(), result=None, seconds=300):
    r = dict(icon={"id": icon if ":" in icon else "lyycore:"+icon}, rarity=rarity, experience_points=xp,
             prerequisites=["lyycore:research/"+p for p in prerequisites], materials=list(materials),
             **{key: f"research.lyycore.{name}.{key}" for key in ("title", "summary", "description")})
    if result: r["production"] = {"result": {"id": "lyycore:"+result}, "duration": seconds*20}
    write(RES / "data/lyycore/research" / f"{name}.json", r)


research("in_our_hands", "dragon_might", "special", ("ritual", "giant_dragon"), 6000,
         materials=[{"id": "lyycore:endless_erosion", "count": 1}], result="dragon_might")
research("blazing_pursuit", "minecraft:blaze_powder", "high", ("ritual",), 6000)
research("ritual", "friendly_proof", "high", ("what_now",), materials=[
    {"id": "lyycore:amplification_potion"}, {"id": "lyycore:control_crystal"},
    {"id": "minecraft:leather", "count": 64}, {"id": "minecraft:white_wool", "count": 64}], result="friendly_proof", seconds=30)
for name in ("dragon_laser", "blazing_pursuit"):
    write(RES / "data/lyycore/damage_type" / f"{name}.json", dict(message_id=name, scaling="never", exhaustion=.1))
for tag, value in (("bypasses_armor", "dragon_laser"), ("bypasses_armor", "blazing_pursuit"), ("is_fire", "blazing_pursuit")):
    path = RES / "data/minecraft/tags/damage_type" / f"{tag}.json"
    data = json.loads(path.read_text()) if path.exists() else {"replace": False, "values": []}
    if "lyycore:"+value not in data["values"]: data["values"].append("lyycore:"+value)
    write(path, data)
path = RES / "data/c/tags/entity_type/bosses.json"
data = json.loads(path.read_text())
for name in ("life_defender", "life_cocoon", "life_usurper"):
    if "lyycore:"+name not in data["values"]: data["values"].append("lyycore:"+name)
write(path, data)

write(ASSETS / "models/item/amplification_potion.json", {"parent": "lyycore:item/strange_potion"})
write(ASSETS / "models/item/endless_erosion.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "lyycore:item/endless_erosion"}})
# Geometry is rendered by the animated item renderer. Display transforms keep the -X bow horizontal in the hand.
write(ASSETS / "models/item/dragon_might.json", {"parent": "minecraft:item/handheld", "textures": {"layer0": "lyycore:item/dragon_might/alien_dragon_might"}, "display": {
    "gui": {"rotation": [0, 0, -39.75], "translation": [-1.75, 2.25, 0], "scale": [.35, .35, .35]},
    "firstperson_righthand": {"rotation": [0, -90, 0], "translation": [1, 1, -1], "scale": [.5, .5, .5]},
    "firstperson_lefthand": {"rotation": [0, 90, 0], "translation": [1, 1, -1], "scale": [.5, .5, .5]},
    "thirdperson_righthand": {"rotation": [0, -90, 0], "translation": [0, 2, 0], "scale": [.5, .5, .5]},
    "thirdperson_lefthand": {"rotation": [0, 90, 0], "translation": [0, 2, 0], "scale": [.5, .5, .5]},
    "ground": {"scale": [.3, .3, .3]}, "fixed": {"scale": [.4, .4, .4]}}})

zh = {
"tooltip.lyycore.description.dragon_might": "焚尽一切",
"item.lyycore.dragon_might": "龙", "item.lyycore.amplification_potion": "增幅药剂", "item.lyycore.endless_erosion": "无尽的「侵蚀」",
"entity.lyycore.life_defender": "歌颂生命的捍卫者", "entity.lyycore.life_cocoon": "褪茧", "entity.lyycore.life_usurper": "腐蚀生命的篡夺者", "entity.lyycore.life_spell": "生命法术", "entity.lyycore.magic_beam": "异龙激光",
"effect.lyycore.dragon_fire": "龙火灼烧", "effect.lyycore.life_curse": "生命诅咒",
"gui.lyycore.dragon_control": "龙的状态", "gui.lyycore.dragon.unavailable": "无法查询绑定的龙巢，暂不能切换模式。", "gui.lyycore.dragon.full_power": "真正的力量", "gui.lyycore.dragon.quarter_power": "尚有余力",
"message.lyycore.altar.life_materials": "外围供奉台需要各放 1 个材料：虚钢块 ×4、水晶块 ×4。",
"research.lyycore.in_our_hands.title": "尽在手中", "research.lyycore.in_our_hands.summary": "不仅只在空中翱翔。",
"research.lyycore.in_our_hands.description": "我们的伙伴似乎愿意直接成为我们的力量，如此一来，我们的远程火力也大大加强了。\n龙：拉弓最多蓄力 1 秒，每道激光造成（1+t）⁴×40 魔法伤害，满蓄力 640 点，射程 64 格。进攻风格发射四连激光；技巧风格拉弓期间额外减少 50% 伤害。龙处于收起模式时，命中附加 5 秒龙火灼烧，最多 3 级，每秒造成 50/100/400 点魔法伤害。查询到龙巢但未收起时，激光仅有 1/4 伤害且无灼烧；无法查询到龙巢时为基础伤害的 10 倍，满蓄力 6400，无灼烧。潜行右键打开龙模式界面。",
"research.lyycore.blazing_pursuit.title": "炽热追击", "research.lyycore.blazing_pursuit.summary": "燃起来了。",
"research.lyycore.blazing_pursuit.description": "我们的羽翼足以划破长空，亦能灼烧敌人。\n进攻风格的追击改为单次 640 点火焰伤害，替换原有羽毛多次物理攻击，保留 1 秒内置冷却。",
"research.lyycore.ritual.title": "仪式", "research.lyycore.ritual.summary": "更多用法。",
"research.lyycore.ritual.description": "我们制作仪式祭坛可不只是为了恢复我们与那虚幻的世界的联系，让我们来探索一下仪式是否能召唤出更多的东西吧。\n将未熄的「欲望」放入合金锅，可制作增幅药剂。研究材料中的羊毛接受任意颜色。\n中心仪式祭坛放置仪式帽，外围八个供奉台分别放置虚钢块 ×4、水晶块 ×4，每台 1 个。潜行右键启动仪式。三阶段首领会将参战者限制在 25×15×25 场地内，每 45 秒叠加一层生命诅咒。死亡次数大于初始参战人数时首领离场。\n击败最终阶段后获得无尽的「侵蚀」与 3000 经验；若清除诅咒前有场内玩家最大生命值不超过 8，额外掉落往世的飞花·爱之诗。",
"death.attack.dragon_laser": "%1$s 被龙火吞没了", "death.attack.dragon_laser.player": "%1$s 被 %2$s 的龙火吞没了", "death.attack.blazing_pursuit": "%1$s 在炽热追击中化为灰烬", "death.attack.blazing_pursuit.player": "%1$s 被 %2$s 的炽热追击吞没了"
}
en = {
"tooltip.lyycore.description.dragon_might": "Burn everything to ashes",
"item.lyycore.dragon_might": "Dragon", "item.lyycore.amplification_potion": "Amplification Potion", "item.lyycore.endless_erosion": "Endless Erosion",
"entity.lyycore.life_defender": "Defender Who Praises Life", "entity.lyycore.life_cocoon": "Shedding Cocoon", "entity.lyycore.life_usurper": "Usurper Who Corrodes Life", "entity.lyycore.life_spell": "Life Spell", "entity.lyycore.magic_beam": "Dragon Laser",
"effect.lyycore.dragon_fire": "Dragonfire Burn", "effect.lyycore.life_curse": "Curse of Life",
"gui.lyycore.dragon_control": "Dragon's State", "gui.lyycore.dragon.unavailable": "The bound nest cannot be found. Mode controls are unavailable.", "gui.lyycore.dragon.full_power": "True power", "gui.lyycore.dragon.quarter_power": "Power to spare",
"message.lyycore.altar.life_materials": "Offer 4 Imaginary Steel Blocks and 4 Crystal Blocks, one on each outer offering stand.",
"research.lyycore.in_our_hands.title": "In Our Hands", "research.lyycore.in_our_hands.summary": "More than soaring through the sky.",
"research.lyycore.in_our_hands.description": "Our companion is willing to become our strength. Charge for up to 1 second: each laser deals (1+t)^4 x 40 magic damage, up to 640, with a 64-block range. Offense fires four lasers; Technique adds 50% damage reduction while drawing. With the dragon stowed, hits apply a 5-second burn, stacking up to three levels for 50/100/400 magic damage per second. With a queryable nest but an unstowed dragon, damage is quartered without the burn. With no queryable nest, damage is multiplied by ten (6400 at full charge), without the burn. Sneak-use opens the remote dragon controls.",
"research.lyycore.blazing_pursuit.title": "Blazing Pursuit", "research.lyycore.blazing_pursuit.summary": "Burn bright.",
"research.lyycore.blazing_pursuit.description": "Our wings can cut through the sky and burn our enemies. Replaces Offense's feather follow-up with one 640-point fire hit, retaining the one-second internal cooldown.",
"research.lyycore.ritual.title": "Ritual", "research.lyycore.ritual.summary": "More possibilities.",
"research.lyycore.ritual.description": "Explore what else the altar can summon. Brew Unextinguished Desire in the alloy cauldron for Amplification Potion. Any wool color is accepted for research. Place a Ritual Hat on the central ritual altar, and four Imaginary Steel Blocks plus four Crystal Blocks on the eight outer offering stands. Sneak-use to begin a three-phase battle in a 25 x 15 x 25 arena. Curse of Life stacks every 45 seconds. More deaths than the initial participant count ends the encounter. Victory grants Endless Erosion and 3000 XP. If any present player's maximum health is at most 8 before curses are cleared, also receive the Sonnet bow.",
"death.attack.dragon_laser": "%1$s was consumed by dragonfire", "death.attack.dragon_laser.player": "%1$s was consumed by %2$s's dragonfire", "death.attack.blazing_pursuit": "%1$s burned in blazing pursuit", "death.attack.blazing_pursuit.player": "%1$s was consumed by %2$s's blazing pursuit"
}
for locale, values in (("zh_cn", zh), ("en_us", en)):
    path = ASSETS / "lang" / f"{locale}.json"
    data = json.loads(path.read_text(encoding="utf-8")); data.update(values); write(path, data)

# Item art is generated separately and preserved intact; effect icons reuse this crystal motif.
art = Path("C:/Users/elysia/.codex/generated_images/01a0fba1-4cf7-7100-b2ff-0d50d4993dcf/exec-3810a5d1-adba-4bd3-acea-b7c0834ed934.png")
if art.exists():
    Image.open(art).convert("RGBA").resize((64, 64), Image.Resampling.NEAREST).save(ASSETS / "textures/item/endless_erosion.png")
for name in ("dragon_fire", "life_curse"):
    dest = ASSETS / "textures/mob_effect" / f"{name}.png"
    dest.parent.mkdir(parents=True, exist_ok=True)
    Image.open(ASSETS / "textures/item/endless_erosion.png").resize((32, 32), Image.Resampling.NEAREST).save(dest)
