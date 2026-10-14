package myau.module.misc;

import java.awt.Color;
import java.util.List;
import java.util.stream.Collectors;
import myau.core.ClientContext;
import myau.event.Listen;
import myau.event.lifecycle.OnPostTick;
import myau.event.lifecycle.OnPreTick;
import myau.event.network.OnPacketSend;
import myau.event.render.OnRenderWorld;
import myau.mixin.accessor.PlayerControllerMPAccessor;
import myau.mixin.accessor.RenderManagerAccessor;
import myau.module.ClientModule;
import myau.module.render.HeadsUpDisplay;
import myau.module.world.BedNuker;
import myau.rotation.RotationHelper;
import myau.setting.BooleanOption;
import myau.setting.ChoiceOption;
import myau.setting.FloatOption;
import myau.setting.IntegerOption;
import myau.util.entity.EntityHelper;
import myau.util.item.ItemHelper;
import myau.util.math.Maths;
import myau.util.render.RenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C07PacketPlayerDigging.Action;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;

public class LagRange extends ClientModule {
   private static final Minecraft minecraft = Minecraft.func_71410_x();
   private int delayTicks = -1;
   private long remainingDelayMillis;
   private boolean delaying;
   private Vec3 previousPosition;
   private Vec3 currentPosition;
   public final IntegerOption delay = new IntegerOption("delay", 100, 0, 1000);
   public final FloatOption range = new FloatOption("range", 10.0F, 3.0F, 100.0F);
   public final BooleanOption weaponsOnly = new BooleanOption("weapons-only", true);
   public final BooleanOption allowTools = new BooleanOption("allow-tools", false, this.weaponsOnly::getValue);
   public final BooleanOption botCheck = new BooleanOption("bot-check", true);
   public final BooleanOption teams = new BooleanOption("teams", true);
   public final ChoiceOption showPosition = new ChoiceOption("show-position", 0, new String[]{"NONE", "DEFAULT", "HUD"});

   public LagRange() {
      super("LagRange", false, 0);
   }

   private boolean isTarget(EntityPlayer player) {
      if (player == minecraft.field_71439_g
         || player == minecraft.field_71439_g.field_70154_o
         || player == minecraft.func_175606_aa()
         || player == minecraft.func_175606_aa().field_70154_o) {
         return false;
      } else if (player.field_70725_aQ > 0 || EntityHelper.isFriend(player)) {
         return false;
      } else if (EntityHelper.isEnemy(player)) {
         return true;
      } else {
         return this.teams.getValue() && EntityHelper.isTeammate(player) ? false : !this.botCheck.getValue() || !EntityHelper.isBot(player);
      }
   }

   private boolean shouldFlush(Packet packet) {
      if (packet instanceof C02PacketUseEntity) {
         return true;
      } else if (packet instanceof C07PacketPlayerDigging) {
         return ((C07PacketPlayerDigging)packet).func_180762_c() != Action.RELEASE_USE_ITEM;
      } else if (!(packet instanceof C08PacketPlayerBlockPlacement)) {
         return false;
      } else {
         ItemStack stack = ((C08PacketPlayerBlockPlacement)packet).func_149574_g();
         return stack == null || !(stack.func_77973_b() instanceof ItemSword);
      }
   }

   @Listen(
      priority = -50
   )
   public void onPreTick(OnPreTick event) {
      if (this.isEnabled()) {
         ClientContext.packetDelayManager.setDelayTicks(0);
         this.delaying = false;
         BedNuker conflictingModule = (BedNuker)ClientContext.moduleManager.modules.get(BedNuker.class);
         if (conflictingModule.isEnabled() && conflictingModule.isControllingInteractions()) {
            this.delayTicks = -1;
         } else if (!((PlayerControllerMPAccessor)minecraft.field_71442_b).getIsHittingBlock()
            && (!minecraft.field_71439_g.func_71039_bw() || minecraft.field_71439_g.func_70632_aY())
            && (!this.weaponsOnly.getValue() || ItemHelper.isHoldingWeapon() || this.allowTools.getValue() && ItemHelper.isHoldingTool())) {
            List<EntityPlayer> targets = minecraft.field_71441_e
               .field_72996_f
               .stream()
               .filter(entity -> entity instanceof EntityPlayer)
               .map(EntityPlayer.class::cast)
               .filter(this::isTarget)
               .collect(Collectors.toList());
            if (targets.isEmpty()) {
               this.delayTicks = -1;
            } else {
               double eyeHeight = minecraft.field_71439_g.func_70047_e();
               Vec3 bufferedEyes = ClientContext.packetDelayManager.lastReleasedPosition().func_72441_c(0.0, eyeHeight, 0.0);
               Vec3 previousEyes = new Vec3(
                  minecraft.field_71439_g.field_70142_S, minecraft.field_71439_g.field_70137_T + eyeHeight, minecraft.field_71439_g.field_70136_U
               );
               Vec3 currentEyes = new Vec3(
                  minecraft.field_71439_g.field_70165_t, minecraft.field_71439_g.field_70163_u + eyeHeight, minecraft.field_71439_g.field_70161_v
               );

               for (EntityPlayer target : targets) {
                  double currentDistance = RotationHelper.distanceToEntityFrom(target, currentEyes);
                  if (!(currentDistance > this.range.getValue().floatValue())) {
                     double previousDistance = RotationHelper.distanceToEntityFrom(target, previousEyes);
                     double bufferedDistance = RotationHelper.distanceToEntityFrom(target, bufferedEyes);
                     if (currentDistance < previousDistance || currentDistance < bufferedDistance) {
                        this.delayTicks = 0;

                        for (this.remainingDelayMillis = this.remainingDelayMillis + this.delay.getValue().intValue();
                           this.remainingDelayMillis > 0L;
                           this.remainingDelayMillis -= 50L
                        ) {
                           this.delayTicks++;
                        }

                        ClientContext.packetDelayManager.setDelayTicks(this.delayTicks);
                        this.delaying = true;
                        break;
                     }
                  }
               }
            }
         } else {
            this.delayTicks = -1;
         }
      }
   }

   @Listen(
      priority = -50
   )
   public void onPostTick(OnPostTick event) {
      if (this.isEnabled()) {
         Vec3 position = ClientContext.packetDelayManager.lastReleasedPosition();
         this.previousPosition = this.currentPosition == null ? position : this.currentPosition;
         this.currentPosition = position;
      }
   }

   @Listen
   public void onPacketSend(OnPacketSend event) {
      boolean flush = this.isEnabled();
      if (flush && !event.isCancelled()) {
         flush = this.shouldFlush(event.getPacket());
         if (flush) {
            ClientContext.packetDelayManager.setDelayTicks(0);
            this.delayTicks = -1;
         }
      }
   }

   @Listen(
      priority = 50
   )
   public void onRenderWorld(OnRenderWorld event) {
      boolean draw = this.isEnabled();
      if (draw && this.showPosition.getValue() != 0 && minecraft.field_71474_y.field_74320_O != 0) {
         draw = this.delaying;
         if (draw && this.previousPosition != null) {
            if (this.currentPosition != null) {
               Color positionColor = new Color(-1);
               switch (this.showPosition.getValue()) {
                  case 1:
                     positionColor = EntityHelper.teamColor(minecraft.field_71439_g, 1.0F);
                     break;
                  case 2:
                     positionColor = ((HeadsUpDisplay)ClientContext.moduleManager.modules.get(HeadsUpDisplay.class)).getColor(System.currentTimeMillis());
               }

               double x = Maths.lerpDouble(this.previousPosition.field_72450_a, this.currentPosition.field_72450_a, event.getPartialTicks());
               double y = Maths.lerpDouble(this.previousPosition.field_72448_b, this.currentPosition.field_72448_b, event.getPartialTicks());
               double z = Maths.lerpDouble(this.previousPosition.field_72449_c, this.currentPosition.field_72449_c, event.getPartialTicks());
               float border = minecraft.field_71439_g.func_70111_Y();
               AxisAlignedBB bounds = new AxisAlignedBB(
                     x - minecraft.field_71439_g.field_70130_N / 2.0,
                     y,
                     z - minecraft.field_71439_g.field_70130_N / 2.0,
                     x + minecraft.field_71439_g.field_70130_N / 2.0,
                     y + minecraft.field_71439_g.field_70131_O,
                     z + minecraft.field_71439_g.field_70130_N / 2.0
                  )
                  .func_72314_b(border, border, border)
                  .func_72317_d(
                     -((RenderManagerAccessor)minecraft.func_175598_ae()).getRenderPosX(),
                     -((RenderManagerAccessor)minecraft.func_175598_ae()).getRenderPosY(),
                     -((RenderManagerAccessor)minecraft.func_175598_ae()).getRenderPosZ()
                  );
               RenderHelper.beginOverlay();
               RenderHelper.fillBounds(bounds, positionColor.getRed(), positionColor.getGreen(), positionColor.getBlue(), 63);
               RenderHelper.endOverlay();
            }
         }
      }
   }

   @Override
   public void onDisable() {
      ClientContext.packetDelayManager.setDelayTicks(0);
      this.delayTicks = -1;
      this.remainingDelayMillis = 0L;
      this.delaying = false;
      this.previousPosition = null;
      this.currentPosition = null;
   }

   @Override
   public String[] getHudInfo() {
      return new String[]{String.format("%dms", this.delay.getValue())};
   }
}
