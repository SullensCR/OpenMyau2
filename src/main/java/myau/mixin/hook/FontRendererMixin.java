package myau.mixin.hook;

import myau.core.ClientContext;
import myau.module.misc.AntiObfuscate;
import myau.module.misc.NickHider;
import net.minecraft.client.gui.FontRenderer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

@SideOnly(Side.CLIENT)
@Mixin({FontRenderer.class})
public abstract class FontRendererMixin {
   @ModifyVariable(
      method = {"renderString"},
      at = @At("HEAD"),
      ordinal = 0,
      argsOnly = true
   )
   private String modifyRenderString(String text) {
      if (ClientContext.moduleManager == null) {
         return text;
      } else {
         AntiObfuscate antiObfuscate = (AntiObfuscate)ClientContext.moduleManager.modules.get(AntiObfuscate.class);
         if (antiObfuscate != null && antiObfuscate.isEnabled() && text != null) {
            text = antiObfuscate.formatText(text);
         }

         NickHider nickHider = (NickHider)ClientContext.moduleManager.modules.get(NickHider.class);
         return nickHider != null && nickHider.isEnabled() ? nickHider.formatText(text) : text;
      }
   }

   @ModifyVariable(
      method = {"getStringWidth"},
      at = @At("HEAD"),
      ordinal = 0,
      argsOnly = true
   )
   private String modifyGetStringWidth(String text) {
      if (ClientContext.moduleManager == null) {
         return text;
      } else {
         AntiObfuscate antiObfuscate = (AntiObfuscate)ClientContext.moduleManager.modules.get(AntiObfuscate.class);
         if (antiObfuscate != null && antiObfuscate.isEnabled() && text != null) {
            text = antiObfuscate.formatText(text);
         }

         NickHider nickHider = (NickHider)ClientContext.moduleManager.modules.get(NickHider.class);
         return nickHider != null && nickHider.isEnabled() ? nickHider.formatText(text) : text;
      }
   }

   @Redirect(
      method = {"getStringWidth"},
      at = @At(
         value = "INVOKE",
         target = "Ljava/lang/String;charAt(I)C",
         ordinal = 1
      )
   )
   private char redirectFormattingCodeCharacter(String text, int index) {
      char character = text.charAt(index);
      return character != '0'
            && character != '1'
            && character != '2'
            && character != '3'
            && character != '4'
            && character != '5'
            && character != '6'
            && character != '7'
            && character != '8'
            && character != '9'
            && character != 'a'
            && character != 'A'
            && character != 'b'
            && character != 'B'
            && character != 'c'
            && character != 'C'
            && character != 'd'
            && character != 'D'
            && character != 'e'
            && character != 'E'
            && character != 'f'
            && character != 'F'
         ? character
         : 'r';
   }
}
