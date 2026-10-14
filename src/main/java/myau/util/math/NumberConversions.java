package myau.util.math;

public final class NumberConversions {
   private NumberConversions() {
   }

   public static long truncateFloatToLong(float input) {
      return !Float.isNaN(input) && !(input >= 9.223372E18F) ? (long)input : Long.MIN_VALUE;
   }
}
