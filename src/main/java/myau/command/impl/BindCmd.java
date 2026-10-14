package myau.command.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import myau.command.ChatCommand;
import myau.core.ClientContext;
import myau.module.ClientModule;
import myau.util.chat.ChatHelper;
import myau.util.input.InputHelper;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class BindCmd extends ChatCommand {
   public BindCmd() {
      super(new ArrayList<>(Arrays.asList("bind", "b")));
   }

   @Override
   public void execute(ArrayList<String> arguments) {
      if (arguments.size() < 3) {
         if (arguments.size() == 2 && (arguments.get(1).equalsIgnoreCase("list") || arguments.get(1).equalsIgnoreCase("l"))) {
            List<ClientModule> boundModules = ClientContext.moduleManager
               .modules
               .values()
               .stream()
               .filter(modulex -> modulex.getKeyCode() != 0)
               .collect(Collectors.toList());
            if (boundModules.isEmpty()) {
               ChatHelper.addFormattedMessage(String.format("%sNo binds&r", myau.util.chat.ChatPrefix.get()));
            } else {
               ChatHelper.addFormattedMessage(String.format("%sBinds:&r", myau.util.chat.ChatPrefix.get()));

               for (ClientModule module : boundModules) {
                  ChatHelper.addFormattedMessage(String.format("%s»&r %s&r", module.isHidden() ? "&8" : "&7", module.getDisplayText()));
               }
            }
         } else {
            String command = arguments.get(0).toLowerCase(Locale.ROOT);
            ChatHelper.addFormattedMessage(String.format("%sUsage: .%s <&omodule&r> <&okey&r>&r | .%s &olist&r", myau.util.chat.ChatPrefix.get(), command, command));
         }
      } else {
         int keyCode = Keyboard.getKeyIndex(arguments.get(2).toUpperCase());
         if (keyCode == 0) {
            int mouseButton = Mouse.getButtonIndex(arguments.get(2).toUpperCase());
            if (mouseButton != -1) {
               keyCode = mouseButton - 100;
            }
         }

         String moduleName = arguments.get(1);
         if (!moduleName.equals("*")) {
            ClientModule module = ClientContext.moduleManager.getModule(moduleName);
            if (module == null) {
               ChatHelper.addFormattedMessage(String.format("%sModule not found (&o%s&r)&r", myau.util.chat.ChatPrefix.get(), moduleName));
            } else {
               module.setKeyCode(keyCode);
               ChatHelper.addFormattedMessage(String.format("%sBound &o%s&r to &l[%s]&r", myau.util.chat.ChatPrefix.get(), module.getName(), InputHelper.getKeyName(keyCode)));
            }
         } else {
            for (ClientModule module : ClientContext.moduleManager.modules.values()) {
               module.setKeyCode(keyCode);
            }

            ChatHelper.addFormattedMessage(String.format("%sBound all modules to &l[%s]&r", myau.util.chat.ChatPrefix.get(), InputHelper.getKeyName(keyCode)));
         }
      }
   }

   @Override
   public ArrayList<String> completeArguments(ArrayList<String> arguments) {
      if (arguments.size() == 2) {
         ArrayList<String> completions = ClientContext.moduleManager
            .modules
            .values()
            .stream()
            .map(module -> module.getName().toLowerCase(Locale.ROOT))
            .collect(Collectors.toCollection(ArrayList::new));
         completions.add("list");
         completions.add("l");
         return completions;
      } else if (arguments.size() != 3) {
         return new ArrayList<>();
      } else {
         String moduleName = arguments.get(1);
         if (!moduleName.equals("*") && ClientContext.moduleManager.getModule(moduleName) == null) {
            return new ArrayList<>();
         } else {
            ArrayList<String> completions = new ArrayList<>();

            for (int keyCode = 0; keyCode < 256; keyCode++) {
               String keyName = Keyboard.getKeyName(keyCode);
               if (keyName != null) {
                  completions.add(keyName.toLowerCase(Locale.ROOT));
               }
            }

            for (int button = 0; button < Mouse.getButtonCount(); button++) {
               String buttonName = Mouse.getButtonName(button);
               if (buttonName != null) {
                  completions.add(buttonName.toLowerCase(Locale.ROOT));
               }
            }

            return completions;
         }
      }
   }
}
