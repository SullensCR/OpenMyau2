package myau.module.combat;

import java.util.Objects;
import myau.core.ClientContext;
import myau.event.Listen;
import myau.event.input.OnLeftClick;
import myau.event.input.OnRightClick;
import myau.event.lifecycle.OnPreTick;
import myau.module.ClientModule;
import myau.setting.BooleanOption;
import myau.setting.FloatOption;
import myau.setting.IntegerOption;
import myau.util.input.InputHelper;
import myau.util.item.ItemHelper;
import myau.util.math.NumberConversions;
import myau.util.math.RandomHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.world.WorldSettings.GameType;

public class AutoClicker extends ClientModule {
   private static final Minecraft minecraft = Minecraft.func_71410_x();
   private boolean restoreAttackKey;
   private long clickCooldownMillis;
   private boolean restoreUseItemKey;
   private long blockHitHoldMillis;
   private long blockHitCycleMillis;
   public final IntegerOption minCps = new IntegerOption("min-cps", 10, 1, 20);
   public final IntegerOption maxCps = new IntegerOption("max-cps", 14, 1, 20);
   public final BooleanOption blockHit = new BooleanOption("block-hit", false);
   public final FloatOption blockHitHold = new FloatOption("block-hit-hold", 1.5F, 1.0F, 20.0F, this.blockHit::getValue);
   public final FloatOption blockHitDelay = new FloatOption("block-hit-delay", 0.0F, 0.0F, 20.0F, this.blockHit::getValue);
   public final IntegerOption blockHitHurtTime = new IntegerOption("block-hit-hurt-time", 6, 0, 10, this.blockHit::getValue);
   public final BooleanOption weaponsOnly = new BooleanOption("weapons-only", true);
   public final BooleanOption allowTools = new BooleanOption("allow-tools", false, this.weaponsOnly::getValue);
   public final BooleanOption breakBlocks = new BooleanOption("break-blocks", true);

   private long nextClickDelayMillis() {
      return 1000L / RandomHelper.randomLong(this.minCps.getValue().longValue(), this.maxCps.getValue().longValue());
   }

   private long blockHitHoldDurationMillis() {
      return NumberConversions.truncateFloatToLong(this.blockHitHold.getValue() * 50.0F);
   }

   private long blockHitDelayDurationMillis() {
      return NumberConversions.truncateFloatToLong(this.blockHitDelay.getValue() * 50.0F);
   }

   private boolean isLookingAtBlock() {
      MovingObjectPosition hitResult = minecraft.field_71476_x;
      return hitResult != null && hitResult.field_72313_a == MovingObjectType.BLOCK;
   }

   private boolean canAutoClick() {
      if (!this.weaponsOnly.getValue() || ItemHelper.isHoldingWeapon() || this.allowTools.getValue() && ItemHelper.isHoldingTool()) {
         if (this.breakBlocks.getValue() && this.isLookingAtBlock()) {
            GameType gameType = minecraft.field_71442_b.func_178889_l();
            return gameType != GameType.SURVIVAL && gameType != GameType.CREATIVE;
         } else {
            return true;
         }
      } else {
         return false;
      }
   }

   public AutoClicker() {
      super("AutoClicker", false, 0);
   }

   @Listen
   public void onPreTick(OnPreTick event) {
      if (this.clickCooldownMillis > 0L) {
         this.clickCooldownMillis -= 50L;
      }

      if (this.blockHitHoldMillis > 0L) {
         this.blockHitHoldMillis -= 50L;
      }

      if (this.blockHitCycleMillis > 0L) {
         this.blockHitCycleMillis -= 50L;
      }

      if (minecraft.field_71462_r != null) {
         this.restoreAttackKey = false;
         this.restoreUseItemKey = false;
      } else {
         if (this.restoreAttackKey) {
            this.restoreAttackKey = false;
            InputHelper.updateKeyState(minecraft.field_71474_y.field_74312_F.func_151463_i());
         }

         if (this.restoreUseItemKey) {
            this.restoreUseItemKey = false;
            InputHelper.updateKeyState(minecraft.field_71474_y.field_74313_G.func_151463_i());
         }

         if (this.isEnabled() && this.canAutoClick() && minecraft.field_71474_y.field_74312_F.func_151470_d()) {
            if (!minecraft.field_71439_g.func_71039_bw()) {
               while (this.clickCooldownMillis <= 0L) {
                  this.restoreAttackKey = true;
                  this.clickCooldownMillis = this.clickCooldownMillis + this.nextClickDelayMillis();
                  InputHelper.setKeyState(minecraft.field_71474_y.field_74312_F.func_151463_i(), false);
                  InputHelper.onKeyTick(minecraft.field_71474_y.field_74312_F.func_151463_i());
               }
            }

            if (this.blockHit.getValue() && minecraft.field_71474_y.field_74313_G.func_151470_d() && ItemHelper.isHoldingSword()) {
               boolean hurtTimeTooHigh = ClientContext.hurtTimeTracker.remainingHurtTicks() > this.blockHitHurtTime.getValue();
               if (this.blockHitHoldMillis <= 0L || hurtTimeTooHigh) {
                  this.restoreUseItemKey = true;
                  InputHelper.setKeyState(minecraft.field_71474_y.field_74313_G.func_151463_i(), false);
                  if (this.blockHitCycleMillis > 0L || hurtTimeTooHigh) {
                     return;
                  }
               }

               if (this.blockHitHoldMillis <= 0L && !minecraft.field_71439_g.func_71039_bw()) {
                  long holdDurationMillis = this.blockHitHoldDurationMillis();
                  this.blockHitHoldMillis += holdDurationMillis;
                  this.blockHitCycleMillis = this.blockHitCycleMillis + holdDurationMillis + 50L + this.blockHitDelayDurationMillis();
                  InputHelper.onKeyTick(minecraft.field_71474_y.field_74313_G.func_151463_i());
               }
            }
         }
      }
   }

   @Listen(
      priority = -100
   )
   public void onLeftClick(OnLeftClick event) {
      if (this.isEnabled() && !event.isCancelled() && !this.restoreAttackKey) {
         this.clickCooldownMillis = this.clickCooldownMillis + this.nextClickDelayMillis();
      }
   }

   @Listen(
      priority = -100
   )
   public void onRightClick(OnRightClick event) {
      if (this.isEnabled() && !event.isCancelled() && ItemHelper.isHoldingSword() && !this.restoreUseItemKey) {
         this.blockHitHoldMillis = this.blockHitHoldMillis + this.blockHitHoldDurationMillis();
      }
   }

   @Override
   public void onEnable() {
      this.clickCooldownMillis = 0L;
      this.blockHitHoldMillis = 0L;
      this.blockHitCycleMillis = 0L;
   }

   @Override
   public void onSettingChanged(String name) {
      if (this.minCps.getName().equals(name)) {
         if (this.minCps.getValue() > this.maxCps.getValue()) {
            this.maxCps.setValue(this.minCps.getValue());
         }
      } else if (this.maxCps.getName().equals(name) && this.minCps.getValue() > this.maxCps.getValue()) {
         this.minCps.setValue(this.maxCps.getValue());
      }
   }

   @Override
   public String[] getHudInfo() {
      return Objects.equals(this.minCps.getValue(), this.maxCps.getValue())
         ? new String[]{this.minCps.getValue().toString()}
         : new String[]{String.format("%d-%d", this.minCps.getValue(), this.maxCps.getValue())};
   }
}
