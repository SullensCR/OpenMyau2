package myau.mixin.hook;

import myau.core.ClientContext;
import myau.module.render.Chams;
import myau.module.render.ViewClip;
import myau.module.render.Xray;
import net.minecraft.client.renderer.chunk.SetVisibility;
import net.minecraft.client.renderer.chunk.VisGraph;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SideOnly(Side.CLIENT)
@Mixin({VisGraph.class})
public abstract class VisGraphMixin {
   @Inject(
      method = {"func_178606_a"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void beforeMarkOpaqueCube(CallbackInfo callbackInfo) {
      if (ClientContext.moduleManager != null
         && (
            ClientContext.moduleManager.modules.get(Chams.class).isEnabled()
               || ClientContext.moduleManager.modules.get(ViewClip.class).isEnabled()
               || ClientContext.moduleManager.modules.get(Xray.class).isEnabled()
         )) {
         callbackInfo.cancel();
      }
   }

   @Inject(
      method = {"computeVisibility"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void beforeComputeVisibility(CallbackInfoReturnable<SetVisibility> callbackInfo) {
      if (ClientContext.moduleManager != null
         && (
            ClientContext.moduleManager.modules.get(Chams.class).isEnabled()
               || ClientContext.moduleManager.modules.get(ViewClip.class).isEnabled()
               || ClientContext.moduleManager.modules.get(Xray.class).isEnabled()
         )) {
         SetVisibility visibility = new SetVisibility();
         visibility.func_178618_a(true);
         callbackInfo.setReturnValue(visibility);
      }
   }
}
