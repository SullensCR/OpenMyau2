package myau.module.combat;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Random;
import myau.event.Listen;
import myau.event.lifecycle.OnPreTick;
import myau.event.player.OnBlockReach;
import myau.event.player.OnEntityReach;
import myau.module.ClientModule;
import myau.setting.FloatOption;
import myau.setting.PercentOption;

public class Reach extends ClientModule {
   private static final DecimalFormat rangeFormat = new DecimalFormat("0.0#", new DecimalFormatSymbols(Locale.US));
   private final Random random = new Random();
   private boolean chancePassed = true;
   public final FloatOption range = new FloatOption("range", 3.1F, 3.0F, 6.0F);
   public final PercentOption chance = new PercentOption("chance", 100);

   public Reach() {
      super("Reach", false, 0);
   }

   private boolean shouldApplyReach() {
      return this.isEnabled() && this.chancePassed;
   }

   @Listen
   public void onEntityReach(OnEntityReach event) {
      if (this.shouldApplyReach()) {
         event.setReachDistance(this.range.getValue().doubleValue());
      }
   }

   @Listen
   public void onBlockReach(OnBlockReach event) {
      if (this.shouldApplyReach()) {
         event.setReachDistance(Math.max(event.getReachDistance(), this.range.getValue().doubleValue() + 0.5));
      }
   }

   @Listen
   public void onPreTick(OnPreTick event) {
      if (this.isEnabled()) {
         double difference = this.random.nextDouble() - this.chance.getValue().intValue() / 100.0;
         this.chancePassed = difference <= 0.0;
      }
   }

   @Override
   public String[] getHudInfo() {
      return new String[]{rangeFormat.format(this.range.getValue())};
   }
}
