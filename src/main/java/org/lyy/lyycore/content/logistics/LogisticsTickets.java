package org.lyy.lyycore.content.logistics;

import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.world.chunk.*;
import org.lyy.lyycore.content.blockEntities.LogisticsMatrixBlockEntity;

@EventBusSubscriber(modid = "lyycore")
public final class LogisticsTickets {
    public static final TicketController TICKETS = new TicketController(ResourceLocation.parse("lyycore:logistics_matrix"), (level, helper) -> {
        for (var pos : helper.getBlockTickets().keySet()) {
            if (level.getBlockEntity(pos) instanceof LogisticsMatrixBlockEntity) continue;
            helper.removeAllTickets(pos);
            LogisticsChannels.get(level).removeMatrix(GlobalPos.of(level.dimension(), pos));
        }
    });
    @SubscribeEvent public static void register(RegisterTicketControllersEvent event) { event.register(TICKETS); }
    private LogisticsTickets() { }
}
