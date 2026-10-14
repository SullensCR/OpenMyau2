package myau.module.world;

import com.google.common.base.CaseFormat;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import myau.core.ClientContext;
import myau.event.Listen;
import myau.event.input.OnBlockClick;
import myau.event.input.OnHotbarChange;
import myau.event.input.OnLeftClick;
import myau.event.input.OnRightClick;
import myau.event.lifecycle.OnPreTick;
import myau.event.lifecycle.OnWorldLoad;
import myau.event.network.OnPacketReceive;
import myau.event.player.OnMovementInput;
import myau.event.player.OnPreWalkingUpdate;
import myau.event.player.OnRotationUpdate;
import myau.event.player.OnVelocity;
import myau.event.render.OnRenderOverlay;
import myau.event.render.OnRenderWorld;
import myau.mixin.accessor.PlayerControllerMPAccessor;
import myau.module.ClientModule;
import myau.module.render.BedESP;
import myau.module.render.HeadsUpDisplay;
import myau.network.InboundQueueOwner;
import myau.rotation.RotationHelper;
import myau.rotation.RotationStore;
import myau.setting.BooleanOption;
import myau.setting.ChoiceOption;
import myau.setting.FloatOption;
import myau.setting.PercentOption;
import myau.util.Stopwatch;
import myau.util.chat.ChatFormat;
import myau.util.item.ItemHelper;
import myau.util.movement.MovementHelper;
import myau.util.network.PacketHelper;
import myau.util.player.PlayerHelper;
import myau.util.render.ColorHelper;
import myau.util.render.RenderHelper;
import myau.util.world.BlockHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockBed.EnumPartType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.client.C07PacketPlayerDigging.Action;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.potion.Potion;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;

public class BedNuker extends ClientModule {
   private static final Minecraft minecraft = Minecraft.func_71410_x();
   private final ScheduledExecutorService bedScanExecutor = Executors.newScheduledThreadPool(1);
   private final Stopwatch bedBreakTimer = new Stopwatch();
   private final Color lowProgressColor = new Color(ChatFormat.RED.getRgb());
   private final Color midProgressColor = new Color(ChatFormat.YELLOW.getRgb());
   private final Color highProgressColor = new Color(ChatFormat.GREEN.getRgb());
   private BlockPos targetBlock = null;
   private BlockPos whitelistedBed = null;
   private int breakPhase = 0;
   private int breakTicks = 0;
   private float accumulatedBreakDamage = 0.0F;
   private boolean targetIsBed = false;
   private int previousHotbarSlot = -1;
   private boolean interactionLocked = false;
   private boolean diggingInProgress = false;
   private boolean awaitingSpawnTeleport = false;
   public final ChoiceOption mode = new ChoiceOption("mode", 0, new String[]{"LEGIT", "SWAP"});
   public final FloatOption range = new FloatOption("range", 4.5F, 3.0F, 6.0F);
   public final PercentOption speed = new PercentOption("speed", 0);
   public final BooleanOption groundSpoof = new BooleanOption("ground-spoof", false);
   public final ChoiceOption ignoreVelocity = new ChoiceOption("ignore-velocity", 0, new String[]{"NONE", "CANCEL", "DELAY"});
   public final BooleanOption surroundings = new BooleanOption("surroundings", true);
   public final BooleanOption toolCheck = new BooleanOption("tool-check", true);
   public final BooleanOption whitelist = new BooleanOption("whitelist", true);
   public final BooleanOption swing = new BooleanOption("swing", true);
   public final ChoiceOption moveFix = new ChoiceOption("move-fix", 1, new String[]{"NONE", "SILENT", "STRICT"});
   public final ChoiceOption showTarget = new ChoiceOption("show-target", 1, new String[]{"NONE", "DEFAULT", "HUD"});
   public final ChoiceOption showProgress = new ChoiceOption("show-progress", 1, new String[]{"NONE", "DEFAULT", "HUD"});
   private static final long BED_SCAN_COOLDOWN_MILLIS = 500L;

   private void resetBreakState() {
      if (this.targetBlock != null) {
         minecraft.field_71441_e.func_175715_c(minecraft.field_71439_g.func_145782_y(), this.targetBlock, -1);
      }

      this.targetBlock = null;
      this.breakPhase = 0;
      this.breakTicks = 0;
      this.accumulatedBreakDamage = 0.0F;
      this.targetIsBed = false;
      this.interactionLocked = false;
      this.diggingInProgress = false;
   }

   private float getBreakProgress() {
      if (this.targetBlock == null) {
         return 0.0F;
      } else {
         float breakDamage = this.accumulatedBreakDamage;
         if (this.groundSpoof.getValue()) {
            IBlockState state = minecraft.field_71441_e.func_180495_p(this.targetBlock);
            int bestToolSlot = ItemHelper.findBestToolForBlock(minecraft.field_71439_g.field_71071_by.field_70461_c, state.func_177230_c());
            breakDamage = this.breakTicks * this.getRelativeBlockHardness(state, this.targetBlock, bestToolSlot, true);
         }

         float completionThreshold = 1.0F - 0.3F * (this.speed.getValue().intValue() / 100.0F);
         return Math.min(1.0F, breakDamage / completionThreshold);
      }
   }

   private void restoreHotbarSlot() {
      if (this.previousHotbarSlot != -1) {
         minecraft.field_71439_g.field_71071_by.field_70461_c = this.previousHotbarSlot;
         this.syncHeldItem();
         this.previousHotbarSlot = -1;
      }
   }

   private void syncHeldItem() {
      PlayerControllerMPAccessor controller = (PlayerControllerMPAccessor)minecraft.field_71442_b;
      if (controller.getCurrentPlayerItem() != minecraft.field_71439_g.field_71071_by.field_70461_c) {
         minecraft.field_71439_g.func_71034_by();
      }

      controller.callSyncCurrentPlayItem();
   }

   private boolean hasRequiredTool(Block block) {
      Material material = block.func_149688_o();
      if (material != Material.field_151573_f && material != Material.field_151574_g && material != Material.field_151576_e) {
         return true;
      } else {
         for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = minecraft.field_71439_g.field_71071_by.func_70301_a(slot);
            if (stack != null && stack.func_77973_b() instanceof ItemPickaxe) {
               return true;
            }
         }

         return false;
      }
   }

   private EnumFacing findBreakFace(BlockPos position) {
      double deltaX = position.func_177958_n() + 0.5 - minecraft.field_71439_g.field_70165_t;
      double deltaY = position.func_177956_o() + 0.25 - minecraft.field_71439_g.field_70163_u - minecraft.field_71439_g.func_70047_e();
      double deltaZ = position.func_177952_p() + 0.5 - minecraft.field_71439_g.field_70161_v;
      float[] rotations = RotationHelper.rotationsFromDelta(deltaX, deltaY, deltaZ);
      MovingObjectPosition hit = RotationHelper.traceBlocks(rotations[0], rotations[1], 8.0, 1.0F);
      return hit == null ? EnumFacing.UP : hit.field_178784_b;
   }

   private float getDigSpeedForSlot(IBlockState blockState, int slot, boolean onGround) {
      ItemStack stack = minecraft.field_71439_g.field_71071_by.func_70301_a(slot);
      float digSpeed = stack == null ? 1.0F : stack.func_77973_b().getDigSpeed(stack, blockState);
      if (digSpeed > 1.0F && stack != null) {
         int efficiencyLevel = EnchantmentHelper.func_77506_a(Enchantment.field_77349_p.field_77352_x, stack);
         if (efficiencyLevel > 0) {
            digSpeed += efficiencyLevel * efficiencyLevel + 1;
         }
      }

      if (minecraft.field_71439_g.func_70644_a(Potion.field_76422_e)) {
         int amplifier = minecraft.field_71439_g.func_70660_b(Potion.field_76422_e).func_76458_c();
         digSpeed *= 1.0F + (amplifier + 1) * 0.2F;
      }

      if (minecraft.field_71439_g.func_70644_a(Potion.field_76419_f)) {
         switch (minecraft.field_71439_g.func_70660_b(Potion.field_76419_f).func_76458_c()) {
            case 0:
               digSpeed *= 0.3F;
               break;
            case 1:
               digSpeed *= 0.09F;
               break;
            case 2:
               digSpeed *= 0.0027F;
               break;
            default:
               digSpeed *= 8.1E-4F;
         }
      }

      if (minecraft.field_71439_g.func_70055_a(Material.field_151586_h) && !EnchantmentHelper.func_77510_g(minecraft.field_71439_g)) {
         digSpeed /= 5.0F;
      }

      if (!onGround) {
         digSpeed /= 5.0F;
      }

      return digSpeed;
   }

   private boolean canHarvestBlock(Block block, int slot) {
      if (block.func_149688_o().func_76229_l()) {
         return true;
      } else {
         ItemStack stack = minecraft.field_71439_g.field_71071_by.func_70301_a(slot);
         return stack != null && stack.func_150998_b(block);
      }
   }

   private float getRelativeBlockHardness(IBlockState blockState, BlockPos position, int slot, boolean onGround) {
      Block block = blockState.func_177230_c();
      float hardness = block.func_176195_g(minecraft.field_71441_e, position);
      if (hardness < 0.0F) {
         return 0.0F;
      } else {
         float divisor = this.canHarvestBlock(block, slot) ? 30.0F : 100.0F;
         return this.getDigSpeedForSlot(blockState, slot, onGround) / hardness / divisor;
      }
   }

   private float getBestBreakSpeed(BlockPos position) {
      IBlockState state = minecraft.field_71441_e.func_180495_p(position);
      int bestToolSlot = ItemHelper.findBestToolForBlock(minecraft.field_71439_g.field_71071_by.field_70461_c, state.func_177230_c());
      return this.getRelativeBlockHardness(state, position, bestToolSlot, minecraft.field_71439_g.field_70122_E);
   }

   private BlockPos findSurroundingBlock(BlockPos bedPosition) {
      IBlockState bedState = minecraft.field_71441_e.func_180495_p(bedPosition);
      if (!(bedState.func_177230_c() instanceof BlockBed)) {
         return null;
      } else {
         EnumPartType part = (EnumPartType)bedState.func_177229_b(BlockBed.field_176472_a);
         EnumFacing facing = (EnumFacing)bedState.func_177229_b(BlockBed.field_176387_N);
         BlockPos otherHalf = bedPosition.func_177972_a(part == EnumPartType.HEAD ? facing.func_176734_d() : facing);
         BlockPos[] bedHalves = new BlockPos[]{bedPosition, otherHalf};
         ArrayList<BlockPos> surroundingBlocks = new ArrayList<>();
         EnumFacing[] surroundingFaces = new EnumFacing[]{EnumFacing.UP, EnumFacing.NORTH, EnumFacing.EAST, EnumFacing.SOUTH, EnumFacing.WEST};

         for (BlockPos bedHalf : bedHalves) {
            for (EnumFacing surroundingFace : surroundingFaces) {
               BlockPos surroundingPosition = bedHalf.func_177972_a(surroundingFace);
               Block surroundingBlock = minecraft.field_71441_e.func_180495_p(surroundingPosition).func_177230_c();
               if (BlockHelper.isReplaceable(surroundingBlock)) {
                  return null;
               }

               if (!(surroundingBlock instanceof BlockBed)) {
                  surroundingBlocks.add(surroundingPosition);
               }
            }
         }

         if (surroundingBlocks.isEmpty()) {
            return null;
         } else {
            surroundingBlocks.sort(this::compareSurroundingBlocks);
            return surroundingBlocks.get(0);
         }
      }
   }

   private BlockPos findTargetBlock() {
      return this.findTargetBlockNear(
         minecraft.field_71439_g.field_70165_t,
         minecraft.field_71439_g.field_70163_u + minecraft.field_71439_g.func_70047_e(),
         minecraft.field_71439_g.field_70161_v
      );
   }

   private BlockPos findTargetBlockNear(double originX, double originY, double originZ) {
      ArrayList<BlockPos> beds = new ArrayList<>();
      int blockX = MathHelper.func_76128_c(originX);
      int blockY = MathHelper.func_76128_c(originY);
      int blockZ = MathHelper.func_76128_c(originZ);

      for (int x = blockX - 6; x <= blockX + 6; x++) {
         for (int y = blockY - 6; y <= blockY + 6; y++) {
            for (int z = blockZ - 6; z <= blockZ + 6; z++) {
               BlockPos position = new BlockPos(x, y, z);
               if ((!this.whitelist.getValue() || this.whitelistedBed == null || !(this.whitelistedBed.func_177951_i(position) < 10.0))
                  && minecraft.field_71441_e.func_180495_p(position).func_177230_c() instanceof BlockBed
                  && PlayerHelper.isBlockWithinReachFrom(position, originX, originY, originZ, this.range.getValue().doubleValue())) {
                  beds.add(position);
               }
            }
         }
      }

      if (beds.isEmpty()) {
         return null;
      } else {
         beds.sort(Comparator.comparingDouble(BedNuker::distanceFromEyesSquared));

         for (BlockPos bed : beds) {
            if (!this.surroundings.getValue()) {
               return bed;
            }

            BlockPos surroundingBlock = this.findSurroundingBlock(bed);
            if (surroundingBlock == null) {
               return bed;
            }

            if (!this.toolCheck.getValue() || this.hasRequiredTool(minecraft.field_71441_e.func_180495_p(surroundingBlock).func_177230_c())) {
               return surroundingBlock;
            }
         }

         return null;
      }
   }

   private void swingOrSendAnimation() {
      if (this.swing.getValue()) {
         minecraft.field_71439_g.func_71038_i();
      } else {
         PacketHelper.send(new C0APacketAnimation());
      }
   }

   private Color getProgressColor(int displayMode) {
      switch (displayMode) {
         case 1:
            float progress = this.getBreakProgress();
            if (progress <= 0.5F) {
               return ColorHelper.interpolate(progress / 0.5F, this.lowProgressColor, this.midProgressColor);
            }

            return ColorHelper.interpolate((progress - 0.5F) / 0.5F, this.midProgressColor, this.highProgressColor);
         case 2:
            return ((HeadsUpDisplay)ClientContext.moduleManager.modules.get(HeadsUpDisplay.class)).getColor(System.currentTimeMillis());
         default:
            return new Color(-1);
      }
   }

   public BedNuker() {
      super("BedNuker", false, 0);
   }

   public boolean isControllingInteractions() {
      return this.targetBlock != null && this.interactionLocked;
   }

   public boolean isDigging() {
      return this.targetBlock != null && this.diggingInProgress;
   }

   @Listen(
      priority = 50
   )
   public void onPreTick(OnPreTick event) {
      if (this.isEnabled()) {
         if (this.targetBlock != null) {
            boolean invalidTarget = minecraft.field_71441_e.func_175623_d(this.targetBlock)
               || !PlayerHelper.isBlockWithinReach(this.targetBlock, this.range.getValue().doubleValue());
            if (!invalidTarget && !this.targetIsBed) {
               BlockPos newTarget = this.findTargetBlock();
               invalidTarget = newTarget != null && minecraft.field_71441_e.func_180495_p(newTarget).func_177230_c() instanceof BlockBed;
            }

            if (invalidTarget) {
               this.restoreHotbarSlot();
               this.resetBreakState();
            }
         }

         if (this.targetBlock != null) {
            IBlockState blockState = minecraft.field_71441_e.func_180495_p(this.targetBlock);
            int toolSlot = ItemHelper.findBestToolForBlock(minecraft.field_71439_g.field_71071_by.field_70461_c, blockState.func_177230_c());
            if (this.mode.getValue() == 0 && this.previousHotbarSlot == -1) {
               this.previousHotbarSlot = minecraft.field_71439_g.field_71071_by.field_70461_c;
               minecraft.field_71439_g.field_71071_by.field_70461_c = toolSlot;
               this.syncHeldItem();
            }

            switch (this.breakPhase) {
               case 0:
                  if (!minecraft.field_71439_g.func_71039_bw()) {
                     this.swingOrSendAnimation();
                     PacketHelper.send(new C07PacketPlayerDigging(Action.START_DESTROY_BLOCK, this.targetBlock, this.findBreakFace(this.targetBlock)));
                     this.swingOrSendAnimation();
                     minecraft.field_71452_i.func_180532_a(this.targetBlock, this.findBreakFace(this.targetBlock));
                     this.breakPhase = 1;
                  }
                  break;
               case 1:
                  if (this.mode.getValue() != 0) {
                     this.interactionLocked = false;
                  }

                  this.diggingInProgress = true;
                  this.breakTicks++;
                  this.accumulatedBreakDamage = this.accumulatedBreakDamage
                     + this.getRelativeBlockHardness(blockState, this.targetBlock, toolSlot, minecraft.field_71439_g.field_70122_E);
                  boolean effectiveGround = minecraft.field_71439_g.field_70122_E || this.groundSpoof.getValue();
                  float projectedDamage = this.breakTicks * this.getRelativeBlockHardness(blockState, this.targetBlock, toolSlot, effectiveGround);
                  minecraft.field_71452_i.func_180532_a(this.targetBlock, this.findBreakFace(this.targetBlock));
                  float threshold = 1.0F - 0.3F * (this.speed.getValue().intValue() / 100.0F);
                  if (!(this.accumulatedBreakDamage >= threshold) && !(projectedDamage >= threshold)) {
                     break;
                  }

                  if (this.mode.getValue() != 0) {
                     this.interactionLocked = true;
                     this.previousHotbarSlot = minecraft.field_71439_g.field_71071_by.field_70461_c;
                     minecraft.field_71439_g.field_71071_by.field_70461_c = toolSlot;
                     this.syncHeldItem();
                     if (minecraft.field_71439_g.func_71039_bw()) {
                        this.previousHotbarSlot = minecraft.field_71439_g.field_71071_by.field_70461_c;
                        minecraft.field_71439_g.field_71071_by.field_70461_c = (minecraft.field_71439_g.field_71071_by.field_70461_c + 1) % 9;
                        this.syncHeldItem();
                     }
                  }

                  this.diggingInProgress = false;
                  PacketHelper.send(new C07PacketPlayerDigging(Action.STOP_DESTROY_BLOCK, this.targetBlock, this.findBreakFace(this.targetBlock)));
                  this.swingOrSendAnimation();
                  IBlockState currentState = minecraft.field_71441_e.func_180495_p(this.targetBlock);
                  Block currentBlock = currentState.func_177230_c();
                  if (currentBlock.func_149688_o() != Material.field_151579_a) {
                     minecraft.field_71441_e.func_175718_b(2001, this.targetBlock, Block.func_176210_f(currentState));
                     minecraft.field_71441_e.func_175698_g(this.targetBlock);
                  }

                  if (currentBlock instanceof BlockBed) {
                     this.bedBreakTimer.reset();
                  }

                  this.breakPhase = 2;
                  break;
               case 2:
                  this.restoreHotbarSlot();
                  this.resetBreakState();
                  break;
               default:
                  this.resetBreakState();
            }

            if (this.targetBlock != null) {
               return;
            }
         }

         if (minecraft.field_71439_g.field_71075_bZ.field_75099_e && this.bedBreakTimer.hasElapsed(500L)) {
            this.targetBlock = this.findTargetBlock();
            this.breakPhase = 0;
            this.breakTicks = 0;
            this.accumulatedBreakDamage = 0.0F;
            this.targetIsBed = this.targetBlock != null && minecraft.field_71441_e.func_180495_p(this.targetBlock).func_177230_c() instanceof BlockBed;
            this.restoreHotbarSlot();
            if (this.targetBlock != null) {
               this.interactionLocked = true;
            }
         }

         if (this.targetBlock == null) {
            ClientContext.incomingPacketBuffer.setEnabled(false, InboundQueueOwner.BED_NUKER);
         }
      }
   }

   @Listen
   public void onRotationUpdate(OnRotationUpdate event) {
      if (this.isEnabled() && this.isControllingInteractions()) {
         double deltaX = this.targetBlock.func_177958_n() + 0.5 - minecraft.field_71439_g.field_70165_t;
         double deltaY = this.targetBlock.func_177956_o() + 0.5 - minecraft.field_71439_g.field_70163_u - minecraft.field_71439_g.func_70047_e();
         double deltaZ = this.targetBlock.func_177952_p() + 0.5 - minecraft.field_71439_g.field_70161_v;
         float previousYaw = event.getPreviousYaw();
         float previousPitch = event.getPreviousPitch();
         float[] rotation = RotationHelper.rotationsFromDelta(deltaX, deltaY, deltaZ);
         rotation[0] = RotationHelper.snapToMouseSensitivity(rotation[0], previousYaw);
         rotation[1] = RotationHelper.snapToMouseSensitivity(rotation[1], previousPitch);
         rotation[1] = MathHelper.func_76131_a(rotation[1], -90.0F, 90.0F);
         event.setRotation(rotation[0], rotation[1], 5);
         if (this.moveFix.getValue() != 0) {
            event.setMovementYaw(rotation[0], 5);
         }
      }
   }

   @Listen
   public void onPreWalkingUpdate(OnPreWalkingUpdate event) {
      if (this.isEnabled() && this.isDigging()) {
         if (!ClientContext.packetActionTracker.sentUseEntity
            && !ClientContext.packetActionTracker.sentDigging
            && !ClientContext.packetActionTracker.sentBlockPlacement
            && !ClientContext.packetActionTracker.sentAnimation) {
            this.swingOrSendAnimation();
         }
      }
   }

   @Listen
   public void onMovementInput(OnMovementInput event) {
      if (this.isEnabled() && this.moveFix.getValue() != 0) {
         if (RotationStore.isActive() && RotationStore.movementPriority() == 5.0F && MovementHelper.hasMovementInput()) {
            MovementHelper.correctMovementForYaw(RotationStore.movementYaw());
         }
      }
   }

   @Listen(
      priority = 50
   )
   public void onVelocity(OnVelocity event) {
      if (this.isEnabled() && !event.isCancelled() && !(event.getVelocityY() <= 0.0)) {
         if (this.ignoreVelocity.getValue() == 1 && this.targetBlock != null) {
            event.setCancelled(true);
            event.setVelocityX(minecraft.field_71439_g.field_70159_w);
            event.setVelocityY(minecraft.field_71439_g.field_70181_x);
            event.setVelocityZ(minecraft.field_71439_g.field_70179_y);
         }
      }
   }

   @Listen
   public void onRenderOverlay(OnRenderOverlay event) {
      if (this.isEnabled() && this.targetBlock != null) {
         if (!this.targetIsBed || !this.surroundings.getValue()) {
            if (this.showProgress.getValue() != 0) {
               HeadsUpDisplay hud = (HeadsUpDisplay)ClientContext.moduleManager.modules.get(HeadsUpDisplay.class);
               float scale = hud.scale.getValue();
               String message = String.format("%d%%", (int)(this.getBreakProgress() * 100.0F));
               GlStateManager.func_179094_E();
               GlStateManager.func_179152_a(scale, scale, 0.0F);
               GlStateManager.func_179097_i();
               GlStateManager.func_179147_l();
               GlStateManager.func_179112_b(770, 771);
               int textWidth = minecraft.field_71466_p.func_78256_a(message);
               ScaledResolution resolution = new ScaledResolution(minecraft);
               minecraft.field_71466_p
                  .func_175065_a(
                     message,
                     resolution.func_78326_a() / 2.0F / scale - textWidth / 2.0F,
                     resolution.func_78328_b() / 5.0F * 2.0F / scale,
                     this.getProgressColor(this.showProgress.getValue()).getRGB() & 16777215 | -1090519040,
                     hud.shadow.getValue()
                  );
               GlStateManager.func_179084_k();
               GlStateManager.func_179126_j();
               GlStateManager.func_179121_F();
            }
         }
      }
   }

   @Listen(
      priority = -50
   )
   public void onRenderWorld(OnRenderWorld event) {
      if (this.isEnabled() && this.targetBlock != null && !minecraft.field_71441_e.func_175623_d(this.targetBlock)) {
         minecraft.field_71441_e.func_175715_c(minecraft.field_71439_g.func_145782_y(), this.targetBlock, (int)(this.getBreakProgress() * 10.0F) - 1);
         if (this.showTarget.getValue() != 0) {
            BedESP bedESP = (BedESP)ClientContext.moduleManager.modules.get(BedESP.class);
            Color color = this.getProgressColor(this.showTarget.getValue());
            double height = this.targetIsBed ? bedESP.getBedHeight() : 1.0;
            RenderHelper.beginOverlay();
            RenderHelper.fillBlockBounds(this.targetBlock, height, color.getRed(), color.getGreen(), color.getBlue(), 63);
            RenderHelper.endOverlay();
         }
      }
   }

   @Listen
   public void onWorldLoad(OnWorldLoad event) {
      this.awaitingSpawnTeleport = false;
   }

   @Listen
   public void onPacketReceive(OnPacketReceive event) {
      if (!event.isCancelled()) {
         if (event.getPacket() instanceof S02PacketChat) {
            String message = ((S02PacketChat)event.getPacket()).func_148915_c().func_150254_d();
            if (message.contains("§e§lProtect your bed and destroy the enemy bed")
               || message.contains("§e§lDestroy the enemy bed and then eliminate them")
               || message.contains("§e§lEvery few seconds brings a new surprise")) {
               this.awaitingSpawnTeleport = true;
            }
         }

         if (event.getPacket() instanceof S08PacketPlayerPosLook && this.awaitingSpawnTeleport) {
            this.awaitingSpawnTeleport = false;
            this.whitelistedBed = null;
            this.bedScanExecutor.schedule(this::scanNearbyBedForWhitelist, 1L, TimeUnit.SECONDS);
         }

         if (this.isEnabled()
            && this.targetBlock != null
            && this.ignoreVelocity.getValue() == 2
            && ClientContext.incomingPacketBuffer.owner() != InboundQueueOwner.BED_NUKER) {
            if (event.getPacket() instanceof S12PacketEntityVelocity) {
               S12PacketEntityVelocity velocityPacket = (S12PacketEntityVelocity)event.getPacket();
               if (velocityPacket.func_149412_c() == minecraft.field_71439_g.func_145782_y() && velocityPacket.func_149410_e() > 0) {
                  ClientContext.incomingPacketBuffer.setOwner(InboundQueueOwner.BED_NUKER);
                  ClientContext.incomingPacketBuffer.queuedPackets.offer(velocityPacket);
                  event.setCancelled(true);
               }
            } else {
               if (event.getPacket() instanceof S27PacketExplosion) {
                  S27PacketExplosion explosionPacket = (S27PacketExplosion)event.getPacket();
                  if (explosionPacket.func_149149_c() != 0.0F || explosionPacket.func_149144_d() != 0.0F || explosionPacket.func_149147_e() != 0.0F) {
                     ClientContext.incomingPacketBuffer.setOwner(InboundQueueOwner.BED_NUKER);
                     ClientContext.incomingPacketBuffer.queuedPackets.offer(explosionPacket);
                     event.setCancelled(true);
                  }
               }
            }
         }
      }
   }

   @Listen
   public void onLeftClick(OnLeftClick event) {
      if (this.isEnabled()) {
         if (this.isControllingInteractions()) {
            event.setCancelled(true);
         } else {
            MovingObjectPosition hit = minecraft.field_71476_x;
            if (this.targetBlock != null && hit != null && hit.field_72313_a == MovingObjectType.BLOCK) {
               event.setCancelled(true);
            }
         }
      }
   }

   @Listen
   public void onRightClick(OnRightClick event) {
      if (this.isEnabled() && this.isControllingInteractions()) {
         event.setCancelled(true);
      }
   }

   @Listen
   public void onBlockClick(OnBlockClick event) {
      if (this.isEnabled()) {
         if (this.isControllingInteractions()) {
            event.setCancelled(true);
         } else {
            MovingObjectPosition hit = minecraft.field_71476_x;
            if (this.targetBlock != null && hit != null && hit.field_72313_a == MovingObjectType.BLOCK) {
               event.setCancelled(true);
            }
         }
      }
   }

   @Listen
   public void onHotbarChange(OnHotbarChange event) {
      if (this.isEnabled() && this.previousHotbarSlot != -1) {
         this.previousHotbarSlot = event.getTargetSlot(this.previousHotbarSlot);
         event.setCancelled(true);
      }
   }

   @Override
   public void onDisable() {
      this.resetBreakState();
      this.previousHotbarSlot = -1;
      ClientContext.incomingPacketBuffer.setEnabled(false, InboundQueueOwner.BED_NUKER);
   }

   @Override
   public String[] getHudInfo() {
      return new String[]{CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, this.mode.getModeName())};
   }

   private void scanNearbyBedForWhitelist() {
      if (minecraft.field_71439_g != null && minecraft.field_71441_e != null) {
         int centerX = MathHelper.func_76128_c(minecraft.field_71439_g.field_70165_t);
         int centerY = MathHelper.func_76128_c(minecraft.field_71439_g.field_70163_u + minecraft.field_71439_g.func_70047_e());
         int centerZ = MathHelper.func_76128_c(minecraft.field_71439_g.field_70161_v);
         BlockPos closestBed = null;
         int closestDistance = Integer.MAX_VALUE;

         for (int x = centerX - 25; x <= centerX + 25; x++) {
            for (int y = centerY - 25; y <= centerY + 25; y++) {
               for (int z = centerZ - 25; z <= centerZ + 25; z++) {
                  BlockPos position = new BlockPos(x, y, z);
                  if (minecraft.field_71441_e.func_180495_p(position).func_177230_c() instanceof BlockBed) {
                     int distance = Math.abs(x - centerX) + Math.abs(y - centerY) + Math.abs(z - centerZ);
                     if (distance < closestDistance) {
                        closestBed = position;
                        closestDistance = distance;
                     }
                  }
               }
            }
         }

         if (closestBed != null) {
            this.whitelistedBed = closestBed;
         }
      }
   }

   private static double distanceFromEyesSquared(BlockPos blockPos) {
      return blockPos.func_177957_d(
         minecraft.field_71439_g.field_70165_t,
         minecraft.field_71439_g.field_70163_u + minecraft.field_71439_g.func_70047_e(),
         minecraft.field_71439_g.field_70161_v
      );
   }

   private int compareSurroundingBlocks(BlockPos first, BlockPos second) {
      int speedComparison = Float.compare(this.getBestBreakSpeed(second), this.getBestBreakSpeed(first));
      return speedComparison != 0 ? speedComparison : Double.compare(distanceFromEyesSquared(first), distanceFromEyesSquared(second));
   }
}
