package myau.module.movement;

import myau.core.ClientContext;
import myau.event.Listen;
import myau.event.player.OnRotationUpdate;
import myau.event.player.OnSafeWalk;
import myau.module.ClientModule;
import myau.module.world.Scaffold;
import myau.setting.BooleanOption;
import myau.setting.FloatOption;
import myau.util.item.ItemHelper;
import myau.util.movement.MovementHelper;
import myau.util.player.PlayerHelper;
import net.minecraft.client.Minecraft;

public class SafeWalk extends ClientModule {
   private static final Minecraft minecraft = Minecraft.func_71410_x();
   public final FloatOption motion = new FloatOption("motion", 1.0F, 0.5F, 1.0F);
   public final FloatOption speedMotion = new FloatOption("speed-motion", 1.0F, 0.5F, 1.5F);
   public final BooleanOption air = new BooleanOption("air", false);
   public final BooleanOption directionCheck = new BooleanOption("direction-check", true);
   public final BooleanOption pitchCheck = new BooleanOption("pitch-check", true);
   public final BooleanOption requirePress = new BooleanOption("require-press", false);
   public final BooleanOption blocksOnly = new BooleanOption("blocks-only", true);

   public SafeWalk() {
      super("SafeWalk", false, 0);
   }

   private boolean shouldApplySafeWalk() {
      Scaffold conflictingModule = (Scaffold)ClientContext.moduleManager.modules.get(Scaffold.class);
      boolean otherEnabled = conflictingModule.isEnabled();
      if (otherEnabled) {
         return false;
      } else if (this.directionCheck.getValue() && minecraft.field_71474_y.field_74351_w.func_151470_d()) {
         return false;
      } else if (this.pitchCheck.getValue() && minecraft.field_71439_g.field_70125_A < 69.0F) {
         return false;
      } else if (this.blocksOnly.getValue() && !ItemHelper.isHoldingPlaceableBlock()) {
         return false;
      } else if (this.requirePress.getValue() && !minecraft.field_71474_y.field_74313_G.func_151470_d()) {
         return false;
      } else {
         return minecraft.field_71439_g.field_70122_E
               && PlayerHelper.isOffsetCollisionFree(minecraft.field_71439_g.field_70159_w, -1.0, minecraft.field_71439_g.field_70179_y)
            ? true
            : this.air.getValue() && PlayerHelper.isOffsetCollisionFree(minecraft.field_71439_g.field_70159_w, -2.0, minecraft.field_71439_g.field_70179_y);
      }
   }

   @Listen
   public void onSafeWalk(OnSafeWalk event) {
      if (this.isEnabled() && this.shouldApplySafeWalk()) {
         event.setSafeWalk(true);
      }
   }

   @Listen
   public void onRotationUpdate(OnRotationUpdate event) {
      if (this.isEnabled()) {
         if (minecraft.field_71439_g.field_70122_E && MovementHelper.hasMovementInput() && this.shouldApplySafeWalk()) {
            if (MovementHelper.speedPotionLevel() <= 0) {
               if (this.motion.getValue() != 1.0F) {
                  MovementHelper.setSpeedAlongMotion(MovementHelper.horizontalSpeed() * this.motion.getValue().floatValue());
               }
            } else if (this.speedMotion.getValue() != 1.0F) {
               MovementHelper.setSpeedAlongMotion(MovementHelper.horizontalSpeed() * this.speedMotion.getValue().floatValue());
            }
         }
      }
   }
}
