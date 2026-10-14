package myau.command.impl;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import myau.command.ChatCommand;
import myau.util.chat.ChatHelper;
import net.minecraft.client.Minecraft;

public class VClipCmd extends ChatCommand {
   private static final Minecraft minecraft = Minecraft.func_71410_x();
   private static final DecimalFormat decimalFormat = new DecimalFormat("#.##", new DecimalFormatSymbols(Locale.US));

   public VClipCmd() {
      super(new ArrayList<>(Collections.singletonList("vclip")));
   }

   @Override
   public void execute(ArrayList<String> arguments) {
      if (arguments.size() < 2) {
         ChatHelper.addFormattedMessage(String.format("%sUsage: .%s <&odistance&r>&r", myau.util.chat.ChatPrefix.get(), arguments.get(0).toLowerCase(Locale.ROOT)));
      } else {
         double distance = 0.0;

         try {
            distance = Double.parseDouble(arguments.get(1));
         } catch (NumberFormatException var5) {
         }

         minecraft.field_71439_g
            .func_70634_a(minecraft.field_71439_g.field_70165_t, minecraft.field_71439_g.field_70163_u + distance, minecraft.field_71439_g.field_70161_v);
         ChatHelper.addFormattedMessage(String.format("%sClipped (%s blocks)", myau.util.chat.ChatPrefix.get(), decimalFormat.format(distance)));
      }
   }
}
