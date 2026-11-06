# Sit build tooling

The mod half of the jar talks to Minecraft through reflection, so a single file runs on every Minecraft version and
every loader. These scripts generate the data that makes that possible and test the result. They need **Node 18+**
(and a JDK for `javap`); the Maven build itself does not need any of them.

## How the one-jar mod works

* `api.spec` lists every Minecraft class, method and field the mod uses, written with Mojang's official names.
* `fetch-mappings.js` downloads the real mapping files for each Minecraft version: Mojang (proguard), Fabric
  intermediary and the Forge SRG / MCP files.
* `gen-mappings.js` joins them and writes `src/main/resources/sit-mappings.txt`: for each entry, every name it has
  had in any mapping scheme. `me.usainsrht.sit.mod.Names` resolves entries against the running game at startup.
* The generator also prints which versions are missing each entry, so supporting a new Minecraft version is:
  fetch its mappings, regenerate, read the report, add aliases to `api.spec` where something was renamed.
* Minecraft 26.x ships unobfuscated, so there is nothing to download for it. `check-unobf.js <server.jar> api.spec`
  checks the spec against such a jar with `javap` (use the `versions/<v>/paper-<v>.jar` Paper unpacks, or any
  deobfuscated server jar).

```bash
node tools/fetch-mappings.js /tmp/maps            # downloads ~35 versions, a few hundred MB
node tools/gen-mappings.js /tmp/maps tools/api.spec src/main/resources/sit-mappings.txt
node tools/check-unobf.js path/to/paper-26.3.jar tools/api.spec
```

Regenerating from the same inputs gives a byte-identical table.

## End to end tests (`tools/test`)

The tests start a real server, join it with a tiny headless client (`sitbot.js`, Node built-ins only), build a
stair, a slab and an end rod, right click them and check, through server console commands, that the seat entity
exists, that the player rides it, that top-half stairs are ignored, that permission-gated layouts need an operator,
that the seat disappears on dismount and that it survives a log out while sitting.

* `prep.js <loader> <mc> <root>` downloads and installs a server (`fabric`, `forge`, `neoforge`, `paper`, `purpur`,
  `folia`) and works out the packet ids the client needs (from the `--reports` output on 1.20.5+, or from the
  bytecode of the vanilla jar on older versions, see `oldids.js`).
* `matrix.js <root> <jar> <loader:mc>...` installs the jar into each server and runs `runtest.js`.

```bash
node tools/test/matrix.js /tmp/srv target/sit-1.8.0.jar fabric:1.21.4 forge:1.20.1 neoforge:26.3 paper:1.13.2
```

Servers need the Java version their Minecraft version asks for; `prep.js` expects JDK 8, 17, 21 and 25 at the paths
listed at its top. Set `SIT_PORT` to run several matrices side by side.
