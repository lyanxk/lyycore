package org.lyy.lyycore;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.fml.loading.FMLPaths;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

/** Runtime balance knobs. Values are read lazily so config reloads take effect. */
public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue GENERATOR_FE_PER_TARGET_TICK = BUILDER
            .comment("FE generated for every discovered target each tick; 512 FE equals 5.12 IE, with fractional IE accumulated")
            .defineInRange("generator.fePerTargetTick", 512, 0, Integer.MAX_VALUE);
    public static final ModConfigSpec.IntValue GENERATOR_RESCAN_INTERVAL = BUILDER
            .comment("Ticks between automatic generator target rescans")
            .defineInRange("generator.rescanInterval", 200, 20, 12_000);

    public static final ModConfigSpec.IntValue ENERGY_CELL_IE_RATE = BUILDER
            .comment("Maximum IE converted or exported by an energy cell per tick")
            .defineInRange("energyCell.ieRatePerTick", 1_000, 1, 10_000_000);
    public static final ModConfigSpec.IntValue ENERGY_CELL_TRANSFER_INTERVAL = BUILDER
            .comment("Ticks between energy-cell neighbor transfers")
            .defineInRange("energyCell.transferInterval", 2, 1, 200);

    public static final ModConfigSpec.IntValue COLLECTOR_SMALL_RADIUS = BUILDER
            .comment("Small-mode collection radius; 1 means a 3x3x3 area")
            .defineInRange("collector.smallRadius", 1, 1, 16);
    public static final ModConfigSpec.IntValue COLLECTOR_LARGE_RADIUS = BUILDER
            .comment("Large-mode collection radius; 3 means a 7x7x7 area")
            .defineInRange("collector.largeRadius", 3, 1, 32);
    public static final ModConfigSpec.IntValue COLLECTOR_TICK_INTERVAL = BUILDER
            .comment("Ticks between collection attempts")
            .defineInRange("collector.tickInterval", 20, 1, 1_200);
    public static final ModConfigSpec.IntValue COLLECTOR_TARGET_RADIUS = BUILDER
            .comment("Container search radius around the collector")
            .defineInRange("collector.targetRadius", 3, 1, 16);
    public static final ModConfigSpec.IntValue COLLECTOR_SEARCH_BACKOFF = BUILDER
            .comment("Ticks to wait before searching again when no container exists")
            .defineInRange("collector.searchBackoff", 100, 20, 12_000);

    public static final ModConfigSpec.IntValue FORGE_PASSIVE_IE = BUILDER
            .comment("IE generated naturally by an alloy forge each interval")
            .defineInRange("forge.passiveIe", 1, 0, 1_000_000);
    public static final ModConfigSpec.IntValue FORGE_PASSIVE_INTERVAL = BUILDER
            .comment("Ticks between passive alloy-forge energy gains")
            .defineInRange("forge.passiveInterval", 20, 1, 1_200);

    public static final ModConfigSpec.IntValue DISASSEMBLER_MAX_VEIN = BUILDER
            .comment("Maximum vein blocks including the originally mined block")
            .defineInRange("disassembler.maxVeinBlocks", 64, 1, 64);
    public static final ModConfigSpec.IntValue DISASSEMBLER_MAX_BREAK_EFFECTS = BUILDER
            .comment("Maximum standard block break sounds/particles per operation")
            .defineInRange("disassembler.maxBreakEffects", 8, 1, 8);
    public static final ModConfigSpec.IntValue DISASSEMBLER_AOE_RADIUS = BUILDER
            .comment("AOE mining radius; 2 means a 5x5 plane")
            .defineInRange("disassembler.aoeRadius", 2, 0, 2);

    public static final ModConfigSpec SPEC = BUILDER.build();

    /** Correct mislabelled settings before NeoForge removes unknown keys. */
    public static void migrateLegacyEnergyUnits() {
        Path path = FMLPaths.CONFIGDIR.get().resolve("lyycore-common.toml");
        if (!Files.isRegularFile(path)) return;
        try {
            String original = Files.readString(path);
            String migrated = migrateLegacyEnergyUnits(original);
            if (!original.equals(migrated)) Files.writeString(path, migrated);
        } catch (IOException exception) {
            LyyCore.LOGGER.warn("Could not migrate legacy energy unit settings in {}", path, exception);
        }
    }

    public static String migrateLegacyEnergyUnits(String source) {
        // Generator rates were accidentally renamed to IE without converting the FE values.
        String result = migrateEnergyKey(source, "generator", "iePerTargetTick", "fePerTargetTick");
        return migrateEnergyKey(result, "forge", "passiveFe", "passiveIe");
    }

    private static String migrateEnergyKey(String source, String section, String oldKey, String newKey) {
        Pattern pattern = Pattern.compile("(?ms)(^\\s*\\[" + section + "\\][^\\r\\n]*\\R)(.*?)(?=^\\s*\\[|\\z)");
        return pattern.matcher(source).replaceAll(match -> {
            String body = match.group(2);
            if (Pattern.compile("(?m)^\\h*" + newKey + "\\h*=").matcher(body).find())
                return java.util.regex.Matcher.quoteReplacement(match.group());
            return java.util.regex.Matcher.quoteReplacement(match.group(1)
                    + body.replaceAll("(?m)^(\\h*)" + oldKey + "(\\h*=)", "$1" + newKey + "$2"));
        });
    }

    private Config() { }
}
