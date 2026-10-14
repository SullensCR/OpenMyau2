package myau.util.network;

import io.netty.util.concurrent.GenericFutureListener;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Packet;

public final class PacketHelper {
   private static final Minecraft minecraft = Minecraft.func_71410_x();

   private PacketHelper() {
   }

   public static void send(Packet<?> packet) {
      minecraft.func_147114_u().func_147298_b().func_179290_a(packet);
   }

   public static void sendWithoutSendEvent(Packet<?> packet) {
      minecraft.func_147114_u().func_147298_b().func_179288_a(packet, null, new GenericFutureListener[0]);
   }
}
