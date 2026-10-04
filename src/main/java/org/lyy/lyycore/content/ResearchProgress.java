package org.lyy.lyycore.content;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.lyy.lyycore.content.research.ResearchDefinition;
import org.lyy.lyycore.content.skills.SkillSystem;
import org.lyy.lyycore.network.ResearchNetwork;
import org.lyy.lyycore.content.research.ResearchManager;

public final class ResearchProgress {
    private static final String KEY = "lyycore:research";
    private ResearchProgress() { }
    public static boolean completed(Player player, ResourceLocation id) {
        return player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getCompound(KEY).getBoolean(id.toString());
    }
    /** Plan the entire payment before changing anything, including duplicate material costs. */
    private static int[] payment(Player player, ResearchDefinition research) {
        int[] consumed = new int[player.getInventory().items.size()];
        for (ItemStack cost : research.materials()) {
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
        if (!ItemStack.isSameItem(held, cost)) return false;
        for (var entry : cost.getComponentsPatch().entrySet()) {
            if (!java.util.Objects.equals(held.get(entry.getKey()), entry.getValue().orElse(null))) return false;
        }
        return true;
    }
    public static boolean canAfford(Player player, ResearchDefinition research) {
        return hasExperience(player, research) && payment(player, research) != null;
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
        if (completed(player, id) || !hasExperience(player, research)) return false;
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
            if (SkillSystem.unlocked(player) != unlocked) {
                SkillSystem.setUnlocked(player, unlocked);
                ResearchNetwork.sync(player);
            }
        }
        if (!value && id.equals(EnderCompanions.RESEARCH)) EnderCompanions.recall(player);
        return true;
    }
}
