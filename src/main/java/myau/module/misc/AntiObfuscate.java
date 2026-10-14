package myau.module.misc;

import myau.module.ClientModule;

public class AntiObfuscate extends ClientModule {
   public AntiObfuscate() {
      super("AntiObfuscate", false, 0, true);
   }

   public String formatText(String text) {
      return text.replaceAll("§k", "");
   }
}
