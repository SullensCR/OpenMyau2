package myau.command.impl;

import java.util.ArrayList;
import java.util.Arrays;
import myau.command.ChatCommand;
import myau.core.ClientContext;
import myau.util.chat.ChatHelper;

public class HelpCmd extends ChatCommand {
   public HelpCmd() {
      super(new ArrayList<>(Arrays.asList("help", "commands")));
   }

   @Override
   public void execute(ArrayList<String> arguments) {
      if (!ClientContext.moduleManager.modules.isEmpty()) {
         ChatHelper.addFormattedMessage(String.format("%sCommands:&r", myau.util.chat.ChatPrefix.get()));

         for (ChatCommand command : ClientContext.commandManager.commands) {
            if (!(command instanceof ModuleSettingsCmd)) {
               ChatHelper.addFormattedMessage(String.format("&7»&r .%s&r", String.join(" &7/&r .", command.aliases)));
            }
         }
      }
   }
}
