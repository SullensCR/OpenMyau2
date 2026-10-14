package myau.module.movement;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import myau.module.ClientModule;
import myau.setting.BooleanOption;
import myau.setting.FloatOption;

public class Jesus extends ClientModule {
   private static final DecimalFormat speedFormat = new DecimalFormat("#.##", new DecimalFormatSymbols(Locale.US));
   public final FloatOption speed = new FloatOption("speed", 2.5F, 0.0F, 3.0F);
   public final BooleanOption noPush = new BooleanOption("no-push", true);
   public final BooleanOption groundOnly = new BooleanOption("ground-only", true);

   public Jesus() {
      super("Jesus", false, 0);
   }

   @Override
   public String[] getHudInfo() {
      return new String[]{speedFormat.format(this.speed.getValue())};
   }
}
