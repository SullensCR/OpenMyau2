package myau.network;

import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import myau.core.ClientContext;
import myau.event.Listen;
import myau.event.lifecycle.OnPostTick;
import myau.event.network.OnPacketReceive;
import myau.event.network.OnPacketSend;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.DataWatcher.WatchableObject;
import net.minecraft.network.Packet;
import net.minecraft.network.ThreadQuickExitException;
import net.minecraft.network.handshake.client.C00Handshake;
import net.minecraft.network.login.client.C00PacketLoginStart;
import net.minecraft.network.login.client.C01PacketEncryptionResponse;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.server.S00PacketKeepAlive;
import net.minecraft.network.play.server.S01PacketJoinGame;
import net.minecraft.network.play.server.S06PacketUpdateHealth;
import net.minecraft.network.play.server.S07PacketRespawn;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraft.network.play.server.S1CPacketEntityMetadata;
import net.minecraft.network.status.client.C00PacketServerQuery;
import net.minecraft.network.status.client.C01PacketPing;

public class InboundPacketQueue {
   private static final Minecraft minecraft = Minecraft.func_71410_x();
   private static final int HEALTH_METADATA_ID = 6;
   private InboundQueueOwner bufferOwner = InboundQueueOwner.NONE;
   private long bufferedTicks;
   public final Deque<Packet<INetHandlerPlayClient>> queuedPackets = new ConcurrentLinkedDeque<>();

   public boolean buffer(Packet<INetHandlerPlayClient> packet) {
      if (this.bufferOwner == InboundQueueOwner.NONE) {
         return false;
      } else if (packet instanceof S01PacketJoinGame || packet instanceof S07PacketRespawn) {
         this.setEnabled(false, this.bufferOwner);
         return false;
      } else if (!(packet instanceof S00PacketKeepAlive) && !(packet instanceof S06PacketUpdateHealth)) {
         if (packet instanceof S19PacketEntityStatus) {
            byte opcode = ((S19PacketEntityStatus)packet).func_149160_c();
            if (opcode == 2 || opcode == 3) {
               return false;
            }
         }

         if (packet instanceof S1CPacketEntityMetadata) {
            List<WatchableObject> metadata = ((S1CPacketEntityMetadata)packet).func_149376_c();
            if (metadata != null) {
               for (WatchableObject entry : metadata) {
                  if (entry.func_75672_a() == 6) {
                     return false;
                  }
               }
            }
         }

         this.queuedPackets.offer(packet);
         return true;
      } else {
         return false;
      }
   }

   public boolean setEnabled(boolean enabled, InboundQueueOwner owner) {
      if (owner != InboundQueueOwner.NONE && (this.bufferOwner == InboundQueueOwner.NONE || this.bufferOwner == owner)) {
         if (enabled) {
            this.bufferOwner = owner;
            return true;
         } else {
            this.bufferOwner = InboundQueueOwner.NONE;
            this.bufferedTicks = 0L;
            if (minecraft.func_147114_u() == null) {
               this.queuedPackets.clear();
               return true;
            } else {
               while (!this.queuedPackets.isEmpty() && this.bufferOwner == InboundQueueOwner.NONE) {
                  Packet<INetHandlerPlayClient> packet = this.queuedPackets.poll();
                  OnPacketReceive event = new OnPacketReceive(packet);
                  ClientContext.eventBus.post(event);
                  if (!event.isCancelled()) {
                     try {
                        packet.func_148833_a(minecraft.func_147114_u());
                     } catch (ThreadQuickExitException var6) {
                     }
                  }
               }

               return true;
            }
         }
      } else {
         return false;
      }
   }

   public InboundQueueOwner owner() {
      return this.bufferOwner;
   }

   public void setOwner(InboundQueueOwner owner) {
      this.bufferOwner = owner;
   }

   public long bufferedTickCount() {
      return this.bufferedTicks;
   }

   @Listen
   public void onPacketSend(OnPacketSend event) {
      if (event.getPacket() instanceof C00Handshake
         || event.getPacket() instanceof C00PacketLoginStart
         || event.getPacket() instanceof C00PacketServerQuery
         || event.getPacket() instanceof C01PacketPing
         || event.getPacket() instanceof C01PacketEncryptionResponse) {
         this.setEnabled(false, this.bufferOwner);
      }
   }

   @Listen
   public void onPostTick(OnPostTick event) {
      if (minecraft.field_71439_g.field_70128_L) {
         this.setEnabled(false, this.bufferOwner);
      }

      if (this.bufferOwner != InboundQueueOwner.NONE) {
         this.bufferedTicks++;
      }
   }
}
