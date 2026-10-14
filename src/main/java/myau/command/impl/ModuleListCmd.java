package myau.command.impl;

import java.util.ArrayList;
import java.util.Arrays;
import myau.command.ChatCommand;
import myau.core.ClientContext;
import myau.module.ClientModule;
import myau.util.chat.ChatHelper;

public class ModuleListCmd extends ChatCommand {
   public ModuleListCmd() {
      super(new ArrayList<>(Arrays.asList("list", "l", "modules", "myau")));
   }

   @Override
   public void execute(ArrayList<String> arguments) {
      if (!ClientContext.moduleManager.modules.isEmpty()) {
         ChatHelper.addFormattedMessage(String.format("%s- Cracked by Skid Solutions&r", myau.util.chat.ChatPrefix.get()));

         for (ClientModule module : ClientContext.moduleManager.modules.values()) {
            ChatHelper.addFormattedMessage(String.format("%s»&r %s&r", module.isHidden() ? "&8" : "&7", module.getDisplayText()));
         }
      }
   }
}
