package myau.module.render;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import myau.core.ClientContext;
import myau.event.Listen;
import myau.event.render.OnFramebufferResize;
import myau.event.render.OnRenderOverlay;
import myau.event.render.OnRenderWorld;
import myau.mixin.accessor.EntityRendererAccessor;
import myau.mixin.accessor.RenderManagerAccessor;
import myau.module.ClientModule;
import myau.render.shader.ColorMaskProgram;
import myau.render.shader.OutlineProgram;
import myau.setting.BooleanOption;
import myau.setting.ChoiceOption;
import myau.util.chat.ChatFormat;
import myau.util.chat.ChatHelper;
import myau.util.entity.EntityHelper;
import myau.util.math.Maths;
import myau.util.render.ColorHelper;
import myau.util.render.RenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.util.vector.Vector4f;

public class ESP extends ClientModule {
   private static final Minecraft minecraft = Minecraft.func_71410_x();
   private final OutlineProgram outlineShader = new OutlineProgram();
   private final ColorMaskProgram colorShader = new ColorMaskProgram();
   private Framebuffer outlineFramebuffer;
   private boolean nameTagsAllowed = true;
   private boolean enchantmentGlintAllowed = true;
   public final ChoiceOption mode = new ChoiceOption("mode", 2, new String[]{"NONE", "2D", "3D", "OUTLINE"});
   public final ChoiceOption color = new ChoiceOption("color", 0, new String[]{"DEFAULT", "TEAMS", "HUD"});
   public final ChoiceOption healthBar = new ChoiceOption("health-bar", 0, new String[]{"NONE", "2D", "RAVEN"});
   public final BooleanOption players = new BooleanOption("players", true);
   public final BooleanOption friends = new BooleanOption("friends", true);
   public final BooleanOption enemies = new BooleanOption("enemies", true);
   public final BooleanOption self = new BooleanOption("self", false);
   public final BooleanOption bots = new BooleanOption("bots", false);

   public ESP() {
      super("ESP", false, 0);
   }

   public boolean areNameTagsAllowed() {
      return this.nameTagsAllowed;
   }

   public boolean isEnchantmentGlintAllowed() {
      return this.enchantmentGlintAllowed;
   }

   private boolean isEligible(EntityPlayer player) {
      if (!EntityHelper.isLocalOrViewPlayer(player)) {
         if (EntityHelper.isFriend(player)) {
            return this.friends.getValue();
         } else if (EntityHelper.isEnemy(player)) {
            return this.enemies.getValue();
         } else {
            return EntityHelper.isBot(player) ? this.bots.getValue() : this.players.getValue();
         }
      } else {
         return this.self.getValue() && minecraft.field_71474_y.field_74320_O != 0;
      }
   }

   private boolean shouldRender(EntityPlayer player) {
      return this.isEligible(player) && RenderHelper.isEntityInFrustum(player, 0.1F);
   }

   private Color colorFor(EntityPlayer player) {
      if (EntityHelper.isFriend(player)) {
         return ClientContext.friendList.color();
      } else if (EntityHelper.isEnemy(player)) {
         return ClientContext.enemyList.color();
      } else {
         switch (this.color.getValue()) {
            case 0:
               return EntityHelper.teamColor(player, 1.0F);
            case 1:
               boolean teammate = EntityHelper.isTeammate(player);
               byte teamColor = (byte)(teammate ? ChatFormat.BLUE.getRgb() : ChatFormat.RED.getRgb());
               return new Color(teamColor);
            case 2:
               return ((HeadsUpDisplay)ClientContext.moduleManager.modules.get(HeadsUpDisplay.class)).getColor(System.currentTimeMillis());
            default:
               return new Color(-1);
         }
      }
   }

   private void reportFramebufferUnavailable() {
      this.setEnabled(false);
      ChatHelper.addFormattedMessage(String.format("%s%s: &cFramebuffer is not enabled!&r", myau.util.chat.ChatPrefix.get(), this.getName()));
   }

   @Listen
   public void onFramebufferResize(OnFramebufferResize event) {
      if (this.outlineFramebuffer != null) {
         this.outlineFramebuffer.func_147608_a();
      }

      this.outlineFramebuffer = new Framebuffer(minecraft.field_71443_c, minecraft.field_71440_d, false);
   }

   @Listen(
      priority = 50
   )
   public void onRenderOverlay(OnRenderOverlay event) {
      if (this.isEnabled()) {
         int selectedMode = this.mode.getValue();
         if (selectedMode == 1 || selectedMode == 3 || this.healthBar.getValue() == 1) {
            if (selectedMode == 3 && !OpenGlHelper.func_148822_b()) {
               this.reportFramebufferUnavailable();
            } else {
               List<EntityPlayer> targets = EntityHelper.entitiesBackToFront()
                  .stream()
                  .filter(entity -> entity instanceof EntityPlayer)
                  .map(EntityPlayer.class::cast)
                  .filter(this::shouldRender)
                  .collect(Collectors.toList());
               if (!targets.isEmpty()) {
                  if (selectedMode == 3) {
                     this.drawOutlines(event, targets);
                  }

                  if (selectedMode == 1 || this.healthBar.getValue() == 1) {
                     Map<EntityPlayer, Vector4f> screenBounds = new HashMap<>();
                     ((EntityRendererAccessor)minecraft.field_71460_t).callSetupCameraTransform(event.getPartialTicks(), 0);

                     for (EntityPlayer player : targets) {
                        screenBounds.put(player, RenderHelper.projectEntityBounds(player, 0.1F));
                     }

                     minecraft.field_71460_t.func_78478_c();
                     GlStateManager.func_179147_l();
                     GlStateManager.func_179112_b(770, 771);
                     GlStateManager.func_179090_x();
                     GlStateManager.func_179118_c();

                     for (EntityPlayer player : targets) {
                        Vector4f bounds = screenBounds.get(player);
                        if (bounds != null) {
                           float left = bounds.x;
                           float top = bounds.y;
                           float right = bounds.z;
                           float bottom = bounds.w;
                           if (selectedMode == 1) {
                              int rgb = this.colorFor(player).getRGB();
                              RenderHelper.drawBorderedRect(left, top, right, bottom, 3.0F, 0, (rgb & 16579836) >> 2 | rgb & 0xFF000000);
                              RenderHelper.drawBorderedRect(left, top, right, bottom, 1.5F, 0, rgb);
                           }

                           if (this.healthBar.getValue() == 1) {
                              float health = player.func_110143_aJ() + player.func_110139_bj();
                              float fraction = Math.min(Math.max(health / player.func_110138_aP(), 0.0F), 1.0F);
                              float offset = (right - left) * 0.08F;
                              int rgb = ColorHelper.healthGradient(fraction).getRGB();
                              RenderHelper.drawLine2D(left - offset, top, left - offset, bottom, 3.0F, (rgb & 16579836) >> 2 | rgb & 0xFF000000);
                              RenderHelper.drawLine2D(left - offset, bottom, left - offset, bottom + (top - bottom) * fraction, 1.5F, rgb);
                           }
                        }
                     }

                     GlStateManager.func_179141_d();
                     GlStateManager.func_179098_w();
                     GlStateManager.func_179084_k();
                  }
               }
            }
         }
      }
   }

   private void drawOutlines(OnRenderOverlay event, List<EntityPlayer> targets) {
      GlStateManager.func_179094_E();
      GlStateManager.func_179123_a();
      if (this.outlineFramebuffer == null) {
         this.outlineFramebuffer = new Framebuffer(minecraft.field_71443_c, minecraft.field_71440_d, false);
      } else {
         this.outlineFramebuffer.func_147614_f();
      }

      this.outlineFramebuffer.func_147610_a(false);
      ((EntityRendererAccessor)minecraft.field_71460_t).callSetupCameraTransform(event.getPartialTicks(), 0);
      boolean shadows = minecraft.field_71474_y.field_181151_V;
      minecraft.field_71474_y.field_181151_V = false;
      this.nameTagsAllowed = false;
      this.enchantmentGlintAllowed = false;
      this.colorShader.use();

      for (EntityPlayer player : targets) {
         Color tint = this.colorFor(player);
         this.colorShader.setColor(tint);
         boolean invisible = player.func_82150_aj();
         player.func_82142_c(false);
         minecraft.func_175598_ae().func_147936_a(player, event.getPartialTicks(), true);
         player.func_82142_c(invisible);
      }

      this.colorShader.stop();
      this.enchantmentGlintAllowed = true;
      this.nameTagsAllowed = true;
      minecraft.field_71474_y.field_181151_V = shadows;
      minecraft.field_71460_t.func_78478_c();
      minecraft.func_147110_a().func_147610_a(false);
      this.outlineShader.use();
      RenderHelper.drawFramebuffer(this.outlineFramebuffer);
      this.outlineShader.stop();
      GlStateManager.func_179099_b();
      GlStateManager.func_179121_F();
   }

   @Listen
   public void onRenderWorld(OnRenderWorld event) {
      if (this.isEnabled()) {
         if (this.mode.getValue() == 2 || this.healthBar.getValue() == 2) {
            if (this.mode.getValue() == 3 && !OpenGlHelper.func_148822_b()) {
               this.reportFramebufferUnavailable();
            } else {
               RenderHelper.beginOverlay();

               for (EntityPlayer player : EntityHelper.entitiesBackToFront()
                  .stream()
                  .filter(entity -> entity instanceof EntityPlayer)
                  .map(EntityPlayer.class::cast)
                  .filter(this::shouldRender)
                  .collect(Collectors.toList())) {
                  if (this.mode.getValue() == 2) {
                     Color tint = this.colorFor(player);
                     RenderHelper.outlineEntityBounds(player, tint.getRed(), tint.getGreen(), tint.getBlue(), tint.getAlpha(), 1.5F, 0.1F);
                  }

                  if (this.healthBar.getValue() == 2) {
                     double x = Maths.lerpDouble(player.field_70142_S, player.field_70165_t, event.getPartialTicks())
                        - ((RenderManagerAccessor)minecraft.func_175598_ae()).getRenderPosX();
                     double y = Maths.lerpDouble(player.field_70137_T, player.field_70163_u, event.getPartialTicks())
                        - ((RenderManagerAccessor)minecraft.func_175598_ae()).getRenderPosY()
                        - 0.1F;
                     double z = Maths.lerpDouble(player.field_70136_U, player.field_70161_v, event.getPartialTicks())
                        - ((RenderManagerAccessor)minecraft.func_175598_ae()).getRenderPosZ();
                     GlStateManager.func_179094_E();
                     GlStateManager.func_179137_b(x, y, z);
                     GlStateManager.func_179114_b(minecraft.func_175598_ae().field_78735_i * -1.0F, 0.0F, 1.0F, 0.0F);
                     float currentHealth = player.func_110143_aJ() + player.func_110139_bj();
                     float healthFraction = Math.min(Math.max(currentHealth / player.func_110138_aP(), 0.0F), 1.0F);
                     Color tint = ColorHelper.healthGradient(healthFraction);
                     float left = 0.6F;
                     float height = player.field_70131_O + 0.2F;
                     float width = 0.1F;
                     float border = width * 0.275F;
                     RenderHelper.fillSmoothRect(left - border, -border, left + width + border, height + border, Color.black.getRGB());
                     RenderHelper.fillSmoothRect(left, 0.0F, left + width, height, Color.darkGray.getRGB());
                     RenderHelper.fillSmoothRect(left, 0.0F, left + width, height * healthFraction, tint.getRGB());
                     GlStateManager.func_179121_F();
                  }
               }

               RenderHelper.endOverlay();
            }
         }
      }
   }

   @Override
   public void onEnable() {
      if (this.mode.getValue() == 3 && !OpenGlHelper.func_148822_b()) {
         this.reportFramebufferUnavailable();
      }
   }
}
