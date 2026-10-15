package myau.ui.components;

import java.util.concurrent.atomic.AtomicInteger;
import myau.core.ClientContext;
import myau.module.render.GuiModule;
import myau.module.render.HeadsUpDisplay;
import myau.ui.Component;
import myau.ui.dataset.BindStage;
import myau.util.input.InputHelper;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

public class BindComponent implements Component {
   private boolean isBinding;
   private final ModuleComponent parentModule;
   private int offsetY;
   private int x;
   private int y;

   public BindComponent(ModuleComponent b, int offsetY) {
      this.parentModule = b;
      this.x = b.category.getX() + b.category.getWidth();
      this.y = b.category.getY() + b.offsetY;
      this.offsetY = offsetY;
   }

   @Override
   public void draw(AtomicInteger offset) {
      GL11.glPushMatrix();
      GL11.glScaled(0.5, 0.5, 0.5);
      String displayText = this.isBinding
         ? BindStage.binding
         : BindStage.bind + ": " + InputHelper.getKeyName(this.parentModule.mod.getKeyCode());
      HeadsUpDisplay hud = (HeadsUpDisplay)ClientContext.moduleManager.modules.get(HeadsUpDisplay.class);
      int color = hud == null ? -1 : hud.getColorForRow(System.currentTimeMillis(), (long)offset.get()).getRGB();
      this.renderText(displayText, color);
      GL11.glPopMatrix();
   }

   @Override
   public void update(int mousePosX, int mousePosY) {
      this.y = this.parentModule.category.getY() + this.offsetY;
      this.x = this.parentModule.category.getX();
   }

   @Override
   public void mouseDown(int x, int y, int button) {
      if (this.isHovered(x, y) && button == 0 && this.parentModule.panelExpand) {
         this.isBinding = !this.isBinding;
      } else if (this.isBinding && this.parentModule.panelExpand) {
         if (button == 0) {
            this.isBinding = false;
         } else {
            this.parentModule.mod.setKeyCode(button - 100);
            this.isBinding = false;
         }
      }
   }

   @Override
   public void mouseReleased(int x, int y, int button) {
   }

   @Override
   public void keyTyped(char chatTyped, int keyCode) {
      if (this.isBinding) {
         if (keyCode == 1) {
            this.isBinding = false;
            return;
         }

         if (keyCode == 11) {
            if (this.parentModule.mod instanceof GuiModule) {
               this.parentModule.mod.setKeyCode(54);
            } else {
               this.parentModule.mod.setKeyCode(0);
            }
         } else {
            this.parentModule.mod.setKeyCode(keyCode);
         }

         this.isBinding = false;
      }
   }

   @Override
   public void setComponentStartAt(int newOffsetY) {
      this.offsetY = newOffsetY;
   }

   public boolean isHovered(int x, int y) {
      return x > this.x && x < this.x + this.parentModule.category.getWidth() && y > this.y - 1 && y < this.y + 12;
   }

   @Override
   public int getHeight() {
      return 12;
   }

   @Override
   public boolean isVisible() {
      return true;
   }

   private void renderText(String s, int color) {
      Minecraft.func_71410_x()
         .field_71466_p
         .func_175063_a(s, (float)((this.parentModule.category.getX() + 4) * 2), (float)((this.parentModule.category.getY() + this.offsetY + 3) * 2), color);
   }
}
