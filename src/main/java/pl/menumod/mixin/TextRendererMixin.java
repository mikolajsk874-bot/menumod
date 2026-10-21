package pl.menumod.mixin;

import net.minecraft.client.font.TextRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import pl.menumod.MenuConfig;

/** Mod "Cienie tekstu": wymusza cień pod każdym tekstem (nicki, tablica wyników, tytuły okien itd.). */
@Mixin(TextRenderer.class)
public class TextRendererMixin {

    @ModifyVariable(method = "draw", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private boolean menumod$forceShadow(boolean shadow) {
        return shadow || MenuConfig.textShadows;
    }
}
