package myau.command.impl;

import java.awt.Desktop;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import myau.command.ChatCommand;
import myau.config.ConfigPreset;
import myau.core.ClientContext;
import myau.module.ClientModule;
import myau.setting.Option;
import myau.util.chat.ChatFormat;
import myau.util.chat.ChatHelper;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.event.ClickEvent.Action;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.apache.commons.io.filefilter.WildcardFileFilter;

public class ConfigCmd extends ChatCommand {
   private static final File CONFIG_DIRECTORY = new File("./config/Myau/");
   private static final FileFilter CONFIG_FILE_FILTER = new WildcardFileFilter("*.json", IOCase.INSENSITIVE);

   public ConfigCmd() {
      super(new ArrayList<>(Arrays.asList("config", "cfg", "c")));
   }

   @Override
   public void execute(ArrayList<String> arguments) {
      if (arguments.size() < 2) {
         String alias = arguments.get(0).toLowerCase(Locale.ROOT);
         ChatHelper.addFormattedMessage(
            String.format(
               "%sUsage: .%s &oload&r/&osave&r <&oname&r> | .%s &oreset&r | .%s &olist&r | .%s &ofolder&r", myau.util.chat.ChatPrefix.get(), alias, alias, alias, alias
            )
         );
      } else {
         String subcommand = arguments.get(1);
         if (subcommand.equalsIgnoreCase("l")) {
            subcommand = arguments.size() < 3 ? "list" : "load";
         }

         subcommand = subcommand.toLowerCase(Locale.ROOT);
         switch (subcommand) {
            case "load":
            case "reload":
               loadConfig(arguments);
               return;
            case "save":
            case "s":
               saveConfig(arguments);
               return;
            case "reset":
            case "clear":
               resetConfig();
               return;
            case "list":
               listConfigs();
               return;
            case "folder":
            case "f":
            case "directory":
            case "dir":
               openConfigDirectory();
               return;
            default:
               ChatHelper.addFormattedMessage(String.format("%sInvalid argument (&o%s&r)&r", myau.util.chat.ChatPrefix.get(), arguments.get(1)));
         }
      }
   }

   private static void loadConfig(List<String> arguments) {
      ConfigPreset profile = new ConfigPreset(arguments.size() < 3 ? ConfigPreset.activeProfileName : arguments.get(2));
      if (profile.load()) {
         ChatHelper.addFormattedMessage(String.format("%sConfig has been loaded (&a&o%s&r)&r", myau.util.chat.ChatPrefix.get(), profile.fileName()));
      } else {
         ChatHelper.addFormattedMessage(String.format("%sConfig couldn't be loaded (&c&o%s&r)&r", myau.util.chat.ChatPrefix.get(), profile.fileName()));
      }
   }

   private static void saveConfig(List<String> arguments) {
      ConfigPreset profile = new ConfigPreset(arguments.size() < 3 ? ConfigPreset.activeProfileName : arguments.get(2));
      if (profile.save()) {
         ChatHelper.addFormattedMessage(String.format("%sConfig has been saved (&a&o%s&r)&r", myau.util.chat.ChatPrefix.get(), profile.fileName()));
      } else {
         ChatHelper.addFormattedMessage(String.format("%sConfig couldn't be saved (&c&o%s&r)&r", myau.util.chat.ChatPrefix.get(), profile.fileName()));
      }
   }

   private static void resetConfig() {
      for (ClientModule module : ClientContext.moduleManager.modules.values()) {
         List<Option<?>> settings = ClientContext.settingManager.settingsByModule.get(module.getClass());
         if (settings != null) {
            for (Option<?> setting : settings) {
               setting.reset();
            }
         }

         module.resetHidden();
         module.resetKeyCode();
         module.setEnabled(false);
      }

      ChatHelper.addFormattedMessage(String.format("%sConfig has been reset to defaults&r", myau.util.chat.ChatPrefix.get()));
   }

   private static void listConfigs() {
      try {
         File[] files = CONFIG_DIRECTORY.listFiles(CONFIG_FILE_FILTER);
         if (files == null) {
            throw new IllegalStateException("Unable to list config directory");
         }

         if (files.length == 0) {
            ChatHelper.addFormattedMessage(String.format("%sNo configs found (&o%s&r)&r", myau.util.chat.ChatPrefix.get(), CONFIG_DIRECTORY));
            return;
         }

         Arrays.sort(files, LastModifiedFileComparator.LASTMODIFIED_REVERSE);
         ChatHelper.addFormattedMessage(String.format("%sConfigs:&r", myau.util.chat.ChatPrefix.get()));

         for (File file : files) {
            String displayText = ChatFormat.formatText(String.format("&7»&r &o%s&r", file.getName()));
            String command = String.format(".config load %s", FilenameUtils.removeExtension(file.getName()));
            ChatHelper.addComponent(
               new ChatComponentText(displayText)
                  .func_150255_a(
                     new ChatStyle()
                        .func_150241_a(new ClickEvent(Action.RUN_COMMAND, command))
                        .func_150209_a(new HoverEvent(net.minecraft.event.HoverEvent.Action.SHOW_TEXT, new ChatComponentText(command)))
                  )
            );
         }
      } catch (Exception var7) {
         ChatHelper.addFormattedMessage(String.format("%sFailed to read (&o%s&r)&r", myau.util.chat.ChatPrefix.get(), CONFIG_DIRECTORY));
      }
   }

   private static void openConfigDirectory() {
      try {
         Desktop.getDesktop().open(CONFIG_DIRECTORY);
      } catch (Exception var1) {
         ChatHelper.addFormattedMessage(String.format("%sFailed to open (&o%s&r)&r", myau.util.chat.ChatPrefix.get(), CONFIG_DIRECTORY));
      }
   }

   @Override
   public ArrayList<String> completeArguments(ArrayList<String> arguments) {
      if (arguments.size() == 2) {
         return new ArrayList<>(Arrays.asList("load", "reload", "l", "save", "s", "reset", "clear", "list", "folder", "f", "directory", "dir"));
      } else if (arguments.size() == 3) {
         String action = arguments.get(1).toLowerCase(Locale.ROOT);
         if (!action.equals("load") && !action.equals("reload") && !action.equals("l") && !action.equals("save") && !action.equals("s")) {
            return new ArrayList<>();
         } else {
            File[] files = CONFIG_DIRECTORY.listFiles(CONFIG_FILE_FILTER);
            if (files == null) {
               return new ArrayList<>();
            } else {
               Arrays.sort(files, LastModifiedFileComparator.LASTMODIFIED_REVERSE);
               return Arrays.stream(files).map(File::getName).map(FilenameUtils::removeExtension).collect(Collectors.toCollection(ArrayList::new));
            }
         }
      } else {
         return new ArrayList<>();
      }
   }
}
