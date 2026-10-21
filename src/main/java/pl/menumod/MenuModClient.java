package pl.menumod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class MenuModClient implements ClientModInitializer {

    public static KeyBinding openKey;

    @Override
    public void onInitializeClient() {
        MenuConfig.load();

        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.menumod.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_N,
                "category.menumod"
        ));

        ClientLifecycleEvents.CLIENT_STARTED.register(NoShadows::applyOnStart);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.wasPressed()) {
                if (client.currentScreen == null && client.player != null) {
                    client.setScreen(new MenuScreen());
                }
            }
        });
    }
}
