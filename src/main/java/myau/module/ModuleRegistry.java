package myau.module;

import java.util.LinkedHashMap;
import myau.module.render.HeadsUpDisplay;
import myau.util.MutableRef;
import myau.util.chat.ChatHelper;
import myau.util.input.InputHelper;
import myau.util.sound.SoundHelper;
import net.minecraft.client.Minecraft;

public class ModuleRegistry {
   private static final Minecraft minecraft = Minecraft.func_71410_x();
   private boolean toggleSoundPending;
   public final LinkedHashMap<Class<?>, ClientModule> modules = new LinkedHashMap<>();

   public ClientModule getModule(String name) {
      return this.modules.values().stream().filter(module -> module.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
   }

   public boolean toggle(ClientModule module) {
      boolean requestedState = !module.isEnabled();
      module.setEnabled(requestedState);
      return module.isEnabled() == requestedState;
   }

   public void update() {
      if (minecraft.field_71441_e != null && minecraft.field_71439_g != null && minecraft.field_71462_r == null) {
         HeadsUpDisplay hud = (HeadsUpDisplay)this.modules.get(HeadsUpDisplay.class);

         for (ClientModule module : this.modules.values()) {
            int keyCode = module.getKeyCode();
            if (keyCode == 0) {
               module.setPreviousKeyState(null);
            } else {
               boolean keyDown = InputHelper.isKeyDown(keyCode);
               MutableRef<Boolean> previousKeyState = module.getPreviousKeyState();
               if (previousKeyState != null && !previousKeyState.value && keyDown && this.toggle(module)) {
                  if (hud != null && hud.toggleAlerts.getValue()) {
                     ChatHelper.addFormattedMessage(String.format("%s%s: %s&r", myau.util.chat.ChatPrefix.get(), module.getName(), module.isEnabled() ? "&a&lON" : "&c&lOFF"));
                  }

                  if (hud != null && hud.toggleSounds.getValue()) {
                     this.toggleSoundPending = true;
                  }
               }

               module.setPreviousKeyState(new MutableRef<>(keyDown));
            }
         }

         if (this.toggleSoundPending) {
            this.toggleSoundPending = false;
            SoundHelper.play("random.click");
         }
      } else {
         this.resetKeyStates();
      }
   }

   private void resetKeyStates() {
      for (ClientModule module : this.modules.values()) {
         module.setPreviousKeyState(null);
      }

      this.toggleSoundPending = false;
   }
}
