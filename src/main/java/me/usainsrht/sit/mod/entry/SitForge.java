package me.usainsrht.sit.mod.entry;

import me.usainsrht.sit.mod.Loaders;
import net.minecraftforge.fml.common.Mod;

/** Forge entrypoint, found by FML through the {@link Mod} annotation. */
@Mod("sit")
public final class SitForge {
    public SitForge() {
        Loaders.forge();
    }
}
