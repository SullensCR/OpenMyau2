package myau.event;

public abstract class CancelableGameEvent extends GameEvent {
   private boolean cancelled;

   public boolean isCancelled() {
      return this.cancelled;
   }

   public void setCancelled(boolean cancelled) {
      this.cancelled = cancelled;
   }
}
