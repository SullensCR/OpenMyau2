package myau.module.render;

import java.awt.Color;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.concurrent.CopyOnWriteArraySet;
import myau.event.Listen;
import myau.event.lifecycle.OnWorldLoad;
import myau.event.network.OnPacketReceive;
import myau.event.render.OnRenderWorld;
import myau.mixin.accessor.MinecraftAccessor;
import myau.module.ClientModule;
import myau.setting.BooleanOption;
import myau.setting.ChoiceOption;
import myau.setting.IntegerOption;
import myau.setting.PercentOption;
import myau.util.math.Maths;
import myau.util.render.RenderHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockMobSpawner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.network.play.server.S22PacketMultiBlockChange;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.network.play.server.S22PacketMultiBlockChange.BlockUpdateData;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import net.minecraft.util.Vec3i;
import net.minecraftforge.common.ForgeModContainer;

public class Xray extends ClientModule {
   private static final Minecraft minecraft = Minecraft.func_71410_x();
   private static final LinkedHashSet<Integer> supportedBlocks = new LinkedHashSet<>(Arrays.asList(56, 14, 15, 16, 73, 74, 21, 129, 52, 83, 115));
   private static final LinkedHashSet<Vec3i> adjacentOffsets = new LinkedHashSet<>(
      Arrays.asList(new Vec3i(0, -1, 0), new Vec3i(1, 0, 0), new Vec3i(0, 0, -1), new Vec3i(0, 0, 1), new Vec3i(-1, 0, 0), new Vec3i(0, 1, 0))
   );
   private static final LinkedHashSet<Vec3i> radiusTwoOffsets = new LinkedHashSet<>(
      Arrays.asList(
         new Vec3i(0, -2, 0),
         new Vec3i(1, -1, 0),
         new Vec3i(0, -1, -1),
         new Vec3i(0, -1, 0),
         new Vec3i(0, -1, 1),
         new Vec3i(-1, -1, 0),
         new Vec3i(2, 0, 0),
         new Vec3i(0, 0, 2),
         new Vec3i(0, 0, -2),
         new Vec3i(-2, 0, 0),
         new Vec3i(1, 0, -1),
         new Vec3i(1, 0, 0),
         new Vec3i(1, 0, 1),
         new Vec3i(0, 0, -1),
         new Vec3i(0, 0, 1),
         new Vec3i(-1, 0, -1),
         new Vec3i(-1, 0, 0),
         new Vec3i(-1, 0, 1),
         new Vec3i(1, 1, 0),
         new Vec3i(0, 1, -1),
         new Vec3i(0, 1, 0),
         new Vec3i(0, 1, 1),
         new Vec3i(-1, 1, 0),
         new Vec3i(0, 2, 0)
      )
   );
   public final CopyOnWriteArraySet<BlockPos> discoveredBlocks = new CopyOnWriteArraySet<>();
   public final CopyOnWriteArraySet<BlockPos> updatedBlocks = new CopyOnWriteArraySet<>();
   public final ChoiceOption mode = new ChoiceOption("mode", 0, new String[]{"SOFT", "FULL"});
   public final PercentOption opacity = new PercentOption("opacity", 50);
   public final IntegerOption range = new IntegerOption("range", 64, 16, 512);
   public final BooleanOption cavesOnly = new BooleanOption("caves-only", true);
   public final IntegerOption cavesRadius = new IntegerOption("caves-radius", 2, 1, 2);
   public final BooleanOption diamonds = new BooleanOption("diamonds", true);
   public final BooleanOption diamondsTracers = new BooleanOption("diamonds-tracers", true, this.diamonds::getValue);
   public final BooleanOption gold = new BooleanOption("gold", true);
   public final BooleanOption goldTracers = new BooleanOption("gold-tracers", true, this.gold::getValue);
   public final BooleanOption iron = new BooleanOption("iron", false);
   public final BooleanOption ironTracers = new BooleanOption("iron-tracers", false, this.iron::getValue);
   public final BooleanOption coal = new BooleanOption("coal", false);
   public final BooleanOption coalTracers = new BooleanOption("coal-tracers", false, this.coal::getValue);
   public final BooleanOption redstone = new BooleanOption("redstone", false);
   public final BooleanOption redstoneTracers = new BooleanOption("redstone-tracers", false, this.redstone::getValue);
   public final BooleanOption lapis = new BooleanOption("lapis", false);
   public final BooleanOption lapisTracers = new BooleanOption("lapis-tracers", false, this.lapis::getValue);
   public final BooleanOption emeralds = new BooleanOption("emeralds", false);
   public final BooleanOption emeraldsTracers = new BooleanOption("emeralds-tracers", false, this.emeralds::getValue);
   public final BooleanOption spawners = new BooleanOption("spawners", false);
   public final BooleanOption spawnersTracers = new BooleanOption("spawners-tracers", false, this.spawners::getValue);
   public final BooleanOption canes = new BooleanOption("canes", false);
   public final BooleanOption canesTracers = new BooleanOption("canes-tracers", false, this.canes::getValue);
   public final BooleanOption warts = new BooleanOption("warts", false);
   public final BooleanOption wartsTracers = new BooleanOption("warts-tracers", false, this.warts::getValue);

   public Xray() {
      super("Xray", false, 0);
   }

   public boolean isSupportedBlock(int blockId) {
      return supportedBlocks.contains(blockId);
   }

   public boolean isBlockEnabled(int blockId) {
      switch (blockId) {
         case 14:
            return this.gold.getValue();
         case 15:
            return this.iron.getValue();
         case 16:
            return this.coal.getValue();
         case 21:
            return this.lapis.getValue();
         case 52:
            return this.spawners.getValue();
         case 56:
            return this.diamonds.getValue();
         case 73:
         case 74:
            return this.redstone.getValue();
         case 83:
            return this.canes.getValue();
         case 115:
            return this.warts.getValue();
         case 129:
            return this.emeralds.getValue();
         default:
            return false;
      }
   }

   private boolean shouldDrawTracer(int blockId) {
      if (!this.isBlockEnabled(blockId)) {
         return false;
      } else {
         switch (blockId) {
            case 14:
               return this.goldTracers.getValue();
            case 15:
               return this.ironTracers.getValue();
            case 16:
               return this.coalTracers.getValue();
            case 21:
               return this.lapisTracers.getValue();
            case 52:
               return this.spawnersTracers.getValue();
            case 56:
               return this.diamondsTracers.getValue();
            case 73:
            case 74:
               return this.redstoneTracers.getValue();
            case 83:
               return this.canesTracers.getValue();
            case 115:
               return this.wartsTracers.getValue();
            case 129:
               return this.emeraldsTracers.getValue();
            default:
               return false;
         }
      }
   }

   private Color colorFor(int blockId) {
      switch (blockId) {
         case 14:
            return new Color(16777045);
         case 15:
            return new Color(16777215);
         case 16:
            return new Color(0);
         case 21:
            return new Color(5592575);
         case 52:
            return new Color(16733695);
         case 56:
            return new Color(5636095);
         case 73:
         case 74:
            return new Color(16733525);
         case 83:
            return new Color(11206570);
         case 115:
            return new Color(11141120);
         case 129:
            return new Color(5635925);
         default:
            return new Color(-1);
      }
   }

   private void drawBlock(BlockPos position, int blockId, Vec3 tracerStart) {
      if (minecraft.field_71439_g.func_70011_f(position.func_177958_n(), position.func_177956_o(), position.func_177952_p())
         <= this.range.getValue().doubleValue()) {
         Color tint = this.colorFor(blockId);
         RenderHelper.outlineBlockBounds(position, 1.0, tint.getRed(), tint.getGreen(), tint.getBlue(), tint.getAlpha(), 1.5F);
         if (this.shouldDrawTracer(blockId)) {
            RenderHelper.drawTracer(
               tracerStart,
               position.func_177958_n() + 0.5,
               position.func_177956_o() + 0.5,
               position.func_177952_p() + 0.5,
               tint.getRed() / 255.0F,
               tint.getGreen() / 255.0F,
               tint.getBlue() / 255.0F,
               1.0F,
               1.5F
            );
         }
      }
   }

   private boolean isCaveOpening(BlockPos position) {
      if (!minecraft.field_71441_e.func_175668_a(position, false)) {
         return false;
      } else {
         Block block = minecraft.field_71441_e.func_180495_p(position).func_177230_c();
         return block instanceof BlockMobSpawner || !block.func_149730_j() || !block.func_149688_o().func_76218_k() || block.func_149744_f();
      }
   }

   public boolean passesCaveCheck(BlockPos position) {
      if (!this.cavesOnly.getValue()) {
         return true;
      } else {
         for (Vec3i offset : this.cavesRadius.getValue() >= 2 ? radiusTwoOffsets : adjacentOffsets) {
            if (this.isCaveOpening(position.func_177971_a(offset))) {
               return true;
            }
         }

         return false;
      }
   }

   @Listen
   public void onRenderWorld(OnRenderWorld event) {
      if (this.isEnabled()) {
         float partialTicks = ((MinecraftAccessor)minecraft).getTimer().field_74281_c;
         Vec3 direction;
         if (minecraft.field_71474_y.field_74320_O == 0) {
            direction = new Vec3(0.0, 0.0, 1.0)
               .func_178789_a(
                  (float)(-Math.toRadians(Maths.lerpFloat(minecraft.func_175606_aa().field_70127_C, minecraft.func_175606_aa().field_70125_A, partialTicks)))
               )
               .func_178785_b(
                  (float)(-Math.toRadians(Maths.lerpFloat(minecraft.func_175606_aa().field_70126_B, minecraft.func_175606_aa().field_70177_z, partialTicks)))
               );
         } else {
            direction = new Vec3(0.0, 0.0, 0.0)
               .func_178789_a(
                  (float)(-Math.toRadians(Maths.lerpFloat(minecraft.field_71439_g.field_70727_aS, minecraft.field_71439_g.field_70726_aT, partialTicks)))
               )
               .func_178785_b(
                  (float)(-Math.toRadians(Maths.lerpFloat(minecraft.field_71439_g.field_71107_bF, minecraft.field_71439_g.field_71109_bG, partialTicks)))
               );
         }

         Vec3 tracerStart = new Vec3(direction.field_72450_a, direction.field_72448_b + minecraft.func_175606_aa().func_70047_e(), direction.field_72449_c);
         RenderHelper.beginOverlay();

         for (BlockPos position : this.discoveredBlocks) {
            if (this.updatedBlocks.contains(position)) {
               this.discoveredBlocks.remove(position);
            } else {
               this.drawTrackedBlock(position, tracerStart, this.discoveredBlocks);
            }
         }

         for (BlockPos positionx : this.updatedBlocks) {
            this.drawTrackedBlock(positionx, tracerStart, this.updatedBlocks);
         }

         RenderHelper.endOverlay();
      }
   }

   private void drawTrackedBlock(BlockPos position, Vec3 tracerStart, CopyOnWriteArraySet<BlockPos> source) {
      int blockId = Block.func_149682_b(minecraft.field_71441_e.func_180495_p(position).func_177230_c());
      if (this.isBlockEnabled(blockId)) {
         this.drawBlock(position, blockId, tracerStart);
      } else {
         source.remove(position);
      }
   }

   @Listen
   public void onPacketReceive(OnPacketReceive event) {
      if (event.getPacket() instanceof S22PacketMultiBlockChange) {
         S22PacketMultiBlockChange packet = (S22PacketMultiBlockChange)event.getPacket();

         for (BlockUpdateData update : packet.func_179844_a()) {
            int blockId = Block.func_149682_b(update.func_180088_c().func_177230_c());
            if (this.isBlockEnabled(blockId)) {
               this.updatedBlocks.add(new BlockPos(update.func_180090_a()));
            }
         }
      } else {
         if (event.getPacket() instanceof S23PacketBlockChange) {
            S23PacketBlockChange packet = (S23PacketBlockChange)event.getPacket();
            int blockId = Block.func_149682_b(packet.func_180728_a().func_177230_c());
            if (this.isBlockEnabled(blockId)) {
               this.updatedBlocks.add(new BlockPos(packet.func_179827_b()));
            }
         }
      }
   }

   @Listen
   public void onWorldLoad(OnWorldLoad event) {
      this.discoveredBlocks.clear();
      this.updatedBlocks.clear();
   }

   @Override
   public void onEnable() {
      ForgeModContainer.forgeLightPipelineEnabled = false;
      RenderGlobal renderer = minecraft.field_71438_f;
      if (renderer != null) {
         renderer.func_72712_a();
      }
   }

   @Override
   public void onDisable() {
      ForgeModContainer.forgeLightPipelineEnabled = true;
      RenderGlobal renderer = minecraft.field_71438_f;
      if (renderer != null) {
         renderer.func_72712_a();
      }
   }

   @Override
   public void onSettingChanged(String name) {
      this.discoveredBlocks.clear();
      this.updatedBlocks.clear();
      if (this.isEnabled()) {
         RenderGlobal renderer = minecraft.field_71438_f;
         if (minecraft.field_71438_f == null) {
            return;
         }

         renderer.func_72712_a();
      }
   }
}
