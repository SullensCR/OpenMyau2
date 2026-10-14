package myau.mixin.hook;

import myau.core.ClientContext;
import myau.event.input.OnStopUsingItem;
import myau.event.input.OnWindowClick;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SideOnly(Side.CLIENT)
@Mixin({PlayerControllerMP.class})
public abstract class PlayerControllerMPMixin {
   @Inject(
      method = {"windowClick"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onWindowClick(int windowId, int slotId, int mouseButton, int clickMode, EntityPlayer player, CallbackInfoReturnable<ItemStack> callbackInfo) {
      if (ClientContext.eventBus != null) {
         OnWindowClick event = new OnWindowClick(windowId, slotId, mouseButton, clickMode);
         ClientContext.eventBus.post(event);
         if (event.isCancelled()) {
            callbackInfo.cancel();
         }
      }
   }

   @Inject(
      method = {"onStoppedUsingItem"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void beforeStopUsingItem(CallbackInfo callbackInfo) {
      if (ClientContext.eventBus != null) {
         OnStopUsingItem stopUsingItemEvent = new OnStopUsingItem();
         ClientContext.eventBus.post(stopUsingItemEvent);
         if (stopUsingItemEvent.isCancelled()) {
            callbackInfo.cancel();
         }
      }
   }
}
