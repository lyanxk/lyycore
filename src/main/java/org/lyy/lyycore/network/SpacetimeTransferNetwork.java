package org.lyy.lyycore.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.lyy.lyycore.content.blockEntities.SpacetimeFactoryBlockEntity.Page;
import org.lyy.lyycore.content.menu.SpacetimeFactoryMenu;
import org.lyy.lyycore.content.recipes.*;

import java.util.*;

/** JEI previews and the server use the same counted, component-preserving transfer plan. */
@EventBusSubscriber(modid = "lyycore")
public final class SpacetimeTransferNetwork {
    public record Request(int menu, ResourceLocation recipe, boolean maximum) implements CustomPacketPayload {
        public static final Type<Request> TYPE = new Type<>(ResourceLocation.parse("lyycore:spacetime_transfer"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC = StreamCodec.of((b, p) -> {
            b.writeVarInt(p.menu); b.writeResourceLocation(p.recipe); b.writeBoolean(p.maximum);
        }, b -> new Request(b.readVarInt(), b.readResourceLocation(), b.readBoolean()));
        @Override public Type<Request> type() { return TYPE; }
    }
    public record SelectedPage(int menu, int page) implements CustomPacketPayload {
        public static final Type<SelectedPage> TYPE = new Type<>(ResourceLocation.parse("lyycore:spacetime_transfer_page"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SelectedPage> CODEC = StreamCodec.of((b, p) -> {
            b.writeVarInt(p.menu); b.writeVarInt(p.page);
        }, b -> new SelectedPage(b.readVarInt(), b.readVarInt()));
        @Override public Type<SelectedPage> type() { return TYPE; }
    }
    public enum Error { INVALID, MISSING, INVENTORY_FULL }
    private record Required(int slot, Ingredient ingredient, int count, boolean reusable) { }
    public record Plan(Page page, List<ItemStack> inputs, List<ItemStack> inventory) { }
    public record Result(Plan plan, Error error) { }
    private record Allocation(ItemStack[] inputs, List<ItemStack> inventory) { }

    public static Page page(Recipe<?> recipe) {
        if (recipe instanceof SpacetimeRecipe) return Page.SPACETIME;
        if (recipe instanceof ImaginaryCondensingRecipe) return Page.CONDENSING;
        if (recipe instanceof ImaginaryCraftingRecipe) return Page.CRAFTING;
        if (recipe instanceof ImaginaryAlloyingRecipe) return Page.ALLOYING;
        return null;
    }

    /** Reads snapshots only, including when JEI probes an inactive page. */
    public static Result plan(SpacetimeFactoryMenu menu, Recipe<?> recipe, boolean maximum) {
        Page page = page(recipe);
        if (page == null || menu.slots.size() != SpacetimeFactoryMenu.MACHINE_SLOTS + 36)
            return new Result(null, Error.INVALID);
        var required = requirements(recipe);
        var pool = new ArrayList<ItemStack>(page.inputCount + 36);
        for (int slot = 0; slot < page.inputCount; slot++) pool.add(menu.factory().inputs(page).getStackInSlot(slot).copy());
        for (int slot = SpacetimeFactoryMenu.MACHINE_SLOTS; slot < menu.slots.size(); slot++) pool.add(menu.getSlot(slot).getItem().copy());
        // Constrained ingredients go first so a broad tag cannot steal a later specific ingredient.
        required.sort(Comparator.comparingInt(input -> candidates(pool, input.ingredient).size()));
        boolean[] foundMaterials = {false};
        for (int batches = maximum ? 64 : 1; batches > 0; batches--) {
            var placed = new ItemStack[page.inputCount];
            Arrays.fill(placed, ItemStack.EMPTY);
            var allocation = assign(required, 0, batches, pool, placed, new int[]{16384}, foundMaterials);
            if (allocation == null) continue;
            return new Result(new Plan(page, List.of(allocation.inputs), allocation.inventory), null);
        }
        return new Result(null, foundMaterials[0] ? Error.INVENTORY_FULL : Error.MISSING);
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
                                     ItemStack[] placed, int[] budget, boolean[] foundMaterials) {
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
            if (count > Math.min(64, candidate.getMaxStackSize())) continue;
            int available = pool.stream().filter(stack -> ItemStack.isSameItemSameComponents(stack, candidate))
                    .mapToInt(ItemStack::getCount).sum();
            if (available < count) continue;
            var remaining = new ArrayList<ItemStack>(pool.size());
            int needed = count;
            for (var stack : pool) {
                int taken = ItemStack.isSameItemSameComponents(stack, candidate) ? Math.min(needed, stack.getCount()) : 0;
                remaining.add(stack.copyWithCount(stack.getCount() - taken));
                needed -= taken;
            }
            placed[input.slot] = candidate.copyWithCount(count);
            var result = assign(required, index + 1, batches, remaining, placed, budget, foundMaterials);
            if (result != null) return result;
        }
        return null;
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
            if (!(context.player() instanceof ServerPlayer player)
                    || !(player.containerMenu instanceof SpacetimeFactoryMenu menu)
                    || menu.containerId != request.menu || !menu.stillValid(player) || player.isSpectator()) return;
            var holder = player.level().getRecipeManager().byKey(request.recipe).orElse(null);
            if (holder == null) return;
            var result = plan(menu, holder.value(), request.maximum);
            if (result.error != null) {
                player.displayClientMessage(Component.translatable("jei.lyycore.spacetime.transfer." + result.error.name().toLowerCase(Locale.ROOT)), true);
                return;
            }
            var plan = result.plan;
            // Nothing mutates until the complete plan has passed validation and all leftovers fit.
            for (int slot = 0; slot < plan.page.inputCount; slot++) menu.factory().inputs(plan.page).setStackInSlot(slot, plan.inputs.get(slot));
            for (int slot = 0; slot < 36; slot++) menu.getSlot(SpacetimeFactoryMenu.MACHINE_SLOTS + slot).set(plan.inventory.get(slot));
            menu.selectPage(plan.page.ordinal());
            menu.broadcastChanges();
            PacketDistributor.sendToPlayer(player, new SelectedPage(menu.containerId, plan.page.ordinal()));
        });
        registrar.playToClient(SelectedPage.TYPE, SelectedPage.CODEC, (payload, context) -> {
            if (context.player().containerMenu instanceof SpacetimeFactoryMenu menu && menu.containerId == payload.menu)
                menu.selectPage(payload.page);
        });
    }
    private SpacetimeTransferNetwork() { }
}
