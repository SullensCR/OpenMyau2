package myau.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import myau.core.ClientContext;
import myau.mixin.accessor.MinecraftAccessor;
import myau.module.ClientModule;
import myau.setting.Option;
import net.minecraft.client.Minecraft;

public class ConfigPreset {
   private static final Minecraft minecraft = Minecraft.func_71410_x();
   private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
   private final String profileName;
   private final File file;
   public static String activeProfileName;

   public ConfigPreset(String name) {
      this.profileName = name != null && !name.isEmpty() && !name.equals("!") ? name : "default";
      this.file = new File("./config/Myau/", String.format("%s.json", this.profileName));

      try {
         Files.createDirectories(this.file.toPath().getParent());
      } catch (Exception var3) {
         ((MinecraftAccessor)minecraft).getLogger().error(var3.getMessage());
      }
   }

   public String fileName() {
      return this.file.getName();
   }

   public boolean load() {
      try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(this.file.toPath()), StandardCharsets.UTF_8)) {
         JsonObject config = new JsonParser().parse(reader).getAsJsonObject();

         for (ClientModule module : ClientContext.moduleManager.modules.values()) {
            List<Option<?>> settings = ClientContext.settingManager.settingsByModule.get(module.getClass());
            JsonElement entry = config.get(module.getName());
            if (entry == null) {
               resetModule(module, settings);
            } else {
               JsonObject state = entry.getAsJsonObject();
               if (settings != null) {
                  for (Option<?> setting : settings) {
                     try {
                        if (!setting.readJson(state)) {
                           throw new IllegalArgumentException("Invalid setting value");
                        }
                     } catch (Exception var27) {
                        setting.reset();
                     }
                  }
               }

               try {
                  module.setHidden(state.get("hidden").getAsBoolean());
               } catch (Exception var26) {
                  module.resetHidden();
               }

               try {
                  module.setKeyCode(state.get("key").getAsInt());
               } catch (Exception var25) {
                  module.resetKeyCode();
               }

               try {
                  module.setEnabled(state.get("toggled").getAsBoolean());
               } catch (Exception var24) {
                  module.resetEnabled();
               }
            }
         }

         activeProfileName = this.profileName;
         return true;
      } catch (Exception var30) {
         return false;
      }
   }

   public boolean save() {
      try {
         JsonObject config = new JsonObject();

         for (ClientModule module : ClientContext.moduleManager.modules.values()) {
            JsonObject state = new JsonObject();
            state.addProperty("toggled", module.isEnabled());
            state.addProperty("key", module.getKeyCode());
            state.addProperty("hidden", module.isHidden());
            List<Option<?>> settings = ClientContext.settingManager.settingsByModule.get(module.getClass());
            if (settings != null) {
               for (Option<?> setting : settings) {
                  setting.writeJson(state);
               }
            }

            config.add(module.getName(), state);
         }

         try (OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(this.file.toPath()), StandardCharsets.UTF_8)) {
            GSON.toJson((JsonElement)config, writer);
         }

         activeProfileName = this.profileName;
         return true;
      } catch (Exception var18) {
         return false;
      }
   }

   private static void resetModule(ClientModule module, List<Option<?>> settings) {
      if (settings != null) {
         for (Option<?> setting : settings) {
            setting.reset();
         }
      }

      module.resetHidden();
      module.resetKeyCode();
      module.resetEnabled();
   }
}
