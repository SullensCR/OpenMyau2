package myau.module.combat;

import myau.event.Listen;
import myau.event.network.OnPacketSend;
import myau.event.player.OnMovementInput;
import myau.module.ClientModule;
import myau.setting.FloatOption;
import myau.setting.PercentOption;
import myau.util.Stopwatch;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C02PacketUseEntity.Action;
import net.minecraft.potion.Potion;

public class WTap extends ClientModule {
   private static final Minecraft minecraft = Minecraft.func_71410_x();
   private final Stopwatch attackCooldownTimer = new Stopwatch();
   private int chanceAccumulator = 0;
   private boolean wTapActive = false;
   private boolean forwardSuppressionStarted = false;
   private long delayRemainingMillis = 0L;
   private long suppressionRemainingMillis = 0L;
   public final FloatOption delay = new FloatOption("delay", 5.5F, 0.0F, 10.0F);
   public final FloatOption duration = new FloatOption("duration", 1.5F, 1.0F, 5.0F);
   public final PercentOption chance = new PercentOption("chance", 100);

   public WTap() {
      super("WTap", false, 0);
   }

   private boolean canContinueWTap() {
      if (!(minecraft.field_71439_g.field_71158_b.field_78900_b < 0.8F) && !minecraft.field_71439_g.field_70123_F) {
         if (minecraft.field_71439_g.func_71024_bL().func_75116_a() <= 6.0F && !minecraft.field_71439_g.field_71075_bZ.field_75101_c) {
            return false;
         } else if (minecraft.field_71439_g.func_70093_af()) {
            return true;
         } else {
            return !minecraft.field_71439_g.func_71039_bw() && !minecraft.field_71439_g.func_70644_a(Potion.field_76440_q)
               ? minecraft.field_71474_y.field_151444_V.func_151470_d()
               : false;
         }
      } else {
         return false;
      }
   }

   @Listen(
      priority = -100
   )
   public void onMovementInput(OnMovementInput event) {
      if (this.wTapActive) {
         if (!this.forwardSuppressionStarted && !this.canContinueWTap()) {
            this.wTapActive = false;

            while (this.delayRemainingMillis > 0L) {
               this.delayRemainingMillis -= 50L;
            }

            while (this.suppressionRemainingMillis > 0L) {
               this.suppressionRemainingMillis -= 50L;
            }
         } else if (this.delayRemainingMillis > 0L) {
            this.delayRemainingMillis -= 50L;
         } else {
            if (this.suppressionRemainingMillis > 0L) {
               this.suppressionRemainingMillis -= 50L;
               this.forwardSuppressionStarted = true;
               minecraft.field_71439_g.field_71158_b.field_78900_b = 0.0F;
            }

            if (this.suppressionRemainingMillis <= 0L) {
               this.wTapActive = false;
            }
         }
      }
   }

   @Listen
   public void onPacketSend(OnPacketSend event) {
      if (this.isEnabled() && !event.isCancelled() && event.getPacket() instanceof C02PacketUseEntity) {
         C02PacketUseEntity packet = (C02PacketUseEntity)event.getPacket();
         if (packet.func_149565_c() == Action.ATTACK
            && !this.wTapActive
            && this.attackCooldownTimer.hasElapsed(500L)
            && minecraft.field_71439_g.func_70051_ag()) {
            this.attackCooldownTimer.reset();
            this.chanceAccumulator = this.chanceAccumulator % 100 + this.chance.getValue();
            if (this.chanceAccumulator >= 100) {
               this.wTapActive = true;
               this.forwardSuppressionStarted = false;
               this.delayRemainingMillis = this.delayRemainingMillis + (long)(50.0F * this.delay.getValue());
               this.suppressionRemainingMillis = this.suppressionRemainingMillis + (long)(50.0F * this.duration.getValue());
            }
         }
      }
   }
}
