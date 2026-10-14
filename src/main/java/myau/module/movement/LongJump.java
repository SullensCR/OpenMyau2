package myau.module.movement;

import myau.event.Listen;
import myau.event.input.OnKeyPress;
import myau.event.lifecycle.OnPostTick;
import myau.event.lifecycle.OnPreTick;
import myau.event.network.OnPacketReceive;
import myau.event.player.OnMoveFlying;
import myau.event.player.OnMovementInput;
import myau.event.player.OnRotationUpdate;
import myau.event.player.OnVelocity;
import myau.mixin.accessor.PlayerControllerMPAccessor;
import myau.module.ClientModule;
import myau.rotation.RotationHelper;
import myau.rotation.RotationStore;
import myau.setting.ChoiceOption;
import myau.setting.FloatOption;
import myau.setting.PercentOption;
import myau.util.Stopwatch;
import myau.util.chat.ChatHelper;
import myau.util.math.RandomHelper;
import myau.util.movement.MovementHelper;
import myau.util.network.PacketHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemFireball;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;

public class LongJump extends ClientModule {
   private static final Minecraft minecraft = Minecraft.func_71410_x();
   private final Stopwatch activationTimer = new Stopwatch();
   private final Stopwatch damageWaitTimer = new Stopwatch();
   private boolean jumpStarted;
   private int jumpTicks;
   private int jumpPhase;
   private boolean launchRotationReady;
   private boolean fireballLaunchAttempted;
   private int previousHotbarSlot = -1;
   public final ChoiceOption mode = new ChoiceOption("mode", 0, new String[]{"FIREBALL", "FIREBALL_MANUAL", "FIREBALL_HIGH", "FIREBALL_FLAT"});
   public final FloatOption motion = new FloatOption("motion", 1.0F, 1.0F, 20.0F);
   public final FloatOption speedMotion = new FloatOption("speed-motion", 1.0F, 1.0F, 20.0F);
   public final PercentOption strafe = new PercentOption("strafe", 0);

   public LongJump() {
      super("LongJump", false, 0);
   }

   private int findFireballSlot() {
      if (minecraft.field_71439_g == null) {
         return -1;
      } else {
         for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = minecraft.field_71439_g.field_71071_by.func_70301_a(slot);
            if (stack != null && stack.func_77973_b() instanceof ItemFireball) {
               return slot;
            }
         }

         return -1;
      }
   }

   private double getConfiguredMotion() {
      return MovementHelper.speedPotionLevel() > 0 ? this.speedMotion.getValue().doubleValue() : this.motion.getValue().doubleValue();
   }

   public boolean isAutomaticFireballMode() {
      int currentMode = this.mode.getValue();
      return currentMode == 0 || currentMode == 2 || currentMode == 3;
   }

   public boolean isManualFireballMode() {
      return this.mode.getValue() == 1;
   }

   public boolean isFireballMode() {
      return this.isAutomaticFireballMode() || this.isManualFireballMode();
   }

   public boolean isAwaitingLaunchVelocity() {
      return !this.activationTimer.hasElapsed(1000L) && !this.jumpStarted;
   }

   public boolean hasJumpStarted() {
      return this.jumpStarted;
   }

   @Listen(
      priority = 100
   )
   public void onVelocity(OnVelocity event) {
      if (this.isEnabled() && !event.isCancelled()) {
         if (this.isManualFireballMode() || this.isAutomaticFireballMode()) {
            if (this.isAwaitingLaunchVelocity()) {
               event.setCancelled(true);
               this.jumpStarted = true;
               this.jumpTicks = 0;
            }
         }
      }
   }

   @Listen(
      priority = 100
   )
   public void onPreTick(OnPreTick event) {
      if (this.isEnabled() && this.isAutomaticFireballMode() && !this.fireballLaunchAttempted && this.launchRotationReady) {
         int fireballSlot = this.findFireballSlot();
         if (fireballSlot != -1) {
            this.previousHotbarSlot = minecraft.field_71439_g.field_71071_by.field_70461_c;
            minecraft.field_71439_g.field_71071_by.field_70461_c = fireballSlot;
            ((PlayerControllerMPAccessor)minecraft.field_71442_b).callSyncCurrentPlayItem();
            PacketHelper.send(new C08PacketPlayerBlockPlacement(minecraft.field_71439_g.func_70694_bm()));
            this.activationTimer.reset();
            this.fireballLaunchAttempted = true;
         }
      }
   }

   @Listen(
      priority = 100
   )
   public void onPostTick(OnPostTick event) {
      if (this.previousHotbarSlot != -1) {
         minecraft.field_71439_g.field_71071_by.field_70461_c = this.previousHotbarSlot;
         this.previousHotbarSlot = -1;
      }
   }

   @Listen
   public void onRotationUpdate(OnRotationUpdate event) {
      if (this.isEnabled() && this.isFireballMode()) {
         if (this.jumpStarted) {
            this.jumpTicks++;
            if (this.jumpTicks == 1) {
               switch (this.mode.getValue()) {
                  case 0:
                  case 1:
                     this.jumpPhase = 0;
                     break;
                  case 2:
                     this.jumpPhase = 1;
                     break;
                  case 3:
                     this.jumpPhase = MovementHelper.hasMovementInput() ? 2 : 1;
               }
            }

            if (this.jumpTicks == 2 && MovementHelper.hasMovementInput()) {
               MovementHelper.setSpeedAlongMotion(MovementHelper.horizontalSpeed() * this.getConfiguredMotion());
            }

            if (this.jumpTicks >= 1 && this.jumpTicks <= 30) {
               switch (this.jumpPhase) {
                  case 1:
                     if (this.jumpTicks == 1) {
                        minecraft.field_71439_g.field_70181_x *= 0.75;
                     } else {
                        double adjustedMotionY = minecraft.field_71439_g.field_70181_x / 0.98F + 0.055;
                        if (adjustedMotionY > 0.0) {
                           minecraft.field_71439_g.field_70181_x = adjustedMotionY;
                        }
                     }
                     break;
                  case 2:
                     if (this.jumpTicks == 1) {
                        minecraft.field_71439_g.field_70181_x *= 0.75;
                     } else {
                        minecraft.field_71439_g.field_70181_x = 0.01 + this.jumpTicks * 0.003;
                     }
               }
            }

            if (this.jumpTicks >= 30) {
               this.jumpStarted = false;
               this.jumpTicks = 0;
               this.jumpPhase = 0;
               if (this.isAutomaticFireballMode()) {
                  this.setEnabled(false);
               }

               return;
            }
         }

         if (this.isAutomaticFireballMode() && !this.jumpStarted) {
            if (this.damageWaitTimer.hasElapsed(1500L)) {
               this.setEnabled(false);
            } else {
               this.launchRotationReady = true;
               float yaw = RotationHelper.snapToMouseSensitivity(
                  minecraft.field_71439_g.field_70177_z - 180.0F - RandomHelper.randomFloat(0.0F, 1.0F), event.getPreviousYaw()
               );
               float pitch = RotationHelper.snapToMouseSensitivity(89.0F + RandomHelper.randomFloat(-0.25F, 0.25F), event.getPreviousPitch());
               event.setRotation(yaw, pitch, 4);
               event.setMovementYaw(yaw, 4);
            }
         }
      }
   }

   @Listen
   public void onMovementInput(OnMovementInput event) {
      if (this.isEnabled() && RotationStore.isActive() && RotationStore.movementPriority() == 4.0F && MovementHelper.hasMovementInput()) {
         MovementHelper.correctMovementForYaw(RotationStore.movementYaw());
      }
   }

   @Listen
   public void onMoveFlying(OnMoveFlying event) {
      if (this.isEnabled() && this.isFireballMode() && this.jumpStarted && this.jumpTicks >= 5 && this.jumpTicks <= 30 && this.strafe.getValue() > 0) {
         double horizontalSpeed = MovementHelper.horizontalSpeed();
         MovementHelper.setHorizontalMotion(horizontalSpeed * ((100 - this.strafe.getValue()) / 100.0F), MovementHelper.motionYaw());
         MovementHelper.addHorizontalMotion(horizontalSpeed * (this.strafe.getValue().intValue() / 100.0F), MovementHelper.movementYaw());
         MovementHelper.setSpeedAlongMotion(horizontalSpeed);
      }
   }

   @Listen
   public void onKeyPress(OnKeyPress event) {
      if (event.getKeyCode() == minecraft.field_71474_y.field_74313_G.func_151463_i()) {
         ItemStack stack = minecraft.field_71439_g.field_71071_by.func_70448_g();
         if (stack != null && stack.func_77973_b() instanceof ItemFireball) {
            this.activationTimer.reset();
         }
      }
   }

   @Listen(
      priority = 50
   )
   public void onPacketReceive(OnPacketReceive event) {
      if (this.isEnabled() && !event.isCancelled() && event.getPacket() instanceof S08PacketPlayerPosLook) {
         this.jumpStarted = false;
         this.jumpTicks = 0;
         this.jumpPhase = 0;
         if (this.isAutomaticFireballMode()) {
            this.setEnabled(false);
         }
      }
   }

   @Override
   public void onEnable() {
      this.damageWaitTimer.reset();
      if (this.isAutomaticFireballMode() && this.findFireballSlot() == -1) {
         this.setEnabled(false);
         ChatHelper.addFormattedMessage(String.format("%s%s: &cNo fireball found in your hotbar!&r", myau.util.chat.ChatPrefix.get(), this.getName()));
      }
   }

   @Override
   public void onDisable() {
      this.jumpStarted = false;
      this.jumpTicks = 0;
      this.jumpPhase = 0;
      this.launchRotationReady = false;
      this.fireballLaunchAttempted = false;
   }

   @Override
   public String[] getHudInfo() {
      return new String[]{"Fireball"};
   }
}
