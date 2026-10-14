package myau.setting;

import com.google.gson.JsonObject;
import java.util.function.BooleanSupplier;

public class BooleanOption extends Option<Boolean> {
   public BooleanOption(String name, Boolean defaultValue) {
      this(name, defaultValue, null);
   }

   public BooleanOption(String name, Boolean defaultValue, BooleanSupplier visibilityCondition) {
      super(name, defaultValue, visibilityCondition);
   }

   @Override
   public String getUsage() {
      return "true/false";
   }

   @Override
   public String getDisplayValue() {
      return this.getValue() ? "&atrue" : "&cfalse";
   }

   @Override
   public boolean parseValue(String input) {
      if (input == null) {
         return this.setValue(!this.getValue());
      } else if (input.equalsIgnoreCase("true") || input.equalsIgnoreCase("on") || input.equalsIgnoreCase("1")) {
         return this.setValue(true);
      } else {
         return !input.equalsIgnoreCase("false") && !input.equalsIgnoreCase("off") && !input.equalsIgnoreCase("0") ? false : this.setValue(false);
      }
   }

   @Override
   public boolean readJson(JsonObject json) {
      return this.setValue(json.get(this.getName()).getAsBoolean());
   }

   @Override
   public void writeJson(JsonObject json) {
      json.addProperty(this.getName(), this.getValue());
   }
}
