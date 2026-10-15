package myau.ui.components;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import myau.core.ClientContext;
import myau.module.ClientModule;
import myau.module.render.HeadsUpDisplay;
import myau.setting.BooleanOption;
import myau.setting.ChoiceOption;
import myau.setting.ColorOption;
import myau.setting.FloatOption;
import myau.setting.IntegerOption;
import myau.setting.Option;
import myau.setting.PercentOption;
import myau.setting.TextOption;
import myau.ui.Component;
import myau.ui.dataset.impl.FloatSlider;
import myau.ui.dataset.impl.IntSlider;
import myau.ui.dataset.impl.PercentageSlider;
import net.minecraft.client.Minecraft;

public class ModuleComponent implements Component {
   public ClientModule mod;
   public CategoryComponent category;
   public int offsetY;
   private final ArrayList<Component> settings;
   public boolean panelExpand;

   public ModuleComponent(ClientModule mod, CategoryComponent category, int offsetY) {
      this.mod = mod;
      this.category = category;
      this.offsetY = offsetY;
      this.settings = new ArrayList<>();
      this.panelExpand = false;
      int y = offsetY + 12;
      List<Option<?>> options = ClientContext.settingManager.settingsByModule.get(mod.getClass());
      if (options != null) {
         for (Option<?> option : options) {
            if (option instanceof BooleanOption) {
               CheckBoxComponent component = new CheckBoxComponent((BooleanOption)option, this, y);
               this.settings.add(component);
               y += component.getHeight();
            } else if (option instanceof FloatOption) {
               SliderComponent component = new SliderComponent(new FloatSlider((FloatOption)option), this, y);
               this.settings.add(component);
               y += component.getHeight();
            } else if (option instanceof IntegerOption) {
               SliderComponent component = new SliderComponent(new IntSlider((IntegerOption)option), this, y);
               this.settings.add(component);
               y += component.getHeight();
            } else if (option instanceof PercentOption) {
               SliderComponent component = new SliderComponent(new PercentageSlider((PercentOption)option), this, y);
               this.settings.add(component);
               y += component.getHeight();
            } else if (option instanceof ChoiceOption) {
               ModeComponent component = new ModeComponent((ChoiceOption)option, this, y);
               this.settings.add(component);
               y += component.getHeight();
            } else if (option instanceof ColorOption) {
               ColorSliderComponent component = new ColorSliderComponent((ColorOption)option, this, y);
               this.settings.add(component);
               y += component.getHeight();
            } else if (option instanceof TextOption) {
               TextComponent component = new TextComponent((TextOption)option, this, y);
               this.settings.add(component);
               y += component.getHeight();
            }
         }
      }

      this.settings.add(new BindComponent(this, y));
   }

   @Override
   public void setComponentStartAt(int newOffsetY) {
      this.offsetY = newOffsetY;
      int y = this.offsetY + 16;

      for (Component component : this.settings) {
         component.setComponentStartAt(y);
         if (component.isVisible()) {
            y += component.getHeight();
         }
      }
   }

   @Override
   public void draw(AtomicInteger offset) {
      int textColor;
      if (this.mod.isEnabled()) {
         HeadsUpDisplay hud = (HeadsUpDisplay)ClientContext.moduleManager.modules.get(HeadsUpDisplay.class);
         textColor = hud == null ? Color.WHITE.getRGB() : hud.getColorForRow(System.currentTimeMillis(), (long)offset.get()).getRGB();
      } else {
         textColor = new Color(102, 102, 102).getRGB();
      }

      Minecraft.func_71410_x()
         .field_71466_p
         .func_175063_a(
            this.mod.getName(),
            (float)(this.category.getX() + this.category.getWidth() / 2 - Minecraft.func_71410_x().field_71466_p.func_78256_a(this.mod.getName()) / 2),
            (float)(this.category.getY() + this.offsetY + 4),
            textColor
         );
      if (this.panelExpand && !this.settings.isEmpty()) {
         for (Component component : this.settings) {
            if (component.isVisible()) {
               component.draw(offset);
               offset.incrementAndGet();
            }
         }
      }
   }

   @Override
   public int getHeight() {
      if (!this.panelExpand) {
         return 16;
      } else {
         int h = 16;

         for (Component component : this.settings) {
            if (component.isVisible()) {
               h += component.getHeight();
            }
         }

         return h;
      }
   }

   @Override
   public void update(int mousePosX, int mousePosY) {
      if (this.panelExpand && !this.settings.isEmpty()) {
         for (Component component : this.settings) {
            if (component.isVisible()) {
               component.update(mousePosX, mousePosY);
            }
         }
      }
   }

   @Override
   public void mouseDown(int x, int y, int button) {
      if (this.isHovered(x, y) && button == 0) {
         ClientContext.moduleManager.toggle(this.mod);
      }

      if (this.isHovered(x, y) && button == 1) {
         this.panelExpand = !this.panelExpand;
      }

      if (this.panelExpand) {
         for (Component component : this.settings) {
            if (component.isVisible()) {
               component.mouseDown(x, y, button);
            }
         }
      }
   }

   @Override
   public void mouseReleased(int x, int y, int button) {
      if (this.panelExpand) {
         for (Component component : this.settings) {
            if (component.isVisible()) {
               component.mouseReleased(x, y, button);
            }
         }
      }
   }

   @Override
   public void keyTyped(char chatTyped, int keyCode) {
      if (this.panelExpand) {
         for (Component component : this.settings) {
            if (component.isVisible()) {
               component.keyTyped(chatTyped, keyCode);
            }
         }
      }
   }

   public boolean isHovered(int x, int y) {
      return x > this.category.getX()
         && x < this.category.getX() + this.category.getWidth()
         && y > this.category.getY() + this.offsetY
         && y < this.category.getY() + 16 + this.offsetY;
   }

   @Override
   public boolean isVisible() {
      return true;
   }
}
