package net.pcal.splitscreen.common.mixins;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.pcal.splitscreen.common.MinecraftWindow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.pcal.splitscreen.common.Mod.mod;

@Mixin(InputConstants.class)
public abstract class InputConstantsMixin {

    @Inject(method = "releaseMouse(Lcom/mojang/blaze3d/platform/Window;DD)V", at = @At("RETURN"), remap = false)
    private static void splitscreen_keepMenuCursorInWindow(Window window, double x, double y, CallbackInfo ci) {
        mod().onMouseRelease((MinecraftWindow) (Object) window);
    }
}
