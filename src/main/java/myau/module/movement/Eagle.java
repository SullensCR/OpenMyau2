package myau.module.movement;

import java.util.Objects;
import myau.event.Listen;
import myau.event.input.OnRightClick;
import myau.event.lifecycle.OnPreTick;
import myau.event.player.OnMovementInput;
import myau.module.ClientModule;
import myau.setting.BooleanOption;
import myau.setting.IntegerOption;
import myau.setting.PercentOption;
import myau.util.item.ItemHelper;
import myau.util.math.Maths;
import myau.util.movement.MovementHelper;
import myau.util.player.PlayerHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import org.apache.commons.lang3.RandomUtils;

public class Eagle extends ClientModule {
   private static final Minecraft minecraft = Minecraft.func_71410_x();
   private int delayTicksRemaining = 0;
   public final IntegerOption minDelay = new IntegerOption("min-delay", 2, 0, 10);
   public final IntegerOption maxDelay = new IntegerOption("max-delay", 3, 0, 10);
   public final PercentOption offset = new PercentOption("offset", 100);
   public final BooleanOption align = new BooleanOption("align", true);
   public final BooleanOption directionCheck = new BooleanOption("direction-check", true);
   public final BooleanOption pitchCheck = new BooleanOption("pitch-check", true);
   public final BooleanOption blocksOnly = new BooleanOption("blocks-only", true);

   public Eagle() {
      super("Eagle", false, 0);
   }

   private boolean isNearEdge() {
      double[] movement = MovementHelper.inputAcceleration();
      return PlayerHelper.isNarrowOffsetCollisionFree(
         minecraft.field_71439_g.field_70159_w + movement[0],
         -1.0,
         minecraft.field_71439_g.field_70179_y + movement[1],
         this.offset.getValue().intValue() / 100.0F
      );
   }

   private boolean canActivate() {
      if (this.directionCheck.getValue() && minecraft.field_71474_y.field_74351_w.func_151470_d()) {
         return false;
      } else if (this.pitchCheck.getValue() && minecraft.field_71439_g.field_70125_A < 69.0F) {
         return false;
      } else {
         return this.blocksOnly.getValue() && !ItemHelper.isHoldingPlaceableBlock() ? false : minecraft.field_71439_g.field_70122_E;
      }
   }

   @Listen
   public void onPreTick(OnPreTick event) {
      if (this.isEnabled()) {
         if (this.delayTicksRemaining > 0) {
            this.delayTicksRemaining--;
         }

         if (this.delayTicksRemaining == 0) {
            if (this.isNearEdge()) {
               this.delayTicksRemaining = RandomUtils.nextInt(this.minDelay.getValue(), this.maxDelay.getValue() + 1);
            }
         }
      }
   }

   @Listen
   public void onRightClick(OnRightClick event) {
      if (this.isEnabled()
         && minecraft.field_71462_r == null
         && this.align.getValue()
         && !minecraft.field_71474_y.field_74311_E.func_151470_d()
         && this.canActivate()) {
         if (this.delayTicksRemaining > 0 || this.isNearEdge()) {
            float movementYaw = Maths.toRadians(
               MovementHelper.movementYawForInput(minecraft.field_71439_g.field_70177_z, MovementHelper.forwardInput(), MovementHelper.strafeInput()) - 180.0F
            );
            float xDirection = -MathHelper.func_76126_a(movementYaw);
            float zDirection = MathHelper.func_76134_b(movementYaw);
            if (!(Math.abs(xDirection) < 0.1F) && !(Math.abs(zDirection) < 0.1F)) {
               AxisAlignedBB bounds = minecraft.field_71439_g.func_174813_aQ();
               double xEdge = xDirection >= 0.0F ? bounds.field_72336_d : bounds.field_72340_a;
               double zEdge = zDirection >= 0.0F ? bounds.field_72334_f : bounds.field_72339_c;
               double xAlignment = Math.abs(xEdge - Math.rint(xEdge));
               double zAlignment = Math.abs(zEdge - Math.rint(zEdge));
               if (Math.abs(xAlignment - zAlignment) > 0.125) {
                  event.setCancelled(true);
               }
            }
         }
      }
   }

   @Listen(
      priority = -100
   )
   public void onMovementInput(OnMovementInput event) {
      if (this.isEnabled() && minecraft.field_71462_r == null && !minecraft.field_71439_g.field_71158_b.field_78899_d && this.canActivate()) {
         if (this.delayTicksRemaining > 0 || this.isNearEdge()) {
            minecraft.field_71439_g.field_71158_b.field_78899_d = true;
            minecraft.field_71439_g.field_71158_b.field_78902_a *= 0.3F;
            minecraft.field_71439_g.field_71158_b.field_78900_b *= 0.3F;
         }
      }
   }

   @Override
   public void onDisable() {
      this.delayTicksRemaining = 0;
   }

   @Override
   public void onSettingChanged(String settingName) {
      if ("min-delay".equals(settingName)) {
         if (this.minDelay.getValue() > this.maxDelay.getValue()) {
            this.maxDelay.setValue(this.minDelay.getValue());
         }
      } else if ("max-delay".equals(settingName) && this.minDelay.getValue() > this.maxDelay.getValue()) {
         this.minDelay.setValue(this.maxDelay.getValue());
      }
   }

   @Override
   public String[] getHudInfo() {
      return Objects.equals(this.minDelay.getValue(), this.maxDelay.getValue())
         ? new String[]{this.minDelay.getValue().toString()}
         : new String[]{String.format("%d-%d", this.minDelay.getValue(), this.maxDelay.getValue())};
   }
}
