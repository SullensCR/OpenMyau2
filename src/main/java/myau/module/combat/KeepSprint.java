package myau.module.combat;

import myau.module.ClientModule;
import myau.setting.BooleanOption;
import myau.setting.PercentOption;
import net.minecraft.client.Minecraft;

public class KeepSprint extends ClientModule {
   private static final Minecraft minecraft = Minecraft.func_71410_x();
   public final PercentOption slowdown = new PercentOption("slowdown", 0);
   public final BooleanOption groundOnly = new BooleanOption("ground-only", false);
   public final BooleanOption reachOnly = new BooleanOption("reach-only", false);

   public KeepSprint() {
      super("KeepSprint", false, 0);
   }

   public boolean shouldPreserveSprint() {
      boolean groundOnly = this.groundOnly.getValue();
      if (groundOnly && !minecraft.field_71439_g.field_70122_E) {
         return false;
      } else {
         return !this.reachOnly.getValue() ? true : minecraft.field_71476_x.field_72307_f.func_72438_d(minecraft.func_175606_aa().func_174824_e(1.0F)) > 3.0;
      }
   }
}
