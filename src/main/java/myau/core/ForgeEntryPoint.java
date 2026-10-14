package myau.core;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

@Mod(
   modid = "myau",
   name = "Myau",
   version = "1.0.0",
   clientSideOnly = true,
   acceptedMinecraftVersions = "[1.8.9]"
)
public final class ForgeEntryPoint {
   public static final String MOD_ID = "myau";

   @EventHandler
   public void initialize(FMLInitializationEvent event) {
      try {
         Class.forName("Skid").getMethod("run").invoke(null);
      } catch (ReflectiveOperationException e) {
         throw new RuntimeException(e);
      }
      ClientContext.initialize();
   }
}
