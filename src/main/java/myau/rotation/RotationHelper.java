package myau.rotation;

import myau.mixin.accessor.EntityAccessor;
import myau.util.math.Maths;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

public class RotationHelper {
   private static final Minecraft minecraft = Minecraft.func_71410_x();

   public static float nearestEquivalentAngle(float targetAngle, float referenceAngle) {
      return referenceAngle + MathHelper.func_76142_g(targetAngle - referenceAngle);
   }

   public static float clampAngleChange(float change, float maximumChange) {
      maximumChange = MathHelper.func_76131_a(maximumChange, 0.0F, 180.0F);
      if (change > maximumChange) {
         return maximumChange;
      } else {
         if (change < -maximumChange) {
            change = -maximumChange;
         }

         return change;
      }
   }

   public static float scaleAngleChange(float change, float factor) {
      return change * (0.5F + 0.5F * (1.0F - MathHelper.func_76131_a(factor, 0.0F, 1.0F)));
   }

   public static float snapToMouseSensitivity(float targetAngle, float referenceAngle) {
      float change = MathHelper.func_76142_g(targetAngle - referenceAngle);
      float sensitivity = minecraft.field_71474_y.field_74341_c * 0.6F + 0.2F;
      float increment = sensitivity * sensitivity * sensitivity * 8.0F;
      double roundedChange = (float)Math.round(change / (increment * 0.15)) * increment * 0.15;
      return (float)(referenceAngle + roundedChange);
   }

   public static float[] aimAtBounds(AxisAlignedBB bounds) {
      Vec3 eyePosition = minecraft.field_71439_g.func_174824_e(1.0F);
      Vec3 center = boundsCenter(bounds);
      Vec3 closestPoint = closestPoint(eyePosition, bounds);
      double deltaX = Maths.lerpDouble(center.field_72450_a, closestPoint.field_72450_a, 0.875) - eyePosition.field_72450_a;
      double deltaY = closestPoint.field_72448_b - eyePosition.field_72448_b;
      double deltaZ = Maths.lerpDouble(center.field_72449_c, closestPoint.field_72449_c, 0.875) - eyePosition.field_72449_c;
      float[] rotation = rotationsFromDelta(deltaX, deltaY, deltaZ);
      rotation[1] += 10.0F
         * MathHelper.func_76131_a((float)((eyePosition.field_72448_b - center.field_72448_b) / (center.field_72448_b - bounds.field_72338_b)), -0.5F, 0.5F);
      return rotation;
   }

   public static float[] rotationsFromDelta(double deltaX, double deltaY, double deltaZ) {
      double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
      float yaw = (float)(Math.atan2(deltaZ, deltaX) * 180.0 / Math.PI) - 90.0F;
      float pitch = (float)(-Math.atan2(deltaY, horizontalDistance) * 180.0 / Math.PI);
      return new float[]{yaw, pitch};
   }

   public static Vec3 boundsCenter(AxisAlignedBB bounds) {
      return new Vec3(
         (bounds.field_72340_a + bounds.field_72336_d) / 2.0,
         (bounds.field_72338_b + bounds.field_72337_e) / 2.0,
         (bounds.field_72339_c + bounds.field_72334_f) / 2.0
      );
   }

   public static Vec3 closestPoint(Vec3 position, AxisAlignedBB bounds) {
      double[] coordinates = new double[]{position.field_72450_a, position.field_72448_b, position.field_72449_c};
      double[] minimum = new double[]{bounds.field_72340_a, bounds.field_72338_b, bounds.field_72339_c};
      double[] maximum = new double[]{bounds.field_72336_d, bounds.field_72337_e, bounds.field_72334_f};

      for (int axis = 0; axis < 3; axis++) {
         if (coordinates[axis] > maximum[axis]) {
            coordinates[axis] = maximum[axis];
         } else if (coordinates[axis] < minimum[axis]) {
            coordinates[axis] = minimum[axis];
         }
      }

      return new Vec3(coordinates[0], coordinates[1], coordinates[2]);
   }

   public static double distanceToEntity(Entity entity) {
      float collisionBorder = entity.func_70111_Y();
      AxisAlignedBB bounds = entity.func_174813_aQ().func_72314_b(collisionBorder, collisionBorder, collisionBorder);
      return distanceToBounds(bounds);
   }

   public static double distanceToEntityFrom(Entity entity, Vec3 position) {
      float collisionBorder = entity.func_70111_Y();
      AxisAlignedBB bounds = entity.func_174813_aQ().func_72314_b(collisionBorder, collisionBorder, collisionBorder);
      return distanceToBoundsFrom(bounds, position);
   }

   public static double distanceToBounds(AxisAlignedBB bounds) {
      return distanceToBoundsFrom(bounds, minecraft.field_71439_g.func_174824_e(1.0F));
   }

   public static double distanceToBoundsFrom(AxisAlignedBB bounds, Vec3 position) {
      if (bounds.func_72318_a(position)) {
         return 0.0;
      } else {
         Vec3 closestPoint = closestPoint(position, bounds);
         double deltaX = closestPoint.field_72450_a - position.field_72450_a;
         double deltaY = closestPoint.field_72448_b - position.field_72448_b;
         double deltaZ = closestPoint.field_72449_c - position.field_72449_c;
         return Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
      }
   }

   public static float entityAimError(Entity entity) {
      Vec3 eyePosition = minecraft.field_71439_g.func_174824_e(1.0F);
      float collisionBorder = entity.func_70111_Y();
      AxisAlignedBB bounds = entity.func_174813_aQ().func_72314_b(collisionBorder, collisionBorder, collisionBorder);
      return boundsAimError(bounds, eyePosition);
   }

   public static float boundsAimError(AxisAlignedBB bounds, Vec3 position) {
      if (bounds.func_72318_a(position)) {
         return 0.0F;
      } else {
         double traceDistance = 30.0
            + bounds.field_72336_d
            - bounds.field_72340_a
            + bounds.field_72334_f
            - bounds.field_72339_c
            + bounds.field_72337_e
            - bounds.field_72338_b;
         MovingObjectPosition hit = intersectBoundsFrom(
            bounds, position, minecraft.field_71439_g.field_70177_z, minecraft.field_71439_g.field_70125_A, traceDistance
         );
         if (hit != null) {
            return 0.0F;
         } else {
            float[] rotation = aimAtBounds(bounds);
            float yawError = Math.abs(MathHelper.func_76142_g(rotation[0] - minecraft.field_71439_g.field_70177_z));
            float pitchError = Math.abs(rotation[1] - minecraft.field_71439_g.field_70125_A);
            return Math.max(yawError * 2.0F, pitchError * 2.0F);
         }
      }
   }

   public static float horizontalYawError(double startX, double startZ, double endX, double endZ) {
      float yaw = (float)(Math.atan2(endZ - startZ, endX - startX) * 180.0 / Math.PI) - 90.0F;
      return MathHelper.func_76142_g(yaw - minecraft.field_71439_g.field_70177_z);
   }

   public static MovingObjectPosition traceBlocks(float yaw, float pitch, double distance, float partialTicks) {
      Vec3 eyePosition = minecraft.field_71439_g.func_174824_e(partialTicks);
      return traceBlocksFrom(eyePosition, yaw, pitch, distance);
   }

   public static MovingObjectPosition traceBlocksFrom(Vec3 position, float yaw, float pitch, double distance) {
      Vec3 direction = ((EntityAccessor)minecraft.field_71439_g).callGetVectorForRotation(pitch, yaw);
      Vec3 end = position.func_72441_c(direction.field_72450_a * distance, direction.field_72448_b * distance, direction.field_72449_c * distance);
      return minecraft.field_71441_e.func_72933_a(position, end);
   }

   public static MovingObjectPosition traceToEntity(Entity entity) {
      Vec3 eyePosition = minecraft.field_71439_g.func_174824_e(1.0F);
      float collisionBorder = entity.func_70111_Y();
      Vec3 closestPoint = closestPoint(eyePosition, entity.func_174813_aQ().func_72314_b(collisionBorder, collisionBorder, collisionBorder));
      return minecraft.field_71441_e.func_72933_a(eyePosition, closestPoint);
   }

   public static MovingObjectPosition intersectBounds(AxisAlignedBB bounds, float yaw, float pitch, double distance) {
      Vec3 eyePosition = minecraft.field_71439_g.func_174824_e(1.0F);
      return intersectBoundsFrom(bounds, eyePosition, yaw, pitch, distance);
   }

   public static MovingObjectPosition intersectBoundsFrom(AxisAlignedBB bounds, Vec3 position, float yaw, float pitch, double distance) {
      Vec3 direction = ((EntityAccessor)minecraft.field_71439_g).callGetVectorForRotation(pitch, yaw);
      Vec3 end = position.func_72441_c(direction.field_72450_a * distance, direction.field_72448_b * distance, direction.field_72449_c * distance);
      return bounds.func_72327_a(position, end);
   }

   public static boolean isBoundsRayObstructed(AxisAlignedBB bounds, Vec3 position, float yaw, float pitch, double distance) {
      if (bounds.func_72318_a(position)) {
         return false;
      } else {
         MovingObjectPosition boxHit = intersectBoundsFrom(bounds, position, yaw, pitch, distance);
         if (boxHit == null) {
            return true;
         } else {
            MovingObjectPosition blockHit = traceBlocksFrom(position, yaw, pitch, distance);
            return blockHit == null ? false : position.func_72438_d(boxHit.field_72307_f) > position.func_72438_d(blockHit.field_72307_f);
         }
      }
   }
}
