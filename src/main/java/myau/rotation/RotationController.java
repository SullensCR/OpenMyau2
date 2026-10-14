package myau.rotation;

import myau.event.Listen;
import myau.event.lifecycle.OnPreTick;
import myau.event.render.OnRenderWorld;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MathHelper;

public class RotationController {
   private static final Minecraft minecraft = Minecraft.func_71410_x();
   private float lastPartialTicks;
   private float yawDelta;
   private float pitchDelta;
   private int requestPriority;
   private boolean requestActive;

   public RotationController() {
      this.resetRotationRequest();
   }

   private void applyRotationDelta(float partialTicks) {
      if (minecraft.field_71439_g != null && !Float.isNaN(this.yawDelta) && !Float.isNaN(this.pitchDelta) && !Float.isNaN(this.lastPartialTicks)) {
         float partialDelta = partialTicks - this.lastPartialTicks;
         float yawStep = this.yawDelta * partialDelta;
         if (yawStep != 0.0F) {
            minecraft.field_71439_g.field_70126_B = minecraft.field_71439_g.field_70177_z;
            minecraft.field_71439_g.field_70177_z += yawStep;
         }

         float pitchStep = this.pitchDelta * partialDelta;
         if (pitchStep != 0.0F) {
            minecraft.field_71439_g.field_70127_C = minecraft.field_71439_g.field_70125_A;
            minecraft.field_71439_g.field_70125_A += pitchStep;
            minecraft.field_71439_g.field_70125_A = MathHelper.func_76131_a(minecraft.field_71439_g.field_70125_A, -90.0F, 90.0F);
         }

         this.lastPartialTicks = partialTicks;
      }
   }

   private void resetRotationRequest() {
      this.lastPartialTicks = Float.NaN;
      this.yawDelta = Float.NaN;
      this.pitchDelta = Float.NaN;
      this.requestPriority = Integer.MIN_VALUE;
      this.requestActive = false;
   }

   public void requestRotation(float yaw, float pitch, int priority, boolean active) {
      if (this.requestPriority <= priority) {
         this.requestPriority = priority;
         this.yawDelta = MathHelper.func_76142_g(yaw - minecraft.field_71439_g.field_70177_z);
         this.pitchDelta = pitch - minecraft.field_71439_g.field_70125_A;
         this.lastPartialTicks = 0.0F;
         this.requestActive = active;
      }
   }

   public boolean isRequestActive() {
      return this.requestActive;
   }

   @Listen(
      priority = 100
   )
   public void onPreTick(OnPreTick event) {
      this.applyRotationDelta(1.0F);
      this.resetRotationRequest();
   }

   @Listen(
      priority = 100
   )
   public void onRenderWorld(OnRenderWorld event) {
      this.applyRotationDelta(event.getPartialTicks());
   }
}
