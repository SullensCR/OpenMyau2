package myau.event.network;

import myau.event.CancelableGameEvent;
import net.minecraft.network.Packet;

public class OnPacketSend extends CancelableGameEvent {
   private final Packet<?> packet;

   public OnPacketSend(Packet<?> packet) {
      this.packet = packet;
   }

   public Packet<?> getPacket() {
      return this.packet;
   }
}
