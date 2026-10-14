package myau.command.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;
import myau.command.ChatCommand;
import myau.core.ClientContext;
import myau.module.ClientModule;
import myau.util.chat.ChatHelper;

public class ToggleCmd extends ChatCommand {
   public ToggleCmd() {
      super(new ArrayList<>(Arrays.asList("toggle", "t")));
   }

   @Override
   public void execute(ArrayList<String> arguments) {
      if (arguments.size() < 2) {
         ChatHelper.addFormattedMessage(String.format("%sUsage: .%s <&omodule&r>&r", myau.util.chat.ChatPrefix.get(), arguments.get(0).toLowerCase(Locale.ROOT)));
      } else {
         ClientModule module = ClientContext.moduleManager.getModule(arguments.get(1));
         if (module == null) {
            ChatHelper.addFormattedMessage(String.format("%sModule not found (&o%s&r)&r", myau.util.chat.ChatPrefix.get(), arguments.get(1)));
         } else {
            boolean shouldToggle = true;
            if (arguments.size() >= 3) {
               String requestedState = arguments.get(2);
               if (requestedState.equalsIgnoreCase("true") || requestedState.equalsIgnoreCase("on") || requestedState.equalsIgnoreCase("1")) {
                  shouldToggle = !module.isEnabled();
               } else if (requestedState.equalsIgnoreCase("false") || requestedState.equalsIgnoreCase("off") || requestedState.equalsIgnoreCase("0")) {
                  shouldToggle = module.isEnabled();
               }
            }

            if (shouldToggle && ClientContext.moduleManager.toggle(module)) {
               ChatHelper.addFormattedMessage(String.format("%s%s: %s&r", myau.util.chat.ChatPrefix.get(), module.getName(), module.isEnabled() ? "&a&lON" : "&c&lOFF"));
            }
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
            .map(module -> module.getName().toLowerCase(Locale.ROOT))
            .collect(Collectors.toCollection(ArrayList::new));
   }
}
