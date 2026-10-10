package org.lyy.lyycore.content.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import org.lyy.lyycore.content.research.ResearchDefinition;
import org.lyy.lyycore.content.research.ResearchManager;
import org.lyy.lyycore.content.research.ExperimentDefinition;
import org.lyy.lyycore.registry.LyyItems;

import javax.annotation.Nullable;
import java.util.UUID;

public final class ResearchNotesItem extends Item {
    public ResearchNotesItem(Properties properties) { super(properties); }

    public static ItemStack create(ResourceLocation research, UUID owner) {
        ItemStack stack = new ItemStack(LyyItems.RESEARCH_NOTES.get());
        CompoundTag data = new CompoundTag();
        data.putString("Research", research.toString());
        data.putUUID("Owner", owner);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        return stack;
    }

    @Nullable public static ResearchDefinition research(ItemStack stack, Level level) {
        if (!stack.is(LyyItems.RESEARCH_NOTES.get())) return null;
        var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        ResourceLocation id = ResourceLocation.tryParse(data.getString("Research"));
        if (id == null || !data.hasUUID("Owner")) return null;
        var holder = ResearchManager.get(level, id);
        return holder == null ? null : holder.value();
    }

    @Nullable public static UUID owner(ItemStack stack) {
        if (!stack.is(LyyItems.RESEARCH_NOTES.get())) return null;
        var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return data.hasUUID("Owner") ? data.getUUID("Owner") : null;
    }

    @Override public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        // Ownership is deliberately not checked in containers, on the ground, or in other inventory slots.
        if (level.isClientSide || !(entity instanceof Player player) || !(selected || player.getOffhandItem() == stack)) return;
        var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!data.hasUUID("Owner") || !data.getUUID("Owner").equals(player.getUUID())) {
            stack.setCount(0);
            player.getInventory().setChanged();
            player.displayClientMessage(Component.translatable("message.lyycore.foreign_research").withColor(0xED8CBC), true);
        }
    }

    @Nullable public static ExperimentDefinition experiment(ItemStack stack, Level level) {
        var research = research(stack, level);
        return research == null ? null : research.production().flatMap(ResearchDefinition.Production::experiment).orElse(null);
    }
    public static int[] experimentPositions(ItemStack stack, ExperimentDefinition experiment) {
        var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound("Experiment");
        int[] positions = data.getIntArray("Positions");
        return data.getString("Definition").equals(experiment.signature()) && experiment.valid(positions) ? positions : experiment.initial();
    }
    public static void setExperimentPositions(ItemStack stack, ExperimentDefinition experiment, int[] positions) {
        if (!experiment.valid(positions)) return;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, data -> {
            CompoundTag state = new CompoundTag();
            state.putString("Definition", experiment.signature());
            state.putIntArray("Positions", positions);
            state.putBoolean("Complete", experiment.complete(positions));
            data.put("Experiment", state);
        });
    }
    public static boolean experimentComplete(ItemStack stack, ExperimentDefinition experiment) {
        return experiment.complete(experimentPositions(stack, experiment));
    }
    public static boolean readyForProduction(ItemStack stack, ResearchDefinition.Production production) {
        return production.experiment().map(e -> experimentComplete(stack, e)).orElse(true);
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> lines, net.minecraft.world.item.TooltipFlag flag) {
        if (context.level() != null) {
            ResearchDefinition research = research(stack, context.level());
            if (research != null) lines.add(Component.translatable(research.title()).withColor(research.rarity().color));
            var experiment = experiment(stack, context.level());
            if (experiment != null) lines.add(Component.translatable("gui.lyycore.experiment." +
                    (experimentComplete(stack, experiment) ? "complete" : "incomplete")).withColor(0x8CAFD1));
        }
    }
}
