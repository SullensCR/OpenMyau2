package myau.mixin.hook;

import myau.core.ClientContext;
import myau.module.movement.Sprint;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@SideOnly(Side.CLIENT)
@Pseudo
@Mixin(
   targets = {"club.sk1er.patcher.util.fov.FovHandler"}
)
public abstract class FovHandlerMixin {
   @Redirect(
      method = {"fovChange"},
      remap = false,
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/player/EntityPlayer;func_70051_ag()Z",
         remap = false
      )
   )
   @Dynamic("Patcher")
   private boolean redirectSprintState(EntityPlayer player) {
      boolean sprinting = player.func_70051_ag();
      if (player instanceof EntityPlayerSP && ClientContext.moduleManager != null) {
         Sprint sprint = (Sprint)ClientContext.moduleManager.modules.get(Sprint.class);
         return sprint.isEnabled() && sprint.shouldCompensateFov(sprinting) ? true : sprinting;
      } else {
         return sprinting;
      }
   }
}
