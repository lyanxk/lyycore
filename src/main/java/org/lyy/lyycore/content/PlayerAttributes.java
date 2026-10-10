package org.lyy.lyycore.content;

import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Persistent allocations; only our own modifiers are replaced, leaving equipment and effects intact. */
@EventBusSubscriber(modid = "lyycore")
public final class PlayerAttributes {
    public static final ResourceLocation RESEARCH = ResourceLocation.parse("lyycore:research/self");
    private static final String KEY = "lyycore:attributes";
    private static final ResourceLocation ALLOCATION = ResourceLocation.parse("lyycore:allocated_points");
    public enum Stat {
        LIFE(Attributes.MAX_HEALTH, 2), ARMOR(Attributes.ARMOR, 2), TOUGHNESS(Attributes.ARMOR_TOUGHNESS, 1), ATTACK(Attributes.ATTACK_DAMAGE, 2);
        public final Holder<Attribute> attribute;
        public final int perPoint;
        Stat(Holder<Attribute> attribute, int perPoint) { this.attribute = attribute; this.perPoint = perPoint; }
    }
    public static boolean unlocked(Player player) { return ResearchProgress.completed(player, RESEARCH); }
    private static CompoundTag data(Player player) { return player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getCompound(KEY); }
    private static void save(Player player, CompoundTag data) {
        var persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        persisted.put(KEY, data); player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
    }
    public static int allocated(Player player, Stat stat) { return Math.max(0, data(player).getInt(stat.name())); }
    public static int remaining(Player player) { return Math.max(0, data(player).getInt("Remaining")); }
    /** Point rewards will call this once their acquisition rules are defined. */
    public static void grant(ServerPlayer player, int amount) {
        if (amount <= 0) return;
        var data = data(player); data.putInt("Remaining", (int)Math.min(Integer.MAX_VALUE, (long)remaining(player) + amount)); save(player, data);
    }
    public static boolean allocate(ServerPlayer player, int index, boolean add) {
        if (!unlocked(player) || index < 0 || index >= Stat.values().length) return false;
        var stat = Stat.values()[index]; int points = allocated(player, stat), remaining = remaining(player);
        if (add ? remaining == 0 || points == Integer.MAX_VALUE : points == 0 || remaining == Integer.MAX_VALUE) return false;
        var data = data(player); data.putInt(stat.name(), points + (add ? 1 : -1));
        data.putInt("Remaining", remaining + (add ? -1 : 1)); save(player, data); update(player); return true;
    }
    public static void update(ServerPlayer player) {
        for (var stat : Stat.values()) {
            double amount = unlocked(player) ? (double)allocated(player, stat) * stat.perPoint : 0;
            setBonus(player, stat.attribute, ALLOCATION, amount, AttributeModifier.Operation.ADD_VALUE);
        }
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }
    /** Future equipment/research bonuses use stable source IDs so refreshes never stack twice. */
    public static void setBonus(ServerPlayer player, Holder<Attribute> attribute, ResourceLocation source,
                                double amount, AttributeModifier.Operation operation) {
        if (!Double.isFinite(amount)) throw new IllegalArgumentException("Attribute bonus must be finite");
        var instance = player.getAttribute(attribute);
        if (instance == null) return;
        instance.removeModifier(source);
        if (amount != 0) instance.addTransientModifier(new AttributeModifier(source, amount, operation));
        if (attribute.equals(Attributes.MAX_HEALTH) && player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) { update((ServerPlayer)event.getEntity()); }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event) { update((ServerPlayer)event.getEntity()); }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) { update((ServerPlayer)event.getEntity()); }
}
