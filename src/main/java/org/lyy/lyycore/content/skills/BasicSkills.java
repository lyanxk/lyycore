package org.lyy.lyycore.content.skills;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.lyy.lyycore.content.ResearchProgress;
import org.lyy.lyycore.network.SkillNetwork;
import org.lyy.lyycore.registry.LyyEffects;

/** Exploration skill registration and lifecycle ordering; each mechanic owns its own state. */
@EventBusSubscriber(modid = "lyycore")
public final class BasicSkills {
    public static final ResourceLocation RESEARCH = ResourceLocation.parse("lyycore:research/exploration");
    private BasicSkills() { }
    public static void register() {
        SkillSystem.register(StyleSystem.Style.MOBILITY, SkillSystem.SPECIAL,
                new SkillSystem.Skill(MovementSkill::available, MovementSkill::cast));
        SkillSystem.register(StyleSystem.Style.TECHNIQUE, SkillSystem.SPECIAL, new SkillSystem.Skill(RESEARCH, GuardSkill::start));
        SkillSystem.register(StyleSystem.Style.OFFENSE, SkillSystem.SPECIAL,
                new SkillSystem.Skill(OffenseSkills.SCOOP_RESEARCH, OffenseSkills::scoop));
    }
    public static boolean available(Player player) {
        return SkillSystem.unlocked(player) && (player.level().isClientSide
                ? player.getPersistentData().getBoolean("lyycore:exploration") : ResearchProgress.completed(player, RESEARCH));
    }
    static boolean active(ServerPlayer player, StyleSystem.Style style) {
        return available(player) && player.isAlive() && !player.isSpectator() && !player.hasEffect(LyyEffects.CRYSTALLIZATION)
                && StyleSystem.current(player) == style;
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        BuildingSkills.updateFlight(player);
        SkillInput.tick(player);
        GuardSkill.tick(player);
        MovementSkill.tick(player);
        SkillNetwork.sync(player);
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        SkillInput.forget(player);
        MovementSkill.forget(player);
        OffenseSkills.forget(player);
        GuardSkill.forget(player);
        SkillSystem.forget(player);
        SkillNetwork.forget(player);
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) {
        SkillInput.clear(); GuardSkill.clear(); MovementSkill.clear(); OffenseSkills.clear(); SkillSystem.clear(); SkillNetwork.clear();
    }
}
