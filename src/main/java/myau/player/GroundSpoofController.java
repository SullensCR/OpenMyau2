package myau.player;

import java.util.LinkedHashMap;
import myau.event.Listen;
import myau.event.player.OnPreWalkingUpdate;
import net.minecraft.client.Minecraft;

public class GroundSpoofController {
   private static final Minecraft minecraft = Minecraft.func_71410_x();
   private final LinkedHashMap<GroundSpoofSource, Boolean> requestsByOwner = new LinkedHashMap<>();
   private boolean offsetActive;

   public boolean isOffsetActive() {
      return this.offsetActive;
   }

   public boolean isDescendingOnGround() {
      return minecraft.field_71439_g.field_70122_E
         && minecraft.field_71439_g.field_70163_u < minecraft.field_71439_g.field_70137_T
         && minecraft.field_71439_g.field_70181_x < 0.0;
   }

   public boolean hasActiveRequest() {
      return this.requestsByOwner.containsValue(true);
   }

   public void setRequest(boolean enabled, GroundSpoofSource owner) {
      this.requestsByOwner.put(owner, enabled);
   }

   @Listen
   public void onPreWalkingUpdate(OnPreWalkingUpdate event) {
      if ((this.hasActiveRequest() || this.offsetActive) && this.isDescendingOnGround()) {
         minecraft.field_71439_g
            .func_70107_b(minecraft.field_71439_g.field_70165_t, minecraft.field_71439_g.field_70163_u + 0.001, minecraft.field_71439_g.field_70161_v);
         this.offsetActive = true;
      } else {
         this.offsetActive = false;
      }
   }
}
