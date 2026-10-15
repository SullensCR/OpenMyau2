package myau.ui.components;

import java.util.concurrent.atomic.AtomicInteger;
import myau.setting.TextOption;
import myau.ui.ClickGui;
import myau.ui.Component;
import myau.ui.callback.GuiInput;
import myau.util.chat.ChatFormat;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

public class TextComponent implements Component {
   private final TextOption option;
   private final ModuleComponent module;
   private int offsetY;
   private int x;
   private int y;

   public TextComponent(TextOption option, ModuleComponent parentModule, int offsetY) {
      this.option = option;
      this.module = parentModule;
      this.x = parentModule.category.getX() + parentModule.category.getWidth();
      this.y = parentModule.category.getY() + parentModule.offsetY;
      this.offsetY = offsetY;
   }

   @Override
   public void draw(AtomicInteger offset) {
      GL11.glPushMatrix();
      GL11.glScaled(0.5, 0.5, 0.5);
      Minecraft.func_71410_x()
         .field_71466_p
         .func_175065_a(
            this.option.getName().replace("-", " ") + ": " + ChatFormat.formatText(this.option.getDisplayValue()),
            (float)((this.module.category.getX() + 4) * 2),
            (float)((this.module.category.getY() + this.offsetY + 5) * 2),
            -1,
            false
         );
      GL11.glPopMatrix();
   }

   @Override
   public void setComponentStartAt(int newOffsetY) {
      this.offsetY = newOffsetY;
   }

   @Override
   public int getHeight() {
      return 12;
   }

   @Override
   public void update(int mousePosX, int mousePosY) {
      this.y = this.module.category.getY() + this.offsetY;
      this.x = this.module.category.getX();
   }

   @Override
   public void mouseDown(int x, int y, int button) {
      if (this.isHovered(x, y) && button == 0 && this.module.panelExpand) {
         GuiInput.prompt(this.option.getName().replace("-", " "), this.option.getValue(), this.option::setValue, ClickGui.getInstance());
      }
   }

   @Override
   public void mouseReleased(int x, int y, int button) {
   }

   @Override
   public void keyTyped(char chatTyped, int keyCode) {
   }

   public boolean isHovered(int x, int y) {
      return x > this.x && x < this.x + this.module.category.getWidth() && y > this.y && y < this.y + 11;
   }

   @Override
   public boolean isVisible() {
      return this.option.isVisible();
   }
}
