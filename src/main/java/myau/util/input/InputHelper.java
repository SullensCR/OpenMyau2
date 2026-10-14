package myau.util.input;

import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public final class InputHelper {
   private InputHelper() {
   }

   public static String getKeyName(int keyCode) {
      return keyCode < 0 ? Mouse.getButtonName(keyCode + 100) : Keyboard.getKeyName(keyCode);
   }

   public static boolean isKeyDown(int keyCode) {
      return keyCode < 0 ? Mouse.isButtonDown(keyCode + 100) : Keyboard.isKeyDown(keyCode);
   }

   public static void updateKeyState(int keyCode) {
      setKeyState(keyCode, isKeyDown(keyCode));
   }

   public static void setKeyState(int keyCode, boolean pressed) {
      KeyBinding.func_74510_a(keyCode, pressed);
   }

   public static void onKeyTick(int keyCode) {
      KeyBinding.func_74507_a(keyCode);
   }
}
