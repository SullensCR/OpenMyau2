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
