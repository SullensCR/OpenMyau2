package myau.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import myau.event.Listen;
import myau.event.lifecycle.OnPostTick;
import myau.event.network.OnPacketSend;
import myau.mixin.accessor.C14PacketTabCompleteAccessor;
import myau.util.chat.ChatHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.network.play.client.C01PacketChatMessage;
import net.minecraft.network.play.client.C14PacketTabComplete;

public class CommandRegistry {
   private static final Minecraft minecraft = Minecraft.func_71410_x();
   private static final char COMMAND_PREFIX = '.';
   private String[] pendingCompletions;
   public final ArrayList<ChatCommand> commands = new ArrayList<>();

   private void executeCommand(String message) {
      ArrayList<String> arguments = new ArrayList<>(Arrays.asList(message.substring(1).trim().split("\\s+")));
      if (arguments.isEmpty()) {
         ChatHelper.addFormattedMessage(String.format("%sUnknown command&r", myau.util.chat.ChatPrefix.get()));
      } else {
         String commandName = arguments.get(0).toLowerCase(Locale.ROOT);
         if (commandName.isEmpty()) {
            ChatHelper.addFormattedMessage(String.format("%sUnknown command&r", myau.util.chat.ChatPrefix.get()));
         } else {
            for (ChatCommand command : this.commands) {
               for (String alias : command.aliases) {
                  if (commandName.equals(alias)) {
                     command.execute(arguments);
                     return;
                  }
               }
            }

            ChatHelper.addFormattedMessage(String.format("%sUnknown command (&o%s&r)&r", myau.util.chat.ChatPrefix.get(), commandName));
         }
      }
   }

   private String[] completeCommand(String message) {
      ArrayList<String> arguments = new ArrayList<>(Arrays.asList(message.substring(1).split("\\s+", -1)));
      String commandName = arguments.get(0).toLowerCase(Locale.ROOT);
      if (arguments.size() == 1) {
         ArrayList<String> matches = new ArrayList<>();

         for (ChatCommand command : this.commands) {
            for (String alias : command.aliases) {
               if (alias.startsWith(commandName)) {
                  matches.add(String.format(".%s", alias));
               }
            }
         }

         return matches.toArray(new String[0]);
      } else {
         for (ChatCommand command : this.commands) {
            for (String aliasx : command.aliases) {
               if (commandName.equals(aliasx)) {
                  ArrayList<String> completions = command.completeArguments(arguments);
                  if (completions == null) {
                     return null;
                  }

                  String prefix = arguments.get(arguments.size() - 1).toLowerCase(Locale.ROOT);
                  return completions.stream().filter(completion -> completion.toLowerCase(Locale.ROOT).startsWith(prefix)).toArray(String[]::new);
               }
            }
         }

         return new String[0];
      }
   }

   public boolean isClientCommand(String message) {
      return message != null && message.length() >= 2 && message.charAt(0) == '.' && Character.isLetterOrDigit(message.charAt(1));
   }

   @Listen(
      priority = 150
   )
   public void onPacketSend(OnPacketSend event) {
      if (!event.isCancelled()) {
         if (event.getPacket() instanceof C01PacketChatMessage) {
            C01PacketChatMessage packet = (C01PacketChatMessage)event.getPacket();
            String message = packet.func_149439_c();
            if (this.isClientCommand(message)) {
               event.setCancelled(true);
               this.executeCommand(message);
            }
         } else if (event.getPacket() instanceof C14PacketTabComplete) {
            C14PacketTabComplete packet = (C14PacketTabComplete)event.getPacket();
            String message = packet.func_149419_c();
            if (this.isClientCommand(message)) {
               this.pendingCompletions = this.completeCommand(message);
               if (this.pendingCompletions != null) {
                  event.setCancelled(true);
               } else {
                  String[] words = message.split("\\s+", -1);
                  String lastWord = words[words.length - 1];
                  ((C14PacketTabCompleteAccessor)packet).setMessage(lastWord.isEmpty() ? " " : lastWord);
               }
            }
         }
      }
   }

   @Listen(
      priority = -150
   )
   public void onPostTick(OnPostTick event) {
      if (this.pendingCompletions != null) {
         try {
            if (minecraft.field_71462_r instanceof GuiChat) {
               ((GuiChat)minecraft.field_71462_r).func_146406_a(this.pendingCompletions);
            }
         } catch (Exception var6) {
         } finally {
            this.pendingCompletions = null;
         }
      }
   }
}
