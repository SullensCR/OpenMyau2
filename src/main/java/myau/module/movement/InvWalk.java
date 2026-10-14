package myau.module.movement;

import com.google.common.base.CaseFormat;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import myau.core.ClientContext;
import myau.event.Listen;
import myau.event.lifecycle.OnPreTick;
import myau.event.network.OnPacketSend;
import myau.event.player.OnRotationUpdate;
import myau.mixin.accessor.C0DPacketCloseWindowAccessor;
import myau.module.ClientModule;
import myau.setting.ChoiceOption;
import myau.util.input.InputHelper;
import myau.util.network.PacketHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.client.C0EPacketClickWindow;
import net.minecraft.network.play.client.C16PacketClientStatus;
import net.minecraft.network.play.client.C16PacketClientStatus.EnumState;

public class InvWalk extends ClientModule {
   private static final Minecraft minecraft = Minecraft.func_71410_x();
   private final Queue<C0EPacketClickWindow> queuedWindowClicks = new ConcurrentLinkedQueue<>();
   private boolean movementKeysUpdated = false;
   private C16PacketClientStatus pendingInventoryOpenPacket = null;
   private int clickReleaseDelayTicks = 0;
   public final ChoiceOption mode = new ChoiceOption("mode", 1, new String[]{"VANILLA", "LEGIT", "HYPIXEL"});

   public InvWalk() {
      super("InvWalk", false, 0);
   }

   public void updateMovementKeys() {
      KeyBinding[] movementKeys = new KeyBinding[]{
         minecraft.field_71474_y.field_74351_w,
         minecraft.field_71474_y.field_74370_x,
         minecraft.field_71474_y.field_74368_y,
         minecraft.field_71474_y.field_74366_z,
         minecraft.field_71474_y.field_74314_A,
         minecraft.field_71474_y.field_151444_V
      };

      for (KeyBinding binding : movementKeys) {
         InputHelper.updateKeyState(binding.func_151463_i());
      }

      if (ClientContext.moduleManager.modules.get(Sprint.class).isEnabled()) {
         InputHelper.setKeyState(minecraft.field_71474_y.field_151444_V.func_151463_i(), true);
      }

      this.movementKeysUpdated = true;
   }

   public boolean canUpdateMovementKeys() {
      if (minecraft.field_71462_r instanceof GuiContainer && !(minecraft.field_71462_r instanceof GuiContainerCreative)) {
         switch (this.mode.getValue()) {
            case 1:
               return minecraft.field_71462_r instanceof GuiInventory && this.pendingInventoryOpenPacket != null && this.queuedWindowClicks.isEmpty();
            case 2:
               return minecraft.field_71462_r instanceof GuiInventory && this.queuedWindowClicks.isEmpty();
            default:
               return true;
         }
      } else {
         return false;
      }
   }

   @Listen(
      priority = -100
   )
   public void onPreTick(OnPreTick event) {
      C0EPacketClickWindow queuedClick;
      while ((queuedClick = this.queuedWindowClicks.poll()) != null) {
         PacketHelper.sendWithoutSendEvent(queuedClick);
      }
   }

   @Listen(
      priority = -100
   )
   public void onRotationUpdate(OnRotationUpdate event) {
      if (this.isEnabled()) {
         if (this.canUpdateMovementKeys() && this.clickReleaseDelayTicks == 0) {
            this.updateMovementKeys();
         } else {
            if (this.movementKeysUpdated) {
               if (minecraft.field_71462_r != null) {
                  KeyBinding.func_74506_a();
               }

               this.movementKeysUpdated = false;
            }

            if (this.pendingInventoryOpenPacket != null) {
               PacketHelper.sendWithoutSendEvent(this.pendingInventoryOpenPacket);
               this.pendingInventoryOpenPacket = null;
            }

            if (this.clickReleaseDelayTicks > 0) {
               this.clickReleaseDelayTicks--;
            }
         }
      }
   }

   @Listen
   public void onPacketSend(OnPacketSend event) {
      if (this.isEnabled()) {
         if (event.getPacket() instanceof C16PacketClientStatus) {
            if (this.mode.getValue() == 1) {
               C16PacketClientStatus statusPacket = (C16PacketClientStatus)event.getPacket();
               if (statusPacket.func_149435_c() == EnumState.OPEN_INVENTORY_ACHIEVEMENT) {
                  event.setCancelled(true);
                  this.pendingInventoryOpenPacket = statusPacket;
               }
            }
         } else if (!(event.getPacket() instanceof C0EPacketClickWindow)) {
            if (event.getPacket() instanceof C0DPacketCloseWindow) {
               C0DPacketCloseWindow closePacket = (C0DPacketCloseWindow)event.getPacket();
               if (this.pendingInventoryOpenPacket != null && ((C0DPacketCloseWindowAccessor)closePacket).getWindowId() == 0) {
                  this.pendingInventoryOpenPacket = null;
                  event.setCancelled(true);
               }
            }
         } else {
            C0EPacketClickWindow clickPacket = (C0EPacketClickWindow)event.getPacket();
            switch (this.mode.getValue()) {
               case 1:
                  if (clickPacket.func_149548_c() == 0) {
                     if ((clickPacket.func_149542_h() == 3 || clickPacket.func_149542_h() == 4) && clickPacket.func_149544_d() == -999) {
                        event.setCancelled(true);
                        return;
                     }

                     if (this.pendingInventoryOpenPacket != null) {
                        KeyBinding.func_74506_a();
                        event.setCancelled(true);
                        this.queuedWindowClicks.offer(clickPacket);
                     }
                  }
                  break;
               case 2:
                  if (clickPacket.func_149548_c() == 0) {
                     if ((clickPacket.func_149542_h() == 3 || clickPacket.func_149542_h() == 4) && clickPacket.func_149544_d() == -999) {
                        event.setCancelled(true);
                     } else {
                        KeyBinding.func_74506_a();
                        event.setCancelled(true);
                        this.queuedWindowClicks.offer(clickPacket);
                        this.clickReleaseDelayTicks = 8;
                     }
                  }
            }

            if (this.pendingInventoryOpenPacket != null) {
               PacketHelper.sendWithoutSendEvent(this.pendingInventoryOpenPacket);
               this.pendingInventoryOpenPacket = null;
            }
         }
      }
   }

   @Override
   public void onDisable() {
      if (this.movementKeysUpdated) {
         if (minecraft.field_71462_r != null) {
            KeyBinding.func_74506_a();
         }

         this.movementKeysUpdated = false;
      }

      if (this.pendingInventoryOpenPacket != null) {
         PacketHelper.sendWithoutSendEvent(this.pendingInventoryOpenPacket);
         this.pendingInventoryOpenPacket = null;
      }

      this.clickReleaseDelayTicks = 0;
   }

   @Override
   public String[] getHudInfo() {
      return new String[]{CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, this.mode.getModeName())};
   }
}
