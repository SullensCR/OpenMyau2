package myau.module.player;

import myau.event.Listen;
import myau.event.lifecycle.OnPreTick;
import myau.module.ClientModule;
import myau.setting.BooleanOption;
import myau.setting.IntegerOption;
import myau.util.input.InputHelper;
import myau.util.item.ItemHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;

public class AutoTool extends ClientModule {
   private static final Minecraft minecraft = Minecraft.func_71410_x();
   private int selectedSlot = -1;
   private int previousSlot = -1;
   private int miningTicks;
   public final IntegerOption delay = new IntegerOption("delay", 0, 0, 5);
   public final BooleanOption switchBack = new BooleanOption("switch-back", true);
   public final BooleanOption sneakOnly = new BooleanOption("sneak-only", true);

   public AutoTool() {
      super("AutoTool", false, 0);
   }

   @Listen
   public void onPreTick(OnPreTick event) {
      boolean enabled = this.isEnabled();
      if (enabled) {
         if (minecraft.field_71476_x != null
            && minecraft.field_71476_x.field_72313_a == MovingObjectType.BLOCK
            && minecraft.field_71474_y.field_74312_F.func_151470_d()
            && !minecraft.field_71439_g.func_71039_bw()) {
            if (this.miningTicks >= this.delay.getValue()
               && (!this.sneakOnly.getValue() || InputHelper.isKeyDown(minecraft.field_71474_y.field_74311_E.func_151463_i()))) {
               int bestSlot = ItemHelper.findBestToolForBlock(
                  minecraft.field_71439_g.field_71071_by.field_70461_c,
                  minecraft.field_71441_e.func_180495_p(minecraft.field_71476_x.func_178782_a()).func_177230_c()
               );
               if (minecraft.field_71439_g.field_71071_by.field_70461_c != bestSlot) {
                  this.previousSlot = minecraft.field_71439_g.field_71071_by.field_70461_c;
                  minecraft.field_71439_g.field_71071_by.field_70461_c = this.selectedSlot = bestSlot;
               }
            }

            this.miningTicks++;
         } else {
            this.selectedSlot = -1;
            this.previousSlot = -1;
            this.miningTicks = 0;
         }
      }
   }

   @Override
   public void onDisable() {
      this.selectedSlot = -1;
      this.previousSlot = -1;
      this.miningTicks = 0;
   }
}
