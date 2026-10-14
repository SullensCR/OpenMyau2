package myau.util.item;

import java.util.ArrayList;
import java.util.Map;
import myau.util.chat.ChatFormat;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;

public final class EnchantDescriber {
   private static final Map<Integer, EnchantTag> ENCHANTMENT_LABELS = new EnchantTagTable();

   private EnchantDescriber() {
   }

   private static ChatFormat colorForLevel(int level, int maximumLevel) {
      if (level > maximumLevel) {
         return ChatFormat.LIGHT_PURPLE;
      } else if (level == maximumLevel) {
         return ChatFormat.RED;
      } else {
         switch (level) {
            case 1:
               return ChatFormat.AQUA;
            case 2:
               return ChatFormat.GREEN;
            case 3:
               return ChatFormat.YELLOW;
            case 4:
               return ChatFormat.GOLD;
            default:
               return ChatFormat.GRAY;
         }
      }
   }

   public static ArrayList<String> labels(ItemStack stack) {
      ArrayList<String> labels = new ArrayList<>();
      NBTTagList enchantments = stack.func_77973_b() == Items.field_151134_bR ? Items.field_151134_bR.func_92110_g(stack) : stack.func_77986_q();
      if (enchantments == null) {
         return labels;
      } else {
         for (int index = 0; index < enchantments.func_74745_c(); index++) {
            int enchantmentId = enchantments.func_150305_b(index).func_74762_e("id");
            EnchantTag label = ENCHANTMENT_LABELS.get(enchantmentId);
            if (label != null) {
               short level = enchantments.func_150305_b(index).func_74765_d("lvl");
               labels.add(ChatFormat.formatText(String.format("&r%s%s%d&r", label.abbreviation, colorForLevel(level, label.maxLevel), level)));
            }
         }

         return labels;
      }
   }
}
