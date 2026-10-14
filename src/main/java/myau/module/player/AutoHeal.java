package myau.module.player;

import myau.event.Listen;
import myau.event.input.OnBlockClick;
import myau.event.input.OnHotbarChange;
import myau.event.input.OnLeftClick;
import myau.event.input.OnRightClick;
import myau.event.lifecycle.OnPostTick;
import myau.event.lifecycle.OnPreTick;
import myau.mixin.accessor.PlayerControllerMPAccessor;
import myau.module.ClientModule;
import myau.setting.BooleanOption;
import myau.setting.IntegerOption;
import myau.setting.PercentOption;
import myau.util.Stopwatch;
import myau.util.network.PacketHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemSkull;
import net.minecraft.item.ItemSoup;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.potion.Potion;

public class AutoHeal extends ClientModule {
   private static final Minecraft minecraft = Minecraft.func_71410_x();
   private final Stopwatch healDelayTimer = new Stopwatch();
   private boolean wasBelowHealthThreshold;
   private int previousHotbarSlot = -1;
   public final PercentOption health = new PercentOption("health", 35);
   public final IntegerOption delay = new IntegerOption("delay", 4000, 0, 5000);
   public final BooleanOption regenCheck = new BooleanOption("regen-check", false);

   public AutoHeal() {
      super("AutoHeal", false, 0);
   }

   private int findHealingItemSlot() {
      for (int slot = 0; slot < 9; slot++) {
         ItemStack stack = minecraft.field_71439_g.field_71071_by.func_70301_a(slot);
         if (stack != null && stack.func_82837_s()) {
            String displayName = stack.func_82833_r();
            if (stack.func_77973_b() instanceof ItemSkull) {
               if (displayName.contains("§6") && displayName.contains("Golden Head")) {
                  return slot;
               }

               if (displayName.matches("\\S+§c's Head")) {
                  return slot;
               }
            } else if (stack.func_77973_b() instanceof ItemSoup
               && displayName.contains("§a")
               && (displayName.contains("Tasty Soup") || displayName.contains("Assist Soup"))) {
               return slot;
            }
         }
      }

      return -1;
   }

   private boolean isRegenerationBlockingHeal() {
      return this.regenCheck.getValue() && minecraft.field_71439_g.func_70644_a(Potion.field_76428_l);
   }

   public boolean hasPendingSlotRestore() {
      return this.previousHotbarSlot != -1;
   }

   @Listen(
      priority = 50
   )
   public void onPreTick(OnPreTick event) {
      if (this.isEnabled()) {
         float currentHealth = (float)Math.ceil(minecraft.field_71439_g.func_110143_aJ() + minecraft.field_71439_g.func_110139_bj());
         boolean belowHealthThreshold = currentHealth / minecraft.field_71439_g.func_110138_aP() <= this.health.getValue().intValue() / 100.0F;
         if (this.wasBelowHealthThreshold
            && belowHealthThreshold
            && !this.isRegenerationBlockingHeal()
            && this.healDelayTimer.hasElapsed(this.delay.getValue().intValue())) {
            int healingSlot = this.findHealingItemSlot();
            if (healingSlot != -1) {
               this.previousHotbarSlot = minecraft.field_71439_g.field_71071_by.field_70461_c;
               minecraft.field_71439_g.field_71071_by.field_70461_c = healingSlot;
               ((PlayerControllerMPAccessor)minecraft.field_71442_b).callSyncCurrentPlayItem();
               PacketHelper.send(new C08PacketPlayerBlockPlacement(minecraft.field_71439_g.func_70694_bm()));
               this.healDelayTimer.reset();
            }
         }

         this.wasBelowHealthThreshold = belowHealthThreshold;
      }
   }

   @Listen
   public void onPostTick(OnPostTick event) {
      if (this.previousHotbarSlot != -1) {
         minecraft.field_71439_g.field_71071_by.field_70461_c = this.previousHotbarSlot;
         this.previousHotbarSlot = -1;
      }
   }

   @Listen
   public void onLeftClick(OnLeftClick event) {
      if (this.isEnabled() && this.hasPendingSlotRestore()) {
         event.setCancelled(true);
      }
   }

   @Listen
   public void onRightClick(OnRightClick event) {
      if (this.isEnabled() && this.hasPendingSlotRestore()) {
         event.setCancelled(true);
      }
   }

   @Listen
   public void onBlockClick(OnBlockClick event) {
      if (this.isEnabled() && this.hasPendingSlotRestore()) {
         event.setCancelled(true);
      }
   }

   @Listen
   public void onHotbarChange(OnHotbarChange event) {
      if (this.isEnabled() && this.hasPendingSlotRestore()) {
         event.setCancelled(true);
      }
   }
}
