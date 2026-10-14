package myau.module.render;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import myau.event.Listen;
import myau.event.lifecycle.OnPostTick;
import myau.event.lifecycle.OnWorldLoad;
import myau.event.network.OnPacketReceive;
import myau.event.render.OnRenderOverlay;
import myau.module.ClientModule;
import myau.setting.BooleanOption;
import myau.setting.ChoiceOption;
import myau.setting.FloatOption;
import myau.setting.IntegerOption;
import myau.setting.TextOption;
import myau.util.chat.ChatFormat;
import myau.util.chat.ChatHelper;
import myau.util.entity.EntityHelper;
import myau.util.render.ColorHelper;
import myau.util.sound.SoundHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderPearl;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemEnderPearl;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;

public class BedTracker extends ClientModule {
   private static final Minecraft minecraft = Minecraft.func_71410_x();
   private final ScheduledExecutorService scannerExecutor = Executors.newScheduledThreadPool(1);
   private final LinkedHashMap<String, Long> lastAlertMillisByPlayer = new LinkedHashMap<>();
   private final LinkedHashSet<EntityEnderPearl> seenPearls = new LinkedHashSet<>();
   private final LinkedHashSet<String> ignoredTeammates = new LinkedHashSet<>();
   private final Color inactiveColor = new Color(ChatFormat.WHITE.getRgb());
   private final Color farColor = new Color(ChatFormat.RED.getRgb());
   private final Color warningColor = new Color(ChatFormat.YELLOW.getRgb());
   private final Color safeColor = new Color(ChatFormat.GREEN.getRgb());
   private BlockPos trackedBed = null;
   private long lastMacroMillis = -1L;
   private boolean waitingForSpawnTeleport = false;
   public final BooleanOption alerts = new BooleanOption("alerts", true);
   public final IntegerOption alertsRange = new IntegerOption("alerts-range", 48, 8, 128, this.alerts::getValue);
   public final BooleanOption alertsOnPearl = new BooleanOption("alerts-on-pearl", true);
   public final ChoiceOption alertsSound = new ChoiceOption("alerts-sound", 1, new String[]{"NONE", "MEOW", "ANVIL"}, this::isAlertsSoundVisible);
   public final IntegerOption alertsFrequency = new IntegerOption("alerts-frequency", 5, 1, 30, this::isAlertsFrequencyVisible);
   public final BooleanOption macro = new BooleanOption("macro", false);
   public final IntegerOption macroRange = new IntegerOption("macro-range", 24, 8, 128, this.macro::getValue);
   public final BooleanOption macroOnPearl = new BooleanOption("macro-on-pearl", false);
   public final TextOption macroText = new TextOption("macro-text", "/lobby", this::isMacroTextVisible);
   public final IntegerOption macroDelay = new IntegerOption("macro-delay", 1, 1, 10, this::isMacroDelayVisible);
   public final BooleanOption hud = new BooleanOption("hud", true);
   public final ChoiceOption hudPositionX = new ChoiceOption("hud-position-x", 0, new String[]{"LEFT", "MIDDLE", "RIGHT"}, this.hud::getValue);
   public final ChoiceOption hudPositionY = new ChoiceOption("hud-position-y", 0, new String[]{"TOP", "MIDDLE", "BOTTOM"}, this.hud::getValue);
   public final IntegerOption hudOffsetX = new IntegerOption("hud-offset-x", 2, 0, 255, this.hud::getValue);
   public final IntegerOption hudOffsetY = new IntegerOption("hud-offset-y", 2, 0, 255, this.hud::getValue);
   public final FloatOption hudScale = new FloatOption("hud-scale", 1.0F, 0.5F, 1.5F, this.hud::getValue);
   public final BooleanOption hudShadow = new BooleanOption("hud-shadow", true, this.hud::getValue);

   public BedTracker() {
      super("BedTracker", false, 0, true);
   }

   private void playAlertSound() {
      switch (this.alertsSound.getValue()) {
         case 1:
            SoundHelper.play("mob.cat.meow");
            break;
         case 2:
            SoundHelper.play("random.anvil_land");
      }
   }

   private Color getDistanceColor(int distance) {
      if (distance < 0) {
         return this.inactiveColor;
      } else if (distance <= 100) {
         return this.safeColor;
      } else if (distance <= 114) {
         return ColorHelper.interpolate((114 - distance) / 14.0F, this.warningColor, this.safeColor);
      } else {
         return distance > 128 ? this.farColor : ColorHelper.interpolate((128 - distance) / 14.0F, this.farColor, this.warningColor);
      }
   }

   private boolean isBed(BlockPos position) {
      return position != null && minecraft.field_71441_e.func_180495_p(position).func_177230_c() == Blocks.field_150324_C;
   }

   private boolean claimPlayerAlert(String playerName, long nowMillis) {
      Long previousAlertMillis = this.lastAlertMillisByPlayer.get(playerName);
      if (previousAlertMillis != null && previousAlertMillis + this.alertsFrequency.getValue().intValue() * 1000L > nowMillis) {
         return false;
      } else {
         this.lastAlertMillisByPlayer.put(playerName, nowMillis);
         return true;
      }
   }

   private boolean claimMacro(long nowMillis) {
      if (this.lastMacroMillis + this.macroDelay.getValue().intValue() * 1000L > nowMillis) {
         return false;
      } else {
         this.lastMacroMillis = nowMillis;
         return true;
      }
   }

   @Listen
   public void onPostTick(OnPostTick event) {
      if (this.isEnabled() && this.isBed(this.trackedBed)) {
         long nowMillis = System.currentTimeMillis();
         boolean sentAlert = false;
         boolean runMacro = false;

         for (Entity entity : minecraft.field_71441_e.field_72996_f) {
            if (entity instanceof EntityEnderPearl) {
               EntityEnderPearl pearl = (EntityEnderPearl)entity;
               if (this.seenPearls.add(pearl)) {
                  if (this.alertsOnPearl.getValue()) {
                     ChatHelper.addFormattedMessage(String.format("%s%s: &fDetected &5Ender Pearl&r &e&l⚠&r", myau.util.chat.ChatPrefix.get(), this.getName()));
                     sentAlert = true;
                  }

                  if (this.macroOnPearl.getValue() && this.claimMacro(nowMillis)) {
                     runMacro = true;
                  }
               }
            }
         }

         for (Entity entityx : minecraft.field_71441_e.field_72996_f) {
            if (entityx instanceof EntityPlayer) {
               EntityPlayer player = (EntityPlayer)entityx;
               if (!EntityHelper.isBot(player) && !this.ignoredTeammates.contains(player.func_70005_c_())) {
                  if (EntityHelper.isTeammate(player)) {
                     this.ignoredTeammates.add(player.func_70005_c_());
                  } else {
                     double distance = player.func_70011_f(
                        this.trackedBed.func_177958_n() + 0.5, this.trackedBed.func_177956_o() + 0.5, this.trackedBed.func_177952_p() + 0.5
                     );
                     String playerName = player.func_70005_c_();
                     String displayName = player.func_145748_c_().func_150254_d();
                     ItemStack heldItem = player.func_70694_bm();
                     boolean holdsEnderPearl = heldItem != null && heldItem.func_77973_b() instanceof ItemEnderPearl;
                     if (this.alerts.getValue() && distance < this.alertsRange.getValue().intValue() && this.claimPlayerAlert(playerName, nowMillis)) {
                        ChatHelper.addFormattedMessage(
                           String.format(
                              "%s%s: %s&r &fis %d blocks away from your bed &e&l⚠&r", myau.util.chat.ChatPrefix.get(), this.getName(), displayName, (int)distance + 1
                           )
                        );
                        sentAlert = true;
                     }

                     if (this.alertsOnPearl.getValue() && holdsEnderPearl && this.claimPlayerAlert(playerName, nowMillis)) {
                        ChatHelper.addFormattedMessage(
                           String.format("%s%s: %s&r &fhas &5Ender Pearl&r &e&l⚠&r", myau.util.chat.ChatPrefix.get(), this.getName(), displayName)
                        );
                        sentAlert = true;
                     }

                     boolean macroThreat = this.macro.getValue() && distance < this.macroRange.getValue().intValue()
                        || this.macroOnPearl.getValue() && holdsEnderPearl;
                     if (macroThreat && this.claimMacro(nowMillis)) {
                        runMacro = true;
                     }
                  }
               }
            }
         }

         if (sentAlert) {
            this.playAlertSound();
         }

         if (runMacro) {
            ChatHelper.addRawMessage(
               String.format(
                  ChatFormat.formatText("%s%s: &fRunning &6%s&r"), ChatFormat.formatText(myau.util.chat.ChatPrefix.get()), this.getName(), this.macroText.getValue()
               )
            );
            ChatHelper.sendMessage(this.macroText.getValue());
         }
      }
   }

   @Listen(
      priority = -50
   )
   public void onRenderOverlay(OnRenderOverlay event) {
      if (this.isEnabled()
         && this.hud.getValue()
         && minecraft.field_71441_e != null
         && minecraft.field_71439_g != null
         && !minecraft.field_71474_y.field_74330_P
         && (minecraft.field_71462_r == null || minecraft.field_71462_r instanceof GuiChat)) {
         boolean hasBed = this.isBed(this.trackedBed);
         int distance = 0;
         if (hasBed) {
            double deltaX = minecraft.field_71439_g.field_70165_t - this.trackedBed.func_177958_n();
            double deltaZ = minecraft.field_71439_g.field_70161_v - this.trackedBed.func_177952_p();
            distance = (int)Math.sqrt(deltaX * deltaX + deltaZ * deltaZ) + 1;
         }

         String status = hasBed ? "&atrue&r" : "&cfalse&r";
         String distanceText = "";
         if (hasBed) {
            String warning = distance >= 128 ? " &c&l⚠&r" : "";
            distanceText = String.format(" &7| &fDistance: &r%d%s", distance, warning);
         }

         String message = ChatFormat.formatText(String.format("&fBed: %s%s", status, distanceText));
         ScaledResolution resolution = new ScaledResolution(minecraft);
         float textWidth = minecraft.field_71466_p.func_78256_a(message);
         float textHeight = minecraft.field_71466_p.field_78288_b - 1.0F;
         float scale = this.hudScale.getValue();
         float x = this.hudOffsetX.getValue().intValue() / scale;
         float y = this.hudOffsetY.getValue().intValue() / scale;
         switch (this.hudPositionX.getValue()) {
            case 0:
               x++;
               break;
            case 1:
               x += resolution.func_78326_a() / scale / 2.0F - textWidth / 2.0F;
               break;
            case 2:
               x = resolution.func_78326_a() / scale - textWidth - x - 1.0F;
         }

         switch (this.hudPositionY.getValue()) {
            case 0:
               y++;
               break;
            case 1:
               y += resolution.func_78328_b() / scale / 2.0F - textHeight / 2.0F;
               break;
            case 2:
               y = resolution.func_78328_b() / scale - textHeight - y - 1.0F;
         }

         GlStateManager.func_179094_E();
         GlStateManager.func_179152_a(scale, scale, 1.0F);
         GlStateManager.func_179109_b(x, y, 0.0F);
         GlStateManager.func_179097_i();
         GlStateManager.func_179147_l();
         GlStateManager.func_179112_b(770, 771);
         minecraft.field_71466_p.func_175065_a(message, 0.0F, 0.0F, this.getDistanceColor(distance).getRGB(), this.hudShadow.getValue());
         GlStateManager.func_179084_k();
         GlStateManager.func_179126_j();
         GlStateManager.func_179121_F();
      }
   }

   @Listen
   public void onWorldLoad(OnWorldLoad event) {
      this.waitingForSpawnTeleport = false;
   }

   @Listen
   public void onPacketReceive(OnPacketReceive event) {
      if (this.isEnabled()) {
         if (event.getPacket() instanceof S02PacketChat) {
            String message = ((S02PacketChat)event.getPacket()).func_148915_c().func_150254_d();
            if (isBedWarsStartMessage(message)) {
               this.lastAlertMillisByPlayer.clear();
               this.seenPearls.clear();
               this.ignoredTeammates.clear();
               this.trackedBed = null;
               this.waitingForSpawnTeleport = true;
            }
         }

         if (event.getPacket() instanceof S08PacketPlayerPosLook && this.waitingForSpawnTeleport) {
            this.waitingForSpawnTeleport = false;
            this.scannerExecutor.schedule(this::scanForNearestBed, 3L, TimeUnit.SECONDS);
         }
      }
   }

   private static boolean isBedWarsStartMessage(String message) {
      return message.contains("§e§lProtect your bed and destroy the enemy bed")
         || message.contains("§e§lDestroy the enemy bed and then eliminate them")
         || message.contains("§e§lEvery few seconds brings a new surprise");
   }

   private void scanForNearestBed() {
      if (minecraft.field_71441_e != null && minecraft.field_71439_g != null) {
         int playerX = MathHelper.func_76128_c(minecraft.field_71439_g.field_70165_t);
         int playerY = MathHelper.func_76128_c(minecraft.field_71439_g.field_70163_u + minecraft.field_71439_g.func_70047_e());
         int playerZ = MathHelper.func_76128_c(minecraft.field_71439_g.field_70161_v);
         BlockPos nearestBed = null;
         int nearestDistance = -1;

         for (int x = playerX - 25; x <= playerX + 25; x++) {
            for (int y = playerY - 25; y <= playerY + 25; y++) {
               for (int z = playerZ - 25; z <= playerZ + 25; z++) {
                  BlockPos candidate = new BlockPos(x, y, z);
                  if (this.isBed(candidate)) {
                     int distance = Math.abs(x - playerX) + Math.abs(y - playerY) + Math.abs(z - playerZ);
                     if (nearestDistance < 0 || nearestDistance > distance) {
                        nearestBed = candidate;
                        nearestDistance = distance;
                     }
                  }
               }
            }
         }

         if (nearestBed != null) {
            this.trackedBed = nearestBed;
            ChatHelper.addFormattedMessage(
               String.format(
                  "%s%s: &fWhitelisted your bed at (%d, %d, %d) &a&l✔&r",
                  myau.util.chat.ChatPrefix.get(),
                  this.getName(),
                  nearestBed.func_177958_n(),
                  nearestBed.func_177956_o(),
                  nearestBed.func_177952_p()
               )
            );
            SoundHelper.play("note.pling");
         }
      }
   }

   @Override
   public void onDisable() {
      this.lastAlertMillisByPlayer.clear();
      this.seenPearls.clear();
      this.ignoredTeammates.clear();
      this.trackedBed = null;
   }

   private boolean isMacroDelayVisible() {
      return this.macro.getValue() || this.macroOnPearl.getValue();
   }

   private boolean isMacroTextVisible() {
      return this.macro.getValue() || this.macroOnPearl.getValue();
   }

   private boolean isAlertsFrequencyVisible() {
      return this.alerts.getValue() || this.alertsOnPearl.getValue();
   }

   private boolean isAlertsSoundVisible() {
      return this.alerts.getValue() || this.alertsOnPearl.getValue();
   }
}
