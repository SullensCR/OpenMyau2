package myau.event.render;

import myau.event.GameEvent;

public class OnRenderWorld extends GameEvent {
   private final float partialTicks;

   public OnRenderWorld(float partialTicks) {
      this.partialTicks = partialTicks;
   }

   public float getPartialTicks() {
      return this.partialTicks;
   }
}
