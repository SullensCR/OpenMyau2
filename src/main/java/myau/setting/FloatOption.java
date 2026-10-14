package myau.setting;

import com.google.gson.JsonObject;
import java.util.function.BooleanSupplier;

public class FloatOption extends Option<Float> {
   private final Float min;
   private final Float max;

   public FloatOption(String name, Float defaultValue, Float min, Float max) {
      this(name, defaultValue, min, max, null);
   }

   public FloatOption(String name, Float defaultValue, Float min, Float max, BooleanSupplier visibilityCondition) {
      super(name, defaultValue, value -> value >= min && value <= max, visibilityCondition);
      this.min = min;
      this.max = max;
   }

   @Override
   public String getUsage() {
      return String.format("%s-%s", this.min, this.max);
   }

   @Override
   public String getDisplayValue() {
      return String.format("&6%s", this.getValue());
   }

   @Override
   public boolean parseValue(String input) {
      return this.setValue(Float.parseFloat(input));
   }

   @Override
   public boolean readJson(JsonObject json) {
      return this.setValue(json.get(this.getName()).getAsNumber().floatValue());
   }

   @Override
   public void writeJson(JsonObject json) {
      json.addProperty(this.getName(), this.getValue());
   }
}
