package myau.mixin.hook;

import myau.core.ClientContext;
import myau.module.combat.KeepSprint;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

@SideOnly(Side.CLIENT)
@Mixin({EntityPlayer.class})
public abstract class EntityPlayerMixin extends EntityLivingBaseMixin {
   @ModifyConstant(
      method = {"attackTargetEntityWithCurrentItem"},
      constant = {@Constant(
         doubleValue = 0.6
      )}
   )
   private double modifyAttackSlowdown(double slowdownMultiplier) {
      if (ClientContext.moduleManager == null) {
         return slowdownMultiplier;
      } else {
         KeepSprint keepSprint = (KeepSprint)ClientContext.moduleManager.modules.get(KeepSprint.class);
         return keepSprint.isEnabled() && keepSprint.shouldPreserveSprint()
            ? slowdownMultiplier + (1.0 - slowdownMultiplier) * (1.0 - keepSprint.slowdown.getValue().doubleValue() / 100.0)
            : slowdownMultiplier;
      }
   }

   @Redirect(
      method = {"attackTargetEntityWithCurrentItem"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/player/EntityPlayer;setSprinting(Z)V"
      )
   )
   private void redirectAttackSprintReset(EntityPlayer player, boolean sprinting) {
      if (ClientContext.moduleManager != null) {
         KeepSprint keepSprint = (KeepSprint)ClientContext.moduleManager.modules.get(KeepSprint.class);
         if (!keepSprint.isEnabled() || !keepSprint.shouldPreserveSprint()) {
            player.func_70031_b(sprinting);
         }
      }
   }
}
