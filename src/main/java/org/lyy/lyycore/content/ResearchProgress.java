package org.lyy.lyycore.content;

import org.lyy.lyycore.content.skills.BasicSkills;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.lyy.lyycore.content.research.ResearchDefinition;
import org.lyy.lyycore.content.skills.SkillSystem;
import org.lyy.lyycore.network.SkillNetwork;
import org.lyy.lyycore.network.WingsNetwork;
import org.lyy.lyycore.content.research.ResearchManager;
import org.lyy.lyycore.content.research.ResearchMaterial;
import java.util.Comparator;

public final class ResearchProgress {
    private static final String KEY = "lyycore:research";
    private ResearchProgress() { }
    public static boolean completed(Player player, ResourceLocation id) {
        return player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getCompound(KEY).getBoolean(id.toString());
    }
    /** Availability is checked by the server; it never scans or consumes the inventory. */
    public static boolean available(Player player, ResearchDefinition research) {
        if (!research.prerequisites().stream().allMatch(id -> completed(player, id))) return false;
        if (research.requiredAdvancement().isEmpty()) return true;
        if (!(player instanceof ServerPlayer server)) return false;
        var advancement = server.server.getAdvancements().get(research.requiredAdvancement().get());
        return advancement != null && server.getAdvancements().getOrStartProgress(advancement).isDone();
    }
    /** Plan the entire payment before changing anything, including duplicate material costs. */
    private static int[] payment(Player player, ResearchDefinition research) {
        int[] consumed = new int[player.getInventory().items.size()];
        // Stream sorting is stable: absent/equal priorities retain data-pack order.
        var materials = research.materials().stream().sorted(Comparator.comparingInt(ResearchMaterial::priority).reversed()).toList();
        for (var material : materials) {
            ItemStack cost = material.stack();
            int remaining = cost.getCount();
            for (int slot = 0; slot < consumed.length && remaining > 0; slot++) {
                ItemStack held = player.getInventory().items.get(slot);
                if (!matchesMaterial(held, cost)) continue;
                int take = Math.min(remaining, held.getCount() - consumed[slot]);
                consumed[slot] += take;
                remaining -= take;
            }
            if (remaining > 0) return null;
        }
        return consumed;
    }
    /** Require the components explicitly declared by a research definition; allow a used or renamed tool. */
    public static boolean matchesMaterial(ItemStack held, ItemStack cost) {
        if (!ItemStack.isSameItem(held, cost) && !(cost.is(net.minecraft.tags.ItemTags.WOOL) && held.is(net.minecraft.tags.ItemTags.WOOL))) {
            String substitute = cost.is(org.lyy.lyycore.registry.LyyItems.IMAGINARY_ALLOY_INGOT.get()) ? "imaginary_alloy"
                    : cost.is(org.lyy.lyycore.registry.LyyItems.ALLOY_BLOCK.get()) ? "imaginary_alloy_blocks" : null;
            if (substitute == null || !held.is(net.minecraft.tags.ItemTags.create(ResourceLocation.fromNamespaceAndPath("lyycore", substitute)))) return false;
        }
        for (var entry : cost.getComponentsPatch().entrySet()) {
            if (!java.util.Objects.equals(held.get(entry.getKey()), entry.getValue().orElse(null))) return false;
        }
        return true;
    }
    public static boolean canAfford(Player player, ResearchDefinition research) {
        return available(player, research) && hasExperience(player, research) && payment(player, research) != null;
    }
    /** Derive spendable XP from the level and bar; commands can leave totalExperience stale. */
    public static int experiencePoints(Player player) {
        long level = player.experienceLevel;
        long base = level <= 16 ? level * level + 6 * level
                : level <= 31 ? (5 * level * level - 81 * level + 720) / 2
                : (9 * level * level - 325 * level + 4440) / 2;
        return (int) Math.min(Integer.MAX_VALUE, base + Math.round(player.experienceProgress * player.getXpNeededForNextLevel()));
    }
    private static boolean hasExperience(Player player, ResearchDefinition research) {
        return player.experienceLevel >= research.experienceLevels() && experiencePoints(player) >= research.experiencePoints();
    }
    public static boolean complete(ServerPlayer player, ResourceLocation id, ResearchDefinition research) {
        if (completed(player, id) || !available(player, research) || !hasExperience(player, research)) return false;
        int[] consumed = payment(player, research);
        if (consumed == null) return false;
        for (int slot = 0; slot < consumed.length; slot++) player.getInventory().items.get(slot).shrink(consumed[slot]);
        if (research.experiencePoints() > 0) player.giveExperiencePoints(-research.experiencePoints());
        else if (research.experienceLevels() > 0) player.giveExperienceLevels(-research.experienceLevels());
        setCompleted(player, id, research, true);
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
        return true;
    }

    /** Change an unlock without charging or refunding its research costs. */
    public static boolean setCompleted(ServerPlayer player, ResourceLocation id, ResearchDefinition research, boolean value) {
        if (completed(player, id) == value) return false;
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        CompoundTag entries = persisted.getCompound(KEY);
        if (value) entries.putBoolean(id.toString(), true);
        else entries.remove(id.toString());
        persisted.put(KEY, entries);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        if (research.unlocksSkills()) {
            boolean unlocked = value || ResearchManager.all(player.level()).stream()
                    .anyMatch(entry -> entry.value().unlocksSkills() && completed(player, entry.id()));
            SkillSystem.setUnlocked(player, unlocked);
        }
        if (!value && id.equals(EnderCompanions.RESEARCH)) EnderCompanions.recall(player);
        if (id.equals(EnderCompanions.EVOLUTION)) {
            if (value) EnderCompanions.evolve(player);
            else EnderCompanions.recall(player);
        }
        if (id.equals(AegisWings.RESEARCH) || id.equals(AegisWings.ENHANCEMENT)) WingsNetwork.sync(player);
        if (id.equals(BasicSkills.RESEARCH)) {
            org.lyy.lyycore.content.skills.GuardSkill.reset(player);
            org.lyy.lyycore.content.skills.BuildingSkills.updateFlight(player);
        }
        if (research.unlocksSkills() || id.equals(BasicSkills.RESEARCH)) SkillNetwork.sync(player);
        if (value && id.equals(ResourceLocation.parse("lyycore:research/adaptive_enhancement"))) {
            var advancement = player.server.getAdvancements().get(ResourceLocation.parse("lyycore:progression/well_fed"));
            if (advancement != null) player.getAdvancements().award(advancement, "research");
        }
        return true;
    }
}
