package myau.util.chat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Central chat prefix. All mod messages go through {@link #get()} so the
 *  .revertmyauchatcolor command can swap the default blue prefix for the
 *  rainbow one. Choice persists in ./config/Myau/chatcolor.json. */
public final class ChatPrefix {
   public static final String DEFAULT = myau.util.chat.ChatPrefix.get();
   public static final String RAINBOW = "&7[&cM&6y&ea&au&7] &f";

   private static final Path FILE = Paths.get("./config/Myau/chatcolor.json");
   private static boolean rainbow = false;

   static {
      load();
   }

   private ChatPrefix() {
   }

   public static String get() {
      return rainbow ? RAINBOW : DEFAULT;
   }

   public static boolean isRainbow() {
      return rainbow;
   }

   public static void setRainbow(boolean value) {
      rainbow = value;
      save();
   }

   public static void load() {
      if (!Files.isRegularFile(FILE)) {
         return;
      }
      try (Reader reader = new InputStreamReader(Files.newInputStream(FILE), StandardCharsets.UTF_8)) {
         JsonObject root = new JsonParser().parse(reader).getAsJsonObject();
         if (root.has("rainbow")) {
            rainbow = root.get("rainbow").getAsBoolean();
         }
      } catch (Exception ignored) {
      }
   }

   public static void save() {
      try {
         Files.createDirectories(FILE.getParent());
         JsonObject root = new JsonObject();
         root.addProperty("rainbow", rainbow);
         try (Writer writer = new OutputStreamWriter(Files.newOutputStream(FILE), StandardCharsets.UTF_8)) {
            writer.write(root.toString());
         }
      } catch (Exception ignored) {
      }
   }
}
