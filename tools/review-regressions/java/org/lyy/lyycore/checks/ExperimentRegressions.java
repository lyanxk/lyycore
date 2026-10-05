package org.lyy.lyycore.checks;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;
import org.lyy.lyycore.content.blockEntities.*;
import org.lyy.lyycore.content.blocks.AlloyCauldronBlock;
import org.lyy.lyycore.content.control.*;
import org.lyy.lyycore.registry.*;

@GameTestHolder("lyycore")
@PrefixGameTestTemplate(false)
public final class ExperimentRegressions {
    @GameTest(template = "empty", batch = "mind_control_execution")
    public static void executionDropsLootAndExperienceWithPlayerCredit(GameTestHelper test) {
        var level = test.getLevel();
        var pos = test.absolutePos(new BlockPos(3, 1, 3));
        level.setBlockAndUpdate(pos, LyyBlocks.MIND_CONTROL_BEACON.get().defaultBlockState());
        var beacon = (MindControlBeaconBlockEntity)level.getBlockEntity(pos);
        var player = ReviewRegressions.player(level);
        beacon.activate(player);
        var immediate = test.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 2));
        var deferred = EntityType.ZOMBIE.create(level);
        deferred.moveTo(test.absolutePos(new BlockPos(4, 2, 4)).getCenter());
        for (var mob : java.util.List.of(immediate, deferred)) {
            mob.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND));
            mob.setDropChance(net.minecraft.world.entity.EquipmentSlot.MAINHAND, 2);
            mob.invulnerableTime = 20;
            MindControl.bind(mob, beacon.binding());
        }
        var sources = new java.util.HashMap<java.util.UUID, net.minecraft.world.damagesource.DamageSource>();
        java.util.function.Consumer<net.neoforged.neoforge.event.entity.living.LivingDeathEvent> listener = event -> {
            if (event.getEntity() == immediate || event.getEntity() == deferred) sources.put(event.getEntity().getUUID(), event.getSource());
        };
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(listener);
        try {
            test.assertTrue(beacon.command(player, 0) && !immediate.isAlive(), "Loaded monster was not executed immediately");
            test.assertTrue(deferred.isAlive(), "Unloaded monster was executed before loading");
            // The fixture's player is absent from the player list, exercising offline-owner attribution too.
            level.addFreshEntity(deferred);
            MindControl.tick(new net.neoforged.neoforge.event.tick.EntityTickEvent.Pre(deferred));
            test.assertFalse(deferred.isAlive(), "Deferred execution did not run on loading");
            for (var mob : java.util.List.of(immediate, deferred)) {
                var source = sources.get(mob.getUUID());
                test.assertTrue(source != null && source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK)
                        && source.getEntity() instanceof net.minecraft.world.entity.player.Player killer && killer.getUUID().equals(player.getUUID()), "Execution lost player damage or owner credit");
            }
            var area = new net.minecraft.world.phys.AABB(pos).inflate(5);
            int diamonds = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, area).stream()
                    .filter(item -> item.getItem().is(Items.DIAMOND)).mapToInt(item -> item.getItem().getCount()).sum();
            test.assertTrue(diamonds >= 2, "Execution suppressed item drops");
            test.assertFalse(level.getEntitiesOfClass(net.minecraft.world.entity.ExperienceOrb.class, area).isEmpty(), "Execution did not spawn experience");
            test.assertTrue(beacon.binding().mobs.isEmpty(), "Execution kept stale bound monsters");
        } finally {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(listener);
            beacon.release();
        }
        test.succeed();
    }
    @GameTest(template = "empty", batch = "mind_control_scan")
    public static void passiveAndActiveScansCollectAllMonstersIndependently(GameTestHelper test) {
        var level = test.getLevel();
        var pos = test.absolutePos(new BlockPos(3, 16, 3));
        level.setBlockAndUpdate(pos, LyyBlocks.MIND_CONTROL_BEACON.get().defaultBlockState());
        var beacon = (MindControlBeaconBlockEntity)level.getBlockEntity(pos);
        var player = ReviewRegressions.player(level);
        test.assertTrue(beacon.activate(player), "Could not activate control beacon");
        var binding = beacon.binding();
        var near = test.spawn(EntityType.ZOMBIE, new BlockPos(2, 17, 2));
        var otherNear = test.spawn(EntityType.SKELETON, new BlockPos(4, 17, 4));
        var far = test.spawn(EntityType.ZOMBIE, new BlockPos(28, 40, 28));
        var phantom = test.spawn(EntityType.PHANTOM, new BlockPos(3, 80, 3));
        var outside = test.spawn(EntityType.ZOMBIE, new BlockPos(3, 90, 3));
        var below = test.spawn(EntityType.ZOMBIE, new BlockPos(3, 8, 3));
        var tooLow = test.spawn(EntityType.ZOMBIE, new BlockPos(3, 3, 3));
        var cow = test.spawn(EntityType.COW, new BlockPos(2, 17, 4));
        for (int i = 0; i < 59; i++) MindControlBeaconBlockEntity.serverTick(level, pos, beacon.getBlockState(), beacon);
        test.assertFalse(binding.mobs.contains(near.getUUID()), "Passive scan ran before three seconds");
        MindControlBeaconBlockEntity.serverTick(level, pos, beacon.getBlockState(), beacon);
        test.assertTrue(binding.mobs.containsAll(java.util.List.of(near.getUUID(), otherNear.getUUID())), "Passive scan did not record all nearby monsters");
        test.assertFalse(binding.mobs.contains(far.getUUID()), "Passive scan used the large volume");
        test.assertTrue(beacon.command(player, 2), "Active control command was rejected");
        test.assertTrue(binding.mobs.containsAll(java.util.List.of(near.getUUID(), otherNear.getUUID(), far.getUUID(), phantom.getUUID(), below.getUUID())), "Active scan was delayed or lost monsters");
        test.assertFalse(binding.mobs.contains(outside.getUUID()) || binding.mobs.contains(tooLow.getUUID()) || binding.mobs.contains(cow.getUUID()), "Scan accepted an out-of-range or passive entity");
        int count = binding.mobs.size();
        beacon.command(player, 2);
        test.assertTrue(binding.mobs.size() == count, "Repeated scan duplicated records");
        beacon.command(player, 4);
        near.setTarget(outside);
        var newcomer = test.spawn(EntityType.ZOMBIE, new BlockPos(4, 17, 2));
        for (int i = 0; i < 60; i++) MindControlBeaconBlockEntity.serverTick(level, pos, beacon.getBlockState(), beacon);
        test.assertTrue(binding.mobs.contains(newcomer.getUUID()) && binding.mobs.contains(far.getUUID()), "Passive scanning stopped after active control or replaced earlier records");
        test.assertTrue(near.getTarget() == outside, "Repeated scan interrupted an existing combat target");
        var saved = MindControlData.get(level).save(new net.minecraft.nbt.CompoundTag(), level.registryAccess());
        var entry = saved.getList("Bindings", net.minecraft.nbt.Tag.TAG_COMPOUND).stream().map(net.minecraft.nbt.CompoundTag.class::cast)
                .filter(tag -> tag.getUUID("Owner").equals(player.getUUID())).findFirst().orElseThrow();
        test.assertTrue(entry.getList("Mobs", net.minecraft.nbt.Tag.TAG_INT_ARRAY).size() == binding.mobs.size(), "Save omitted controlled monsters");
        beacon.release();
        test.succeed();
    }
    @GameTest(template = "empty")
    public static void potionBottlingConsumesAllMaterialsAndKeepsWater(GameTestHelper test) {
        var pos = test.absolutePos(new BlockPos(2, 1, 2));
        test.getLevel().setBlockAndUpdate(pos, LyyBlocks.ALLOY_CAULDRON.get().defaultBlockState());
        var pot = (AlloyCauldronBlockEntity)test.getLevel().getBlockEntity(pos);
        pot.add(new ItemStack(LyyItems.INCOMPLETE_POTION.get()));
        test.assertTrue(pot.ingredients().isEmpty() && pot.liquid() == AlloyCauldronBlock.Liquid.WATER,
                "Empty creative potion was recorded as an ingredient");
        pot.add(new ItemStack(LyyItems.HEART_OF_NOTHINGNESS.get()));
        pot.add(new ItemStack(Items.PHANTOM_MEMBRANE));
        var incomplete = pot.bottle();
        test.assertTrue(incomplete.is(LyyItems.INCOMPLETE_POTION) && pot.ingredients().isEmpty(), "Incomplete batch was not consumed");
        var savedBottle = incomplete.save(test.getLevel().registryAccess());
        pot.add(ItemStack.parseOptional(test.getLevel().registryAccess(), (net.minecraft.nbt.CompoundTag)savedBottle));
        test.assertTrue(pot.ingredients().getOrDefault(LyyItems.HEART_OF_NOTHINGNESS.get(), 0) == 1
                && pot.ingredients().getOrDefault(Items.PHANTOM_MEMBRANE, 0) == 1
                && !pot.ingredients().containsKey(LyyItems.INCOMPLETE_POTION.get()), "Pouring did not restore the saved contents");
        pot.add(new ItemStack(Items.FEATHER, 5));
        pot.add(new ItemStack(Items.COBBLESTONE));
        test.assertTrue(pot.liquid() == AlloyCauldronBlock.Liquid.STRANGE, "Fixture did not contaminate the mixture");
        pot.add(new ItemStack(Items.PHANTOM_MEMBRANE)); pot.add(new ItemStack(Items.DRAGON_EGG));
        test.assertTrue(pot.ingredients().isEmpty() && pot.finishedPotion().is(LyyItems.CONTROL_ENHANCEMENT_POTION),
                "Complete recipe did not override contamination and consume all extra materials immediately");
        var restored = new AlloyCauldronBlockEntity(pos, pot.getBlockState());
        restored.loadWithComponents(pot.saveWithFullMetadata(test.getLevel().registryAccess()), test.getLevel().registryAccess());
        test.assertTrue(restored.finishedPotion().is(LyyItems.CONTROL_ENHANCEMENT_POTION) && restored.ingredients().isEmpty(),
                "Finished potion was lost when saving the cauldron");
        test.assertFalse(pot.add(new ItemStack(Items.FEATHER)), "Finished potion accepted more materials before bottling");
        test.assertTrue(pot.bottle().is(LyyItems.CONTROL_ENHANCEMENT_POTION), "Minimum-count recipe did not accept extra ingredients");
        test.assertTrue(pot.ingredients().isEmpty() && pot.liquid() == AlloyCauldronBlock.Liquid.WATER, "Bottling did not restore water");
        pot.water.drain(10000, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        test.assertTrue(pot.water.getFluidInTank(0).getAmount() == Integer.MAX_VALUE, "Infinite water was depleted");
        pot.add(new ItemStack(Items.SLIME_BLOCK, 64)); pot.add(new ItemStack(Items.LAVA_BUCKET));
        test.assertTrue(pot.bottle().is(LyyItems.ADHESIVE_POTION), "Special building ingredient turned the mixture black");
        pot.add(new ItemStack(Items.DANDELION, 33));
        test.assertTrue(pot.liquid() == AlloyCauldronBlock.Liquid.STRANGE, "Plant overflow did not turn black");
        pot.bottle();
        test.assertTrue(pot.liquid() == AlloyCauldronBlock.Liquid.WATER, "Black bottle did not reset the pot");
        test.succeed();
    }
    @GameTest(template = "empty")
    public static void controlIncludesPhantomsButRejectsPassiveMobsAndBosses(GameTestHelper test) {
        var phantom = test.spawn(EntityType.PHANTOM, new BlockPos(2, 1, 2));
        var cow = test.spawn(EntityType.COW, new BlockPos(3, 1, 2));
        var guardian = LyyEntities.IMAGINARY_GUARDIAN.get().create(test.getLevel());
        test.assertTrue(MindControl.eligible(phantom), "Phantom was not eligible");
        test.assertFalse(MindControl.eligible(cow), "Passive animal was eligible");
        test.assertFalse(MindControl.eligible(guardian), "Boss was eligible");
        var owner = java.util.UUID.randomUUID();
        var saved = MindControlData.get(test.getLevel());
        var binding = saved.activate(owner, GlobalPos.of(test.getLevel().dimension(), test.absolutePos(BlockPos.ZERO)));
        MindControl.bind(phantom, binding);
        test.assertTrue(MindControl.binding(phantom) == binding, "Monster did not bind");
        saved.execute(binding);
        test.assertTrue(binding.mobs.contains(phantom.getUUID()) && saved.execution(phantom.getUUID()) != null,
                "Execution released control before death was confirmed");
        var attempt = saved.beginExecution(phantom.getUUID());
        test.assertTrue(attempt != null && attempt.owner().equals(owner), "Execution lost its issuing owner");
        test.assertTrue(saved.beginExecution(phantom.getUUID()) == null, "Execution was attempted twice");
        saved.finishExecution(attempt, true);
        test.assertTrue(binding.mobs.isEmpty() && saved.execution(phantom.getUUID()) == null, "Successful execution was not acknowledged");
        saved.release(owner, binding.beacon);
        test.succeed();
    }
    @GameTest(template = "empty")
    public static void condensingChargesEveryWorkingTickAndPausesWithoutPower(GameTestHelper test) {
        var pos = test.absolutePos(new BlockPos(3, 1, 3));
        var level = test.getLevel();
        level.setBlockAndUpdate(pos, LyyBlocks.IMAGINARY_CONDENSING_BEACON.get().defaultBlockState());
        var beacon = (ImaginaryCondensingBeaconBlockEntity)level.getBlockEntity(pos);
        beacon.items().setStackInSlot(0, new ItemStack(LyyBlocks.CRYSTAL_BLOCK.get()));
        for (int i = 1; i < 9; i++) beacon.items().setStackInSlot(i, new ItemStack(i % 2 == 1 ? LyyItems.CONTROL_CRYSTAL.get() : Items.DIAMOND));
        beacon.energy().setImaginaryEnergy(40000);
        for (int i = 0; i < 3; i++) AbstractImaginaryCraftingBlockEntity.serverTick(level, pos, beacon.getBlockState(), beacon);
        test.assertTrue(beacon.data().get(0) == 2 && beacon.energy().getImaginaryEnergyStored() == 0, "Per-tick energy did not pause cleanly");
        for (int i = 2; i < 2400; i++) {
            beacon.energy().setImaginaryEnergy(20000);
            AbstractImaginaryCraftingBlockEntity.serverTick(level, pos, beacon.getBlockState(), beacon);
        }
        test.assertTrue(beacon.items().getStackInSlot(0).is(LyyItems.MIND_CONTROL_BEACON), "Sustained energy recipe did not finish");
        test.assertTrue(beacon.energy().getImaginaryEnergyStored() == 0, "Final tick was not charged exactly once");
        test.succeed();
    }
    @GameTest(template = "empty")
    public static void flightPotionScalesTravelWithoutCompoundingVelocity(GameTestHelper test) {
        var normal = test.spawn(EntityType.COW, new BlockPos(1, 2, 1));
        var enhanced = test.spawn(EntityType.COW, new BlockPos(4, 2, 1));
        normal.setNoAi(false); enhanced.setNoAi(false);
        normal.setNoGravity(true); enhanced.setNoGravity(true);
        // Ordinary movement is unchanged; loading the vanilla flag then exercises glide travel.
        normal.setDeltaMovement(.1, 0, 0); enhanced.setDeltaMovement(.1, 0, 0);
        enhanced.addEffect(new MobEffectInstance(LyyEffects.FLIGHT_SPEED, 200));
        double firstX = normal.getX(), secondX = enhanced.getX();
        normal.travel(Vec3.ZERO); enhanced.travel(Vec3.ZERO);
        test.assertTrue(Math.abs((normal.getX()-firstX)-(enhanced.getX()-secondX)) < .00001, "Flight potion changed grounded movement");
        for (var entity : java.util.List.of(normal, enhanced)) {
            var tag = new net.minecraft.nbt.CompoundTag(); entity.saveWithoutId(tag);
            tag.putBoolean("FallFlying", true); entity.load(tag); entity.setDeltaMovement(.1, 0, 0);
            entity.setYRot(0); entity.setXRot(0);
        }
        firstX = normal.getX(); secondX = enhanced.getX();
        normal.travel(Vec3.ZERO); enhanced.travel(Vec3.ZERO);
        test.assertTrue(Math.abs((enhanced.getX()-secondX) / (normal.getX()-firstX) - 1.5) < .00001, "Flight potion did not increase travel by 50 percent: base=" + (normal.getX()-firstX) + ", effect=" + (enhanced.getX()-secondX) + ", gliding=" + enhanced.isFallFlying() + ", hasEffect=" + enhanced.hasEffect(LyyEffects.FLIGHT_SPEED) + ", velocity=" + enhanced.getDeltaMovement());
        test.assertTrue(normal.getDeltaMovement().distanceTo(enhanced.getDeltaMovement()) < .00001, "Flight speed compounded into next tick's velocity");
        test.succeed();
    }
}
