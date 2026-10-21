package com.example.menumod.mixin;

import com.example.menumod.MenuConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {
    @Shadow @Final private MinecraftClient client;
    @Shadow private ItemStack mainHand;
    @Shadow private ItemStack offHand;
    @Shadow private float equipProgressMainHand;
    @Shadow private float prevEquipProgressMainHand;
    @Shadow private float equipProgressOffHand;
    @Shadow private float prevEquipProgressOffHand;

    // Przedmiot w ręce zmienia się od razu, bez animacji wyciągania.
    @Inject(method = "updateHeldItems", at = @At("HEAD"))
    private void menumod$instantEquip(CallbackInfo ci) {
        if (!MenuConfig.I.noEquipAnim) return;
        ClientPlayerEntity p = client.player;
        if (p == null) return;
        mainHand = p.getMainHandStack();
        offHand = p.getOffHandStack();
        equipProgressMainHand = 1.0f;
        prevEquipProgressMainHand = 1.0f;
        equipProgressOffHand = 1.0f;
        prevEquipProgressOffHand = 1.0f;
    }
}
