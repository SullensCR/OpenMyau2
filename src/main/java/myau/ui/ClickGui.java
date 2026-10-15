package myau.ui;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.Color;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import myau.core.ClientContext;
import myau.module.ClientModule;
import myau.ui.components.CategoryComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Mouse;

public class ClickGui extends GuiScreen {
   private static final String VERSION = resolveVersion();
   private static ClickGui instance;
   private final File configFile = new File("./config/Myau/", "clickgui.txt");
   private final ArrayList<CategoryComponent> categoryList;

   public ClickGui() {
      instance = this;
      List<ClientModule> combatModules = new ArrayList<>();
      List<ClientModule> movementModules = new ArrayList<>();
      List<ClientModule> renderModules = new ArrayList<>();
      List<ClientModule> playerModules = new ArrayList<>();
      List<ClientModule> worldModules = new ArrayList<>();
      List<ClientModule> miscModules = new ArrayList<>();
      if (ClientContext.moduleManager != null) {
         for (ClientModule module : ClientContext.moduleManager.modules.values()) {
            String packageName = module.getClass().getPackage().getName();
            if (packageName.endsWith(".combat")) {
               combatModules.add(module);
            } else if (packageName.endsWith(".movement")) {
               movementModules.add(module);
            } else if (packageName.endsWith(".render")) {
               renderModules.add(module);
            } else if (packageName.endsWith(".player")) {
               playerModules.add(module);
            } else if (packageName.endsWith(".world")) {
               worldModules.add(module);
            } else {
               miscModules.add(module);
            }
         }
      }

      Comparator<ClientModule> comparator = Comparator.comparing(module -> module.getName().toLowerCase());
      combatModules.sort(comparator);
      movementModules.sort(comparator);
      renderModules.sort(comparator);
      playerModules.sort(comparator);
      worldModules.sort(comparator);
      miscModules.sort(comparator);
      this.categoryList = new ArrayList<>();
      this.categoryList.add(new CategoryComponent("Combat", combatModules));
      this.categoryList.add(new CategoryComponent("Movement", movementModules));
      this.categoryList.add(new CategoryComponent("Render", renderModules));
      this.categoryList.add(new CategoryComponent("Player", playerModules));
      this.categoryList.add(new CategoryComponent("World", worldModules));
      this.categoryList.add(new CategoryComponent("Misc", miscModules));
      int columnOffset = 5;

      for (CategoryComponent category : this.categoryList) {
         category.setX(columnOffset);
         category.setY(5);
         category.setOpened(true);
         columnOffset += category.getWidth() + 8;
      }

      this.loadPositions();
   }

   private static String resolveVersion() {
      try (InputStream stream = ClickGui.class.getResourceAsStream("/version.json")) {
         if (stream != null) {
            JsonObject object = new JsonParser().parse(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            if (object.has("version")) {
               return object.get("version").getAsString();
            }
         }
      } catch (Exception ignore) {
      }

      return "dev";
   }

   public static ClickGui getInstance() {
      return instance;
   }

   @Override
   public void func_73866_w_() {
      super.func_73866_w_();
   }

   @Override
   public void func_73863_a(int x, int y, float partialTicks) {
      func_73734_a(0, 0, this.field_146294_l, this.field_146295_m, new Color(0, 0, 0, 100).getRGB());
      Minecraft.func_71410_x()
         .field_71466_p
         .func_175063_a(
            "Myau " + VERSION,
            4.0F,
            (float)(this.field_146295_m - 3 - Minecraft.func_71410_x().field_71466_p.field_78288_b * 2),
            new Color(60, 162, 253).getRGB()
         );
      Minecraft.func_71410_x()
         .field_71466_p
         .func_175063_a(
            "dev, ksyz",
            4.0F,
            (float)(this.field_146295_m - 3 - Minecraft.func_71410_x().field_71466_p.field_78288_b),
            new Color(60, 162, 253).getRGB()
         );

      for (CategoryComponent category : this.categoryList) {
         category.render(Minecraft.func_71410_x().field_71466_p);
         category.handleDrag(x, y);

         for (Component module : category.getModules()) {
            module.update(x, y);
         }
      }

      int wheel = Mouse.getDWheel();
      if (wheel != 0) {
         int scrollDir = wheel > 0 ? 1 : -1;

         for (CategoryComponent category : this.categoryList) {
            category.onScroll(x, y, scrollDir);
         }
      }
   }

   @Override
   public void func_73864_a(int x, int y, int mouseButton) {
      Iterator<CategoryComponent> btnCat = this.categoryList.iterator();

      while (true) {
         CategoryComponent category;
         do {
            do {
               if (!btnCat.hasNext()) {
                  return;
               }

               category = btnCat.next();
               if (category.insideArea(x, y) && !category.isHovered(x, y) && !category.mousePressed(x, y) && mouseButton == 0) {
                  category.mousePressed(true);
                  category.xx = x - category.getX();
                  category.yy = y - category.getY();
               }

               if (category.mousePressed(x, y) && mouseButton == 0) {
                  category.setOpened(!category.isOpened());
               }

               if (category.isHovered(x, y) && mouseButton == 0) {
                  category.setPin(!category.isPin());
               }
            } while (!category.isOpened());
         } while (category.getModules().isEmpty());

         for (Component component : category.getModules()) {
            component.mouseDown(x, y, mouseButton);
         }
      }
   }

   @Override
   public void func_146286_b(int x, int y, int mouseButton) {
      for (CategoryComponent category : this.categoryList) {
         if (mouseButton == 0) {
            category.mousePressed(false);
         }
      }

      Iterator<CategoryComponent> iterator = this.categoryList.iterator();

      while (true) {
         CategoryComponent category;
         do {
            do {
               if (!iterator.hasNext()) {
                  return;
               }

               category = iterator.next();
            } while (!category.isOpened());
         } while (category.getModules().isEmpty());

         for (Component component : category.getModules()) {
            component.mouseReleased(x, y, mouseButton);
         }
      }
   }

   @Override
   public void func_73869_a(char typedChar, int key) {
      if (key == 1) {
         Minecraft.func_71410_x().func_147108_a(null);
      } else {
         Iterator<CategoryComponent> btnCat = this.categoryList.iterator();

         while (true) {
            CategoryComponent category;
            do {
               do {
                  if (!btnCat.hasNext()) {
                     return;
                  }

                  category = btnCat.next();
               } while (!category.isOpened());
            } while (category.getModules().isEmpty());

            for (Component component : category.getModules()) {
               component.keyTyped(typedChar, key);
            }
         }
      }
   }

   @Override
   public void func_146281_b() {
      this.savePositions();
   }

   @Override
   public boolean func_73868_f() {
      return false;
   }

   private void savePositions() {
      JsonObject json = new JsonObject();

      for (CategoryComponent cat : this.categoryList) {
         JsonObject pos = new JsonObject();
         pos.addProperty("x", cat.getX());
         pos.addProperty("y", cat.getY());
         pos.addProperty("open", cat.isOpened());
         json.add(cat.getName(), pos);
      }

      try (FileWriter writer = new FileWriter(this.configFile)) {
         new GsonBuilder().setPrettyPrinting().create().toJson(json, writer);
      } catch (IOException exception) {
         exception.printStackTrace();
      }
   }

   private void loadPositions() {
      if (this.configFile.exists()) {
         try (FileReader reader = new FileReader(this.configFile)) {
            JsonObject json = new JsonParser().parse(reader).getAsJsonObject();

            for (CategoryComponent cat : this.categoryList) {
               if (json.has(cat.getName())) {
                  JsonObject pos = json.getAsJsonObject(cat.getName());
                  cat.setX(pos.get("x").getAsInt());
                  cat.setY(pos.get("y").getAsInt());
                  cat.setOpened(pos.get("open").getAsBoolean());
               }
            }
         } catch (IOException exception) {
            exception.printStackTrace();
         }
      }
   }
}
