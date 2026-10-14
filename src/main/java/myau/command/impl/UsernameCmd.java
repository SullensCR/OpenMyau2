package myau.command.impl;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.Arrays;
import myau.command.ChatCommand;
import myau.util.chat.ChatHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Session;
import net.minecraft.util.StringUtils;

public class UsernameCmd extends ChatCommand {
   private static final Minecraft minecraft = Minecraft.func_71410_x();

   public UsernameCmd() {
      super(new ArrayList<>(Arrays.asList("username", "name", "ign")));
   }

   @Override
   public void execute(ArrayList<String> arguments) {
      Session session = minecraft.func_110432_I();
      if (session != null && !StringUtils.func_151246_b(session.func_111285_a())) {
         String username = session.func_111285_a();

         try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(username), null);
            ChatHelper.addFormattedMessage(String.format("%sYour username has been copied to the clipboard (&o%s&r)&r", myau.util.chat.ChatPrefix.get(), username));
         } catch (Exception var5) {
            ChatHelper.addFormattedMessage(String.format("%sFailed to copy&r", myau.util.chat.ChatPrefix.get()));
         }
      }
   }
}
