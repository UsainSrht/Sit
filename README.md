<div align="center">

<img src="icon.png" alt="Sit Plugin Icon" width="128"/>

# Sit

**A simple, highly configurable sitting plugin for Minecraft servers.**  
*Right-click a stair, slab, or any custom block to sit down.*

[![Modrinth Downloads](https://img.shields.io/modrinth/dt/stairsit?logo=modrinth&label=Downloads&color=00AF5C)](https://modrinth.com/plugin/stairsit)
[![Modrinth Version](https://img.shields.io/modrinth/v/stairsit?logo=modrinth&label=Latest%20Version&color=00AF5C)](https://modrinth.com/plugin/stairsit)
[![Modrinth Game Versions](https://img.shields.io/modrinth/game-versions/stairsit?label=Minecraft&color=62b0e8)](https://modrinth.com/plugin/stairsit)
[![Java](https://img.shields.io/badge/Java-8%2B-ED8B00?logo=openjdk&logoColor=white)](https://www.java.com)
[![Folia Supported](https://img.shields.io/badge/Folia-Supported-brightgreen)](https://papermc.io/software/folia)
[![bStats](https://img.shields.io/badge/bStats-30679-1bae9f?logo=chart-bar)](https://bstats.org/plugin/bukkit/Sit/30679)

</div>

---

## Features

- **Sit on stairs and slabs** — right-click any stair or slab with an empty hand to sit
- **Fully configurable** — define unlimited custom *sittable* layouts in `config.yml`
- **Permission-based layouts** — optionally lock any layout behind a permission node
- **One jar for everything** — the same file runs as a plugin (Spigot, Paper, Purpur, Folia) and as a mod (Fabric, Forge, NeoForge)
- **Folia compatible** — uses schedulers for full Folia/Paper/Spigot support
- **Same `config.yml` everywhere** — move between the plugin and the mod without touching your configuration
- **Two check modes** — match blocks by `BLOCKDATA` class or explicit `BLOCKS` material list

---

## Download

Get the latest release from Modrinth:

[![Download on Modrinth](https://img.shields.io/badge/Download%20on-Modrinth-00AF5C?style=for-the-badge&logo=modrinth&logoColor=white)](https://modrinth.com/plugin/stairsit)

https://modrinth.com/plugin/stairsit

---

## Supported platforms

One `sit-x.x.x.jar` works on every platform below, there is nothing to pick between.

| Platform | Minecraft versions | Where it goes |
|----------|--------------------|---------------|
| Spigot · Paper · Purpur · Folia | 1.13 – 26.3 | `plugins/` |
| Fabric (needs [Fabric API](https://modrinth.com/mod/fabric-api)) | 1.16.5 – 26.3 | `mods/` |
| Forge | 1.16.5 – 26.3 | `mods/` |
| NeoForge | 1.20.2 – 26.3 | `mods/` |

Mods run on dedicated servers and in singleplayer / LAN worlds (the integrated server runs the same code). They
are server side only: players do **not** need the mod to join a server that has it, and installing it on a client
that joins a server without Sit simply does nothing.

## Installation

### Plugin (Spigot, Paper, Purpur, Folia)

1. Download the latest `sit-x.x.x.jar` from [Modrinth](https://modrinth.com/plugin/stairsit).
2. Drop the `.jar` into your server's `plugins/` folder.
3. Start (or restart) your server — a default `config.yml` will be created under `plugins/Sit/`.
4. Tweak `config.yml` to your needs (see [Configuration](#configuration) below).
5. Run `/sit reload` to apply config changes without restarting.

### Mod (Fabric, Forge, NeoForge)

1. Download the same `sit-x.x.x.jar` and drop it into `mods/` (on a dedicated server, or in your game folder for singleplayer).
2. On Fabric also install [Fabric API](https://modrinth.com/mod/fabric-api).
3. Start the game or server — a default `config.yml` will be created under `config/Sit/`.
4. Run `/sit reload` to apply config changes without restarting.

> **Requirements:** Java **8+** for old versions; use the Java version your Minecraft version asks for.

#### Mod specifics

- **Config:** `config/Sit/config.yml`, same keys and behaviour as the plugin's `plugins/Sit/config.yml`.
- **Permissions:** the mod has no permission system of its own, so it follows the plugin's default (operators). Layouts with
  `permission.require: true` are allowed for operators (permission level 2) and for players granted the node through the
  [Fabric Permissions API](https://github.com/lucko/fabric-permissions-api) (LuckPerms). `/sit reload` needs an operator.
  In singleplayer, turn on cheats (or `/op` yourself) to use permission-locked layouts.
- **`check: BLOCKDATA`:** the common Bukkit classes are understood (`Stairs`, `Slab`, `Fence`, `Wall`, `Gate`, `Door`,
  `TrapDoor`, `Bed`, `Chest`, `Ladder`, `BlockData`) as well as the property interfaces (`Directional`, `Bisected`,
  `Rotatable`, `Waterlogged`, `Openable`, `Powerable`, `Lightable`, ...). `check: BLOCKS` accepts any block id's path (`END_ROD`, `oak_stairs`).
- **`entity.saddle`:** accepted but not needed, the seat entity is invisible and cannot be steered.
- **Debugging:** start with `-Dsit.debug=true` to log why an interaction was or was not turned into sitting.

---

---

## Usage

| Action | Result |
|--------|--------|
| Right-click a **stair or slab** with an empty hand | Sit down |
| Hold any **item** in hand | Sitting is skipped |
| **Sneak** while clicking | Sitting is skipped |
| Jump / move away | Stand up |

---

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/sit reload` | Reload the plugin configuration live | `sit.command` |

---

## Permissions

| Permission | Default | Description |
|------------|---------|-------------|
| `sit.command` | OP | Use `/sit reload` |
| `sit.<layout>` | OP | Sit on a layout that has `permission.require: true` (e.g. `sit.fun`) |

---

## Configuration

Default `config.yml`:

```yaml
sitables:
  stairs:
    entity:
      type: PIG
      saddle: true
    offsets:
      x: 0.5
      y: -0.4
      z: 0.5
    # BLOCKDATA matches by BlockData class name
    # BLOCKS matches by material name
    check: BLOCKDATA
    list:
      - org.bukkit.block.data.type.Stairs

  slabs:
    entity:
      type: PIG
      saddle: true
    offsets:
      x: 0.5
      y: -0.4
      z: 0.5
    check: BLOCKDATA
    list:
      - org.bukkit.block.data.type.Slab

  fun:
    # Require the player to have the "sit.fun" permission
    permission:
      require: true
      name: "sit.%s"   # %s is replaced with the layout name
    entity:
      type: ARMOR_STAND
    offsets:
      x: 0.5
      y: -1.1
      z: 0.5
    check: BLOCKS
    list:
      - END_ROD
```

### Config Reference

| Key | Type | Description |
|-----|------|-------------|
| `entity.type` | String | Entity used as the invisible seat (`PIG`, `ARMOR_STAND`, …) |
| `entity.saddle` | Boolean | Attach a saddle to the entity *(Pig only)* |
| `offsets.x/y/z` | Double | Fine-tune the player's seated position relative to the block |
| `check` | String | `BLOCKDATA` — match by BlockData class · `BLOCKS` — match by material name |
| `list` | List | BlockData class names *(BLOCKDATA)* or material names *(BLOCKS)* |
| `permission.require` | Boolean | Whether this layout needs a permission to use |
| `permission.name` | String | Permission node — `%s` is replaced with the layout name |

---

## Building from Source

**Prerequisites:** Java 8+ (the build uses `--release 8`), [Apache Maven](https://maven.apache.org/) 3.6+

```bash
# 1. Clone the repository
git clone https://github.com/UsainSrht/Sit.git
cd Sit

# 2. Build
mvn package
```

The shaded jar will be output to `target/sit-<version>.jar`. It is the plugin and the Fabric / Forge / NeoForge mod at the same time.

How the mod side supports every Minecraft version with one jar, and how it is tested, is described in [tools/README.md](tools/README.md).

---

## Contributing

Contributions are welcome! Please follow these steps:

1. **Fork** this repository and clone your fork.
2. **Create a feature branch:**
   ```bash
   git checkout -b feature/your-feature-name
   ```
3. **Make your changes** and test them on a local Paper or Folia server.
4. **Commit** with a clear message:
   ```bash
   git commit -m "Add: short description of your change"
   ```
5. **Push** your branch and open a **Pull Request** against `main`.

### Guidelines

- Keep code style consistent with the existing codebase.
- Any scheduling or entity spawning in the plugin **must** use `MorePaperLib` schedulers to preserve Folia compatibility.
- The mod code must not reference Minecraft classes directly: go through `Names` / `Mc` (see [tools/README.md](tools/README.md)), and add new Minecraft members to `tools/api.spec`.
- If you add a new config option, update `config.yml` **and** this README.
- Verify your changes work on both Paper and Folia before submitting, and run `tools/test` for anything that touches the mod.

---

## Stats

[![bStats Graph](https://bstats.org/signatures/bukkit/stairsit.svg)](https://bstats.org/plugin/bukkit/stairsit/30679)

---

## License

This project is open source. See the [LICENSE](LICENSE) file for details.
