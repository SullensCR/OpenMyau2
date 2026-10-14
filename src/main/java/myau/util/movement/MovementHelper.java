package myau.util.movement;

import myau.core.ClientContext;
import myau.module.movement.TargetStrafe;
import myau.rotation.RotationStore;
import net.minecraft.client.Minecraft;
import net.minecraft.potion.Potion;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovementInput;

public final class MovementHelper {
   private static final Minecraft minecraft = Minecraft.func_71410_x();

   private MovementHelper() {
   }

   public static boolean hasMovementInput() {
      return minecraft.field_71474_y.field_74351_w.func_151470_d() != minecraft.field_71474_y.field_74368_y.func_151470_d()
         || minecraft.field_71474_y.field_74370_x.func_151470_d() != minecraft.field_71474_y.field_74366_z.func_151470_d();
   }

   public static int forwardInput() {
      int input = 0;
      if (minecraft.field_71474_y.field_74351_w.func_151470_d()) {
         input++;
      }

      if (minecraft.field_71474_y.field_74368_y.func_151470_d()) {
         input--;
      }

      return input;
   }

   public static int strafeInput() {
      int input = 0;
      if (minecraft.field_71474_y.field_74370_x.func_151470_d()) {
         input++;
      }

      if (minecraft.field_71474_y.field_74366_z.func_151470_d()) {
         input--;
      }

      return input;
   }

   public static float movementYaw() {
      float baseYaw = RotationStore.isActive() ? RotationStore.movementYaw() : minecraft.field_71439_g.field_70177_z;
      return movementYawForInput(baseYaw, minecraft.field_71439_g.field_71158_b.field_78900_b, minecraft.field_71439_g.field_71158_b.field_78902_a);
   }

   public static float movementYawForInput(float yaw, float forward, float strafe) {
      TargetStrafe targetStrafe = (TargetStrafe)ClientContext.moduleManager.modules.get(TargetStrafe.class);
      if (targetStrafe != null && targetStrafe.isEnabled()) {
         float strafeYaw = targetStrafe.getStrafeYaw();
         if (!Float.isNaN(strafeYaw)) {
            return strafeYaw;
         }
      }

      if (forward < 0.0F) {
         yaw += 180.0F;
      }

      if (strafe != 0.0F) {
         float forwardFactor = forward == 0.0F ? 1.0F : 0.5F * Math.signum(forward);
         yaw += -90.0F * forwardFactor * Math.signum(strafe);
      }

      return MathHelper.func_76142_g(yaw);
   }

   public static float motionYaw() {
      return horizontalSpeed() == 0.0
         ? MathHelper.func_76142_g(minecraft.field_71439_g.field_70177_z)
         : MathHelper.func_76142_g((float)Math.toDegrees(Math.atan2(minecraft.field_71439_g.field_70179_y, minecraft.field_71439_g.field_70159_w)) - 90.0F);
   }

   public static double baseMovementSpeed() {
      double speed = 0.28015;
      if (speedPotionTicks() > 0) {
         speed *= 1.0 + 0.15 * speedPotionLevel();
      }

      return speed;
   }

   public static double boostedSpeedForPotionLevel(int potionLevel) {
      double speed = 0.452;
      if (potionLevel == 1) {
         return speed * 1.1;
      } else {
         if (potionLevel >= 2) {
            speed *= 1.2;
         }

         return speed;
      }
   }

   public static double boostedMovementSpeed() {
      int potionLevel = speedPotionTicks() > 0 ? speedPotionLevel() : 0;
      return boostedSpeedForPotionLevel(potionLevel);
   }

   public static double horizontalSpeed() {
      return horizontalMagnitude(minecraft.field_71439_g.field_70159_w, minecraft.field_71439_g.field_70179_y);
   }

   public static double horizontalMagnitude(double motionX, double motionZ) {
      return Math.hypot(motionX, motionZ);
   }

   public static void setSpeedAlongMotion(double speed) {
      setHorizontalMotion(speed, motionYaw());
   }

   public static void setHorizontalMotion(double speed, float yaw) {
      minecraft.field_71439_g.field_70159_w = -Math.sin(Math.toRadians(yaw)) * speed;
      minecraft.field_71439_g.field_70179_y = Math.cos(Math.toRadians(yaw)) * speed;
   }

   public static void addHorizontalMotion(double speed, float yaw) {
      minecraft.field_71439_g.field_70159_w = minecraft.field_71439_g.field_70159_w + -Math.sin(Math.toRadians(yaw)) * speed;
      minecraft.field_71439_g.field_70179_y = minecraft.field_71439_g.field_70179_y + Math.cos(Math.toRadians(yaw)) * speed;
   }

   public static int speedPotionLevel() {
      return !minecraft.field_71439_g.func_70644_a(Potion.field_76424_c) ? 0 : minecraft.field_71439_g.func_70660_b(Potion.field_76424_c).func_76458_c() + 1;
   }

   public static int speedPotionTicks() {
      return !minecraft.field_71439_g.func_70644_a(Potion.field_76424_c) ? 0 : minecraft.field_71439_g.func_70660_b(Potion.field_76424_c).func_76459_b();
   }

   public static float groundAcceleration() {
      float friction = minecraft.field_71439_g
            .field_70170_p
            .func_180495_p(
               new BlockPos(
                  MathHelper.func_76128_c(minecraft.field_71439_g.field_70165_t),
                  MathHelper.func_76128_c(minecraft.field_71439_g.func_174813_aQ().field_72338_b) - 1,
                  MathHelper.func_76128_c(minecraft.field_71439_g.field_70161_v)
               )
            )
            .func_177230_c()
            .field_149765_K
         * 0.91F;
      return minecraft.field_71439_g.func_70689_ay() * (0.16277136F / (friction * friction * friction));
   }

   public static double[] inputAcceleration() {
      float strafe = strafeInput() * 0.98F;
      float forward = forwardInput() * 0.98F;
      float magnitudeSquared = strafe * strafe + forward * forward;
      if (magnitudeSquared < 1.0E-4F) {
         return new double[]{0.0, 0.0};
      } else {
         float magnitude = MathHelper.func_76129_c(magnitudeSquared);
         if (magnitude < 1.0F) {
            magnitude = 1.0F;
         }

         float acceleration = groundAcceleration() / magnitude;
         strafe *= acceleration;
         forward *= acceleration;
         float sinYaw = MathHelper.func_76126_a(minecraft.field_71439_g.field_70177_z * (float) Math.PI / 180.0F);
         float cosYaw = MathHelper.func_76134_b(minecraft.field_71439_g.field_70177_z * (float) Math.PI / 180.0F);
         return new double[]{strafe * cosYaw - forward * sinYaw, forward * cosYaw + strafe * sinYaw};
      }
   }

   public static void correctMovementForYaw(float yaw) {
      float relativeYaw = MathHelper.func_76142_g(movementYawForInput(minecraft.field_71439_g.field_70177_z, forwardInput(), strafeInput()) - yaw + 22.5F);
      int direction = (int)(relativeYaw + 180.0F) / 45 % 8;
      switch (direction) {
         case 0:
            setMovementInput(-1.0F, 0.0F);
            break;
         case 1:
            setMovementInput(-1.0F, 1.0F);
            break;
         case 2:
            setMovementInput(0.0F, 1.0F);
            break;
         case 3:
            setMovementInput(1.0F, 1.0F);
            break;
         case 4:
            setMovementInput(1.0F, 0.0F);
            break;
         case 5:
            setMovementInput(1.0F, -1.0F);
            break;
         case 6:
            setMovementInput(0.0F, -1.0F);
            break;
         case 7:
            setMovementInput(-1.0F, -1.0F);
      }

      if (minecraft.field_71439_g.field_71158_b.field_78899_d) {
         minecraft.field_71439_g.field_71158_b.field_78900_b *= 0.3F;
         minecraft.field_71439_g.field_71158_b.field_78902_a *= 0.3F;
      }
   }

   private static void setMovementInput(float forward, float strafe) {
      MovementInput input = minecraft.field_71439_g.field_71158_b;
      input.field_78900_b = forward;
      input.field_78902_a = strafe;
   }
}
