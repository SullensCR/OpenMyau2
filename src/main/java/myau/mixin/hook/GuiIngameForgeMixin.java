package myau.mixin.hook;

import myau.core.ClientContext;
import myau.event.render.OnRenderOverlay;
import myau.module.misc.NickHider;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SideOnly(Side.CLIENT)
@Mixin(
   value = {GuiIngameForge.class},
   remap = false
)
public abstract class GuiIngameForgeMixin {
   @Inject(
      method = {"renderGameOverlay(F)V", "func_175180_a(F)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraftforge/client/GuiIngameForge;renderTitle(IIF)V",
         shift = At.Shift.AFTER,
         remap = false
      )},
      remap = false
   )
   private void afterRenderTitle(float partialTicks, CallbackInfo callbackInfo) {
      if (ClientContext.eventBus != null) {
         ClientContext.eventBus.post(new OnRenderOverlay(partialTicks));
      }
   }

   @Redirect(
      method = {"renderExperience(II)V"},
      at = @At(
         value = "FIELD",
         target = "Lnet/minecraft/client/entity/EntityPlayerSP;experience:F",
         remap = true
      ),
      remap = false
   )
   private float redirectRenderExperience(EntityPlayerSP player) {
      if (ClientContext.moduleManager == null) {
         return player.field_71106_cc;
      } else {
         NickHider nickHider = (NickHider)ClientContext.moduleManager.modules.get(NickHider.class);
         return nickHider.isEnabled() && nickHider.level.getValue() ? 0.0F : player.field_71106_cc;
      }
   }

   @Redirect(
      method = {"renderExperience(II)V"},
      at = @At(
         value = "FIELD",
         target = "Lnet/minecraft/client/entity/EntityPlayerSP;experienceLevel:I",
         remap = true
      ),
      remap = false
   )
   private int redirectExperienceLevel(EntityPlayerSP player) {
      if (ClientContext.moduleManager == null) {
         return player.field_71068_ca;
      } else {
         NickHider nickHider = (NickHider)ClientContext.moduleManager.modules.get(NickHider.class);
         return nickHider.isEnabled() && nickHider.level.getValue() ? 0 : player.field_71068_ca;
      }
   }
}
