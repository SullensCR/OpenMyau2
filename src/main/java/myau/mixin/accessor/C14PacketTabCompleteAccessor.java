package myau.mixin.accessor;

import net.minecraft.network.play.client.C14PacketTabComplete;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@SideOnly(Side.CLIENT)
@Mixin({C14PacketTabComplete.class})
public interface C14PacketTabCompleteAccessor {
   @Accessor("message")
   void setMessage(String var1);
}
