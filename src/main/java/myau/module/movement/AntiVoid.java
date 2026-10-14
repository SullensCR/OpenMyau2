package myau.module.movement;

import com.google.common.base.CaseFormat;
import myau.core.ClientContext;
import myau.event.Listen;
import myau.event.input.OnKeyPress;
import myau.event.player.OnPreWalkingUpdate;
import myau.module.ClientModule;
import myau.network.OutboundQueueOwner;
import myau.setting.ChoiceOption;
import myau.setting.FloatOption;
import myau.util.math.RandomHelper;
import myau.util.player.PlayerHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemEnderPearl;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition;
import net.minecraft.util.AxisAlignedBB;

public class AntiVoid extends ClientModule {
   private static final Minecraft minecraft = Minecraft.func_71410_x();
   private boolean overVoid = false;
   private boolean previouslyOverVoid = false;
   private double[] lastSafePosition = null;
   public final ChoiceOption mode = new ChoiceOption("mode", 0, new String[]{"BLINK"});
   public final FloatOption distance = new FloatOption("distance", 5.0F, 0.0F, 16.0F);

   public AntiVoid() {
      super("AntiVoid", false, 0);
   }

   private void releaseBufferAndClearSafePosition() {
      ClientContext.outgoingPacketBuffer.setEnabled(false, OutboundQueueOwner.ANTI_VOID);
      this.lastSafePosition = null;
   }

   private boolean canStartBuffering() {
      LongJump longJump = (LongJump)ClientContext.moduleManager.modules.get(LongJump.class);
      return !longJump.hasJumpStarted();
   }

   @Listen
   public void onPreWalkingUpdate(OnPreWalkingUpdate event) {
      if (this.isEnabled()) {
         this.overVoid = !minecraft.field_71439_g.field_71075_bZ.field_75101_c && PlayerHelper.isOverVoid();
         if (this.mode.getValue() == 0) {
            if (!this.overVoid) {
               this.releaseBufferAndClearSafePosition();
            }

            if (this.lastSafePosition != null) {
               float halfWidth = minecraft.field_71439_g.field_70130_N / 2.0F;
               float playerHeight = minecraft.field_71439_g.field_70131_O;
               AxisAlignedBB savedBounds = new AxisAlignedBB(
                  this.lastSafePosition[0] - halfWidth,
                  this.lastSafePosition[1],
                  this.lastSafePosition[2] - halfWidth,
                  this.lastSafePosition[0] + halfWidth,
                  this.lastSafePosition[1] + playerHeight,
                  this.lastSafePosition[2] + halfWidth
               );
               if (PlayerHelper.isBoundsOverVoid(savedBounds)) {
                  this.releaseBufferAndClearSafePosition();
               }
            }

            if (!this.previouslyOverVoid && this.overVoid && this.canStartBuffering()) {
               ClientContext.outgoingPacketBuffer.setEnabled(false, OutboundQueueOwner.AUTO_BLOCK);
               if (ClientContext.outgoingPacketBuffer.setEnabled(true, OutboundQueueOwner.ANTI_VOID)) {
                  this.lastSafePosition = new double[]{
                     minecraft.field_71439_g.field_70169_q, minecraft.field_71439_g.field_70167_r, minecraft.field_71439_g.field_70166_s
                  };
               }
            }

            if (ClientContext.outgoingPacketBuffer.owner() == OutboundQueueOwner.ANTI_VOID
               && this.lastSafePosition != null
               && this.lastSafePosition[1] - this.distance.getValue().floatValue() > minecraft.field_71439_g.field_70163_u) {
               ClientContext.outgoingPacketBuffer
                  .queuedPackets
                  .offerFirst(
                     new C04PacketPlayerPosition(
                        this.lastSafePosition[0], this.lastSafePosition[1] - RandomHelper.randomDouble(10.0, 20.0), this.lastSafePosition[2], false
                     )
                  );
               this.releaseBufferAndClearSafePosition();
            }
         }

         this.previouslyOverVoid = this.overVoid;
      }
   }

   @Listen
   public void onKeyPress(OnKeyPress event) {
      if (event.getKeyCode() == minecraft.field_71474_y.field_74313_G.func_151463_i()) {
         ItemStack selectedStack = minecraft.field_71439_g.field_71071_by.func_70448_g();
         if (selectedStack != null && selectedStack.func_77973_b() instanceof ItemEnderPearl) {
            this.releaseBufferAndClearSafePosition();
         }
      }
   }

   @Override
   public void onEnable() {
      this.overVoid = false;
      this.previouslyOverVoid = false;
      this.releaseBufferAndClearSafePosition();
   }

   @Override
   public void onDisable() {
      ClientContext.outgoingPacketBuffer.setEnabled(false, OutboundQueueOwner.ANTI_VOID);
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
