package net.pcal.splitscreen.common;

import net.pcal.splitscreen.common.MinecraftWindow.Rectangle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void mouseReleaseWaitsForInitialPlacement() throws IOException {
        FakeWindow window = new FakeWindow();
        new Mod().onMouseRelease(window);
        Mod mod = createMod("LEFT");
        mod.onWindowCreate(window);
        mod.onMouseRelease(window);
        assertEquals(0, window.confinementUpdates);

        mod.onUiReady(window);
        assertTrue(window.mouseConfined);
        assertEquals(1, window.confinementUpdates);
    }

    @ParameterizedTest
    @ValueSource(strings = {"LEFT", "RIGHT", "TOP", "BOTTOM", "TOP_LEFT", "TOP_RIGHT", "BOTTOM_LEFT", "BOTTOM_RIGHT", "BORDERLESS_FULLSCREEN"})
    void menuReleaseKeepsSplitCursorConfinedWithoutRepositioning(String mode) throws IOException {
        Mod mod = createMod(mode);
        FakeWindow window = new FakeWindow();
        mod.onUiReady(window);
        assertTrue(window.mouseConfined);
        Rectangle bounds = window.bounds;

        // The low-level release hook runs after vanilla mouse release.
        window.mouseConfined = false;
        mod.onMouseRelease(window);
        assertTrue(window.mouseConfined);
        assertEquals(2, window.confinementUpdates);
        assertEquals(1, window.repositions);
        assertEquals(bounds, window.bounds);
    }

    @ParameterizedTest
    @ValueSource(strings = {"WINDOWED", "FULLSCREEN"})
    void menuReleaseOutsideSplitscreenDoesNotChangeConfinement(String mode) throws IOException {
        Mod mod = createMod(mode);
        FakeWindow window = new FakeWindow();
        mod.onUiReady(window);
        assertFalse(window.mouseConfined);

        // Leave any constraint applied by another component alone on release.
        window.mouseConfined = true;
        mod.onMouseRelease(window);
        assertTrue(window.mouseConfined);
        assertEquals(1, window.confinementUpdates);
    }

    @Test
    void cyclingOutOfSplitscreenRemovesConfinementAndBackRestoresIt() throws IOException {
        Mod mod = createMod("BOTTOM_RIGHT");
        FakeWindow window = new FakeWindow();
        mod.onUiReady(window);
        assertTrue(window.mouseConfined);

        mod.onToggleFullscreen(window); // BORDERLESS_FULLSCREEN
        assertTrue(window.mouseConfined);
        assertEquals(window.screen, window.bounds);
        mod.onToggleFullscreen(window); // FULLSCREEN
        assertFalse(window.mouseConfined);
        mod.onToggleFullscreen(window); // WINDOWED
        assertFalse(window.mouseConfined);
        mod.onToggleFullscreen(window); // LEFT
        assertTrue(window.mouseConfined);
    }

    @Test
    void callbacksDuringRepositionDoNotReenterIt() throws IOException {
        Mod mod = createMod("LEFT");
        FakeWindow window = new FakeWindow();
        window.duringReposition = () -> {
            mod.onUiReady(window);
            mod.onResolutionChange(window);
            mod.onMouseRelease(window);
        };
        mod.onUiReady(window);
        assertEquals(1, window.repositions);

        window.screen = new Rectangle(0, 0, 1920, 1080);
        mod.onResolutionChange(window);
        assertEquals(2, window.repositions);
        assertEquals(2, window.confinementUpdates);
    }

    @Test
    void cyclingBackToWindowedRestoresTheLatestOrdinaryWindowBounds() throws IOException {
        Mod mod = createMod("WINDOWED");
        FakeWindow window = new FakeWindow();
        mod.onUiReady(window);
        Rectangle resized = new Rectangle(-1700, 80, 1200, 800);
        window.bounds = resized;
        window.screen = new Rectangle(-1920, 0, 1920, 1080);

        for (int i = 0; i < WindowMode.getModes(1).size(); i++) mod.onToggleFullscreen(window);

        assertEquals(WindowStyle.WINDOWED, window.style);
        assertEquals(resized, window.bounds);
        assertFalse(window.mouseConfined);
    }

    @ParameterizedTest
    @ValueSource(strings = {"BORDERLESS_FULLSCREEN", "FULLSCREEN"})
    void restoredLayoutDoesNotOverwriteTheInitialOrdinaryWindowBounds(String mode) throws IOException {
        Mod mod = createMod(mode);
        FakeWindow window = new FakeWindow();
        Rectangle original = window.bounds;
        mod.onUiReady(window);
        if (mode.equals("BORDERLESS_FULLSCREEN")) mod.onToggleFullscreen(window);
        mod.onToggleFullscreen(window);
        assertEquals(WindowStyle.WINDOWED, window.style);
        assertEquals(original, window.bounds);
    }

    @Test
    void borderlessFullscreenLoadsAsAnOrdinaryBorderlessWindow() throws IOException {
        Mod mod = createMod("BORDERLESS_FULLSCREEN");
        FakeWindow window = new FakeWindow();
        mod.onWindowCreate(window);
        mod.onResolutionChange(window);
        assertEquals(0, window.repositions);

        mod.onUiReady(window);
        assertEquals(WindowStyle.SPLITSCREEN, window.style);
        assertEquals(window.screen, window.bounds);
        assertEquals(1, window.repositions);
        assertTrue(window.mouseConfined);
    }

    @Test
    void borderlessFullscreenPersistsAndStillCyclesThroughNativeFullscreenAndWindowed() throws IOException {
        Mod mod = createMod("BOTTOM_RIGHT");
        FakeWindow window = new FakeWindow();
        Rectangle original = window.bounds;
        mod.onUiReady(window);
        mod.onToggleFullscreen(window);
        assertEquals(WindowStyle.SPLITSCREEN, window.style);
        assertEquals(window.screen, window.bounds);

        Properties saved = new Properties();
        try (var reader = Files.newBufferedReader(configDir.resolve("splitscreen.properties"))) {
            saved.load(reader);
        }
        assertEquals("BORDERLESS_FULLSCREEN", saved.getProperty("mode"));
        assertEquals("1", saved.getProperty("gap"));

        Mod restarted = new Mod();
        restarted.onModInitialize(configDir);
        FakeWindow restartedWindow = new FakeWindow();
        restarted.onUiReady(restartedWindow);
        assertEquals(WindowStyle.SPLITSCREEN, restartedWindow.style);
        assertEquals(restartedWindow.screen, restartedWindow.bounds);

        mod.onToggleFullscreen(window);
        assertEquals(WindowStyle.FULLSCREEN, window.style);
        mod.onToggleFullscreen(window);
        assertEquals(WindowStyle.WINDOWED, window.style);
        assertEquals(original, window.bounds);
    }

    @Test
    void borderlessResizeIsIdempotentAndDoesNotReenterPlacement() throws IOException {
        Mod mod = createMod("BORDERLESS_FULLSCREEN");
        FakeWindow window = new FakeWindow();
        window.duringReposition = () -> {
            mod.onUiReady(window);
            mod.onResolutionChange(window);
            mod.onSetMode(window);
            mod.onMouseRelease(window);
        };
        mod.onUiReady(window);
        mod.onResolutionChange(window);
        assertEquals(1, window.repositions);
        assertEquals(1, window.confinementUpdates);

        window.screen = new Rectangle(-3840, -2160, 3840, 2160);
        mod.onResolutionChange(window);
        assertEquals(window.screen, window.bounds);
        assertEquals(2, window.repositions);
        assertEquals(2, window.confinementUpdates);
        mod.onResolutionChange(window);
        assertEquals(2, window.repositions);
        assertEquals(2, window.confinementUpdates);
    }

    @Test
    void borderlessWaitsForAnAvailableDisplay() throws IOException {
        Mod mod = createMod("BORDERLESS_FULLSCREEN");
        FakeWindow window = new FakeWindow();
        Rectangle original = window.bounds;
        window.screen = null;
        mod.onUiReady(window);
        assertEquals(original, window.bounds);
        assertEquals(0, window.repositions);
        assertEquals(0, window.confinementUpdates);

        window.screen = new Rectangle(-1920, 0, 1920, 1080);
        mod.onResolutionChange(window);
        assertEquals(window.screen, window.bounds);
        assertEquals(WindowStyle.SPLITSCREEN, window.style);
        assertTrue(window.mouseConfined);
    }

    @Test
    void instancesKeepIndependentLayoutsOnDifferentDisplays() throws IOException {
        Mod first = createMod("BORDERLESS_FULLSCREEN");
        FakeWindow firstWindow = new FakeWindow();
        firstWindow.screen = new Rectangle(-1920, 0, 1920, 1080);
        first.onUiReady(firstWindow);

        Path secondConfig = Files.createDirectory(configDir.resolve("second"));
        Files.writeString(secondConfig.resolve("splitscreen.properties"), "mode=BORDERLESS_FULLSCREEN\n");
        Mod second = new Mod();
        second.onModInitialize(secondConfig);
        FakeWindow secondWindow = new FakeWindow();
        second.onUiReady(secondWindow);

        assertEquals(firstWindow.screen, firstWindow.bounds);
        assertEquals(secondWindow.screen, secondWindow.bounds);
        second.onToggleFullscreen(secondWindow);
        assertEquals(WindowStyle.FULLSCREEN, secondWindow.style);
        assertFalse(secondWindow.mouseConfined);
        assertEquals(WindowStyle.SPLITSCREEN, firstWindow.style);
        assertEquals(firstWindow.screen, firstWindow.bounds);
        assertTrue(firstWindow.mouseConfined);
        assertEquals(1, firstWindow.repositions);
        assertEquals(1, firstWindow.confinementUpdates);
    }

    @Test
    void unknownSavedModeStillFallsBackToWindowed() throws IOException {
        Mod mod = createMod("UNKNOWN_MODE");
        FakeWindow window = new FakeWindow();
        Rectangle original = window.bounds;
        mod.onUiReady(window);
        assertEquals(WindowStyle.WINDOWED, window.style);
        assertEquals(original, window.bounds);
    }

    private static class FakeWindow implements MinecraftWindow {
        Rectangle bounds = new Rectangle(2000, 100, 854, 480);
        Rectangle screen = new Rectangle(1920, 0, 2560, 1440);
        WindowStyle style;
        int repositions;
        boolean mouseConfined;
        int confinementUpdates;
        Runnable duringReposition = () -> {};

        @Override
        public Rectangle getWindowBounds() { return bounds; }

        @Override
        public Rectangle getScreenBounds() { return screen; }

        @Override
        public void reposition(WindowStyle style, Rectangle bounds) {
            repositions++;
            duringReposition.run();
            this.style = style;
            this.bounds = style == WindowStyle.FULLSCREEN ? screen : bounds;
        }

        @Override
        public void setMouseConfined(boolean confined) {
            mouseConfined = confined;
            confinementUpdates++;
        }

        @Override
        public void updateMouseConfinement() {}
    }
}
