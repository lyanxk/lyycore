package org.lyy.lyycore.content.fishing;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.lyy.lyycore.registry.LyyEntities;

/** Vanilla bite timers, open-water rules, loot, XP and fishing events are retained. */
public final class VoidFishingHook extends FishingHook {
    private static final EntityDataAccessor<Boolean> OFF_HAND = SynchedEntityData.defineId(VoidFishingHook.class, EntityDataSerializers.BOOLEAN);

    public VoidFishingHook(EntityType<? extends VoidFishingHook> type, Level level) { super(type, level); }
    public VoidFishingHook(ServerLevel level, int luck, int lure) { super(LyyEntities.VOID_FISHING_HOOK.get(), level, luck, lure); }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OFF_HAND, false);
    }

    public InteractionHand castHand() { return entityData.get(OFF_HAND) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND; }

    @Override protected void updateOwnerInfo(FishingHook hook) {
        // Keep Projectile's owner, but never write or clear the vanilla single-hook reference.
    }

    public void launch(ServerPlayer player, InteractionHand hand, float yawOffset) {
        setOwner(player);
        entityData.set(OFF_HAND, hand == InteractionHand.OFF_HAND);
        float yaw = player.getYRot() + yawOffset;
        float pitch = player.getXRot();
        float cosYaw = Mth.cos(-yaw * Mth.DEG_TO_RAD - Mth.PI);
        float sinYaw = Mth.sin(-yaw * Mth.DEG_TO_RAD - Mth.PI);
        float cosPitch = -Mth.cos(-pitch * Mth.DEG_TO_RAD);
        float sinPitch = Mth.sin(-pitch * Mth.DEG_TO_RAD);
        moveTo(player.getX() - sinYaw * 0.3, player.getEyeY(), player.getZ() - cosYaw * 0.3, yaw, pitch);
        Vec3 direction = new Vec3(-sinYaw, Mth.clamp(-sinPitch / cosPitch, -5, 5), -cosYaw);
        double speed = 0.6 / direction.length();
        Vec3 velocity = direction.multiply(speed + random.triangle(0.5, 0.0103365),
                speed + random.triangle(0.5, 0.0103365), speed + random.triangle(0.5, 0.0103365));
        setDeltaMovement(velocity);
        setYRot((float) (Mth.atan2(velocity.x, velocity.z) * Mth.RAD_TO_DEG));
        setXRot((float) (Mth.atan2(velocity.y, velocity.horizontalDistance()) * Mth.RAD_TO_DEG));
        yRotO = getYRot();
        xRotO = getXRot();
    }

    @Override public void tick() {
        if (!level().isClientSide && VoidFishing.rodFor(this).isEmpty()) { discard(); return; }
        super.tick();
        if (level().isClientSide || isRemoved() || nibble <= 0) return;
        var rod = VoidFishing.rodFor(this);
        if (rod.isEmpty()) { discard(); return; }
        retrieve(rod); // One vanilla harvest, then discard. The session replaces only this slot next tick.
        playSound(SoundEvents.FISHING_BOBBER_RETRIEVE, 0.25F, 1);
        if (getOwner() != null) getOwner().gameEvent(GameEvent.ITEM_INTERACT_FINISH);
    }
}
