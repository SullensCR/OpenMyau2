package myau.module.render;

import java.awt.Color;
import java.util.stream.Collectors;
import myau.core.ClientContext;
import myau.event.Listen;
import myau.event.render.OnRenderWorld;
import myau.mixin.accessor.MinecraftAccessor;
import myau.mixin.accessor.RenderManagerAccessor;
import myau.module.ClientModule;
import myau.setting.BooleanOption;
import myau.setting.ColorOption;
import myau.setting.PercentOption;
import myau.util.math.Maths;
import myau.util.render.RenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;

public class ChestESP extends ClientModule {
   private static final Minecraft minecraft = Minecraft.func_71410_x();
   public final ColorOption color = new ColorOption("color", 16755200);
   public final PercentOption opacity = new PercentOption("opacity", 100);
   public final BooleanOption tracers = new BooleanOption("tracers", false);

   public ChestESP() {
      super("ChestESP", false, 0);
   }

   @Listen
   public void onRenderWorld(OnRenderWorld event) {
      if (this.isEnabled()) {
         RenderHelper.beginOverlay();

         for (TileEntity chest : minecraft.field_71441_e.field_147482_g.stream().filter(ChestESP::isChest).collect(Collectors.toList())) {
            AxisAlignedBB bounds = new AxisAlignedBB(
                  chest.func_174877_v().func_177958_n() + 0.0625,
                  chest.func_174877_v().func_177956_o() + 0.0,
                  chest.func_174877_v().func_177952_p() + 0.0625,
                  chest.func_174877_v().func_177958_n() + 0.9375,
                  chest.func_174877_v().func_177956_o() + 0.875,
                  chest.func_174877_v().func_177952_p() + 0.9375
               )
               .func_72317_d(
                  -((RenderManagerAccessor)minecraft.func_175598_ae()).getRenderPosX(),
                  -((RenderManagerAccessor)minecraft.func_175598_ae()).getRenderPosY(),
                  -((RenderManagerAccessor)minecraft.func_175598_ae()).getRenderPosZ()
               );
            Color outline = new Color(this.color.getValue());
            RenderHelper.outlineBounds(
               bounds, outline.getRed(), outline.getGreen(), outline.getBlue(), (int)(this.opacity.getValue().intValue() / 100.0F * 255.0F), 1.5F
            );
            if (this.tracers.getValue()) {
               this.drawTracer(chest, outline);
            }
         }

         RenderHelper.endOverlay();
      }
   }

   private static boolean isChest(TileEntity tile) {
      return tile instanceof TileEntityChest || tile instanceof TileEntityEnderChest;
   }

   private void drawTracer(TileEntity chest, Color outline) {
      Vec3 start;
      if (minecraft.field_71474_y.field_74320_O == 0) {
         start = new Vec3(0.0, 0.0, 1.0)
            .func_178789_a(
               (float)(
                  -Math.toRadians(
                     Maths.lerpFloat(
                        minecraft.func_175606_aa().field_70127_C,
                        minecraft.func_175606_aa().field_70125_A,
                        ((MinecraftAccessor)minecraft).getTimer().field_74281_c
                     )
                  )
               )
            )
            .func_178785_b(
               (float)(
                  -Math.toRadians(
                     Maths.lerpFloat(
                        minecraft.func_175606_aa().field_70126_B,
                        minecraft.func_175606_aa().field_70177_z,
                        ((MinecraftAccessor)minecraft).getTimer().field_74281_c
                     )
                  )
               )
            );
      } else {
         start = new Vec3(0.0, 0.0, 0.0)
            .func_178789_a(
               (float)(
                  -Math.toRadians(
                     Maths.lerpFloat(
                        minecraft.field_71439_g.field_70727_aS, minecraft.field_71439_g.field_70726_aT, ((MinecraftAccessor)minecraft).getTimer().field_74281_c
                     )
                  )
               )
            )
            .func_178785_b(
               (float)(
                  -Math.toRadians(
                     Maths.lerpFloat(
                        minecraft.field_71439_g.field_71107_bF, minecraft.field_71439_g.field_71109_bG, ((MinecraftAccessor)minecraft).getTimer().field_74281_c
                     )
                  )
               )
            );
      }

      start = new Vec3(start.field_72450_a, start.field_72448_b + minecraft.func_175606_aa().func_70047_e(), start.field_72449_c);
      float tracerOpacity = ((Tracers)ClientContext.moduleManager.modules.get(Tracers.class)).opacity.getValue().intValue() / 100.0F;
      RenderHelper.drawTracer(
         start,
         chest.func_174877_v().func_177958_n() + 0.5,
         chest.func_174877_v().func_177956_o() + 0.5,
         chest.func_174877_v().func_177952_p() + 0.5,
         outline.getRed() / 255.0F,
         outline.getGreen() / 255.0F,
         outline.getBlue() / 255.0F,
         tracerOpacity,
         1.5F
      );
   }
}
