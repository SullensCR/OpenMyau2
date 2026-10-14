package myau.module.movement;

final class StrafeVector {
   private final double offsetX;
   private final double offsetZ;

   StrafeVector(double offsetX, double offsetZ) {
      this.offsetX = offsetX;
      this.offsetZ = offsetZ;
   }

   double x() {
      return this.offsetX;
   }

   double z() {
      return this.offsetZ;
   }
}
