package me.usainsrht.sit.mod.entry;

import me.usainsrht.sit.mod.Loaders;
import net.neoforged.fml.common.Mod;

/** NeoForge entrypoint, found by FML through the {@link Mod} annotation. */
@Mod("sit")
public final class SitNeoForge {
    public SitNeoForge() {
        Loaders.neoForge();
    }
}
