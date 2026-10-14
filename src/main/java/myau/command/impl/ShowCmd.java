package myau.command.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;
import myau.command.ChatCommand;
import myau.core.ClientContext;
import myau.module.ClientModule;
import myau.util.chat.ChatHelper;

public class ShowCmd extends ChatCommand {
   public ShowCmd() {
      super(new ArrayList<>(Arrays.asList("show", "s", "unhide")));
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
            } else if (!module.isHidden()) {
               ChatHelper.addFormattedMessage(String.format("%s&o%s&r is not hidden in HUD&r", myau.util.chat.ChatPrefix.get(), module.getName()));
            } else {
               module.setHidden(false);
               ChatHelper.addFormattedMessage(String.format("%s&o%s&r is no longer hidden in HUD&r", myau.util.chat.ChatPrefix.get(), module.getName()));
            }
         } else {
            for (ClientModule module : ClientContext.moduleManager.modules.values()) {
               module.setHidden(false);
            }

            ChatHelper.addFormattedMessage(String.format("%sAll modules are no longer hidden in HUD&r", myau.util.chat.ChatPrefix.get()));
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
            .filter(ClientModule::isHidden)
            .map(module -> module.getName().toLowerCase(Locale.ROOT))
            .collect(Collectors.toCollection(ArrayList::new));
   }
}
