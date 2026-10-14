package myau.event.player;

import myau.event.GameEvent;

public class OnBlockReach extends GameEvent {
   private double reachDistance;

   public OnBlockReach(double reachDistance) {
      this.reachDistance = reachDistance;
   }

   public double getReachDistance() {
      return this.reachDistance;
   }

   public void setReachDistance(double reachDistance) {
      this.reachDistance = reachDistance;
   }
}
