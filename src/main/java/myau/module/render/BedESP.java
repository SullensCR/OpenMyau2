package myau.module.render;

import java.awt.Color;
import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArraySet;
import myau.core.ClientContext;
import myau.event.Listen;
import myau.event.render.OnRenderWorld;
import myau.mixin.accessor.RenderManagerAccessor;
import myau.module.ClientModule;
import myau.setting.BooleanOption;
import myau.setting.ChoiceOption;
import myau.setting.ColorOption;
import myau.setting.PercentOption;
import myau.util.render.RenderHelper;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockObsidian;
import net.minecraft.block.BlockBed.EnumPartType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

public class BedESP extends ClientModule {
   private static final Minecraft minecraft = Minecraft.func_71410_x();
   public final CopyOnWriteArraySet<BlockPos> bedHeads = new CopyOnWriteArraySet<>();
   public final ChoiceOption mode = new ChoiceOption("mode", 0, new String[]{"DEFAULT", "FULL"});
   public final ChoiceOption color = new ChoiceOption("color", 0, new String[]{"CUSTOM", "HUD"});
   public final ColorOption customColor = new ColorOption("custom-color", 16733525, this::showCustomColor);
   public final PercentOption opacity = new PercentOption("opacity", 25);
   public final BooleanOption outline = new BooleanOption("outline", false);
   public final BooleanOption obsidian = new BooleanOption("obsidian", true);

   public BedESP() {
      super("BedESP", false, 0);
   }

   public double getBedHeight() {
      return this.mode.getValue() == 1 ? 1.0 : 0.5625;
   }

   private Color bedColor() {
      switch (this.color.getValue()) {
         case 0:
            return new Color(this.customColor.getValue());
         case 1:
            return ((HeadsUpDisplay)ClientContext.moduleManager.modules.get(HeadsUpDisplay.class)).getColor(System.currentTimeMillis());
         default:
            return new Color(-1);
      }
   }

   private boolean showCustomColor() {
      int selection = this.color.getValue();
      return selection == 0;
   }

   private void drawObsidianBox(AxisAlignedBB bounds) {
      if (this.outline.getValue()) {
         RenderHelper.outlineBounds(bounds, 170, 0, 170, 255, 1.5F);
      }

      RenderHelper.fillBounds(bounds, 170, 0, 170, (int)(this.opacity.getValue().intValue() / 100.0F * 255.0F));
   }

   private void drawObsidianBlock(BlockPos position) {
      if (this.outline.getValue()) {
         RenderHelper.outlineBlockBounds(position, 1.0, 170, 0, 170, 255, 1.5F);
      }

      RenderHelper.fillBlockBounds(position, 1.0, 170, 0, 170, (int)(this.opacity.getValue().intValue() / 100.0F * 255.0F));
   }

   private AxisAlignedBB pairedBounds(BlockPos first, BlockPos second, double height) {
      return new AxisAlignedBB(
            Math.min(first.func_177958_n(), second.func_177958_n()),
            first.func_177956_o(),
            Math.min(first.func_177952_p(), second.func_177952_p()),
            Math.max(first.func_177958_n() + 1.0, second.func_177958_n() + 1.0),
            first.func_177956_o() + height,
            Math.max(first.func_177952_p() + 1.0, second.func_177952_p() + 1.0)
         )
         .func_72317_d(
            -((RenderManagerAccessor)minecraft.func_175598_ae()).getRenderPosX(),
            -((RenderManagerAccessor)minecraft.func_175598_ae()).getRenderPosY(),
            -((RenderManagerAccessor)minecraft.func_175598_ae()).getRenderPosZ()
         );
   }

   @Listen
   public void onRenderWorld(OnRenderWorld event) {
      if (this.isEnabled()) {
         RenderHelper.beginOverlay();

         for (BlockPos head : this.bedHeads) {
            IBlockState headState = minecraft.field_71441_e.func_180495_p(head);
            if (headState.func_177230_c() instanceof BlockBed && headState.func_177229_b(BlockBed.field_176472_a) == EnumPartType.HEAD) {
               BlockPos foot = head.func_177972_a(((EnumFacing)headState.func_177229_b(BlockBed.field_176387_N)).func_176734_d());
               IBlockState footState = minecraft.field_71441_e.func_180495_p(foot);
               if (footState.func_177230_c() instanceof BlockBed && footState.func_177229_b(BlockBed.field_176472_a) == EnumPartType.FOOT) {
                  if (this.obsidian.getValue()) {
                     for (EnumFacing side : Arrays.asList(EnumFacing.UP, EnumFacing.NORTH, EnumFacing.EAST, EnumFacing.SOUTH, EnumFacing.WEST)) {
                        BlockPos nearHead = head.func_177972_a(side);
                        BlockPos nearFoot = foot.func_177972_a(side);
                        boolean headCovered = minecraft.field_71441_e.func_180495_p(nearHead).func_177230_c() instanceof BlockObsidian;
                        boolean footCovered = minecraft.field_71441_e.func_180495_p(nearFoot).func_177230_c() instanceof BlockObsidian;
                        if (headCovered && footCovered) {
                           this.drawObsidianBox(this.pairedBounds(nearHead, nearFoot, 1.0));
                        } else if (headCovered) {
                           this.drawObsidianBlock(nearHead);
                        } else if (footCovered) {
                           this.drawObsidianBlock(nearFoot);
                        }
                     }
                  }

                  AxisAlignedBB bounds = this.pairedBounds(head, foot, this.getBedHeight());
                  Color selected = this.bedColor();
                  if (this.outline.getValue()) {
                     RenderHelper.outlineBounds(bounds, selected.getRed(), selected.getGreen(), selected.getBlue(), 255, 1.5F);
                  }

                  RenderHelper.fillBounds(
                     bounds, selected.getRed(), selected.getGreen(), selected.getBlue(), (int)(this.opacity.getValue().intValue() / 100.0F * 255.0F)
                  );
               }
            } else {
               this.bedHeads.remove(head);
            }
         }

         RenderHelper.endOverlay();
      }
   }

   @Override
   public void onEnable() {
      if (minecraft.field_71438_f != null) {
         minecraft.field_71438_f.func_72712_a();
      }
   }
}
