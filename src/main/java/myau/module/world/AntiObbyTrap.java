package myau.module.world;

import myau.module.ClientModule;
import myau.setting.BooleanOption;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class AntiObbyTrap extends ClientModule {
   private static final Minecraft minecraft = Minecraft.func_71410_x();
   public final BooleanOption setAir = new BooleanOption("set-air", true);

   public AntiObbyTrap() {
      super("AntiObbyTrap", false, 0);
   }

   public boolean shouldIgnoreBlock(World world, BlockPos position) {
      IBlockState state = world.func_180495_p(position);
      Block block = state.func_177230_c();
      boolean solid = block.func_149688_o().func_76220_a();
      if (solid && block.func_149730_j()) {
         Vec3 eyes = new Vec3(
            minecraft.field_71439_g.field_70165_t,
            minecraft.field_71439_g.field_70163_u + minecraft.field_71439_g.func_70047_e(),
            minecraft.field_71439_g.field_70161_v
         );
         return block.func_180640_a(minecraft.field_71441_e, position, state).func_72318_a(eyes);
      } else {
         return false;
      }
   }
}
