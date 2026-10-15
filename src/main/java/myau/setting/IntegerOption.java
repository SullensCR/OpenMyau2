package myau.setting;

import com.google.gson.JsonObject;
import java.util.function.BooleanSupplier;

public class IntegerOption extends Option<Integer> {
   private final Integer min;
   private final Integer max;

   public IntegerOption(String name, Integer defaultValue, Integer min, Integer max) {
      this(name, defaultValue, min, max, null);
   }

   public IntegerOption(String name, Integer defaultValue, Integer min, Integer max, BooleanSupplier visibilityCondition) {
      super(name, defaultValue, value -> value >= min && value <= max, visibilityCondition);
      this.min = min;
      this.max = max;
   }

   public Integer getMin() {
      return this.min;
   }

   public Integer getMax() {
      return this.max;
   }

   @Override
   public String getUsage() {
      return String.format("%d-%d", this.min, this.max);
   }

   @Override
   public String getDisplayValue() {
      return String.format("&e%s", this.getValue());
   }

   @Override
   public boolean parseValue(String input) {
      return this.setValue(Integer.parseInt(input));
   }

   @Override
   public boolean readJson(JsonObject json) {
      return this.setValue(json.get(this.getName()).getAsNumber().intValue());
   }

   @Override
   public void writeJson(JsonObject json) {
      json.addProperty(this.getName(), this.getValue());
   }
}
