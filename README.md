# Splitscreen Support

Simple mod that makes it easier to position windows for local multiplayer.

With this mod installed, hitting the 'Toggle Fullscreen' button (F11) will cycle you through different half- and quarter-size
window positions, a full-monitor borderless window, and native fullscreen. It then saves your choice and applies the layout when you
restart the game. 

This makes it easier for you to start two or more copies of Minecraft and have the windows be positioned automatically 
for splitscreen.

In split-screen and borderless fullscreen layouts, the mouse cursor stays inside the active window, including in inventories and menus.
Use `Alt+Tab` (or your operating system's window switcher) to switch instances; losing focus releases the cursor.
Cycling back to ordinary windowed or native fullscreen mode removes this additional constraint.

This mod is designed to be used in conjunction with a Minecraft launcher (such as [PrismLauncher](https://prismlauncher.org/)) and
a gamepad mod (such [Midnight Controls](https://modrinth.com/mod/midnightcontrols))

## Usage Video

[![Video](https://github.com/pcal43/splitscreen/blob/pcal/readme-updates/etc/movie-thumb.png?raw=true)](https://youtu.be/QtsTT2dEED0)

## Suggested Setup 

### Stuff you need
* Two or more legal minecraft accounts
* [PrismLauncher](https://prismlauncher.org/)
* [Midnight Controls](https://modrinth.com/mod/midnightcontrols)
* [Splitscreen Support](https://modrinth.com/mod/splitscreen)
* Two or more gamepads
* One or more friends
* One big TV hooked up to your computer
* One couch

### What to do
* Create two or more minecraft instances in PrismLauncher, one for each account
* In each instance, install SplitScreen Support and MidnightControls
* Launch the instances and configure _each_ of them as follows:
  * **SplitScreen Support**: Press `F11` to position the windows
  * **MidnightControls**: Enable `Unfocused Input` and `Virtual Mouse` as [described here](https://www.midnightdust.eu/wiki/midnightcontrols/)
  * **Minecraft**: Press `F3+P` to disable [_Pause on Lost Focus_](https://minecraft.wiki/w/Debug_hotkey)
* Get a gamepad for each instance and configure MidnightControls
* In one of the instances, create a new world and 'Open to LAN'
* Connect the other instances to that one (e.g., localhost:25565)
* Get your friends and have a Minecraft party!

## Other mod features
* Username is displayed in the upper-left corner of the main menu (so you can tell who's who)

## One instance per monitor (Minecraft 26.3)

Use `BORDERLESS_FULLSCREEN` for a full-monitor view without native fullscreen. It uses the same ordinary,
undecorated SDL window path as the split-screen layouts, rather than requesting SDL fullscreen.
Native `FULLSCREEN` remains a separate mode with its existing behavior and fullscreen settings.

Move each instance's ordinary window onto its intended monitor, then press `F11` to cycle to the new mode.
The end of the cycle is `BOTTOM_RIGHT` -> `BORDERLESS_FULLSCREEN` -> `FULLSCREEN` -> `WINDOWED`.
The borderless mode covers the whole monitor using SDL's desktop coordinates, ignores `gap`, and does not use
Minecraft's configured native fullscreen resolution. Returning to `WINDOWED` restores the ordinary window's
saved bounds for the current session. Layout changes do not explicitly request keyboard focus.
As with split-screen layouts, use `Alt+Tab` to switch instances; the menu cursor is confined only while focused.

The selected mode is saved as `mode=BORDERLESS_FULLSCREEN` in each instance's `config/splitscreen.properties`.
You can also set it while that instance is stopped. Existing mode names, including `FULLSCREEN`, are unchanged.
The config remembers the layout, not a monitor assignment: placement uses the monitor containing the window
at the time the layout is applied. After restarting, you may need to return to `WINDOWED` and move the window
to the intended monitor again.

Keeping both windows visible is separate from keeping both games running and accepting input. Disable
pause on lost focus (`F3+P`) and enable unfocused controller input in each instance as described above.
Window-manager behavior, background rendering and controller support still need testing on your system.

### Dual-monitor verification

Unit tests cover layout geometry, configuration and mode transitions, but not SDL or OS focus behavior.
Before relying on this setup, test with two instances on two monitors:

* Select `BORDERLESS_FULLSCREEN` in both, then alternate focus and switch to another application.
  Both game windows should remain visible rather than automatically minimize.
* Test monitors to the left or above the primary display, and monitors with different resolutions or scaling.
  Each window should cover only its intended monitor without a gap or visible decoration.
* Set a native fullscreen resolution different from the desktop resolution. The borderless layout should
  still cover the desktop, including after cycling through native `FULLSCREEN` and back.
* Cycle back to `WINDOWED` and verify the ordinary bounds and decorations are restored. Resize/move that
  window and repeat the cycle to verify the new bounds are remembered.
* Restart each instance and verify that the borderless mode is restored; monitor assignment is not persisted.
* Open an inventory or menu, switch focus with `Alt+Tab`, and verify that only the focused instance confines
  the cursor. Returning to `WINDOWED` or native `FULLSCREEN` should remove the extra confinement.
* With unfocused input enabled and pausing disabled, verify background gameplay and controllers separately.

## Adjusting the gap between windows
By default, the mod puts a small gap between windows.  You can change the width of this gap
(or make it `0`) by setting the value of the `gap` property in `config/splitscreen.properties`.
(This file doesn't exist until you quit minecraft for the first time with the mod running).

## Acknowledgements
* Video courtesy of [wormstweaker](https://www.youtube.com/@WORMSTweaker)
* Icon based on [Squares icons created by Rahul Kaklotar - Flaticon](https://www.flaticon.com/free-icons/squares)

## Questions?

If you have questions about this mod, please join the Discord server:

[https://discord.pcal.net](https://discord.pcal.net)

Comments have been disabled and I will **not** reply to private messages on Curseforge.

![System](https://github.com/pcal43/splitscreen/blob/main/etc/screenshot-0.png?raw=true)
