package myau.mixin.hook;

import java.nio.IntBuffer;
import myau.core.ClientContext;
import myau.module.render.Xray;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@SideOnly(Side.CLIENT)
@Mixin({WorldRenderer.class})
public abstract class WorldRendererMixin {
   @Redirect(
      method = {"putColorMultiplier"},
      at = @At(
         value = "INVOKE",
         target = "Ljava/nio/IntBuffer;put(II)Ljava/nio/IntBuffer;",
         remap = false
      )
   )
   private IntBuffer redirectColorBufferWrite(IntBuffer intBuffer, int index, int packedColor) {
      if (ClientContext.moduleManager == null) {
         return intBuffer.put(index, packedColor);
      } else {
         Xray xray = (Xray)ClientContext.moduleManager.modules.get(Xray.class);
         return xray.isEnabled()
            ? intBuffer.put(index, packedColor & 16777215 | (int)(xray.opacity.getValue().intValue() * 255.0F / 100.0F) << 24)
            : intBuffer.put(index, packedColor);
      }
   }
}
