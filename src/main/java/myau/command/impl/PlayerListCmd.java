package myau.command.impl;

import java.util.ArrayList;
import java.util.Arrays;
import myau.command.ChatCommand;
import myau.util.chat.ChatFormat;
import myau.util.chat.ChatHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;

public class PlayerListCmd extends ChatCommand {
   private static final Minecraft minecraft = Minecraft.func_71410_x();

   public PlayerListCmd() {
      super(new ArrayList<>(Arrays.asList("playerlist", "players")));
   }

   @Override
   public void execute(ArrayList<String> arguments) {
      ArrayList<String> players = new ArrayList<>();

      for (NetworkPlayerInfo playerInfo : minecraft.func_147114_u().func_175106_d()) {
         players.add(playerInfo.func_178845_a().getName().replace("§", "&"));
      }

      if (players.isEmpty()) {
         ChatHelper.addFormattedMessage(String.format("%sNo players&r", myau.util.chat.ChatPrefix.get()));
      } else {
         ChatHelper.addRawMessage(String.format(ChatFormat.formatText("%sPlayers:&r %s"), ChatFormat.formatText(myau.util.chat.ChatPrefix.get()), String.join(", ", players)));
      }
   }
}
