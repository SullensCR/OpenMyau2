package myau.module.movement;

import myau.event.Listen;
import myau.event.lifecycle.OnPreTick;
import myau.mixin.accessor.EntityLivingBaseAccessor;
import myau.module.ClientModule;
import myau.setting.IntegerOption;
import net.minecraft.client.Minecraft;

public class NoJumpDelay extends ClientModule {
   private static final Minecraft minecraft = Minecraft.func_71410_x();
   public final IntegerOption delay = new IntegerOption("delay", 3, 0, 8);

   public NoJumpDelay() {
      super("NoJumpDelay", false, 0);
   }

   @Listen(
      priority = 100
   )
   public void onPreTick(OnPreTick event) {
      if (this.isEnabled()) {
         EntityLivingBaseAccessor player = (EntityLivingBaseAccessor)minecraft.field_71439_g;
         player.setJumpTicks(Math.min(player.getJumpTicks(), this.delay.getValue() + 1));
      }
   }

   @Override
   public String[] getHudInfo() {
      return new String[]{this.delay.getValue().toString()};
   }
}
