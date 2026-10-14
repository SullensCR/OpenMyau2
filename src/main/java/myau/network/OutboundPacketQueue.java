package myau.network;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import myau.event.Listen;
import myau.event.lifecycle.OnPostTick;
import myau.event.network.OnPacketSend;
import myau.util.network.PacketHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Packet;
import net.minecraft.network.handshake.client.C00Handshake;
import net.minecraft.network.login.client.C00PacketLoginStart;
import net.minecraft.network.login.client.C01PacketEncryptionResponse;
import net.minecraft.network.play.client.C00PacketKeepAlive;
import net.minecraft.network.play.client.C01PacketChatMessage;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C0FPacketConfirmTransaction;
import net.minecraft.network.status.client.C00PacketServerQuery;
import net.minecraft.network.status.client.C01PacketPing;

public class OutboundPacketQueue {
   private static final Minecraft minecraft = Minecraft.func_71410_x();
   private OutboundQueueOwner bufferOwner = OutboundQueueOwner.NONE;
   private boolean flushing;
   public final Deque<Packet<?>> queuedPackets = new ConcurrentLinkedDeque<>();

   public boolean buffer(Packet<?> packet) {
      if (this.bufferOwner == OutboundQueueOwner.NONE) {
         return false;
      } else if (packet instanceof C00PacketKeepAlive || packet instanceof C01PacketChatMessage) {
         return false;
      } else if (this.queuedPackets.isEmpty() && packet instanceof C0FPacketConfirmTransaction) {
         return false;
      } else {
         this.queuedPackets.offer(packet);
         return true;
      }
   }

   public boolean setEnabled(boolean enabled, OutboundQueueOwner owner) {
      if (owner != OutboundQueueOwner.NONE && (this.bufferOwner == OutboundQueueOwner.NONE || this.bufferOwner == owner)) {
         if (enabled) {
            this.bufferOwner = owner;
            return true;
         } else {
            this.bufferOwner = OutboundQueueOwner.NONE;
            if (minecraft.func_147114_u() == null) {
               this.queuedPackets.clear();
               return true;
            } else {
               this.flushing = true;

               while (!this.queuedPackets.isEmpty()) {
                  PacketHelper.sendWithoutSendEvent(this.queuedPackets.poll());
               }

               this.flushing = false;
               return true;
            }
         }
      } else {
         return false;
      }
   }

   public OutboundQueueOwner owner() {
      return this.bufferOwner;
   }

   public long movementPacketCount() {
      return this.queuedPackets.stream().filter(OutboundPacketQueue::isMovementPacket).count();
   }

   public boolean isFlushing() {
      return this.flushing;
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
   }

   private static boolean isMovementPacket(Packet<?> packet) {
      return packet instanceof C03PacketPlayer;
   }
}
