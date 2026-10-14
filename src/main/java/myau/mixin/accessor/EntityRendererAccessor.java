package myau.mixin.accessor;

import net.minecraft.client.renderer.EntityRenderer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@SideOnly(Side.CLIENT)
@Mixin({EntityRenderer.class})
public interface EntityRendererAccessor {
   @Invoker
   void callSetupCameraTransform(float var1, int var2);
}
