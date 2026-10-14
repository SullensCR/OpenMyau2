package myau.command.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import myau.command.ChatCommand;
import myau.core.ClientContext;
import myau.module.ClientModule;
import myau.setting.BooleanOption;
import myau.setting.ChoiceOption;
import myau.setting.Option;
import myau.util.chat.ChatHelper;

public class ModuleSettingsCmd extends ChatCommand {
   public ModuleSettingsCmd() {
      super(new ArrayList<>(ClientContext.moduleManager.modules.values().stream().map(ClientModule::getName).collect(Collectors.toList())));
   }

   @Override
   public void execute(ArrayList<String> arguments) {
      ClientModule module = ClientContext.moduleManager.getModule(arguments.get(0));
      if (arguments.size() >= 2) {
         this.handleSetting(module, arguments);
      } else {
         List<Option<?>> settings = ClientContext.settingManager.settingsByModule.get(module.getClass());
         if (settings == null) {
            this.showNoProperties(module);
         } else {
            List<Option<?>> visibleSettings = settings.stream().filter(Option::isVisible).collect(Collectors.toList());
            if (visibleSettings.isEmpty()) {
               this.showNoProperties(module);
            } else {
               ChatHelper.addFormattedMessage(String.format("%s%s:&r", myau.util.chat.ChatPrefix.get(), module.getDisplayText()));

               for (Option<?> setting : visibleSettings) {
                  ChatHelper.addFormattedMessage(String.format("&7»&r %s: %s&r", setting.getName(), setting.getDisplayValue()));
               }
            }
         }
      }
   }

   private void handleSetting(ClientModule module, ArrayList<String> arguments) {
      Option<?> setting = ClientContext.settingManager.findSetting(module, arguments.get(1));
      if (setting == null) {
         ChatHelper.addFormattedMessage(String.format("%s%s has no property &o%s&r", myau.util.chat.ChatPrefix.get(), module.getName(), arguments.get(1)));
      } else if (!setting.isVisible()) {
         ChatHelper.addFormattedMessage(String.format("%s%s: &o%s&r is not available&r", myau.util.chat.ChatPrefix.get(), module.getName(), setting.getName()));
      } else if (arguments.size() < 3 && !(setting instanceof BooleanOption)) {
         ChatHelper.addFormattedMessage(
            String.format(
               "%s%s: &o%s&r is set to %s&r (%s)&r", myau.util.chat.ChatPrefix.get(), module.getName(), setting.getName(), setting.getDisplayValue(), setting.getUsage()
            )
         );
      } else {
         String input = arguments.size() < 3 ? null : String.join(" ", arguments.subList(2, arguments.size()));

         try {
            if (setting.parseValue(input)) {
               ChatHelper.addFormattedMessage(
                  String.format("%s%s: &o%s&r has been set to %s&r", myau.util.chat.ChatPrefix.get(), module.getName(), setting.getName(), setting.getDisplayValue())
               );
               return;
            }
         } catch (Exception var6) {
         }

         ChatHelper.addFormattedMessage(String.format("%sInvalid value for property &o%s&r (%s)&r", myau.util.chat.ChatPrefix.get(), setting.getName(), setting.getUsage()));
      }
   }

   private void showNoProperties(ClientModule module) {
      ChatHelper.addFormattedMessage(String.format("%s%s has no properties&r", myau.util.chat.ChatPrefix.get(), module.getDisplayText()));
   }

   @Override
   public ArrayList<String> completeArguments(ArrayList<String> arguments) {
      ClientModule module = ClientContext.moduleManager.getModule(arguments.get(0));
      if (module == null) {
         return new ArrayList<>();
      } else if (arguments.size() == 2) {
         List<Option<?>> settings = ClientContext.settingManager.settingsByModule.get(module.getClass());
         return settings == null
            ? new ArrayList<>()
            : settings.stream()
               .filter(Option::isVisible)
               .map(settingx -> settingx.getName().toLowerCase(Locale.ROOT))
               .collect(Collectors.toCollection(ArrayList::new));
      } else {
         if (arguments.size() == 3) {
            Option<?> setting = ClientContext.settingManager.findSetting(module, arguments.get(1));
            if (setting instanceof ChoiceOption && setting.isVisible()) {
               return Arrays.stream(((ChoiceOption)setting).getModes())
                  .map(mode -> mode.toLowerCase(Locale.ROOT))
                  .collect(Collectors.toCollection(ArrayList::new));
            }
         }

         return new ArrayList<>();
      }
   }
}
