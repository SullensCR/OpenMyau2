package myau.event.input;

import myau.event.CancelableGameEvent;

public class OnHotbarChange extends CancelableGameEvent {
   private static final int HOTBAR_SIZE = 9;
   private final int requestedSlot;
   private final int scrollDirection;

   public OnHotbarChange(int requestedSlot, int scrollDirection) {
      this.requestedSlot = requestedSlot;
      this.scrollDirection = Math.min(Math.max(scrollDirection, -1), 1);
   }

   public int getTargetSlot(int currentSlot) {
      return this.requestedSlot >= 0 && this.requestedSlot < 9 ? this.requestedSlot : Math.floorMod(currentSlot - this.scrollDirection, 9);
   }
}
