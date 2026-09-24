package net.pcal.splitscreen.common;

import net.pcal.splitscreen.common.MinecraftWindow.Rectangle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModTest {
    @TempDir
    Path configDir;

    private Mod createMod(String mode) throws IOException {
        Files.writeString(configDir.resolve("splitscreen.properties"), "mode=" + mode + "\ngap=1\n");
        Mod mod = new Mod();
        mod.onModInitialize(configDir);
        return mod;
    }

    @Test
    void resizeWaitsUntilUiIsReady() throws IOException {
        Mod mod = createMod("LEFT");
        FakeWindow window = new FakeWindow();
        mod.onResolutionChange(window);
        mod.onWindowCreate(window);
        mod.onResolutionChange(window);
        assertEquals(0, window.repositions);

        mod.onUiReady(window);
        assertEquals(1, window.repositions);
        assertEquals(new Rectangle(1920, 0, 1279, 1440), window.bounds);
    }

    @Test
    void ordinaryWindowResizeIsNotUndone() throws IOException {
        Mod mod = createMod("WINDOWED");
        FakeWindow window = new FakeWindow();
        mod.onUiReady(window);
        window.bounds = new Rectangle(2000, 100, 1000, 700);
        mod.onResolutionChange(window);
        assertEquals(1, window.repositions);
        assertEquals(new Rectangle(2000, 100, 1000, 700), window.bounds);
    }

    @Test
    void splitResizeOnlyRepositionsWhenDesktopBoundsChange() throws IOException {
        Mod mod = createMod("RIGHT");
        FakeWindow window = new FakeWindow();
        mod.onUiReady(window);
        mod.onResolutionChange(window);
        mod.onResolutionChange(window);
        assertEquals(1, window.repositions);

        window.screen = new Rectangle(1920, 0, 1920, 1080);
        mod.onResolutionChange(window);
        assertEquals(2, window.repositions);
        assertEquals(new Rectangle(2881, 0, 959, 1080), window.bounds);
        mod.onResolutionChange(window);
        assertEquals(2, window.repositions);
    }

    @Test
    void fullscreenResizeDoesNotRestartTheModeTransition() throws IOException {
        Mod mod = createMod("FULLSCREEN");
        FakeWindow window = new FakeWindow();
        mod.onUiReady(window);
        mod.onResolutionChange(window);
        assertEquals(1, window.repositions);
    }

    @Test
    void unavailableDisplayLeavesTheWindowAloneAndCanRecover() throws IOException {
        Mod mod = createMod("LEFT");
        FakeWindow window = new FakeWindow();
        Rectangle original = window.bounds;
        window.screen = null;
        mod.onUiReady(window);
        mod.onResolutionChange(window);
        assertEquals(0, window.repositions);
        assertEquals(original, window.bounds);

        window.screen = new Rectangle(0, 0, 1920, 1080);
        mod.onResolutionChange(window);
        assertEquals(1, window.repositions);
        assertEquals(new Rectangle(0, 0, 959, 1080), window.bounds);
    }

    @Test
    void callbacksDuringRepositionDoNotReenterIt() throws IOException {
        Mod mod = createMod("LEFT");
        FakeWindow window = new FakeWindow();
        window.duringReposition = () -> {
            mod.onUiReady(window);
            mod.onResolutionChange(window);
        };
        mod.onUiReady(window);
        assertEquals(1, window.repositions);

        window.screen = new Rectangle(0, 0, 1920, 1080);
        mod.onResolutionChange(window);
        assertEquals(2, window.repositions);
    }

    private static class FakeWindow implements MinecraftWindow {
        Rectangle bounds = new Rectangle(2000, 100, 854, 480);
        Rectangle screen = new Rectangle(1920, 0, 2560, 1440);
        int repositions;
        Runnable duringReposition = () -> {};

        @Override
        public Rectangle getWindowBounds() { return bounds; }

        @Override
        public Rectangle getScreenBounds() { return screen; }

        @Override
        public void reposition(WindowStyle style, Rectangle bounds) {
            repositions++;
            duringReposition.run();
            if (style != WindowStyle.FULLSCREEN) this.bounds = bounds;
        }
    }
}
