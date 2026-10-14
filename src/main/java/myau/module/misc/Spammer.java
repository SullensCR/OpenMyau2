package myau.module.misc;

import myau.event.Listen;
import myau.event.render.OnRenderOverlay;
import myau.module.ClientModule;
import myau.setting.FloatOption;
import myau.setting.IntegerOption;
import myau.setting.TextOption;
import myau.util.Stopwatch;
import net.minecraft.client.Minecraft;

public class Spammer extends ClientModule {
   private static final Minecraft minecraft = Minecraft.func_71410_x();
   private final Stopwatch timer = new Stopwatch();
   private int nextSuffixCharacter = 19968;
   public final TextOption text = new TextOption("text", "meow");
   public final FloatOption delay = new FloatOption("delay", 3.5F, 0.0F, 3600.0F);
   public final IntegerOption random = new IntegerOption("random", 0, 0, 10);

   public Spammer() {
      super("Spammer", false, 0);
   }

   @Listen
   public void onRenderOverlay(OnRenderOverlay event) {
      if (this.isEnabled()) {
         if (this.timer.hasElapsed((long)(this.delay.getValue() * 1000.0F))) {
            this.timer.reset();
            String message = this.text.getValue();
            int suffixLength = this.random.getValue();
            if (suffixLength <= 0) {
               minecraft.field_71439_g.func_71165_d(message);
            } else {
               message = String.format("%s ", message);

               for (int index = 0; index < suffixLength; index++) {
                  message = String.format("%s%s", message, (char)this.nextSuffixCharacter);
                  this.nextSuffixCharacter++;
               }

               minecraft.field_71439_g.func_71165_d(message);
            }
         }
      }
   }
}
