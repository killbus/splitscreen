package net.pcal.splitscreen.common;

import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

/** Keeps the requested cursor constraint without touching background windows. */
public final class MouseConfinement {
    private final BooleanSupplier focused;
    private final Predicate<Boolean> setGrab;
    private boolean confined;
    private boolean releasePending;
    private boolean wasFocused;

    public MouseConfinement(BooleanSupplier focused, Predicate<Boolean> setGrab) {
        this.focused = focused;
        this.setGrab = setGrab;
    }

    public void setConfined(boolean confined) {
        this.releasePending |= this.confined && !confined;
        this.confined = confined;
        apply();
    }

    public void updateFocus() {
        boolean focused = this.focused.getAsBoolean();
        if (focused != this.wasFocused) {
            this.wasFocused = focused;
            if (focused) apply();
        }
    }

    private void apply() {
        // On Windows, even releasing a background grab can undo another
        // instance's ClipCursor constraint. SDL itself handles focus loss.
        this.wasFocused = this.focused.getAsBoolean();
        if ((this.confined || this.releasePending) && this.wasFocused) {
            if (this.setGrab.test(this.confined)) this.releasePending = false;
        }
    }
}
