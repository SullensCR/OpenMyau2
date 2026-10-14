package myau.module.movement;

import myau.event.Listen;
import myau.event.lifecycle.OnPostTick;
import myau.event.lifecycle.OnPreTick;
import myau.mixin.accessor.EntityLivingBaseAccessor;
import myau.module.ClientModule;
import myau.setting.BooleanOption;
import myau.util.input.InputHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;

public class Sprint extends ClientModule {
   private static final Minecraft minecraft = Minecraft.func_71410_x();
   private boolean wasSprinting;
   public final BooleanOption fovFix = new BooleanOption("fov-fix", true);

   public Sprint() {
      super("Sprint", true, 0, true);
   }

   public boolean shouldCompensateFov(IAttributeInstance movementAttribute) {
      boolean fixEnabled = this.fovFix.getValue();
      if (!fixEnabled) {
         return false;
      } else {
         AttributeModifier boost = ((EntityLivingBaseAccessor)minecraft.field_71439_g).getSprintingSpeedBoostModifier();
         return movementAttribute.func_111127_a(boost.func_111167_a()) == null && this.wasSprinting;
      }
   }

   public boolean shouldCompensateFov(boolean sprintModifierPresent) {
      boolean fixEnabled = this.fovFix.getValue();
      return fixEnabled && !sprintModifierPresent && this.wasSprinting;
   }

   @Listen
   public void onPreTick(OnPreTick event) {
      boolean enabled = this.isEnabled();
      if (enabled) {
         InputHelper.setKeyState(minecraft.field_71474_y.field_151444_V.func_151463_i(), true);
      }
   }

   @Listen
   public void onPostTick(OnPostTick event) {
      if (this.isEnabled()) {
         this.wasSprinting = minecraft.field_71439_g.func_70051_ag();
      }
   }

   @Override
   public void onDisable() {
      this.wasSprinting = false;
      InputHelper.updateKeyState(minecraft.field_71474_y.field_151444_V.func_151463_i());
   }
}
