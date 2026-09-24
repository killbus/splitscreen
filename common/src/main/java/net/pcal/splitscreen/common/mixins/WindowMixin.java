/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2023 pcal.net
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package net.pcal.splitscreen.common.mixins;

import com.mojang.blaze3d.platform.DisplayData;
import com.mojang.blaze3d.platform.MonitorManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.WindowEventHandler;
import com.mojang.renderpearl.api.device.GpuBackend;
import net.pcal.splitscreen.common.MinecraftWindow;
import net.pcal.splitscreen.common.MouseConfinement;
import net.pcal.splitscreen.common.WindowStyle;
import org.lwjgl.sdl.SDLError;
import org.lwjgl.sdl.SDLKeyboard;
import org.lwjgl.sdl.SDLVideo;
import org.lwjgl.sdl.SDL_Rect;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.pcal.splitscreen.common.Mod.mod;
import static net.pcal.splitscreen.common.logging.SystemLogger.syslog;

/**
 * @author pcal
 * @since 0.0.1
 */
@Mixin(Window.class)
public abstract class WindowMixin implements MinecraftWindow {

    @Final
    @Shadow
    private long handle;
    @Shadow
    private int windowedX;
    @Shadow
    private int windowedY;
    @Shadow
    private int windowedWidth;
    @Shadow
    private int windowedHeight;
    @Shadow
    private int x;
    @Shadow
    private int y;
    @Shadow
    private int width;
    @Shadow
    private int height;
    @Shadow
    private boolean fullscreen;
    @Shadow
    private boolean fullscreenRequested;
    @Shadow
    protected abstract void setMode();

    @Unique
    private final MouseConfinement splitscreen_mouseConfinement = new MouseConfinement(
            () -> SDLKeyboard.SDL_GetKeyboardFocus() == this.handle,
            confined -> {
                boolean success = SDLVideo.SDL_SetWindowMouseGrab(this.handle, confined);
                if (!success) syslog().warn("Could not update mouse confinement: " + SDLError.SDL_GetError());
                return success;
            });

    // ======================================================================
    // Mixins

    @Inject(method = "<init>", at = @At(value = "TAIL"), remap = false)
    private void Window(WindowEventHandler eventHandler, DisplayData displayData, String fullscreenVideoModeString, boolean exclusiveFullscreen, String title, MonitorManager monitorManager, final GpuBackend backend, CallbackInfo ci) {
        mod().onWindowCreate(this);
    }

    /**
     * 26.3 removed Window.toggleFullScreen(); the F11 keybind and the fullscreen
     * option both funnel through setFullscreen(boolean) instead.
     */
    @Inject(method = "setFullscreen(Z)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void splitscreen_setFullscreen(boolean requested, CallbackInfo ci) {
        if (requested != this.fullscreenRequested) {
            mod().onToggleFullscreen(this);
            ci.cancel();
        }
    }

    @Inject(method = "onFramebufferResize(II)V", at = @At("RETURN"), remap = false)
    private void splitscreen_onFramebufferSizeChanged(int width, int height, CallbackInfo ci) {
        // Mirror the vanilla guard; minimize fires framebuffer resize with zero sizes.
        if (width > 0 && height > 0) mod().onResolutionChange(this);
    }

    // ======================================================================
    // RepositionableWindow implementation

    @Override
    @Unique
    public Rectangle getWindowBounds() {
        return new Rectangle(x, y, width, height);
    }

    @Override
    @Unique
    public Rectangle getScreenBounds() {
        // Window placement uses desktop coordinates, not a preferred exclusive
        // fullscreen resolution or framebuffer pixels (which may differ on HiDPI).
        try (final MemoryStack stack = MemoryStack.stackPush()) {
            final int display = SDLVideo.SDL_GetDisplayForWindow(this.handle);
            final SDL_Rect bounds = SDL_Rect.malloc(stack);
            if (display == 0 || !SDLVideo.SDL_GetDisplayBounds(display, bounds)) {
                syslog().warn("Could not determine desktop bounds");
                return null;
            }
            return new Rectangle(bounds.x(), bounds.y(), bounds.w(), bounds.h());
        }
    }

    @Override
    @Unique
    public void setMouseConfined(boolean confined) {
        // Mouse grab keeps menu cursors visible and does not grab the keyboard.
        this.splitscreen_mouseConfinement.setConfined(confined);
    }

    @Override
    @Unique
    public void updateMouseConfinement() {
        this.splitscreen_mouseConfinement.updateFocus();
    }

    @Override
    @Unique
    public void reposition(WindowStyle style, Rectangle newBounds) {
        switch (style) {
            case FULLSCREEN:
                this.fullscreenRequested = true;
                this.fullscreen = false; // force setMode to actually apply it
                this.setMode();
                break;
            case WINDOWED:
            case SPLITSCREEN:
                this.fullscreenRequested = false;
                this.fullscreen = true; // force setMode to actually apply it
                this.windowedX = newBounds.x();
                this.windowedY = newBounds.y();
                this.windowedWidth = newBounds.width();
                this.windowedHeight = newBounds.height();
                this.setMode();
                SDLVideo.SDL_SetWindowBordered(this.handle, style == WindowStyle.WINDOWED);
                // Restoring a layout must not take focus from the keyboard/mouse player.
        }
    }
}
