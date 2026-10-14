package myau.mixin.hook;

import myau.core.ClientContext;
import myau.event.input.OnBlockClick;
import myau.event.input.OnHotbarChange;
import myau.event.input.OnKeyPress;
import myau.event.input.OnLeftClick;
import myau.event.input.OnRightClick;
import myau.event.lifecycle.OnPostTick;
import myau.event.lifecycle.OnPreTick;
import myau.event.lifecycle.OnWorldLoad;
import myau.event.render.OnFramebufferResize;
import myau.module.combat.NoHitDelay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SideOnly(Side.CLIENT)
@Mixin({Minecraft.class})
public abstract class MinecraftMixin {
   @Shadow
   private int field_71429_W;
   @Shadow
   public PlayerControllerMP field_71442_b;
   @Shadow
   public WorldClient field_71441_e;
   @Shadow
   public EntityPlayerSP field_71439_g;
   @Shadow
   public GuiScreen field_71462_r;

   @Inject(
      method = {"runGameLoop"},
      at = {@At("RETURN")}
   )
   private void afterRunGameLoop(CallbackInfo callbackInfo) {
      if (ClientContext.moduleManager != null) {
         ClientContext.moduleManager.update();
      }
   }

   @Inject(
      method = {"runTick"},
      at = {@At("HEAD")}
   )
   private void beforeRunTick(CallbackInfo callbackInfo) {
      if (this.field_71441_e != null && this.field_71439_g != null && ClientContext.eventBus != null) {
         ClientContext.eventBus.post(new OnPreTick());
      }
   }

   @Inject(
      method = {"runTick"},
      at = {@At("RETURN")}
   )
   private void afterRunTick(CallbackInfo callbackInfo) {
      if (this.field_71441_e != null && this.field_71439_g != null && ClientContext.eventBus != null) {
         ClientContext.eventBus.post(new OnPostTick());
      }
   }

   @Inject(
      method = {"loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;Ljava/lang/String;)V"},
      at = {@At("HEAD")}
   )
   private void beforeLoadWorld(WorldClient world, String loadingMessage, CallbackInfo callbackInfo) {
      if (ClientContext.eventBus != null) {
         ClientContext.eventBus.post(new OnWorldLoad());
      }
   }

   @Inject(
      method = {"updateFramebufferSize"},
      at = {@At("RETURN")}
   )
   private void afterFramebufferResize(CallbackInfo callbackInfo) {
      if (ClientContext.eventBus != null) {
         ClientContext.eventBus.post(new OnFramebufferResize());
      }
   }

   @Inject(
      method = {"clickMouse"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void beforeLeftClick(CallbackInfo callbackInfo) {
      if (ClientContext.moduleManager != null && ClientContext.moduleManager.modules.get(NoHitDelay.class).isEnabled()) {
         this.field_71429_W = 0;
      }

      if (ClientContext.eventBus != null) {
         OnLeftClick event = new OnLeftClick();
         ClientContext.eventBus.post(event);
         if (event.isCancelled()) {
            callbackInfo.cancel();
         }
      }
   }

   @Inject(
      method = {"rightClickMouse"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void beforeRightClick(CallbackInfo callbackInfo) {
      if (ClientContext.eventBus != null) {
         OnRightClick rightClickEvent = new OnRightClick();
         ClientContext.eventBus.post(rightClickEvent);
         if (rightClickEvent.isCancelled()) {
            callbackInfo.cancel();
         }
      }
   }

   @Inject(
      method = {"sendClickBlockToController"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void beforeBlockClick(CallbackInfo callbackInfo) {
      if (ClientContext.eventBus != null) {
         OnBlockClick blockClickEvent = new OnBlockClick();
         ClientContext.eventBus.post(blockClickEvent);
         if (blockClickEvent.isCancelled()) {
            callbackInfo.cancel();
            this.field_71442_b.func_78767_c();
         }
      }
   }

   @Redirect(
      method = {"runTick"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/settings/KeyBinding;setKeyBindState(IZ)V"
      )
   )
   private void redirectKeyStateChange(int keyCode, boolean pressed) {
      KeyBinding.func_74510_a(keyCode, pressed);
      if (pressed && this.field_71462_r == null && ClientContext.eventBus != null) {
         ClientContext.eventBus.post(new OnKeyPress(keyCode));
      }
   }

   @Redirect(
      method = {"runTick"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/player/InventoryPlayer;changeCurrentItem(I)V"
      )
   )
   private void redirectHotbarScroll(InventoryPlayer inventoryPlayer, int scrollDirection) {
      if (ClientContext.eventBus != null) {
         OnHotbarChange hotbarChangeEvent = new OnHotbarChange(-1, scrollDirection);
         ClientContext.eventBus.post(hotbarChangeEvent);
         if (!hotbarChangeEvent.isCancelled()) {
            inventoryPlayer.func_70453_c(scrollDirection);
         }
      }
   }
}
