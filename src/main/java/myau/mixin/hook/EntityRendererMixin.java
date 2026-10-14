package myau.mixin.hook;

import java.util.List;
import myau.core.ClientContext;
import myau.event.player.OnBlockReach;
import myau.event.player.OnEntityReach;
import myau.mixin.accessor.EntityLivingBaseAccessor;
import myau.mixin.accessor.EntityPlayerAccessor;
import myau.module.combat.KillAura;
import myau.module.player.AntiDebuff;
import myau.module.player.GhostHand;
import myau.module.render.NoHurtCam;
import myau.module.render.ViewClip;
import myau.module.world.Scaffold;
import myau.util.MutableRef;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SideOnly(Side.CLIENT)
@Mixin({EntityRenderer.class})
public abstract class EntityRendererMixin {
   @Unique
   private MutableRef<Integer> spoofedHotbarSlot = null;
   @Unique
   private MutableRef<ItemStack> previousItemInUse = null;
   @Unique
   private MutableRef<Integer> previousItemUseCount = null;
   @Shadow
   private Minecraft field_78531_r;
   @Shadow
   private float field_78490_B;

   @Inject(
      method = {"updateCameraAndRender"},
      at = {@At("HEAD")}
   )
   private void beforeUpdateCameraAndRender(float partialTicks, long nanoTime, CallbackInfo callbackInfo) {
      if (this.field_78531_r.field_71439_g != null) {
         Scaffold scaffold = (Scaffold)ClientContext.moduleManager.modules.get(Scaffold.class);
         if (scaffold.isEnabled() && scaffold.itemSpoof.getValue()) {
            int spoofedSlot = scaffold.getPreviousHotbarSlot();
            if (spoofedSlot >= 0) {
               this.spoofedHotbarSlot = new MutableRef<>(this.field_78531_r.field_71439_g.field_71071_by.field_70461_c);
               this.field_78531_r.field_71439_g.field_71071_by.field_70461_c = spoofedSlot;
            }
         }

         KillAura killAura = (KillAura)ClientContext.moduleManager.modules.get(KillAura.class);
         if (killAura.isEnabled() && killAura.shouldRenderBlocking()) {
            this.previousItemInUse = new MutableRef<>(((EntityPlayerAccessor)this.field_78531_r.field_71439_g).getItemInUse());
            ((EntityPlayerAccessor)this.field_78531_r.field_71439_g).setItemInUse(this.field_78531_r.field_71439_g.field_71071_by.func_70448_g());
            this.previousItemUseCount = new MutableRef<>(((EntityPlayerAccessor)this.field_78531_r.field_71439_g).getItemInUseCount());
            ((EntityPlayerAccessor)this.field_78531_r.field_71439_g).setItemInUseCount(69000);
         }
      }
   }

   @Inject(
      method = {"updateCameraAndRender"},
      at = {@At("RETURN")}
   )
   private void afterUpdateCameraAndRender(float partialTicks, long nanoTime, CallbackInfo callbackInfo) {
      if (this.spoofedHotbarSlot != null) {
         this.field_78531_r.field_71439_g.field_71071_by.field_70461_c = this.spoofedHotbarSlot.value;
         this.spoofedHotbarSlot = null;
      }

      if (this.previousItemInUse != null) {
         ((EntityPlayerAccessor)this.field_78531_r.field_71439_g).setItemInUse(this.previousItemInUse.value);
         this.previousItemInUse = null;
      }

      if (this.previousItemUseCount != null) {
         ((EntityPlayerAccessor)this.field_78531_r.field_71439_g).setItemInUseCount(this.previousItemUseCount.value);
         this.previousItemUseCount = null;
      }
   }

   @Inject(
      method = {"updateRenderer"},
      at = {@At("HEAD")}
   )
   private void beforeUpdateRenderer(CallbackInfo callbackInfo) {
      Scaffold scaffold = (Scaffold)ClientContext.moduleManager.modules.get(Scaffold.class);
      if (scaffold.isEnabled() && scaffold.itemSpoof.getValue()) {
         int spoofedSlot = scaffold.getPreviousHotbarSlot();
         if (spoofedSlot >= 0) {
            this.spoofedHotbarSlot = new MutableRef<>(this.field_78531_r.field_71439_g.field_71071_by.field_70461_c);
            this.field_78531_r.field_71439_g.field_71071_by.field_70461_c = spoofedSlot;
         }
      }
   }

   @Inject(
      method = {"updateRenderer"},
      at = {@At("RETURN")}
   )
   private void afterUpdateRenderer(CallbackInfo callbackInfo) {
      if (this.spoofedHotbarSlot != null) {
         this.field_78531_r.field_71439_g.field_71071_by.field_70461_c = this.spoofedHotbarSlot.value;
         this.spoofedHotbarSlot = null;
      }
   }

   @ModifyConstant(
      method = {"hurtCameraEffect"},
      constant = {@Constant(
         floatValue = 14.0F,
         ordinal = 0
      )}
   )
   private float modifyHurtCameraStrength(float strength) {
      if (ClientContext.moduleManager == null) {
         return strength;
      } else {
         NoHurtCam noHurtCam = (NoHurtCam)ClientContext.moduleManager.modules.get(NoHurtCam.class);
         return noHurtCam.isEnabled() ? strength * noHurtCam.multiplier.getValue().intValue() / 100.0F : strength;
      }
   }

   @ModifyConstant(
      method = {"getMouseOver"},
      constant = {@Constant(
         doubleValue = 3.0,
         ordinal = 1
      )}
   )
   private double modifyEntityReach(double reachDistance) {
      if (ClientContext.eventBus != null) {
         OnEntityReach event = new OnEntityReach(reachDistance);
         ClientContext.eventBus.post(event);
         return event.getReachDistance();
      } else {
         return reachDistance;
      }
   }

   @ModifyVariable(
      method = {"getMouseOver"},
      at = @At("STORE"),
      name = {"d0"}
   )
   private double modifyBlockReach(double reachDistance) {
      if (ClientContext.eventBus != null) {
         OnBlockReach blockReachEvent = new OnBlockReach(reachDistance);
         ClientContext.eventBus.post(blockReachEvent);
         return blockReachEvent.getReachDistance();
      } else {
         return reachDistance;
      }
   }

   @Redirect(
      method = {"getMouseOver"},
      at = @At(
         value = "INVOKE",
         target = "Ljava/util/List;size()I",
         ordinal = 0,
         remap = false
      )
   )
   private int filterMouseOverEntities(List<Entity> entities) {
      if (ClientContext.moduleManager != null) {
         GhostHand ghostHand = (GhostHand)ClientContext.moduleManager.modules.get(GhostHand.class);
         if (ghostHand.isEnabled()) {
            entities.removeIf(ghostHand::shouldIgnoreEntity);
         }
      }

      return entities.size();
   }

   @Redirect(
      method = {"orientCamera"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/util/Vec3;distanceTo(Lnet/minecraft/util/Vec3;)D"
      )
   )
   private double redirectOrientCamera(Vec3 cameraPosition, Vec3 collisionPosition) {
      if (ClientContext.moduleManager == null) {
         return cameraPosition.func_72438_d(collisionPosition);
      } else {
         ViewClip viewClip = (ViewClip)ClientContext.moduleManager.modules.get(ViewClip.class);
         return viewClip.isEnabled() ? this.field_78490_B : cameraPosition.func_72438_d(collisionPosition);
      }
   }

   @Redirect(
      method = {"setupFog"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/block/Block;getMaterial()Lnet/minecraft/block/material/Material;"
      )
   )
   private Material redirectCameraBlockMaterial(Block block) {
      if (ClientContext.moduleManager == null) {
         return block.func_149688_o();
      } else {
         ViewClip viewClip = (ViewClip)ClientContext.moduleManager.modules.get(ViewClip.class);
         return viewClip.isEnabled() ? Material.field_151579_a : block.func_149688_o();
      }
   }

   @Redirect(
      method = {"updateFogColor"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/EntityLivingBase;isPotionActive(Lnet/minecraft/potion/Potion;)Z"
      )
   )
   private boolean redirectBlindnessForFogColor(EntityLivingBase target, Potion potion) {
      if (potion == Potion.field_76440_q && ClientContext.moduleManager != null) {
         AntiDebuff antiDebuff = (AntiDebuff)ClientContext.moduleManager.modules.get(AntiDebuff.class);
         if (antiDebuff.isEnabled() && antiDebuff.blindness.getValue()) {
            return false;
         }
      }

      return ((EntityLivingBaseAccessor)target).getActivePotionsMap().containsKey(potion.field_76415_H);
   }

   @Redirect(
      method = {"setupFog"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/EntityLivingBase;isPotionActive(Lnet/minecraft/potion/Potion;)Z"
      )
   )
   private boolean redirectBlindnessForFogSetup(EntityLivingBase livingEntity, Potion potion) {
      if (potion == Potion.field_76440_q && ClientContext.moduleManager != null) {
         AntiDebuff antiDebuff = (AntiDebuff)ClientContext.moduleManager.modules.get(AntiDebuff.class);
         if (antiDebuff.isEnabled() && antiDebuff.blindness.getValue()) {
            return false;
         }
      }

      return ((EntityLivingBaseAccessor)livingEntity).getActivePotionsMap().containsKey(potion.field_76415_H);
   }

   @Redirect(
      method = {"setupCameraTransform"},
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
