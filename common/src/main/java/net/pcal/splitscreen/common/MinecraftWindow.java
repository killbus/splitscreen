package net.pcal.splitscreen.common;

/**
 * Encapsulates some messiness in the mixin code for querying and
 * repositioning the minecraft window and the screen it's on.
 */
public interface MinecraftWindow {

    /**
     * @return the bounding rectangle for the window
     */
    Rectangle getWindowBounds();

    /**
     * @return the desktop bounds of the display containing the window, in window
     * coordinates (not framebuffer pixels), or null if the display is unavailable.
     */
    Rectangle getScreenBounds();


    /**
     * Reposition the window according to the given style and bounds.
     */
    void reposition(WindowStyle style, Rectangle newBounds);

    /**
     * Confine the cursor while this window has input focus, without changing
     * cursor visibility or relative mouse mode. Losing focus releases the
     * constraint; regaining focus restores it until confinement is disabled.
     */
    void setMouseConfined(boolean confined);

    /** Apply any deferred constraint when the window regains real input focus. */
    void updateMouseConfinement();

    record Rectangle(int x, int y, int width, int height) {}
}
