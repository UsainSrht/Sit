package me.usainsrht.sit.mod.entry;

import me.usainsrht.sit.mod.Loaders;
import net.fabricmc.api.ModInitializer;

/** Fabric entrypoint (fabric.mod.json, "main"). Runs on dedicated servers and on clients (singleplayer). */
public final class SitFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Loaders.fabric();
    }
}
