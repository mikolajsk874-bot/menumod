package com.example.menumod.mixin;

import com.example.menumod.MenuConfig;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientWorld.class)
public class ClientWorldMixin {
    // Każda ściana bloku ma tę samą jasność, więc znika cieniowanie ścian.
    @Inject(method = "getBrightness", at = @At("HEAD"), cancellable = true)
    private void menumod$flat(Direction direction, boolean shaded, CallbackInfoReturnable<Float> cir) {
        if (MenuConfig.I.blockShadowsOff()) {
            cir.setReturnValue(1.0f);
        }
    }
}
