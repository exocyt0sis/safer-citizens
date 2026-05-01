[![Version](https://img.shields.io/badge/version-1.0-blue.svg)](https://github.com/exocyt0sis/safer-citizens/releases)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-3c8527.svg)](https://www.minecraft.net/)
[![NeoForge](https://img.shields.io/badge/NeoForge-21.1.226-2f6db5.svg)](https://neoforged.net/)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)

# Styx's Safer Citizens

Minecraft protects its own players by not allowing monsters and other hostile creatures to spawn within a 24 block radius of the player - so why shouldn't your citizens enjoy the same safety? Styx's Safer Citizens tweaks how monsters and other creatures are being spawned, giving your denizens a well deserved break. While the mod has been created with MineColonies in mind, it works just as well with a plain installation of NeoForge.

## Features

- Blocks configured hostile mob spawns near configured protected entities.
- Can optionally block configured hostile spawns anywhere inside MineColonies colony borders.
- Works without MineColonies installed.
- Uses a simple TOML config so the protected entities, affected entities and radius can be changed without recompiling.
- Can optionally log startup and prevented spawns to the console.

## Compatibility

- Minecraft 1.21.1
- NeoForge 21.1.226 (will likely work fine with other versions, too)
- Java 21
- MineColonies is optional

## Configuration

The mod writes its config to `safercitizens.toml`.

Available options:

- `ConsoleMessages`: enables startup and prevented-spawn log messages.
- `FendedEntities`: entity ids that should be protected.
- `FendedRadius`: radius around protected entities where configured hostile spawns are blocked.
- `AffectedEntities`: entity ids that should be blocked by the mechanic.
- `AlwaysFendWithinColonies`: when `true`, configured hostile spawns are blocked anywhere inside MineColonies colony borders.

## Release Files

Each GitHub release is intended to provide:

- The built NeoForge mod JAR.
- GitHub's auto-generated source archive in `.zip` format.
- GitHub's auto-generated source archive in `.tar.gz` format.

## Development

Build the mod locally with:

```powershell
.\gradlew.bat jar --console=plain
```

The built artifact is written to:

```text
build/libs/safercitizens-1.21.1-neoforge-1.0.jar
```

## License

This project is licensed under the GNU General Public License v3.0 only. See the [LICENSE](LICENSE) file for details.
