package myau.util;

public final class SneakyThrow {
   private SneakyThrow() {
   }

   public static RuntimeException propagate(Throwable failure) {
      return throwOriginal(failure);
   }

   private static <Failure extends Throwable> RuntimeException throwOriginal(Throwable failure) throws Failure {
      throw (Failure)failure;
   }
}
