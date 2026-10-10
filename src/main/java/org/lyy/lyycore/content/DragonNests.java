package org.lyy.lyycore.content;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.world.chunk.*;
import org.lyy.lyycore.content.blockEntities.ImaginaryDragonNestBlockEntity;

@EventBusSubscriber(modid = "lyycore")
public final class DragonNests {
    public static final TicketController TICKETS = new TicketController(ResourceLocation.parse("lyycore:dragon_nest"), (level, helper) -> {
        for (var pos : helper.getBlockTickets().keySet())
            if (!(level.getBlockEntity(pos) instanceof ImaginaryDragonNestBlockEntity)) helper.removeAllTickets(pos);
    });
    @SubscribeEvent public static void register(RegisterTicketControllersEvent event) { event.register(TICKETS); }
    private DragonNests() { }
}
