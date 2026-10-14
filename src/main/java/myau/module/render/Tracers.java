package myau.module.render;

import java.awt.Color;
import java.util.stream.Collectors;
import myau.core.ClientContext;
import myau.event.Listen;
import myau.event.render.OnRenderOverlay;
import myau.event.render.OnRenderWorld;
import myau.mixin.accessor.MinecraftAccessor;
import myau.module.ClientModule;
import myau.rotation.RotationHelper;
import myau.setting.BooleanOption;
import myau.setting.ChoiceOption;
import myau.setting.PercentOption;
import myau.util.chat.ChatFormat;
import myau.util.entity.EntityHelper;
import myau.util.math.Maths;
import myau.util.render.RenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

public class Tracers extends ClientModule {
   private static final Minecraft minecraft = Minecraft.func_71410_x();
   public final ChoiceOption color = new ChoiceOption("color", 0, new String[]{"DEFAULT", "TEAMS", "HUD"});
   public final BooleanOption lines = new BooleanOption("lines", true);
   public final BooleanOption arrows = new BooleanOption("arrows", false);
   public final PercentOption opacity = new PercentOption("opacity", 100);
   public final BooleanOption players = new BooleanOption("players", true);
   public final BooleanOption friends = new BooleanOption("friends", true);
   public final BooleanOption enemies = new BooleanOption("enemies", true);
   public final BooleanOption bots = new BooleanOption("bots", false);

   public Tracers() {
      super("Tracers", false, 0);
   }

   private boolean shouldTrackPlayer(EntityPlayer player) {
      if (player.field_70725_aQ > 0) {
         return false;
      } else if (minecraft.func_175606_aa().func_70032_d(player) > 512.0F) {
         return false;
      } else if (EntityHelper.isLocalOrViewPlayer(player)) {
         return false;
      } else if (EntityHelper.isFriend(player)) {
         return this.friends.getValue();
      } else if (EntityHelper.isEnemy(player)) {
         return this.enemies.getValue();
      } else {
         return EntityHelper.isBot(player) ? this.bots.getValue() : this.players.getValue();
      }
   }

   private boolean shouldTrackEntity(Entity entity) {
      return entity instanceof EntityPlayer && this.shouldTrackPlayer((EntityPlayer)entity);
   }

   private Color colorFor(EntityPlayer player, float alpha) {
      if (EntityHelper.isFriend(player)) {
         return withAlpha(ClientContext.friendList.color(), alpha);
      } else if (EntityHelper.isEnemy(player)) {
         return withAlpha(ClientContext.enemyList.color(), alpha);
      } else {
         switch (this.color.getValue()) {
            case 0:
               return EntityHelper.teamColor(player, alpha);
            case 1:
               boolean teammate = EntityHelper.isTeammate(player);
               byte teamColor = (byte)(teammate ? ChatFormat.BLUE.getRgb() : ChatFormat.RED.getRgb());
               return new Color(teamColor & 16777215 | (int)(alpha * 255.0F) << 24, true);
            case 2:
               int hudColor = ((HeadsUpDisplay)ClientContext.moduleManager.modules.get(HeadsUpDisplay.class)).getColor(System.currentTimeMillis()).getRGB();
               return new Color(hudColor & 16777215 | (int)(alpha * 255.0F) << 24, true);
            default:
               return new Color(1.0F, 1.0F, 1.0F, alpha);
         }
      }
   }

   private static Color withAlpha(Color color, float alpha) {
      return new Color(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, alpha);
   }

   @Listen
   public void onRenderWorld(OnRenderWorld event) {
      if (this.isEnabled() && this.lines.getValue()) {
         RenderHelper.beginOverlay();
         int thirdPersonView = minecraft.field_71474_y.field_74320_O;
         Vec3 direction;
         if (thirdPersonView == 0) {
            direction = new Vec3(0.0, 0.0, 1.0)
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
            direction = new Vec3(0.0, 0.0, 0.0)
               .func_178789_a(
                  (float)(
                     -Math.toRadians(
                        Maths.lerpFloat(
                           minecraft.field_71439_g.field_70727_aS,
                           minecraft.field_71439_g.field_70726_aT,
                           ((MinecraftAccessor)minecraft).getTimer().field_74281_c
                        )
                     )
                  )
               )
               .func_178785_b(
                  (float)(
                     -Math.toRadians(
                        Maths.lerpFloat(
                           minecraft.field_71439_g.field_71107_bF,
                           minecraft.field_71439_g.field_71109_bG,
                           ((MinecraftAccessor)minecraft).getTimer().field_74281_c
                        )
                     )
                  )
               );
         }

         Vec3 start = new Vec3(direction.field_72450_a, direction.field_72448_b + minecraft.func_175606_aa().func_70047_e(), direction.field_72449_c);

         for (EntityPlayer player : EntityHelper.entitiesBackToFront()
            .stream()
            .filter(this::shouldTrackEntity)
            .map(EntityPlayer.class::cast)
            .collect(Collectors.toList())) {
            Color lineColor = this.colorFor(player, this.opacity.getValue().intValue() / 100.0F);
            double x = Maths.lerpDouble(player.field_70142_S, player.field_70165_t, event.getPartialTicks());
            double y = Maths.lerpDouble(player.field_70137_T, player.field_70163_u, event.getPartialTicks()) - (player.func_70093_af() ? 0.125 : 0.0);
            double z = Maths.lerpDouble(player.field_70136_U, player.field_70161_v, event.getPartialTicks());
            RenderHelper.drawTracer(
               start,
               x,
               y + player.func_70047_e(),
               z,
               lineColor.getRed() / 255.0F,
               lineColor.getGreen() / 255.0F,
               lineColor.getBlue() / 255.0F,
               lineColor.getAlpha() / 255.0F,
               1.5F
            );
         }

         RenderHelper.endOverlay();
      }
   }

   @Listen
   public void onRenderOverlay(OnRenderOverlay event) {
      boolean drawArrows = this.isEnabled();
      if (drawArrows) {
         drawArrows = this.arrows.getValue();
         if (drawArrows) {
            for (EntityPlayer player : EntityHelper.entitiesBackToFront()
               .stream()
               .filter(this::shouldTrackEntity)
               .map(EntityPlayer.class::cast)
               .collect(Collectors.toList())) {
               float radius = 40.0F;
               float angle = RotationHelper.horizontalYawError(
                  Maths.lerpDouble(minecraft.field_71439_g.field_70169_q, minecraft.field_71439_g.field_70165_t, event.getPartialTicks()),
                  Maths.lerpDouble(minecraft.field_71439_g.field_70166_s, minecraft.field_71439_g.field_70161_v, event.getPartialTicks()),
                  Maths.lerpDouble(player.field_70169_q, player.field_70165_t, event.getPartialTicks()),
                  Maths.lerpDouble(player.field_70166_s, player.field_70161_v, event.getPartialTicks())
               );
               if (minecraft.field_71474_y.field_74320_O == 2) {
                  angle += 180.0F;
               }

               float directionX = (float)Math.sin(Math.toRadians(angle));
               float directionY = (float)Math.cos(Math.toRadians(angle)) * -1.0F;
               float alpha = this.opacity.getValue().floatValue() / 100.0F;
               float angleFromCenter = Math.abs(MathHelper.func_76142_g(angle));
               if (angleFromCenter < 30.0F) {
                  alpha = 0.0F;
               } else if (angleFromCenter < 60.0F) {
                  alpha *= (angleFromCenter - 30.0F) / 30.0F;
               }

               HeadsUpDisplay hud = (HeadsUpDisplay)ClientContext.moduleManager.modules.get(HeadsUpDisplay.class);
               GlStateManager.func_179094_E();
               GlStateManager.func_179152_a(hud.scale.getValue(), hud.scale.getValue(), 0.0F);
               GlStateManager.func_179109_b(
                  new ScaledResolution(minecraft).func_78326_a() / 2.0F / hud.scale.getValue(),
                  new ScaledResolution(minecraft).func_78328_b() / 2.0F / hud.scale.getValue(),
                  0.0F
               );
               GlStateManager.func_179094_E();
               GlStateManager.func_179109_b((radius + 15.0F) * directionX + 1.0F, (radius + 15.0F) * directionY + 1.0F, -100.0F);
               RenderHelper.beginOverlay();
               RenderHelper.fillArrow(0.0F, 0.0F, (float)(Math.atan2(directionY, directionX) + Math.PI), 10.0F, this.colorFor(player, alpha).getRGB());
               RenderHelper.endOverlay();
               GlStateManager.func_179121_F();
               GlStateManager.func_179121_F();
            }
         }
      }
   }
}
