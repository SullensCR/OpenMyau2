package myau.mixin.hook;

import myau.core.ClientContext;
import myau.module.render.Xray;
import net.minecraft.block.Block;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumWorldBlockLayer;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SideOnly(Side.CLIENT)
@Mixin({Block.class})
public abstract class BlockMixin {
   @Inject(
      method = {"shouldSideBeRendered"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onShouldSideBeRendered(IBlockAccess iBlockAccess, BlockPos position, EnumFacing facing, CallbackInfoReturnable<Boolean> callbackInfo) {
      if (ClientContext.moduleManager != null) {
         Xray xray = (Xray)ClientContext.moduleManager.modules.get(Xray.class);
         if (xray.isEnabled() && xray.mode.getValue() == 1 && xray.isSupportedBlock(Block.func_149682_b((Block)(Object)this))) {
            int neighborX = position.func_177958_n() - facing.func_176730_m().func_177958_n();
            int neighborY = position.func_177956_o() - facing.func_176730_m().func_177956_o();
            int neighborZ = position.func_177952_p() - facing.func_176730_m().func_177952_p();
            if (xray.passesCaveCheck(new BlockPos(neighborX, neighborY, neighborZ))) {
               callbackInfo.setReturnValue(true);
            }
         }
      }
   }

   @Inject(
      method = {"getBlockLayer"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onGetBlockLayer(CallbackInfoReturnable<EnumWorldBlockLayer> callbackInfo) {
      if (ClientContext.moduleManager != null) {
         Xray xray = (Xray)ClientContext.moduleManager.modules.get(Xray.class);
         if (xray.isEnabled()) {
            int blockId = Block.func_149682_b((Block)(Object)this);
            if (!xray.isSupportedBlock(blockId) || xray.mode.getValue() == 0 && !xray.isBlockEnabled(blockId)) {
               callbackInfo.setReturnValue(EnumWorldBlockLayer.TRANSLUCENT);
            }
         }
      }
   }
}
