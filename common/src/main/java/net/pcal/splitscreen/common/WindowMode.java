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

package net.pcal.splitscreen.common;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static net.pcal.splitscreen.common.WindowStyle.FULLSCREEN;
import static net.pcal.splitscreen.common.WindowStyle.SPLITSCREEN;
import static net.pcal.splitscreen.common.WindowStyle.WINDOWED;

/**
 * A named, user-selectable rule for sizing and positioning the main minecraft
 * window.  It can describe what the window shape ought to be given a screen
 * resolution.
 *
 * @author pcal
 * @since 0.0.1
 */
record WindowMode(
        String name,
        WindowStyle style,
        Function<MinecraftWindow.Rectangle, Integer> xFn,
        Function<MinecraftWindow.Rectangle, Integer> yFn,
        Function<MinecraftWindow.Rectangle, Integer> widthFn,
        Function<MinecraftWindow.Rectangle, Integer> heightFn) {

    /**
     * @return an enum indicating the style for windows in this mode.
     */
    WindowStyle getStyle() {
        return style;
    }


    /**
     * A name for this mode in the config file.
     */
    String getName() {
        return name;
    }

    /**
     * @return what the bounding rectangle for the given window would be if
     * this mode's rules were applied to it, or null if the bounds are unavailable.
     */
    MinecraftWindow.Rectangle getRepositionedBoundsFor(MinecraftWindow window) {
        if (style == FULLSCREEN) {
            return new MinecraftWindow.Rectangle(-1, -1, -1, -1);
        }
        final MinecraftWindow.Rectangle bounds = style == SPLITSCREEN ? window.getScreenBounds() : window.getWindowBounds();
        if (bounds == null) return null;
        return new MinecraftWindow.Rectangle(xFn.apply(bounds), yFn.apply(bounds), widthFn.apply(bounds), heightFn.apply(bounds));
    }

    static List<WindowMode> getModes(int gap) {
        final List<WindowMode> modes = new ArrayList<>();
        addMode(modes, "WINDOWED", WINDOWED, r -> r.x(), r -> r.y(), r -> r.width(), r -> r.height());
        addMode(modes, "LEFT", SPLITSCREEN, r -> r.x(), r -> r.y(), r -> r.width() / 2 - gap, r -> r.height());
        addMode(modes, "RIGHT", SPLITSCREEN, r -> r.x() + r.width() / 2 + gap, r -> r.y(), r -> r.width() / 2 - gap, r -> r.height());
        addMode(modes, "TOP", SPLITSCREEN, r -> r.x(), r -> r.y(), r -> r.width(), r -> r.height() / 2 - gap);
        addMode(modes, "BOTTOM", SPLITSCREEN, r -> r.x(), r -> r.y() + r.height() / 2 + gap, r -> r.width(), r -> r.height() / 2 - gap);
        addMode(modes, "TOP_LEFT", SPLITSCREEN, r -> r.x(), r -> r.y(),
                r -> r.width() / 2 - gap, r -> r.height() / 2 - gap);
        addMode(modes, "TOP_RIGHT", SPLITSCREEN, r -> r.x() + r.width() / 2 + gap, r -> r.y(),
                r -> r.width() / 2 - gap, r -> r.height() / 2 - gap);
        addMode(modes, "BOTTOM_LEFT", SPLITSCREEN, r -> r.x(), r -> r.y() + r.height() / 2 + gap,
                r -> r.width() / 2 - gap, r -> r.height() / 2 - gap);
        addMode(modes, "BOTTOM_RIGHT", SPLITSCREEN, r -> r.x() + r.width() / 2 + gap, r -> r.y() + r.height() / 2 + gap,
                r -> r.width() / 2 - gap, r -> r.height() / 2 - gap);
        addMode(modes, "FULLSCREEN", FULLSCREEN, no(), no(), no(), no());
        return modes;
    }

    // ======================================================================
    // Private

    private static void addMode(List<WindowMode> modes,
                                String name,
                                WindowStyle style,
                                Function<MinecraftWindow.Rectangle, Integer> xFn,
                                Function<MinecraftWindow.Rectangle, Integer> yFn,
                                Function<MinecraftWindow.Rectangle, Integer> widthFn,
                                Function<MinecraftWindow.Rectangle, Integer> heightFn) {
        modes.add(new WindowMode(name, style, xFn, yFn, widthFn, heightFn));
    }

    private static Function<MinecraftWindow.Rectangle, Integer> no() {
        return r -> -1;
    }

}
