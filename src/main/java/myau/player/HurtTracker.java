package myau.player;

import myau.event.Listen;
import myau.event.lifecycle.OnPreTick;
import myau.event.network.OnPacketReceive;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.DataWatcher.WatchableObject;
import net.minecraft.network.play.server.S06PacketUpdateHealth;
import net.minecraft.network.play.server.S1CPacketEntityMetadata;

public class HurtTracker {
   private static final Minecraft minecraft = Minecraft.func_71410_x();
   private static final int HEALTH_METADATA_ID = 6;
   private boolean healthDecreasePending;
   private int remainingHurtTicks;

   public int remainingHurtTicks() {
      return this.remainingHurtTicks;
   }

   @Listen(
      priority = 100
   )
   public void onPreTick(OnPreTick event) {
      if (this.remainingHurtTicks > 0) {
         this.remainingHurtTicks--;
      }

      if (this.healthDecreasePending) {
         this.healthDecreasePending = false;
         this.remainingHurtTicks = minecraft.field_71439_g.field_70737_aN;
      }
   }

   @Listen
   public void onPacketReceive(OnPacketReceive event) {
      if (!event.isCancelled()) {
         if (event.getPacket() instanceof S06PacketUpdateHealth) {
            S06PacketUpdateHealth packet = (S06PacketUpdateHealth)event.getPacket();
            if (packet.func_149332_c() < minecraft.field_71439_g.func_110143_aJ()) {
               this.healthDecreasePending = true;
            }
         } else if (event.getPacket() instanceof S1CPacketEntityMetadata) {
            S1CPacketEntityMetadata packet = (S1CPacketEntityMetadata)event.getPacket();
            if (packet.func_149375_d() == minecraft.field_71439_g.func_145782_y()) {
               for (WatchableObject metadata : packet.func_149376_c()) {
                  if (metadata.func_75672_a() == 6 && (Float)metadata.func_75669_b() < minecraft.field_71439_g.func_110143_aJ()) {
                     this.healthDecreasePending = true;
                     break;
                  }
               }
            }
         }
      }
   }
}
