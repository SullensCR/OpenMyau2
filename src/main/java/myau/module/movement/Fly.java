package myau.module.movement;

import myau.event.Listen;
import myau.event.player.OnMoveFlying;
import myau.event.player.OnRotationUpdate;
import myau.module.ClientModule;
import myau.setting.FloatOption;
import myau.util.input.InputHelper;
import myau.util.movement.MovementHelper;
import net.minecraft.client.Minecraft;

public class Fly extends ClientModule {
   private static final Minecraft minecraft = Minecraft.func_71410_x();
   private double verticalMotion;
   public final FloatOption horizontalSpeed = new FloatOption("horizontal-speed", 1.0F, 0.0F, 100.0F);
   public final FloatOption verticalSpeed = new FloatOption("vertical-speed", 1.0F, 0.0F, 100.0F);

   public Fly() {
      super("Fly", false, 0);
   }

   @Listen
   public void onMoveFlying(OnMoveFlying event) {
      boolean keepVerticalMotion = this.isEnabled();
      if (keepVerticalMotion) {
         keepVerticalMotion = minecraft.field_71439_g.field_70122_E;
         if (!keepVerticalMotion) {
            minecraft.field_71439_g.field_70181_x = this.verticalMotion;
         }

         MovementHelper.setSpeedAlongMotion(0.0);
         event.setFriction((float)MovementHelper.baseMovementSpeed() * this.horizontalSpeed.getValue());
      }
   }

   @Listen
   public void onRotationUpdate(OnRotationUpdate event) {
      if (this.isEnabled()) {
         this.verticalMotion = 0.0;
         if (minecraft.field_71462_r == null) {
            boolean jumpPressed = InputHelper.isKeyDown(minecraft.field_71474_y.field_74314_A.func_151463_i());
            if (jumpPressed) {
               this.verticalMotion = this.verticalMotion + this.verticalSpeed.getValue().doubleValue() * 0.42F;
            }

            if (InputHelper.isKeyDown(minecraft.field_71474_y.field_74311_E.func_151463_i())) {
               this.verticalMotion = this.verticalMotion - this.verticalSpeed.getValue().doubleValue() * 0.42F;
            }

            InputHelper.setKeyState(minecraft.field_71474_y.field_74311_E.func_151463_i(), false);
         }
      }
   }

   @Override
   public void onDisable() {
      minecraft.field_71439_g.field_70181_x = 0.0;
      MovementHelper.setSpeedAlongMotion(0.0);
      InputHelper.updateKeyState(minecraft.field_71474_y.field_74311_E.func_151463_i());
   }
}
