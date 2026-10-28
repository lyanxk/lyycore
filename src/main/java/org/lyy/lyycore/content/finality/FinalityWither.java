package org.lyy.lyycore.content.finality;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.level.Level;

/** Vanilla AI and attacks; the encounter owns presentation, rewards and lifetime. */
public final class FinalityWither extends WitherBoss {
    public FinalityWither(EntityType<? extends WitherBoss> type, Level level) {
        super(type, level);
        xpReward = 0;
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {}

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {}

    @Override
    public void dropAllDeathLoot(ServerLevel level, DamageSource source) {}

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel level && !FinalityTrials.get(level).active()) discard();
    }
}
