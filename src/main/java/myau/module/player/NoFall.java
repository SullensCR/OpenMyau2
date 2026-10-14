package myau.module.player;

import com.google.common.base.CaseFormat;
import myau.core.ClientContext;
import myau.event.Listen;
import myau.event.lifecycle.OnPreTick;
import myau.event.network.OnPacketReceive;
import myau.event.network.OnPacketSend;
import myau.mixin.accessor.C03PacketPlayerAccessor;
import myau.mixin.accessor.MinecraftAccessor;
import myau.module.ClientModule;
import myau.network.OutboundQueueOwner;
import myau.setting.ChoiceOption;
import myau.setting.FloatOption;
import myau.setting.IntegerOption;
import myau.util.Stopwatch;
import myau.util.chat.ChatHelper;
import myau.util.network.PacketHelper;
import myau.util.player.PlayerHelper;
import myau.util.world.ScoreboardHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.util.AxisAlignedBB;

public class NoFall extends ClientModule {
   private static final Minecraft minecraft = Minecraft.func_71410_x();
   private static final int MAX_DELAY_MILLIS = 10000;
   private static final long ARM_DELAY_MILLIS = 3000L;
   private final Stopwatch actionDelayTimer = new Stopwatch();
   private final Stopwatch armDelayTimer = new Stopwatch();
   private boolean packetModeActive = false;
   private boolean blinkReady = false;
   public final ChoiceOption mode = new ChoiceOption("mode", 0, new String[]{"PACKET", "BLINK", "NO_GROUND", "SPOOF"});
   public final FloatOption distance = new FloatOption("distance", 3.0F, 0.0F, 20.0F);
   public final IntegerOption delay = new IntegerOption("delay", 0, 0, 10000);

   public NoFall() {
      super("NoFall", false, 0);
   }

   private boolean isActionReady() {
      return this.armDelayTimer.hasElapsed(3000L) && this.actionDelayTimer.hasElapsed(this.delay.getValue().longValue());
   }

   public boolean isPacketModeActive() {
      return this.packetModeActive;
   }

   @Listen(
      priority = 50
   )
   public void onPacketSend(OnPacketSend event) {
      if (this.isEnabled() && !event.isCancelled() && event.getPacket() instanceof C03PacketPlayer) {
         C03PacketPlayer packet = (C03PacketPlayer)event.getPacket();
         switch (this.mode.getValue()) {
            case 0:
               this.handlePacketMode(packet);
               return;
            case 1:
               this.handleBlinkMode(packet);
               return;
            case 2:
               ((C03PacketPlayerAccessor)packet).setOnGround(false);
               return;
            case 3:
               this.handleSpoofMode(packet);
               return;
         }
      }
   }

   private void handlePacketMode(C03PacketPlayer packet) {
      if (this.packetModeActive) {
         this.packetModeActive = false;
         ((MinecraftAccessor)minecraft).getTimer().field_74278_d = 1.0F;
      } else if (!packet.func_149465_i()) {
         AxisAlignedBB bounds = minecraft.field_71439_g.func_174813_aQ().func_72314_b(2.0, 0.0, 2.0);
         if (PlayerHelper.wouldTakeFallDamage(this.distance.getValue()) && !PlayerHelper.isBoundsOverVoid(bounds) && this.isActionReady()) {
            this.actionDelayTimer.reset();
            this.packetModeActive = true;
            ((MinecraftAccessor)minecraft).getTimer().field_74278_d = 0.5F;
         }
      }
   }

   private void handleBlinkMode(C03PacketPlayer packet) {
      boolean playerSafe = !minecraft.field_71439_g.func_70617_f_()
         && !minecraft.field_71439_g.field_71075_bZ.field_75101_c
         && minecraft.field_71439_g.field_70737_aN == 0;
      if (ClientContext.outgoingPacketBuffer.owner() != OutboundQueueOwner.NO_FALL) {
         if (this.blinkReady
            && !packet.func_149465_i()
            && playerSafe
            && PlayerHelper.isAirBelow(this.distance.getValue().intValue())
            && minecraft.field_71439_g.field_70181_x < 0.0) {
            ClientContext.outgoingPacketBuffer.setEnabled(false, ClientContext.outgoingPacketBuffer.owner());
            ClientContext.outgoingPacketBuffer.setEnabled(true, OutboundQueueOwner.NO_FALL);
         }
      } else if (!playerSafe) {
         ClientContext.outgoingPacketBuffer.setEnabled(false, OutboundQueueOwner.NO_FALL);
         ChatHelper.addFormattedMessage(String.format("%s%s: &cFailed player check!&r", myau.util.chat.ChatPrefix.get(), this.getName()));
      } else if (PlayerHelper.isBoundsOverVoid(minecraft.field_71439_g.func_174813_aQ().func_72314_b(2.0, 0.0, 2.0))) {
         ClientContext.outgoingPacketBuffer.setEnabled(false, OutboundQueueOwner.NO_FALL);
         ChatHelper.addFormattedMessage(String.format("%s%s: &cFailed void check!&r", myau.util.chat.ChatPrefix.get(), this.getName()));
      } else if (packet.func_149465_i()) {
         for (Packet queuedPacket : ClientContext.outgoingPacketBuffer.queuedPackets) {
            if (queuedPacket instanceof C03PacketPlayer) {
               ((C03PacketPlayerAccessor)queuedPacket).setOnGround(true);
            }
         }

         ClientContext.outgoingPacketBuffer.setEnabled(false, OutboundQueueOwner.NO_FALL);
         this.actionDelayTimer.reset();
      }

      this.blinkReady = packet.func_149465_i() && playerSafe && this.isActionReady();
   }

   private void handleSpoofMode(C03PacketPlayer packet) {
      if (!packet.func_149465_i()) {
         AxisAlignedBB bounds = minecraft.field_71439_g.func_174813_aQ().func_72314_b(2.0, 0.0, 2.0);
         if (PlayerHelper.wouldTakeFallDamage(this.distance.getValue()) && !PlayerHelper.isBoundsOverVoid(bounds) && this.isActionReady()) {
            this.actionDelayTimer.reset();
            ((C03PacketPlayerAccessor)packet).setOnGround(true);
            minecraft.field_71439_g.field_70143_R = 0.0F;
         }
      }
   }

   @Listen(
      priority = 50
   )
   public void onPacketReceive(OnPacketReceive event) {
      if (this.isEnabled() && !event.isCancelled()) {
         if (event.getPacket() instanceof S08PacketPlayerPosLook) {
            this.onDisable();
         }
      }
   }

   @Listen(
      priority = 100
   )
   public void onPreTick(OnPreTick event) {
      if (this.isEnabled()) {
         if (ScoreboardHelper.hasLobbyPlayerCount()) {
            this.armDelayTimer.reset();
         }

         if (this.mode.getValue() == 0 && this.packetModeActive) {
            PacketHelper.sendWithoutSendEvent(new C03PacketPlayer(true));
            minecraft.field_71439_g.field_70143_R = 0.0F;
         }
      }
   }

   @Override
   public void onDisable() {
      this.blinkReady = false;
      ClientContext.outgoingPacketBuffer.setEnabled(false, OutboundQueueOwner.NO_FALL);
      if (this.packetModeActive) {
         this.packetModeActive = false;
         ((MinecraftAccessor)minecraft).getTimer().field_74278_d = 1.0F;
      }
   }

   @Override
   public void onSettingChanged(String settingName) {
      if (this.isEnabled()) {
         this.onDisable();
      }
   }

   @Override
   public String[] getHudInfo() {
      return new String[]{CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, this.mode.getModeName())};
   }
}
