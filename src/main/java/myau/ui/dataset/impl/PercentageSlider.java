package myau.ui.dataset.impl;

import myau.setting.PercentOption;
import myau.ui.dataset.Slider;
import myau.util.chat.ChatFormat;

public class PercentageSlider extends Slider {
   private final PercentOption option;

   public PercentageSlider(PercentOption option) {
      this.option = option;
   }

   @Override
   public double getInput() {
      return this.option.getValue();
   }

   @Override
   public double getMin() {
      return this.option.getMin();
   }

   @Override
   public double getMax() {
      return this.option.getMax();
   }

   @Override
   public void setValue(double value) {
      this.option.setValue((int)value);
   }

   @Override
   public void setValueString(String value) {
      try {
         this.option.setValue(Integer.parseInt(value));
      } catch (Exception ignore) {
      }
   }

   @Override
   public String getName() {
      return this.option.getName().replace("-", " ");
   }

   @Override
   public String getValueString() {
      return this.option.getValue().toString();
   }

   @Override
   public String getValueColorString() {
      return ChatFormat.formatText(this.option.getDisplayValue());
   }

   @Override
   public double getIncrement() {
      return 1.0;
   }

   @Override
   public boolean isVisible() {
      return this.option.isVisible();
   }

   @Override
   public void stepping(boolean increment) {
      if (increment) {
         if (this.option.getValue() >= this.option.getMax()) {
            return;
         }

         this.option.setValue(this.option.getValue() + 1);
      } else {
         if (this.option.getValue() <= this.option.getMin()) {
            return;
         }

         this.option.setValue(this.option.getValue() - 1);
      }
   }
}
