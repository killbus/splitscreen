package net.pcal.splitscreen.common;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MouseConfinementTest {
    private boolean focused;
    private boolean succeeds = true;
    private final List<Boolean> grabs = new ArrayList<>();
    private final MouseConfinement confinement = new MouseConfinement(() -> focused, confined -> {
        assertTrue(focused, "Background instances must never update the native grab");
        grabs.add(confined);
        return succeeds;
    });

    @Test
    void backgroundStartupWaitsForFocusAndStableFramesDoNotRepeatGrab() {
        confinement.setConfined(true);
        confinement.updateFocus();
        assertEquals(List.of(), grabs);

        focused = true;
        confinement.updateFocus();
        confinement.updateFocus();
        confinement.updateFocus();
        assertEquals(List.of(true), grabs);
    }

    @Test
    void focusLossAndBackgroundMenuReleaseNeverTouchNativeGrab() {
        focused = true;
        confinement.setConfined(true);
        focused = false;
        confinement.updateFocus();
        confinement.setConfined(true);
        assertEquals(List.of(true), grabs);

        focused = true;
        confinement.updateFocus();
        assertEquals(List.of(true, true), grabs);
    }

    @Test
    void exitingSplitInBackgroundDefersReleaseUntilFocusReturns() {
        focused = true;
        confinement.setConfined(true);
        focused = false;
        confinement.setConfined(false);
        confinement.setConfined(false);
        confinement.updateFocus();
        assertEquals(List.of(true), grabs);

        focused = true;
        confinement.updateFocus();
        confinement.updateFocus();
        focused = false;
        confinement.updateFocus();
        focused = true;
        confinement.updateFocus();
        assertEquals(List.of(true, false), grabs);
    }

    @Test
    void menuReleaseReappliesGrabEvenWhenDesiredStateIsUnchanged() {
        focused = true;
        confinement.setConfined(true);
        confinement.setConfined(true);
        confinement.updateFocus();
        assertEquals(List.of(true, true), grabs);
    }

    @Test
    void ordinaryWindowFocusDoesNotAlterOtherMouseConstraints() {
        confinement.setConfined(false);
        focused = true;
        confinement.updateFocus();
        confinement.setConfined(false);
        assertEquals(List.of(), grabs);
    }

    @Test
    void failedReleaseRetriesOnNextFocusGainWithoutRepeatingEveryFrame() {
        focused = true;
        confinement.setConfined(true);
        succeeds = false;
        confinement.setConfined(false);
        confinement.updateFocus();
        assertEquals(List.of(true, false), grabs);

        focused = false;
        confinement.updateFocus();
        succeeds = true;
        focused = true;
        confinement.updateFocus();
        confinement.updateFocus();
        assertEquals(List.of(true, false, false), grabs);
    }

    @Test
    void focusLostBetweenDetectionAndApplyDefersNativeCall() {
        int[] checks = {0};
        List<Boolean> applied = new ArrayList<>();
        MouseConfinement racing = new MouseConfinement(
                () -> {
                    int check = ++checks[0];
                    return check == 2 || check >= 4;
                },
                confined -> {
                    applied.add(confined);
                    return true;
                });
        racing.setConfined(true); // background
        racing.updateFocus(); // gain observed, then lost before apply
        assertEquals(List.of(), applied);

        racing.updateFocus(); // stable focus returns
        racing.updateFocus();
        assertEquals(List.of(true), applied);
    }
}
