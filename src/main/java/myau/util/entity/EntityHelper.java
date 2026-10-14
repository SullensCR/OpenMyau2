package myau.util.entity;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import myau.core.ClientContext;
import myau.util.world.ScoreboardHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.ScorePlayerTeam;

public final class EntityHelper {
   private static final Minecraft minecraft = Minecraft.func_71410_x();
   private static final int DEFAULT_TEAM_COLOR = 16777215;

   private EntityHelper() {
   }

   public static boolean isLoaded(Entity entity) {
      return entity != null && minecraft.field_71441_e.field_72996_f.contains(entity);
   }

   public static List<Entity> entitiesBackToFront() {
      RenderManager renderManager = minecraft.func_175598_ae();
      ArrayList<Entity> entities = new ArrayList<>(minecraft.field_71441_e.field_72996_f);
      entities.sort((first, second) -> compareRenderOrder(renderManager, first, second));
      return entities;
   }

   public static float armorAdjustedHealth(EntityLivingBase entity) {
      return entity.func_110143_aJ() * (20.0F / entity.func_70658_aO());
   }

   public static String formattedName(Entity entity) {
      return entity.func_145748_c_().func_150254_d().replaceAll("§\\S$", "").replaceAll("(?i)§r", "§f").trim();
   }

   public static Color teamColor(EntityPlayer player, float alpha) {
      int rgb = 16777215;
      ScorePlayerTeam team = (ScorePlayerTeam)player.func_96124_cp();
      if (team == null) {
         return fromRgb(rgb, alpha);
      } else {
         String formatting = FontRenderer.func_78282_e(team.func_96668_e());
         if (formatting.length() < 2) {
            return fromRgb(rgb, alpha);
         } else {
            rgb = minecraft.field_71466_p.func_175064_b(formatting.charAt(1));
            return fromRgb(rgb, alpha);
         }
      }
   }

   private static Color fromRgb(int rgb, float alpha) {
      return new Color((rgb >> 16 & 0xFF) / 255.0F, (rgb >> 8 & 0xFF) / 255.0F, (rgb & 0xFF) / 255.0F, alpha);
   }

   public static boolean isLocalOrViewPlayer(EntityPlayer player) {
      return player == minecraft.field_71439_g || player == minecraft.func_175606_aa();
   }

   public static boolean isBot(EntityPlayer player) {
      if (player == minecraft.field_71439_g) {
         return false;
      } else {
         NetworkPlayerInfo playerInfo = minecraft.func_147114_u().func_175104_a(player.func_70005_c_());
         if (playerInfo == null) {
            return true;
         } else if (!ScoreboardHelper.isHypixelScoreboard()) {
            return false;
         } else if (player.func_70005_c_().startsWith("§k")) {
            return player.func_82150_aj();
         } else if (playerInfo.func_178853_c() < 1) {
            return true;
         } else {
            ScorePlayerTeam team = playerInfo.func_178850_i();
            if (team != null) {
               if (team.func_96669_c().isEmpty() && team.func_96668_e().equals("§c")) {
                  return true;
               }

               if (!team.func_96668_e().isEmpty()) {
                  return false;
               }
            }

            return playerInfo.func_178845_a().getId().version() == 2;
         }
      }
   }

   public static boolean isTeammate(EntityPlayer player) {
      if (player == minecraft.field_71439_g) {
         return true;
      } else {
         NetworkPlayerInfo localInfo = minecraft.func_147114_u().func_175102_a(minecraft.field_71439_g.func_110124_au());
         if (localInfo != null && localInfo.func_178850_i() != null) {
            NetworkPlayerInfo targetInfo = minecraft.func_147114_u().func_175102_a(player.func_110124_au());
            return targetInfo != null && targetInfo.func_178850_i() != null
               ? localInfo.func_178850_i().func_96668_e().equals(targetInfo.func_178850_i().func_96668_e())
               : false;
         } else {
            return false;
         }
      }
   }

   public static boolean hasTeamArmorStand(EntityLivingBase entity) {
      if (entity == minecraft.field_71439_g) {
         return true;
      } else {
         NetworkPlayerInfo localInfo = minecraft.func_147114_u().func_175102_a(minecraft.field_71439_g.func_110124_au());
         if (localInfo == null) {
            return false;
         } else {
            ScorePlayerTeam localTeam = localInfo.func_178850_i();
            if (localTeam != null && localTeam.func_96668_e().length() >= 2) {
               EntityLivingBase armorStand = (EntityLivingBase)minecraft.field_71441_e.func_72857_a(EntityArmorStand.class, entity.func_174813_aQ(), entity);
               return armorStand != null && armorStand.func_70005_c_().contains(localTeam.func_96668_e().substring(0, 2));
            } else {
               return false;
            }
         }
      }
   }

   public static boolean isShopNPC(EntityLivingBase entity) {
      EntityLivingBase armorStand = (EntityLivingBase)minecraft.field_71441_e.func_72857_a(EntityArmorStand.class, entity.func_174813_aQ(), entity);
      if (armorStand == null) {
         return false;
      } else {
         String name = armorStand.func_70005_c_();
         return name.contains("RIGHT CLICK")
            || name.contains("ITEM SHOP")
            || name.contains("UPGRADES")
            || name.contains("BANKER")
            || name.contains("STREAK POWERS");
      }
   }

   public static boolean isFriend(EntityPlayer player) {
      return ClientContext.friendList.contains(player.func_70005_c_());
   }

   public static boolean isEnemy(EntityPlayer player) {
      return ClientContext.enemyList.contains(player.func_70005_c_());
   }

   private static int compareRenderOrder(RenderManager renderManager, Entity first, Entity second) {
      return Double.compare(
         renderManager.func_78714_a(second.field_70165_t, second.field_70163_u, second.field_70161_v),
         renderManager.func_78714_a(first.field_70165_t, first.field_70163_u, first.field_70161_v)
      );
   }
}
