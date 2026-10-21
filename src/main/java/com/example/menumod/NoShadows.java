package com.example.menumod;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class NoShadows {
    /** Włącza/wyłącza cienie bytów (gracz, moby) i wygładzone oświetlenie, potem przebudowuje chunki. */
    public static void apply(MinecraftClient c) {
        c.options.getEntityShadows().setValue(!MenuConfig.I.entityShadowsOff());
        c.options.getAo().setValue(!MenuConfig.I.blockShadowsOff());
        if (c.worldRenderer != null) {
            c.worldRenderer.reload();
        }
    }

    /** Przełącza moduł (np. z bindu) i pokazuje komunikat nad paskiem skrótów. */
    public static void toggle(MinecraftClient c) {
        MenuConfig.I.noShadows = !MenuConfig.I.noShadows;
        MenuConfig.save();
        apply(c);
        if (c.player != null) {
            c.player.sendMessage(Text.literal("Brak cieni: " + (MenuConfig.I.noShadows ? "WŁ." : "WYŁ.")), true);
        }
    }
}
