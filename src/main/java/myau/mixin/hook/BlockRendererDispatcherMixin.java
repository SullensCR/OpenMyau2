package myau.mixin.hook;

import myau.core.ClientContext;
import myau.module.render.BedESP;
import myau.module.render.Xray;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockBed.EnumPartType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.util.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SideOnly(Side.CLIENT)
@Mixin({BlockRendererDispatcher.class})
public abstract class BlockRendererDispatcherMixin {
   @Inject(
      method = {"renderBlock"},
      at = {@At("HEAD")}
   )
   private void onRenderBlock(
      IBlockState blockState, BlockPos position, IBlockAccess iBlockAccess, WorldRenderer worldRenderer, CallbackInfoReturnable<Boolean> callbackInfo
   ) {
      if (ClientContext.moduleManager != null) {
         BedESP bedESP = (BedESP)ClientContext.moduleManager.modules.get(BedESP.class);
         if (bedESP.isEnabled() && blockState.func_177230_c() instanceof BlockBed && blockState.func_177229_b(BlockBed.field_176472_a) == EnumPartType.HEAD) {
            bedESP.bedHeads.add(new BlockPos(position));
         }

         Xray xray = (Xray)ClientContext.moduleManager.modules.get(Xray.class);
         if (xray.isEnabled() && xray.isBlockEnabled(Block.func_149682_b(blockState.func_177230_c()))) {
            if (xray.passesCaveCheck(position)) {
               xray.discoveredBlocks.add(new BlockPos(position));
            } else {
               xray.discoveredBlocks.remove(position);
            }
         }
      }
   }
}
