package myau.module.render;

import myau.module.ClientModule;
import myau.ui.ClickGui;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

public class GuiModule extends ClientModule {
   private static final Minecraft minecraft = Minecraft.func_71410_x();
   private ClickGui clickGui;

   public GuiModule() {
      super("ClickGui", false, Keyboard.KEY_RSHIFT, true);
   }

   @Override
   public void onEnable() {
      this.setEnabled(false);
      if (this.clickGui == null) {
         this.clickGui = new ClickGui();
      }

      minecraft.func_147108_a(this.clickGui);
   }
}
