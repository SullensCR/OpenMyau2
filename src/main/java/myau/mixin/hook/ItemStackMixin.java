package myau.mixin.hook;

import myau.core.ClientContext;
import myau.module.render.ESP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SideOnly(Side.CLIENT)
@Mixin({ItemStack.class})
public abstract class ItemStackMixin {
   @Inject(
      method = {"hasEffect"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onHasEffect(CallbackInfoReturnable<Boolean> callbackInfo) {
      if (ClientContext.moduleManager != null) {
         ESP eSP = (ESP)ClientContext.moduleManager.modules.get(ESP.class);
         if (eSP.isEnabled() && !eSP.isEnchantmentGlintAllowed()) {
            callbackInfo.setReturnValue(false);
         }
      }
   }
}
