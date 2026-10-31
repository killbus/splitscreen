package net.pcal.splitscreen.common.mixins;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.pcal.splitscreen.common.MinecraftWindow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "runTick(Z)V", at = @At("HEAD"), remap = false)
    private void splitscreen_updateMouseConfinement(boolean advanceGameTime, CallbackInfo ci) {
        // The state helper only reapplies a grab when real SDL focus returns.
        Window window = ((Minecraft) (Object) this).getWindow();
        if (window != null) ((MinecraftWindow) (Object) window).updateMouseConfinement();
    }
}
