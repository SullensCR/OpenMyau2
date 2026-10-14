package myau.network;

import net.minecraft.network.Packet;

public class HeldPacket {
   public final Packet<?> packet;
   public int ageTicks;

   public HeldPacket(Packet<?> packet) {
      this.packet = packet;
      this.ageTicks = 0;
   }
}
