package myau.module.misc;

import com.google.common.base.CaseFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import myau.core.ClientContext;
import myau.event.Listen;
import myau.event.lifecycle.OnPostTick;
import myau.event.network.OnPacketReceive;
import myau.event.network.OnPacketSend;
import myau.event.player.OnMoveFlying;
import myau.event.player.OnMovementInput;
import myau.mixin.accessor.EntityPlayerSPAccessor;
import myau.mixin.accessor.MinecraftAccessor;
import myau.module.ClientModule;
import myau.module.player.NoFall;
import myau.setting.ChoiceOption;
import myau.setting.FloatOption;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C03PacketPlayer.C05PacketPlayerLook;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;

public class GameTimer extends ClientModule {
   private static final Minecraft minecraft = Minecraft.func_71410_x();
   private static final DecimalFormat speedFormat = new DecimalFormat("0.0#", new DecimalFormatSymbols(Locale.US));
   private boolean timerSpeedOwned;
   private boolean allowNextMovementPacket;
   private boolean movementInputCleared;
   private boolean motionStored;
   private double savedMotionX;
   private double savedMotionY;
   private double savedMotionZ;
   private int bufferedMovementPackets;
   public final ChoiceOption mode = new ChoiceOption("mode", 0, new String[]{"VANILLA", "HYPIXEL"});
   public final FloatOption speed = new FloatOption("speed", 1.0F, 0.0F, 10.0F, this::showSpeedSetting);

   public GameTimer() {
      super("Timer", false, 0);
   }

   private boolean readyToMove() {
      return this.bufferedMovementPackets >= 10 || ((EntityPlayerSPAccessor)minecraft.field_71439_g).getPositionUpdateTicks() >= 20;
   }

   private boolean showSpeedSetting() {
      int selection = this.mode.getValue();
      return selection == 0;
   }

   @Listen
   public void onPostTick(OnPostTick event) {
      boolean enabled = this.isEnabled();
      if (enabled && this.mode.getValue() == 0) {
         if (minecraft.field_71462_r != null) {
            this.onDisable();
         } else {
            NoFall conflictingModule = (NoFall)ClientContext.moduleManager.modules.get(NoFall.class);
            if (conflictingModule.isPacketModeActive()) {
               this.timerSpeedOwned = false;
            } else {
               this.timerSpeedOwned = true;
               ((MinecraftAccessor)minecraft).getTimer().field_74278_d = this.speed.getValue();
            }
         }
      }
   }

   private void clearMovementInput() {
      minecraft.field_71439_g.field_71158_b.field_78902_a = 0.0F;
      minecraft.field_71439_g.field_71158_b.field_78900_b = 0.0F;
      minecraft.field_71439_g.field_71158_b.field_78901_c = false;
      minecraft.field_71439_g.field_71158_b.field_78899_d = false;
   }

   @Listen(
      priority = -1000
   )
   public void onMovementInput(OnMovementInput event) {
      boolean enabled = this.isEnabled();
      if (enabled && this.mode.getValue() == 1 && !this.readyToMove()) {
         this.movementInputCleared = true;
         this.clearMovementInput();
      } else {
         if (this.movementInputCleared) {
            this.movementInputCleared = false;
            this.clearMovementInput();
         }
      }
   }

   @Listen(
      priority = -1000
   )
   public void onMoveFlying(OnMoveFlying event) {
      boolean enabled = this.isEnabled();
      if (enabled && this.mode.getValue() == 1 && !this.readyToMove()) {
         if (!this.motionStored) {
            this.motionStored = true;
            this.savedMotionX = minecraft.field_71439_g.field_70159_w;
            this.savedMotionY = minecraft.field_71439_g.field_70181_x;
            this.savedMotionZ = minecraft.field_71439_g.field_70179_y;
         }

         minecraft.field_71439_g.field_70159_w = 0.0;
         if (!minecraft.field_71439_g.field_70122_E) {
            minecraft.field_71439_g.field_70181_x = 0.0;
         }

         minecraft.field_71439_g.field_70179_y = 0.0;
      } else {
         if (this.motionStored) {
            this.motionStored = false;
            minecraft.field_71439_g.field_70159_w = this.savedMotionX;
            minecraft.field_71439_g.field_70181_x = this.savedMotionY;
            minecraft.field_71439_g.field_70179_y = this.savedMotionZ;
         }
      }
   }

   @Listen(
      priority = -1000
   )
   public void onPacketSend(OnPacketSend event) {
      boolean enabled = this.isEnabled();
      if (enabled && !event.isCancelled() && event.getPacket() instanceof C03PacketPlayer) {
         if (this.allowNextMovementPacket) {
            this.allowNextMovementPacket = false;
         } else if (this.mode.getValue() == 1 && !this.readyToMove()) {
            this.bufferedMovementPackets++;
            if (!(event.getPacket() instanceof C05PacketPlayerLook)) {
               event.setCancelled(true);
            }
         } else {
            this.bufferedMovementPackets = 0;
         }
      }
   }

   @Listen(
      priority = -1000
   )
   public void onPacketReceive(OnPacketReceive event) {
      if (this.isEnabled()) {
         if (this.mode.getValue() == 1 && !event.isCancelled() && event.getPacket() instanceof S08PacketPlayerPosLook) {
            this.allowNextMovementPacket = true;
            this.motionStored = false;
         }
      }
   }

   @Override
   public void onDisable() {
      if (this.timerSpeedOwned) {
         this.timerSpeedOwned = false;
         ((MinecraftAccessor)minecraft).getTimer().field_74278_d = 1.0F;
      }

      this.bufferedMovementPackets = 0;
   }

   @Override
   public void onSettingChanged(String name) {
      if (this.isEnabled()) {
         this.onDisable();
      }
   }

   @Override
   public String[] getHudInfo() {
      int selection = this.mode.getValue();
      switch (selection) {
         case 0:
            return new String[]{speedFormat.format(this.speed.getValue())};
         case 1:
            return new String[]{CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, this.mode.getModeName())};
         default:
            return new String[0];
      }
   }
}
