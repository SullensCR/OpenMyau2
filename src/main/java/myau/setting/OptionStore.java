package myau.setting;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import myau.module.ClientModule;

public class OptionStore {
   public final LinkedHashMap<Class<?>, ArrayList<Option<?>>> settingsByModule = new LinkedHashMap<>();

   public Option<?> findSetting(ClientModule module, String name) {
      ArrayList<Option<?>> settings = this.settingsByModule.get(module.getClass());
      if (settings == null) {
         return null;
      } else {
         String normalizedName = name.replace("-", "");
         return settings.stream().filter(setting -> normalizedName.equalsIgnoreCase(setting.getName().replace("-", ""))).findFirst().orElse(null);
      }
   }
}
