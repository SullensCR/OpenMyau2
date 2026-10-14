package myau.module.render;

import java.awt.Color;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import myau.core.ClientContext;
import myau.event.Listen;
import myau.event.render.OnRenderWorld;
import myau.mixin.accessor.RenderManagerAccessor;
import myau.module.ClientModule;
import myau.setting.BooleanOption;
import myau.setting.ChoiceOption;
import myau.setting.FloatOption;
import myau.setting.PercentOption;
import myau.util.chat.ChatFormat;
import myau.util.entity.EntityHelper;
import myau.util.math.Maths;
import myau.util.render.ColorHelper;
import myau.util.render.RenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.monster.EntityBlaze;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.EnumChatFormatting;
import org.apache.commons.lang3.StringUtils;

public class NameTags extends ClientModule {
   private static final Minecraft minecraft = Minecraft.func_71410_x();
   private static final DecimalFormat heartFormat = new DecimalFormat("0.0", new DecimalFormatSymbols(Locale.US));
   public final FloatOption scale = new FloatOption("scale", 1.0F, 0.5F, 2.0F);
   public final BooleanOption autoScale = new BooleanOption("auto-scale", true);
   public final PercentOption background = new PercentOption("background", 25);
   public final BooleanOption shadow = new BooleanOption("shadow", true);
   public final ChoiceOption distance = new ChoiceOption("distance", 0, new String[]{"NONE", "DEFAULT", "VAPE"});
   public final ChoiceOption health = new ChoiceOption("health", 2, new String[]{"NONE", "HP", "HEARTS", "TAB"});
   public final BooleanOption armor = new BooleanOption("armor", true);
   public final BooleanOption effects = new BooleanOption("effects", true);
   public final BooleanOption players = new BooleanOption("players", true);
   public final BooleanOption friends = new BooleanOption("friends", true);
   public final BooleanOption enemies = new BooleanOption("enemies", true);
   public final BooleanOption bosses = new BooleanOption("bosses", false);
   public final BooleanOption mobs = new BooleanOption("mobs", false);
   public final BooleanOption creepers = new BooleanOption("creepers", false);
   public final BooleanOption endermen = new BooleanOption("endermen", false);
   public final BooleanOption blazes = new BooleanOption("blazes", false);
   public final BooleanOption animals = new BooleanOption("animals", false);
   public final BooleanOption self = new BooleanOption("self", false);
   public final BooleanOption bots = new BooleanOption("bots", false);

   public NameTags() {
      super("NameTags", false, 0);
   }

   public boolean shouldRenderNameTag(EntityLivingBase entity) {
      if (entity.field_70725_aQ > 0 || minecraft.func_175606_aa().func_70032_d(entity) > 512.0F) {
         return false;
      } else if (entity instanceof EntityPlayer) {
         EntityPlayer player = (EntityPlayer)entity;
         if (EntityHelper.isLocalOrViewPlayer(player)) {
            return this.self.getValue() && minecraft.field_71474_y.field_74320_O != 0;
         } else if (EntityHelper.isFriend(player)) {
            return this.friends.getValue();
         } else if (EntityHelper.isEnemy(player)) {
            return this.enemies.getValue();
         } else {
            return EntityHelper.isBot(player) ? this.bots.getValue() : this.players.getValue();
         }
      } else if (entity instanceof EntityDragon || entity instanceof EntityWither) {
         return !entity.func_82150_aj() && this.bosses.getValue();
      } else if (!(entity instanceof EntityMob) && !(entity instanceof EntitySlime)) {
         return !(entity instanceof EntityAnimal) && !(entity instanceof EntityBat) && !(entity instanceof EntitySquid) && !(entity instanceof EntityVillager)
            ? false
            : this.animals.getValue();
      } else if (entity instanceof EntityCreeper) {
         return this.creepers.getValue();
      } else if (entity instanceof EntityEnderman) {
         return this.endermen.getValue();
      } else {
         return entity instanceof EntityBlaze ? this.blazes.getValue() : this.mobs.getValue();
      }
   }

   @Listen
   public void onRenderWorld(OnRenderWorld event) {
      if (this.isEnabled()) {
         for (Entity entity : EntityHelper.entitiesBackToFront()) {
            if (entity instanceof EntityLivingBase
               && this.shouldRenderNameTag((EntityLivingBase)entity)
               && (entity.field_70158_ak || RenderHelper.isEntityInFrustum(entity, 10.0))) {
               String displayName = EntityHelper.formattedName(entity);
               if (!StringUtils.isBlank(EnumChatFormatting.func_110646_a(displayName))) {
                  double x = Maths.lerpDouble(entity.field_70142_S, entity.field_70165_t, event.getPartialTicks())
                     - ((RenderManagerAccessor)minecraft.func_175598_ae()).getRenderPosX();
                  double y = Maths.lerpDouble(entity.field_70137_T, entity.field_70163_u, event.getPartialTicks())
                     - ((RenderManagerAccessor)minecraft.func_175598_ae()).getRenderPosY()
                     + entity.func_70047_e();
                  double z = Maths.lerpDouble(entity.field_70136_U, entity.field_70161_v, event.getPartialTicks())
                     - ((RenderManagerAccessor)minecraft.func_175598_ae()).getRenderPosZ();
                  double entityDistance = minecraft.func_175606_aa().func_70032_d(entity);
                  GlStateManager.func_179094_E();
                  GlStateManager.func_179137_b(x, y + (entity.func_70093_af() ? 0.225 : 0.4), z);
                  float rotation = minecraft.func_175598_ae().field_78735_i * -1.0F;
                  float axisX = 0.0F;
                  GlStateManager.func_179114_b(rotation, 0.0F, 1.0F, 0.0F);
                  rotation = minecraft.func_175598_ae().field_78732_j;
                  axisX = minecraft.field_71474_y.field_74320_O == 2 ? -1.0F : 1.0F;
                  GlStateManager.func_179114_b(rotation, axisX, 0.0F, 0.0F);
                  double labelScale = Math.pow(Math.min(Math.max(this.autoScale.getValue() ? entityDistance : 0.0, 6.0), 128.0), 0.75) * 0.0075;
                  GlStateManager.func_179139_a(-labelScale * this.scale.getValue().floatValue(), -labelScale * this.scale.getValue().floatValue(), 1.0);
                  String distancePrefix = "";
                  switch (this.distance.getValue()) {
                     case 1:
                        distancePrefix = String.format("&7%dm&r ", (int)entityDistance);
                        break;
                     case 2:
                        distancePrefix = String.format("&a[&f%d&a]&r ", (int)entityDistance);
                  }

                  float currentHealth = ((EntityLivingBase)entity).func_110143_aJ();
                  float absorption = ((EntityLivingBase)entity).func_110139_bj();
                  float maxHealth = ((EntityLivingBase)entity).func_110138_aP();
                  float healthFraction = Math.min(Math.max((currentHealth + absorption) / maxHealth, 0.0F), 1.0F);
                  String healthSuffix = "";
                  switch (this.health.getValue()) {
                     case 1:
                        healthSuffix = String.format(" %d%s", (int)currentHealth, absorption > 0.0F ? String.format(" &6%d&r", (int)absorption) : "&r");
                        break;
                     case 2:
                        String hearts = heartFormat.format(currentHealth / 2.0);
                        String absorptionHearts = absorption > 0.0F ? String.format(" &6%s&r", heartFormat.format(absorption / 2.0)) : "&r";
                        healthSuffix = String.format(" %s%s", hearts, absorptionHearts);
                        break;
                     case 3:
                        if (entity instanceof EntityPlayer) {
                           Scoreboard scoreboard = minecraft.field_71441_e.func_96441_U();
                           if (scoreboard != null) {
                              ScoreObjective objective = scoreboard.func_96539_a(2);
                              if (objective != null) {
                                 Score score = scoreboard.func_96529_a(entity.func_70005_c_(), objective);
                                 if (score != null) {
                                    healthSuffix = String.format(" &e%d&r", score.func_96652_c());
                                 }
                              }
                           }
                        }
                  }

                  String label = ChatFormat.formatText(String.format("%s&f%s&r%s", distancePrefix, displayName, healthSuffix));
                  int width = minecraft.field_71466_p.func_78256_a(label);
                  if (this.background.getValue() > 0) {
                     Color backgroundColor = !entity.func_70093_af() && !entity.func_82150_aj()
                        ? new Color(0.0F, 0.0F, 0.0F, this.background.getValue().intValue() / 100.0F)
                        : new Color(0.33F, 0.0F, 0.33F, this.background.getValue().intValue() / 100.0F);
                     RenderHelper.beginOverlay();
                     RenderHelper.fillRect(
                        -width / 2.0F - 1.0F,
                        -minecraft.field_71466_p.field_78288_b - 1.0F,
                        width / 2.0F + (this.shadow.getValue() ? 1.0F : 0.0F),
                        this.shadow.getValue() ? 0.0F : -1.0F,
                        backgroundColor.getRGB()
                     );
                     RenderHelper.endOverlay();
                  }

                  GlStateManager.func_179097_i();
                  minecraft.field_71466_p
                     .func_175065_a(
                        label,
                        -width / 2.0F,
                        -minecraft.field_71466_p.field_78288_b,
                        ColorHelper.healthGradient(healthFraction).getRGB(),
                        this.shadow.getValue()
                     );
                  GlStateManager.func_179126_j();
                  if (entity instanceof EntityPlayer) {
                     this.drawPlayerDetails((EntityPlayer)entity, width);
                  }

                  GlStateManager.func_179121_F();
               }
            }
         }
      }
   }

   private void drawPlayerDetails(EntityPlayer player, int labelWidth) {
      int rowOffset = minecraft.field_71466_p.field_78288_b + 2;
      if (this.armor.getValue()) {
         ArrayList<ItemStack> equipment = new ArrayList<>();

         for (int slot = 4; slot >= 0; slot--) {
            ItemStack stack = slot == 0 ? player.func_70694_bm() : player.field_71071_by.field_70460_b[slot - 1];
            if (stack != null) {
               equipment.add(stack);
            }
         }

         if (!equipment.isEmpty()) {
            int left = equipment.size() * -8;

            for (int index = 0; index < equipment.size(); index++) {
               RenderHelper.drawItemWithEnchantments(equipment.get(index), left + index * 16, -rowOffset - 16);
            }

            rowOffset += 16;
         }
      }

      if (this.effects.getValue()) {
         List<PotionEffect> visibleEffects = player.func_70651_bq().stream().filter(NameTags::hasStatusIcon).collect(Collectors.toList());
         if (!visibleEffects.isEmpty()) {
            GlStateManager.func_179094_E();
            GlStateManager.func_179152_a(0.5F, 0.5F, 1.0F);
            int left = visibleEffects.size() * -9;

            for (int index = 0; index < visibleEffects.size(); index++) {
               RenderHelper.drawPotionIcon(visibleEffects.get(index), left + index * 18, -(rowOffset * 2) - 18);
            }

            GlStateManager.func_179121_F();
         }
      }

      if (EntityHelper.isFriend(player)) {
         RenderHelper.beginOverlay();
         RenderHelper.drawBorderedRect(
            -labelWidth / 2.0F - 1.0F,
            -minecraft.field_71466_p.field_78288_b - 1.0F,
            labelWidth / 2.0F + 1.0F,
            this.shadow.getValue() ? 0.0F : -1.0F,
            1.5F,
            0,
            ClientContext.friendList.color().getRGB()
         );
         RenderHelper.endOverlay();
      } else if (EntityHelper.isEnemy(player)) {
         RenderHelper.beginOverlay();
         RenderHelper.drawBorderedRect(
            -labelWidth / 2.0F - 1.0F,
            -minecraft.field_71466_p.field_78288_b - 1.0F,
            labelWidth / 2.0F + 1.0F,
            this.shadow.getValue() ? 0.0F : -1.0F,
            1.5F,
            0,
            ClientContext.enemyList.color().getRGB()
         );
         RenderHelper.endOverlay();
      }
   }

   private static boolean hasStatusIcon(PotionEffect effect) {
      return Potion.field_76425_a[effect.func_76456_a()].func_76400_d();
   }
}
