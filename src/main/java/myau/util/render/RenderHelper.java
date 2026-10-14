package myau.util.render;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;
import myau.mixin.accessor.EntityRendererAccessor;
import myau.mixin.accessor.MinecraftAccessor;
import myau.mixin.accessor.RenderManagerAccessor;
import myau.util.item.EnchantDescriber;
import myau.util.math.Maths;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

public final class RenderHelper {
   private static final Minecraft minecraft = Minecraft.func_71410_x();
   private static final Frustum frustum = new Frustum();
   private static final IntBuffer viewportBuffer = GLAllocation.func_74527_f(16);
   private static final FloatBuffer modelViewBuffer = GLAllocation.func_74529_h(16);
   private static final FloatBuffer projectionBuffer = GLAllocation.func_74529_h(16);
   private static final FloatBuffer screenCoordsBuffer = GLAllocation.func_74529_h(4);

   private RenderHelper() {
   }

   public static void drawOutlinedText(String text, float x, float y, int color) {
      String plainText = text.replaceAll("(?i)§[\\da-f]", "");
      minecraft.field_71466_p.func_175065_a(plainText, x + 1.0F, y, 0, false);
      minecraft.field_71466_p.func_175065_a(plainText, x - 1.0F, y, 0, false);
      minecraft.field_71466_p.func_175065_a(plainText, x, y + 1.0F, 0, false);
      minecraft.field_71466_p.func_175065_a(plainText, x, y - 1.0F, 0, false);
      minecraft.field_71466_p.func_175065_a(text, x, y, color, false);
   }

   public static void drawItemWithEnchantments(ItemStack stack, int x, int y) {
      GlStateManager.func_179094_E();
      GlStateManager.func_179132_a(true);
      GlStateManager.func_179086_m(256);
      net.minecraft.client.renderer.RenderHelper.func_74520_c();
      GL11.glDisable(2896);
      GlStateManager.func_179094_E();
      GlStateManager.func_179152_a(1.0F, 1.0F, -0.01F);
      minecraft.func_175599_af().field_77023_b = -150.0F;
      minecraft.func_175599_af().func_180450_b(stack, x, y);
      minecraft.func_175599_af().func_175030_a(minecraft.field_71466_p, stack, x, y);
      minecraft.func_175599_af().field_77023_b = 0.0F;
      GlStateManager.func_179121_F();
      net.minecraft.client.renderer.RenderHelper.func_74518_a();
      GlStateManager.func_179141_d();
      GlStateManager.func_179084_k();
      GlStateManager.func_179098_w();
      GlStateManager.func_179121_F();
      GlStateManager.func_179094_E();
      GlStateManager.func_179152_a(0.5F, 0.5F, 0.5F);
      GlStateManager.func_179097_i();
      List<String> enchantmentLabels = EnchantDescriber.labels(stack);

      for (int index = 0; index < enchantmentLabels.size(); index++) {
         drawOutlinedText(enchantmentLabels.get(index), x * 2.0F, (y + index * 4.0F) * 2.0F, -1);
      }

      GlStateManager.func_179126_j();
      GlStateManager.func_179152_a(2.0F, 2.0F, 2.0F);
      GlStateManager.func_179121_F();
   }

   public static void drawPotionIcon(PotionEffect effect, int x, int y) {
      Potion potion = Potion.field_76425_a[effect.func_76456_a()];
      int iconIndex = potion.func_76392_e();
      GlStateManager.func_179131_c(1.0F, 1.0F, 1.0F, 1.0F);
      GlStateManager.func_179094_E();
      GlStateManager.func_179132_a(true);
      GlStateManager.func_179086_m(256);
      GlStateManager.func_179094_E();
      GlStateManager.func_179152_a(1.0F, 1.0F, -0.01F);
      minecraft.func_110434_K().func_110577_a(new ResourceLocation("textures/gui/container/inventory.png"));
      Gui.func_146110_a(x, y, iconIndex % 8 * 18, 198 + iconIndex / 8 * 18, 18, 18, 256.0F, 256.0F);
      GlStateManager.func_179121_F();
      GlStateManager.func_179141_d();
      GlStateManager.func_179084_k();
      GlStateManager.func_179098_w();
      GlStateManager.func_179121_F();
   }

   public static void fillRect(float left, float top, float right, float bottom, int color) {
      if (color != 0) {
         setColor(color);
         GL11.glBegin(9);
         GL11.glVertex2f(left, top);
         GL11.glVertex2f(left, bottom);
         GL11.glVertex2f(right, bottom);
         GL11.glVertex2f(right, top);
         GL11.glEnd();
         GlStateManager.func_179117_G();
      }
   }

   public static void drawBorderedRect(float left, float top, float right, float bottom, float lineWidth, int fillColor, int borderColor) {
      fillRect(left, top, right, bottom, fillColor);
      if (!(lineWidth <= 0.0F) && borderColor != 0) {
         setColor(borderColor);
         GL11.glLineWidth(lineWidth);
         GL11.glEnable(2848);
         GL11.glHint(3154, 4354);
         GL11.glBegin(1);
         GL11.glVertex2f(left, top);
         GL11.glVertex2f(left, bottom);
         GL11.glVertex2f(right, bottom);
         GL11.glVertex2f(right, top);
         GL11.glVertex2f(left, top);
         GL11.glVertex2f(right, top);
         GL11.glVertex2f(left, bottom);
         GL11.glVertex2f(right, bottom);
         GL11.glEnd();
         GL11.glDisable(2848);
         GL11.glLineWidth(2.0F);
         GlStateManager.func_179117_G();
      }
   }

   public static void fillSmoothRect(float left, float top, float right, float bottom, int color) {
      if (color != 0) {
         setColor(color);
         GL11.glEnable(2881);
         GL11.glHint(3155, 4354);
         GL11.glBegin(9);

         for (int pass = 0; pass < 2; pass++) {
            GL11.glVertex2f(left, top);
            GL11.glVertex2f(left, bottom);
            GL11.glVertex2f(right, bottom);
            GL11.glVertex2f(right, top);
         }

         GL11.glEnd();
         GL11.glDisable(2881);
         GlStateManager.func_179117_G();
      }
   }

   public static void drawLine2D(float startX, float startY, float endX, float endY, float lineWidth, int color) {
      setColor(color);
      GL11.glLineWidth(lineWidth);
      GL11.glEnable(2848);
      GL11.glHint(3154, 4354);
      GL11.glBegin(1);
      GL11.glVertex2f(startX, startY);
      GL11.glVertex2f(endX, endY);
      GL11.glEnd();
      GL11.glDisable(2848);
      GL11.glLineWidth(2.0F);
      GlStateManager.func_179117_G();
   }

   public static void drawTracer(Vec3 start, double worldX, double worldY, double worldZ, float red, float green, float blue, float alpha, float lineWidth) {
      GlStateManager.func_179094_E();
      GlStateManager.func_179131_c(red, green, blue, alpha);
      boolean viewBobbing = minecraft.field_71474_y.field_74336_f;
      minecraft.field_71474_y.field_74336_f = false;
      ((EntityRendererAccessor)minecraft.field_71460_t).callSetupCameraTransform(((MinecraftAccessor)minecraft).getTimer().field_74281_c, 2);
      minecraft.field_71474_y.field_74336_f = viewBobbing;
      GL11.glLineWidth(lineWidth);
      GL11.glEnable(2848);
      GL11.glHint(3154, 4354);
      GL11.glBegin(1);
      GL11.glVertex3d(start.field_72450_a, start.field_72448_b, start.field_72449_c);
      GL11.glVertex3d(
         worldX - ((RenderManagerAccessor)minecraft.func_175598_ae()).getRenderPosX(),
         worldY - ((RenderManagerAccessor)minecraft.func_175598_ae()).getRenderPosY(),
         worldZ - ((RenderManagerAccessor)minecraft.func_175598_ae()).getRenderPosZ()
      );
      GL11.glEnd();
      GL11.glDisable(2848);
      GL11.glLineWidth(2.0F);
      GlStateManager.func_179117_G();
      GlStateManager.func_179121_F();
   }

   public static void drawChevron(float x, float y, float angleRadians, float length, float lineWidth, int color) {
      float leftAngle = angleRadians + (float)Math.toRadians(45.0);
      float rightAngle = angleRadians - (float)Math.toRadians(45.0);
      setColor(color);
      GL11.glLineWidth(lineWidth);
      GL11.glEnable(2848);
      GL11.glHint(3154, 4354);
      GL11.glBegin(1);
      GL11.glVertex2f(x, y);
      GL11.glVertex2f(x + length * (float)Math.cos(leftAngle), y + length * (float)Math.sin(leftAngle));
      GL11.glVertex2f(x, y);
      GL11.glVertex2f(x + length * (float)Math.cos(rightAngle), y + length * (float)Math.sin(rightAngle));
      GL11.glEnd();
      GL11.glDisable(2848);
      GL11.glLineWidth(2.0F);
      GlStateManager.func_179117_G();
   }

   public static void fillArrow(float x, float y, float angleRadians, float length, int color) {
      float leftAngle = angleRadians + (float)Math.toRadians(26.25);
      float rightAngle = angleRadians - (float)Math.toRadians(26.25);
      setColor(color);
      GL11.glEnable(2881);
      GL11.glHint(3155, 4354);
      GL11.glBegin(9);
      GL11.glVertex2f(x, y);
      GL11.glVertex2f(x + length * (float)Math.cos(leftAngle), y + length * (float)Math.sin(leftAngle));
      GL11.glVertex2f(x + length * (float)Math.cos(rightAngle), y + length * (float)Math.sin(rightAngle));
      GL11.glEnd();
      GL11.glDisable(2881);
      GlStateManager.func_179117_G();
   }

   public static void drawFramebuffer(Framebuffer framebuffer) {
      ScaledResolution scaledResolution = new ScaledResolution(minecraft);
      GlStateManager.func_179144_i(framebuffer.field_147617_g);
      GL11.glBegin(7);
      GL11.glTexCoord2d(0.0, 1.0);
      GL11.glVertex2d(0.0, 0.0);
      GL11.glTexCoord2d(0.0, 0.0);
      GL11.glVertex2d(0.0, scaledResolution.func_78328_b());
      GL11.glTexCoord2d(1.0, 0.0);
      GL11.glVertex2d(scaledResolution.func_78326_a(), scaledResolution.func_78328_b());
      GL11.glTexCoord2d(1.0, 1.0);
      GL11.glVertex2d(scaledResolution.func_78326_a(), 0.0);
      GL11.glEnd();
   }

   public static void drawHorizontalCircle(double x, double y, double z, double radius, int segments, float lineWidth, int color) {
      setColor(color);
      GL11.glLineWidth(lineWidth);
      GL11.glEnable(2848);
      GL11.glHint(3154, 4354);
      GL11.glBegin(2);

      for (int segment = 0; segment <= segments; segment++) {
         double angle = segment * ((Math.PI * 2) / segments);
         GL11.glVertex3d(x + Math.cos(angle) * radius, y, z + Math.sin(angle) * radius);
      }

      GL11.glEnd();
      GL11.glDisable(2848);
      GL11.glLineWidth(2.0F);
      GlStateManager.func_179117_G();
   }

   public static void drawEntityCircle(Entity entity, double radius, int segments, float lineWidth, int color) {
      float partialTicks = ((MinecraftAccessor)minecraft).getTimer().field_74281_c;
      double x = Maths.lerpDouble(entity.field_70142_S, entity.field_70165_t, partialTicks)
         - ((RenderManagerAccessor)minecraft.func_175598_ae()).getRenderPosX();
      double y = Maths.lerpDouble(entity.field_70137_T, entity.field_70163_u, partialTicks)
         - ((RenderManagerAccessor)minecraft.func_175598_ae()).getRenderPosY();
      double z = Maths.lerpDouble(entity.field_70136_U, entity.field_70161_v, partialTicks)
         - ((RenderManagerAccessor)minecraft.func_175598_ae()).getRenderPosZ();
      drawHorizontalCircle(x, y, z, radius, segments, lineWidth, color);
   }

   public static void fillBounds(AxisAlignedBB bounds, int red, int green, int blue, int alpha) {
      Tessellator tessellator = Tessellator.func_178181_a();
      WorldRenderer renderer = tessellator.func_178180_c();
      renderer.func_181668_a(7, DefaultVertexFormats.field_181706_f);
      renderer.func_181662_b(bounds.field_72340_a, bounds.field_72338_b, bounds.field_72339_c).func_181669_b(red, green, blue, alpha).func_181675_d();
      renderer.func_181662_b(bounds.field_72340_a, bounds.field_72338_b, bounds.field_72334_f).func_181669_b(red, green, blue, alpha).func_181675_d();
      renderer.func_181662_b(bounds.field_72336_d, bounds.field_72338_b, bounds.field_72334_f).func_181669_b(red, green, blue, alpha).func_181675_d();
      renderer.func_181662_b(bounds.field_72336_d, bounds.field_72338_b, bounds.field_72339_c).func_181669_b(red, green, blue, alpha).func_181675_d();
      renderer.func_181662_b(bounds.field_72340_a, bounds.field_72337_e, bounds.field_72339_c).func_181669_b(red, green, blue, alpha).func_181675_d();
      renderer.func_181662_b(bounds.field_72340_a, bounds.field_72337_e, bounds.field_72334_f).func_181669_b(red, green, blue, alpha).func_181675_d();
      renderer.func_181662_b(bounds.field_72336_d, bounds.field_72337_e, bounds.field_72334_f).func_181669_b(red, green, blue, alpha).func_181675_d();
      renderer.func_181662_b(bounds.field_72336_d, bounds.field_72337_e, bounds.field_72339_c).func_181669_b(red, green, blue, alpha).func_181675_d();
      renderer.func_181662_b(bounds.field_72340_a, bounds.field_72338_b, bounds.field_72339_c).func_181669_b(red, green, blue, alpha).func_181675_d();
      renderer.func_181662_b(bounds.field_72340_a, bounds.field_72337_e, bounds.field_72339_c).func_181669_b(red, green, blue, alpha).func_181675_d();
      renderer.func_181662_b(bounds.field_72336_d, bounds.field_72337_e, bounds.field_72339_c).func_181669_b(red, green, blue, alpha).func_181675_d();
      renderer.func_181662_b(bounds.field_72336_d, bounds.field_72338_b, bounds.field_72339_c).func_181669_b(red, green, blue, alpha).func_181675_d();
      renderer.func_181662_b(bounds.field_72340_a, bounds.field_72338_b, bounds.field_72334_f).func_181669_b(red, green, blue, alpha).func_181675_d();
      renderer.func_181662_b(bounds.field_72340_a, bounds.field_72337_e, bounds.field_72334_f).func_181669_b(red, green, blue, alpha).func_181675_d();
      renderer.func_181662_b(bounds.field_72336_d, bounds.field_72337_e, bounds.field_72334_f).func_181669_b(red, green, blue, alpha).func_181675_d();
      renderer.func_181662_b(bounds.field_72336_d, bounds.field_72338_b, bounds.field_72334_f).func_181669_b(red, green, blue, alpha).func_181675_d();
      renderer.func_181662_b(bounds.field_72340_a, bounds.field_72338_b, bounds.field_72339_c).func_181669_b(red, green, blue, alpha).func_181675_d();
      renderer.func_181662_b(bounds.field_72340_a, bounds.field_72337_e, bounds.field_72339_c).func_181669_b(red, green, blue, alpha).func_181675_d();
      renderer.func_181662_b(bounds.field_72340_a, bounds.field_72337_e, bounds.field_72334_f).func_181669_b(red, green, blue, alpha).func_181675_d();
      renderer.func_181662_b(bounds.field_72340_a, bounds.field_72338_b, bounds.field_72334_f).func_181669_b(red, green, blue, alpha).func_181675_d();
      renderer.func_181662_b(bounds.field_72336_d, bounds.field_72338_b, bounds.field_72339_c).func_181669_b(red, green, blue, alpha).func_181675_d();
      renderer.func_181662_b(bounds.field_72336_d, bounds.field_72337_e, bounds.field_72339_c).func_181669_b(red, green, blue, alpha).func_181675_d();
      renderer.func_181662_b(bounds.field_72336_d, bounds.field_72337_e, bounds.field_72334_f).func_181669_b(red, green, blue, alpha).func_181675_d();
      renderer.func_181662_b(bounds.field_72336_d, bounds.field_72338_b, bounds.field_72334_f).func_181669_b(red, green, blue, alpha).func_181675_d();
      tessellator.func_78381_a();
   }

   public static void outlineBounds(AxisAlignedBB bounds, int red, int green, int blue, int alpha, float lineWidth) {
      GL11.glLineWidth(lineWidth);
      GL11.glEnable(2848);
      GL11.glHint(3154, 4354);
      Tessellator tessellator = Tessellator.func_178181_a();
      WorldRenderer renderer = tessellator.func_178180_c();
      renderer.func_181668_a(3, DefaultVertexFormats.field_181706_f);
      renderer.func_181662_b(bounds.field_72340_a, bounds.field_72338_b, bounds.field_72339_c).func_181669_b(red, green, blue, alpha).func_181675_d();
      renderer.func_181662_b(bounds.field_72336_d, bounds.field_72338_b, bounds.field_72339_c).func_181669_b(red, green, blue, alpha).func_181675_d();
      renderer.func_181662_b(bounds.field_72336_d, bounds.field_72338_b, bounds.field_72334_f).func_181669_b(red, green, blue, alpha).func_181675_d();
      renderer.func_181662_b(bounds.field_72340_a, bounds.field_72338_b, bounds.field_72334_f).func_181669_b(red, green, blue, alpha).func_181675_d();
      renderer.func_181662_b(bounds.field_72340_a, bounds.field_72338_b, bounds.field_72339_c).func_181669_b(red, green, blue, alpha).func_181675_d();
      tessellator.func_78381_a();
      renderer.func_181668_a(3, DefaultVertexFormats.field_181706_f);
      renderer.func_181662_b(bounds.field_72340_a, bounds.field_72337_e, bounds.field_72339_c).func_181669_b(red, green, blue, alpha).func_181675_d();
      renderer.func_181662_b(bounds.field_72336_d, bounds.field_72337_e, bounds.field_72339_c).func_181669_b(red, green, blue, alpha).func_181675_d();
      renderer.func_181662_b(bounds.field_72336_d, bounds.field_72337_e, bounds.field_72334_f).func_181669_b(red, green, blue, alpha).func_181675_d();
      renderer.func_181662_b(bounds.field_72340_a, bounds.field_72337_e, bounds.field_72334_f).func_181669_b(red, green, blue, alpha).func_181675_d();
      renderer.func_181662_b(bounds.field_72340_a, bounds.field_72337_e, bounds.field_72339_c).func_181669_b(red, green, blue, alpha).func_181675_d();
      tessellator.func_78381_a();
      renderer.func_181668_a(1, DefaultVertexFormats.field_181706_f);
      renderer.func_181662_b(bounds.field_72340_a, bounds.field_72338_b, bounds.field_72339_c).func_181669_b(red, green, blue, alpha).func_181675_d();
      renderer.func_181662_b(bounds.field_72340_a, bounds.field_72337_e, bounds.field_72339_c).func_181669_b(red, green, blue, alpha).func_181675_d();
      renderer.func_181662_b(bounds.field_72336_d, bounds.field_72338_b, bounds.field_72339_c).func_181669_b(red, green, blue, alpha).func_181675_d();
      renderer.func_181662_b(bounds.field_72336_d, bounds.field_72337_e, bounds.field_72339_c).func_181669_b(red, green, blue, alpha).func_181675_d();
      renderer.func_181662_b(bounds.field_72336_d, bounds.field_72338_b, bounds.field_72334_f).func_181669_b(red, green, blue, alpha).func_181675_d();
      renderer.func_181662_b(bounds.field_72336_d, bounds.field_72337_e, bounds.field_72334_f).func_181669_b(red, green, blue, alpha).func_181675_d();
      renderer.func_181662_b(bounds.field_72340_a, bounds.field_72338_b, bounds.field_72334_f).func_181669_b(red, green, blue, alpha).func_181675_d();
      renderer.func_181662_b(bounds.field_72340_a, bounds.field_72337_e, bounds.field_72334_f).func_181669_b(red, green, blue, alpha).func_181675_d();
      tessellator.func_78381_a();
      GL11.glDisable(2848);
      GL11.glLineWidth(2.0F);
   }

   public static void fillEntityBounds(Entity entity, int red, int green, int blue, int alpha, double expansion) {
      float partialTicks = ((MinecraftAccessor)minecraft).getTimer().field_74281_c;
      double x = Maths.lerpDouble(entity.field_70142_S, entity.field_70165_t, partialTicks);
      double y = Maths.lerpDouble(entity.field_70137_T, entity.field_70163_u, partialTicks);
      double z = Maths.lerpDouble(entity.field_70136_U, entity.field_70161_v, partialTicks);
      AxisAlignedBB bounds = entity.func_174813_aQ()
         .func_72314_b(expansion, expansion, expansion)
         .func_72317_d(x - entity.field_70165_t, y - entity.field_70163_u, z - entity.field_70161_v)
         .func_72317_d(
            -((RenderManagerAccessor)minecraft.func_175598_ae()).getRenderPosX(),
            -((RenderManagerAccessor)minecraft.func_175598_ae()).getRenderPosY(),
            -((RenderManagerAccessor)minecraft.func_175598_ae()).getRenderPosZ()
         );
      fillBounds(bounds, red, green, blue, alpha);
   }

   public static void outlineEntityBounds(Entity entity, int red, int green, int blue, int alpha, float lineWidth, double expansion) {
      float partialTicks = ((MinecraftAccessor)minecraft).getTimer().field_74281_c;
      double x = Maths.lerpDouble(entity.field_70142_S, entity.field_70165_t, partialTicks);
      double y = Maths.lerpDouble(entity.field_70137_T, entity.field_70163_u, partialTicks);
      double z = Maths.lerpDouble(entity.field_70136_U, entity.field_70161_v, partialTicks);
      AxisAlignedBB bounds = entity.func_174813_aQ()
         .func_72314_b(expansion, expansion, expansion)
         .func_72317_d(x - entity.field_70165_t, y - entity.field_70163_u, z - entity.field_70161_v)
         .func_72317_d(
            -((RenderManagerAccessor)minecraft.func_175598_ae()).getRenderPosX(),
            -((RenderManagerAccessor)minecraft.func_175598_ae()).getRenderPosY(),
            -((RenderManagerAccessor)minecraft.func_175598_ae()).getRenderPosZ()
         );
      outlineBounds(bounds, red, green, blue, alpha, lineWidth);
   }

   public static void fillBlockBounds(BlockPos position, double height, int red, int green, int blue, int alpha) {
      AxisAlignedBB bounds = new AxisAlignedBB(
            position.func_177958_n(),
            position.func_177956_o(),
            position.func_177952_p(),
            position.func_177958_n() + 1.0,
            position.func_177956_o() + height,
            position.func_177952_p() + 1.0
         )
         .func_72317_d(
            -((RenderManagerAccessor)minecraft.func_175598_ae()).getRenderPosX(),
            -((RenderManagerAccessor)minecraft.func_175598_ae()).getRenderPosY(),
            -((RenderManagerAccessor)minecraft.func_175598_ae()).getRenderPosZ()
         );
      fillBounds(bounds, red, green, blue, alpha);
   }

   public static void outlineBlockBounds(BlockPos position, double height, int red, int green, int blue, int alpha, float lineWidth) {
      AxisAlignedBB bounds = new AxisAlignedBB(
            position.func_177958_n(),
            position.func_177956_o(),
            position.func_177952_p(),
            position.func_177958_n() + 1.0,
            position.func_177956_o() + height,
            position.func_177952_p() + 1.0
         )
         .func_72317_d(
            -((RenderManagerAccessor)minecraft.func_175598_ae()).getRenderPosX(),
            -((RenderManagerAccessor)minecraft.func_175598_ae()).getRenderPosY(),
            -((RenderManagerAccessor)minecraft.func_175598_ae()).getRenderPosZ()
         );
      outlineBounds(bounds, red, green, blue, alpha, lineWidth);
   }

   public static Vector4f projectEntityBounds(Entity entity, double expansion) {
      float partialTicks = ((MinecraftAccessor)minecraft).getTimer().field_74281_c;
      RenderManagerAccessor renderManager = (RenderManagerAccessor)minecraft.func_175598_ae();
      ScaledResolution scaledResolution = new ScaledResolution(minecraft);
      float scaleFactor = scaledResolution.func_78325_e();
      double x = Maths.lerpDouble(entity.field_70142_S, entity.field_70165_t, partialTicks);
      double y = Maths.lerpDouble(entity.field_70137_T, entity.field_70163_u, partialTicks);
      double z = Maths.lerpDouble(entity.field_70136_U, entity.field_70161_v, partialTicks);
      AxisAlignedBB bounds = entity.func_174813_aQ()
         .func_72314_b(expansion, expansion, expansion)
         .func_72317_d(x - entity.field_70165_t, y - entity.field_70163_u, z - entity.field_70161_v)
         .func_72317_d(-renderManager.getRenderPosX(), -renderManager.getRenderPosY(), -renderManager.getRenderPosZ());
      GL11.glGetFloat(2982, modelViewBuffer);
      GL11.glGetFloat(2983, projectionBuffer);
      GL11.glGetInteger(2978, viewportBuffer);
      Vector3f[] corners = new Vector3f[]{
         new Vector3f((float)bounds.field_72340_a, (float)bounds.field_72338_b, (float)bounds.field_72339_c),
         new Vector3f((float)bounds.field_72340_a, (float)bounds.field_72337_e, (float)bounds.field_72339_c),
         new Vector3f((float)bounds.field_72336_d, (float)bounds.field_72338_b, (float)bounds.field_72339_c),
         new Vector3f((float)bounds.field_72336_d, (float)bounds.field_72337_e, (float)bounds.field_72339_c),
         new Vector3f((float)bounds.field_72340_a, (float)bounds.field_72338_b, (float)bounds.field_72334_f),
         new Vector3f((float)bounds.field_72340_a, (float)bounds.field_72337_e, (float)bounds.field_72334_f),
         new Vector3f((float)bounds.field_72336_d, (float)bounds.field_72338_b, (float)bounds.field_72334_f),
         new Vector3f((float)bounds.field_72336_d, (float)bounds.field_72337_e, (float)bounds.field_72334_f)
      };
      Vector4f screenBounds = null;

      for (Vector3f corner : corners) {
         if (GLU.gluProject(corner.x, corner.y, corner.z, modelViewBuffer, projectionBuffer, viewportBuffer, screenCoordsBuffer)) {
            float projectedX = screenCoordsBuffer.get(0) / scaleFactor;
            float projectedY = scaledResolution.func_78328_b() - screenCoordsBuffer.get(1) / scaleFactor;
            float projectedZ = screenCoordsBuffer.get(2);
            if (!(projectedZ < 0.0F) && !(projectedZ > 1.0F)) {
               if (screenBounds == null) {
                  screenBounds = new Vector4f(projectedX, projectedY, projectedX, projectedY);
               } else {
                  screenBounds.x = Math.min(projectedX, screenBounds.x);
                  screenBounds.y = Math.min(projectedY, screenBounds.y);
                  screenBounds.z = Math.max(projectedX, screenBounds.z);
                  screenBounds.w = Math.max(projectedY, screenBounds.w);
               }
            }
         }
      }

      return screenBounds;
   }

   public static boolean isEntityInFrustum(Entity entity, double expansion) {
      Entity viewEntity = minecraft.func_175606_aa();
      if (viewEntity != null && !(viewEntity.func_70032_d(entity) > 512.0F)) {
         float partialTicks = ((MinecraftAccessor)minecraft).getTimer().field_74281_c;
         double x = Maths.lerpDouble(entity.field_70142_S, entity.field_70165_t, partialTicks);
         double y = Maths.lerpDouble(entity.field_70137_T, entity.field_70163_u, partialTicks);
         double z = Maths.lerpDouble(entity.field_70136_U, entity.field_70161_v, partialTicks);
         AxisAlignedBB bounds = entity.func_174813_aQ()
            .func_72314_b(expansion, expansion, expansion)
            .func_72317_d(x - entity.field_70165_t, y - entity.field_70163_u, z - entity.field_70161_v);
         frustum.func_78547_a(viewEntity.field_70165_t, viewEntity.field_70163_u, viewEntity.field_70161_v);
         return frustum.func_78546_a(bounds);
      } else {
         return false;
      }
   }

   public static void beginOverlay() {
      GlStateManager.func_179147_l();
      GlStateManager.func_179112_b(770, 771);
      GlStateManager.func_179090_x();
      GlStateManager.func_179129_p();
      GlStateManager.func_179118_c();
      GlStateManager.func_179097_i();
   }

   public static void endOverlay() {
      GlStateManager.func_179126_j();
      GlStateManager.func_179141_d();
      GlStateManager.func_179089_o();
      GlStateManager.func_179098_w();
      GlStateManager.func_179084_k();
   }

   public static void setColor(int argb) {
      float alpha = (argb >> 24 & 0xFF) / 255.0F;
      float red = (argb >> 16 & 0xFF) / 255.0F;
      float green = (argb >> 8 & 0xFF) / 255.0F;
      float blue = (argb & 0xFF) / 255.0F;
      GlStateManager.func_179131_c(red, green, blue, alpha);
   }
}
