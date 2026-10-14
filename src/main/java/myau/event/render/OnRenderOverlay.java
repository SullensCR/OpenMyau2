package myau.event.render;

import myau.event.GameEvent;

public class OnRenderOverlay extends GameEvent {
   private final float partialTicks;

   public OnRenderOverlay(float partialTicks) {
      this.partialTicks = partialTicks;
   }

   public float getPartialTicks() {
      return this.partialTicks;
   }
}
