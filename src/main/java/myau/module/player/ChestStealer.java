package myau.module.player;

import myau.event.Listen;
import myau.event.input.OnWindowClick;
import myau.event.player.OnRotationUpdate;
import myau.module.ClientModule;
import myau.setting.BooleanOption;
import myau.setting.IntegerOption;
import myau.util.chat.ChatHelper;
import myau.util.item.ItemHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.world.WorldSettings.GameType;
import org.apache.commons.lang3.RandomUtils;

public class ChestStealer extends ClientModule {
   private static final Minecraft minecraft = Minecraft.func_71410_x();
   private int stealDelayTicks;
   private int openDelayTicks;
   private boolean chestOpen;
   private boolean inventoryFullWarningShown;
   public final IntegerOption minDelay = new IntegerOption("min-delay", 1, 0, 20);
   public final IntegerOption maxDelay = new IntegerOption("max-delay", 2, 0, 20);
   public final IntegerOption openDelay = new IntegerOption("open-delay", 1, 0, 20);
   public final BooleanOption autoClose = new BooleanOption("auto-close", false);
   public final BooleanOption nameCheck = new BooleanOption("name-check", true);
   public final BooleanOption skipTrash = new BooleanOption("skip-trash", true);

   public ChestStealer() {
      super("ChestStealer", false, 0);
   }

   private boolean isSupportedGameMode() {
      GameType gameType = minecraft.field_71442_b.func_178889_l();
      return gameType == GameType.SURVIVAL || gameType == GameType.ADVENTURE;
   }

   private void shiftClickSlot(int windowId, int slot) {
      minecraft.field_71442_b.func_78753_a(windowId, slot, 0, 1, minecraft.field_71439_g);
   }

   @Listen
   public void onRotationUpdate(OnRotationUpdate event) {
      if (this.stealDelayTicks > 0) {
         this.stealDelayTicks--;
      }

      if (this.openDelayTicks > 0) {
         this.openDelayTicks--;
      }

      if (!(minecraft.field_71462_r instanceof GuiChest)) {
         this.chestOpen = false;
      } else {
         Container container = ((GuiChest)minecraft.field_71462_r).field_147002_h;
         if (!(container instanceof ContainerChest)) {
            this.chestOpen = false;
         } else {
            if (!this.chestOpen) {
               this.chestOpen = true;
               this.inventoryFullWarningShown = false;
               this.openDelayTicks = this.openDelay.getValue() + 1;
            }

            if (this.openDelayTicks <= 0 && this.stealDelayTicks <= 0 && this.isEnabled() && this.isSupportedGameMode()) {
               IInventory chestInventory = ((ContainerChest)container).func_85151_d();
               if (this.nameCheck.getValue()) {
                  String inventoryName = chestInventory.func_70005_c_();
                  if (!inventoryName.isEmpty()
                     && !inventoryName.equals(I18n.func_135052_a("container.chest", new Object[0]))
                     && !inventoryName.equals(I18n.func_135052_a("container.chestDouble", new Object[0]))) {
                     return;
                  }
               }

               if (minecraft.field_71439_g.field_71071_by.func_70447_i() == -1) {
                  if (!this.inventoryFullWarningShown) {
                     ChatHelper.addFormattedMessage(String.format("%s%s: &cYour inventory is full!&r", myau.util.chat.ChatPrefix.get(), this.getName()));
                     this.inventoryFullWarningShown = true;
                  }

                  if (this.autoClose.getValue()) {
                     minecraft.field_71439_g.func_71053_j();
                  }
               } else if (!this.skipTrash.getValue() || !this.stealBestUpgrade(container, chestInventory)) {
                  for (int slotIndex = 0; slotIndex < chestInventory.func_70302_i_(); slotIndex++) {
                     Slot slot = container.func_75139_a(slotIndex);
                     if (slot.func_75216_d()) {
                        ItemStack stack = slot.func_75211_c();
                        if (!this.skipTrash.getValue() || !ItemHelper.isDiscardableItem(stack)) {
                           this.shiftClickSlot(container.field_75152_c, slotIndex);
                           return;
                        }
                     }
                  }

                  if (this.autoClose.getValue()) {
                     minecraft.field_71439_g.func_71053_j();
                  }
               }
            }
         }
      }
   }

   private boolean stealBestUpgrade(Container container, IInventory chestInventory) {
      int bestSwordSlot = -1;
      double bestSwordScore = 0.0;
      int[] bestArmorSlots = new int[]{-1, -1, -1, -1};
      double[] bestArmorScores = new double[]{0.0, 0.0, 0.0, 0.0};
      int bestPickaxeSlot = -1;
      float bestPickaxeScore = 1.0F;
      int bestShovelSlot = -1;
      float bestShovelScore = 1.0F;
      int bestAxeSlot = -1;
      float bestAxeScore = 1.0F;

      for (int slotIndex = 0; slotIndex < chestInventory.func_70302_i_(); slotIndex++) {
         Slot slot = container.func_75139_a(slotIndex);
         if (slot.func_75216_d()) {
            ItemStack stack = slot.func_75211_c();
            Item item = stack.func_77973_b();
            if (item instanceof ItemSword) {
               double score = ItemHelper.weaponScore(stack);
               if (bestSwordSlot == -1 || score > bestSwordScore) {
                  bestSwordSlot = slotIndex;
                  bestSwordScore = score;
               }
            } else if (item instanceof ItemArmor) {
               int armorType = ((ItemArmor)item).field_77881_a;
               double score = ItemHelper.armorScore(stack);
               if (bestArmorSlots[armorType] == -1 || score > bestArmorScores[armorType]) {
                  bestArmorSlots[armorType] = slotIndex;
                  bestArmorScores[armorType] = score;
               }
            } else if (item instanceof ItemPickaxe) {
               float score = ItemHelper.toolEfficiencyScore(stack);
               if (bestPickaxeSlot == -1 || score > bestPickaxeScore) {
                  bestPickaxeSlot = slotIndex;
                  bestPickaxeScore = score;
               }
            } else if (item instanceof ItemSpade) {
               float score = ItemHelper.toolEfficiencyScore(stack);
               if (bestShovelSlot == -1 || score > bestShovelScore) {
                  bestShovelSlot = slotIndex;
                  bestShovelScore = score;
               }
            } else if (item instanceof ItemAxe) {
               float score = ItemHelper.toolEfficiencyScore(stack);
               if (bestAxeSlot == -1 || score > bestAxeScore) {
                  bestAxeSlot = slotIndex;
                  bestAxeScore = score;
               }
            }
         }
      }

      int currentSwordSlot = ItemHelper.findBestWeaponSlot(0, true);
      double currentSwordScore = currentSwordSlot != -1 ? ItemHelper.weaponScore(minecraft.field_71439_g.field_71071_by.func_70301_a(currentSwordSlot)) : 0.0;
      if (bestSwordSlot != -1 && bestSwordScore > currentSwordScore) {
         this.shiftClickSlot(container.field_75152_c, bestSwordSlot);
         return true;
      } else {
         for (int armorType = 0; armorType < 4; armorType++) {
            int currentArmorSlot = ItemHelper.findBestArmorSlot(armorType, true);
            double currentArmorScore = currentArmorSlot != -1
               ? ItemHelper.armorScore(minecraft.field_71439_g.field_71071_by.func_70301_a(currentArmorSlot))
               : 0.0;
            if (bestArmorSlots[armorType] != -1 && bestArmorScores[armorType] > currentArmorScore) {
               this.shiftClickSlot(container.field_75152_c, bestArmorSlots[armorType]);
               return true;
            }
         }

         if (isBetterTool("pickaxe", bestPickaxeSlot, bestPickaxeScore)) {
            this.shiftClickSlot(container.field_75152_c, bestPickaxeSlot);
            return true;
         } else if (isBetterTool("shovel", bestShovelSlot, bestShovelScore)) {
            this.shiftClickSlot(container.field_75152_c, bestShovelSlot);
            return true;
         } else if (isBetterTool("axe", bestAxeSlot, bestAxeScore)) {
            this.shiftClickSlot(container.field_75152_c, bestAxeSlot);
            return true;
         } else {
            return false;
         }
      }
   }

   private static boolean isBetterTool(String toolClass, int chestSlot, float chestScore) {
      if (chestSlot == -1) {
         return false;
      } else {
         int currentSlot = ItemHelper.findBestToolSlot(toolClass, 0, true);
         float currentScore = currentSlot != -1 ? ItemHelper.toolEfficiencyScore(minecraft.field_71439_g.field_71071_by.func_70301_a(currentSlot)) : 1.0F;
         return chestScore > currentScore;
      }
   }

   @Listen
   public void onWindowClick(OnWindowClick event) {
      this.stealDelayTicks = RandomUtils.nextInt(this.minDelay.getValue() + 1, this.maxDelay.getValue() + 2);
   }

   @Override
   public void onSettingChanged(String name) {
      if ("min-delay".equals(name) && this.minDelay.getValue() > this.maxDelay.getValue()) {
         this.maxDelay.setValue(this.minDelay.getValue());
      } else if ("max-delay".equals(name) && this.minDelay.getValue() > this.maxDelay.getValue()) {
         this.minDelay.setValue(this.maxDelay.getValue());
      }
   }
}
