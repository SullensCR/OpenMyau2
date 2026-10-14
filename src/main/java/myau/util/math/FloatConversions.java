package myau.util.math;

final class FloatConversions {
   private FloatConversions() {
   }

   static long truncateFloatToLong(float value) {
      return !Float.isNaN(value) && !(value >= 9.223372E18F) ? (long)value : Long.MIN_VALUE;
   }
}
