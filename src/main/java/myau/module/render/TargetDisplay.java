package myau.module.render;

import java.awt.Color;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import myau.core.ClientContext;
import myau.event.Listen;
import myau.event.network.OnPacketSend;
import myau.event.render.OnRenderOverlay;
import myau.module.ClientModule;
import myau.module.combat.KillAura;
import myau.setting.BooleanOption;
import myau.setting.ChoiceOption;
import myau.setting.FloatOption;
import myau.setting.IntegerOption;
import myau.setting.PercentOption;
import myau.util.Stopwatch;
import myau.util.chat.ChatFormat;
import myau.util.entity.EntityHelper;
import myau.util.math.Maths;
import myau.util.render.ColorHelper;
import myau.util.render.RenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C02PacketUseEntity.Action;
import net.minecraft.util.ResourceLocation;

public class TargetDisplay extends ClientModule {
   private static final Minecraft minecraft = Minecraft.func_71410_x();
   private static final DecimalFormat heartFormat = new DecimalFormat("0.0", new DecimalFormatSymbols(Locale.US));
   private static final DecimalFormat differenceFormat = new DecimalFormat("+0.0;-0.0", new DecimalFormatSymbols(Locale.US));
   private final Stopwatch targetTimer = new Stopwatch();
   private final Stopwatch animationTimer = new Stopwatch();
   private EntityLivingBase lastAttackedTarget;
   private EntityLivingBase displayedTarget;
   private ResourceLocation skin;
   private float previousHearts;
   private float targetHearts;
   private float maximumHearts;
   public final ChoiceOption color = new ChoiceOption("color", 0, new String[]{"DEFAULT", "HUD"});
   public final ChoiceOption positionX = new ChoiceOption("position-x", 1, new String[]{"LEFT", "MIDDLE", "RIGHT"});
   public final ChoiceOption positionY = new ChoiceOption("position-y", 1, new String[]{"TOP", "MIDDLE", "BOTTOM"});
   public final FloatOption scale = new FloatOption("scale", 1.0F, 0.5F, 1.5F);
   public final IntegerOption offsetX = new IntegerOption("offset-x", 0, -255, 255);
   public final IntegerOption offsetY = new IntegerOption("offset-y", 40, -255, 255);
   public final PercentOption background = new PercentOption("background", 25);
   public final BooleanOption head = new BooleanOption("head", true);
   public final BooleanOption indicator = new BooleanOption("indicator", true);
   public final BooleanOption outline = new BooleanOption("outline", false);
   public final BooleanOption animations = new BooleanOption("animations", true);
   public final BooleanOption shadow = new BooleanOption("shadow", true);
   public final BooleanOption kaOnly = new BooleanOption("ka-only", true);
   public final BooleanOption chatPreview = new BooleanOption("chat-preview", false);

   public TargetDisplay() {
      super("TargetHUD", false, 0, true);
   }

   private EntityLivingBase selectTarget() {
      KillAura combatModule = (KillAura)ClientContext.moduleManager.modules.get(KillAura.class);
      boolean combatEnabled = combatModule.isEnabled();
      if (combatEnabled && combatModule.isCombatInputAllowed() && EntityHelper.isLoaded(combatModule.getTargetEntity())) {
         return combatModule.getTargetEntity();
      } else if (!this.kaOnly.getValue() && !this.targetTimer.hasElapsed(1500L) && EntityHelper.isLoaded(this.lastAttackedTarget)) {
         return this.lastAttackedTarget;
      } else {
         return this.chatPreview.getValue() && minecraft.field_71462_r instanceof GuiChat ? minecraft.field_71439_g : null;
      }
   }

   private ResourceLocation skinFor(EntityLivingBase target) {
      if (target instanceof EntityPlayer) {
         NetworkPlayerInfo info = minecraft.func_147114_u().func_175104_a(target.func_70005_c_());
         if (info != null) {
            return info.func_178837_g();
         }
      }

      return null;
   }

   private Color colorFor(EntityLivingBase target) {
      boolean player = target instanceof EntityPlayer;
      if (player) {
         if (EntityHelper.isFriend((EntityPlayer)target)) {
            return ClientContext.friendList.color();
         }

         if (EntityHelper.isEnemy((EntityPlayer)target)) {
            return ClientContext.enemyList.color();
         }
      }

      switch (this.color.getValue()) {
         case 0:
            return player ? EntityHelper.teamColor((EntityPlayer)target, 1.0F) : new Color(-1);
         case 1:
            int hudColor = ((HeadsUpDisplay)ClientContext.moduleManager.modules.get(HeadsUpDisplay.class)).getColor(System.currentTimeMillis()).getRGB();
            return new Color(hudColor);
         default:
            return new Color(-1);
      }
   }

   @Listen
   public void onRenderOverlay(OnRenderOverlay event) {
      if (this.isEnabled()) {
         if (minecraft.field_71439_g != null) {
            EntityLivingBase previousTarget = this.displayedTarget;
            this.displayedTarget = this.selectTarget();
            if (this.displayedTarget != null) {
               float playerHearts = (minecraft.field_71439_g.func_110143_aJ() + minecraft.field_71439_g.func_110139_bj()) / 2.0F;
               float absorptionHearts = this.displayedTarget.func_110139_bj() / 2.0F;
               float totalHearts = this.displayedTarget.func_110143_aJ() / 2.0F + absorptionHearts;
               if (this.displayedTarget != previousTarget) {
                  this.skin = null;
                  this.animationTimer.setLastResetMillis(0L);
                  this.previousHearts = totalHearts;
                  this.targetHearts = totalHearts;
               }

               boolean animateHealth = this.animations.getValue();
               if (!animateHealth || this.animationTimer.hasElapsed(150L)) {
                  this.previousHearts = this.targetHearts;
                  this.targetHearts = totalHearts;
                  this.maximumHearts = this.displayedTarget.func_110138_aP() / 2.0F;
                  if (this.previousHearts != this.targetHearts) {
                     this.animationTimer.reset();
                  }
               }

               ResourceLocation currentSkin = this.skinFor(this.displayedTarget);
               if (currentSkin != null) {
                  this.skin = currentSkin;
               }

               float elapsed = (float)Math.min(Math.max(this.animationTimer.elapsedMillis(), 0L), 150L);
               float interpolatedHearts = Maths.lerpFloat(this.previousHearts, this.targetHearts, elapsed / 150.0F);
               float healthFraction = Math.min(Math.max(interpolatedHearts / this.maximumHearts, 0.0F), 1.0F);
               Color targetColor = this.colorFor(this.displayedTarget);
               Color healthColor = this.color.getValue() == 0 ? ColorHelper.healthGradient(healthFraction) : targetColor;
               float advantage = Math.min(Math.max((playerHearts - totalHearts + 1.0F) / 2.0F, 0.0F), 1.0F);
               Color advantageColor = ColorHelper.healthGradient(advantage);
               ScaledResolution resolution = new ScaledResolution(minecraft);
               String nameLabel = ChatFormat.formatText(String.format("&r%s&r", EntityHelper.formattedName(this.displayedTarget)));
               int nameWidth = minecraft.field_71466_p.func_78256_a(nameLabel);
               String healthLabel = ChatFormat.formatText(String.format("&r&f%s%s❤&r", heartFormat.format(totalHearts), absorptionHearts > 0.0F ? "&6" : "&c"));
               int healthWidth = minecraft.field_71466_p.func_78256_a(healthLabel);
               String resultLabel = ChatFormat.formatText(
                  String.format("&r&l%s&r", totalHearts == playerHearts ? "D" : (totalHearts < playerHearts ? "W" : "L"))
               );
               int resultWidth = minecraft.field_71466_p.func_78256_a(resultLabel);
               String differenceLabel = ChatFormat.formatText(
                  String.format("&r%s&r", totalHearts == playerHearts ? "0.0" : differenceFormat.format(playerHearts - totalHearts))
               );
               int differenceWidth = minecraft.field_71466_p.func_78256_a(differenceLabel);
               float firstRowWidth = nameWidth + (this.indicator.getValue() ? 2.0F + resultWidth + 2.0F : 0.0F);
               float secondRowWidth = healthWidth + (this.indicator.getValue() ? 2.0F + differenceWidth + 2.0F : 0.0F);
               float contentWidth = Math.max(firstRowWidth, secondRowWidth);
               float headWidth = this.head.getValue() && this.skin != null ? 25.0F : 0.0F;
               float width = Math.max(headWidth + 70.0F, headWidth + 2.0F + contentWidth + 2.0F);
               float height = 27.0F;
               float x = this.offsetX.getValue().floatValue() / this.scale.getValue();
               switch (this.positionX.getValue()) {
                  case 1:
                     x += resolution.func_78326_a() / this.scale.getValue() / 2.0F - width / 2.0F;
                     break;
                  case 2:
                     x *= -1.0F;
                     x += resolution.func_78326_a() / this.scale.getValue() - width;
               }

               float y = this.offsetY.getValue().floatValue() / this.scale.getValue();
               switch (this.positionY.getValue()) {
                  case 1:
                     y += resolution.func_78328_b() / this.scale.getValue() / 2.0F - height / 2.0F;
                     break;
                  case 2:
                     y *= -1.0F;
                     y += resolution.func_78328_b() / this.scale.getValue() - height;
               }

               GlStateManager.func_179094_E();
               GlStateManager.func_179152_a(this.scale.getValue(), this.scale.getValue(), 0.0F);
               GlStateManager.func_179109_b(x, y, -450.0F);
               RenderHelper.beginOverlay();
               int backgroundColor = new Color(0.0F, 0.0F, 0.0F, this.background.getValue().intValue() / 100.0F).getRGB();
               int outlineColor = this.outline.getValue() ? targetColor.getRGB() : new Color(0, 0, 0, 0).getRGB();
               RenderHelper.drawBorderedRect(0.0F, 0.0F, width, height, 1.5F, backgroundColor, outlineColor);
               RenderHelper.fillRect(headWidth + 2.0F, 22.0F, width - 2.0F, 25.0F, ColorHelper.scaleBrightness(healthColor, 0.2F).getRGB());
               RenderHelper.fillRect(
                  headWidth + 2.0F, 22.0F, headWidth + 2.0F + healthFraction * (width - 2.0F - headWidth - 2.0F), 25.0F, healthColor.getRGB()
               );
               RenderHelper.endOverlay();
               GlStateManager.func_179097_i();
               GlStateManager.func_179147_l();
               GlStateManager.func_179112_b(770, 771);
               minecraft.field_71466_p.func_175065_a(nameLabel, headWidth + 2.0F, 2.0F, -1, this.shadow.getValue());
               minecraft.field_71466_p.func_175065_a(healthLabel, headWidth + 2.0F, 12.0F, -1, this.shadow.getValue());
               if (this.indicator.getValue()) {
                  minecraft.field_71466_p.func_175065_a(resultLabel, width - 2.0F - resultWidth, 2.0F, advantageColor.getRGB(), this.shadow.getValue());
                  minecraft.field_71466_p
                     .func_175065_a(
                        differenceLabel,
                        width - 2.0F - differenceWidth,
                        12.0F,
                        ColorHelper.scaleBrightness(advantageColor, 0.8F).getRGB(),
                        this.shadow.getValue()
                     );
               }

               if (this.head.getValue() && this.skin != null) {
                  GlStateManager.func_179124_c(1.0F, 1.0F, 1.0F);
                  minecraft.func_110434_K().func_110577_a(this.skin);
                  Gui.func_152125_a(2, 2, 8.0F, 8.0F, 8, 8, 23, 23, 64.0F, 64.0F);
                  Gui.func_152125_a(2, 2, 40.0F, 8.0F, 8, 8, 23, 23, 64.0F, 64.0F);
                  GlStateManager.func_179124_c(1.0F, 1.0F, 1.0F);
               }

               GlStateManager.func_179084_k();
               GlStateManager.func_179126_j();
               GlStateManager.func_179121_F();
            }
         }
      }
   }

   @Listen
   public void onPacketSend(OnPacketSend event) {
      boolean attack = this.isEnabled();
      if (attack && !event.isCancelled()) {
         attack = event.getPacket() instanceof C02PacketUseEntity;
         if (attack) {
            Packet raw = event.getPacket();
            C02PacketUseEntity packet = (C02PacketUseEntity)raw;
            if (packet.func_149565_c() == Action.ATTACK) {
               Entity entity = packet.func_149564_a(minecraft.field_71441_e);
               if (entity instanceof EntityLivingBase && !(entity instanceof EntityArmorStand)) {
                  this.targetTimer.reset();
                  this.lastAttackedTarget = (EntityLivingBase)entity;
               }
            }
         }
      }
   }
}
