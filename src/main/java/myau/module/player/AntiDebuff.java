package myau.module.player;

import myau.module.ClientModule;
import myau.setting.BooleanOption;

public class AntiDebuff extends ClientModule {
   public final BooleanOption blindness = new BooleanOption("blindness", true);
   public final BooleanOption nausea = new BooleanOption("nausea", true);

   public AntiDebuff() {
      super("AntiDebuff", false, 0);
   }
}
