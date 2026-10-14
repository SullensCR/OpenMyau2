package myau.module.movement;

import myau.core.ClientContext;
import myau.event.Listen;
import myau.event.input.OnRightClick;
import myau.event.player.OnLivingUpdate;
import myau.event.player.OnPreWalkingUpdate;
import myau.module.ClientModule;
import myau.module.combat.KillAura;
import myau.player.GroundSpoofSource;
import myau.setting.BooleanOption;
import myau.setting.ChoiceOption;
import myau.setting.PercentOption;
import myau.util.entity.EntityHelper;
import myau.util.item.ItemHelper;
import myau.util.player.PlayerHelper;
import myau.util.world.BlockHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.util.BlockPos;

public class NoSlow extends ClientModule {
   private static final Minecraft minecraft = Minecraft.func_71410_x();
   private int lastFloatUseSlot = -1;
   public final ChoiceOption swordMode = new ChoiceOption("sword-mode", 1, new String[]{"NONE", "VANILLA"});
   public final PercentOption swordMotion = new PercentOption("sword-motion", 20, this::isSwordMotionVisible);
   public final BooleanOption swordSprint = new BooleanOption("sword-sprint", true, this::isSwordSprintVisible);
   public final ChoiceOption foodMode = new ChoiceOption("food-mode", 0, new String[]{"NONE", "VANILLA", "FLOAT"});
   public final PercentOption foodMotion = new PercentOption("food-motion", 20, this::isFoodMotionVisible);
   public final BooleanOption foodSprint = new BooleanOption("food-sprint", true, this::isFoodSprintVisible);
   public final ChoiceOption bowMode = new ChoiceOption("bow-mode", 0, new String[]{"NONE", "VANILLA", "FLOAT"});
   public final PercentOption bowMotion = new PercentOption("bow-motion", 20, this::isBowMotionVisible);
   public final BooleanOption bowSprint = new BooleanOption("bow-sprint", true, this::isBowSprintVisible);

   public NoSlow() {
      super("NoSlow", false, 0);
   }

   public boolean isSwordModeActive() {
      return this.swordMode.getValue() != 0 && ItemHelper.isHoldingSword();
   }

   public boolean isFoodModeActive() {
      return this.foodMode.getValue() != 0 && ItemHelper.isHoldingConsumable();
   }

   public boolean isBowModeActive() {
      return this.bowMode.getValue() != 0 && ItemHelper.isHoldingBow();
   }

   public boolean isFloatModeActive() {
      return this.foodMode.getValue() == 2 && ItemHelper.isHoldingConsumable() || this.bowMode.getValue() == 2 && ItemHelper.isHoldingBow();
   }

   public boolean shouldSuppressItemSlowdown() {
      return minecraft.field_71439_g.func_71039_bw() && (this.isSwordModeActive() || this.isFoodModeActive() || this.isBowModeActive());
   }

   public boolean shouldAllowSprint() {
      return this.isSwordModeActive() && this.swordSprint.getValue()
         || this.isFoodModeActive() && this.foodSprint.getValue()
         || this.isBowModeActive() && this.bowSprint.getValue();
   }

   public int getMotionPercent() {
      if (ItemHelper.isHoldingSword()) {
         return this.swordMotion.getValue();
      } else if (ItemHelper.isHoldingConsumable()) {
         return this.foodMotion.getValue();
      } else {
         return ItemHelper.isHoldingBow() ? this.bowMotion.getValue() : 100;
      }
   }

   @Listen
   public void onLivingUpdate(OnLivingUpdate event) {
      if (this.isEnabled() && this.shouldSuppressItemSlowdown()) {
         KillAura killAura = (KillAura)ClientContext.moduleManager.modules.get(KillAura.class);
         if (!killAura.isAutoBlockActive() || !killAura.autoBlockNoSlow.getValue()) {
            float motionScale = this.getMotionPercent() / 100.0F;
            minecraft.field_71439_g.field_71158_b.field_78900_b *= motionScale;
            minecraft.field_71439_g.field_71158_b.field_78902_a *= motionScale;
            if (!this.shouldAllowSprint()) {
               minecraft.field_71439_g.func_70031_b(false);
            }
         }
      }
   }

   @Listen
   public void onPreWalkingUpdate(OnPreWalkingUpdate event) {
      if (this.isEnabled() && this.isFloatModeActive()) {
         int selectedSlot = minecraft.field_71439_g.field_71071_by.field_70461_c;
         if (this.lastFloatUseSlot != selectedSlot && PlayerHelper.isUseItemKeyDown()) {
            this.lastFloatUseSlot = selectedSlot;
            ClientContext.groundSpoofManager.setRequest(true, GroundSpoofSource.NO_SLOW);
         }
      } else {
         this.lastFloatUseSlot = -1;
         ClientContext.groundSpoofManager.setRequest(false, GroundSpoofSource.NO_SLOW);
      }
   }

   @Listen
   public void onRightClick(OnRightClick event) {
      if (this.isEnabled()) {
         if (minecraft.field_71476_x != null) {
            switch (minecraft.field_71476_x.field_72313_a) {
               case BLOCK:
                  BlockPos blockPos = minecraft.field_71476_x.func_178782_a();
                  if (BlockHelper.isInteractableAt(blockPos) && !PlayerHelper.isSneakKeyDown()) {
                     return;
                  }
                  break;
               case ENTITY:
                  Entity entity = minecraft.field_71476_x.field_72308_g;
                  if (entity instanceof EntityVillager) {
                     return;
                  }

                  if (entity instanceof EntityLivingBase && EntityHelper.isShopNPC((EntityLivingBase)entity)) {
                     return;
                  }
            }
         }

         if (this.isFloatModeActive() && !ClientContext.groundSpoofManager.isOffsetActive() && minecraft.field_71439_g.field_70122_E) {
            event.setCancelled(true);
            minecraft.field_71439_g.field_70181_x = 0.42F;
         }
      }
   }

   private boolean isBowSprintVisible() {
      return this.bowMode.getValue() != 0;
   }

   private boolean isBowMotionVisible() {
      return this.bowMode.getValue() != 0;
   }

   private boolean isFoodSprintVisible() {
      return this.foodMode.getValue() != 0;
   }

   private boolean isFoodMotionVisible() {
      return this.foodMode.getValue() != 0;
   }

   private boolean isSwordSprintVisible() {
      return this.swordMode.getValue() != 0;
   }

   private boolean isSwordMotionVisible() {
      return this.swordMode.getValue() != 0;
   }
}
