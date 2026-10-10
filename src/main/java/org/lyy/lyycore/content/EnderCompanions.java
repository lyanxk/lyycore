package org.lyy.lyycore.content;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.lyy.lyycore.content.entity.EnderCompanion;
import org.lyy.lyycore.registry.LyyEntities;

/** Player-owned lifetime data; an entity is only the current, revocable summoned instance. */
@EventBusSubscriber(modid = "lyycore")
public final class EnderCompanions {
    public static final ResourceLocation RESEARCH = ResourceLocation.parse("lyycore:research/ender_sentry");
    public static final ResourceLocation EVOLUTION = ResourceLocation.parse("lyycore:research/giant_dragon");
    public static final int DAY_TICKS = 24000, MAX_AGE = 10 * DAY_TICKS, EGG_INTERVAL = 1200 * 20;
    private static final String KEY = "lyycore:ender_companion";
    private EnderCompanions() { }

    private static CompoundTag data(Player player) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (!persisted.contains(KEY)) {
            persisted.put(KEY, new CompoundTag());
            player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        }
        return persisted.getCompound(KEY);
    }
    public static boolean evolved(Player player) { return ResearchProgress.completed(player, EVOLUTION); }
    public static org.lyy.lyycore.content.blockEntities.ImaginaryDragonNestBlockEntity boundNest(ServerPlayer player) {
        var tag = data(player);
        if (!tag.getBoolean("Nest")) return null;
        var dimension = ResourceLocation.tryParse(tag.getString("HomeDimension"));
        var level = dimension == null ? null : player.server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
        var pos = BlockPos.of(tag.getLong("Home"));
        return level != null && level.hasChunkAt(pos)
                && level.getBlockEntity(pos) instanceof org.lyy.lyycore.content.blockEntities.ImaginaryDragonNestBlockEntity nest
                && nest.canUse(player) ? nest : null;
    }
    public static int age(Player player) { return evolved(player) ? 20 * DAY_TICKS : Math.clamp(data(player).getInt("Age"), 0, MAX_AGE); }
    public static void evolve(ServerPlayer player) {
        data(player).putInt("Age", 20 * DAY_TICKS);
        data(player).putInt("EggTicks", 0);
        var active = active(player);
        if (active != null) active.setGrowth(age(player));
    }
    public static boolean isNest(Player player, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension, BlockPos pos) {
        var tag = data(player);
        return tag.getBoolean("Nest") && tag.getLong("Home") == pos.asLong() && tag.getString("HomeDimension").equals(dimension.location().toString());
    }
    public static void bindNest(ServerPlayer player, BlockPos pos) {
        var tag = data(player);
        var active = active(player);
        if (!tag.contains("Home") && active != null) {
            tag.putLong("Home", active.sentry().asLong()); tag.putString("HomeDimension", active.level().dimension().location().toString());
        }
        recall(player);
        var dimension = ResourceLocation.tryParse(tag.getString("HomeDimension"));
        var oldLevel = dimension == null ? null : player.server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
        if (oldLevel != null && tag.contains("Home")) {
            var old = BlockPos.of(tag.getLong("Home"));
            if (oldLevel != player.level() || !old.equals(pos)) {
                var state = oldLevel.getBlockState(old);
                boolean ownedNest = oldLevel.getBlockEntity(old) instanceof org.lyy.lyycore.content.blockEntities.ImaginaryDragonNestBlockEntity nest && nest.canUse(player);
                if (state.is(org.lyy.lyycore.registry.LyyBlocks.ENDER_SENTRY.get()) || ownedNest) oldLevel.destroyBlock(old, false);
            }
        }
        tag.putLong("Home", pos.asLong()); tag.putString("HomeDimension", player.level().dimension().location().toString()); tag.putBoolean("Nest", true);
    }
    public static boolean ownsActive(Player player, EnderCompanion dragon) {
        CompoundTag tag = data(player);
        return tag.hasUUID("Active") && tag.getUUID("Active").equals(dragon.getUUID());
    }
    public static EnderCompanion active(ServerPlayer player) {
        CompoundTag tag = data(player);
        if (!tag.hasUUID("Active")) return null;
        ResourceLocation dimension = ResourceLocation.tryParse(tag.getString("Dimension"));
        var level = dimension == null ? null : player.server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
        return level != null && level.getEntity(tag.getUUID("Active")) instanceof EnderCompanion dragon ? dragon : null;
    }
    public static void recall(ServerPlayer player) {
        EnderCompanion dragon = active(player);
        data(player).remove("Active");
        data(player).remove("Dimension");
        if (dragon != null) dragon.discard();
        // An unloaded instance will discard itself when its chunk next ticks.
    }
    public static void toggle(ServerPlayer player, BlockPos sentry) {
        if (!player.isAlive() || player.isSpectator()) return;
        if (evolved(player)) { message(player, "evolved"); return; }
        if (!ResearchProgress.completed(player, RESEARCH)) {
            message(player, "unknown");
            return;
        }
        if (data(player).hasUUID("Active")) { recall(player); message(player, "recalled"); return; }
        EnderCompanion dragon = LyyEntities.ENDER_COMPANION.get().create(player.level());
        if (dragon == null) return;
        dragon.bind(player, sentry);
        if (!placeBesideSentry(dragon, sentry)) { message(player, "blocked"); return; }
        if (!player.serverLevel().addFreshEntity(dragon)) return;
        CompoundTag tag = data(player);
        tag.putUUID("Active", dragon.getUUID());
        tag.putString("Dimension", player.level().dimension().location().toString());
        tag.putLong("Home", sentry.asLong()); tag.putString("HomeDimension", player.level().dimension().location().toString()); tag.putBoolean("Nest", false);
        message(player, "summoned", age(player) / DAY_TICKS);
    }
    private static boolean placeBesideSentry(EnderCompanion dragon, BlockPos sentry) {
        for (int y = 1; y <= 3; y++) for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            Vec3 position = Vec3.atBottomCenterOf(sentry.offset(x, y, z));
            BlockPos pos = BlockPos.containing(position);
            if (!dragon.level().hasChunkAt(pos) || !dragon.level().getWorldBorder().isWithinBounds(pos)) continue;
            dragon.setPos(position);
            if (dragon.level().noCollision(dragon) && dragon.level().getFluidState(pos).isEmpty()) return true;
        }
        return false;
    }
    /** Called only by the valid, ticking summoned entity. No background player or world scans. */
    public static void tick(ServerPlayer player, EnderCompanion dragon) {
        if (evolved(player)) { dragon.setGrowth(20 * DAY_TICKS); return; }
        CompoundTag tag = data(player);
        int previousAge = age(player);
        if (previousAge < MAX_AGE) tag.putInt("Age", previousAge + 1);
        dragon.setGrowth(age(player));
        if (previousAge < MAX_AGE) return;
        int elapsed = Math.clamp(tag.getInt("EggTicks"), 0, EGG_INTERVAL - 1) + 1;
        if (elapsed == EGG_INTERVAL) { dragon.spawnAtLocation(Items.DRAGON_EGG); elapsed = 0; }
        tag.putInt("EggTicks", elapsed);
    }
    public static void setDays(ServerPlayer player, int days) {
        data(player).putInt("Age", Math.clamp(days, 0, 10) * DAY_TICKS);
        data(player).putInt("EggTicks", 0);
        EnderCompanion dragon = active(player);
        if (dragon != null) dragon.setGrowth(age(player));
    }
    private static void message(ServerPlayer player, String key, Object... args) {
        player.displayClientMessage(Component.translatable("message.lyycore.ender_sentry." + key, args), true);
    }
    @SubscribeEvent public static void attributes(EntityAttributeCreationEvent event) {
        event.put(LyyEntities.ENDER_COMPANION.get(), EnderCompanion.attributes().build());
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) recall(player);
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) recall(player);
    }
    @SubscribeEvent public static void changeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) recall(player);
    }
    @SubscribeEvent public static void died(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) recall(player);
    }
    @SubscribeEvent public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("lyy").then(Commands.literal("ender")
                .then(Commands.literal("day").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("days", IntegerArgumentType.integer(0, 10)).executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            int days = IntegerArgumentType.getInteger(context, "days");
                            setDays(player, days);
                            context.getSource().sendSuccess(() -> Component.translatable("message.lyycore.ender_sentry.day", days), false);
                            return days;
                        })))));
    }
}
