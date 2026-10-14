package myau.event.input;

import myau.event.CancelableGameEvent;

public class OnWindowClick extends CancelableGameEvent {
   private final int windowId;
   private final int slotId;
   private final int mouseButton;
   private final int clickMode;

   public OnWindowClick(int windowId, int slotId, int mouseButton, int clickMode) {
      this.windowId = windowId;
      this.slotId = slotId;
      this.mouseButton = mouseButton;
      this.clickMode = clickMode;
   }
}
