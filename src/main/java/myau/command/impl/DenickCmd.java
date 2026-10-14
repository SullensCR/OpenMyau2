package myau.command.impl;

import com.google.common.collect.Iterables;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Locale;
import myau.command.ChatCommand;
import myau.util.chat.ChatFormat;
import myau.util.chat.ChatHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;

public class DenickCmd extends ChatCommand {
   private static final Minecraft minecraft = Minecraft.func_71410_x();

   public DenickCmd() {
      super(new ArrayList<>(Collections.singletonList("denick")));
   }

   @Override
   public void execute(ArrayList<String> arguments) {
      if (arguments.size() < 2) {
         ChatHelper.addFormattedMessage(String.format("%sUsage: .%s <&oname&r>&r", myau.util.chat.ChatPrefix.get(), arguments.get(0).toLowerCase(Locale.ROOT)));
      } else {
         String requestedName = arguments.get(1);
         NetworkPlayerInfo playerInfo = minecraft.func_147114_u().func_175104_a(ChatFormat.formatText(requestedName));
         if (playerInfo == null) {
            ChatHelper.addRawMessage(
               String.format(ChatFormat.formatText("%sNo entity with name &o%s&r"), ChatFormat.formatText(myau.util.chat.ChatPrefix.get()), requestedName)
            );
         } else {
            GameProfile profile = playerInfo.func_178845_a();
            Property textures = (Property)Iterables.getFirst(profile.getProperties().get("textures"), null);
            if (textures == null) {
               ChatHelper.addRawMessage(
                  String.format(ChatFormat.formatText("%sNo textures for entity with name &o%s&r"), ChatFormat.formatText(myau.util.chat.ChatPrefix.get()), requestedName)
               );
            } else {
               String payload = new String(Base64.getDecoder().decode(textures.getValue().getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
               String profileName = extractJsonLikeField(payload, "profileName");
               String profileId = extractJsonLikeField(payload, "profileId");
               ChatHelper.addRawMessage(
                  String.format(
                     ChatFormat.formatText("%s%s&r -> %s (&o%s&r)&r"),
                     ChatFormat.formatText(myau.util.chat.ChatPrefix.get()),
                     profile.getName().replace("§", "&"),
                     profileName,
                     profileId
                  )
               );
               if (!profileId.isEmpty() && !profileId.equals("?")) {
                  Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(profileId), null);
               }
            }
         }
      }
   }

   private static String extractJsonLikeField(String payload, String fieldName) {
      String marker = fieldName + "\" : \"";
      return !payload.contains(marker) ? "?" : payload.split(marker, 2)[1].split("\"", 2)[0];
   }

   @Override
   public ArrayList<String> completeArguments(ArrayList<String> arguments) {
      return arguments.size() == 2 ? null : new ArrayList<>();
   }
}
