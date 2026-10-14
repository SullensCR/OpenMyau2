package myau.social;

import java.awt.Color;
import java.io.File;
import myau.util.chat.ChatFormat;

public class EnemyNames extends NameList {
   public EnemyNames() {
      super(new File("./config/Myau/", "enemies.txt"), new Color(ChatFormat.DARK_RED.getRgb()));
   }
}
