package org.lyy.lyycore.content.item;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.lyy.lyycore.registry.LyyBlockEntities;

@EventBusSubscriber(modid = "lyycore")
public final class CaptureCageItem extends BlockItem {
    public CaptureCageItem(Block block, Properties properties) { super(block, properties); }

    @SubscribeEvent public static void capture(PlayerInteractEvent.EntityInteract event) {
        var stack = event.getItemStack();
        if (!(stack.getItem() instanceof CaptureCageItem)) return;
        if (!(event.getTarget() instanceof Mob mob) || !mob.isAlive() || mob.getType() == EntityType.ENDER_DRAGON
                || !BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).getNamespace().equals("minecraft")) return;
        if (event.getEntity().isSpectator()) return;
        if (event.getEntity() instanceof ServerPlayer player) {
            var data = stack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY).copyTag();
            var captured = new CompoundTag();
            captured.putString("id", BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString());
            // Keep appearance/age variants used by vanilla loot predicates, never inventory, AI or passengers.
            var saved = mob.saveWithoutId(new CompoundTag());
            for (String key : List.of("Age", "Color", "Sheared", "Variant", "Type", "Size", "RabbitType", "IsBaby", "ChestedHorse"))
                if (saved.contains(key)) captured.put(key, saved.get(key).copy());
            data.put("Captured", captured);
            BlockItem.setBlockEntityData(stack, LyyBlockEntities.CAPTURE_CAGE.get(), data);
            mob.discard();
            player.displayClientMessage(Component.translatable("message.lyycore.captured", mob.getType().getDescription()), true);
        }
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        event.setCanceled(true);
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        var data = stack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY).copyTag();
        EntityType.byString(data.getCompound("Captured").getString("id")).ifPresent(type -> lines.add(type.getDescription()));
        lines.add(Component.translatable("tooltip.lyycore.capture_energy", String.format(java.util.Locale.ROOT, "%,d", data.getInt("Energy"))));
    }
}
