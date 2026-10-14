package myau.mixin.hook;

import myau.core.ClientContext;
import myau.module.movement.Jesus;
import myau.module.world.AntiObbyTrap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@SideOnly(Side.CLIENT)
@Mixin({World.class})
public abstract class WorldMixin {
   @Redirect(
      method = {"handleMaterialAcceleration"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/Entity;isPushedByWater()Z"
      )
   )
   private boolean redirectHandleMaterialAcceleration(Entity entity) {
      if (entity instanceof EntityPlayerSP && ClientContext.moduleManager != null) {
         Jesus jesus = (Jesus)ClientContext.moduleManager.modules.get(Jesus.class);
         if (jesus.isEnabled() && jesus.noPush.getValue()) {
            return false;
         }
      }

      return entity.func_96092_aw();
   }

   @Redirect(
      method = {"rayTraceBlocks(Lnet/minecraft/util/Vec3;Lnet/minecraft/util/Vec3;ZZZ)Lnet/minecraft/util/MovingObjectPosition;"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/World;getBlockState(Lnet/minecraft/util/BlockPos;)Lnet/minecraft/block/state/IBlockState;"
      )
   )
   private IBlockState redirectRayTraceBlocks(World world, BlockPos position) {
      if (ClientContext.moduleManager == null) {
         return world.func_180495_p(position);
      } else {
         AntiObbyTrap antiObbyTrap = (AntiObbyTrap)ClientContext.moduleManager.modules.get(AntiObbyTrap.class);
         if (antiObbyTrap.isEnabled() && antiObbyTrap.shouldIgnoreBlock(world, position)) {
            if (antiObbyTrap.setAir.getValue()) {
               world.func_175698_g(position);
            }

            return Blocks.field_150350_a.func_176223_P();
         } else {
            return world.func_180495_p(position);
         }
      }
   }
}
