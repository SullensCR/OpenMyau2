package myau.module.world;

import java.text.DecimalFormat;
import myau.event.Listen;
import myau.event.lifecycle.OnPreTick;
import myau.mixin.accessor.MinecraftAccessor;
import myau.module.ClientModule;
import myau.rotation.RotationHelper;
import myau.setting.BooleanOption;
import myau.setting.FloatOption;
import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;

public class FastPlace extends ClientModule {
   private static final Minecraft minecraft = Minecraft.func_71410_x();
   private static final DecimalFormat DELAY_FORMAT = new DecimalFormat("0.0#");
   private long remainingDelayMillis = 0L;
   public final FloatOption delay = new FloatOption("delay", 1.0F, 1.0F, 3.0F);
   public final BooleanOption blocksOnly = new BooleanOption("blocks-only", true);
   public final BooleanOption placeFix = new BooleanOption("place-fix", true);

   public FastPlace() {
      super("FastPlace", false, 0);
   }

   private boolean canPlaceImmediately() {
      ItemStack heldStack = minecraft.field_71439_g.func_70694_bm();
      if (heldStack == null) {
         return !this.blocksOnly.getValue();
      } else {
         Item heldItem = heldStack.func_77973_b();
         if (heldItem instanceof ItemFishingRod) {
            return false;
         } else if (!(heldItem instanceof ItemBlock)) {
            return !this.blocksOnly.getValue();
         } else if (!this.placeFix.getValue()) {
            return true;
         } else {
            MovingObjectPosition hit = RotationHelper.traceBlocks(
               minecraft.field_71439_g.field_70177_z, minecraft.field_71439_g.field_70125_A, minecraft.field_71442_b.func_78757_d(), 1.0F
            );
            return hit == null
               ? false
               : hit.field_72313_a == MovingObjectType.BLOCK
                  && ((ItemBlock)heldItem).func_179222_a(minecraft.field_71441_e, hit.func_178782_a(), hit.field_178784_b, minecraft.field_71439_g, heldStack);
         }
      }
   }

   @Listen
   public void onPreTick(OnPreTick event) {
      if (this.isEnabled()) {
         int rightClickDelay = ((MinecraftAccessor)minecraft).getRightClickDelayTimer();
         if (rightClickDelay == 4) {
            this.remainingDelayMillis = this.remainingDelayMillis + (long)(50.0F * this.delay.getValue());
         }

         if (this.remainingDelayMillis > 0L) {
            this.remainingDelayMillis -= 50L;
         }

         if (this.remainingDelayMillis <= 0L && rightClickDelay > 1 && this.canPlaceImmediately()) {
            ((MinecraftAccessor)minecraft).setRightClickDelayTimer(0);
         }
      }
   }

   @Override
   public void onDisable() {
      this.remainingDelayMillis = 0L;
   }

   @Override
   public String[] getHudInfo() {
      return new String[]{DELAY_FORMAT.format(this.delay.getValue())};
   }
}
