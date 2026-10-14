package myau.event.player;

import myau.event.GameEvent;

public class OnSafeWalk extends GameEvent {
   private boolean safeWalk;

   public OnSafeWalk(boolean safeWalk) {
      this.safeWalk = safeWalk;
   }

   public boolean isSafeWalk() {
      return this.safeWalk;
   }

   public void setSafeWalk(boolean safeWalk) {
      this.safeWalk = safeWalk;
   }
}
