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
- **Folia compatible** — uses schedulers for full Folia/Paper/Spigot support
- **Two check modes** — match blocks by `BLOCKDATA` class or explicit `BLOCKS` material list

---

## Download

Get the latest release from Modrinth:

[![Download on Modrinth](https://img.shields.io/badge/Download%20on-Modrinth-00AF5C?style=for-the-badge&logo=modrinth&logoColor=white)](https://modrinth.com/plugin/stairsit)

https://modrinth.com/plugin/stairsit

---

## Installation

1. Download the latest `sit-x.x.x.jar` from [Modrinth](https://modrinth.com/plugin/stairsit).
2. Drop the `.jar` into your server's `plugins/` folder.
3. Start (or restart) your server — a default `config.yml` will be created under `plugins/Sit/`.
4. Tweak `config.yml` to your needs (see [Configuration](#configuration) below).
5. Run `/sit reload` to apply config changes without restarting.

> **Requirements:** Spigot / Paper / Folia &nbsp;·&nbsp; Minecraft **1.13+** &nbsp;·&nbsp; Java **8+**

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

**Prerequisites:** Java 8+, [Apache Maven](https://maven.apache.org/) 3.6+

```bash
# 1. Clone the repository
git clone https://github.com/UsainSrht/Sit.git
cd Sit

# 2. Build
mvn package
```

The shaded jar will be output to `target/sit-<version>.jar`.

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
- Any scheduling or entity spawning **must** use `MorePaperLib` schedulers to preserve Folia compatibility.
- If you add a new config option, update `config.yml` **and** this README.
- Verify your changes work on both Paper and Folia before submitting.

---

## Stats

[![bStats Graph](https://bstats.org/signatures/bukkit/stairsit.svg)](https://bstats.org/plugin/bukkit/stairsit/30679)

---

## License

This project is open source. See the [LICENSE](LICENSE) file for details.
