package pl.menumod;

import net.minecraft.client.MinecraftClient;

/** Mod "Brak cieni": wyłącza cień graczy/mobów i gładkie oświetlenie, a mixin spłaszcza cieniowanie ścian bloków. */
public final class NoShadows {

    private NoShadows() {}

    public static void set(boolean on) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options == null) return;

        if (on) {
            if (!MenuConfig.noShadows) {
                MenuConfig.prevEntityShadows = mc.options.getEntityShadows().getValue();
                MenuConfig.prevAo = mc.options.getAo().getValue();
            }
            MenuConfig.noShadows = true;
            mc.options.getEntityShadows().setValue(false);
            mc.options.getAo().setValue(false);
        } else {
            MenuConfig.noShadows = false;
            mc.options.getEntityShadows().setValue(MenuConfig.prevEntityShadows);
            mc.options.getAo().setValue(MenuConfig.prevAo);
        }

        MenuConfig.save();
        mc.options.write();
        if (mc.world != null) {
            mc.worldRenderer.reload();
        }
    }

    /** Po starcie gry: jeśli mod był włączony, ustaw opcje jeszcze raz (bez ruszania zapamiętanych wartości). */
    public static void applyOnStart(MinecraftClient mc) {
        if (MenuConfig.noShadows && mc.options != null) {
            mc.options.getEntityShadows().setValue(false);
            mc.options.getAo().setValue(false);
        }
    }
}
