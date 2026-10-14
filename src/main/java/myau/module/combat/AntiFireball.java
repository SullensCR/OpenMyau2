package myau.module.combat;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import myau.core.ClientContext;
import myau.event.Listen;
import myau.event.lifecycle.OnPreTick;
import myau.event.lifecycle.OnWorldLoad;
import myau.event.player.OnMovementInput;
import myau.event.player.OnRotationUpdate;
import myau.event.render.OnRenderWorld;
import myau.module.ClientModule;
import myau.module.render.HeadsUpDisplay;
import myau.rotation.RotationHelper;
import myau.rotation.RotationStore;
import myau.setting.BooleanOption;
import myau.setting.ChoiceOption;
import myau.setting.FloatOption;
import myau.setting.IntegerOption;
import myau.util.entity.EntityHelper;
import myau.util.item.ItemHelper;
import myau.util.movement.MovementHelper;
import myau.util.network.PacketHelper;
import myau.util.player.PlayerHelper;
import myau.util.render.RenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.client.C02PacketUseEntity.Action;
import net.minecraft.util.MathHelper;

public class AntiFireball extends ClientModule {
   private static final Minecraft minecraft = Minecraft.func_71410_x();
   private final ArrayList<EntityFireball> candidateFireballs = new ArrayList<>();
   private final ArrayList<EntityFireball> ignoredCloseFireballs = new ArrayList<>();
   private EntityFireball targetFireball;
   public final FloatOption range = new FloatOption("range", 5.0F, 3.0F, 8.0F);
   public final IntegerOption fov = new IntegerOption("fov", 360, 1, 360);
   public final BooleanOption rotations = new BooleanOption("rotations", true);
   public final BooleanOption swing = new BooleanOption("swing", true);
   public final ChoiceOption moveFix = new ChoiceOption("move-fix", 1, new String[]{"NONE", "SILENT", "STRICT"});
   public final ChoiceOption showTarget = new ChoiceOption("show-target", 0, new String[]{"NONE", "DEFAULT", "HUD"});

   public AntiFireball() {
      super("AntiFireball", false, 0);
   }

   private boolean isValidTarget(EntityFireball fireball) {
      if (fireball.func_174813_aQ().func_181656_b()) {
         return false;
      } else {
         return RotationHelper.distanceToEntity(fireball) > this.range.getValue().floatValue() + 3.0
            ? false
            : RotationHelper.entityAimError(fireball) <= this.fov.getValue().intValue();
      }
   }

   private void swing() {
      if (this.swing.getValue()) {
         minecraft.field_71439_g.func_71038_i();
      } else {
         PacketHelper.send(new C0APacketAnimation());
      }
   }

   @Listen
   public void onPreTick(OnPreTick event) {
      if (this.isEnabled()) {
         List<EntityFireball> loadedFireballs = minecraft.field_71441_e
            .field_72996_f
            .stream()
            .filter(entity -> entity instanceof EntityFireball)
            .map(entity -> (EntityFireball)entity)
            .collect(Collectors.toList());
         this.candidateFireballs.removeIf(fireballx -> !loadedFireballs.contains(fireballx));
         this.ignoredCloseFireballs.removeIf(fireballx -> !loadedFireballs.contains(fireballx));

         for (EntityFireball fireball : loadedFireballs) {
            if (!this.candidateFireballs.contains(fireball) && !this.ignoredCloseFireballs.contains(fireball)) {
               if (RotationHelper.distanceToEntity(fireball) > 3.0) {
                  this.candidateFireballs.add(fireball);
               } else {
                  this.ignoredCloseFireballs.add(fireball);
               }
            }
         }

         if (minecraft.field_71439_g.field_71075_bZ.field_75101_c) {
            this.targetFireball = null;
         } else {
            this.targetFireball = this.candidateFireballs
               .stream()
               .filter(this::isValidTarget)
               .min(Comparator.comparingDouble(RotationHelper::distanceToEntity))
               .orElse(null);
         }
      }
   }

   @Listen(
      priority = -100
   )
   public void onRotationUpdate(OnRotationUpdate event) {
      if (this.isEnabled() && EntityHelper.isLoaded(this.targetFireball)) {
         if (this.rotations.getValue() && !ItemHelper.isHoldingBlocks() && !ItemHelper.isHoldingBow() && !ItemHelper.isHoldingFireball()) {
            float previousYaw = event.getPreviousYaw();
            float previousPitch = event.getPreviousPitch();
            float[] targetRotation = RotationHelper.aimAtBounds(this.targetFireball.func_174813_aQ());
            float yaw = RotationHelper.snapToMouseSensitivity(RotationHelper.nearestEquivalentAngle(targetRotation[0], previousYaw), previousYaw);
            float pitch = RotationHelper.snapToMouseSensitivity(targetRotation[1], previousPitch);
            pitch = MathHelper.func_76131_a(pitch, -90.0F, 90.0F);
            event.setRotation(yaw, pitch, 0);
            if (this.moveFix.getValue() != 0) {
               event.setMovementYaw(yaw, 0);
            }
         }

         if (!ClientContext.packetActionTracker.sentUseEntity
            && !ClientContext.packetActionTracker.sentDigging
            && !ClientContext.packetActionTracker.sentBlockPlacement) {
            this.swing();
            if (RotationHelper.distanceToEntity(this.targetFireball) <= this.range.getValue().floatValue()) {
               PacketHelper.send(new C02PacketUseEntity(this.targetFireball, Action.ATTACK));
               PlayerHelper.attackLocally(this.targetFireball);
            }
         }
      }
   }

   @Listen
   public void onMovementInput(OnMovementInput event) {
      if (this.isEnabled()
         && this.moveFix.getValue() == 1
         && RotationStore.isActive()
         && RotationStore.movementPriority() == 0.0F
         && MovementHelper.hasMovementInput()) {
         MovementHelper.correctMovementForYaw(RotationStore.movementYaw());
      }
   }

   @Listen
   public void onRenderWorld(OnRenderWorld event) {
      if (this.isEnabled() && this.showTarget.getValue() != 0 && EntityHelper.isLoaded(this.targetFireball)) {
         Color color = Color.WHITE;
         switch (this.showTarget.getValue()) {
            case 1:
               double approachDotProduct = (this.targetFireball.field_70165_t - this.targetFireball.field_70142_S)
                     * (minecraft.field_71439_g.field_70165_t - this.targetFireball.field_70165_t)
                  + (this.targetFireball.field_70163_u - this.targetFireball.field_70137_T)
                     * (
                        minecraft.field_71439_g.field_70163_u
                           + minecraft.field_71439_g.func_70047_e()
                           - this.targetFireball.field_70163_u
                           - this.targetFireball.field_70131_O / 2.0
                     )
                  + (this.targetFireball.field_70161_v - this.targetFireball.field_70136_U)
                     * (minecraft.field_71439_g.field_70161_v - this.targetFireball.field_70161_v);
               color = new Color(approachDotProduct < 0.0 ? 16733525 : 5635925);
               break;
            case 2:
               color = ((HeadsUpDisplay)ClientContext.moduleManager.modules.get(HeadsUpDisplay.class)).getColor(System.currentTimeMillis());
         }

         RenderHelper.beginOverlay();
         RenderHelper.fillEntityBounds(this.targetFireball, color.getRed(), color.getGreen(), color.getBlue(), 63, 0.1F);
         RenderHelper.endOverlay();
      }
   }

   @Listen
   public void onWorldLoad(OnWorldLoad event) {
      this.candidateFireballs.clear();
      this.ignoredCloseFireballs.clear();
   }
}
