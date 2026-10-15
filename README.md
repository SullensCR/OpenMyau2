# Myau 260729
Full compilable myau 260729, given to us by anonymous. 

Check releases for jar 
- 
Skid solutions

## .revertmyauchatcolor

Toggles the mod's chat prefix between the default blue `[Myau]` and a
rainbow one (`[M(red)y(gold)a(yellow)u(green)]`, gray brackets, white text), that was in original myau.
Run the command again to flip back.

The choice persists in `config/Myau/chatcolor.json` in the minecraft dir
(`{"rainbow":true/false}`), so it survives restarts. All mod messages go
through `myau.util.chat.ChatPrefix.get()`, which reads that file.

## ClickGUI

**Right-Shift** opens the module menu (the `ClickGui` module's bind; rebind it like
any other module under Render). Ported from OpenMyau's Raven-style GUI.

Six columns — Combat, Movement, Render, Player, World, Misc — laid out side by side
with every category expanded by default.

- **Left-click** a module to toggle it; **right-click** it to open its settings.
- **Drag** a category header to move the column; the `+`/`-` collapses/expands it.
- Settings render inline: checkboxes, sliders (drag it, right-click the left/right
  half to step, click the label to type an exact value), mode cyclers
  (left/right-click to cycle), colour pickers, text fields, and a `Bind` row.
- On a `Bind` row, click then press a key or mouse button; `0` resets it to the
  module's default, `Esc` cancels.

Column positions and open/closed state persist to `config/Myau/clickgui.txt`;
delete that file to get the default layout back. The row needs roughly 600px of GUI
space, so a small window or GUI scale 2 can push the last column off-screen — use
GUI scale 1 if that happens.
