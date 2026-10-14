package myau.event.input;

import myau.event.GameEvent;

public class OnKeyPress extends GameEvent {
   private final int keyCode;

   public OnKeyPress(int keyCode) {
      this.keyCode = keyCode;
   }

   public int getKeyCode() {
      return this.keyCode;
   }
}
