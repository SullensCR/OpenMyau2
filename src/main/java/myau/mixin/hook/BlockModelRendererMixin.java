package myau.mixin.hook;

import myau.core.ClientContext;
import myau.module.render.Xray;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BlockModelRenderer;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.util.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SideOnly(Side.CLIENT)
@Mixin({BlockModelRenderer.class})
public abstract class BlockModelRendererMixin {
   @Shadow
   public boolean func_178265_a(IBlockAccess blockAccess, IBakedModel model, Block block, BlockPos position, WorldRenderer renderer, boolean checkSides) {
      return false;
   }

   @Inject(
      method = {"renderModel(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/client/resources/model/IBakedModel;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/BlockPos;Lnet/minecraft/client/renderer/WorldRenderer;Z)Z"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void beforeRenderModel(
      IBlockAccess blockAccess,
      IBakedModel model,
      IBlockState blockState,
      BlockPos position,
      WorldRenderer renderer,
      boolean checkSides,
      CallbackInfoReturnable<Boolean> callbackInfo
   ) {
      if (ClientContext.moduleManager != null && ClientContext.moduleManager.modules.get(Xray.class).isEnabled()) {
         callbackInfo.setReturnValue(this.func_178265_a(blockAccess, model, blockState.func_177230_c(), position, renderer, checkSides));
      }
   }
}
