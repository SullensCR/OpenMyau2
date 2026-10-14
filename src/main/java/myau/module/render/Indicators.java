package myau.module.render;

import java.awt.Color;
import java.util.stream.Collectors;
import myau.event.Listen;
import myau.event.render.OnRenderOverlay;
import myau.module.ClientModule;
import myau.rotation.RotationHelper;
import myau.setting.BooleanOption;
import myau.setting.FloatOption;
import myau.util.chat.ChatFormat;
import myau.util.entity.EntityHelper;
import myau.util.math.Maths;
import myau.util.render.RenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderPearl;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class Indicators extends ClientModule {
   private static final Minecraft minecraft = Minecraft.func_71410_x();
   public final FloatOption scale = new FloatOption("scale", 1.0F, 0.5F, 1.5F);
   public final FloatOption offset = new FloatOption("offset", 50.0F, 0.0F, 255.0F);
   public final BooleanOption directionCheck = new BooleanOption("direction-check", true);
   public final BooleanOption fireballs = new BooleanOption("fireballs", true);
   public final BooleanOption pearls = new BooleanOption("pearls", true);
   public final BooleanOption arrows = new BooleanOption("arrows", true);

   public Indicators() {
      super("Indicators", false, 0, true);
   }

   private boolean shouldIndicate(Entity entity) {
      double approach = (entity.field_70165_t - entity.field_70142_S) * (minecraft.field_71439_g.field_70165_t - entity.field_70165_t)
         + (entity.field_70163_u - entity.field_70137_T)
            * (minecraft.field_71439_g.field_70163_u + minecraft.field_71439_g.func_70047_e() - entity.field_70163_u - entity.field_70131_O / 2.0)
         + (entity.field_70161_v - entity.field_70136_U) * (minecraft.field_71439_g.field_70161_v - entity.field_70161_v);
      if (approach != 0.0 && (!(approach < 0.0) || !this.directionCheck.getValue())) {
         if (this.fireballs.getValue() && entity instanceof EntityFireball) {
            return true;
         } else {
            return this.pearls.getValue() && entity instanceof EntityEnderPearl ? true : this.arrows.getValue() && entity instanceof EntityArrow;
         }
      } else {
         return false;
      }
   }

   private Item iconFor(Entity entity) {
      if (entity instanceof EntityFireball) {
         return Items.field_151059_bz;
      } else if (entity instanceof EntityEnderPearl) {
         return Items.field_151079_bi;
      } else {
         return entity instanceof EntityArrow ? Items.field_151032_g : new Item();
      }
   }

   private Color colorFor(Entity entity) {
      if (entity instanceof EntityFireball) {
         return new Color(12676363);
      } else {
         return entity instanceof EntityEnderPearl ? new Color(2458740) : new Color(entity instanceof EntityArrow ? 9868950 : -1);
      }
   }

   @Listen(
      priority = 50
   )
   public void onRenderOverlay(OnRenderOverlay event) {
      if (this.isEnabled()) {
         for (Entity entity : EntityHelper.entitiesBackToFront().stream().filter(this::shouldIndicate).collect(Collectors.toList())) {
            float radius = 10.0F + this.offset.getValue();
            float angle = RotationHelper.horizontalYawError(
               Maths.lerpDouble(minecraft.field_71439_g.field_70169_q, minecraft.field_71439_g.field_70165_t, event.getPartialTicks()),
               Maths.lerpDouble(minecraft.field_71439_g.field_70166_s, minecraft.field_71439_g.field_70161_v, event.getPartialTicks()),
               Maths.lerpDouble(entity.field_70169_q, entity.field_70165_t, event.getPartialTicks()),
               Maths.lerpDouble(entity.field_70166_s, entity.field_70161_v, event.getPartialTicks())
            );
            if (minecraft.field_71474_y.field_74320_O == 2) {
               angle += 180.0F;
            }

            float directionX = (float)Math.sin(Math.toRadians(angle));
            float directionY = (float)Math.cos(Math.toRadians(angle)) * -1.0F;
            GlStateManager.func_179094_E();
            GlStateManager.func_179097_i();
            GlStateManager.func_179152_a(this.scale.getValue(), this.scale.getValue(), 0.0F);
            GlStateManager.func_179109_b(
               new ScaledResolution(minecraft).func_78326_a() / 2.0F / this.scale.getValue(),
               new ScaledResolution(minecraft).func_78328_b() / 2.0F / this.scale.getValue(),
               0.0F
            );
            GlStateManager.func_179094_E();
            GlStateManager.func_179109_b((radius + 0.0F) * directionX - 8.0F, (radius + 0.0F) * directionY - 8.0F, -300.0F);
            minecraft.func_175599_af().func_180450_b(new ItemStack(this.iconFor(entity)), 0, 0);
            GlStateManager.func_179121_F();
            String distanceLabel = String.format("%dm", (int)minecraft.field_71439_g.func_70032_d(entity));
            GlStateManager.func_179094_E();
            GlStateManager.func_179109_b(
               (radius + 0.0F) * directionX - minecraft.field_71466_p.func_78256_a(distanceLabel) / 2.0F + 1.0F, (radius + 0.0F) * directionY + 1.0F, -100.0F
            );
            minecraft.field_71466_p.func_175063_a(distanceLabel, 0.0F, 0.0F, ChatFormat.GRAY.getRgb() & 16777215 | -1090519040);
            GlStateManager.func_179121_F();
            GlStateManager.func_179094_E();
            GlStateManager.func_179109_b((radius + 15.0F) * directionX + 1.0F, (radius + 15.0F) * directionY + 1.0F, -100.0F);
            RenderHelper.beginOverlay();
            RenderHelper.drawChevron(0.0F, 0.0F, (float)(Math.atan2(directionY, directionX) + Math.PI), 7.5F, 1.5F, this.colorFor(entity).getRGB());
            RenderHelper.endOverlay();
            GlStateManager.func_179121_F();
            GlStateManager.func_179126_j();
            GlStateManager.func_179121_F();
         }
      }
   }
}
