package myau.module.combat;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import myau.core.ClientContext;
import myau.event.Listen;
import myau.event.input.OnKeyPress;
import myau.event.lifecycle.OnPostTick;
import myau.module.ClientModule;
import myau.rotation.RotationHelper;
import myau.setting.BooleanOption;
import myau.setting.FloatOption;
import myau.setting.IntegerOption;
import myau.setting.PercentOption;
import myau.util.Stopwatch;
import myau.util.entity.EntityHelper;
import myau.util.item.ItemHelper;
import myau.util.math.RandomHelper;
import myau.util.player.PlayerHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;

public class AimAssist extends ClientModule {
   private static final Minecraft minecraft = Minecraft.func_71410_x();
   private static final long ATTACK_COOLDOWN_MILLIS = 350L;
   private final Stopwatch lastAttackTimer = new Stopwatch();
   private final float[] snappedRotations = new float[]{-180.0F, 0.0F};
   private final float[] smoothedRotations = new float[]{-180.0F, 0.0F};
   public final FloatOption horizontalSpeed = new FloatOption("horizontal-speed", 3.0F, 0.0F, 30.0F);
   public final FloatOption verticalSpeed = new FloatOption("vertical-speed", 0.0F, 0.0F, 30.0F);
   public final PercentOption smoothing = new PercentOption("smoothing", 50);
   public final FloatOption range = new FloatOption("range", 4.5F, 3.0F, 8.0F);
   public final IntegerOption fov = new IntegerOption("fov", 90, 30, 360);
   public final BooleanOption requirePress = new BooleanOption("require-press", true);
   public final BooleanOption allowMining = new BooleanOption("allow-mining", true);
   public final BooleanOption weaponsOnly = new BooleanOption("weapons-only", true);
   public final BooleanOption allowTools = new BooleanOption("allow-tools", false, this.weaponsOnly::getValue);
   public final BooleanOption botCheck = new BooleanOption("bot-check", true);
   public final BooleanOption teams = new BooleanOption("teams", true);

   public AimAssist() {
      super("AimAssist", false, 0);
   }

   private boolean isValidAimTarget(EntityLivingBase candidate) {
      if (candidate == minecraft.field_71439_g || candidate == minecraft.field_71439_g.field_70154_o) {
         return false;
      } else if (candidate == minecraft.func_175606_aa() || candidate == minecraft.func_175606_aa().field_70154_o) {
         return false;
      } else if (candidate.field_70725_aQ > 0) {
         return false;
      } else if (RotationHelper.distanceToEntity(candidate) > this.range.getValue().floatValue()) {
         return false;
      } else if (RotationHelper.entityAimError(candidate) > this.fov.getValue().intValue()) {
         return false;
      } else if (RotationHelper.traceToEntity(candidate) != null) {
         return false;
      } else if (!(candidate instanceof EntityPlayer)) {
         return true;
      } else {
         EntityPlayer player = (EntityPlayer)candidate;
         if (EntityHelper.isFriend(player)) {
            return false;
         } else if (EntityHelper.isEnemy(player)) {
            return true;
         } else {
            return this.teams.getValue() && EntityHelper.isTeammate(player) ? false : !this.botCheck.getValue() || !EntityHelper.isBot(player);
         }
      }
   }

   private boolean isWithinEffectiveReach(EntityPlayer player) {
      Reach reach = (Reach)ClientContext.moduleManager.modules.get(Reach.class);
      double effectiveReach = reach.isEnabled() ? reach.range.getValue().floatValue() : 3.0;
      return RotationHelper.distanceToEntity(player) <= effectiveReach;
   }

   private EntityLivingBase findTarget() {
      KillAura killAura = (KillAura)ClientContext.moduleManager.modules.get(KillAura.class);
      if (killAura.isEnabled()) {
         EntityLivingBase auraTarget = killAura.getTargetEntity();
         if (auraTarget != null && this.isValidAimTarget(auraTarget)) {
            return auraTarget;
         }
      }

      List<EntityPlayer> candidates = minecraft.field_71441_e
         .field_72996_f
         .stream()
         .filter(EntityPlayer.class::isInstance)
         .map(EntityPlayer.class::cast)
         .filter(this::isValidAimTarget)
         .sorted(Comparator.comparingDouble(RotationHelper::distanceToEntity))
         .collect(Collectors.toList());
      if (candidates.isEmpty()) {
         return null;
      } else {
         if (candidates.stream().anyMatch(this::isWithinEffectiveReach)) {
            candidates.removeIf(player -> !this.isWithinEffectiveReach(player));
         }

         return (EntityLivingBase)candidates.get(0);
      }
   }

   private boolean isLookingAtBlock() {
      MovingObjectPosition hitResult = minecraft.field_71476_x;
      return hitResult != null && hitResult.field_72313_a == MovingObjectType.BLOCK;
   }

   @Listen
   public void onPostTick(OnPostTick event) {
      if (this.isEnabled() && minecraft.field_71462_r == null) {
         if (!this.weaponsOnly.getValue() || ItemHelper.isHoldingWeapon() || this.allowTools.getValue() && ItemHelper.isHoldingTool()) {
            boolean attackPressed = PlayerHelper.isAttackKeyDown();
            if (!this.allowMining.getValue() || !attackPressed || !this.isLookingAtBlock()) {
               if (!this.requirePress.getValue() || attackPressed || !this.lastAttackTimer.hasElapsed(350L)) {
                  EntityLivingBase target = this.findTarget();
                  if (target != null && !(RotationHelper.distanceToEntity(target) <= 0.0)) {
                     if (this.snappedRotations[0] != minecraft.field_71439_g.field_70177_z) {
                        this.snappedRotations[0] = minecraft.field_71439_g.field_70177_z;
                        this.smoothedRotations[0] = minecraft.field_71439_g.field_70177_z;
                     }

                     if (this.snappedRotations[1] != minecraft.field_71439_g.field_70125_A) {
                        this.snappedRotations[1] = minecraft.field_71439_g.field_70125_A;
                        this.smoothedRotations[1] = minecraft.field_71439_g.field_70125_A;
                     }

                     AxisAlignedBB bounds = target.func_174813_aQ();
                     double border = target.func_70111_Y();
                     float[] targetRotation = RotationHelper.aimAtBounds(bounds.func_72314_b(border, border, border));
                     float yawDelta = MathHelper.func_76142_g(targetRotation[0] - this.smoothedRotations[0]);
                     float pitchDelta = targetRotation[1] - this.smoothedRotations[1];
                     if (Math.abs(yawDelta) < 0.5F) {
                        yawDelta = 0.0F;
                     }

                     if (Math.abs(pitchDelta) < 0.5F) {
                        pitchDelta = 0.0F;
                     }

                     yawDelta = RotationHelper.clampAngleChange(yawDelta, this.horizontalSpeed.getValue() * 6.0F * RandomHelper.randomFloat(0.75F, 1.0F));
                     pitchDelta = RotationHelper.clampAngleChange(
                        pitchDelta, this.verticalSpeed.getValue() * 6.0F * RandomHelper.randomFloat(0.75F, 1.0F) * 0.5F
                     );
                     float smoothingAmount = this.smoothing.getValue().intValue() / 100.0F;
                     yawDelta = RotationHelper.scaleAngleChange(yawDelta, smoothingAmount + RandomHelper.randomFloat(-0.05F, 0.05F));
                     pitchDelta = RotationHelper.scaleAngleChange(pitchDelta, smoothingAmount + RandomHelper.randomFloat(-0.05F, 0.05F));
                     if (yawDelta == 0.0F && pitchDelta != 0.0F) {
                        yawDelta += RandomHelper.randomFloat(0.1F, 0.2F) * (RandomHelper.randomBoolean() ? 1.0F : -1.0F);
                     } else if (yawDelta != 0.0F && pitchDelta == 0.0F) {
                        pitchDelta += RandomHelper.randomFloat(0.1F, 0.2F) * (RandomHelper.randomBoolean() ? 1.0F : -1.0F);
                     }

                     this.smoothedRotations[0] = this.smoothedRotations[0] + yawDelta;
                     this.smoothedRotations[1] = this.smoothedRotations[1] + pitchDelta;
                     this.smoothedRotations[1] = MathHelper.func_76131_a(this.smoothedRotations[1], -90.0F, 90.0F);
                     this.snappedRotations[0] = RotationHelper.snapToMouseSensitivity(this.smoothedRotations[0], this.snappedRotations[0]);
                     this.snappedRotations[1] = RotationHelper.snapToMouseSensitivity(this.smoothedRotations[1], this.snappedRotations[1]);
                     this.snappedRotations[1] = MathHelper.func_76131_a(this.snappedRotations[1], -90.0F, 90.0F);
                     ClientContext.rotationSmoother.requestRotation(this.snappedRotations[0], this.snappedRotations[1], 0, false);
                  }
               }
            }
         }
      }
   }

   @Listen
   public void onKeyPress(OnKeyPress event) {
      if (event.getKeyCode() == minecraft.field_71474_y.field_74312_F.func_151463_i()) {
         AutoClicker autoClicker = (AutoClicker)ClientContext.moduleManager.modules.get(AutoClicker.class);
         if (!autoClicker.isEnabled()) {
            this.lastAttackTimer.reset();
         }
      }
   }
}
