package myau.module.world;

import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

class PlacementTarget {
   private final BlockPos position;
   private final EnumFacing face;

   public PlacementTarget(BlockPos position, EnumFacing face) {
      this.position = position;
      this.face = face;
   }

   public BlockPos position() {
      return this.position;
   }

   public EnumFacing face() {
      return this.face;
   }
}
