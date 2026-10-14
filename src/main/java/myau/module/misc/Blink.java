package myau.module.misc;

import myau.core.ClientContext;
import myau.event.Listen;
import myau.event.lifecycle.OnPostTick;
import myau.event.lifecycle.OnWorldLoad;
import myau.module.ClientModule;
import myau.network.OutboundQueueOwner;
import myau.setting.ChoiceOption;
import myau.setting.IntegerOption;

public class Blink extends ClientModule {
   public final ChoiceOption mode = new ChoiceOption("mode", 0, new String[]{"DEFAULT", "PULSE"});
   public final IntegerOption ticks = new IntegerOption("ticks", 200, 0, 12000);

   public Blink() {
      super("Blink", false, 0);
   }

   @Listen(
      priority = -100
   )
   public void onPostTick(OnPostTick event) {
      boolean enabled = this.isEnabled();
      if (enabled) {
         if (!ClientContext.outgoingPacketBuffer.owner().equals(OutboundQueueOwner.BLINK)) {
            this.setEnabled(false);
         } else if (this.ticks.getValue() > 0) {
            if (ClientContext.outgoingPacketBuffer.movementPacketCount() - this.ticks.getValue().intValue() > 0L) {
               switch (this.mode.getValue()) {
                  case 0:
                     this.setEnabled(false);
                     break;
                  case 1:
                     ClientContext.outgoingPacketBuffer.setEnabled(false, OutboundQueueOwner.BLINK);
                     ClientContext.outgoingPacketBuffer.setEnabled(true, OutboundQueueOwner.BLINK);
               }
            }
         }
      }
   }

   @Listen
   public void onWorldLoad(OnWorldLoad event) {
      this.setEnabled(false);
   }

   @Override
   public void onEnable() {
      ClientContext.outgoingPacketBuffer.setEnabled(false, ClientContext.outgoingPacketBuffer.owner());
      ClientContext.outgoingPacketBuffer.setEnabled(true, OutboundQueueOwner.BLINK);
   }

   @Override
   public void onDisable() {
      ClientContext.outgoingPacketBuffer.setEnabled(false, OutboundQueueOwner.BLINK);
   }
}
