package com.example.menumod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.time.LocalDate;

public class MenuModClient implements ClientModInitializer {
    public static KeyBinding OPEN;

    /** Moment startu tej sesji gry (do zakładki Profil). */
    public static final long SESSION_START = System.currentTimeMillis();

    private static boolean prevBind;
    private static int ticks;
    private static int saveCounter;

    @Override
    public void onInitializeClient() {
        MenuConfig.load();

        OPEN = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.menumod.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.menumod"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN.wasPressed()) {
                client.setScreen(new MenuScreen());
            }

            // bind modułu "Brak cieni"
            int key = MenuConfig.I.noShadowsKey;
            boolean down = key > 0 && InputUtil.isKeyPressed(client.getWindow().getHandle(), key);
            if (down && !prevBind && client.currentScreen == null && client.world != null) {
                NoShadows.toggle(client);
            }
            prevBind = down;

            // licznik czasu gry (sekundy, per dzień)
            if (client.world != null && ++ticks >= 20) {
                ticks = 0;
                MenuConfig.I.playSeconds.merge(LocalDate.now().toString(), 1L, Long::sum);
                if (++saveCounter >= 30) {
                    saveCounter = 0;
                    MenuConfig.save();
                }
            }
        });

        HudRenderCallback.EVENT.register((ctx, tickCounter) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (!MenuConfig.I.hudOn || mc.player == null || mc.options.hudHidden) return;
            if (mc.currentScreen instanceof HudEditScreen) return;
            Hud.draw(ctx, mc, MenuConfig.I.hudX, MenuConfig.I.hudY);
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> MenuConfig.save());
    }
}
