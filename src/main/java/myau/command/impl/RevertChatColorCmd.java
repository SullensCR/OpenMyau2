package myau.command.impl;

import java.util.ArrayList;
import java.util.Arrays;
import myau.command.ChatCommand;
import myau.util.chat.ChatHelper;
import myau.util.chat.ChatPrefix;

public class RevertChatColorCmd extends ChatCommand {
   public RevertChatColorCmd() {
      super(new ArrayList<>(Arrays.asList("revertmyauchatcolor")));
   }

   @Override
   public void execute(ArrayList<String> arguments) {
      boolean rainbow = !ChatPrefix.isRainbow();
      ChatPrefix.setRainbow(rainbow);
      ChatHelper.addFormattedMessage(
         String.format(
            "%sChat color: %s&r",
            ChatPrefix.get(),
            rainbow ? "&a&lRAINBOW" : "&b&lDEFAULT BLUE"
         )
      );
   }
}
