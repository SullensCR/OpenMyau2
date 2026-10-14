package myau.command.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import myau.command.ChatCommand;
import myau.core.ClientContext;
import myau.util.chat.ChatFormat;
import myau.util.chat.ChatHelper;

public class FriendCmd extends ChatCommand {
   public FriendCmd() {
      super(new ArrayList<>(Arrays.asList("friend", "f")));
   }

   @Override
   public void execute(ArrayList<String> arguments) {
      if (arguments.size() < 2) {
         this.showUsage(arguments.get(0));
      } else {
         String var2 = arguments.get(1).toLowerCase(Locale.ROOT);
         switch (var2) {
            case "add":
               if (arguments.size() < 3) {
                  this.showNameUsage(arguments.get(0), "add");
                  return;
               }

               String addedName = ClientContext.friendList.add(arguments.get(2));
               if (addedName == null) {
                  ChatHelper.addFormattedMessage(String.format("%s&o%s&r is already in your friend list&r", myau.util.chat.ChatPrefix.get(), arguments.get(2)));
               } else {
                  ChatHelper.addFormattedMessage(String.format("%sAdded &o%s&r to your friend list&r", myau.util.chat.ChatPrefix.get(), addedName));
               }

               return;
            case "remove":
               if (arguments.size() < 3) {
                  this.showNameUsage(arguments.get(0), "remove");
                  return;
               }

               String removedName = ClientContext.friendList.remove(arguments.get(2));
               if (removedName == null) {
                  ChatHelper.addFormattedMessage(String.format("%s&o%s&r is not in your friend list&r", myau.util.chat.ChatPrefix.get(), arguments.get(2)));
               } else {
                  ChatHelper.addFormattedMessage(String.format("%sRemoved &o%s&r from your friend list&r", myau.util.chat.ChatPrefix.get(), removedName));
               }

               return;
            case "list":
               ArrayList<String> entries = ClientContext.friendList.entries();
               if (entries.isEmpty()) {
                  ChatHelper.addFormattedMessage(String.format("%sNo friends&r", myau.util.chat.ChatPrefix.get()));
                  return;
               }

               ChatHelper.addFormattedMessage(String.format("%sFriends:&r", myau.util.chat.ChatPrefix.get()));

               for (String entry : entries) {
                  ChatHelper.addRawMessage(String.format(ChatFormat.formatText("   &o%s&r"), entry));
               }

               return;
            case "clear":
               ClientContext.friendList.clear();
               ChatHelper.addFormattedMessage(String.format("%sCleared your friend list&r", myau.util.chat.ChatPrefix.get()));
               return;
            default:
               this.showUsage(arguments.get(0));
         }
      }
   }

   private void showUsage(String alias) {
      ChatHelper.addFormattedMessage(String.format("%sUsage: .%s <&oadd&r/&oremove&r/&olist&r/&oclear&r>&r", myau.util.chat.ChatPrefix.get(), alias.toLowerCase(Locale.ROOT)));
   }

   private void showNameUsage(String alias, String action) {
      ChatHelper.addFormattedMessage(String.format("%sUsage: .%s %s <&oname&r>&r", myau.util.chat.ChatPrefix.get(), alias.toLowerCase(Locale.ROOT), action));
   }

   @Override
   public ArrayList<String> completeArguments(ArrayList<String> arguments) {
      if (arguments.size() == 2) {
         return new ArrayList<>(Arrays.asList("add", "remove", "list", "clear"));
      } else {
         if (arguments.size() == 3) {
            String action = arguments.get(1).toLowerCase(Locale.ROOT);
            if (action.equals("add")) {
               return null;
            }

            if (action.equals("remove")) {
               return ClientContext.friendList.entries();
            }
         }

         return new ArrayList<>();
      }
   }
}
