package com.example.menumod;

import net.minecraft.client.MinecraftClient;

public class NoShadows {
    /** Włącza/wyłącza cienie bytów (gracz, moby) i wygładzone oświetlenie, potem przebudowuje chunki. */
    public static void apply(MinecraftClient c) {
        boolean off = MenuConfig.I.noShadows;
        c.options.getEntityShadows().setValue(!off);
        c.options.getAo().setValue(!off);
        if (c.worldRenderer != null) {
            c.worldRenderer.reload();
        }
    }
}
