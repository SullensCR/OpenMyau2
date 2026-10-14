package myau.module.player;

import java.util.HashSet;
import java.util.Set;
import myau.event.Listen;
import myau.event.input.OnWindowClick;
import myau.event.player.OnRotationUpdate;
import myau.module.ClientModule;
import myau.setting.BooleanOption;
import myau.setting.IntegerOption;
import myau.util.item.ItemHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldSettings.GameType;
import org.apache.commons.lang3.RandomUtils;

public class InvManager extends ClientModule {
   private static final Minecraft minecraft = Minecraft.func_71410_x();
   private int actionDelayTicks;
   private int openDelayTicks;
   private boolean inventoryOpen;
   public final IntegerOption minDelay = new IntegerOption("min-delay", 1, 0, 20);
   public final IntegerOption maxDelay = new IntegerOption("max-delay", 2, 0, 20);
   public final IntegerOption openDelay = new IntegerOption("open-delay", 1, 0, 20);
   public final BooleanOption autoArmor = new BooleanOption("auto-armor", true);
   public final BooleanOption dropTrash = new BooleanOption("drop-trash", false);
   public final IntegerOption swordSlot = new IntegerOption("sword-slot", 1, 0, 9);
   public final IntegerOption pickaxeSlot = new IntegerOption("pickaxe-slot", 3, 0, 9);
   public final IntegerOption shovelSlot = new IntegerOption("shovel-slot", 4, 0, 9);
   public final IntegerOption axeSlot = new IntegerOption("axe-slot", 5, 0, 9);
   public final IntegerOption blocksSlot = new IntegerOption("blocks-slot", 2, 0, 9);
   public final IntegerOption blocks = new IntegerOption("blocks", 128, 64, 2304);

   public InvManager() {
      super("InvManager", false, 0);
   }

   private boolean isSupportedGameMode() {
      GameType gameType = minecraft.field_71442_b.func_178889_l();
      return gameType == GameType.SURVIVAL || gameType == GameType.ADVENTURE;
   }

   private static int inventoryIndexToContainerSlot(int inventoryIndex) {
      if (inventoryIndex >= 36) {
         return 44 - inventoryIndex;
      } else {
         return inventoryIndex > 8 ? inventoryIndex : 36 + inventoryIndex;
      }
   }

   private void clickInventorySlot(int slot, int button, int mode) {
      minecraft.field_71442_b.func_78753_a(minecraft.field_71439_g.field_71069_bz.field_75152_c, slot, button, mode, minecraft.field_71439_g);
   }

   private static int stackSize(int inventorySlot) {
      if (inventorySlot == -1) {
         return 0;
      } else {
         ItemStack stack = minecraft.field_71439_g.field_71071_by.func_70301_a(inventorySlot);
         return stack == null ? 0 : stack.field_77994_a;
      }
   }

   @Listen
   public void onRotationUpdate(OnRotationUpdate event) {
      if (this.actionDelayTicks > 0) {
         this.actionDelayTicks--;
      }

      if (this.openDelayTicks > 0) {
         this.openDelayTicks--;
      }

      if (minecraft.field_71462_r instanceof GuiInventory && ((GuiInventory)minecraft.field_71462_r).field_147002_h instanceof ContainerPlayer) {
         if (!this.inventoryOpen) {
            this.inventoryOpen = true;
            this.openDelayTicks = this.openDelay.getValue() + 1;
         }

         if (this.openDelayTicks <= 0 && this.actionDelayTicks <= 0 && this.isEnabled() && this.isSupportedGameMode()) {
            int[] bestArmor = new int[4];
            int[] bestArmorIncludingDamaged = new int[4];

            for (int armorType = 0; armorType < 4; armorType++) {
               bestArmor[armorType] = ItemHelper.findBestArmorSlot(armorType, true);
               bestArmorIncludingDamaged[armorType] = ItemHelper.findBestArmorSlot(armorType, false);
            }

            int desiredSwordSlot = this.swordSlot.getValue() - 1;
            int bestSword = ItemHelper.findBestWeaponSlot(desiredSwordSlot, true);
            int bestSwordIncludingDamaged = ItemHelper.findBestWeaponSlot(desiredSwordSlot, false);
            int desiredPickaxeSlot = this.pickaxeSlot.getValue() - 1;
            int bestPickaxe = ItemHelper.findBestToolSlot("pickaxe", desiredPickaxeSlot, true);
            int bestPickaxeIncludingDamaged = ItemHelper.findBestToolSlot("pickaxe", desiredPickaxeSlot, false);
            int desiredShovelSlot = this.shovelSlot.getValue() - 1;
            int bestShovel = ItemHelper.findBestToolSlot("shovel", desiredShovelSlot, true);
            int bestShovelIncludingDamaged = ItemHelper.findBestToolSlot("shovel", desiredShovelSlot, false);
            int desiredAxeSlot = this.axeSlot.getValue() - 1;
            int bestAxe = ItemHelper.findBestToolSlot("axe", desiredAxeSlot, true);
            int bestAxeIncludingDamaged = ItemHelper.findBestToolSlot("axe", desiredAxeSlot, false);
            int desiredBlocksSlot = this.blocksSlot.getValue() - 1;
            int bestBlocks = ItemHelper.findLargestBlockStackSlot(desiredBlocksSlot);
            if (!this.autoArmor.getValue() || !this.equipBestArmor(bestArmor, bestArmorIncludingDamaged)) {
               Set<Integer> reservedHotbarSlots = new HashSet<>();
               if (!this.moveBestItemToHotbar(desiredSwordSlot, bestSword, bestSwordIncludingDamaged, reservedHotbarSlots)) {
                  if (!this.moveBestItemToHotbar(desiredPickaxeSlot, bestPickaxe, bestPickaxeIncludingDamaged, reservedHotbarSlots)) {
                     if (!this.moveBestItemToHotbar(desiredShovelSlot, bestShovel, bestShovelIncludingDamaged, reservedHotbarSlots)) {
                        if (!this.moveBestItemToHotbar(desiredAxeSlot, bestAxe, bestAxeIncludingDamaged, reservedHotbarSlots)) {
                           if (!this.moveBestItemToHotbar(desiredBlocksSlot, bestBlocks, -1, reservedHotbarSlots)) {
                              if (this.dropTrash.getValue()) {
                                 Set<Integer> protectedSlots = new HashSet<>();
                                 addSlots(protectedSlots, bestArmor);
                                 addSlots(protectedSlots, bestArmorIncludingDamaged);
                                 addSlot(protectedSlots, bestSword);
                                 addSlot(protectedSlots, bestSwordIncludingDamaged);
                                 addSlot(protectedSlots, bestPickaxe);
                                 addSlot(protectedSlots, bestPickaxeIncludingDamaged);
                                 addSlot(protectedSlots, bestShovel);
                                 addSlot(protectedSlots, bestShovelIncludingDamaged);
                                 addSlot(protectedSlots, bestAxe);
                                 addSlot(protectedSlots, bestAxeIncludingDamaged);
                                 addSlot(protectedSlots, bestBlocks);
                                 int retainedBlockCount = stackSize(bestBlocks);

                                 for (int inventorySlot = 0; inventorySlot < 36; inventorySlot++) {
                                    if (!protectedSlots.contains(inventorySlot)) {
                                       ItemStack stack = minecraft.field_71439_g.field_71071_by.func_70301_a(inventorySlot);
                                       if (stack != null) {
                                          boolean placeableBlocks = ItemHelper.isPlaceableBlockStack(stack);
                                          if (ItemHelper.isDiscardableItem(stack) || placeableBlocks && retainedBlockCount >= this.blocks.getValue()) {
                                             this.clickInventorySlot(inventoryIndexToContainerSlot(inventorySlot), 1, 4);
                                             return;
                                          }

                                          if (placeableBlocks) {
                                             retainedBlockCount += stack.field_77994_a;
                                          }
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      } else {
         this.inventoryOpen = false;
      }
   }

   private boolean equipBestArmor(int[] bestArmor, int[] bestArmorIncludingDamaged) {
      for (int armorType = 0; armorType < 4; armorType++) {
         int preferred = bestArmor[armorType];
         int fallback = bestArmorIncludingDamaged[armorType];
         if (preferred != -1 || fallback != -1) {
            int equippedInventorySlot = 39 - armorType;
            if (preferred != equippedInventorySlot && fallback != equippedInventorySlot) {
               if (minecraft.field_71439_g.field_71071_by.func_70301_a(equippedInventorySlot) != null) {
                  if (minecraft.field_71439_g.field_71071_by.func_70447_i() != -1) {
                     this.clickInventorySlot(inventoryIndexToContainerSlot(equippedInventorySlot), 0, 1);
                  } else {
                     this.clickInventorySlot(inventoryIndexToContainerSlot(equippedInventorySlot), 1, 4);
                  }
               } else {
                  int source = preferred != -1 ? preferred : fallback;
                  this.clickInventorySlot(inventoryIndexToContainerSlot(source), 0, 1);
               }

               return true;
            }
         }
      }

      return false;
   }

   private boolean moveBestItemToHotbar(int desiredHotbarSlot, int preferredSource, int fallbackSource, Set<Integer> reservedHotbarSlots) {
      if (desiredHotbarSlot >= 0
         && desiredHotbarSlot <= 8
         && !reservedHotbarSlots.contains(desiredHotbarSlot)
         && (preferredSource != -1 || fallbackSource != -1)) {
         reservedHotbarSlots.add(desiredHotbarSlot);
         if (preferredSource != desiredHotbarSlot && fallbackSource != desiredHotbarSlot) {
            int source = preferredSource != -1 ? preferredSource : fallbackSource;
            this.clickInventorySlot(inventoryIndexToContainerSlot(source), desiredHotbarSlot, 2);
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private static void addSlots(Set<Integer> slots, int[] values) {
      for (int slot : values) {
         addSlot(slots, slot);
      }
   }

   private static void addSlot(Set<Integer> slots, int slot) {
      if (slot >= 0) {
         slots.add(slot);
      }
   }

   @Listen
   public void onWindowClick(OnWindowClick event) {
      this.actionDelayTicks = RandomUtils.nextInt(this.minDelay.getValue() + 1, this.maxDelay.getValue() + 2);
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
