package myau.ui.components;

import java.util.concurrent.atomic.AtomicInteger;
import myau.setting.ChoiceOption;
import myau.ui.Component;
import myau.util.chat.ChatFormat;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

public class ModeComponent implements Component {
   private final ChoiceOption option;
   private final ModuleComponent parentModule;
   private int x;
   private int y;
   private int offsetY;

   public ModeComponent(ChoiceOption option, ModuleComponent parentModule, int offsetY) {
      this.option = option;
      this.parentModule = parentModule;
      this.x = parentModule.category.getX() + parentModule.category.getWidth();
      this.y = parentModule.category.getY() + parentModule.offsetY;
      this.offsetY = offsetY;
   }

   @Override
   public void draw(AtomicInteger offset) {
      GL11.glPushMatrix();
      GL11.glScaled(0.5, 0.5, 0.5);
      String mode = this.option.getModeName().replace("_", " ");
      int nameWidth = (int)((float)Minecraft.func_71410_x().field_71466_p.func_78256_a(this.option.getName() + ": ") * 0.5F);
      Minecraft.func_71410_x()
         .field_71466_p
         .func_175065_a(
            this.option.getName() + ": ",
            (float)((this.parentModule.category.getX() + 4) * 2),
            (float)((this.parentModule.category.getY() + this.offsetY + 4) * 2),
            -1,
            true
         );
      String formattedMode = mode.isEmpty() ? "" : mode.substring(0, 1).toUpperCase() + mode.substring(1).toLowerCase();
      Minecraft.func_71410_x()
         .field_71466_p
         .func_175065_a(
            ChatFormat.formatText("&9" + formattedMode),
            (float)((this.parentModule.category.getX() + 4 + nameWidth) * 2),
            (float)((this.parentModule.category.getY() + this.offsetY + 4) * 2),
            -1,
            true
         );
      GL11.glPopMatrix();
   }

   @Override
   public void update(int mousePosX, int mousePosY) {
      this.y = this.parentModule.category.getY() + this.offsetY;
      this.x = this.parentModule.category.getX();
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
   public void mouseDown(int x, int y, int button) {
      if (this.isHovered(x, y)) {
         String[] modes = this.option.getModes();
         if (modes.length == 0) {
            return;
         }

         if (button == 0) {
            this.option.setValue((this.option.getValue() + 1) % modes.length);
         } else if (button == 1) {
            this.option.setValue((this.option.getValue() - 1 + modes.length) % modes.length);
         }
      }
   }

   @Override
   public void mouseReleased(int x, int y, int button) {
   }

   @Override
   public void keyTyped(char chatTyped, int keyCode) {
   }

   private boolean isHovered(int x, int y) {
      return x > this.x && x < this.x + this.parentModule.category.getWidth() && y > this.y && y < this.y + 11;
   }

   @Override
   public boolean isVisible() {
      return this.option.isVisible();
   }
}
