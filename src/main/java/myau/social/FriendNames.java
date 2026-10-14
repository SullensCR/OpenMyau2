package myau.social;

import java.awt.Color;
import java.io.File;
import myau.util.chat.ChatFormat;

public class FriendNames extends NameList {
   public FriendNames() {
      super(configurationFile(), new Color(ChatFormat.DARK_GREEN.getRgb()));
   }

   private static File configurationFile() {
      return new File("./config/Myau/", "friends.txt");
   }
}
