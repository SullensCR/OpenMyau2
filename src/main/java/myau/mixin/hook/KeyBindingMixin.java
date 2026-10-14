package myau.mixin.hook;

import myau.core.ClientContext;
import myau.event.input.OnHotbarChange;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SideOnly(Side.CLIENT)
@Mixin({KeyBinding.class})
public abstract class KeyBindingMixin {
   @Final
   @Shadow
   private String field_74515_c;

   @Inject(
      method = {"isPressed"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void onIsPressed(CallbackInfoReturnable<Boolean> callbackInfo) {
      if (callbackInfo.getReturnValue() && ClientContext.eventBus != null) {
         Minecraft minecraft = Minecraft.func_71410_x();

         for (int hotbarSlot = 0; hotbarSlot < 9; hotbarSlot++) {
            if (minecraft.field_71474_y.field_151456_ac[hotbarSlot].func_151464_g().equals(this.field_74515_c)) {
               OnHotbarChange event = new OnHotbarChange(hotbarSlot, 0);
               ClientContext.eventBus.post(event);
               if (event.isCancelled()) {
                  callbackInfo.setReturnValue(false);
               }
            }
         }
      }
   }
}
