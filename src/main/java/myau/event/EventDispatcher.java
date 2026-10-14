package myau.event;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EventDispatcher {
   private final Map<Class<? extends GameEvent>, List<RegisteredListener>> listeners = new LinkedHashMap<>();

   public void register(Object owner) {
      for (Method method : owner.getClass().getDeclaredMethods()) {
         Listen handler = method.getAnnotation(Listen.class);
         if (handler != null) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length == 1 && GameEvent.class.isAssignableFrom(parameterTypes[0])) {
               Class<? extends GameEvent> eventType = (Class<? extends GameEvent>)parameterTypes[0];
               if (!method.isAccessible()) {
                  method.setAccessible(true);
               }

               List<RegisteredListener> eventListeners = this.listeners.get(eventType);
               if (eventListeners == null) {
                  eventListeners = new ArrayList<>();
                  this.listeners.put(eventType, eventListeners);
               }

               eventListeners.add(new RegisteredListener(owner, method, handler.priority()));
               Collections.sort(eventListeners, Comparator.comparingInt(RegisteredListener::getPriority).reversed());
            }
         }
      }
   }

   public GameEvent post(GameEvent event) {
      List<RegisteredListener> eventListeners = this.listeners.get(event.getClass());
      if (eventListeners == null) {
         return event;
      } else {
         for (RegisteredListener listener : eventListeners) {
            try {
               listener.getMethod().invoke(listener.getOwner(), event);
            } catch (InvocationTargetException | IllegalArgumentException | IllegalAccessException var6) {
            }
         }

         return event;
      }
   }
}
