package net.pcal.splitscreen.common;

import net.pcal.splitscreen.common.MinecraftWindow.Rectangle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

class WindowModeTest {

    private static final List<Rectangle> SCREENS = List.of(
            new Rectangle(0, 0, 1920, 1080),
            new Rectangle(1920, 0, 1920, 1080),
            new Rectangle(-1920, 0, 1920, 1080),
            new Rectangle(0, -1080, 1920, 1080),
            new Rectangle(0, 1080, 1920, 1080),
            new Rectangle(1920, -1080, 1920, 1080),
            new Rectangle(-1920, 1080, 1920, 1080));

    @ParameterizedTest(name = "{0}, gap={1}")
    @CsvSource({
            "LEFT,         0,   0,   0,  960, 1080",
            "RIGHT,        0, 960,   0,  960, 1080",
            "TOP,          0,   0,   0, 1920,  540",
            "BOTTOM,       0,   0, 540, 1920,  540",
            "TOP_LEFT,     0,   0,   0,  960,  540",
            "TOP_RIGHT,    0, 960,   0,  960,  540",
            "BOTTOM_LEFT,  0,   0, 540,  960,  540",
            "BOTTOM_RIGHT, 0, 960, 540,  960,  540",
            "LEFT,         1,   0,   0,  959, 1080",
            "RIGHT,        1, 961,   0,  959, 1080",
            "TOP,          1,   0,   0, 1920,  539",
            "BOTTOM,       1,   0, 541, 1920,  539",
            "TOP_LEFT,     1,   0,   0,  959,  539",
            "TOP_RIGHT,    1, 961,   0,  959,  539",
            "BOTTOM_LEFT,  1,   0, 541,  959,  539",
            "BOTTOM_RIGHT, 1, 961, 541,  959,  539"
    })
    void splitScreenUsesOneSnapshotIncludingScreenOrigin(String name, int gap,
                                                        int x, int y, int width, int height) {
        for (Rectangle screen : SCREENS) {
            final ChangingWindow window = new ChangingWindow(screen, null);
            final Rectangle expected = new Rectangle(screen.x() + x, screen.y() + y, width, height);

            assertEquals(expected, mode(name, gap).getRepositionedBoundsFor(window),
                    () -> "Screen: " + screen);
            assertEquals(1, window.screenQueries);
            assertEquals(0, window.windowQueries);
        }
    }

    @ParameterizedTest(name = "{0}, gap={1}")
    @CsvSource({
            "RIGHT,        0,  -961, 1081,  960, 1081",
            "RIGHT,        1,  -960, 1081,  959, 1081",
            "BOTTOM,       0, -1921, 1621, 1921,  540",
            "BOTTOM,       1, -1921, 1622, 1921,  539",
            "TOP_LEFT,     0, -1921, 1081,  960,  540",
            "TOP_LEFT,     1, -1921, 1081,  959,  539",
            "BOTTOM_RIGHT, 0,  -961, 1621,  960,  540",
            "BOTTOM_RIGHT, 1,  -960, 1622,  959,  539"
    })
    void oddDimensionsKeepExistingRounding(String name, int gap,
                                          int x, int y, int width, int height) {
        final ChangingWindow window = new ChangingWindow(new Rectangle(-1921, 1081, 1921, 1081), null);

        assertEquals(new Rectangle(x, y, width, height), mode(name, gap).getRepositionedBoundsFor(window));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void windowedPreservesOneWindowSnapshot(int gap) {
        final Rectangle bounds = new Rectangle(-321, 57, 853, 479);
        final ChangingWindow window = new ChangingWindow(SCREENS.get(0), bounds);

        assertEquals(bounds, mode("WINDOWED", gap).getRepositionedBoundsFor(window));
        assertEquals(0, window.screenQueries);
        assertEquals(1, window.windowQueries);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void fullscreenReturnsPlaceholderWithoutQueryingBounds(int gap) {
        final ChangingWindow window = new ChangingWindow(null, null);

        assertEquals(new Rectangle(-1, -1, -1, -1), mode("FULLSCREEN", gap).getRepositionedBoundsFor(window));
        assertEquals(0, window.screenQueries);
        assertEquals(0, window.windowQueries);
    }

    @ParameterizedTest
    @ValueSource(strings = {"LEFT", "RIGHT", "TOP", "BOTTOM", "TOP_LEFT", "TOP_RIGHT", "BOTTOM_LEFT", "BOTTOM_RIGHT"})
    void unavailableScreenReturnsNull(String name) {
        final ChangingWindow window = new ChangingWindow(null, new Rectangle(10, 20, 800, 600));

        assertNull(mode(name, 1).getRepositionedBoundsFor(window));
        assertEquals(1, window.screenQueries);
        assertEquals(0, window.windowQueries);
    }

    @Test
    void modeNamesAndOrderStayUnchanged() {
        assertEquals(List.of("WINDOWED", "LEFT", "RIGHT", "TOP", "BOTTOM", "TOP_LEFT", "TOP_RIGHT",
                        "BOTTOM_LEFT", "BOTTOM_RIGHT", "FULLSCREEN"),
                WindowMode.getModes(0).stream().map(WindowMode::getName).toList());
    }

    private static WindowMode mode(String name, int gap) {
        return WindowMode.getModes(gap).stream()
                .filter(mode -> mode.getName().equals(name))
                .findFirst().orElseThrow();
    }

    private static final class ChangingWindow implements MinecraftWindow {
        private static final Rectangle CHANGED_BOUNDS = new Rectangle(31, 47, 701, 503);

        private final Rectangle screenBounds;
        private final Rectangle windowBounds;
        private int screenQueries;
        private int windowQueries;

        private ChangingWindow(Rectangle screenBounds, Rectangle windowBounds) {
            this.screenBounds = screenBounds;
            this.windowBounds = windowBounds;
        }

        @Override
        public Rectangle getScreenBounds() {
            return ++screenQueries == 1 ? screenBounds : CHANGED_BOUNDS;
        }

        @Override
        public Rectangle getWindowBounds() {
            return ++windowQueries == 1 ? windowBounds : CHANGED_BOUNDS;
        }

        @Override
        public void reposition(WindowStyle style, Rectangle newBounds) {
            fail("Calculating bounds must not reposition the window");
        }
    }
}
