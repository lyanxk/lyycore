# LyyCore

Minecraft 1.21.1 / NeoForge utility mod for modpack progression.

## Features

- Imaginium ores and alloying recipes
- Imaginary Alloy Forge
- Imaginary Energy Cell and FE/IE conversion
- Imaginary Generator with multi-target output
- Item Collector
- Infinite-durability Imaginary Disassembler with speed, area and vein modes
- Optional JEI integration

## Development

Requires Java 21.

```powershell
.\gradlew.bat clean build
.\gradlew.bat runClient
.\gradlew.bat runServer
```

The built mod is written to `build/libs`. Runtime balance settings are generated in
`config/lyycore-common.toml`.
