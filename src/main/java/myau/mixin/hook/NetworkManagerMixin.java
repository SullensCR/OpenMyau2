package myau.mixin.hook;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.GenericFutureListener;
import java.util.concurrent.Future;
import myau.core.ClientContext;
import myau.event.network.OnPacketReceive;
import myau.event.network.OnPacketSend;
import myau.event.network.OnRawPacketReceive;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SideOnly(Side.CLIENT)
@Mixin({NetworkManager.class})
public abstract class NetworkManagerMixin {
   @Inject(
      method = {"channelRead0*"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onChannelRead(ChannelHandlerContext channelHandlerContext, Packet<?> packet, CallbackInfo callbackInfo) {
      if (!packet.getClass().getName().startsWith("net.minecraft.network.play.client")
         && ClientContext.eventBus != null
         && ClientContext.incomingPacketBuffer != null) {
         ClientContext.eventBus.post(new OnRawPacketReceive(packet));
         if (ClientContext.incomingPacketBuffer.buffer((Packet<INetHandlerPlayClient>)packet)) {
            callbackInfo.cancel();
         } else {
            OnPacketReceive event = new OnPacketReceive(packet);
            ClientContext.eventBus.post(event);
            if (event.isCancelled()) {
               callbackInfo.cancel();
            }
         }
      }
   }

   @Inject(
      method = {"sendPacket(Lnet/minecraft/network/Packet;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onSendPacket(Packet<?> packet, CallbackInfo callbackInfo) {
      if (!packet.getClass().getName().startsWith("net.minecraft.network.play.server")
         && ClientContext.eventBus != null
         && ClientContext.packetActionTracker != null
         && ClientContext.outgoingPacketBuffer != null
         && ClientContext.packetDelayManager != null) {
         OnPacketSend packetSendEvent = new OnPacketSend(packet);
         ClientContext.eventBus.post(packetSendEvent);
         if (packetSendEvent.isCancelled()) {
            callbackInfo.cancel();
         } else if (!ClientContext.packetDelayManager.isFlushing()) {
            if (!ClientContext.outgoingPacketBuffer.isFlushing()) {
               ClientContext.packetActionTracker.record(packet);
               if (ClientContext.outgoingPacketBuffer.buffer(packet)) {
                  callbackInfo.cancel();
                  return;
               }
            }

            if (ClientContext.packetDelayManager.buffer(packet)) {
               callbackInfo.cancel();
            }
         }
      }
   }

   @Inject(
      method = {"sendPacket(Lnet/minecraft/network/Packet;Lio/netty/util/concurrent/GenericFutureListener;[Lio/netty/util/concurrent/GenericFutureListener;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onSendPacketWithListeners(
      Packet<?> packet,
      GenericFutureListener<? extends Future<? super Void>> listener,
      GenericFutureListener<? extends Future<? super Void>>[] additionalListeners,
      CallbackInfo callbackInfo
   ) {
      if (!packet.getClass().getName().startsWith("net.minecraft.network.play.server")
         && ClientContext.packetActionTracker != null
         && ClientContext.outgoingPacketBuffer != null
         && ClientContext.packetDelayManager != null
         && !ClientContext.packetDelayManager.isFlushing()) {
         if (!ClientContext.outgoingPacketBuffer.isFlushing()) {
            ClientContext.packetActionTracker.record(packet);
            if (ClientContext.outgoingPacketBuffer.buffer(packet)) {
               callbackInfo.cancel();
               return;
            }
         }

         if (ClientContext.packetDelayManager.buffer(packet)) {
            callbackInfo.cancel();
         }
      }
   }
}
