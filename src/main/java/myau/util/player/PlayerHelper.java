package myau.util.player;

import com.google.common.collect.Multimap;
import myau.core.ClientContext;
import myau.module.combat.KeepSprint;
import myau.rotation.RotationHelper;
import myau.util.input.InputHelper;
import myau.util.world.BlockHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.client.Minecraft;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.stats.AchievementList;
import net.minecraft.stats.StatList;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraftforge.common.ForgeHooks;

public final class PlayerHelper {
   private static final Minecraft minecraft = Minecraft.func_71410_x();

   private PlayerHelper() {
   }

   public static boolean isJumpKeyDown() {
      return isGameplayKeyDown(minecraft.field_71474_y.field_74314_A.func_151463_i());
   }

   public static boolean isSneakKeyDown() {
      return isGameplayKeyDown(minecraft.field_71474_y.field_74311_E.func_151463_i());
   }

   public static boolean isLeftKeyDown() {
      return isGameplayKeyDown(minecraft.field_71474_y.field_74370_x.func_151463_i());
   }

   public static boolean isRightKeyDown() {
      return isGameplayKeyDown(minecraft.field_71474_y.field_74366_z.func_151463_i());
   }

   public static boolean isAttackKeyDown() {
      return isGameplayKeyDown(minecraft.field_71474_y.field_74312_F.func_151463_i());
   }

   public static boolean isUseItemKeyDown() {
      return isGameplayKeyDown(minecraft.field_71474_y.field_74313_G.func_151463_i());
   }

   private static boolean isGameplayKeyDown(int keyCode) {
      return minecraft.field_71462_r == null && InputHelper.isKeyDown(keyCode);
   }

   public static boolean wouldTakeFallDamage(float safeFallDistance) {
      if (!minecraft.field_71439_g.field_71075_bZ.field_75101_c && !minecraft.field_71439_g.field_71075_bZ.field_75102_a) {
         PotionEffect jumpBoost = minecraft.field_71439_g.func_70660_b(Potion.field_76430_j);
         float jumpReduction = jumpBoost == null ? 0.0F : jumpBoost.func_76458_c() + 1;
         float projectedFallDistance = minecraft.field_71439_g.field_70143_R;
         if (minecraft.field_71439_g.field_70181_x < -0.67 || !hasCollisionBelow()) {
            projectedFallDistance -= (float)minecraft.field_71439_g.field_70181_x;
         }

         return MathHelper.func_76123_f(projectedFallDistance - safeFallDistance - jumpReduction) > 0;
      } else {
         return false;
      }
   }

   public static boolean isAirBelow(int depth) {
      if (!minecraft.field_71439_g.field_71075_bZ.field_75101_c && !minecraft.field_71439_g.field_71075_bZ.field_75102_a) {
         int playerY = MathHelper.func_76128_c(minecraft.field_71439_g.field_70163_u);

         for (int offset = 0; offset <= depth; offset++) {
            int y = playerY - offset;
            if (y < 0) {
               break;
            }

            Block block = minecraft.field_71441_e
               .func_180495_p(new BlockPos(minecraft.field_71439_g.field_70165_t, y, minecraft.field_71439_g.field_70161_v))
               .func_177230_c();
            if (!(block instanceof BlockAir)) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public static boolean isOverVoid() {
      AxisAlignedBB bounds = minecraft.field_71439_g.func_174813_aQ().func_72314_b(-1.0E-6, 0.0, -1.0E-6);
      return isBoundsOverVoid(bounds);
   }

   public static boolean isBoundsOverVoid(AxisAlignedBB bounds) {
      if (!minecraft.field_71439_g.func_70090_H() && !minecraft.field_71439_g.func_180799_ab()) {
         int startY = MathHelper.func_76128_c(bounds.field_72338_b);
         if (startY < 0) {
            return true;
         } else {
            int minX = MathHelper.func_76128_c(bounds.field_72340_a);
            int maxX = MathHelper.func_76128_c(bounds.field_72336_d + 1.0);
            int minZ = MathHelper.func_76128_c(bounds.field_72339_c);
            int maxZ = MathHelper.func_76128_c(bounds.field_72334_f + 1.0);

            for (int x = minX; x < maxX; x++) {
               for (int z = minZ; z < maxZ; z++) {
                  for (int y = startY; y >= 0; y--) {
                     if (!BlockHelper.isReplaceableAt(new BlockPos(x, y, z))) {
                        return false;
                     }
                  }
               }
            }

            return true;
         }
      } else {
         return false;
      }
   }

   public static boolean isOffsetCollisionFree(double offsetX, double offsetY, double offsetZ) {
      AxisAlignedBB bounds = minecraft.field_71439_g.func_174813_aQ().func_72317_d(offsetX, offsetY, offsetZ);
      return minecraft.field_71441_e.func_72945_a(minecraft.field_71439_g, bounds).isEmpty();
   }

   public static boolean isNarrowOffsetCollisionFree(double offsetX, double offsetY, double offsetZ, float widthFactor) {
      double horizontalInset = minecraft.field_71439_g.field_70130_N / -2.0F * (1.0F - widthFactor);
      AxisAlignedBB bounds = minecraft.field_71439_g
         .func_174813_aQ()
         .func_72317_d(offsetX, offsetY, offsetZ)
         .func_72314_b(horizontalInset, 0.0, horizontalInset);
      return minecraft.field_71441_e.func_72945_a(minecraft.field_71439_g, bounds).isEmpty();
   }

   public static boolean hasCollisionBelow() {
      AxisAlignedBB bounds = minecraft.field_71439_g.func_174813_aQ().func_72317_d(0.0, -1.0, 0.0);
      return !minecraft.field_71441_e.func_72945_a(minecraft.field_71439_g, bounds).isEmpty();
   }

   public static boolean hasCollisionAbove() {
      AxisAlignedBB bounds = minecraft.field_71439_g.func_174813_aQ().func_72317_d(0.0, 1.0, 0.0);
      return !minecraft.field_71441_e.func_72945_a(minecraft.field_71439_g, bounds).isEmpty();
   }

   public static boolean isBlockWithinReach(BlockPos position, double reach) {
      return isBlockWithinReachFrom(
         position,
         minecraft.field_71439_g.field_70165_t,
         minecraft.field_71439_g.field_70163_u + minecraft.field_71439_g.func_70047_e(),
         minecraft.field_71439_g.field_70161_v,
         reach
      );
   }

   public static boolean isBlockWithinReachFrom(BlockPos position, double eyeX, double eyeY, double eyeZ, double reach) {
      return position.func_177957_d(eyeX, eyeY, eyeZ) < reach * reach;
   }

   public static void attackLocally(Entity target) {
      if (ForgeHooks.onPlayerAttackTarget(minecraft.field_71439_g, target) && target.func_70075_an() && !target.func_85031_j(minecraft.field_71439_g)) {
         float baseDamage = (float)minecraft.field_71439_g.func_110148_a(SharedMonsterAttributes.field_111264_e).func_111126_e();
         ItemStack heldItem = minecraft.field_71439_g.func_70694_bm();
         EnumCreatureAttribute creatureAttribute = target instanceof EntityLivingBase
            ? ((EntityLivingBase)target).func_70668_bt()
            : EnumCreatureAttribute.UNDEFINED;
         float enchantmentDamage = EnchantmentHelper.func_152377_a(heldItem, creatureAttribute);
         int knockbackLevel = EnchantmentHelper.func_77501_a(minecraft.field_71439_g);
         if (minecraft.field_71439_g.func_70051_ag()) {
            knockbackLevel++;
         }

         if (!(baseDamage <= 0.0F) || !(enchantmentDamage <= 0.0F)) {
            boolean criticalHit = isCriticalAttack();
            if (criticalHit && baseDamage > 0.0F) {
               baseDamage *= 1.5F;
            }

            float totalDamage = baseDamage + enchantmentDamage;
            int fireAspectLevel = EnchantmentHelper.func_90036_a(minecraft.field_71439_g);
            boolean temporaryFire = false;
            if (target instanceof EntityLivingBase && fireAspectLevel > 0 && !target.func_70027_ad()) {
               temporaryFire = true;
               target.func_70015_d(1);
            }

            double previousMotionX = target.field_70159_w;
            double previousMotionY = target.field_70181_x;
            double previousMotionZ = target.field_70179_y;
            boolean hit = target.func_70097_a(DamageSource.func_76365_a(minecraft.field_71439_g), totalDamage);
            if (!hit) {
               if (temporaryFire) {
                  target.func_70066_B();
               }
            } else {
               if (knockbackLevel > 0) {
                  target.func_70024_g(
                     -MathHelper.func_76126_a(minecraft.field_71439_g.field_70177_z * (float) Math.PI / 180.0F) * knockbackLevel * 0.5F,
                     0.1,
                     MathHelper.func_76134_b(minecraft.field_71439_g.field_70177_z * (float) Math.PI / 180.0F) * knockbackLevel * 0.5F
                  );
                  applyPostHitSprintSlowdown(target);
               }

               if (target instanceof EntityPlayerMP && target.field_70133_I) {
                  ((EntityPlayerMP)target).field_71135_a.func_147359_a(new S12PacketEntityVelocity(target));
                  target.field_70133_I = false;
                  target.field_70159_w = previousMotionX;
                  target.field_70181_x = previousMotionY;
                  target.field_70179_y = previousMotionZ;
               }

               if (criticalHit) {
                  minecraft.field_71439_g.func_71009_b(target);
               }

               if (enchantmentDamage > 0.0F) {
                  minecraft.field_71439_g.func_71047_c(target);
               }

               if (totalDamage >= 18.0F) {
                  minecraft.field_71439_g.func_71029_a(AchievementList.field_75999_E);
               }

               minecraft.field_71439_g.func_130011_c(target);
               if (target instanceof EntityLivingBase) {
                  EnchantmentHelper.func_151384_a((EntityLivingBase)target, minecraft.field_71439_g);
               }

               EnchantmentHelper.func_151385_b(minecraft.field_71439_g, target);
               if (target instanceof EntityLivingBase) {
                  minecraft.field_71439_g.func_71064_a(StatList.field_75951_w, Math.round(totalDamage * 10.0F));
                  if (fireAspectLevel > 0) {
                     target.func_70015_d(fireAspectLevel * 4);
                  }
               }

               minecraft.field_71439_g.func_71020_j(0.3F);
            }
         }
      }
   }

   private static boolean isCriticalAttack() {
      return minecraft.field_71439_g.field_70143_R > 0.0F
         && !minecraft.field_71439_g.field_70122_E
         && !minecraft.field_71439_g.func_70617_f_()
         && !minecraft.field_71439_g.func_70090_H()
         && !minecraft.field_71439_g.func_70644_a(Potion.field_76440_q)
         && minecraft.field_71439_g.field_70154_o == null;
   }

   private static void applyPostHitSprintSlowdown(Entity target) {
      KeepSprint keepSprint = (KeepSprint)ClientContext.moduleManager.modules.get(KeepSprint.class);
      boolean useKeepSprint = keepSprint != null && keepSprint.isEnabled();
      if (useKeepSprint && keepSprint.groundOnly.getValue() && !minecraft.field_71439_g.field_70122_E) {
         useKeepSprint = false;
      }

      if (useKeepSprint && keepSprint.reachOnly.getValue() && RotationHelper.distanceToEntity(target) <= 3.0) {
         useKeepSprint = false;
      }

      if (useKeepSprint) {
         double multiplier = 0.6 + 0.4 * (1.0 - keepSprint.slowdown.getValue().doubleValue() / 100.0);
         minecraft.field_71439_g.field_70159_w *= multiplier;
         minecraft.field_71439_g.field_70179_y *= multiplier;
      } else {
         minecraft.field_71439_g.field_70159_w *= 0.6;
         minecraft.field_71439_g.field_70179_y *= 0.6;
         minecraft.field_71439_g.func_70031_b(false);
      }
   }

   public static float estimateAttackDamage(Entity target) {
      ItemStack heldItem = minecraft.field_71439_g.func_70694_bm();
      float baseDamage = 1.0F;
      if (heldItem != null) {
         Multimap<String, AttributeModifier> modifiers = heldItem.func_111283_C();
         String attackDamageAttribute = SharedMonsterAttributes.field_111264_e.func_111108_a();

         for (AttributeModifier modifier : modifiers.get(attackDamageAttribute)) {
            baseDamage += (float)modifier.func_111164_d();
         }
      }

      EnumCreatureAttribute creatureAttribute = target instanceof EntityLivingBase
         ? ((EntityLivingBase)target).func_70668_bt()
         : EnumCreatureAttribute.UNDEFINED;
      float enchantmentDamage = EnchantmentHelper.func_152377_a(heldItem, creatureAttribute);
      if (baseDamage <= 0.0F && enchantmentDamage <= 0.0F) {
         return 0.0F;
      } else {
         if (isCriticalAttack() && baseDamage > 0.0F) {
            baseDamage *= 1.5F;
         }

         return baseDamage + enchantmentDamage;
      }
   }

   public static int swingDuration() {
      if (minecraft.field_71439_g.func_70644_a(Potion.field_76422_e)) {
         return 6 - (1 + minecraft.field_71439_g.func_70660_b(Potion.field_76422_e).func_76458_c());
      } else {
         return minecraft.field_71439_g.func_70644_a(Potion.field_76419_f)
            ? 6 + (1 + minecraft.field_71439_g.func_70660_b(Potion.field_76419_f).func_76458_c()) * 2
            : 6;
      }
   }

   public static void swingLocally() {
      if (!minecraft.field_71439_g.field_82175_bq
         || minecraft.field_71439_g.field_110158_av >= swingDuration() / 2
         || minecraft.field_71439_g.field_110158_av < 0) {
         minecraft.field_71439_g.field_110158_av = -1;
         minecraft.field_71439_g.field_82175_bq = true;
      }
   }
}
