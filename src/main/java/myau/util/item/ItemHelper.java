package myau.util.item;

import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import myau.util.world.BlockHelper;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.init.Items;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemBucket;
import net.minecraft.item.ItemEgg;
import net.minecraft.item.ItemEnchantedBook;
import net.minecraft.item.ItemEnderPearl;
import net.minecraft.item.ItemFireball;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemSnowball;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.item.Item.ToolMaterial;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;

public final class ItemHelper {
   private static final Minecraft minecraft = Minecraft.func_71410_x();
   private static final List<Integer> BENEFICIAL_POTION_IDS = new BeneficialPotions();
   private static final int INVENTORY_SEARCH_SIZE = 36;
   private static final int HOTBAR_SIZE = 9;
   private static final int TOTAL_INVENTORY_SLOTS = 40;
   private static final int MINIMUM_TOOL_DURABILITY = 30;

   private ItemHelper() {
   }

   public static boolean isDiscardableItem(ItemStack stack) {
      if (stack == null) {
         return false;
      } else {
         Item item = stack.func_77973_b();
         if (item instanceof ItemBlock) {
            return !isSuitablePlacementBlock((ItemBlock)item);
         } else if (item instanceof ItemPotion) {
            List<PotionEffect> effects = ((ItemPotion)item).func_77832_l(stack);
            return effects.stream().<Integer>map(PotionEffect::func_76456_a).noneMatch(BENEFICIAL_POTION_IDS::contains);
         } else if (item instanceof ItemFishingRod
            || item instanceof ItemSnowball
            || item instanceof ItemEgg
            || item instanceof ItemBucket
            || item instanceof ItemEnderPearl) {
            return false;
         } else {
            return item instanceof ItemFood && item != Items.field_151070_bp ? false : item != Items.field_151156_bN;
         }
      }
   }

   public static boolean isPlaceableBlockStack(ItemStack stack) {
      return stack != null
         && stack.field_77994_a >= 1
         && stack.func_77973_b() instanceof ItemBlock
         && isSuitablePlacementBlock((ItemBlock)stack.func_77973_b());
   }

   public static boolean isSuitablePlacementBlock(ItemBlock itemBlock) {
      Block block = itemBlock.func_179223_d();
      return !BlockHelper.isInteractable(block) && BlockHelper.isSuitableFullBlock(block);
   }

   public static double weaponScore(ItemStack stack) {
      if (stack == null) {
         return 0.0;
      } else {
         double score = 0.0;
         Multimap<String, AttributeModifier> modifiers = stack.func_111283_C();

         for (String attributeName : modifiers.keySet()) {
            if ("generic.attackDamage".equals(attributeName)) {
               Collection<AttributeModifier> attackModifiers = modifiers.get(attributeName);
               if (!attackModifiers.isEmpty()) {
                  score += attackModifiers.iterator().next().func_111164_d();
               }
            }
         }

         if (stack.func_77948_v()) {
            score += EnchantmentHelper.func_77506_a(Enchantment.field_77334_n.field_77352_x, stack);
            score += EnchantmentHelper.func_77506_a(Enchantment.field_180314_l.field_77352_x, stack) * 1.25;
         }

         return score;
      }
   }

   public static float toolEfficiencyScore(ItemStack stack) {
      if (stack != null && stack.func_77973_b() instanceof ItemTool) {
         float score = ((ItemTool)stack.func_77973_b()).func_150913_i().func_77998_b();
         if (score > 1.0F) {
            int efficiencyLevel = EnchantmentHelper.func_77506_a(Enchantment.field_77349_p.field_77352_x, stack);
            if (efficiencyLevel > 0) {
               score += efficiencyLevel * efficiencyLevel + 1;
            }
         }

         return score;
      } else {
         return 1.0F;
      }
   }

   public static double armorScore(ItemStack stack) {
      if (stack != null && stack.func_77973_b() instanceof ItemArmor) {
         double score = ((ItemArmor)stack.func_77973_b()).field_77879_b;
         if (stack.func_77948_v()) {
            score += EnchantmentHelper.func_77506_a(Enchantment.field_180310_c.field_77352_x, stack) * 0.25;
         }

         return score;
      } else {
         return 0.0;
      }
   }

   public static int findBestWeaponSlot(int startSlot, boolean avoidLowDurability) {
      int bestSlot = -1;
      double bestScore = 0.0;

      for (int offset = 0; offset < 36; offset++) {
         int slot = wrapInventorySlot(startSlot + offset);
         ItemStack stackCandidate = minecraft.field_71439_g.field_71071_by.func_70301_a(slot);
         if (stackCandidate != null && stackCandidate.func_77973_b() instanceof ItemSword && (!avoidLowDurability || !hasLowDurability(stackCandidate))) {
            double score = weaponScore(stackCandidate);
            if (score > bestScore) {
               bestSlot = slot;
               bestScore = score;
            }
         }
      }

      return bestSlot;
   }

   public static int findBestToolSlot(String toolClass, int startSlot, boolean avoidLowDurability) {
      int bestSlot = -1;
      float bestScore = 1.0F;

      for (int offset = 0; offset < 36; offset++) {
         int slot = wrapInventorySlot(startSlot + offset);
         ItemStack stack = minecraft.field_71439_g.field_71071_by.func_70301_a(slot);
         if (stack != null
            && stack.func_77973_b() instanceof ItemTool
            && stack.func_77973_b().getToolClasses(stack).contains(toolClass)
            && (!avoidLowDurability || !hasLowDurability(stack))) {
            float score = toolEfficiencyScore(stack);
            if (score > bestScore) {
               bestSlot = slot;
               bestScore = score;
            }
         }
      }

      return bestSlot;
   }

   public static int findBestToolForBlock(int preferredSlot, Block block) {
      int bestSlot = preferredSlot;
      ItemStack preferredStack = minecraft.field_71439_g.field_71071_by.func_70301_a(preferredSlot);
      float bestStrength = preferredStack == null ? 1.0F : preferredStack.func_150997_a(block);

      for (int slot = 0; slot < 9; slot++) {
         ItemStack stackSecondary = minecraft.field_71439_g.field_71071_by.func_70301_a(slot);
         if (stackSecondary != null) {
            float strength = stackSecondary.func_150997_a(block);
            if (strength > bestStrength) {
               bestSlot = slot;
               bestStrength = strength;
            }
         }
      }

      return bestSlot;
   }

   public static int findBestArmorSlot(int armorType, boolean avoidLowDurability) {
      int bestSlot = -1;
      double bestScore = 0.0;

      for (int slot = 0; slot < 40; slot++) {
         ItemStack stackAlternate = minecraft.field_71439_g.field_71071_by.func_70301_a(slot);
         if (stackAlternate != null && stackAlternate.func_77973_b() instanceof ItemArmor) {
            ItemArmor armor = (ItemArmor)stackAlternate.func_77973_b();
            if (armor.field_77881_a == armorType && (!avoidLowDurability || !hasLowDurability(stackAlternate))) {
               double score = armorScore(stackAlternate);
               if (score >= bestScore) {
                  bestSlot = slot;
                  bestScore = score;
               }
            }
         }
      }

      return bestSlot;
   }

   public static int findLargestBlockStackSlot(int startSlot) {
      int bestSlot = -1;
      int largestStackSize = 0;

      for (int offset = 0; offset < 36; offset++) {
         int slot = wrapInventorySlot(startSlot + offset);
         ItemStack stackResult = minecraft.field_71439_g.field_71071_by.func_70301_a(slot);
         if (isPlaceableBlockStack(stackResult) && stackResult.field_77994_a > largestStackSize) {
            bestSlot = slot;
            largestStackSize = stackResult.field_77994_a;
         }
      }

      return bestSlot;
   }

   public static boolean isHoldingWeapon() {
      ItemStack heldItem = minecraft.field_71439_g.func_70694_bm();
      if (heldItem == null) {
         return false;
      } else {
         if (heldItem.func_77942_o()) {
            NBTTagCompound tags = heldItem.func_77978_p();
            if (tags.func_74764_b("ExtraAttributes")) {
               NBTTagCompound extraAttributes = tags.func_74775_l("ExtraAttributes");
               if (extraAttributes.func_74764_b("UHCid")) {
                  long itemId = extraAttributes.func_74763_f("UHCid");
                  if (itemId == 50006L || itemId == 50009L) {
                     return true;
                  }
               }
            }

            if (tags.func_74764_b("HideFlags") && heldItem.func_77973_b() instanceof ItemSpade) {
               ToolMaterial material = ((ItemSpade)heldItem.func_77973_b()).func_150913_i();
               if (material == ToolMaterial.EMERALD) {
                  return true;
               }
            }

            if (tags.func_74764_b("ItemModel") && "minecraft:mace".equals(tags.func_74779_i("ItemModel"))) {
               return true;
            }
         }

         if (heldItem.func_77973_b() instanceof ItemEnchantedBook) {
            return false;
         } else {
            Map enchantments = EnchantmentHelper.func_82781_a(heldItem);
            return enchantments.containsKey(19) || heldItem.func_77973_b() instanceof ItemSword;
         }
      }
   }

   public static boolean isHoldingSword() {
      ItemStack heldItem = minecraft.field_71439_g.func_70694_bm();
      return heldItem != null && heldItem.func_77973_b() instanceof ItemSword;
   }

   public static boolean isHoldingTool() {
      ItemStack heldItem = minecraft.field_71439_g.func_70694_bm();
      return heldItem != null && heldItem.func_77973_b() instanceof ItemTool;
   }

   public static boolean isHoldingConsumable() {
      ItemStack heldItem = minecraft.field_71439_g.func_70694_bm();
      if (heldItem == null) {
         return false;
      } else if (ItemPotion.func_77831_g(heldItem.func_77973_b().getMetadata(heldItem))) {
         return false;
      } else {
         EnumAction action = heldItem.func_77975_n();
         return action == EnumAction.EAT || action == EnumAction.DRINK;
      }
   }

   public static boolean isHoldingBow() {
      ItemStack heldItem = minecraft.field_71439_g.func_70694_bm();
      return heldItem != null && heldItem.func_77973_b() instanceof ItemBow;
   }

   public static boolean isHoldingBlocks() {
      ItemStack heldItem = minecraft.field_71439_g.func_70694_bm();
      return heldItem != null && heldItem.field_77994_a >= 1 && heldItem.func_77973_b() instanceof ItemBlock;
   }

   public static boolean isHoldingPlaceableBlock() {
      return isPlaceableBlockStack(minecraft.field_71439_g.func_70694_bm());
   }

   public static boolean isHoldingFireball() {
      ItemStack heldItem = minecraft.field_71439_g.func_70694_bm();
      return heldItem != null && heldItem.field_77994_a >= 1 && heldItem.func_77973_b() instanceof ItemFireball;
   }

   private static int wrapInventorySlot(int slot) {
      return (slot % 36 + 36) % 36;
   }

   private static boolean hasLowDurability(ItemStack stackValue) {
      return stackValue.func_77951_h() && stackValue.func_77958_k() - stackValue.func_77952_i() < 30;
   }
}
