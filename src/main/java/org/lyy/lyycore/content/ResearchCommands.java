package org.lyy.lyycore.content;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lyy.lyycore.content.research.ResearchEntry;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.lyy.lyycore.content.menu.ResearchMenu;
import org.lyy.lyycore.content.research.ResearchManager;

@EventBusSubscriber(modid = "lyycore")
public final class ResearchCommands {
    private static final DynamicCommandExceptionType UNKNOWN = new DynamicCommandExceptionType(
            id -> Component.translatable("commands.lyycore.research.unknown", id.toString()));
    private static final DynamicCommandExceptionType ALREADY_COMPLETED = new DynamicCommandExceptionType(
            id -> Component.translatable("commands.lyycore.research.already_completed", id.toString()));
    private static final DynamicCommandExceptionType NOT_COMPLETED = new DynamicCommandExceptionType(
            id -> Component.translatable("commands.lyycore.research.not_completed", id.toString()));

    private ResearchCommands() { }

    @SubscribeEvent public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("lyy")
                .then(Commands.literal("research").requires(source -> source.hasPermission(2))
                        .then(action("get", true))
                        .then(action("forget", false))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> action(String name, boolean completed) {
        return Commands.literal(name)
                .then(Commands.literal("all").executes(context -> changeAll(context.getSource(), completed)))
                .then(Commands.argument("research", ResourceLocationArgument.id())
                .suggests((context, builder) -> {
                    var player = context.getSource().getPlayerOrException();
                    return SharedSuggestionProvider.suggestResource(ResearchManager.all(player.level()).stream()
                            .map(ResearchEntry::id)
                            .filter(id -> ResearchProgress.completed(player, id) != completed), builder);
                })
                .executes(context -> change(context.getSource(), ResourceLocationArgument.getId(context, "research"), completed)));
    }

    private static int changeAll(CommandSourceStack source, boolean completed) throws CommandSyntaxException {
        var player = source.getPlayerOrException();
        int changed = 0;
        for (var entry : ResearchManager.all(player.level())) {
            if (ResearchProgress.setCompleted(player, entry.id(), entry.value(), completed)) changed++;
        }
        if (changed > 0 && player.containerMenu instanceof ResearchMenu) player.closeContainer();
        int count = changed;
        source.sendSuccess(() -> Component.translatable("commands.lyycore.research." + (completed ? "get_all" : "forget_all"),
                count), true);
        return changed;
    }

    private static int change(CommandSourceStack source, ResourceLocation id, boolean completed) throws CommandSyntaxException {
        var player = source.getPlayerOrException();
        var entry = ResearchManager.get(player.level(), id);
        if (entry == null) throw UNKNOWN.create(id);
        var research = entry.value();
        if (!ResearchProgress.setCompleted(player, id, research, completed))
            throw (completed ? ALREADY_COMPLETED : NOT_COMPLETED).create(id);

        // Open research menus contain a fixed list of IDs; reopen them to get the new list.
        if (player.containerMenu instanceof ResearchMenu) player.closeContainer();
        source.sendSuccess(() -> Component.translatable("commands.lyycore.research." + (completed ? "get" : "forget"),
                Component.translatable(research.title()), id.toString()), true);
        return 1;
    }
}
