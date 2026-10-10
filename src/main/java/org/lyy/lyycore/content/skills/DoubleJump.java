package org.lyy.lyycore.content.skills;

import java.util.Set;
import java.util.WeakHashMap;
import java.util.Collections;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.lyy.lyycore.content.ResearchProgress;
import org.lyy.lyycore.registry.LyyEffects;

@EventBusSubscriber(modid = "lyycore")
public final class DoubleJump {
    public static final ResourceLocation RESEARCH = ResourceLocation.parse("lyycore:research/double_jump");
    private static final Set<ServerPlayer> USED = Collections.newSetFromMap(new WeakHashMap<>());
    public static boolean available(Player player) {
        return SkillSystem.unlocked(player) && StyleSystem.current(player) == StyleSystem.Style.MOBILITY
                && (player.level().isClientSide ? player.getPersistentData().getBoolean("lyycore:double_jump") : ResearchProgress.completed(player, RESEARCH));
    }
    /** Shared eligibility for input consumption and authoritative server validation. */
    public static boolean canJump(Player player) {
        return available(player) && player.isAlive() && !player.isSpectator()
                && !player.onGround() && !player.isInWaterOrBubble() && !player.isInLava() && !player.onClimbable()
                && !player.isPassenger() && !player.isFallFlying() && !player.getAbilities().flying
                && !player.hasEffect(LyyEffects.CRYSTALLIZATION) && player.containerMenu == player.inventoryMenu;
    }
    public static void jump(ServerPlayer player) {
        if (!canJump(player) || USED.contains(player)) return;
        var hook = org.lyy.lyycore.content.entity.GrappleHook.active(player);
        if (hook != null && hook.attached() && hook.isPullingPlayer() && !hook.isReelingEntity()) return;
        USED.add(player);
        player.jumpFromGround();
        player.fallDistance = 0;
        player.connection.send(new ClientboundSetEntityMotionPacket(player));
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && (player.onGround() || !player.isAlive())) USED.remove(player);
    }
    private DoubleJump() { }
}
