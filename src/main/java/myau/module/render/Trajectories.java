package myau.module.render;

import java.awt.Color;
import java.util.ArrayList;
import myau.event.Listen;
import myau.event.render.OnRenderWorld;
import myau.mixin.accessor.RenderManagerAccessor;
import myau.module.ClientModule;
import myau.setting.BooleanOption;
import myau.setting.PercentOption;
import myau.util.render.RenderHelper;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemEgg;
import net.minecraft.item.ItemEnderPearl;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemSnowball;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

public class Trajectories extends ClientModule {
   private static final Minecraft minecraft = Minecraft.func_71410_x();
   public final PercentOption opacity = new PercentOption("opacity", 100);
   public final BooleanOption bow = new BooleanOption("bow", true);
   public final BooleanOption projectiles = new BooleanOption("projectiles", false);
   public final BooleanOption pearls = new BooleanOption("pearls", true);

   public Trajectories() {
      super("Trajectories", false, 0, true);
   }

   @Listen
   public void onRenderWorld(OnRenderWorld event) {
      if (this.isEnabled()) {
         if (minecraft.field_71439_g.func_70694_bm() != null) {
            if (minecraft.field_71474_y.field_74320_O == 0) {
               Item item = minecraft.field_71439_g.func_70694_bm().func_77973_b();
               RenderManager renderManager = minecraft.func_175598_ae();
               boolean arrow = false;
               float speed = 1.5F;
               float drag = 0.99F;
               float gravity;
               float radius;
               if (item instanceof ItemBow && this.bow.getValue()) {
                  if (!minecraft.field_71439_g.func_71039_bw()) {
                     return;
                  }

                  arrow = true;
                  gravity = 0.05F;
                  radius = 0.3F;
                  float charge = minecraft.field_71439_g.func_71057_bx() / 20.0F;
                  charge = (charge * charge + charge * 2.0F) / 3.0F;
                  if (charge < 0.1F) {
                     return;
                  }

                  if (charge > 1.0F) {
                     charge = 1.0F;
                  }

                  speed = charge * 3.0F;
               } else if (item instanceof ItemFishingRod && this.projectiles.getValue()) {
                  gravity = 0.04F;
                  radius = 0.25F;
                  drag = 0.92F;
               } else if ((item instanceof ItemSnowball || item instanceof ItemEgg) && this.projectiles.getValue()) {
                  gravity = 0.03F;
                  radius = 0.25F;
               } else {
                  if (!(item instanceof ItemEnderPearl) || !this.pearls.getValue()) {
                     return;
                  }

                  gravity = 0.03F;
                  radius = 0.25F;
               }

               float yaw = minecraft.field_71439_g.field_70177_z;
               float pitch = minecraft.field_71439_g.field_70125_A;
               double x = ((RenderManagerAccessor)renderManager).getRenderPosX() - MathHelper.func_76134_b(yaw / 180.0F * (float) Math.PI) * 0.16;
               double y = ((RenderManagerAccessor)renderManager).getRenderPosY() + minecraft.field_71439_g.func_70047_e() - 0.1F;
               double z = ((RenderManagerAccessor)renderManager).getRenderPosZ() - MathHelper.func_76126_a(yaw / 180.0F * (float) Math.PI) * 0.16;
               double motionX = MathHelper.func_76126_a(yaw / 180.0F * (float) Math.PI) * MathHelper.func_76134_b(pitch / 180.0F * (float) Math.PI)
                  * (arrow ? 1.0 : 0.4)
                  * -1.0;
               double motionY = MathHelper.func_76126_a(pitch / 180.0F * (float) Math.PI) * (arrow ? 1.0 : 0.4) * -1.0;
               double motionZ = MathHelper.func_76134_b(yaw / 180.0F * (float) Math.PI) * MathHelper.func_76134_b(pitch / 180.0F * (float) Math.PI)
                  * (arrow ? 1.0 : 0.4);
               float magnitude = MathHelper.func_76133_a(motionX * motionX + motionY * motionY + motionZ * motionZ);
               motionX /= magnitude;
               motionY /= magnitude;
               motionZ /= magnitude;
               motionX *= speed;
               motionY *= speed;
               motionZ *= speed;
               MovingObjectPosition hit = null;
               boolean collided = false;
               boolean hitEntity = false;
               WorldRenderer renderer = Tessellator.func_178181_a().func_178180_c();
               ArrayList<Vec3> points = new ArrayList<>();

               while (!collided && y > 0.0) {
                  Vec3 start = new Vec3(x, y, z);
                  Vec3 end = new Vec3(x + motionX, y + motionY, z + motionZ);
                  hit = minecraft.field_71441_e.func_147447_a(start, end, false, true, false);
                  start = new Vec3(x, y, z);
                  end = new Vec3(x + motionX, y + motionY, z + motionZ);
                  AxisAlignedBB searchBox = new AxisAlignedBB(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius)
                     .func_72321_a(motionX, motionY, motionZ)
                     .func_72314_b(1.0, 1.0, 1.0);
                  int firstChunkX = MathHelper.func_76128_c((searchBox.field_72340_a - 2.0) / 16.0);
                  int lastChunkX = MathHelper.func_76128_c((searchBox.field_72336_d + 2.0) / 16.0);
                  int firstChunkZ = MathHelper.func_76128_c((searchBox.field_72339_c - 2.0) / 16.0);
                  int lastChunkZ = MathHelper.func_76128_c((searchBox.field_72334_f + 2.0) / 16.0);
                  ArrayList<Entity> entities = new ArrayList<>();

                  for (int chunkX = firstChunkX; chunkX <= lastChunkX; chunkX++) {
                     for (int chunkZ = firstChunkZ; chunkZ <= lastChunkZ; chunkZ++) {
                        minecraft.field_71441_e.func_72964_e(chunkX, chunkZ).func_177414_a(minecraft.field_71439_g, searchBox, entities, null);
                     }
                  }

                  for (Entity entity : entities) {
                     if (entity.func_70067_L() && entity != minecraft.field_71439_g) {
                        AxisAlignedBB bounds = entity.func_174813_aQ().func_72314_b(radius, radius, radius);
                        MovingObjectPosition intercept = bounds.func_72327_a(start, end);
                        if (intercept != null) {
                           hitEntity = true;
                           collided = true;
                        }
                     }
                  }

                  x += motionX;
                  y += motionY;
                  z += motionZ;
                  if (minecraft.field_71441_e.func_180495_p(new BlockPos(x, y, z)).func_177230_c().func_149688_o() == Material.field_151586_h) {
                     motionX *= 0.6;
                     motionY *= 0.6;
                     motionZ *= 0.6;
                  } else {
                     motionX *= drag;
                     motionY *= drag;
                     motionZ *= drag;
                  }

                  motionY -= gravity;
                  points.add(
                     new Vec3(
                        x - ((RenderManagerAccessor)renderManager).getRenderPosX(),
                        y - ((RenderManagerAccessor)renderManager).getRenderPosY(),
                        z - ((RenderManagerAccessor)renderManager).getRenderPosZ()
                     )
                  );
               }

               if (points.size() > 1) {
                  RenderHelper.beginOverlay();
                  RenderHelper.setColor(
                     new Color(hitEntity ? 85 : 255, 255, hitEntity ? 85 : 255, (int)(this.opacity.getValue().floatValue() / 100.0F * 255.0F)).getRGB()
                  );
                  GL11.glLineWidth(1.5F);
                  GL11.glEnable(2848);
                  GL11.glHint(3154, 4354);
                  renderer.func_181668_a(3, DefaultVertexFormats.field_181705_e);
                  points.forEach(point -> renderer.func_181662_b(point.field_72450_a, point.field_72448_b, point.field_72449_c).func_181675_d());
                  Tessellator.func_178181_a().func_78381_a();
                  GlStateManager.func_179094_E();
                  GlStateManager.func_179137_b(
                     x - ((RenderManagerAccessor)renderManager).getRenderPosX(),
                     y - ((RenderManagerAccessor)renderManager).getRenderPosY(),
                     z - ((RenderManagerAccessor)renderManager).getRenderPosZ()
                  );
                  GlStateManager.func_179121_F();
                  GL11.glDisable(2848);
                  GL11.glLineWidth(2.0F);
                  GlStateManager.func_179117_G();
                  RenderHelper.endOverlay();
               }
            }
         }
      }
   }
}
