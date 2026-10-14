package myau.util.math;

import java.util.Random;

public final class RandomHelper {
   private static final Random RANDOM = new Random();

   private RandomHelper() {
   }

   public static boolean randomBoolean() {
      return RANDOM.nextBoolean();
   }

   public static long randomLong(long minInclusive, long maxInclusive) {
      return (long)randomDouble(minInclusive, maxInclusive + 1L);
   }

   public static float randomFloat(float min, float max) {
      return RANDOM.nextFloat() * (max - min) + min;
   }

   public static double randomDouble(double min, double max) {
      return RANDOM.nextDouble() * (max - min) + min;
   }
}
