package myau.ui.dataset.impl;

import myau.setting.FloatOption;
import myau.ui.dataset.Slider;
import myau.util.chat.ChatFormat;

public class FloatSlider extends Slider {
   private final FloatOption option;

   public FloatSlider(FloatOption option) {
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
      this.option.setValue((float)value);
   }

   @Override
   public void setValueString(String value) {
      try {
         this.option.setValue(Float.parseFloat(value));
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
      return 0.1;
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

         this.option.setValue(Math.round(this.option.getValue() * 10.0F + 1.0F) / 10.0F);
      } else {
         if (this.option.getValue() <= this.option.getMin()) {
            return;
         }

         this.option.setValue(Math.round(this.option.getValue() * 10.0F - 1.0F) / 10.0F);
      }
   }
}
