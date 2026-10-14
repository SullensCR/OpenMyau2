package myau.setting;

import com.google.gson.JsonObject;
import java.util.function.BooleanSupplier;

public class PercentOption extends Option<Integer> {
   private static final int MAX_PERCENT = 100;
   private final Integer min;
   private final Integer max;

   public PercentOption(String name, Integer defaultValue) {
      this(name, defaultValue, null);
   }

   public PercentOption(String name, Integer defaultValue, BooleanSupplier visibilityCondition) {
      this(name, defaultValue, 0, 100, visibilityCondition);
   }

   public PercentOption(String name, Integer defaultValue, Integer min, Integer max, BooleanSupplier visibilityCondition) {
      super(name, defaultValue, value -> value >= min && value <= max, visibilityCondition);
      this.min = min;
      this.max = max;
   }

   @Override
   public String getUsage() {
      return String.format("%d-%d%%", this.min, this.max);
   }

   @Override
   public String getDisplayValue() {
      return String.format("&b%d%%", this.getValue());
   }

   @Override
   public boolean parseValue(String input) {
      return this.setValue(Integer.parseInt(input.replace("%", "")));
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
