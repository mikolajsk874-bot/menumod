package pl.menumod.mixin;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.menumod.MenuConfig;

/** Mod "Brak cieni": wszystkie ściany bloków mają tę samą jasność (brak cieniowania). */
@Mixin(ClientWorld.class)
public class ClientWorldMixin {

    @Inject(method = "getBrightness", at = @At("HEAD"), cancellable = true, require = 0)
    private void menumod$flatShading(Direction direction, boolean shaded, CallbackInfoReturnable<Float> cir) {
        if (MenuConfig.noShadows) {
            cir.setReturnValue(1.0F);
        }
    }
}
