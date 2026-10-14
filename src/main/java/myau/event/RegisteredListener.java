package myau.event;

import java.lang.reflect.Method;

public final class RegisteredListener {
   private final Object owner;
   private final Method method;
   private final int priority;

   public RegisteredListener(Object owner, Method method, int priority) {
      this.owner = owner;
      this.method = method;
      this.priority = priority;
   }

   public Object getOwner() {
      return this.owner;
   }

   public Method getMethod() {
      return this.method;
   }

   public int getPriority() {
      return this.priority;
   }
}
