package myau.util.math;

public final class Maths {
   private Maths() {
   }

   public static float lerpFloat(float start, float end, float fraction) {
      return start + (end - start) * fraction;
   }

   public static double lerpDouble(double start, double end, double fraction) {
      return start + (end - start) * fraction;
   }

   public static float toRadians(float degrees) {
      return degrees / 180.0F * (float) Math.PI;
   }
}
