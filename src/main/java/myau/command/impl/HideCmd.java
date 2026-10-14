package myau.command.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;
import myau.command.ChatCommand;
import myau.core.ClientContext;
import myau.module.ClientModule;
import myau.util.chat.ChatHelper;

public class HideCmd extends ChatCommand {
   public HideCmd() {
      super(new ArrayList<>(Arrays.asList("hide", "h")));
   }

   @Override
   public void execute(ArrayList<String> arguments) {
      if (arguments.size() < 2) {
         ChatHelper.addFormattedMessage(String.format("%sUsage: .%s <&omodule&r>&r", myau.util.chat.ChatPrefix.get(), arguments.get(0).toLowerCase(Locale.ROOT)));
      } else {
         String moduleName = arguments.get(1);
         if (!moduleName.equals("*")) {
            ClientModule module = ClientContext.moduleManager.getModule(moduleName);
            if (module == null) {
               ChatHelper.addFormattedMessage(String.format("%sModule &o%s&r not found&r", myau.util.chat.ChatPrefix.get(), moduleName));
            } else if (module.isHidden()) {
               ChatHelper.addFormattedMessage(String.format("%s&o%s&r is already hidden in HUD&r", myau.util.chat.ChatPrefix.get(), module.getName()));
            } else {
               module.setHidden(true);
               ChatHelper.addFormattedMessage(String.format("%s&o%s&r has been hidden in HUD&r", myau.util.chat.ChatPrefix.get(), module.getName()));
            }
         } else {
            for (ClientModule module : ClientContext.moduleManager.modules.values()) {
               module.setHidden(true);
            }

            ChatHelper.addFormattedMessage(String.format("%sAll modules have been hidden in HUD&r", myau.util.chat.ChatPrefix.get()));
         }
      }
   }

   @Override
   public ArrayList<String> completeArguments(ArrayList<String> arguments) {
      return arguments.size() != 2
         ? new ArrayList<>()
         : ClientContext.moduleManager
            .modules
            .values()
            .stream()
            .filter(module -> !module.isHidden())
            .map(module -> module.getName().toLowerCase(Locale.ROOT))
            .collect(Collectors.toCollection(ArrayList::new));
   }
}
