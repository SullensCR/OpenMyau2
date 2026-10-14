package myau.module.world;

import java.util.ArrayList;
import java.util.Comparator;
import myau.core.ClientContext;
import myau.event.Listen;
import myau.event.input.OnBlockClick;
import myau.event.input.OnHotbarChange;
import myau.event.input.OnLeftClick;
import myau.event.input.OnRightClick;
import myau.event.player.OnLivingUpdate;
import myau.event.player.OnMoveFlying;
import myau.event.player.OnMovementInput;
import myau.event.player.OnRotationUpdate;
import myau.event.player.OnSafeWalk;
import myau.event.render.OnRenderOverlay;
import myau.module.ClientModule;
import myau.module.movement.LongJump;
import myau.module.render.HeadsUpDisplay;
import myau.rotation.RotationHelper;
import myau.rotation.RotationStore;
import myau.setting.BooleanOption;
import myau.setting.ChoiceOption;
import myau.setting.PercentOption;
import myau.util.item.ItemHelper;
import myau.util.math.RandomHelper;
import myau.util.movement.MovementHelper;
import myau.util.network.PacketHelper;
import myau.util.player.PlayerHelper;
import myau.util.world.BlockHelper;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.potion.Potion;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.util.Vec3i;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.world.WorldSettings.GameType;

public class Scaffold extends ClientModule {
   private static final Minecraft minecraft = Minecraft.func_71410_x();
   private static final double[] FACE_SAMPLE_OFFSETS = new double[]{
      0.03125, 0.09375, 0.15625, 0.21875, 0.28125, 0.34375, 0.40625, 0.46875, 0.53125, 0.59375, 0.65625, 0.71875, 0.78125, 0.84375, 0.90625, 0.96875
   };
   private int placementDelayTicks = 0;
   private int previousHotbarSlot = -1;
   private int remainingStackBlocks = -1;
   private float placementYaw = -180.0F;
   private float placementPitch = 0.0F;
   private boolean hasPlacementRotation = false;
   private float tickPlacementYaw;
   private float tickPlacementPitch;
   private int towerPhase = 0;
   private int towerCycleCount = 0;
   private int keepYState = 0;
   private int keepYLevel = 256;
   private boolean keepYRecoveryPlacement = false;
   private boolean tellyRotationActive = false;
   private EnumFacing pendingPlacementFace = null;
   public final ChoiceOption rotations = new ChoiceOption("rotations", 2, new String[]{"NONE", "DEFAULT", "BACKWARDS", "SIDEWAYS"});
   public final ChoiceOption moveFix = new ChoiceOption("move-fix", 1, new String[]{"NONE", "SILENT"});
   public final ChoiceOption sprint = new ChoiceOption("sprint", 0, new String[]{"NONE", "VANILLA"});
   public final PercentOption groundMotion = new PercentOption("ground-motion", 100);
   public final PercentOption airMotion = new PercentOption("air-motion", 100);
   public final PercentOption speedMotion = new PercentOption("speed-motion", 100);
   public final ChoiceOption tower = new ChoiceOption("tower", 0, new String[]{"NONE", "VANILLA", "EXTRA", "TELLY"});
   public final ChoiceOption keepY = new ChoiceOption("keep-y", 0, new String[]{"NONE", "VANILLA", "EXTRA", "TELLY"});
   public final BooleanOption keepYOnPress = new BooleanOption("keep-y-on-press", false, this::isKeepYOnPressVisible);
   public final BooleanOption multiPlace = new BooleanOption("multi-place", true);
   public final BooleanOption safeWalk = new BooleanOption("safe-walk", true);
   public final BooleanOption swing = new BooleanOption("swing", true);
   public final BooleanOption itemSpoof = new BooleanOption("item-spoof", false);
   public final BooleanOption blockCounter = new BooleanOption("block-counter", true);

   private boolean shouldDisableSprint() {
      if (this.isTellyTakeoff()) {
         return false;
      } else {
         boolean standardKeepY = this.keepY.getValue() == 1 || this.keepY.getValue() == 2;
         return standardKeepY && this.keepYState > 0 ? false : this.sprint.getValue() == 0;
      }
   }

   private boolean canPlaceWithActiveModules() {
      BedNuker bedNuker = (BedNuker)ClientContext.moduleManager.modules.get(BedNuker.class);
      if (bedNuker.isEnabled() && bedNuker.isControllingInteractions()) {
         return false;
      } else {
         LongJump longJump = (LongJump)ClientContext.moduleManager.modules.get(LongJump.class);
         return !longJump.isEnabled() || !longJump.isAutomaticFireballMode() || longJump.hasJumpStarted();
      }
   }

   private EnumFacing findConnectingFace(BlockPos supportBlock, BlockPos desiredPlacementBlock) {
      EnumFacing bestFace = null;
      double bestDistanceSquared = 0.0;

      for (EnumFacing candidateFace : EnumFacing.field_82609_l) {
         if (candidateFace != EnumFacing.DOWN) {
            BlockPos neighborBlock = supportBlock.func_177972_a(candidateFace);
            if (neighborBlock.func_177956_o() <= desiredPlacementBlock.func_177956_o() && BlockHelper.isReplaceableAt(neighborBlock)) {
               double distanceSquared = neighborBlock.func_177957_d(
                  desiredPlacementBlock.func_177958_n() + 0.5, desiredPlacementBlock.func_177956_o() + 0.5, desiredPlacementBlock.func_177952_p() + 0.5
               );
               if (bestFace == null || distanceSquared < bestDistanceSquared || distanceSquared == bestDistanceSquared && candidateFace == EnumFacing.UP) {
                  bestDistanceSquared = distanceSquared;
                  bestFace = candidateFace;
               }
            }
         }
      }

      return bestFace;
   }

   private PlacementTarget findPlacementTarget() {
      int playerBlockY = MathHelper.func_76128_c(minecraft.field_71439_g.field_70163_u);
      int placementY = this.keepYState != 0 && !this.keepYRecoveryPlacement ? Math.min(playerBlockY, this.keepYLevel) : playerBlockY;
      BlockPos desiredPlacementBlock = new BlockPos(
         MathHelper.func_76128_c(minecraft.field_71439_g.field_70165_t), placementY - 1, MathHelper.func_76128_c(minecraft.field_71439_g.field_70161_v)
      );
      if (!BlockHelper.isReplaceableAt(desiredPlacementBlock)) {
         return null;
      } else {
         ArrayList<BlockPos> supportCandidates = new ArrayList<>();
         double blockReach = minecraft.field_71442_b.func_78757_d();

         for (int offsetX = -4; offsetX <= 4; offsetX++) {
            for (int offsetY = -4; offsetY <= 0; offsetY++) {
               for (int offsetZ = -4; offsetZ <= 4; offsetZ++) {
                  BlockPos candidateBlock = desiredPlacementBlock.func_177982_a(offsetX, offsetY, offsetZ);
                  if (!BlockHelper.isReplaceableAt(candidateBlock) && !BlockHelper.isInteractableAt(candidateBlock)) {
                     double distanceToCandidate = minecraft.field_71439_g
                        .func_70011_f(candidateBlock.func_177958_n() + 0.5, candidateBlock.func_177956_o() + 0.5, candidateBlock.func_177952_p() + 0.5);
                     if (!(distanceToCandidate > blockReach)
                        && (this.keepYState == 0 || this.keepYRecoveryPlacement || candidateBlock.func_177956_o() < this.keepYLevel)) {
                        for (EnumFacing face : EnumFacing.field_82609_l) {
                           if (face != EnumFacing.DOWN && BlockHelper.isReplaceableAt(candidateBlock.func_177972_a(face))) {
                              supportCandidates.add(candidateBlock);
                           }
                        }
                     }
                  }
               }
            }
         }

         if (supportCandidates.isEmpty()) {
            return null;
         } else {
            supportCandidates.sort(Comparator.comparingDouble(supportCandidate -> targetDistanceSquared(desiredPlacementBlock, supportCandidate)));
            BlockPos selectedSupportBlock = supportCandidates.get(0);
            EnumFacing connectingFace = this.findConnectingFace(selectedSupportBlock, desiredPlacementBlock);
            return connectingFace == null ? null : new PlacementTarget(selectedSupportBlock, connectingFace);
         }
      }
   }

   private MovingObjectPosition tracePlacement(BlockPos position, EnumFacing facing, float yaw, float pitch) {
      MovingObjectPosition rayHit = RotationHelper.traceBlocks(yaw, pitch, minecraft.field_71442_b.func_78757_d(), 1.0F);
      return rayHit != null && rayHit.field_72313_a == MovingObjectType.BLOCK && rayHit.func_178782_a().equals(position) && rayHit.field_178784_b == facing
         ? rayHit
         : null;
   }

   private void placeBlock(BlockPos position, EnumFacing facing) {
      if (ItemHelper.isHoldingPlaceableBlock() && this.remainingStackBlocks > 0) {
         MovingObjectPosition rayHit = this.tracePlacement(position, facing, this.tickPlacementYaw, this.tickPlacementPitch);
         if (rayHit != null) {
            boolean placed = minecraft.field_71442_b
               .func_178890_a(
                  minecraft.field_71439_g,
                  minecraft.field_71441_e,
                  minecraft.field_71439_g.field_71071_by.func_70448_g(),
                  position,
                  facing,
                  rayHit.field_72307_f
               );
            if (placed) {
               if (minecraft.field_71442_b.func_178889_l() != GameType.CREATIVE) {
                  this.remainingStackBlocks--;
               }

               if (this.swing.getValue()) {
                  minecraft.field_71439_g.func_71038_i();
               } else {
                  PacketHelper.send(new C0APacketAnimation());
               }
            }
         }
      }
   }

   private EnumFacing facingFromYaw(float yaw) {
      if (yaw < -135.0F || yaw > 135.0F) {
         return EnumFacing.NORTH;
      } else if (yaw < -45.0F) {
         return EnumFacing.EAST;
      } else {
         return yaw < 45.0F ? EnumFacing.SOUTH : EnumFacing.WEST;
      }
   }

   private double distanceToBlockEdge(EnumFacing facing) {
      switch (facing) {
         case NORTH:
            return minecraft.field_71439_g.field_70161_v - Math.floor(minecraft.field_71439_g.field_70161_v);
         case EAST:
            return Math.ceil(minecraft.field_71439_g.field_70165_t) - minecraft.field_71439_g.field_70165_t;
         case SOUTH:
            return Math.ceil(minecraft.field_71439_g.field_70161_v) - minecraft.field_71439_g.field_70161_v;
         case WEST:
         default:
            return minecraft.field_71439_g.field_70165_t - Math.floor(minecraft.field_71439_g.field_70165_t);
      }
   }

   private float getMotionMultiplier() {
      int percent;
      if (!minecraft.field_71439_g.field_70122_E) {
         percent = this.airMotion.getValue();
      } else if (MovementHelper.speedPotionLevel() > 0) {
         percent = this.speedMotion.getValue();
      } else {
         percent = this.groundMotion.getValue();
      }

      return percent / 100.0F;
   }

   private double randomEdgeApproachDistance() {
      return 0.2155 - RandomHelper.randomDouble(1.0E-4, 9.0E-4);
   }

   private float getMovementYaw() {
      return MovementHelper.movementYawForInput(minecraft.field_71439_g.field_70177_z, MovementHelper.forwardInput(), MovementHelper.strafeInput());
   }

   private boolean isDiagonalYaw(float angle) {
      float remainder = Math.abs(angle % 90.0F);
      return remainder > 20.0F && remainder < 70.0F;
   }

   private boolean isTellyTakeoff() {
      if (!minecraft.field_71439_g.field_70122_E || !MovementHelper.hasMovementInput()) {
         return false;
      } else if (PlayerHelper.hasCollisionAbove()) {
         return false;
      } else {
         return this.keepY.getValue() == 3 && this.keepYState > 0 ? true : this.tower.getValue() == 3 && minecraft.field_71474_y.field_74314_A.func_151470_d();
      }
   }

   public Scaffold() {
      super("Scaffold", false, 0);
   }

   public int getPreviousHotbarSlot() {
      return this.previousHotbarSlot;
   }

   private void selectPlacementHotbarSlot() {
      ItemStack heldStack = minecraft.field_71439_g.func_70694_bm();
      int heldStackSize = ItemHelper.isPlaceableBlockStack(heldStack) ? heldStack.field_77994_a : 0;
      this.remainingStackBlocks = Math.min(this.remainingStackBlocks, heldStackSize);
      if (this.remainingStackBlocks <= 0) {
         int searchStartSlot = minecraft.field_71439_g.field_71071_by.field_70461_c;
         if (this.remainingStackBlocks == 0) {
            searchStartSlot--;
         }

         for (int slotCursor = searchStartSlot; slotCursor > searchStartSlot - 9; slotCursor--) {
            int hotbarSlot = (slotCursor % 9 + 9) % 9;
            ItemStack candidateStack = minecraft.field_71439_g.field_71071_by.func_70301_a(hotbarSlot);
            if (ItemHelper.isPlaceableBlockStack(candidateStack)) {
               minecraft.field_71439_g.field_71071_by.field_70461_c = hotbarSlot;
               this.remainingStackBlocks = candidateStack.field_77994_a;
               return;
            }
         }
      }
   }

   private Vec3 findBestPlacementHit(PlacementTarget placementTarget, float previousYaw, float previousPitch) {
      if (placementTarget == null) {
         return null;
      } else {
         double[] hitOffsetsX = FACE_SAMPLE_OFFSETS;
         double[] hitOffsetsY = FACE_SAMPLE_OFFSETS;
         double[] hitOffsetsZ = FACE_SAMPLE_OFFSETS;
         switch (placementTarget.face()) {
            case NORTH:
               hitOffsetsZ = new double[]{0.0};
               break;
            case EAST:
               hitOffsetsX = new double[]{1.0};
               break;
            case SOUTH:
               hitOffsetsZ = new double[]{1.0};
               break;
            case WEST:
               hitOffsetsX = new double[]{0.0};
               break;
            case DOWN:
               hitOffsetsY = new double[]{0.0};
               break;
            case UP:
               hitOffsetsY = new double[]{1.0};
         }

         float bestYaw = -180.0F;
         float bestPitch = 0.0F;
         float bestRotationCost = 0.0F;
         Vec3 bestHit = null;

         for (double hitOffsetX : hitOffsetsX) {
            for (double hitOffsetY : hitOffsetsY) {
               for (double hitOffsetZ : hitOffsetsZ) {
                  double deltaX = placementTarget.position().func_177958_n() + hitOffsetX - minecraft.field_71439_g.field_70165_t;
                  double deltaY = placementTarget.position().func_177956_o()
                     + hitOffsetY
                     - minecraft.field_71439_g.field_70163_u
                     - minecraft.field_71439_g.func_70047_e();
                  double deltaZ = placementTarget.position().func_177952_p() + hitOffsetZ - minecraft.field_71439_g.field_70161_v;
                  float[] candidateRotations = RotationHelper.rotationsFromDelta(deltaX, deltaY, deltaZ);
                  candidateRotations[0] = RotationHelper.snapToMouseSensitivity(candidateRotations[0], previousYaw);
                  candidateRotations[1] = RotationHelper.snapToMouseSensitivity(candidateRotations[1], previousPitch);
                  candidateRotations[1] = MathHelper.func_76131_a(candidateRotations[1], -90.0F, 90.0F);
                  MovingObjectPosition candidateHit = RotationHelper.traceBlocks(
                     candidateRotations[0], candidateRotations[1], minecraft.field_71442_b.func_78757_d(), 1.0F
                  );
                  if (candidateHit != null
                     && candidateHit.field_72313_a == MovingObjectType.BLOCK
                     && candidateHit.func_178782_a().equals(placementTarget.position())
                     && candidateHit.field_178784_b == placementTarget.face()) {
                     float rotationCost = Math.abs(MathHelper.func_76142_g(candidateRotations[0] - this.placementYaw))
                        + Math.abs(candidateRotations[1] - this.placementPitch);
                     if (bestYaw == -180.0F && bestPitch == 0.0F || rotationCost < bestRotationCost) {
                        bestYaw = candidateRotations[0];
                        bestPitch = candidateRotations[1];
                        bestRotationCost = rotationCost;
                        bestHit = candidateHit.field_72307_f;
                     }
                  }
               }
            }
         }

         if (bestYaw != -180.0F || bestPitch != 0.0F) {
            this.placementYaw = bestYaw;
            this.placementPitch = bestPitch;
            this.hasPlacementRotation = true;
         }

         return bestHit;
      }
   }

   private void applyPlacementRotation(
      OnRotationUpdate event, PlacementTarget placementTarget, float previousYaw, float previousPitch, float backwardYaw, float sidewaysYaw
   ) {
      if (this.hasPlacementRotation && MovementHelper.hasMovementInput()) {
         float backwardYawDifference = Math.abs(MathHelper.func_76142_g(backwardYaw - this.placementYaw));
         if (backwardYawDifference < 90.0F) {
            switch (this.rotations.getValue()) {
               case 2:
                  float snappedBackwardYaw = RotationHelper.snapToMouseSensitivity(backwardYaw, previousYaw);
                  if (placementTarget != null
                     && this.tracePlacement(placementTarget.position(), placementTarget.face(), snappedBackwardYaw, this.placementPitch) != null) {
                     this.placementYaw = snappedBackwardYaw;
                  }
                  break;
               case 3:
                  float snappedSidewaysYaw = RotationHelper.snapToMouseSensitivity(sidewaysYaw, previousYaw);
                  if (placementTarget != null
                     && this.tracePlacement(placementTarget.position(), placementTarget.face(), snappedSidewaysYaw, this.placementPitch) != null) {
                     this.placementYaw = snappedSidewaysYaw;
                  }
            }
         }
      }

      if (this.rotations.getValue() != 0) {
         float requestedYaw = this.placementYaw;
         float requestedPitch = this.placementPitch;
         if (this.isTellyTakeoff()) {
            float playerYawDifference = MathHelper.func_76142_g(minecraft.field_71439_g.field_70177_z - event.getPreviousYaw());
            requestedYaw = RotationHelper.snapToMouseSensitivity(previousYaw + playerYawDifference * RandomHelper.randomFloat(0.98F, 0.99F), previousYaw);
            requestedPitch = RotationHelper.snapToMouseSensitivity(RandomHelper.randomFloat(30.0F, 80.0F), previousPitch);
            this.placementDelayTicks = 3;
            this.tellyRotationActive = true;
         } else {
            float placementYawDifference = MathHelper.func_76142_g(this.placementYaw - event.getPreviousYaw());
            float maxYawChange = RandomHelper.randomFloat(80.0F, 90.0F);
            if (this.tellyRotationActive && minecraft.field_71439_g.field_70181_x > 0.0 && this.placementDelayTicks <= 1) {
               maxYawChange = RandomHelper.randomFloat(30.0F, 35.0F);
            }

            if (Math.abs(placementYawDifference) > maxYawChange) {
               float clampedYawChange = RotationHelper.clampAngleChange(placementYawDifference, maxYawChange);
               requestedYaw = RotationHelper.snapToMouseSensitivity(previousYaw + clampedYawChange, previousYaw);
               this.placementDelayTicks = Math.max(this.placementDelayTicks, 1);
            }
         }

         event.setRotation(requestedYaw, requestedPitch, 3);
         if (this.moveFix.getValue() == 1) {
            event.setMovementYaw(requestedYaw, 3);
         }
      }
   }

   private void placeCurrentAndAdditionalBlocks(PlacementTarget placementTarget, Vec3 placementHit) {
      if (placementTarget != null && placementHit != null && this.placementDelayTicks <= 0) {
         this.placeBlock(placementTarget.position(), placementTarget.face());
         if (this.multiPlace.getValue()) {
            for (int extraPlacementIndex = 0; extraPlacementIndex < 3; extraPlacementIndex++) {
               placementTarget = this.findPlacementTarget();
               if (placementTarget == null) {
                  break;
               }

               MovingObjectPosition extraPlacementHit = RotationHelper.traceBlocks(
                  this.placementYaw, this.placementPitch, minecraft.field_71442_b.func_78757_d(), 1.0F
               );
               if (extraPlacementHit != null
                  && extraPlacementHit.field_72313_a == MovingObjectType.BLOCK
                  && extraPlacementHit.func_178782_a().equals(placementTarget.position())
                  && extraPlacementHit.field_178784_b == placementTarget.face()) {
                  this.placeBlock(placementTarget.position(), placementTarget.face());
               } else {
                  Vec3 fallbackHit = BlockHelper.randomFacePoint(placementTarget.position(), placementTarget.face());
                  double fallbackDeltaX = fallbackHit.field_72450_a - minecraft.field_71439_g.field_70165_t;
                  double fallbackDeltaY = fallbackHit.field_72448_b - minecraft.field_71439_g.field_70163_u - minecraft.field_71439_g.func_70047_e();
                  double fallbackDeltaZ = fallbackHit.field_72449_c - minecraft.field_71439_g.field_70161_v;
                  float[] fallbackRotations = RotationHelper.rotationsFromDelta(fallbackDeltaX, fallbackDeltaY, fallbackDeltaZ);
                  if (Math.abs(MathHelper.func_76142_g(fallbackRotations[0] - this.placementYaw)) >= 120.0F
                     || Math.abs(fallbackRotations[1] - this.placementPitch) >= 60.0F) {
                     break;
                  }

                  extraPlacementHit = RotationHelper.traceBlocks(fallbackRotations[0], fallbackRotations[1], minecraft.field_71442_b.func_78757_d(), 1.0F);
                  if (extraPlacementHit == null
                     || extraPlacementHit.field_72313_a != MovingObjectType.BLOCK
                     || !extraPlacementHit.func_178782_a().equals(placementTarget.position())
                     || extraPlacementHit.field_178784_b != placementTarget.face()) {
                     break;
                  }

                  this.placeBlock(placementTarget.position(), placementTarget.face());
               }
            }
         }
      }
   }

   private boolean handlePendingTowerPlacement() {
      if (this.pendingPlacementFace == null) {
         return false;
      } else {
         if (this.placementDelayTicks <= 0) {
            BlockPos beneathPlayer = new BlockPos(
               MathHelper.func_76128_c(minecraft.field_71439_g.field_70165_t),
               MathHelper.func_76128_c(minecraft.field_71439_g.field_70163_u) - 1,
               MathHelper.func_76128_c(minecraft.field_71439_g.field_70161_v)
            );
            this.placeBlock(beneathPlayer, this.pendingPlacementFace);
         }

         this.pendingPlacementFace = null;
         return true;
      }
   }

   private void handleExtraKeepYRecovery() {
      if (this.keepY.getValue() == 2 && this.keepYState > 0 && !minecraft.field_71439_g.field_70122_E) {
         int predictedBlockY = MathHelper.func_76128_c(minecraft.field_71439_g.field_70163_u + minecraft.field_71439_g.field_70181_x);
         if (predictedBlockY <= this.keepYLevel && !(minecraft.field_71439_g.field_70163_u <= this.keepYLevel + 1)) {
            this.keepYRecoveryPlacement = true;
            PlacementTarget placementTarget = this.findPlacementTarget();
            if (placementTarget != null && this.placementDelayTicks <= 0) {
               this.placeBlock(placementTarget.position(), placementTarget.face());
            }
         }
      }
   }

   @Listen(
      priority = 50
   )
   public void onRotationUpdate(OnRotationUpdate event) {
      if (this.isEnabled()) {
         if (this.placementDelayTicks > 0) {
            this.placementDelayTicks--;
         }

         if (minecraft.field_71439_g.field_70122_E) {
            if (this.keepYState > 0) {
               this.keepYState--;
            }

            if (this.keepYState < 0) {
               this.keepYState++;
            }

            if (this.keepYState == 0
               && this.keepY.getValue() != 0
               && (!this.keepYOnPress.getValue() || PlayerHelper.isUseItemKeyDown())
               && !minecraft.field_71474_y.field_74314_A.func_151470_d()
               && !minecraft.field_71439_g.func_70644_a(Potion.field_76430_j)) {
               this.keepYState = 1;
            }

            if (!this.keepYRecoveryPlacement) {
               this.keepYLevel = MathHelper.func_76128_c(minecraft.field_71439_g.field_70163_u);
            }

            this.keepYRecoveryPlacement = false;
            this.tellyRotationActive = false;
         }

         if (this.canPlaceWithActiveModules()) {
            this.selectPlacementHotbarSlot();
            float previousYaw = event.getPreviousYaw();
            float previousPitch = event.getPreviousPitch();
            float movementYaw = this.getMovementYaw();
            float backwardYaw = movementYaw - 180.0F;
            float sidewaysYaw;
            if (this.isDiagonalYaw(movementYaw)) {
               sidewaysYaw = backwardYaw;
            } else {
               float sidewaysDirectionFactor = (movementYaw + 180.0F) % 90.0F < 45.0F ? 1.0F : -1.0F;
               sidewaysYaw = movementYaw - 135.0F * sidewaysDirectionFactor;
            }

            if (!this.hasPlacementRotation) {
               if (this.placementYaw == -180.0F && this.placementPitch == 0.0F) {
                  this.placementPitch = RotationHelper.snapToMouseSensitivity(RandomHelper.randomFloat(65.0F, 85.0F), previousPitch);
               }

               switch (this.rotations.getValue()) {
                  case 2:
                     this.placementYaw = RotationHelper.snapToMouseSensitivity(backwardYaw, previousYaw);
                     break;
                  case 3:
                  default:
                     this.placementYaw = RotationHelper.snapToMouseSensitivity(sidewaysYaw, previousYaw);
               }
            }

            PlacementTarget placementTarget = this.findPlacementTarget();
            Vec3 placementHit = this.findBestPlacementHit(placementTarget, previousYaw, previousPitch);
            this.applyPlacementRotation(event, placementTarget, previousYaw, previousPitch, backwardYaw, sidewaysYaw);
            this.tickPlacementYaw = event.getYaw();
            this.tickPlacementPitch = event.getPitch();
            this.placeCurrentAndAdditionalBlocks(placementTarget, placementHit);
            if (!this.handlePendingTowerPlacement()) {
               this.handleExtraKeepYRecovery();
            }
         }
      }
   }

   @Listen
   public void onMoveFlying(OnMoveFlying event) {
      if (this.isEnabled()
         && !minecraft.field_71439_g.field_70123_F
         && minecraft.field_71439_g.field_70737_aN <= 5
         && !minecraft.field_71439_g.func_70644_a(Potion.field_76430_j)
         && minecraft.field_71474_y.field_74314_A.func_151470_d()
         && ItemHelper.isHoldingPlaceableBlock()) {
         int heightHundredths = (int)(minecraft.field_71439_g.field_70163_u % 1.0 * 100.0);
         switch (this.tower.getValue()) {
            case 1:
               this.applyVanillaTowerMotion(heightHundredths, event);
               return;
            case 2:
               this.applyExtraTowerMotion(heightHundredths, event);
               return;
            default:
               this.towerPhase = 0;
               this.towerCycleCount = 0;
         }
      } else {
         this.towerPhase = 0;
         this.towerCycleCount = 0;
      }
   }

   @Listen
   public void onMovementInput(OnMovementInput event) {
      if (this.isEnabled()) {
         if (this.moveFix.getValue() == 1 && RotationStore.isActive() && RotationStore.movementPriority() == 3.0F && MovementHelper.hasMovementInput()) {
            MovementHelper.correctMovementForYaw(RotationStore.movementYaw());
         }

         if (minecraft.field_71439_g.field_70122_E && this.keepYState > 0 && MovementHelper.hasMovementInput()) {
            minecraft.field_71439_g.field_71158_b.field_78901_c = true;
         }
      }
   }

   @Listen
   public void onLivingUpdate(OnLivingUpdate event) {
      if (this.isEnabled()) {
         float motionMultiplier = this.getMotionMultiplier();
         if (motionMultiplier != 1.0F) {
            if (minecraft.field_71439_g.field_71158_b.field_78900_b != 0.0F && minecraft.field_71439_g.field_71158_b.field_78902_a != 0.0F) {
               float diagonalScale = 1.0F / (float)Math.sqrt(2.0);
               minecraft.field_71439_g.field_71158_b.field_78900_b *= diagonalScale;
               minecraft.field_71439_g.field_71158_b.field_78902_a *= diagonalScale;
            }

            minecraft.field_71439_g.field_71158_b.field_78900_b *= motionMultiplier;
            minecraft.field_71439_g.field_71158_b.field_78902_a *= motionMultiplier;
         }

         if (this.shouldDisableSprint()) {
            minecraft.field_71439_g.func_70031_b(false);
         }
      }
   }

   @Listen
   public void onSafeWalk(OnSafeWalk event) {
      if (this.isEnabled() && this.safeWalk.getValue() && minecraft.field_71439_g.field_70122_E && !(minecraft.field_71439_g.field_70181_x > 0.0)) {
         if (PlayerHelper.isOffsetCollisionFree(minecraft.field_71439_g.field_70159_w, -1.0, minecraft.field_71439_g.field_70179_y)) {
            event.setSafeWalk(true);
         }
      }
   }

   @Listen
   public void onRenderOverlay(OnRenderOverlay event) {
      if (this.isEnabled() && this.blockCounter.getValue()) {
         int blockCount = 0;

         for (int hotbarSlot = 0; hotbarSlot < 9; hotbarSlot++) {
            ItemStack stack = minecraft.field_71439_g.field_71071_by.func_70301_a(hotbarSlot);
            if (stack != null && stack.field_77994_a > 0 && stack.func_77973_b() instanceof ItemBlock) {
               Block block = ((ItemBlock)stack.func_77973_b()).func_179223_d();
               if (!BlockHelper.isInteractable(block) && BlockHelper.isSuitableFullBlock(block)) {
                  blockCount += stack.field_77994_a;
               }
            }
         }

         this.drawBlockCounter(blockCount);
      }
   }

   @Listen
   public void onLeftClick(OnLeftClick event) {
      if (this.isEnabled()) {
         event.setCancelled(true);
      }
   }

   @Listen
   public void onRightClick(OnRightClick event) {
      if (this.isEnabled()) {
         event.setCancelled(true);
      }
   }

   @Listen
   public void onBlockClick(OnBlockClick event) {
      if (this.isEnabled()) {
         event.setCancelled(true);
      }
   }

   @Listen
   public void onHotbarChange(OnHotbarChange event) {
      if (this.isEnabled()) {
         this.previousHotbarSlot = event.getTargetSlot(this.previousHotbarSlot);
         event.setCancelled(true);
      }
   }

   @Override
   public void onEnable() {
      this.previousHotbarSlot = minecraft.field_71439_g != null ? minecraft.field_71439_g.field_71071_by.field_70461_c : -1;
      this.remainingStackBlocks = -1;
      this.placementDelayTicks = 0;
      this.placementYaw = -180.0F;
      this.placementPitch = 0.0F;
      this.hasPlacementRotation = false;
      this.towerPhase = 0;
      this.towerCycleCount = 0;
      this.tellyRotationActive = false;
   }

   @Override
   public void onDisable() {
      if (minecraft.field_71439_g != null && this.previousHotbarSlot != -1) {
         minecraft.field_71439_g.field_71071_by.field_70461_c = this.previousHotbarSlot;
      }
   }

   private boolean isKeepYOnPressVisible() {
      return this.keepY.getValue() != 0;
   }

   private static double targetDistanceSquared(BlockPos desiredPlacementBlock, BlockPos supportCandidate) {
      return supportCandidate.func_177957_d(
         desiredPlacementBlock.func_177958_n() + 0.5, desiredPlacementBlock.func_177956_o() + 0.5, desiredPlacementBlock.func_177952_p() + 0.5
      );
   }

   private void applyVanillaTowerMotion(int heightHundredths, OnMoveFlying event) {
      switch (this.towerPhase) {
         case 0:
            if (minecraft.field_71439_g.field_70122_E) {
               this.towerPhase = 1;
               minecraft.field_71439_g.field_70181_x = -0.0784000015258789;
            }

            return;
         case 1:
            if (heightHundredths == 0 && PlayerHelper.hasCollisionBelow()) {
               this.keepYLevel = MathHelper.func_76128_c(minecraft.field_71439_g.field_70163_u);
               this.towerPhase = 2;
               minecraft.field_71439_g.field_70181_x = 0.42F;
               if (MovementHelper.hasMovementInput()) {
                  MovementHelper.setHorizontalMotion(MovementHelper.horizontalSpeed(), MovementHelper.movementYaw());
               } else {
                  MovementHelper.setSpeedAlongMotion(0.0);
                  event.setForward(0.0F);
                  event.setStrafe(0.0F);
               }

               return;
            }

            this.towerPhase = 0;
            return;
         case 2:
            this.towerPhase = 3;
            minecraft.field_71439_g.field_70181_x = 0.75 - minecraft.field_71439_g.field_70163_u % 1.0;
            return;
         case 3:
            this.towerPhase = 1;
            minecraft.field_71439_g.field_70181_x = 1.0 - minecraft.field_71439_g.field_70163_u % 1.0;
            return;
         default:
            this.towerPhase = 0;
      }
   }

   private void applyExtraTowerMotion(int heightHundredths, OnMoveFlying event) {
      switch (this.towerPhase) {
         case 0:
            if (minecraft.field_71439_g.field_70122_E) {
               this.towerPhase = 1;
               minecraft.field_71439_g.field_70181_x = -0.0784000015258789;
            }

            return;
         case 1:
            if (heightHundredths == 0 && PlayerHelper.hasCollisionBelow()) {
               this.keepYLevel = MathHelper.func_76128_c(minecraft.field_71439_g.field_70163_u);
               if (!MovementHelper.hasMovementInput()) {
                  this.towerCycleCount = 2;
                  MovementHelper.setSpeedAlongMotion(0.0);
                  event.setForward(0.0F);
                  event.setStrafe(0.0F);
                  EnumFacing towerFacing = this.facingFromYaw(MathHelper.func_76142_g(this.placementYaw - 180.0F));
                  double edgeDistance = this.distanceToBlockEdge(towerFacing);
                  if (edgeDistance > 0.1) {
                     if (minecraft.field_71439_g.field_70122_E) {
                        Vec3i edgeDirection = towerFacing.func_176730_m();
                        double approachDistance = Math.min(this.randomEdgeApproachDistance(), edgeDistance - 0.05);
                        double edgePadding = RandomHelper.randomDouble(0.02, 0.03);
                        AxisAlignedBB shiftedBounds = minecraft.field_71439_g
                           .func_174813_aQ()
                           .func_72317_d(
                              edgeDirection.func_177958_n() * (approachDistance - edgePadding),
                              0.0,
                              edgeDirection.func_177952_p() * (approachDistance - edgePadding)
                           );
                        if (minecraft.field_71441_e.func_72945_a(minecraft.field_71439_g, shiftedBounds).isEmpty()) {
                           minecraft.field_71439_g.field_70181_x = -0.0784000015258789;
                           minecraft.field_71439_g
                              .func_70107_b(
                                 shiftedBounds.field_72340_a + (shiftedBounds.field_72336_d - shiftedBounds.field_72340_a) / 2.0,
                                 shiftedBounds.field_72338_b,
                                 shiftedBounds.field_72339_c + (shiftedBounds.field_72334_f - shiftedBounds.field_72339_c) / 2.0
                              );
                        }

                        return;
                     }
                  } else {
                     this.towerPhase = 2;
                     this.pendingPlacementFace = towerFacing;
                     minecraft.field_71439_g.field_70181_x = 0.42F;
                  }

                  return;
               }

               this.towerPhase = 2;
               this.towerCycleCount++;
               minecraft.field_71439_g.field_70181_x = 0.42F;
               MovementHelper.setHorizontalMotion(MovementHelper.horizontalSpeed(), MovementHelper.movementYaw());
               return;
            }

            this.towerPhase = 0;
            this.towerCycleCount = 0;
            return;
         case 2:
            this.towerPhase = 3;
            minecraft.field_71439_g.field_70181_x = minecraft.field_71439_g.field_70181_x - RandomHelper.randomDouble(0.00101, 0.00109);
            return;
         case 3:
            if (this.towerCycleCount >= 4) {
               this.towerPhase = 4;
               this.towerCycleCount = 0;
            } else {
               this.towerPhase = 1;
               minecraft.field_71439_g.field_70181_x = 1.0 - minecraft.field_71439_g.field_70163_u % 1.0;
            }

            return;
         case 4:
            this.towerPhase = 5;
            return;
         case 5:
            if (!PlayerHelper.hasCollisionBelow()) {
               this.towerPhase = 0;
            } else {
               this.towerPhase = 1;
               minecraft.field_71439_g.field_70181_x -= 0.08;
               minecraft.field_71439_g.field_70181_x *= 0.98F;
               minecraft.field_71439_g.field_70181_x -= 0.08;
               minecraft.field_71439_g.field_70181_x *= 0.98F;
            }

            return;
         default:
            this.towerPhase = 0;
            this.towerCycleCount = 0;
      }
   }

   private void drawBlockCounter(int blockCount) {
      HeadsUpDisplay hud = (HeadsUpDisplay)ClientContext.moduleManager.modules.get(HeadsUpDisplay.class);
      float hudScale = hud.scale.getValue();
      GlStateManager.func_179094_E();
      GlStateManager.func_179152_a(hudScale, hudScale, 0.0F);
      GlStateManager.func_179097_i();
      GlStateManager.func_179147_l();
      GlStateManager.func_179112_b(770, 771);
      FontRenderer fontRenderer = minecraft.field_71466_p;
      String counterText = String.format("%d block%s left", blockCount, blockCount != 1 ? "s" : "");
      ScaledResolution resolution = new ScaledResolution(minecraft);
      float textX = (resolution.func_78326_a() / 2.0F + fontRenderer.field_78288_b * 1.5F) / hudScale;
      float textY = resolution.func_78328_b() / 2.0F / hudScale - fontRenderer.field_78288_b / 2.0F + 1.0F;
      int textColor = blockCount > 0 ? 16777215 : 16733525;
      fontRenderer.func_175065_a(counterText, textX, textY, textColor | -1090519040, hud.shadow.getValue());
      GlStateManager.func_179084_k();
      GlStateManager.func_179126_j();
      GlStateManager.func_179121_F();
   }
}
