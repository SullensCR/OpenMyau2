package myau.command.impl;

import java.util.ArrayList;
import java.util.Arrays;
import myau.command.ChatCommand;
import myau.util.chat.ChatFormat;
import myau.util.chat.ChatHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;

public class ItemNameCmd extends ChatCommand {
   private static final Minecraft minecraft = Minecraft.func_71410_x();

   public ItemNameCmd() {
      super(new ArrayList<>(Arrays.asList("itemname", "item")));
   }

   @Override
   public void execute(ArrayList<String> arguments) {
      ItemStack stack = minecraft.field_71439_g.field_71071_by.func_70448_g();
      if (stack != null) {
         String displayName = stack.func_82833_r().replace('§', '&');
         String registryName = stack.func_77973_b().getRegistryName();
         String nbt = stack.func_77942_o() ? stack.func_77978_p().toString().replace('§', '&') : "";
         ChatHelper.addRawMessage(String.format("%s%s (%s) %s", ChatFormat.formatText(myau.util.chat.ChatPrefix.get()), displayName, registryName, nbt));
      }
   }
}
