package org.lyy.lyycore.checks;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.lyy.lyycore.content.ProductionOwnership;
import org.lyy.lyycore.content.blockEntities.ProductionLabBlockEntity;
import org.lyy.lyycore.content.item.ImaginaryReaperItem;
import org.lyy.lyycore.content.item.ResearchNotesItem;
import org.lyy.lyycore.content.item.WeatherBallItem;
import org.lyy.lyycore.content.research.ResearchManager;
import org.lyy.lyycore.registry.LyyBlocks;
import org.lyy.lyycore.registry.LyyItems;

@GameTestHolder("lyycore")
@PrefixGameTestTemplate(false)
public final class ProductionOwnershipRegressions {
    private static ServerPlayer player(ServerLevel level) {
        var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "ownership-test"), false);
        var player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation());
        var connection = new Connection(PacketFlow.SERVERBOUND) {
            @Override public void setListenerForServerboundHandshake(PacketListener listener) { }
        };
        player.connection = new ServerGamePacketListenerImpl(level.getServer(), connection, player, cookie) {
            @Override public void send(Packet<?> packet) { }
            @Override public void send(Packet<?> packet, PacketSendListener listener) { }
        };
        return player;
    }

    @GameTest(template = "empty")
    public static void laboratoryInheritsNotesOwner(GameTestHelper test) {
        var level = test.getLevel();
        var owner = UUID.randomUUID();
        var stranger = UUID.randomUUID();
        int checked = 0;
        for (var entry : ResearchManager.all(level)) {
            if (entry.value().production().isEmpty()) continue;
            var production = entry.value().production().orElseThrow();
            var lab = new ProductionLabBlockEntity(test.absolutePos(BlockPos.ZERO), LyyBlocks.PRODUCTION_LAB.get().defaultBlockState());
            lab.setLevel(level);
            lab.items().setStackInSlot(0, ResearchNotesItem.create(entry.id(), owner));
            // Resume from a save partway through production, with no player operating the lab.
            ProductionLabBlockEntity.serverTick(level, lab.getBlockPos(), lab.getBlockState(), lab);
            var saved = lab.saveWithoutMetadata(level.registryAccess());
            lab.loadWithComponents(saved, level.registryAccess());
            for (int tick = 1; tick < production.duration(); tick++)
                ProductionLabBlockEntity.serverTick(level, lab.getBlockPos(), lab.getBlockState(), lab);
            var result = lab.items().getStackInSlot(0);
            test.assertTrue(result.is(production.result().getItem()) && result.getCount() == production.result().getCount(), "Wrong result: " + entry.id());
            boolean weather = result.getItem() instanceof WeatherBallItem;
            test.assertFalse(ProductionOwnership.isForeign(result, owner), "Result rejects notes owner: " + entry.id());
            test.assertTrue(ProductionOwnership.isForeign(result, stranger) != weather, "Missing binding/weather exception: " + entry.id());
            test.assertFalse(ProductionOwnership.isForeign(production.result(), stranger), "Shared research result was modified");
            lab.loadWithComponents(lab.saveWithoutMetadata(level.registryAccess()), level.registryAccess());
            test.assertTrue(ProductionOwnership.isForeign(lab.items().getStackInSlot(0), stranger) != weather, "Output binding lost on reload");
            checked++;
        }
        test.assertTrue(checked >= 4, "Expected production definitions missing");
        test.succeed();
    }

    @GameTest(template = "empty")
    public static void onlyForeignHeldItemsDisappear(GameTestHelper test) {
        var player = player(test.getLevel());
        var owner = UUID.randomUUID();
        var reaper = new ItemStack(LyyItems.IMAGINARY_REAPER.get());
        ImaginaryReaperItem.setEfficiency(reaper, 64);
        ProductionOwnership.bind(reaper, owner);
        test.assertTrue(ImaginaryReaperItem.efficiency(reaper) == 64, "Binding overwrote reaper settings");
        ImaginaryReaperItem.setAttackDamage(reaper, 20);
        test.assertTrue(ProductionOwnership.isForeign(reaper, player.getUUID()), "Settings overwrote binding");
        player.setItemInHand(InteractionHand.MAIN_HAND, reaper);
        var offhand = new ItemStack(Items.STONE, 16);
        ProductionOwnership.bind(offhand, owner);
        player.setItemInHand(InteractionHand.OFF_HAND, offhand);
        player.getInventory().setItem(9, offhand.copy());
        NeoForge.EVENT_BUS.post(new PlayerTickEvent.Pre(player));
        test.assertTrue(player.getMainHandItem().isEmpty() && player.getOffhandItem().isEmpty(), "Foreign main/offhand stacks survived");
        test.assertTrue(player.getInventory().getItem(9).getCount() == 16, "Non-held inventory stack was destroyed");
        var own = new ItemStack(LyyItems.IMAGINARY_REAPER.get());
        ProductionOwnership.bind(own, player.getUUID());
        player.setItemInHand(InteractionHand.MAIN_HAND, own);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(LyyItems.IMAGINARY_REAPER.get()));
        NeoForge.EVENT_BUS.post(new PlayerTickEvent.Pre(player));
        test.assertFalse(player.getMainHandItem().isEmpty(), "Owner lost own item");
        test.assertFalse(player.getOffhandItem().isEmpty(), "Unbound legacy item was destroyed");
        test.succeed();
    }

    @GameTest(template = "empty")
    public static void foreignActionsBlockedBeforeTick(GameTestHelper test) {
        var player = player(test.getLevel());
        var item = new ItemStack(LyyItems.IMAGINARY_REAPER.get());
        ProductionOwnership.bind(item, UUID.randomUUID());
        player.setItemInHand(InteractionHand.MAIN_HAND, item.copy());
        var use = new PlayerInteractEvent.RightClickItem(player, InteractionHand.MAIN_HAND);
        NeoForge.EVENT_BUS.post(use);
        test.assertTrue(use.isCanceled() && player.getMainHandItem().isEmpty(), "Foreign item used before tick");
        player.setItemInHand(InteractionHand.MAIN_HAND, item.copy());
        var attack = new AttackEntityEvent(player, EntityType.ZOMBIE.create(test.getLevel()));
        NeoForge.EVENT_BUS.post(attack);
        test.assertTrue(attack.isCanceled() && player.getMainHandItem().isEmpty(), "Foreign reaper attacked before tick");
        test.succeed();
    }

    @GameTest(template = "empty")
    public static void weatherBallsRemainTransferable(GameTestHelper test) {
        var player = player(test.getLevel());
        var owner = UUID.randomUUID();
        for (var item : new net.minecraft.world.item.Item[]{LyyItems.STORM_BALL.get(), LyyItems.SUN_BALL.get()}) {
            var ball = new ItemStack(item, 16);
            ProductionOwnership.bind(ball, owner);
            test.assertFalse(ball.has(DataComponents.CUSTOM_DATA), "Weather ball acquired binding");
            // Even an existing tagged weather ball is exempt from hand checks.
            CustomData.update(DataComponents.CUSTOM_DATA, ball, tag -> tag.putUUID("lyycore:production_owner", owner));
            player.setItemInHand(InteractionHand.MAIN_HAND, ball);
            NeoForge.EVENT_BUS.post(new PlayerTickEvent.Pre(player));
            test.assertTrue(player.getMainHandItem().getCount() == 16, "Foreign weather ball was destroyed");
        }
        test.succeed();
    }
}
