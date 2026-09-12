package org.lyy.lyycore;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import org.lyy.lyycore.gametest.LyyGameTests;
import org.lyy.lyycore.gametest.SonnetGameTests;
import org.lyy.lyycore.registry.LyyCapabilities;
import org.lyy.lyycore.registry.LyyRegistries;
import org.slf4j.Logger;

@Mod(LyyCore.MODID)
public class LyyCore {
    public static final String MODID = "lyycore";
    public static final Logger LOGGER = LogUtils.getLogger();

    public LyyCore(IEventBus modEventBus, ModContainer modContainer) {
        LyyRegistries.registerAll(modEventBus);
        modEventBus.addListener(LyyCapabilities::register);
        modEventBus.addListener((RegisterGameTestsEvent event) -> {
            event.register(LyyGameTests.class);
            event.register(SonnetGameTests.class);
        });
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}
