package myau.mixin.hook;

import myau.core.ClientContext;
import myau.module.render.Xray;
import net.minecraft.block.BlockPane;
import net.minecraft.util.EnumWorldBlockLayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SideOnly(Side.CLIENT)
@Mixin({BlockPane.class})
public abstract class BlockPaneMixin {
   @Inject(
      method = {"getBlockLayer"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onGetBlockLayer(CallbackInfoReturnable<EnumWorldBlockLayer> callbackInfo) {
      if (ClientContext.moduleManager != null) {
         Xray xray = (Xray)ClientContext.moduleManager.modules.get(Xray.class);
         if (xray.isEnabled()) {
            callbackInfo.setReturnValue(EnumWorldBlockLayer.TRANSLUCENT);
         }
      }
   }
}
