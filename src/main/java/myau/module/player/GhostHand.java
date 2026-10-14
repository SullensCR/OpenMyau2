package myau.module.player;

import myau.module.ClientModule;
import myau.setting.BooleanOption;
import myau.util.entity.EntityHelper;
import myau.util.item.ItemHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

public class GhostHand extends ClientModule {
   public final BooleanOption teamOnly = new BooleanOption("team-only", true);
   public final BooleanOption ignoreWeapons = new BooleanOption("ignore-weapons", false);

   public GhostHand() {
      super("GhostHand", false, 0);
   }

   public boolean shouldIgnoreEntity(Entity entity) {
      boolean player = entity instanceof EntityPlayer;
      if (!player) {
         return false;
      } else if (EntityHelper.isBot((EntityPlayer)entity)) {
         return false;
      } else {
         return this.teamOnly.getValue() && !EntityHelper.isTeammate((EntityPlayer)entity)
            ? false
            : !this.ignoreWeapons.getValue() || !ItemHelper.isHoldingWeapon();
      }
   }
}
