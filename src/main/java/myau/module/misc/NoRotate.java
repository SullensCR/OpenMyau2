package myau.module.misc;

import myau.event.Listen;
import myau.event.lifecycle.OnWorldLoad;
import myau.event.network.OnPacketReceive;
import myau.module.ClientModule;
import myau.rotation.RotationHelper;
import myau.util.math.RandomHelper;
import myau.util.network.PacketHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.util.MathHelper;

public class NoRotate extends ClientModule {
   private static final Minecraft minecraft = Minecraft.func_71410_x();
   private boolean skipNextCorrection;

   public NoRotate() {
      super("NoRotate", false, 0);
   }

   public boolean handleServerPositionAndRotation(EntityPlayer player, double x, double y, double z, float yaw, float pitch) {
      if (this.skipNextCorrection) {
         this.skipNextCorrection = false;
         return false;
      } else if (minecraft.field_71439_g.field_70177_z == -180.0F && minecraft.field_71439_g.field_70125_A == 0.0F) {
         return false;
      } else {
         float sensitivity = 0.2F + minecraft.field_71474_y.field_74341_c * 0.6F;
         float mouseStep = sensitivity * sensitivity * sensitivity * 8.0F * 0.15F;
         float yawJitter = RandomHelper.randomBoolean() ? mouseStep : -mouseStep;
         float localYaw = minecraft.field_71439_g.field_70177_z + yawJitter;
         float serverYaw = yaw % 360.0F;
         float restoredYaw = RotationHelper.snapToMouseSensitivity(localYaw, serverYaw);
         float pitchJitter = RandomHelper.randomBoolean() ? mouseStep : -mouseStep;
         float localPitch = minecraft.field_71439_g.field_70125_A + pitchJitter;
         float serverPitch = pitch % 360.0F;
         float restoredPitch = MathHelper.func_76131_a(RotationHelper.snapToMouseSensitivity(localPitch, serverPitch), -90.0F, 90.0F);
         player.func_70080_a(x, y, z, yaw, pitch);
         PacketHelper.send(
            new C06PacketPlayerPosLook(
               minecraft.field_71439_g.field_70165_t,
               minecraft.field_71439_g.func_174813_aQ().field_72338_b,
               minecraft.field_71439_g.field_70161_v,
               serverYaw,
               serverPitch,
               false
            )
         );
         minecraft.field_71439_g.field_70177_z = restoredYaw;
         minecraft.field_71439_g.field_70125_A = restoredPitch;
         return true;
      }
   }

   @Listen
   public void onPacketReceive(OnPacketReceive event) {
      if (this.isEnabled() && !event.isCancelled() && event.getPacket() instanceof S02PacketChat) {
         String message = ((S02PacketChat)event.getPacket()).func_148915_c().func_150254_d();
         if (message.contains("§e§lProtect your bed and destroy the enemy beds.") || message.contains("§eYou will respawn in §r§c1 §r§esecond!")) {
            this.skipNextCorrection = true;
         }
      }
   }

   @Listen
   public void onWorldLoad(OnWorldLoad event) {
      this.skipNextCorrection = false;
   }

   @Override
   public void onDisable() {
      this.skipNextCorrection = false;
   }
}
