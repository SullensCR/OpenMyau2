package myau.mixin.hook;

import myau.core.ClientContext;
import myau.event.player.OnSafeWalk;
import myau.event.player.OnVelocity;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SideOnly(Side.CLIENT)
@Mixin({Entity.class})
public abstract class EntityMixin {
   @Shadow
   public World field_70170_p;
   @Shadow
   public double field_70165_t;
   @Shadow
   public double field_70163_u;
   @Shadow
   public double field_70161_v;
   @Shadow
   public double field_70159_w;
   @Shadow
   public double field_70181_x;
   @Shadow
   public double field_70179_y;
   @Shadow
   public float field_70177_z;
   @Shadow
   public float field_70125_A;
   @Shadow
   public float field_70126_B;
   @Shadow
   public float field_70127_C;
   @Shadow
   public boolean field_70122_E;

   @Shadow
   public boolean func_70115_ae() {
      return false;
   }

   @Inject(
      method = {"setVelocity"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void beforeSetVelocity(double velocityX, double velocityY, double velocityZ, CallbackInfo callbackInfo) {
      if ((Entity)(Object)this instanceof EntityPlayerSP && ClientContext.eventBus != null) {
         OnVelocity event = new OnVelocity(velocityX, velocityY, velocityZ);
         ClientContext.eventBus.post(event);
         if (event.isCancelled()) {
            callbackInfo.cancel();
            this.field_70159_w = event.getVelocityX();
            this.field_70181_x = event.getVelocityY();
            this.field_70179_y = event.getVelocityZ();
         }
      }
   }

   @Inject(
      method = {"setAngles"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void beforeSetAngles(CallbackInfo callbackInfo) {
      if ((Entity)(Object)this instanceof EntityPlayerSP && ClientContext.rotationSmoother != null && ClientContext.rotationSmoother.isRequestActive()) {
         callbackInfo.cancel();
      }
   }

   @ModifyVariable(
      method = {"moveEntity"},
      ordinal = 0,
      at = @At("STORE"),
      name = {"flag"}
   )
   private boolean modifySafeWalkFlag(boolean safeWalk) {
      if ((Entity)(Object)this instanceof EntityPlayerSP && ClientContext.eventBus != null) {
         OnSafeWalk safeWalkEvent = new OnSafeWalk(safeWalk);
         ClientContext.eventBus.post(safeWalkEvent);
         return safeWalkEvent.isSafeWalk();
      } else {
         return safeWalk;
      }
   }
}
