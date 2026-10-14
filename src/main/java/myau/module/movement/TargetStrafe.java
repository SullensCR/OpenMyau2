package myau.module.movement;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import myau.core.ClientContext;
import myau.event.Listen;
import myau.event.player.OnMoveFlying;
import myau.event.player.OnRotationUpdate;
import myau.event.render.OnRenderWorld;
import myau.module.ClientModule;
import myau.module.combat.KillAura;
import myau.module.render.HeadsUpDisplay;
import myau.rotation.RotationHelper;
import myau.setting.BooleanOption;
import myau.setting.ChoiceOption;
import myau.setting.FloatOption;
import myau.setting.IntegerOption;
import myau.util.entity.EntityHelper;
import myau.util.movement.MovementHelper;
import myau.util.player.PlayerHelper;
import myau.util.render.ColorHelper;
import myau.util.render.RenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;

public class TargetStrafe extends ClientModule {
   private static final Minecraft minecraft = Minecraft.func_71410_x();
   private EntityLivingBase strafeTarget;
   private float strafeYaw = Float.NaN;
   private int strafeDirection = 1;
   public final FloatOption radius = new FloatOption("radius", 1.0F, 0.0F, 6.0F);
   public final IntegerOption points = new IntegerOption("points", 6, 3, 24);
   public final BooleanOption requirePress = new BooleanOption("require-press", true);
   public final BooleanOption speedOnly = new BooleanOption("speed-only", true);
   public final ChoiceOption showTarget = new ChoiceOption("show-target", 1, new String[]{"NONE", "DEFAULT", "HUD"});

   public TargetStrafe() {
      super("TargetStrafe", false, 0);
   }

   private boolean canStrafe() {
      if (this.speedOnly.getValue()) {
         Speed speed = (Speed)ClientContext.moduleManager.modules.get(Speed.class);
         Fly fly = (Fly)ClientContext.moduleManager.modules.get(Fly.class);
         LongJump longJump = (LongJump)ClientContext.moduleManager.modules.get(LongJump.class);
         boolean movementModuleActive = speed.isEnabled() || fly.isEnabled() || longJump.isEnabled() && longJump.hasJumpStarted();
         if (!movementModuleActive) {
            return false;
         }
      }

      return !this.requirePress.getValue() || PlayerHelper.isJumpKeyDown();
   }

   private EntityLivingBase findTarget() {
      KillAura killAura = (KillAura)ClientContext.moduleManager.modules.get(KillAura.class);
      if (killAura.isEnabled() && killAura.isCombatInputAllowed()) {
         EntityLivingBase target = killAura.getTargetEntity();
         return EntityHelper.isLoaded(target) ? target : null;
      } else {
         return null;
      }
   }

   private Color targetColor(EntityLivingBase target) {
      if (target instanceof EntityPlayer) {
         EntityPlayer player = (EntityPlayer)target;
         if (EntityHelper.isFriend(player)) {
            return ClientContext.friendList.color();
         }

         if (EntityHelper.isEnemy(player)) {
            return ClientContext.enemyList.color();
         }
      }

      switch (this.showTarget.getValue()) {
         case 1:
            return target instanceof EntityPlayer ? EntityHelper.teamColor((EntityPlayer)target, 1.0F) : Color.WHITE;
         case 2:
            HeadsUpDisplay hud = (HeadsUpDisplay)ClientContext.moduleManager.modules.get(HeadsUpDisplay.class);
            return new Color(hud.getColor(System.currentTimeMillis()).getRGB());
         default:
            return Color.WHITE;
      }
   }

   private boolean isPointOverVoid(double x, double z) {
      return PlayerHelper.isBoundsOverVoid(
         new AxisAlignedBB(
            x - 0.015,
            minecraft.field_71439_g.field_70163_u,
            z - 0.015,
            x + 0.015,
            minecraft.field_71439_g.field_70163_u + minecraft.field_71439_g.field_70131_O,
            z + 0.015
         )
      );
   }

   private static int wrapPointIndex(int index, int size) {
      if (index < 0) {
         return size - 1;
      } else {
         return index >= size ? 0 : index;
      }
   }

   public float getStrafeYaw() {
      return this.strafeYaw;
   }

   @Listen(
      priority = 100
   )
   public void onRotationUpdate(OnRotationUpdate event) {
      if (this.isEnabled()) {
         boolean leftPressed = PlayerHelper.isLeftKeyDown();
         boolean rightPressed = PlayerHelper.isRightKeyDown();
         if (leftPressed ^ rightPressed) {
            this.strafeDirection = leftPressed ? 1 : -1;
         }

         if (!this.canStrafe()) {
            this.strafeTarget = null;
            this.strafeYaw = Float.NaN;
         } else {
            this.strafeTarget = this.findTarget();
            if (this.strafeTarget == null) {
               this.strafeYaw = Float.NaN;
            } else {
               List<StrafeVector> orbitPoints = new ArrayList<>();

               for (int index = 0; index < this.points.getValue(); index++) {
                  double angle = index * ((Math.PI * 2) / this.points.getValue().intValue());
                  orbitPoints.add(
                     new StrafeVector(this.radius.getValue().floatValue() * Math.cos(angle), this.radius.getValue().floatValue() * Math.sin(angle))
                  );
               }

               if (orbitPoints.isEmpty()) {
                  this.strafeTarget = null;
                  this.strafeYaw = Float.NaN;
               } else {
                  double nearestDistance = 0.0;
                  int nearestIndex = -1;

                  for (int index = 0; index < orbitPoints.size(); index++) {
                     StrafeVector point = orbitPoints.get(index);
                     double distance = minecraft.field_71439_g
                        .func_70011_f(
                           this.strafeTarget.field_70165_t + point.x(), minecraft.field_71439_g.field_70163_u, this.strafeTarget.field_70161_v + point.z()
                        );
                     if (nearestIndex == -1 || distance < nearestDistance) {
                        nearestDistance = distance;
                        nearestIndex = index;
                     }
                  }

                  if (minecraft.field_71439_g.field_70123_F) {
                     this.strafeDirection *= -1;
                  }

                  int nextIndex = wrapPointIndex(nearestIndex + this.strafeDirection, orbitPoints.size());
                  StrafeVector nextPoint = orbitPoints.get(nextIndex);
                  double targetX = this.strafeTarget.field_70165_t + nextPoint.x();
                  double targetZ = this.strafeTarget.field_70161_v + nextPoint.z();
                  if (this.isPointOverVoid(targetX, targetZ)) {
                     this.strafeDirection *= -1;
                     nextIndex = wrapPointIndex(nearestIndex + this.strafeDirection, orbitPoints.size());
                     nextPoint = orbitPoints.get(nextIndex);
                     targetX = this.strafeTarget.field_70165_t + nextPoint.x();
                     targetZ = this.strafeTarget.field_70161_v + nextPoint.z();
                  }

                  this.strafeYaw = RotationHelper.rotationsFromDelta(
                     targetX - minecraft.field_71439_g.field_70165_t, 0.0, targetZ - minecraft.field_71439_g.field_70161_v
                  )[0];
                  event.setMovementYaw(this.strafeYaw, 10);
               }
            }
         }
      }
   }

   @Listen
   public void onMoveFlying(OnMoveFlying event) {
      if (this.isEnabled() && !Float.isNaN(this.strafeYaw) && MovementHelper.hasMovementInput()) {
         event.setStrafe(0.0F);
         event.setForward(1.0F);
      }
   }

   @Listen
   public void onRenderWorld(OnRenderWorld event) {
      if (this.isEnabled() && EntityHelper.isLoaded(this.strafeTarget) && this.showTarget.getValue() != 0) {
         Color color = this.targetColor(this.strafeTarget);
         RenderHelper.beginOverlay();
         RenderHelper.drawEntityCircle(
            this.strafeTarget, this.radius.getValue().floatValue(), this.points.getValue(), 3.0F, ColorHelper.scaleBrightness(color, 0.2F).getRGB()
         );
         RenderHelper.drawEntityCircle(this.strafeTarget, this.radius.getValue().floatValue(), this.points.getValue(), 1.5F, color.getRGB());
         RenderHelper.endOverlay();
      }
   }

   @Override
   public void onDisable() {
      this.strafeTarget = null;
      this.strafeYaw = Float.NaN;
   }
}
