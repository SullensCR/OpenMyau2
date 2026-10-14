package myau.module.combat;

import com.google.common.base.CaseFormat;
import java.awt.Color;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Optional;
import myau.core.ClientContext;
import myau.event.Listen;
import myau.event.input.OnBlockClick;
import myau.event.input.OnLeftClick;
import myau.event.input.OnRightClick;
import myau.event.input.OnStopUsingItem;
import myau.event.lifecycle.OnPostTick;
import myau.event.lifecycle.OnPreTick;
import myau.event.network.OnPacketReceive;
import myau.event.network.OnPacketSend;
import myau.event.player.OnMovementInput;
import myau.event.player.OnPostPlayerUpdate;
import myau.event.player.OnRotationUpdate;
import myau.event.render.OnRenderWorld;
import myau.mixin.accessor.PlayerControllerMPAccessor;
import myau.module.ClientModule;
import myau.module.player.AutoHeal;
import myau.module.render.HeadsUpDisplay;
import myau.module.world.BedNuker;
import myau.module.world.Scaffold;
import myau.network.OutboundQueueOwner;
import myau.rotation.RotationHelper;
import myau.rotation.RotationStore;
import myau.setting.BooleanOption;
import myau.setting.ChoiceOption;
import myau.setting.FloatOption;
import myau.setting.IntegerOption;
import myau.setting.PercentOption;
import myau.util.Stopwatch;
import myau.util.chat.ChatHelper;
import myau.util.entity.EntityHelper;
import myau.util.item.ItemHelper;
import myau.util.math.NumberConversions;
import myau.util.math.RandomHelper;
import myau.util.movement.MovementHelper;
import myau.util.network.PacketHelper;
import myau.util.player.PlayerHelper;
import myau.util.render.RenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.DataWatcher.WatchableObject;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntitySilverfish;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.client.C02PacketUseEntity.Action;
import net.minecraft.network.play.server.S06PacketUpdateHealth;
import net.minecraft.network.play.server.S1CPacketEntityMetadata;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.world.WorldSettings.GameType;

public class KillAura extends ClientModule {
   private static final Minecraft minecraft = Minecraft.func_71410_x();
   private static final DecimalFormat damageFormat = new DecimalFormat("+0.0;-0.0");
   private final Stopwatch targetSwitchTimer = new Stopwatch();
   private AuraTargetInfo currentTarget = null;
   private boolean hasTargetInAutoBlockRange = false;
   private int targetIndex = 0;
   private boolean attackedThisUpdate = false;
   private boolean serverBlocking = false;
   private boolean autoBlockActive = false;
   private boolean renderBlocking = false;
   private boolean bufferRestartPending = false;
   private long attackCooldownMillis = 0L;
   private long blockHoldMillis = 0L;
   private long blockReleaseDelayMillis = 0L;
   private int autoBlockPhase = 0;
   private int lastDamageLogTick = 0;
   public final ChoiceOption mode = new ChoiceOption("mode", 0, new String[]{"SINGLE", "SWITCH"});
   public final ChoiceOption sort = new ChoiceOption("sort", 0, new String[]{"DISTANCE", "HEALTH", "HURT_TIME", "FOV"});
   public final ChoiceOption autoBlock = new ChoiceOption(
      "auto-block", 7, new String[]{"NONE", "VANILLA", "HYPIXEL", "BLINK", "INTERACT", "SPOOF", "SWAP", "LEGIT", "FAKE"}
   );
   public final BooleanOption autoBlockRequirePress = new BooleanOption("auto-block-require-press", true, this::isAutoBlockRequirePressVisible);
   public final BooleanOption autoBlockNoSlow = new BooleanOption("auto-block-no-slow", false, this::isAutoBlockNoSlowVisible);
   public final FloatOption autoBlockHold = new FloatOption("auto-block-hold", 1.5F, 1.0F, 20.0F, this::isAutoBlockHoldVisible);
   public final FloatOption autoBlockDelay = new FloatOption("auto-block-delay", 0.0F, 0.0F, 20.0F, this::isAutoBlockDelayVisible);
   public final IntegerOption autoBlockHurtTime = new IntegerOption("auto-block-hurt-time", 6, 0, 10, this::isAutoBlockHurtTimeVisible);
   public final FloatOption autoBlockRange = new FloatOption("auto-block-range", 4.0F, 3.0F, 8.0F, this::isAutoBlockRangeVisible);
   public final FloatOption swingRange = new FloatOption("swing-range", 4.0F, 3.0F, 6.0F);
   public final FloatOption attackRange = new FloatOption("attack-range", 3.0F, 3.0F, 6.0F);
   public final IntegerOption fov = new IntegerOption("fov", 360, 30, 360);
   public final IntegerOption minAps = new IntegerOption("min-aps", 14, 1, 20);
   public final IntegerOption maxAps = new IntegerOption("max-aps", 14, 1, 20);
   public final IntegerOption switchDelay = new IntegerOption("switch-delay", 150, 0, 1000);
   public final ChoiceOption rotations = new ChoiceOption("rotations", 2, new String[]{"NONE", "LEGIT", "SILENT", "LOCK_VIEW"});
   public final ChoiceOption moveFix = new ChoiceOption("move-fix", 1, new String[]{"NONE", "SILENT", "STRICT"});
   public final PercentOption smoothing = new PercentOption("smoothing", 0);
   public final IntegerOption angleStep = new IntegerOption("angle-step", 90, 30, 180);
   public final BooleanOption throughWalls = new BooleanOption("through-walls", true);
   public final BooleanOption requirePress = new BooleanOption("require-press", false);
   public final BooleanOption allowMining = new BooleanOption("allow-mining", true);
   public final BooleanOption weaponsOnly = new BooleanOption("weapons-only", true);
   public final BooleanOption allowTools = new BooleanOption("allow-tools", false, this.weaponsOnly::getValue);
   public final BooleanOption inventoryCheck = new BooleanOption("inventory-check", true);
   public final BooleanOption botCheck = new BooleanOption("bot-check", true);
   public final BooleanOption players = new BooleanOption("players", true);
   public final BooleanOption bosses = new BooleanOption("bosses", false);
   public final BooleanOption mobs = new BooleanOption("mobs", false);
   public final BooleanOption animals = new BooleanOption("animals", false);
   public final BooleanOption golems = new BooleanOption("golems", false);
   public final BooleanOption silverfish = new BooleanOption("silverfish", false);
   public final BooleanOption teams = new BooleanOption("teams", true);
   public final ChoiceOption showTarget = new ChoiceOption("show-target", 0, new String[]{"NONE", "DEFAULT", "HUD"});
   public final ChoiceOption debugLog = new ChoiceOption("debug-log", 0, new String[]{"NONE", "HEALTH"});

   private long nextAttackDelayMillis() {
      return 1000L / RandomHelper.randomLong(this.minAps.getValue().longValue(), this.maxAps.getValue().longValue());
   }

   private long blockHoldDurationMillis() {
      return NumberConversions.truncateFloatToLong(this.autoBlockHold.getValue() * 50.0F);
   }

   private long blockReleaseDurationMillis() {
      return NumberConversions.truncateFloatToLong(this.autoBlockDelay.getValue() * 50.0F);
   }

   private boolean tryAttackTarget(float yaw, float pitch) {
      if (!ClientContext.packetActionTracker.sentDigging && !ClientContext.packetActionTracker.sentBlockPlacement) {
         if (this.isBlockingSword() && this.autoBlock.getValue() != 1) {
            return false;
         } else if (this.attackCooldownMillis > 0L) {
            return false;
         } else {
            HitSelect hitSelect = (HitSelect)ClientContext.moduleManager.modules.get(HitSelect.class);
            if (hitSelect.isEnabled() && hitSelect.shouldDelayAttack(this.currentTarget.entity)) {
               return false;
            } else {
               this.attackCooldownMillis = this.attackCooldownMillis + this.nextAttackDelayMillis();
               minecraft.field_71439_g.func_71038_i();
               boolean canAttack;
               if (this.rotations.getValue() == 0) {
                  canAttack = this.isWithinAttackRange(this.currentTarget.distance);
               } else {
                  Vec3 eyes = minecraft.field_71439_g.func_174824_e(1.0F);
                  if (this.throughWalls.getValue()) {
                     canAttack = RotationHelper.intersectBoundsFrom(this.currentTarget.bounds, eyes, yaw, pitch, this.attackRange.getValue().doubleValue())
                        != null;
                  } else {
                     canAttack = !RotationHelper.isBoundsRayObstructed(this.currentTarget.bounds, eyes, yaw, pitch, this.attackRange.getValue().doubleValue());
                  }
               }

               if (!canAttack) {
                  return false;
               } else {
                  ((PlayerControllerMPAccessor)minecraft.field_71442_b).callSyncCurrentPlayItem();
                  PacketHelper.send(new C02PacketUseEntity(this.currentTarget.entity, Action.ATTACK));
                  if (minecraft.field_71442_b.func_178889_l() != GameType.SPECTATOR) {
                     PlayerHelper.attackLocally(this.currentTarget.entity);
                  }

                  this.attackedThisUpdate = true;
                  return true;
               }
            }
         }
      } else {
         return false;
      }
   }

   private void startBlockingHeldItem() {
      ((PlayerControllerMPAccessor)minecraft.field_71442_b).callSyncCurrentPlayItem();
      this.startBlockingWithStack(minecraft.field_71439_g.func_70694_bm());
   }

   private void startBlockingWithStack(ItemStack stack) {
      PacketHelper.send(new C08PacketPlayerBlockPlacement(stack));
      minecraft.field_71439_g.func_71008_a(stack, stack.func_77988_m());
      this.serverBlocking = true;
   }

   private void releaseBlocking() {
      PacketHelper.send(
         new C07PacketPlayerDigging(net.minecraft.network.play.client.C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.field_177992_a, EnumFacing.DOWN)
      );
      minecraft.field_71439_g.func_71034_by();
      this.serverBlocking = false;
   }

   private void interactWithTargetAndBlock(float yaw, float pitch) {
      if (this.currentTarget != null) {
         MovingObjectPosition hit = RotationHelper.intersectBounds(this.currentTarget.bounds, yaw, pitch, 8.0);
         if (hit != null) {
            ((PlayerControllerMPAccessor)minecraft.field_71442_b).callSyncCurrentPlayItem();
            PacketHelper.send(
               new C02PacketUseEntity(
                  this.currentTarget.entity,
                  new Vec3(
                     hit.field_72307_f.field_72450_a - this.currentTarget.x,
                     hit.field_72307_f.field_72448_b - this.currentTarget.y,
                     hit.field_72307_f.field_72449_c - this.currentTarget.z
                  )
               )
            );
            PacketHelper.send(new C02PacketUseEntity(this.currentTarget.entity, Action.INTERACT));
            this.startBlockingWithStack(minecraft.field_71439_g.func_70694_bm());
         }
      }
   }

   private boolean canRunCombat() {
      if (this.currentTarget == null) {
         return false;
      } else if (this.inventoryCheck.getValue() && minecraft.field_71462_r instanceof GuiContainer) {
         return false;
      } else if (!this.weaponsOnly.getValue() || ItemHelper.isHoldingWeapon() || this.allowTools.getValue() && ItemHelper.isHoldingTool()) {
         if ((ItemHelper.isHoldingConsumable() || ItemHelper.isHoldingBow()) && PlayerHelper.isUseItemKeyDown()) {
            return false;
         } else {
            AutoHeal autoHeal = (AutoHeal)ClientContext.moduleManager.modules.get(AutoHeal.class);
            if (autoHeal.isEnabled() && autoHeal.hasPendingSlotRestore()) {
               return false;
            } else {
               BedNuker bedNuker = (BedNuker)ClientContext.moduleManager.modules.get(BedNuker.class);
               if (bedNuker.isEnabled() && bedNuker.isControllingInteractions()) {
                  return false;
               } else {
                  Scaffold scaffold = (Scaffold)ClientContext.moduleManager.modules.get(Scaffold.class);
                  if (scaffold.isEnabled()) {
                     return false;
                  } else {
                     boolean attackKeyDown = PlayerHelper.isAttackKeyDown();
                     return this.allowMining.getValue() && attackKeyDown && minecraft.field_71476_x.field_72313_a == MovingObjectType.BLOCK
                        ? false
                        : !this.requirePress.getValue() || attackKeyDown;
                  }
               }
            }
         }
      } else {
         return false;
      }
   }

   private boolean canAutoBlock() {
      if (!ItemHelper.isHoldingSword()) {
         return false;
      } else {
         int autoBlockMode = this.autoBlock.getValue();
         if (autoBlockMode == 0) {
            return true;
         } else if (autoBlockMode == 8) {
            return this.hasTargetInAutoBlockRange;
         } else {
            return !this.autoBlockRequirePress.getValue() && this.hasTargetInAutoBlockRange ? true : PlayerHelper.isUseItemKeyDown();
         }
      }
   }

   private boolean isValidTargetEntity(EntityLivingBase candidate) {
      return candidate != minecraft.field_71439_g
            && candidate != minecraft.field_71439_g.field_70154_o
            && candidate != minecraft.func_175606_aa()
            && candidate != minecraft.func_175606_aa().field_70154_o
         ? this.passesEntityCategoryFilters(candidate)
         : false;
   }

   private boolean passesEntityCategoryFilters(EntityLivingBase candidate) {
      if (candidate.field_70725_aQ > 0) {
         return false;
      } else if (candidate instanceof EntityOtherPlayerMP) {
         if (!this.players.getValue()) {
            return false;
         } else {
            EntityPlayer player = (EntityPlayer)candidate;
            if (EntityHelper.isFriend(player)) {
               return false;
            } else if (EntityHelper.isEnemy(player)) {
               return true;
            } else {
               return this.teams.getValue() && EntityHelper.isTeammate(player) ? false : !this.botCheck.getValue() || !EntityHelper.isBot(player);
            }
         }
      } else if (candidate instanceof EntityDragon || candidate instanceof EntityWither) {
         return this.bosses.getValue();
      } else if (!(candidate instanceof EntityMob) && !(candidate instanceof EntitySlime)) {
         if (candidate instanceof EntityAnimal || candidate instanceof EntityBat || candidate instanceof EntitySquid || candidate instanceof EntityVillager) {
            return this.animals.getValue();
         } else if (!(candidate instanceof EntityIronGolem)) {
            return false;
         } else {
            return !this.golems.getValue() ? false : !this.teams.getValue() || !EntityHelper.hasTeamArmorStand(candidate);
         }
      } else if (!(candidate instanceof EntitySilverfish)) {
         return this.mobs.getValue();
      } else {
         return !this.silverfish.getValue() ? false : !this.teams.getValue() || !EntityHelper.hasTeamArmorStand(candidate);
      }
   }

   private boolean isWithinAnyCombatRange(double distance) {
      return this.isWithinAutoBlockRange(distance) || this.isWithinSwingRange(distance) || this.isWithinAttackRange(distance);
   }

   private boolean isWithinAutoBlockRange(double distance) {
      return this.autoBlock.getValue() != 0 && distance <= this.autoBlockRange.getValue().doubleValue();
   }

   private boolean isWithinSwingRange(double distance) {
      return distance <= this.swingRange.getValue().doubleValue();
   }

   private boolean isWithinAttackRange(double distance) {
      return distance <= this.attackRange.getValue().doubleValue();
   }

   private boolean isEnemyPlayer(EntityLivingBase entity) {
      return entity instanceof EntityPlayer && EntityHelper.isEnemy((EntityPlayer)entity);
   }

   private int findSwapSlot(int excludedSlot) {
      for (int slot = 0; slot < 9; slot++) {
         if (slot != excludedSlot && minecraft.field_71439_g.field_71071_by.func_70301_a(slot) == null) {
            return slot;
         }
      }

      for (int slotx = 0; slotx < 9; slotx++) {
         ItemStack stack = minecraft.field_71439_g.field_71071_by.func_70301_a(slotx);
         if (slotx != excludedSlot && stack != null && !stack.func_82837_s()) {
            return slotx;
         }
      }

      return Math.floorMod(excludedSlot - 1, 9);
   }

   private int findAlternateSwordSlot(int excludedSlot) {
      for (int slot = 0; slot < 9; slot++) {
         if (slot != excludedSlot) {
            ItemStack stack = minecraft.field_71439_g.field_71071_by.func_70301_a(slot);
            if (stack != null && stack.func_77973_b() instanceof ItemSword) {
               return slot;
            }
         }
      }

      return -1;
   }

   public KillAura() {
      super("KillAura", false, 0);
   }

   public EntityLivingBase getTargetEntity() {
      return this.currentTarget == null ? null : this.currentTarget.entity;
   }

   public boolean isCombatInputAllowed() {
      if (this.inventoryCheck.getValue() && minecraft.field_71462_r instanceof GuiContainer) {
         return false;
      } else if (((PlayerControllerMPAccessor)minecraft.field_71442_b).getIsHittingBlock()) {
         return false;
      } else {
         Scaffold scaffold = (Scaffold)ClientContext.moduleManager.modules.get(Scaffold.class);
         if (scaffold.isEnabled()) {
            return false;
         } else {
            return !this.weaponsOnly.getValue() || ItemHelper.isHoldingWeapon() || this.allowTools.getValue() && ItemHelper.isHoldingTool()
               ? !this.requirePress.getValue() || PlayerHelper.isAttackKeyDown()
               : false;
         }
      }
   }

   public boolean shouldRenderBlocking() {
      return this.renderBlocking && ItemHelper.isHoldingSword();
   }

   public boolean isBlockingSword() {
      return (minecraft.field_71439_g.func_71039_bw() || this.serverBlocking) && ItemHelper.isHoldingSword();
   }

   public boolean isAutoBlockActive() {
      return this.autoBlockActive;
   }

   private float[] calculateCombatRotation(OnRotationUpdate event) {
      float previousYaw = event.getPreviousYaw();
      float previousPitch = event.getPreviousPitch();
      float yawDelta = MathHelper.func_76142_g(this.currentTarget.yaw - previousYaw);
      float pitchDelta = this.currentTarget.pitch - previousPitch;
      if (Math.abs(yawDelta) < 0.5F) {
         yawDelta = 0.0F;
      }

      if (Math.abs(pitchDelta) < 0.5F) {
         pitchDelta = 0.0F;
      }

      yawDelta = RotationHelper.clampAngleChange(yawDelta, this.angleStep.getValue().intValue() * RandomHelper.randomFloat(0.75F, 1.0F));
      pitchDelta = RotationHelper.clampAngleChange(pitchDelta, this.angleStep.getValue().intValue() * RandomHelper.randomFloat(0.75F, 1.0F) * 0.5F);
      yawDelta = RotationHelper.scaleAngleChange(yawDelta, this.smoothing.getValue().intValue() / 100.0F + RandomHelper.randomFloat(-0.05F, 0.05F));
      pitchDelta = RotationHelper.scaleAngleChange(pitchDelta, this.smoothing.getValue().intValue() / 100.0F + RandomHelper.randomFloat(-0.05F, 0.05F));
      float sensitivity = minecraft.field_71474_y.field_74341_c * 0.6F + 0.2F;
      float mouseStep = sensitivity * sensitivity * sensitivity * 8.0F;
      if (yawDelta == 0.0F && pitchDelta != 0.0F) {
         yawDelta += mouseStep * 0.15F * (RandomHelper.randomBoolean() ? 1.0F : -1.0F);
      }

      if (yawDelta != 0.0F && pitchDelta == 0.0F) {
         pitchDelta += mouseStep * 0.15F * (RandomHelper.randomBoolean() ? 1.0F : -1.0F);
      }

      float yaw = RotationHelper.snapToMouseSensitivity(previousYaw + yawDelta, previousYaw);
      float pitch = RotationHelper.snapToMouseSensitivity(previousPitch + pitchDelta, previousPitch);
      return new float[]{yaw, MathHelper.func_76131_a(pitch, -90.0F, 90.0F)};
   }

   @Listen(
      priority = -50
   )
   public void onRotationUpdate(OnRotationUpdate event) {
      if (this.isEnabled()) {
         int controllerSlot = ((PlayerControllerMPAccessor)minecraft.field_71442_b).getCurrentPlayerItem();
         boolean combatEligible = this.canRunCombat();
         boolean canAttackThisTick = combatEligible;
         boolean canBlockThisTick = combatEligible && this.canAutoBlock();
         int autoBlockMode = this.autoBlock.getValue();
         boolean blockSuppressed = autoBlockMode != 0
            && autoBlockMode != 8
            && canBlockThisTick
            && (ClientContext.hurtTimeTracker.remainingHurtTicks() > this.autoBlockHurtTime.getValue() || this.blockReleaseDelayMillis > 0L);
         if (this.attackCooldownMillis > 0L) {
            this.attackCooldownMillis -= 50L;
         }

         if (this.blockHoldMillis > 0L) {
            this.blockHoldMillis -= 50L;
         }

         if (this.blockReleaseDelayMillis > 0L) {
            this.blockReleaseDelayMillis -= 50L;
         }

         if (!canBlockThisTick || blockSuppressed) {
            if (this.autoBlockActive
               && this.isBlockingSword()
               && !ClientContext.packetActionTracker.sentDigging
               && !ClientContext.packetActionTracker.sentBlockPlacement) {
               if (autoBlockMode != 4 && autoBlockMode != 5) {
                  this.releaseBlocking();
               } else {
                  int swapSlot = this.findSwapSlot(controllerSlot);
                  PacketHelper.send(new C09PacketHeldItemChange(swapSlot));
                  ((PlayerControllerMPAccessor)minecraft.field_71442_b).setCurrentPlayerItem(swapSlot);
               }

               canAttackThisTick = false;
            }

            ClientContext.outgoingPacketBuffer.setEnabled(false, OutboundQueueOwner.AUTO_BLOCK);
            this.autoBlockActive = false;
            this.renderBlocking = false;
            if (this.blockHoldMillis > 0L) {
               this.blockHoldMillis = 0L;
            }

            this.autoBlockPhase = 0;
         }

         if (combatEligible) {
            if (blockSuppressed) {
               canBlockThisTick = false;
               this.autoBlockActive = true;
               this.renderBlocking = true;
            }

            boolean shouldStartBlocking = false;
            if (canBlockThisTick) {
               switch (autoBlockMode) {
                  case 0:
                     ClientContext.outgoingPacketBuffer.setEnabled(false, OutboundQueueOwner.AUTO_BLOCK);
                     this.autoBlockActive = false;
                     this.renderBlocking = false;
                     if (PlayerHelper.isUseItemKeyDown()
                        && !this.isBlockingSword()
                        && !ClientContext.packetActionTracker.sentDigging
                        && !ClientContext.packetActionTracker.sentBlockPlacement) {
                        shouldStartBlocking = true;
                     }
                     break;
                  case 1:
                     ClientContext.outgoingPacketBuffer.setEnabled(false, OutboundQueueOwner.AUTO_BLOCK);
                     this.autoBlockActive = true;
                     this.renderBlocking = false;
                     if (!this.isBlockingSword() && !ClientContext.packetActionTracker.sentDigging && !ClientContext.packetActionTracker.sentBlockPlacement) {
                        shouldStartBlocking = true;
                     }
                     break;
                  case 2:
                     this.autoBlockActive = true;
                     this.renderBlocking = true;
                     if (!ClientContext.packetActionTracker.sentDigging && !ClientContext.packetActionTracker.sentBlockPlacement) {
                        switch (this.autoBlockPhase) {
                           case 0:
                              if (!this.isBlockingSword()) {
                                 shouldStartBlocking = true;
                                 this.blockHoldMillis = this.blockHoldMillis + this.blockHoldDurationMillis();
                              }

                              this.bufferRestartPending = true;
                              this.autoBlockPhase = 1;
                              break;
                           case 1:
                              if (this.blockHoldMillis <= 0L) {
                                 if (this.isBlockingSword()) {
                                    this.releaseBlocking();
                                    canAttackThisTick = false;
                                 }

                                 this.blockReleaseDelayMillis = this.blockReleaseDelayMillis + this.blockReleaseDurationMillis();
                                 this.autoBlockPhase = 0;
                              } else {
                                 this.bufferRestartPending = true;
                              }
                              break;
                           default:
                              this.autoBlockPhase = 0;
                        }
                     }
                     break;
                  case 3:
                     this.autoBlockActive = true;
                     this.renderBlocking = true;
                     if (!ClientContext.packetActionTracker.sentDigging && !ClientContext.packetActionTracker.sentBlockPlacement) {
                        switch (this.autoBlockPhase) {
                           case 0:
                              if (!this.isBlockingSword()) {
                                 shouldStartBlocking = true;
                                 this.blockHoldMillis = this.blockHoldMillis + this.blockHoldDurationMillis();
                              }

                              this.bufferRestartPending = true;
                              this.autoBlockPhase = 1;
                              break;
                           case 1:
                              if (this.isBlockingSword()) {
                                 this.releaseBlocking();
                                 canAttackThisTick = false;
                              }

                              if (this.blockHoldMillis <= 0L) {
                                 this.blockReleaseDelayMillis = this.blockReleaseDelayMillis + this.blockReleaseDurationMillis();
                                 this.autoBlockPhase = 0;
                              }
                              break;
                           default:
                              this.autoBlockPhase = 0;
                        }
                     }
                     break;
                  case 4:
                     this.autoBlockActive = true;
                     this.renderBlocking = true;
                     if (minecraft.field_71439_g.field_71071_by.field_70461_c == controllerSlot
                        && !ClientContext.packetActionTracker.sentDigging
                        && !ClientContext.packetActionTracker.sentBlockPlacement) {
                        switch (this.autoBlockPhase) {
                           case 0:
                              if (!this.isBlockingSword()) {
                                 shouldStartBlocking = true;
                                 this.blockHoldMillis = this.blockHoldMillis + this.blockHoldDurationMillis();
                              }

                              this.bufferRestartPending = true;
                              this.autoBlockPhase = 1;
                              break;
                           case 1:
                              if (this.isBlockingSword()) {
                                 int swapSlot = this.findSwapSlot(controllerSlot);
                                 PacketHelper.send(new C09PacketHeldItemChange(swapSlot));
                                 ((PlayerControllerMPAccessor)minecraft.field_71442_b).setCurrentPlayerItem(swapSlot);
                                 canAttackThisTick = false;
                              }

                              if (this.blockHoldMillis <= 0L) {
                                 this.blockReleaseDelayMillis = this.blockReleaseDelayMillis + this.blockReleaseDurationMillis();
                                 this.autoBlockPhase = 0;
                              }
                              break;
                           default:
                              this.autoBlockPhase = 0;
                        }
                     }
                     break;
                  case 5:
                     ClientContext.outgoingPacketBuffer.setEnabled(false, OutboundQueueOwner.AUTO_BLOCK);
                     this.autoBlockActive = true;
                     this.renderBlocking = false;
                     if (!ClientContext.packetActionTracker.sentDigging
                        && !ClientContext.packetActionTracker.sentBlockPlacement
                        && minecraft.field_71439_g.field_71071_by.field_70461_c == controllerSlot
                        && (!this.isBlockingSword() || this.blockHoldMillis <= 0L)) {
                        int swapSlot = this.findSwapSlot(controllerSlot);
                        PacketHelper.send(new C09PacketHeldItemChange(swapSlot));
                        PacketHelper.send(new C09PacketHeldItemChange(controllerSlot));
                        shouldStartBlocking = true;
                        this.blockHoldMillis = this.blockHoldMillis + this.blockHoldDurationMillis();
                     }
                     break;
                  case 6:
                     ClientContext.outgoingPacketBuffer.setEnabled(false, OutboundQueueOwner.AUTO_BLOCK);
                     this.autoBlockActive = true;
                     this.renderBlocking = true;
                     if (minecraft.field_71439_g.field_71071_by.field_70461_c == controllerSlot
                        && !ClientContext.packetActionTracker.sentDigging
                        && !ClientContext.packetActionTracker.sentBlockPlacement) {
                        switch (this.autoBlockPhase) {
                           case 0:
                              if (!this.isBlockingSword()) {
                                 shouldStartBlocking = true;
                                 this.blockHoldMillis = this.blockHoldMillis + this.blockHoldDurationMillis();
                              }

                              this.autoBlockPhase = 1;
                              break;
                           case 1:
                              if (this.blockHoldMillis <= 0L) {
                                 if (this.isBlockingSword()) {
                                    int swordSlot = this.findAlternateSwordSlot(controllerSlot);
                                    if (swordSlot != -1) {
                                       PacketHelper.send(new C09PacketHeldItemChange(swordSlot));
                                       ((PlayerControllerMPAccessor)minecraft.field_71442_b).setCurrentPlayerItem(swordSlot);
                                       this.startBlockingWithStack(minecraft.field_71439_g.field_71071_by.func_70301_a(swordSlot));
                                    } else {
                                       this.releaseBlocking();
                                    }

                                    canAttackThisTick = false;
                                 }

                                 this.blockReleaseDelayMillis = this.blockReleaseDelayMillis + this.blockReleaseDurationMillis();
                                 this.autoBlockPhase = 0;
                              }
                              break;
                           default:
                              this.autoBlockPhase = 0;
                        }
                     }
                     break;
                  case 7:
                     ClientContext.outgoingPacketBuffer.setEnabled(false, OutboundQueueOwner.AUTO_BLOCK);
                     this.autoBlockActive = true;
                     this.renderBlocking = true;
                     if (!ClientContext.packetActionTracker.sentDigging && !ClientContext.packetActionTracker.sentBlockPlacement) {
                        switch (this.autoBlockPhase) {
                           case 0:
                              if (!this.isBlockingSword()) {
                                 shouldStartBlocking = true;
                                 this.blockHoldMillis = this.blockHoldMillis + this.blockHoldDurationMillis();
                              }

                              this.autoBlockPhase = 1;
                              break;
                           case 1:
                              if (this.blockHoldMillis <= 0L) {
                                 if (this.isBlockingSword()) {
                                    this.releaseBlocking();
                                    canAttackThisTick = false;
                                 }

                                 this.blockReleaseDelayMillis = this.blockReleaseDelayMillis + this.blockReleaseDurationMillis();
                                 this.autoBlockPhase = 0;
                              }
                              break;
                           default:
                              this.autoBlockPhase = 0;
                        }
                     }
                     break;
                  case 8:
                     ClientContext.outgoingPacketBuffer.setEnabled(false, OutboundQueueOwner.AUTO_BLOCK);
                     this.autoBlockActive = false;
                     this.renderBlocking = true;
                     if (PlayerHelper.isUseItemKeyDown()
                        && !this.isBlockingSword()
                        && !ClientContext.packetActionTracker.sentDigging
                        && !ClientContext.packetActionTracker.sentBlockPlacement) {
                        shouldStartBlocking = true;
                     }
               }
            }

            boolean attacked = false;
            if (this.currentTarget.hasUsableAim && this.isWithinSwingRange(this.currentTarget.distance)) {
               int rotationMode = this.rotations.getValue();
               if (rotationMode == 2 || rotationMode == 3) {
                  float[] rotation = this.calculateCombatRotation(event);
                  event.setRotation(rotation[0], rotation[1], 1);
                  if (rotationMode == 3) {
                     ClientContext.rotationSmoother.requestRotation(rotation[0], rotation[1], 1, true);
                  }

                  if (this.moveFix.getValue() != 0 || rotationMode == 3) {
                     event.setMovementYaw(rotation[0], 1);
                  }
               }

               if (canAttackThisTick) {
                  attacked = this.tryAttackTarget(event.getYaw(), event.getPitch());
               }
            }

            if (shouldStartBlocking) {
               if (attacked) {
                  this.interactWithTargetAndBlock(event.getYaw(), event.getPitch());
               } else {
                  this.startBlockingHeldItem();
               }
            }
         }
      }
   }

   @Listen(
      priority = -50
   )
   public void onPostPlayerUpdate(OnPostPlayerUpdate event) {
      if (this.bufferRestartPending) {
         this.bufferRestartPending = false;
         ClientContext.outgoingPacketBuffer.setEnabled(false, OutboundQueueOwner.AUTO_BLOCK);
         ClientContext.outgoingPacketBuffer.setEnabled(true, OutboundQueueOwner.AUTO_BLOCK);
      }
   }

   @Listen
   public void onPreTick(OnPreTick event) {
      if (this.isEnabled()) {
         ArrayList<AuraTargetInfo> candidates = this.collectTargetCandidates();
         if (candidates.isEmpty()) {
            this.currentTarget = null;
            this.hasTargetInAutoBlockRange = false;
         } else {
            this.hasTargetInAutoBlockRange = candidates.stream().anyMatch(this::isCandidateWithinAutoBlockRange);
            if (candidates.stream().anyMatch(KillAura::hasCandidateUsableAim)) {
               candidates.removeIf(KillAura::shouldDiscardUnusableAim);
            }

            if (candidates.stream().anyMatch(this::isCandidateWithinSwingRange)) {
               candidates.removeIf(this::shouldDiscardOutsideSwingRange);
            }

            if (candidates.stream().anyMatch(this::isCandidateWithinAttackRange)) {
               candidates.removeIf(this::shouldDiscardOutsideAttackRange);
            }

            if (candidates.stream().anyMatch(this::isEnemyCandidate)) {
               candidates.removeIf(this::shouldDiscardNonEnemyCandidate);
            }

            if (candidates.isEmpty()) {
               this.currentTarget = null;
            } else {
               if (this.currentTarget != null && !this.targetSwitchTimer.hasElapsed(this.switchDelay.getValue().intValue())) {
                  Optional<AuraTargetInfo> existing = candidates.stream().filter(this::matchesCurrentTarget).findFirst();
                  if (existing.isPresent()) {
                     this.currentTarget = existing.get();
                     return;
                  }
               }

               candidates.sort(this::compareTargetCandidates);
               if (this.mode.getValue() == 1 && this.attackedThisUpdate) {
                  this.attackedThisUpdate = false;
                  this.targetIndex++;
               }

               if (this.mode.getValue() == 0 || this.targetIndex >= candidates.size()) {
                  this.targetIndex = 0;
               }

               this.currentTarget = candidates.get(this.targetIndex);
               this.targetSwitchTimer.reset();
            }
         }
      }
   }

   private ArrayList<AuraTargetInfo> collectTargetCandidates() {
      Vec3 eyes = minecraft.field_71439_g.func_174824_e(1.0F);
      ArrayList<AuraTargetInfo> candidates = new ArrayList<>();

      for (Entity entity : minecraft.field_71441_e.field_72996_f) {
         if (entity instanceof EntityLivingBase) {
            EntityLivingBase living = (EntityLivingBase)entity;
            if (this.isValidTargetEntity(living)) {
               double border = living.func_70111_Y();
               AxisAlignedBB bounds = living.func_174813_aQ().func_72314_b(border, border, border);
               double distance = RotationHelper.distanceToBoundsFrom(bounds, eyes);
               if (this.isWithinAnyCombatRange(distance)) {
                  float angularDistance = RotationHelper.boundsAimError(bounds, eyes);
                  if (!(angularDistance > this.fov.getValue().intValue())) {
                     float[] aim = RotationHelper.aimAtBounds(bounds);
                     float yaw = aim[0];
                     float pitch = aim[1];
                     boolean usableAim = true;
                     if (!this.throughWalls.getValue() && RotationHelper.isBoundsRayObstructed(bounds, eyes, yaw, pitch, 8.0)) {
                        if (RotationHelper.isBoundsRayObstructed(
                           bounds, eyes, minecraft.field_71439_g.field_70177_z, minecraft.field_71439_g.field_70125_A, 8.0
                        )) {
                           usableAim = false;
                        } else {
                           yaw = minecraft.field_71439_g.field_70177_z;
                           pitch = minecraft.field_71439_g.field_70125_A;
                        }
                     }

                     candidates.add(
                        new AuraTargetInfo(
                           living, bounds, living.field_70165_t, living.field_70163_u, living.field_70161_v, yaw, pitch, distance, angularDistance, usableAim
                        )
                     );
                  }
               }
            }
         }
      }

      return candidates;
   }

   @Listen
   public void onPostTick(OnPostTick event) {
      if (this.isEnabled() && this.isBlockingSword() && !minecraft.field_71439_g.func_70632_aY()) {
         ItemStack heldItem = minecraft.field_71439_g.func_70694_bm();
         if (heldItem != null) {
            minecraft.field_71439_g.func_71008_a(heldItem, heldItem.func_77988_m());
         }
      }
   }

   @Listen(
      priority = -100
   )
   public void onPacketSend(OnPacketSend event) {
      if (this.isEnabled() && !event.isCancelled()) {
         if (event.getPacket() instanceof C07PacketPlayerDigging) {
            C07PacketPlayerDigging digging = (C07PacketPlayerDigging)event.getPacket();
            if (digging.func_180762_c() == net.minecraft.network.play.client.C07PacketPlayerDigging.Action.RELEASE_USE_ITEM) {
               this.serverBlocking = false;
            }
         } else {
            if (event.getPacket() instanceof C09PacketHeldItemChange) {
               this.serverBlocking = false;
               if (this.autoBlockActive) {
                  minecraft.field_71439_g.func_71034_by();
               }
            }
         }
      }
   }

   private void logHealthDelta(float delta) {
      if (delta != 0.0F && this.lastDamageLogTick != minecraft.field_71439_g.field_70173_aa) {
         this.lastDamageLogTick = minecraft.field_71439_g.field_70173_aa;
         String color = delta > 0.0F ? "&a" : "&c";
         ChatHelper.addFormattedMessage(
            String.format("%sHealth: %s&l%s&r (&otick: %d&r)&r", myau.util.chat.ChatPrefix.get(), color, damageFormat.format(delta), minecraft.field_71439_g.field_70173_aa)
         );
      }
   }

   @Listen(
      priority = -100
   )
   public void onPacketReceive(OnPacketReceive event) {
      if (this.isEnabled() && !event.isCancelled() && this.debugLog.getValue() == 1) {
         if (event.getPacket() instanceof S06PacketUpdateHealth) {
            S06PacketUpdateHealth healthPacket = (S06PacketUpdateHealth)event.getPacket();
            this.logHealthDelta(healthPacket.func_149332_c() - minecraft.field_71439_g.func_110143_aJ());
         } else if (event.getPacket() instanceof S1CPacketEntityMetadata) {
            S1CPacketEntityMetadata metadataPacket = (S1CPacketEntityMetadata)event.getPacket();
            if (metadataPacket.func_149375_d() == minecraft.field_71439_g.func_145782_y()) {
               for (WatchableObject metadata : metadataPacket.func_149376_c()) {
                  if (metadata.func_75672_a() == 6) {
                     this.logHealthDelta((Float)metadata.func_75669_b() - minecraft.field_71439_g.func_110143_aJ());
                  }
               }
            }
         }
      }
   }

   @Listen
   public void onMovementInput(OnMovementInput event) {
      if (this.isEnabled() && this.moveFix.getValue() == 1) {
         if (this.rotations.getValue() != 3) {
            if (RotationStore.isActive() && RotationStore.movementPriority() == 1.0F && MovementHelper.hasMovementInput()) {
               MovementHelper.correctMovementForYaw(RotationStore.movementYaw());
            }
         }
      }
   }

   @Listen
   public void onRenderWorld(OnRenderWorld event) {
      if (this.isEnabled()
         && this.showTarget.getValue() != 0
         && this.currentTarget != null
         && EntityHelper.isLoaded(this.currentTarget.entity)
         && this.isCombatInputAllowed()) {
         Color color = Color.WHITE;
         if (this.showTarget.getValue() == 1) {
            color = this.currentTarget.entity.field_70737_aN > 0 ? new Color(16733525) : new Color(5635925);
         } else if (this.showTarget.getValue() == 2) {
            color = ((HeadsUpDisplay)ClientContext.moduleManager.modules.get(HeadsUpDisplay.class)).getColor(System.currentTimeMillis());
         }

         RenderHelper.beginOverlay();
         RenderHelper.fillEntityBounds(this.currentTarget.entity, color.getRed(), color.getGreen(), color.getBlue(), 63, 0.1F);
         RenderHelper.endOverlay();
      }
   }

   @Listen
   public void onLeftClick(OnLeftClick event) {
      if (this.autoBlockActive) {
         event.setCancelled(true);
      } else {
         if (this.isEnabled() && this.canRunCombat() && this.currentTarget != null && this.isWithinSwingRange(this.currentTarget.distance)) {
            event.setCancelled(true);
         }
      }
   }

   @Listen
   public void onRightClick(OnRightClick event) {
      if (this.autoBlockActive) {
         event.setCancelled(true);
      } else {
         if (this.isEnabled() && this.canRunCombat() && this.currentTarget != null && this.isWithinSwingRange(this.currentTarget.distance)) {
            event.setCancelled(true);
         }
      }
   }

   @Listen
   public void onBlockClick(OnBlockClick event) {
      if (this.autoBlockActive) {
         event.setCancelled(true);
      } else {
         if (this.isEnabled() && this.canRunCombat() && this.currentTarget != null && this.isWithinSwingRange(this.currentTarget.distance)) {
            event.setCancelled(true);
         }
      }
   }

   @Listen
   public void onStopUsingItem(OnStopUsingItem event) {
      if (this.autoBlockActive) {
         event.setCancelled(true);
      }
   }

   @Override
   public void onEnable() {
      this.currentTarget = null;
      this.hasTargetInAutoBlockRange = false;
      this.targetIndex = 0;
      this.attackedThisUpdate = false;
      this.attackCooldownMillis = 0L;
      this.blockHoldMillis = 0L;
      this.blockReleaseDelayMillis = 0L;
      this.autoBlockPhase = 0;
   }

   @Override
   public void onDisable() {
      ClientContext.outgoingPacketBuffer.setEnabled(false, OutboundQueueOwner.AUTO_BLOCK);
      if (this.serverBlocking) {
         minecraft.field_71439_g.func_71034_by();
      }

      this.currentTarget = null;
      this.hasTargetInAutoBlockRange = false;
      this.bufferRestartPending = false;
      this.serverBlocking = false;
      this.autoBlockActive = false;
      this.renderBlocking = false;
      this.autoBlockPhase = 0;
      this.blockHoldMillis = 0L;
      this.blockReleaseDelayMillis = 0L;
   }

   @Override
   public void onSettingChanged(String settingName) {
      if (this.swingRange.getName().equals(settingName) && this.swingRange.getValue() < this.attackRange.getValue()) {
         this.attackRange.setValue(this.swingRange.getValue());
      } else if (this.attackRange.getName().equals(settingName) && this.attackRange.getValue() > this.swingRange.getValue()) {
         this.swingRange.setValue(this.attackRange.getValue());
      }

      if (this.minAps.getValue() > this.maxAps.getValue()) {
         if (this.minAps.getName().equals(settingName)) {
            this.maxAps.setValue(this.minAps.getValue());
         } else {
            this.minAps.setValue(this.maxAps.getValue());
         }
      }
   }

   @Override
   public String[] getHudInfo() {
      return new String[]{CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, this.mode.getModeName())};
   }

   private int compareTargetCandidates(AuraTargetInfo first, AuraTargetInfo second) {
      int comparison;
      switch (this.sort.getValue()) {
         case 1:
            comparison = Float.compare(EntityHelper.armorAdjustedHealth(first.entity), EntityHelper.armorAdjustedHealth(second.entity));
            break;
         case 2:
            comparison = Integer.compare(first.entity.field_70172_ad, second.entity.field_70172_ad);
            break;
         case 3:
            comparison = Float.compare(first.angularDistance, second.angularDistance);
            break;
         default:
            comparison = Double.compare(first.distance, second.distance);
      }

      return comparison != 0 ? comparison : Double.compare(first.distance, second.distance);
   }

   private boolean matchesCurrentTarget(AuraTargetInfo candidate) {
      return this.currentTarget != null && candidate.entity == this.currentTarget.entity;
   }

   private boolean shouldDiscardNonEnemyCandidate(AuraTargetInfo candidate) {
      return !this.isEnemyPlayer(candidate.entity);
   }

   private boolean isEnemyCandidate(AuraTargetInfo candidate) {
      return this.isEnemyPlayer(candidate.entity);
   }

   private boolean shouldDiscardOutsideAttackRange(AuraTargetInfo candidate) {
      return !this.isWithinAttackRange(candidate.distance);
   }

   private boolean isCandidateWithinAttackRange(AuraTargetInfo candidate) {
      return this.isWithinAttackRange(candidate.distance);
   }

   private boolean shouldDiscardOutsideSwingRange(AuraTargetInfo candidate) {
      return !this.isWithinSwingRange(candidate.distance);
   }

   private boolean isCandidateWithinSwingRange(AuraTargetInfo candidate) {
      return this.isWithinSwingRange(candidate.distance);
   }

   private static boolean shouldDiscardUnusableAim(AuraTargetInfo candidate) {
      return !candidate.hasUsableAim;
   }

   private static boolean hasCandidateUsableAim(AuraTargetInfo candidate) {
      return candidate.hasUsableAim;
   }

   private boolean isCandidateWithinAutoBlockRange(AuraTargetInfo candidate) {
      return this.isWithinAutoBlockRange(candidate.distance);
   }

   private boolean isAutoBlockRangeVisible() {
      return this.autoBlock.getValue() != 0;
   }

   private boolean isAutoBlockHurtTimeVisible() {
      int mode = this.autoBlock.getValue();
      return mode != 0 && mode != 8;
   }

   private boolean isAutoBlockDelayVisible() {
      int mode = this.autoBlock.getValue();
      return mode != 0 && mode != 1 && mode != 8;
   }

   private boolean isAutoBlockHoldVisible() {
      int mode = this.autoBlock.getValue();
      return mode != 0 && mode != 8;
   }

   private boolean isAutoBlockNoSlowVisible() {
      return this.autoBlock.getValue() != 0;
   }

   private boolean isAutoBlockRequirePressVisible() {
      return this.autoBlock.getValue() != 0;
   }
}
