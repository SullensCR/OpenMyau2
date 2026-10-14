package myau.module.render;

import myau.module.ClientModule;
import myau.setting.BooleanOption;
import myau.util.entity.EntityHelper;
import net.minecraft.client.Minecraft;
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

public class Chams extends ClientModule {
   private static final Minecraft minecraft = Minecraft.func_71410_x();
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

   public Chams() {
      super("Chams", false, 0);
   }

   public boolean shouldRenderEntity(EntityLivingBase entity) {
      int deathTime = entity.field_70725_aQ;
      if (deathTime > 0) {
         return false;
      } else if (minecraft.func_175606_aa().func_70032_d(entity) > 512.0F) {
         return false;
      } else if (entity instanceof EntityPlayer) {
         EntityPlayer player = (EntityPlayer)entity;
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
      } else if (!(entity instanceof EntityDragon) && !(entity instanceof EntityWither)) {
         if (!(entity instanceof EntityMob) && !(entity instanceof EntitySlime)) {
            return !(entity instanceof EntityAnimal)
                  && !(entity instanceof EntityBat)
                  && !(entity instanceof EntitySquid)
                  && !(entity instanceof EntityVillager)
               ? false
               : this.animals.getValue();
         } else if (entity instanceof EntityCreeper) {
            return this.creepers.getValue();
         } else if (entity instanceof EntityEnderman) {
            return this.endermen.getValue();
         } else {
            return entity instanceof EntityBlaze ? this.blazes.getValue() : this.mobs.getValue();
         }
      } else {
         return !entity.func_82150_aj() && this.bosses.getValue();
      }
   }
}
