package org.lyy.lyycore.content.blockEntities;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.lyy.lyycore.content.blocks.AlloyCauldronBlock;
import org.lyy.lyycore.content.cauldron.CauldronMixes;
import org.lyy.lyycore.content.cauldron.IncompletePotionContents;
import org.lyy.lyycore.registry.*;

public final class AlloyCauldronBlockEntity extends BlockEntity {
    private final LinkedHashMap<Item, Integer> ingredients = new LinkedHashMap<>();
    private boolean black;
    private ItemStack finishedPotion = ItemStack.EMPTY;
    public AlloyCauldronBlockEntity(BlockPos pos, BlockState state) { super(LyyBlockEntities.ALLOY_CAULDRON.get(), pos, state); }
    public Map<Item, Integer> ingredients() { return Collections.unmodifiableMap(ingredients); }
    public ItemStack finishedPotion() { return finishedPotion.copy(); }
    public final IFluidHandler water = new IFluidHandler() {
        @Override public int getTanks() { return 1; }
        @Override public FluidStack getFluidInTank(int tank) { return tank == 0 ? new FluidStack(Fluids.WATER, Integer.MAX_VALUE) : FluidStack.EMPTY; }
        @Override public int getTankCapacity(int tank) { return tank == 0 ? Integer.MAX_VALUE : 0; }
        @Override public boolean isFluidValid(int tank, FluidStack stack) { return tank == 0 && stack.is(Fluids.WATER); }
        @Override public int fill(FluidStack stack, FluidAction action) { return 0; }
        @Override public FluidStack drain(FluidStack stack, FluidAction action) { return stack.is(Fluids.WATER) ? drain(stack.getAmount(), action) : FluidStack.EMPTY; }
        @Override public FluidStack drain(int amount, FluidAction action) { return amount > 0 ? new FluidStack(Fluids.WATER, amount) : FluidStack.EMPTY; }
    };
    public boolean add(ItemStack stack) {
        if (stack.isEmpty() || !finishedPotion.isEmpty()) return false;
        if (stack.is(LyyItems.INCOMPLETE_POTION)) {
            var stored = IncompletePotionContents.read(stack);
            if (stored.isEmpty()) return true;
            // Restore the whole bottle before matching, including all its extra materials.
            stored.forEach(this::addIngredient);
        } else addIngredient(stack);
        completeRecipe();
        changed();
        return true;
    }
    private void addIngredient(ItemStack stack) {
        Item item = stack.getItem();
        boolean special = CauldronMixes.specialIngredient(item);
        if (CauldronMixes.brewingModifier(item) && !special) return;
        boolean plant = CauldronMixes.plant(item);
        if (item instanceof BlockItem && !special && !plant) black = true;
        else if (ingredients.containsKey(item) || ingredients.size() < 10) {
            ingredients.merge(item, stack.getCount(), (a, b) -> (int)Math.min(Integer.MAX_VALUE, (long)a + b));
        }
        if (plantCount() > 32 || effects().size() > 6) black = true;
    }
    private void completeRecipe() {
        for (var recipe : CauldronMixes.specials()) {
            if (!recipe.matches(ingredients)) continue;
            finishedPotion = new ItemStack(recipe.result());
            ingredients.clear();
            black = false;
            return;
        }
    }
    private long plantCount() {
        return ingredients.entrySet().stream().filter(entry -> CauldronMixes.plant(entry.getKey()))
                .mapToLong(Map.Entry::getValue).sum();
    }
    private List<Holder<MobEffect>> effects() {
        return ingredients.keySet().stream().map(CauldronMixes::brewingEffect).filter(Objects::nonNull).distinct().toList();
    }
    public AlloyCauldronBlock.Liquid liquid() {
        if (!finishedPotion.isEmpty()) return AlloyCauldronBlock.Liquid.PINK;
        if (black) return AlloyCauldronBlock.Liquid.STRANGE;
        if (ingredients.isEmpty()) return AlloyCauldronBlock.Liquid.WATER;
        // A partial special mixture stays pink even when one ingredient also has a brewing effect.
        if (ingredients.keySet().stream().anyMatch(item -> CauldronMixes.specialIngredient(item) && CauldronMixes.brewingEffect(item) == null))
            return AlloyCauldronBlock.Liquid.PINK;
        if (!effects().isEmpty()) return AlloyCauldronBlock.Liquid.BLUE;
        if (plantCount() > 0) return AlloyCauldronBlock.Liquid.SILVER;
        return AlloyCauldronBlock.Liquid.PINK;
    }
    /** One bottle consumes the entire mixture; infinite water is independent of the mixture. */
    public ItemStack bottle() {
        if (!finishedPotion.isEmpty()) {
            var result = finishedPotion.copy();
            clear(false);
            return result;
        }
        ItemStack result;
        var effects = new ArrayList<MobEffectInstance>();
        switch (liquid()) {
            case WATER -> result = PotionContents.createItemStack(Items.POTION, Potions.WATER);
            case PINK -> result = new ItemStack(LyyItems.INCOMPLETE_POTION.get());
            case BLUE -> {
                result = new ItemStack(LyyItems.BLUE_POTION.get());
                for (var effect : effects()) effects.add(new MobEffectInstance(effect, 36000));
            }
            case SILVER -> {
                result = new ItemStack(LyyItems.SILVER_POTION.get());
                var candidates = new ArrayList<>(BuiltInRegistries.MOB_EFFECT.holders().filter(holder -> holder.key().location().getNamespace().equals("minecraft")).toList());
                int count = 1 + level.random.nextInt(3);
                for (int i = 0; i < count && !candidates.isEmpty(); i++) effects.add(new MobEffectInstance(candidates.remove(level.random.nextInt(candidates.size())), 1200));
            }
            default -> {
                result = new ItemStack(LyyItems.STRANGE_POTION.get());
                var choices = List.of(LyyEffects.NETHER_VISION, LyyEffects.STOMACH_THUNDER, LyyEffects.SPATIAL_CONFUSION, LyyEffects.WORLD_DESTRUCTION);
                effects.add(new MobEffectInstance(choices.get(level.random.nextInt(choices.size())), 300));
            }
        }
        if (!effects.isEmpty()) result.set(DataComponents.POTION_CONTENTS, new PotionContents(Optional.empty(), Optional.empty(), effects));
        if (result.is(LyyItems.INCOMPLETE_POTION)) {
            IncompletePotionContents.write(result, ingredients);
        }
        clear(false);
        return result;
    }
    public void clear(boolean explode) {
        boolean wasBlack = black;
        black = false; ingredients.clear(); finishedPotion = ItemStack.EMPTY; changed();
        if (explode && wasBlack && level != null && !level.isClientSide)
            level.explode(null, worldPosition.getX() + .5, worldPosition.getY() + .5, worldPosition.getZ() + .5, 1, Level.ExplosionInteraction.NONE);
    }
    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.setBlock(worldPosition, getBlockState().setValue(AlloyCauldronBlock.LIQUID, liquid()), 3);
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        var contents = new CompoundTag();
        ingredients.forEach((item, count) -> contents.putInt(BuiltInRegistries.ITEM.getKey(item).toString(), count));
        tag.put("Ingredients", contents); tag.putBoolean("Black", black);
        if (!finishedPotion.isEmpty()) tag.put("FinishedPotion", finishedPotion.save(registries));
        var order = new ListTag(); ingredients.keySet().forEach(item -> order.add(StringTag.valueOf(BuiltInRegistries.ITEM.getKey(item).toString())));
        tag.put("Order", order);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries); ingredients.clear();
        var contents = tag.getCompound("Ingredients");
        // Preserve insertion order explicitly across save/load for the ten-kind limit and display.
        var order = tag.getList("Order", Tag.TAG_STRING);
        var keys = order.isEmpty() ? contents.getAllKeys().stream().toList() : order.stream().map(Tag::getAsString).toList();
        for (var key : keys) {
            var id = ResourceLocation.tryParse(key);
            if (id == null || !BuiltInRegistries.ITEM.containsKey(id) || contents.getInt(key) <= 0 || ingredients.size() == 10) continue;
            ingredients.put(BuiltInRegistries.ITEM.get(id), contents.getInt(key));
        }
        black = tag.getBoolean("Black");
        finishedPotion = ItemStack.parseOptional(registries, tag.getCompound("FinishedPotion"));
        // Existing saves may already contain a complete recipe that was waiting for bottling.
        if (finishedPotion.isEmpty()) completeRecipe();
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
