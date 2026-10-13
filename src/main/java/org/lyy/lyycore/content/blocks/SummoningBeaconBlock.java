package org.lyy.lyycore.content.blocks;

import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.Tags;
import org.lyy.lyycore.registry.LyyBlockEntities;

/** Material use is the entire operation; there is no inventory or per-tick work. */
public final class SummoningBeaconBlock extends SquareMachineBlock {
    public static final MapCodec<SummoningBeaconBlock> CODEC = simpleCodec(SummoningBeaconBlock::new);
    private static final int COUNT = 16;

    public SummoningBeaconBlock() { this(Properties.of().strength(5).sound(SoundType.AMETHYST)); }
    private SummoningBeaconBlock(Properties properties) { super(properties, "summoning_beacon", 4); }
    @Override protected MapCodec<SummoningBeaconBlock> codec() { return CODEC; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == CENTER ? new Controller(pos, state) : null;
    }

    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
            BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        EntityType<? extends Animal> type = target(stack);
        if (type == null) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!(level instanceof ServerLevel server)) return ItemInteractionResult.SUCCESS;
        var center = center(pos, state);
        if (!(server.getBlockEntity(center) instanceof Controller)) return ItemInteractionResult.FAIL;
        var animals = prepareAnimals(server, center, type);
        if (animals.size() != COUNT) {
            player.displayClientMessage(Component.translatable("message.lyycore.summoning_beacon.no_space"), true);
            return ItemInteractionResult.CONSUME;
        }
        for (var animal : animals) {
            animal.finalizeSpawn(server, server.getCurrentDifficultyAt(animal.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
            animal.setAge(0);
            animal.setPersistenceRequired();
            server.addFreshEntity(animal);
            server.sendParticles(ParticleTypes.END_ROD, animal.getX(), animal.getY() + .5, animal.getZ(), 8, .3, .4, .3, .01);
        }
        if (!player.getAbilities().instabuild) stack.shrink(1);
        server.playSound(null, center, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1, 1);
        return ItemInteractionResult.CONSUME;
    }

    private static EntityType<? extends Animal> target(ItemStack stack) {
        if (stack.is(Items.PORKCHOP)) return EntityType.PIG;
        if (stack.is(Items.BEEF)) return EntityType.COW;
        if (stack.is(Items.MUTTON)) return EntityType.SHEEP;
        if (stack.is(Items.CHICKEN)) return EntityType.CHICKEN;
        return stack.is(Tags.Items.MUSHROOMS) ? EntityType.MOOSHROOM : null;
    }

    private static ArrayList<Animal> prepareAnimals(ServerLevel level, BlockPos center, EntityType<? extends Animal> type) {
        var animals = new ArrayList<Animal>(COUNT);
        // Search outward on nearby solid ground, and reserve all positions before consuming anything.
        for (int radius = 2; radius <= 8 && animals.size() < COUNT; radius++) {
            for (int x = -radius; x <= radius && animals.size() < COUNT; x++) {
                for (int z = -radius; z <= radius && animals.size() < COUNT; z++) {
                    if (Math.max(Math.abs(x), Math.abs(z)) != radius) continue;
                    for (int dy = 4; dy >= -4; dy--) {
                        var pos = center.offset(x, dy, z);
                        if (!level.hasChunkAt(pos) || level.isOutsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos)
                                || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) continue;
                        var animal = type.create(level);
                        if (animal == null) return animals;
                        animal.moveTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5, level.random.nextFloat() * 360, 0);
                        var box = animal.getBoundingBox();
                        if (!level.noCollision(animal, box) || level.containsAnyLiquid(box)
                                || animals.stream().anyMatch(other -> other.getBoundingBox().intersects(box))) continue;
                        animals.add(animal);
                        break;
                    }
                }
            }
        }
        return animals;
    }

    /** The shared multiblock lifecycle uses this center marker when removing all its parts. */
    public static final class Controller extends BlockEntity {
        public Controller(BlockPos pos, BlockState state) { super(LyyBlockEntities.SUMMONING_BEACON.get(), pos, state); }
    }
}
