package myau.module.combat;

import java.util.HashMap;
import java.util.Map;
import myau.event.Listen;
import myau.event.input.OnLeftClick;
import myau.event.lifecycle.OnPreTick;
import myau.module.ClientModule;
import myau.setting.BooleanOption;
import myau.setting.IntegerOption;
import myau.util.player.PlayerHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;

public class HitSelect extends ClientModule {
   private static final Minecraft minecraft = Minecraft.func_71410_x();
   Map<EntityLivingBase, Integer> remainingHurtTicks = new HashMap<>();
   Map<EntityLivingBase, Float> previousAttackDamage = new HashMap<>();
   public final IntegerOption hurtTime = new IntegerOption("hurt-time", 4, 0, 10);
   public final BooleanOption swing = new BooleanOption("swing", true);

   public HitSelect() {
      super("HitSelect", false, 0);
   }

   public boolean shouldDelayAttack(EntityLivingBase target) {
      float attackDamage = PlayerHelper.estimateAttackDamage(target);
      int remaining = this.remainingHurtTicks.getOrDefault(target, 0);
      if (remaining <= 0) {
         this.remainingHurtTicks.put(target, target.field_70737_aN);
         this.previousAttackDamage.put(target, attackDamage);
         return false;
      } else {
         remaining = this.remainingHurtTicks.getOrDefault(target, 0);
         if (remaining <= this.hurtTime.getValue()) {
            return false;
         } else if (this.previousAttackDamage.getOrDefault(target, 0.0F) > attackDamage) {
            this.previousAttackDamage.put(target, attackDamage);
            return false;
         } else if (minecraft.field_71439_g.field_70737_aN <= 0 && minecraft.field_71439_g.func_70051_ag()) {
            if (this.swing.getValue()) {
               PlayerHelper.swingLocally();
            }

            return true;
         } else {
            return false;
         }
      }
   }

   @Listen(
      priority = -100
   )
   public void onLeftClick(OnLeftClick event) {
      boolean enabled = this.isEnabled();
      if (enabled && !event.isCancelled()) {
         if (minecraft.field_71476_x != null) {
            if (minecraft.field_71476_x.field_72313_a == MovingObjectType.ENTITY) {
               if (minecraft.field_71476_x.field_72308_g instanceof EntityLivingBase) {
                  EntityLivingBase target = (EntityLivingBase)minecraft.field_71476_x.field_72308_g;
                  if (this.shouldDelayAttack(target)) {
                     event.setCancelled(true);
                  }
               }
            }
         }
      }
   }

   @Listen
   public void onPreTick(OnPreTick event) {
      if (this.isEnabled()) {
         this.remainingHurtTicks.replaceAll(HitSelect::decrementHurtTicks);
      }
   }

   private static Integer decrementHurtTicks(EntityLivingBase target, Integer remaining) {
      return remaining > 0 ? remaining - 1 : remaining;
   }

   @Override
   public void onDisable() {
      this.remainingHurtTicks.clear();
      this.previousAttackDamage.clear();
   }
}
