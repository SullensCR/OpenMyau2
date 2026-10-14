package myau.mixin.hook;

import myau.core.ClientContext;
import myau.event.player.OnLivingUpdate;
import myau.event.player.OnMovementInput;
import myau.event.player.OnPostPlayerUpdate;
import myau.event.player.OnPreWalkingUpdate;
import myau.event.player.OnRotationUpdate;
import myau.mixin.accessor.EntityLivingBaseAccessor;
import myau.module.combat.KillAura;
import myau.module.movement.NoSlow;
import myau.module.player.AntiDebuff;
import myau.rotation.RotationHelper;
import myau.rotation.RotationStore;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.potion.Potion;
import net.minecraft.util.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SideOnly(Side.CLIENT)
@Mixin({EntityPlayerSP.class})
public abstract class EntityPlayerSPMixin extends EntityPlayerMixin {
   @Unique
   private float spoofedYaw = Float.NaN;
   @Unique
   private float spoofedPitch = Float.NaN;
   @Unique
   private float originalYaw;
   @Unique
   private float originalPitch;
   @Shadow
   private float field_175164_bL;
   @Shadow
   private float field_175165_bM;
   @Shadow
   public float field_71154_f;
   @Shadow
   public float field_71163_h;

   @Inject(
      method = {"onUpdate"},
      at = {@At("HEAD")}
   )
   private void beforePlayerUpdate(CallbackInfo callbackInfo) {
      if (this.field_70170_p.func_175667_e(new BlockPos(this.field_70165_t, 0.0, this.field_70161_v)) && ClientContext.eventBus != null) {
         OnRotationUpdate event = new OnRotationUpdate(this.field_175164_bL, this.field_175165_bM, this.field_70177_z, this.field_70125_A);
         ClientContext.eventBus.post(event);
         RotationStore.update(
            event.isModified() && !this.func_70115_ae(), event.getYaw(), event.getPitch(), event.getMovementYaw(), event.getMovementPriority()
         );
         if (event.isModified()) {
            this.originalYaw = this.field_70177_z;
            this.originalPitch = this.field_70125_A;
            this.spoofedYaw = event.getYaw();
            this.spoofedPitch = event.getPitch();
         } else {
            this.originalYaw = Float.NaN;
            this.originalPitch = Float.NaN;
            this.spoofedYaw = Float.NaN;
            this.spoofedPitch = Float.NaN;
         }
      }
   }

   @Inject(
      method = {"onUpdate"},
      at = {@At("RETURN")}
   )
   private void afterPlayerUpdate(CallbackInfo callbackInfo) {
      if (this.field_70170_p.func_175667_e(new BlockPos(this.field_70165_t, 0.0, this.field_70161_v))) {
         if (!Float.isNaN(this.originalYaw) && !Float.isNaN(this.originalPitch)) {
            this.field_175164_bL = this.field_70177_z;
            this.field_175165_bM = this.field_70125_A;
            this.field_70177_z = RotationHelper.nearestEquivalentAngle(this.originalYaw, this.field_70177_z);
            this.field_70125_A = this.originalPitch;
            this.field_70126_B = this.field_70177_z;
            this.field_70127_C = this.field_70125_A;
            this.field_71163_h = this.field_70177_z - (this.field_71154_f - this.field_71163_h) * 2.0F;
            this.field_71154_f = this.field_70177_z;
         }

         if (ClientContext.eventBus != null) {
            ClientContext.eventBus.post(new OnPostPlayerUpdate());
         }
      }
   }

   @Redirect(
      method = {"onUpdate"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/entity/EntityPlayerSP;isRiding()Z"
      )
   )
   private boolean applySpoofedRotationBeforeRidingCheck(EntityPlayerSP player) {
      if (!Float.isNaN(this.spoofedYaw) && !Float.isNaN(this.spoofedPitch)) {
         this.field_70177_z = this.spoofedYaw;
         this.field_70125_A = this.spoofedPitch;
      }

      return player.func_70115_ae();
   }

   @Inject(
      method = {"onUpdate"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/entity/EntityPlayerSP;onUpdateWalkingPlayer()V"
      )}
   )
   private void beforeWalkingPlayerUpdate(CallbackInfo callbackInfo) {
      if (ClientContext.eventBus != null) {
         ClientContext.eventBus.post(new OnPreWalkingUpdate());
      }
   }

   @Inject(
      method = {"onLivingUpdate"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/entity/AbstractClientPlayer;onLivingUpdate()V"
      )}
   )
   private void beforeLivingUpdate(CallbackInfo callbackInfo) {
      if (ClientContext.eventBus != null) {
         ClientContext.eventBus.post(new OnLivingUpdate());
      }
   }

   @Inject(
      method = {"onLivingUpdate"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/util/MovementInput;updatePlayerMoveState()V",
         shift = At.Shift.AFTER
      )}
   )
   private void afterMovementInputUpdate(CallbackInfo callbackInfo) {
      if (ClientContext.eventBus != null) {
         ClientContext.eventBus.post(new OnMovementInput());
      }
   }

   @Redirect(
      method = {"onLivingUpdate"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/entity/EntityPlayerSP;isUsingItem()Z"
      )
   )
   private boolean redirectUsingItemForNoSlow(EntityPlayerSP player) {
      KillAura killAura = (KillAura)ClientContext.moduleManager.modules.get(KillAura.class);
      if (killAura.isAutoBlockActive() && killAura.autoBlockNoSlow.getValue()) {
         return false;
      } else {
         NoSlow noSlow = (NoSlow)ClientContext.moduleManager.modules.get(NoSlow.class);
         return noSlow.isEnabled() && noSlow.shouldSuppressItemSlowdown() ? false : player.func_71039_bw();
      }
   }

   @Redirect(
      method = {"onLivingUpdate"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/entity/EntityPlayerSP;isPotionActive(Lnet/minecraft/potion/Potion;)Z"
      )
   )
   private boolean redirectNauseaCheck(EntityPlayerSP player, Potion potion) {
      if (potion == Potion.field_76431_k && ClientContext.moduleManager != null) {
         AntiDebuff antiDebuff = (AntiDebuff)ClientContext.moduleManager.modules.get(AntiDebuff.class);
         if (antiDebuff.isEnabled() && antiDebuff.nausea.getValue()) {
            return false;
         }
      }

      return ((EntityLivingBaseAccessor)player).getActivePotionsMap().containsKey(potion.field_76415_H);
   }
}
