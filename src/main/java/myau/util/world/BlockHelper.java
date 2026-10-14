package myau.util.world;

import myau.rotation.RotationHelper;
import myau.util.math.RandomHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAnvil;
import net.minecraft.block.BlockBasePressurePlate;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockButton;
import net.minecraft.block.BlockCactus;
import net.minecraft.block.BlockCarpet;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockEndPortal;
import net.minecraft.block.BlockEndPortalFrame;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockJukebox;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.BlockLever;
import net.minecraft.block.BlockNote;
import net.minecraft.block.BlockPane;
import net.minecraft.block.BlockPumpkin;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.BlockRedstoneDiode;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockSlime;
import net.minecraft.block.BlockSnow;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockTNT;
import net.minecraft.block.BlockTorch;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.BlockTripWire;
import net.minecraft.block.BlockTripWireHook;
import net.minecraft.block.BlockVine;
import net.minecraft.block.BlockWall;
import net.minecraft.block.BlockWeb;
import net.minecraft.block.BlockWorkbench;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;

public class BlockHelper {
   private static final Minecraft minecraft = Minecraft.func_71410_x();

   private BlockHelper() {
   }

   public static boolean isReplaceableAt(BlockPos position) {
      return isReplaceable(minecraft.field_71441_e.func_180495_p(position).func_177230_c());
   }

   public static boolean isReplaceable(Block block) {
      return !block.func_149688_o().func_76222_j() ? false : !(block instanceof BlockSnow) || block.func_149669_A() <= 0.125;
   }

   public static boolean isInteractableAt(BlockPos position) {
      return isInteractable(minecraft.field_71441_e.func_180495_p(position).func_177230_c());
   }

   public static boolean isInteractable(Block block) {
      return block instanceof BlockContainer
         || block instanceof BlockWorkbench
         || block instanceof BlockAnvil
         || block instanceof BlockBed
         || block instanceof BlockDoor && block.func_149688_o() != Material.field_151573_f
         || block instanceof BlockTrapDoor
         || block instanceof BlockFenceGate
         || block instanceof BlockFence
         || block instanceof BlockButton
         || block instanceof BlockLever
         || block instanceof BlockJukebox
         || block instanceof BlockNote;
   }

   public static boolean isSuitableFullBlock(Block block) {
      return !(block instanceof BlockStairs)
         && !(block instanceof BlockSlab)
         && !(block instanceof BlockEndPortalFrame)
         && !(block instanceof BlockEndPortal)
         && !(block instanceof BlockVine)
         && !(block instanceof BlockPumpkin)
         && !(block instanceof BlockCactus)
         && !(block instanceof BlockBush)
         && !(block instanceof BlockFalling)
         && !(block instanceof BlockWeb)
         && !(block instanceof BlockPane)
         && !(block instanceof BlockCarpet)
         && !(block instanceof BlockSnow)
         && !(block instanceof BlockFence)
         && !(block instanceof BlockFenceGate)
         && !(block instanceof BlockWall)
         && !(block instanceof BlockLadder)
         && !(block instanceof BlockTorch)
         && !(block instanceof BlockRedstoneWire)
         && !(block instanceof BlockRedstoneDiode)
         && !(block instanceof BlockBasePressurePlate)
         && !(block instanceof BlockTripWire)
         && !(block instanceof BlockTripWireHook)
         && !(block instanceof BlockRailBase)
         && !(block instanceof BlockSlime)
         && !(block instanceof BlockTNT);
   }

   public static Vec3 placementHitVector(BlockPos position, EnumFacing face, float yaw, float pitch) {
      MovingObjectPosition rayHit = RotationHelper.traceBlocks(yaw, pitch, minecraft.field_71442_b.func_78757_d(), 1.0F);
      return rayHit != null && rayHit.field_72313_a == MovingObjectType.BLOCK && rayHit.func_178782_a().equals(position) && rayHit.field_178784_b == face
         ? rayHit.field_72307_f
         : randomFacePoint(position, face);
   }

   public static Vec3 randomFacePoint(BlockPos position, EnumFacing face) {
      Block block = minecraft.field_71441_e.func_180495_p(position).func_177230_c();
      Vec3 point = new Vec3(
         position.func_177958_n() + Math.min(Math.max(RandomHelper.randomDouble(0.0, 1.0), block.func_149704_x()), block.func_149753_y()),
         position.func_177956_o() + Math.min(Math.max(RandomHelper.randomDouble(0.0, 1.0), block.func_149665_z()), block.func_149669_A()),
         position.func_177952_p() + Math.min(Math.max(RandomHelper.randomDouble(0.0, 1.0), block.func_149706_B()), block.func_149693_C())
      );
      switch (face) {
         case DOWN:
            return new Vec3(point.field_72450_a, position.func_177956_o() + block.func_149665_z(), point.field_72449_c);
         case UP:
            return new Vec3(point.field_72450_a, position.func_177956_o() + block.func_149669_A(), point.field_72449_c);
         case NORTH:
            return new Vec3(point.field_72450_a, point.field_72448_b, position.func_177952_p() + block.func_149706_B());
         case EAST:
            return new Vec3(position.func_177958_n() + block.func_149753_y(), point.field_72448_b, point.field_72449_c);
         case SOUTH:
            return new Vec3(point.field_72450_a, point.field_72448_b, position.func_177952_p() + block.func_149693_C());
         case WEST:
            return new Vec3(position.func_177958_n() + block.func_149704_x(), point.field_72448_b, point.field_72449_c);
         default:
            return point;
      }
   }
}
