package myau.event.player;

import myau.event.GameEvent;

public class OnEntityReach extends GameEvent {
   private double reachDistance;

   public OnEntityReach(double reachDistance) {
      this.reachDistance = reachDistance;
   }

   public double getReachDistance() {
      return this.reachDistance;
   }

   public void setReachDistance(double reachDistance) {
      this.reachDistance = reachDistance;
   }
}
