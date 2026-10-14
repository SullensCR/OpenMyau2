package myau.module.combat;

import com.google.common.base.CaseFormat;
import myau.core.ClientContext;
import myau.event.Listen;
import myau.event.lifecycle.OnWorldLoad;
import myau.event.network.OnPacketReceive;
import myau.event.network.OnRawPacketReceive;
import myau.event.player.OnLivingUpdate;
import myau.event.player.OnPostPlayerUpdate;
import myau.event.player.OnVelocity;
import myau.mixin.accessor.EntityAccessor;
import myau.module.ClientModule;
import myau.module.movement.LongJump;
import myau.network.InboundQueueOwner;
import myau.setting.BooleanOption;
import myau.setting.ChoiceOption;
import myau.setting.IntegerOption;
import myau.setting.PercentOption;
import myau.util.chat.ChatFormat;
import myau.util.chat.ChatHelper;
import myau.util.movement.MovementHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.potion.Potion;

public class Velocity extends ClientModule {
   private static final Minecraft minecraft = Minecraft.func_71410_x();
   private int chanceAccumulator;
   private int delayChanceAccumulator;
   private boolean explosionPending;
   private boolean awaitingDamageStatus = true;
   private boolean jumpPending;
   private boolean bufferingVelocity;
   private boolean reversePending;
   public final ChoiceOption mode = new ChoiceOption("mode", 0, new String[]{"VANILLA", "JUMP", "DELAY", "REVERSE"});
   public final IntegerOption delayTicks = new IntegerOption("delay-ticks", 3, 1, 20, this::showDelaySettings);
   public final PercentOption delayChance = new PercentOption("delay-chance", 100, this::showDelaySettings);
   public final PercentOption chance = new PercentOption("chance", 100);
   public final PercentOption horizontal = new PercentOption("horizontal", 0);
   public final PercentOption vertical = new PercentOption("vertical", 100);
   public final PercentOption explosionsHorizontal = new PercentOption("explosions-horizontal", 100);
   public final PercentOption explosionsVertical = new PercentOption("explosions-vertical", 100);
   public final BooleanOption fakeCheck = new BooleanOption("fake-check", true);
   public final BooleanOption debugLog = new BooleanOption("debug-log", false);

   public Velocity() {
      super("Velocity", false, 0);
   }

   private boolean inFluidOrWeb() {
      boolean inWater = minecraft.field_71439_g.func_70090_H();
      return inWater || minecraft.field_71439_g.func_180799_ab() || ((EntityAccessor)minecraft.field_71439_g).getIsInWeb();
   }

   private boolean showDelaySettings() {
      int selection = this.mode.getValue();
      return selection == 2;
   }

   @Listen
   public void onVelocity(OnVelocity event) {
      if (!this.isEnabled() || event.isCancelled()) {
         this.explosionPending = false;
         this.awaitingDamageStatus = true;
      } else if (!this.awaitingDamageStatus || !this.fakeCheck.getValue()) {
         this.awaitingDamageStatus = true;
         if (this.explosionPending) {
            this.explosionPending = false;
            if (this.explosionsHorizontal.getValue() > 0) {
               event.setVelocityX(event.getVelocityX() * this.explosionsHorizontal.getValue().intValue() / 100.0);
               event.setVelocityZ(event.getVelocityZ() * this.explosionsHorizontal.getValue().intValue() / 100.0);
            } else {
               event.setVelocityX(minecraft.field_71439_g.field_70159_w);
               event.setVelocityZ(minecraft.field_71439_g.field_70179_y);
            }

            if (this.explosionsVertical.getValue() > 0) {
               event.setVelocityY(event.getVelocityY() * this.explosionsVertical.getValue().intValue() / 100.0);
            } else {
               event.setVelocityY(minecraft.field_71439_g.field_70181_x);
            }
         } else {
            this.chanceAccumulator = this.chanceAccumulator % 100 + this.chance.getValue();
            if (this.chanceAccumulator >= 100) {
               this.jumpPending = (this.mode.getValue() == 1 || this.mode.getValue() == 2) && event.getVelocityY() > 0.0;
               this.reversePending = this.mode.getValue() == 3;
               if (this.horizontal.getValue() > 0) {
                  event.setVelocityX(event.getVelocityX() * this.horizontal.getValue().intValue() / 100.0);
                  event.setVelocityZ(event.getVelocityZ() * this.horizontal.getValue().intValue() / 100.0);
               } else {
                  event.setVelocityX(minecraft.field_71439_g.field_70159_w);
                  event.setVelocityZ(minecraft.field_71439_g.field_70179_y);
               }

               if (this.vertical.getValue() > 0) {
                  event.setVelocityY(event.getVelocityY() * this.vertical.getValue().intValue() / 100.0);
               } else {
                  event.setVelocityY(minecraft.field_71439_g.field_70181_x);
               }
            }
         }
      }
   }

   @Listen
   public void onPostPlayerUpdate(OnPostPlayerUpdate event) {
      boolean applyReverse = this.bufferingVelocity;
      if (this.bufferingVelocity
         && (
            minecraft.field_71439_g.field_70122_E
               || this.inFluidOrWeb()
               || ClientContext.incomingPacketBuffer.bufferedTickCount() >= this.delayTicks.getValue().intValue()
         )) {
         ClientContext.incomingPacketBuffer.setEnabled(false, InboundQueueOwner.VELOCITY);
         this.bufferingVelocity = false;
      }

      applyReverse = this.reversePending;
      if (applyReverse) {
         MovementHelper.setHorizontalMotion(MovementHelper.horizontalSpeed(), MovementHelper.movementYaw());
         this.reversePending = false;
      }
   }

   @Listen
   public void onLivingUpdate(OnLivingUpdate event) {
      if (this.jumpPending) {
         this.jumpPending = false;
         if (minecraft.field_71439_g.field_70122_E && minecraft.field_71439_g.func_70051_ag() && !minecraft.field_71439_g.func_70644_a(Potion.field_76430_j)) {
            if (!this.inFluidOrWeb()) {
               minecraft.field_71439_g.field_71158_b.field_78901_c = true;
            }
         }
      }
   }

   @Listen
   public void onRawPacketReceive(OnRawPacketReceive event) {
      Packet packet = event.getPacket();
      if (packet instanceof S12PacketEntityVelocity) {
         packet = event.getPacket();
         S12PacketEntityVelocity velocity = (S12PacketEntityVelocity)packet;
         int entityId = velocity.func_149412_c();
         if (entityId == minecraft.field_71439_g.func_145782_y() && this.bufferingVelocity) {
            ClientContext.incomingPacketBuffer.setEnabled(false, InboundQueueOwner.VELOCITY);
            this.bufferingVelocity = false;
         }
      }
   }

   @Listen
   public void onPacketReceive(OnPacketReceive event) {
      boolean enabled = this.isEnabled();
      if (enabled && !event.isCancelled()) {
         if (event.getPacket() instanceof S12PacketEntityVelocity) {
            S12PacketEntityVelocity velocity = (S12PacketEntityVelocity)event.getPacket();
            if (velocity.func_149412_c() == minecraft.field_71439_g.func_145782_y()) {
               LongJump conflictingModule = (LongJump)ClientContext.moduleManager.modules.get(LongJump.class);
               if (this.mode.getValue() == 2
                  && !this.bufferingVelocity
                  && ClientContext.incomingPacketBuffer.owner() == InboundQueueOwner.NONE
                  && !minecraft.field_71439_g.field_70122_E
                  && !this.inFluidOrWeb()
                  && !this.explosionPending
                  && (!this.awaitingDamageStatus || !this.fakeCheck.getValue())
                  && (!conflictingModule.isEnabled() || !conflictingModule.isAwaitingLaunchVelocity())) {
                  this.delayChanceAccumulator = this.delayChanceAccumulator % 100 + this.delayChance.getValue();
                  if (this.delayChanceAccumulator >= 100) {
                     ClientContext.incomingPacketBuffer.setEnabled(true, InboundQueueOwner.VELOCITY);
                     ClientContext.incomingPacketBuffer.queuedPackets.offer(velocity);
                     event.setCancelled(true);
                     this.bufferingVelocity = true;
                     return;
                  }
               }

               if (this.debugLog.getValue()) {
                  ChatHelper.addFormattedMessage(
                     String.format(
                        "%sVelocity (&otick: %d, x: %.2f, y: %.2f, z: %.2f&r)&r",
                        myau.util.chat.ChatPrefix.get(),
                        minecraft.field_71439_g.field_70173_aa,
                        velocity.func_149411_d() / 8000.0,
                        velocity.func_149410_e() / 8000.0,
                        velocity.func_149409_f() / 8000.0
                     )
                  );
               }
            }
         } else if (event.getPacket() instanceof S27PacketExplosion) {
            S27PacketExplosion explosion = (S27PacketExplosion)event.getPacket();
            if (explosion.func_149149_c() != 0.0F || explosion.func_149144_d() != 0.0F || explosion.func_149147_e() != 0.0F) {
               this.explosionPending = true;
               if (this.explosionsHorizontal.getValue() == 0 || this.explosionsVertical.getValue() == 0) {
                  event.setCancelled(true);
               }

               if (this.debugLog.getValue()) {
                  ChatHelper.addFormattedMessage(
                     String.format(
                        "%sExplosion (&otick: %d, x: %.2f, y: %.2f, z: %.2f&r)&r",
                        myau.util.chat.ChatPrefix.get(),
                        minecraft.field_71439_g.field_70173_aa,
                        minecraft.field_71439_g.field_70159_w + explosion.func_149149_c(),
                        minecraft.field_71439_g.field_70181_x + explosion.func_149144_d(),
                        minecraft.field_71439_g.field_70179_y + explosion.func_149147_e()
                     )
                  );
               }
            }
         } else {
            if (event.getPacket() instanceof S19PacketEntityStatus) {
               S19PacketEntityStatus status = (S19PacketEntityStatus)event.getPacket();
               Entity entity = status.func_149161_a(minecraft.field_71441_e);
               if (entity == null || !entity.equals(minecraft.field_71439_g)) {
                  return;
               }

               if (status.func_149160_c() == 2) {
                  this.awaitingDamageStatus = false;
               }
            }
         }
      }
   }

   @Listen
   public void onWorldLoad(OnWorldLoad event) {
      this.onDisable();
   }

   @Override
   public void onDisable() {
      this.explosionPending = false;
      this.awaitingDamageStatus = true;
   }

   @Override
   public String[] getHudInfo() {
      int selection = this.mode.getValue();
      boolean jumpOrDelay = selection == 1 || this.mode.getValue() == 2;
      if (jumpOrDelay && this.horizontal.getValue() == 100 && this.vertical.getValue() == 100) {
         return new String[]{CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, this.mode.getModeName())};
      } else {
         int length = 2;
         String[] suffix = new String[length];
         suffix[0] = ChatFormat.formatText(String.format(this.mode.getValue() == 3 ? "&m%d%%&r" : "%d%%", this.horizontal.getValue()));
         suffix[1] = String.format("%d%%", this.vertical.getValue());
         return suffix;
      }
   }
}
