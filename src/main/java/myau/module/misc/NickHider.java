package myau.module.misc;

import java.util.regex.Matcher;
import myau.module.ClientModule;
import myau.setting.BooleanOption;
import myau.setting.TextOption;
import myau.util.chat.ChatFormat;
import net.minecraft.client.Minecraft;

public class NickHider extends ClientModule {
   private static final Minecraft minecraft = Minecraft.func_71410_x();
   public final TextOption nameSetting = new TextOption("name", "You");
   public final BooleanOption scoreboard = new BooleanOption("scoreboard", true);
   public final BooleanOption level = new BooleanOption("level", true);

   public NickHider() {
      super("NickHider", false, 0, true);
   }

   public String formatText(String text) {
      if (text != null && minecraft.field_71439_g != null) {
         if (this.scoreboard.getValue() && text.matches("§7\\d{2}/\\d{2}/\\d{2}(?:\\d{2})?  ?§8.*")) {
            text = text.replaceAll("§8", "§8§k").replaceAll("[^\\x00-\\x7F§]", "?");
         }

         return text.replaceAll(minecraft.field_71439_g.func_70005_c_(), Matcher.quoteReplacement(ChatFormat.formatText(this.nameSetting.getValue())));
      } else {
         return text;
      }
   }
}
