package org.lyy.lyycore.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.lyy.lyycore.content.blockEntities.SpacetimeFactoryBlockEntity.Page;
import org.lyy.lyycore.content.menu.*;
import org.lyy.lyycore.content.recipes.*;

import java.util.*;

/** JEI previews and the server use the same counted, component-preserving transfer plan. */
@EventBusSubscriber(modid = "lyycore")
public final class MachineRecipeTransferNetwork {
    public record Request(int menu, ResourceLocation recipe, boolean maximum) implements CustomPacketPayload {
        public static final Type<Request> TYPE = new Type<>(ResourceLocation.parse("lyycore:machine_recipe_transfer"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC = StreamCodec.of((b, p) -> {
            b.writeVarInt(p.menu); b.writeResourceLocation(p.recipe); b.writeBoolean(p.maximum);
        }, b -> new Request(b.readVarInt(), b.readResourceLocation(), b.readBoolean()));
        @Override public Type<Request> type() { return TYPE; }
    }
    public record SelectedPage(int menu, int page) implements CustomPacketPayload {
        public static final Type<SelectedPage> TYPE = new Type<>(ResourceLocation.parse("lyycore:machine_recipe_transfer_page"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SelectedPage> CODEC = StreamCodec.of((b, p) -> {
            b.writeVarInt(p.menu); b.writeVarInt(p.page);
        }, b -> new SelectedPage(b.readVarInt(), b.readVarInt()));
        @Override public Type<SelectedPage> type() { return TYPE; }
    }
    public enum Error {
        INVALID, MISSING, INVENTORY_FULL, BUSY, CONDENSING_REQUIRED;
        public Component message() {
            return Component.translatable(switch (this) {
                case BUSY -> "jei.lyycore.imaginary_crafting.busy";
                case CONDENSING_REQUIRED -> "jei.lyycore.condensing.required";
                default -> "jei.lyycore.transfer." + name().toLowerCase(Locale.ROOT);
            });
        }
    }
    private record Required(int slot, Ingredient ingredient, int count, boolean reusable) { }
    public record Plan(Page page, List<ItemStack> inputs, List<ItemStack> inventory) { }
    public record Result(Plan plan, Error error, List<Integer> missingSlots) {
        private Result(Plan plan, Error error) { this(plan, error, List.of()); }
    }
    private record Allocation(ItemStack[] inputs, List<ItemStack> inventory) { }
    private record Slots(List<Slot> inputs, List<Slot> inventory, Page page) { }

    private static Slots slots(AbstractContainerMenu menu, Recipe<?> recipe) {
        if (menu instanceof SpacetimeFactoryMenu && page(recipe) != null) {
            var page = page(recipe);
            return new Slots(menu.slots.subList(SpacetimeFactoryMenu.firstInputSlot(page), SpacetimeFactoryMenu.outputSlot(page)),
                    menu.slots.subList(SpacetimeFactoryMenu.MACHINE_SLOTS, menu.slots.size()), page);
        }
        if (menu instanceof ImaginaryCraftingMenu && recipe instanceof OctagonalRecipe)
            return new Slots(menu.slots.subList(0, 9), menu.slots.subList(9, 45), null);
        if (menu instanceof IAFMenu && recipe instanceof ImaginaryAlloyingRecipe)
            return new Slots(menu.slots.subList(0, 3), menu.slots.subList(4, 40), null);
        if (menu instanceof FissionFurnaceMenu && recipe instanceof SmeltingRecipe)
            return new Slots(menu.slots.subList(0, 1), menu.slots.subList(3, 39), null);
        return null;
    }

    public static Page page(Recipe<?> recipe) {
        if (recipe instanceof SpacetimeRecipe) return Page.SPACETIME;
        if (recipe instanceof ImaginaryCondensingRecipe) return Page.CONDENSING;
        if (recipe instanceof ImaginaryCraftingRecipe) return Page.CRAFTING;
        if (recipe instanceof ImaginaryAlloyingRecipe) return Page.ALLOYING;
        return null;
    }

    /** Reads snapshots only, including when JEI probes an inactive page. */
    public static Result plan(AbstractContainerMenu menu, Recipe<?> recipe, boolean maximum) {
        var slots = slots(menu, recipe);
        if (slots == null) return new Result(null, Error.INVALID);
        if (menu instanceof ImaginaryCraftingMenu crafting) {
            if (recipe instanceof ImaginaryCondensingRecipe && !crafting.condensing()) return new Result(null, Error.CONDENSING_REQUIRED);
            if (crafting.progress() != 0) return new Result(null, Error.BUSY);
        }
        var required = requirements(recipe);
        var pool = new ArrayList<ItemStack>(slots.inputs.size() + 36);
        slots.inputs.forEach(slot -> pool.add(slot.getItem().copy()));
        slots.inventory.forEach(slot -> pool.add(slot.getItem().copy()));
        int[] limits = slots.inputs.stream().mapToInt(Slot::getMaxStackSize).toArray();
        // Constrained ingredients go first so a broad tag cannot steal a later specific ingredient.
        required.sort(Comparator.comparingInt(input -> candidates(pool, input.ingredient).size()));
        boolean[] foundMaterials = {false};
        int maxBatches = maximum ? 64 : 1;
        for (var input : required) if (!input.reusable) maxBatches = Math.min(maxBatches, limits[input.slot] / input.count);
        for (int batches = maxBatches; batches > 0; batches--) {
            var placed = new ItemStack[slots.inputs.size()];
            Arrays.fill(placed, ItemStack.EMPTY);
            var allocation = assign(required, 0, batches, pool, placed, limits, new int[]{16384}, foundMaterials);
            if (allocation == null) continue;
            return new Result(new Plan(slots.page, List.of(allocation.inputs), allocation.inventory), null);
        }
        if (foundMaterials[0]) return new Result(null, Error.INVENTORY_FULL);
        var missing = new MissingInputs(required);
        missing.search(required, 0, pool, limits, new ArrayList<>());
        return new Result(null, Error.MISSING, missing.slots.stream().sorted().toList());
    }

    private static List<Required> requirements(Recipe<?> recipe) {
        var required = new ArrayList<Required>();
        if (recipe instanceof SpacetimeRecipe spacetime) {
            for (int i = 0; i < spacetime.inputs().size(); i++) {
                var input = spacetime.inputs().get(i);
                add(required, i, input.ingredient(), input.count(), false);
            }
        } else if (recipe instanceof OctagonalRecipe octagonal) {
            for (int i = 0; i < 9; i++) add(required, i, octagonal.ingredients().get(i), 1, false);
        } else if (recipe instanceof ImaginaryAlloyingRecipe alloying) {
            for (int i = 0; i < alloying.inputs().size(); i++) add(required, i, alloying.inputs().get(i), 1, false);
            add(required, 2, alloying.catalyst(), 1, !alloying.catalystConsumed());
        } else if (recipe instanceof SmeltingRecipe smelting) {
            add(required, 0, smelting.getIngredients().getFirst(), 1, false);
        }
        return required;
    }
    private static void add(List<Required> required, int slot, Ingredient ingredient, int count, boolean reusable) {
        if (!ingredient.isEmpty()) required.add(new Required(slot, ingredient, count, reusable));
    }
    private static List<ItemStack> candidates(List<ItemStack> pool, Ingredient ingredient) {
        var candidates = new ArrayList<ItemStack>();
        for (var stack : pool) {
            if (!stack.isEmpty() && ingredient.test(stack)
                    && candidates.stream().noneMatch(candidate -> ItemStack.isSameItemSameComponents(candidate, stack)))
                candidates.add(stack);
        }
        return candidates;
    }
    private static Allocation assign(List<Required> required, int index, int batches, List<ItemStack> pool,
                                     ItemStack[] placed, int[] limits, int[] budget, boolean[] foundMaterials) {
        if (index == required.size()) {
            foundMaterials[0] = true;
            var inventory = new ArrayList<ItemStack>(pool.subList(placed.length, pool.size()));
            for (int slot = 0; slot < placed.length; slot++) if (!insert(inventory, pool.get(slot))) return null;
            return new Allocation(placed.clone(), inventory);
        }
        if (--budget[0] < 0) return null;
        var input = required.get(index);
        int count = input.count * (input.reusable ? 1 : batches);
        for (var candidate : candidates(pool, input.ingredient)) {
            if (count > Math.min(limits[input.slot], candidate.getMaxStackSize())) continue;
            if (available(pool, candidate) < count) continue;
            var remaining = take(pool, candidate, count);
            placed[input.slot] = candidate.copyWithCount(count);
            var result = assign(required, index + 1, batches, remaining, placed, limits, budget, foundMaterials);
            if (result != null) return result;
        }
        return null;
    }

    /** Find the smallest missing set for one batch, without counting the same items twice. */
    private static final class MissingInputs {
        private List<Integer> slots;
        private int budget = 16384;

        private MissingInputs(List<Required> required) { slots = required.stream().map(Required::slot).toList(); }

        private void search(List<Required> required, int index, List<ItemStack> pool, int[] limits, List<Integer> missing) {
            if (missing.size() >= slots.size() || --budget < 0) return;
            if (index == required.size()) {
                slots = List.copyOf(missing);
                return;
            }
            var input = required.get(index);
            for (var candidate : candidates(pool, input.ingredient)) {
                if (input.count > Math.min(limits[input.slot], candidate.getMaxStackSize()) || available(pool, candidate) < input.count) continue;
                search(required, index + 1, take(pool, candidate, input.count), limits, missing);
            }
            // A missing ingredient must not prevent later, independent ingredients from being checked.
            missing.add(input.slot);
            search(required, index + 1, pool, limits, missing);
            missing.removeLast();
        }
    }

    private static int available(List<ItemStack> pool, ItemStack candidate) {
        return pool.stream().filter(stack -> ItemStack.isSameItemSameComponents(stack, candidate))
                .mapToInt(ItemStack::getCount).sum();
    }

    private static List<ItemStack> take(List<ItemStack> pool, ItemStack candidate, int count) {
        var remaining = new ArrayList<ItemStack>(pool.size());
        for (var stack : pool) {
            int taken = ItemStack.isSameItemSameComponents(stack, candidate) ? Math.min(count, stack.getCount()) : 0;
            remaining.add(stack.copyWithCount(stack.getCount() - taken));
            count -= taken;
        }
        return remaining;
    }
    private static boolean insert(List<ItemStack> inventory, ItemStack remainder) {
        int count = remainder.getCount();
        if (count == 0) return true;
        for (int pass = 0; pass < 2; pass++) {
            for (int slot = 0; slot < inventory.size() && count > 0; slot++) {
                var current = inventory.get(slot);
                if (pass == 0 ? current.isEmpty() || !ItemStack.isSameItemSameComponents(current, remainder) : !current.isEmpty()) continue;
                int moved = Math.min(count, Math.min(64, remainder.getMaxStackSize()) - current.getCount());
                if (moved <= 0) continue;
                inventory.set(slot, remainder.copyWithCount(current.getCount() + moved));
                count -= moved;
            }
        }
        return count == 0;
    }

    @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(Request.TYPE, Request.CODEC, (request, context) -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            var menu = player.containerMenu;
            if (menu.containerId != request.menu || !menu.stillValid(player) || player.isSpectator()) return;
            var holder = player.level().getRecipeManager().byKey(request.recipe).orElse(null);
            if (holder == null) return;
            var result = plan(menu, holder.value(), request.maximum);
            if (result.error != null) {
                player.displayClientMessage(result.error.message(), true);
                return;
            }
            var plan = result.plan;
            // Nothing mutates until the complete plan has passed validation and all leftovers fit.
            var slots = slots(menu, holder.value());
            for (int i = 0; i < slots.inputs.size(); i++) {
                var slot = slots.inputs.get(i);
                var previous = slot.getItem().copy();
                slot.set(plan.inputs.get(i));
                if (!previous.isEmpty()) slot.onTake(player, previous);
            }
            for (int i = 0; i < slots.inventory.size(); i++) slots.inventory.get(i).set(plan.inventory.get(i));
            if (menu instanceof SpacetimeFactoryMenu factory) {
                factory.selectPage(plan.page.ordinal());
                PacketDistributor.sendToPlayer(player, new SelectedPage(menu.containerId, plan.page.ordinal()));
            }
            menu.broadcastChanges();
        });
        registrar.playToClient(SelectedPage.TYPE, SelectedPage.CODEC, (payload, context) -> {
            if (context.player().containerMenu instanceof SpacetimeFactoryMenu menu && menu.containerId == payload.menu)
                menu.selectPage(payload.page);
        });
    }
    private MachineRecipeTransferNetwork() { }
}
