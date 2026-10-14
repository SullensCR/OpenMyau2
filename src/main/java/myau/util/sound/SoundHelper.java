package myau.util.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.util.ResourceLocation;

public class SoundHelper {
   private static final Minecraft minecraft = Minecraft.func_71410_x();

   public static void play(String soundName) {
      SoundHandler soundHandler = minecraft.func_147118_V();
      if (soundHandler != null) {
         PositionedSoundRecord sound = PositionedSoundRecord.func_147673_a(new ResourceLocation(soundName));
         soundHandler.func_147682_a(sound);
      }
   }
}
