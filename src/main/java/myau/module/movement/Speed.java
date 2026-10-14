package myau.module.movement;

import myau.core.ClientContext;
import myau.event.Listen;
import myau.event.player.OnLivingUpdate;
import myau.event.player.OnMoveFlying;
import myau.mixin.accessor.EntityAccessor;
import myau.module.ClientModule;
import myau.module.world.Scaffold;
import myau.setting.FloatOption;
import myau.setting.PercentOption;
import myau.util.movement.MovementHelper;
import net.minecraft.client.Minecraft;

public class Speed extends ClientModule {
   private static final Minecraft minecraft = Minecraft.func_71410_x();
   public final FloatOption multiplier = new FloatOption("multiplier", 1.0F, 0.0F, 10.0F);
   public final FloatOption friction = new FloatOption("friction", 1.0F, 0.0F, 10.0F);
   public final PercentOption strafe = new PercentOption("strafe", 0);

   public Speed() {
      super("Speed", false, 0);
   }

   private boolean canApplySpeed() {
      Scaffold conflictingModule = (Scaffold)ClientContext.moduleManager.modules.get(Scaffold.class);
      boolean otherEnabled = conflictingModule.isEnabled();
      if (!otherEnabled && MovementHelper.hasMovementInput()) {
         if (minecraft.field_71439_g.func_71024_bL().func_75116_a() <= 6) {
            return false;
         } else {
            return !minecraft.field_71439_g.func_70093_af() && !minecraft.field_71439_g.func_70090_H() && !minecraft.field_71439_g.func_180799_ab()
               ? !((EntityAccessor)minecraft.field_71439_g).getIsInWeb()
               : false;
         }
      } else {
         return false;
      }
   }

   private void applyStrafe() {
      double speed = MovementHelper.horizontalSpeed();
      MovementHelper.setHorizontalMotion(speed * ((100 - this.strafe.getValue()) / 100.0F), MovementHelper.motionYaw());
      MovementHelper.addHorizontalMotion(speed * (this.strafe.getValue().intValue() / 100.0F), MovementHelper.movementYaw());
      MovementHelper.setSpeedAlongMotion(speed);
   }

   @Listen(
      priority = -50
   )
   public void onMoveFlying(OnMoveFlying event) {
      if (this.isEnabled()) {
         if (this.canApplySpeed()) {
            if (minecraft.field_71439_g.field_70122_E) {
               minecraft.field_71439_g.field_70181_x = 0.42F;
               MovementHelper.setHorizontalMotion(MovementHelper.boostedMovementSpeed() * this.multiplier.getValue().floatValue(), MovementHelper.movementYaw());
            } else {
               if (this.friction.getValue() != 1.0F) {
                  event.setFriction(event.getFriction() * this.friction.getValue());
               }

               if (this.strafe.getValue() > 0) {
                  this.applyStrafe();
               }
            }
         }
      }
   }

   @Listen(
      priority = -50
   )
   public void onLivingUpdate(OnLivingUpdate event) {
      if (this.isEnabled() && this.canApplySpeed()) {
         minecraft.field_71439_g.field_71158_b.field_78901_c = false;
      }
   }
}
