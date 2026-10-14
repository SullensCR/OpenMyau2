package myau.module.world;

import myau.event.Listen;
import myau.event.lifecycle.OnPostTick;
import myau.event.network.OnPacketSend;
import myau.mixin.accessor.PlayerControllerMPAccessor;
import myau.module.ClientModule;
import myau.setting.IntegerOption;
import myau.setting.PercentOption;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.C07PacketPlayerDigging;

public class SpeedMine extends ClientModule {
   private static final Minecraft minecraft = Minecraft.func_71410_x();
   private boolean startDiggingPending;
   private boolean stopDiggingPending;
   private int chanceAccumulator = 100;
   public final PercentOption speed = new PercentOption("speed", 15);
   public final IntegerOption delay = new IntegerOption("delay", 0, 0, 5);
   public final PercentOption chance = new PercentOption("chance", 50);

   public SpeedMine() {
      super("SpeedMine", false, 0);
   }

   @Listen
   public void onPostTick(OnPostTick event) {
      if (this.isEnabled()) {
         if (this.startDiggingPending) {
            this.startDiggingPending = false;
            if (((PlayerControllerMPAccessor)minecraft.field_71442_b).getIsHittingBlock() && this.chanceAccumulator >= 100) {
               float damage = ((PlayerControllerMPAccessor)minecraft.field_71442_b).getCurBlockDamageMP();
               float bonus = 0.3F * (this.speed.getValue().floatValue() / 100.0F);
               ((PlayerControllerMPAccessor)minecraft.field_71442_b).setCurBlockDamageMP(damage + bonus);
            }
         }

         if (this.stopDiggingPending) {
            this.stopDiggingPending = false;
            if (((PlayerControllerMPAccessor)minecraft.field_71442_b).getBlockHitDelay() == 5) {
               this.chanceAccumulator = this.chanceAccumulator % 100 + this.chance.getValue();
               if (this.chanceAccumulator >= 100) {
                  ((PlayerControllerMPAccessor)minecraft.field_71442_b).setBlockHitDelay(this.delay.getValue());
               }
            }
         }
      }
   }

   @Listen(
      priority = -100
   )
   public void onPacketSend(OnPacketSend event) {
      if (this.isEnabled()) {
         if (!event.isCancelled() && event.getPacket() instanceof C07PacketPlayerDigging) {
            C07PacketPlayerDigging packet = (C07PacketPlayerDigging)event.getPacket();
            switch (packet.func_180762_c()) {
               case START_DESTROY_BLOCK:
                  this.startDiggingPending = true;
                  break;
               case STOP_DESTROY_BLOCK:
                  this.stopDiggingPending = true;
            }
         }
      }
   }

   @Override
   public void onDisable() {
      this.startDiggingPending = false;
      this.stopDiggingPending = false;
   }

   @Override
   public String[] getHudInfo() {
      return new String[]{String.format("%d%%", this.speed.getValue()), String.format("%s", this.delay.getValue())};
   }
}
