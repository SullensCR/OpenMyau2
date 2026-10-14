package myau.event.network;

import myau.event.GameEvent;
import net.minecraft.network.Packet;

public class OnRawPacketReceive extends GameEvent {
   private final Packet<?> packet;

   public OnRawPacketReceive(Packet<?> packet) {
      this.packet = packet;
   }

   public Packet<?> getPacket() {
      return this.packet;
   }
}
