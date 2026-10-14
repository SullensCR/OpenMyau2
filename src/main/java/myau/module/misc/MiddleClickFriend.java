package myau.module.misc;

import myau.core.ClientContext;
import myau.event.Listen;
import myau.event.input.OnKeyPress;
import myau.module.ClientModule;
import myau.util.chat.ChatHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;

public class MiddleClickFriend extends ClientModule {
   private static final Minecraft minecraft = Minecraft.func_71410_x();
   private static final int MIDDLE_MOUSE_KEY = -98;

   public MiddleClickFriend() {
      super("MCF", false, 0, true);
   }

   @Listen
   public void onKeyPress(OnKeyPress event) {
      if (this.isEnabled() && event.getKeyCode() == -98) {
         if (minecraft.field_71476_x != null && minecraft.field_71476_x.field_72313_a == MovingObjectType.ENTITY) {
            if (minecraft.field_71476_x.field_72308_g instanceof EntityPlayer) {
               String playerName = minecraft.field_71476_x.field_72308_g.func_70005_c_();
               if (ClientContext.friendList.contains(playerName)) {
                  ClientContext.friendList.remove(playerName);
                  ChatHelper.addFormattedMessage(String.format("%sRemoved &o%s&r from your friend list&r", myau.util.chat.ChatPrefix.get(), playerName));
               } else {
                  ClientContext.friendList.add(playerName);
                  ChatHelper.addFormattedMessage(String.format("%sAdded &o%s&r to your friend list&r", myau.util.chat.ChatPrefix.get(), playerName));
               }
            }
         }
      }
   }
}
