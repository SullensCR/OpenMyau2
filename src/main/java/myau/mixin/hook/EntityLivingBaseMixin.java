package myau.mixin.hook;

import myau.core.ClientContext;
import myau.event.player.OnMoveFlying;
import myau.module.movement.Jesus;
import myau.rotation.RotationStore;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

@SideOnly(Side.CLIENT)
@Mixin({EntityLivingBase.class})
public abstract class EntityLivingBaseMixin extends EntityMixin {
   @ModifyVariable(
      method = {"jump"},
      at = @At("STORE"),
      ordinal = 0
   )
   private float modifyJumpYawRadians(float yawRadians) {
      return (Entity)(Object)this instanceof EntityPlayerSP && RotationStore.isActive() ? RotationStore.movementYaw() * (float) (Math.PI / 180.0) : yawRadians;
   }

   @Redirect(
      method = {"moveEntityWithHeading"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/EntityLivingBase;moveFlying(FFF)V"
      )
   )
   private void redirectMoveFlying(EntityLivingBase target, float strafe, float forward, float friction) {
      if ((Entity)(Object)this instanceof EntityPlayerSP && ClientContext.eventBus != null) {
         OnMoveFlying event = new OnMoveFlying(strafe, forward, friction);
         ClientContext.eventBus.post(event);
         strafe = event.getStrafe();
         forward = event.getForward();
         friction = event.getFriction();
         boolean rotationSpoofActive = RotationStore.isActive();
         float yaw = this.field_70177_z;
         if (rotationSpoofActive) {
            this.field_70177_z = RotationStore.movementYaw();
         }

         target.func_70060_a(strafe, forward, friction);
         if (rotationSpoofActive) {
            this.field_70177_z = yaw;
         }
      } else {
         target.func_70060_a(strafe, forward, friction);
      }
   }

   @ModifyVariable(
      method = {"moveEntityWithHeading"},
      name = {"f3"},
      at = @At("STORE")
   )
   private float modifyDepthStriderLevel(float depthStriderLevel) {
      if ((EntityLivingBase)(Object)this instanceof EntityPlayerSP && depthStriderLevel == EnchantmentHelper.func_180318_b((EntityLivingBase)(Object)this)) {
         if (ClientContext.moduleManager == null) {
            return depthStriderLevel;
         }

         Jesus jesus = (Jesus)ClientContext.moduleManager.modules.get(Jesus.class);
         if (jesus.isEnabled() && (!jesus.groundOnly.getValue() || this.field_70122_E)) {
            return Math.max(depthStriderLevel, jesus.speed.getValue());
         }
      }

      return depthStriderLevel;
   }
}
