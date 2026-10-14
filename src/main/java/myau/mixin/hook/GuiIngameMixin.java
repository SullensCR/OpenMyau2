package myau.mixin.hook;

import myau.core.ClientContext;
import myau.module.world.Scaffold;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@SideOnly(Side.CLIENT)
@Mixin({GuiIngame.class})
public abstract class GuiIngameMixin {
   @Redirect(
      method = {"updateTick"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/player/InventoryPlayer;getCurrentItem()Lnet/minecraft/item/ItemStack;"
      )
   )
   private ItemStack redirectCurrentHotbarItem(InventoryPlayer inventoryPlayer) {
      if (ClientContext.moduleManager == null) {
         return inventoryPlayer.func_70448_g();
      } else {
         Scaffold scaffold = (Scaffold)ClientContext.moduleManager.modules.get(Scaffold.class);
         if (scaffold != null && scaffold.isEnabled() && scaffold.itemSpoof.getValue()) {
            int spoofedSlot = scaffold.getPreviousHotbarSlot();
            if (spoofedSlot >= 0) {
               return inventoryPlayer.func_70301_a(spoofedSlot);
            }
         }

         return inventoryPlayer.func_70448_g();
      }
   }
}
