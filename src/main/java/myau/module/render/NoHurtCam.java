package myau.module.render;

import myau.module.ClientModule;
import myau.setting.PercentOption;

public class NoHurtCam extends ClientModule {
   public final PercentOption multiplier = new PercentOption("multiplier", 0);

   public NoHurtCam() {
      super("NoHurtCam", false, 0, true);
   }
}
