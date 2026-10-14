package myau.module.render;

import java.awt.Color;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import myau.core.ClientContext;
import myau.event.Listen;
import myau.event.render.OnRenderOverlay;
import myau.mixin.accessor.GuiChatAccessor;
import myau.module.ClientModule;
import myau.network.OutboundQueueOwner;
import myau.setting.BooleanOption;
import myau.setting.ChoiceOption;
import myau.setting.ColorOption;
import myau.setting.FloatOption;
import myau.setting.IntegerOption;
import myau.setting.PercentOption;
import myau.util.chat.ChatFormat;
import myau.util.render.ColorHelper;
import myau.util.render.RenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;

public class HeadsUpDisplay extends ClientModule {
   private static final Minecraft minecraft = Minecraft.func_71410_x();
   public final ChoiceOption color = new ChoiceOption("color", 3, new String[]{"RAINBOW", "CHROMA", "ASTOLFO", "CUSTOM1", "CUSTOM12", "CUSTOM123"});
   public final FloatOption colorSpeed = new FloatOption("color-speed", 1.0F, 0.5F, 1.5F);
   public final PercentOption colorSaturation = new PercentOption("color-saturation", 50);
   public final PercentOption colorBrightness = new PercentOption("color-brightness", 100);
   public final ColorOption customColor1 = new ColorOption("custom-color-1", 16777215, this::usesFirstCustomColor);
   public final ColorOption customColor2 = new ColorOption("custom-color-2", 16777215, this::usesSecondCustomColor);
   public final ColorOption customColor3 = new ColorOption("custom-color-3", 16777215, this::usesThirdCustomColor);
   public final ChoiceOption positionX = new ChoiceOption("position-x", 0, new String[]{"LEFT", "RIGHT"});
   public final ChoiceOption positionY = new ChoiceOption("position-y", 0, new String[]{"TOP", "BOTTOM"});
   public final IntegerOption offsetX = new IntegerOption("offset-x", 2, 0, 255);
   public final IntegerOption offsetY = new IntegerOption("offset-y", 2, 0, 255);
   public final FloatOption scale = new FloatOption("scale", 1.0F, 0.5F, 1.5F);
   public final PercentOption background = new PercentOption("background", 25);
   public final BooleanOption bar = new BooleanOption("bar", true);
   public final BooleanOption shadow = new BooleanOption("shadow", true);
   public final BooleanOption suffixes = new BooleanOption("suffixes", true);
   public final BooleanOption lowerCase = new BooleanOption("lower-case", false);
   public final BooleanOption chatOutline = new BooleanOption("chat-outline", true);
   public final BooleanOption blinkTimer = new BooleanOption("blink-timer", true);
   public final BooleanOption toggleSounds = new BooleanOption("toggle-sounds", true);
   public final BooleanOption toggleAlerts = new BooleanOption("toggle-alerts", true);

   public HeadsUpDisplay() {
      super("HUD", true, 0, true);
   }

   private String displayName(ClientModule module) {
      String name = module.getName();
      return this.lowerCase.getValue() ? name.toLowerCase(Locale.ROOT) : name;
   }

   private String[] displaySuffixes(ClientModule module) {
      String[] values = module.getHudInfo();
      boolean lowercase = this.lowerCase.getValue();
      if (!lowercase) {
         return values;
      } else {
         for (int index = 0; index < values.length; index++) {
            values[index] = values[index].toLowerCase();
         }

         return values;
      }
   }

   private int displayWidth(String name, String[] values) {
      int width = minecraft.field_71466_p.func_78256_a(name);
      boolean includeSuffixes = this.suffixes.getValue();
      if (includeSuffixes) {
         for (String suffix : values) {
            width += 3 + minecraft.field_71466_p.func_78256_a(suffix);
         }
      }

      return width;
   }

   private int moduleWidth(ClientModule module) {
      return this.displayWidth(this.displayName(module), this.displaySuffixes(module));
   }

   private float colorPhase(long time, long row) {
      long period = (long)(3000.0 / Math.pow(Math.min(Math.max(0.5F, this.colorSpeed.getValue()), 1.5F), 3.0));
      return 1.0F - (float)(Math.abs(time - row * 300L) % period) / (float)period;
   }

   public Color getColor(long time) {
      return this.getColorForRow(time, 0L);
   }

   public Color getColorForRow(long time, long row) {
      Color base = Color.white;
      int mode = this.color.getValue();
      switch (mode) {
         case 0:
            base = ColorHelper.fromHSB(this.colorPhase(time, row), 1.0F, 1.0F);
            break;
         case 1:
            base = ColorHelper.fromHSB(this.colorPhase(time / 3L, 0L), 1.0F, 1.0F);
            break;
         case 2:
            float hue = this.colorPhase(time, row);
            if (hue % 1.0F < 0.5F) {
               hue = 1.0F - hue % 1.0F;
            }

            base = ColorHelper.fromHSB(hue, 1.0F, 1.0F);
            break;
         case 3:
            base = new Color(this.customColor1.getValue());
            break;
         case 4:
            double phase = this.colorPhase(time, row);
            float blend = (float)(2.0 * Math.abs(phase - Math.floor(phase + 0.5)));
            base = ColorHelper.interpolate(blend, new Color(this.customColor1.getValue()), new Color(this.customColor2.getValue()));
            break;
         case 5:
            double threeColorPhase = this.colorPhase(time, row);
            float threeColorBlend = (float)(2.0 * Math.abs(threeColorPhase - Math.floor(threeColorPhase + 0.5)));
            if (threeColorBlend <= 0.5F) {
               base = ColorHelper.interpolate(threeColorBlend * 2.0F, new Color(this.customColor1.getValue()), new Color(this.customColor2.getValue()));
            } else {
               base = ColorHelper.interpolate((threeColorBlend - 0.5F) * 2.0F, new Color(this.customColor2.getValue()), new Color(this.customColor3.getValue()));
            }
      }

      float[] hsb = Color.RGBtoHSB(base.getRed(), base.getGreen(), base.getBlue(), null);
      return Color.getHSBColor(
         hsb[0], hsb[1] * (this.colorSaturation.getValue().floatValue() / 100.0F), hsb[2] * (this.colorBrightness.getValue().floatValue() / 100.0F)
      );
   }

   @Listen
   public void onRenderOverlay(OnRenderOverlay event) {
      boolean outline = this.chatOutline.getValue();
      if (outline && minecraft.field_71462_r instanceof GuiChat) {
         String input = ((GuiChatAccessor)minecraft.field_71462_r).getInputField().func_146179_b().trim();
         if (ClientContext.commandManager != null && ClientContext.commandManager.isClientCommand(input)) {
            RenderHelper.beginOverlay();
            RenderHelper.drawBorderedRect(
               2.0F,
               minecraft.field_71462_r.field_146295_m - 14,
               minecraft.field_71462_r.field_146294_l - 2,
               minecraft.field_71462_r.field_146295_m - 2,
               1.5F,
               0,
               this.getColor(System.currentTimeMillis()).getRGB()
            );
            RenderHelper.endOverlay();
         }
      }

      if (this.isEnabled()) {
         if (!minecraft.field_71474_y.field_74330_P) {
            List<ClientModule> modules = ClientContext.moduleManager
               .modules
               .values()
               .stream()
               .filter(HeadsUpDisplay::shouldDisplay)
               .sorted(Comparator.comparingInt(this::moduleWidth).reversed())
               .collect(Collectors.toList());
            float rowHeight = minecraft.field_71466_p.field_78288_b - 1.0F;
            float xOffset = this.offsetX.getValue().intValue();
            boolean showBar = this.bar.getValue();
            float barSpace = !showBar ? 0.0F : (this.shadow.getValue() ? 2.0F : 1.0F);
            float x = xOffset + (1.0F + barSpace) * this.scale.getValue();
            float y = this.offsetY.getValue().intValue() + 1.0F * this.scale.getValue();
            int horizontalPosition = this.positionX.getValue();
            if (horizontalPosition == 1) {
               x = new ScaledResolution(minecraft).func_78326_a() - x;
            }

            int verticalPosition = this.positionY.getValue();
            if (verticalPosition == 1) {
               y = new ScaledResolution(minecraft).func_78328_b() - y - rowHeight * this.scale.getValue();
            }

            GlStateManager.func_179094_E();
            GlStateManager.func_179152_a(this.scale.getValue(), this.scale.getValue(), 0.0F);
            long now = System.currentTimeMillis();
            long row = 0L;

            for (ClientModule module : modules) {
               String name = this.displayName(module);
               String[] values = this.displaySuffixes(module);
               int width = this.displayWidth(name, values);
               float rowWidth = width - (this.shadow.getValue() ? 0 : 1);
               int rgb = this.getColorForRow(now, row).getRGB();
               RenderHelper.beginOverlay();
               if (this.background.getValue() > 0) {
                  float left = x / this.scale.getValue() - 1.0F - (this.positionX.getValue() == 0 ? 0.0F : rowWidth);
                  float top = y / this.scale.getValue() - (this.positionY.getValue() == 0 ? (row == 0L ? 1.0F : 0.0F) : (this.shadow.getValue() ? 1.0F : 0.0F));
                  float right = x / this.scale.getValue() + 1.0F + (this.positionX.getValue() == 0 ? rowWidth : 0.0F);
                  float bottom = y / this.scale.getValue()
                     + rowHeight
                     + (this.positionY.getValue() == 0 ? (this.shadow.getValue() ? 1.0F : 0.0F) : (row == 0L ? 1.0F : 0.0F));
                  RenderHelper.fillRect(left, top, right, bottom, new Color(0.0F, 0.0F, 0.0F, this.background.getValue().floatValue() / 100.0F).getRGB());
               }

               if (this.bar.getValue()) {
                  if (this.shadow.getValue()) {
                     RenderHelper.fillRect(
                        x / this.scale.getValue() + (this.positionX.getValue() == 0 ? -3.0F : 1.0F),
                        y / this.scale.getValue() - (this.positionY.getValue() == 0 ? (row == 0L ? 1.0F : 0.0F) : 1.0F),
                        x / this.scale.getValue() + (this.positionX.getValue() == 0 ? -2.0F : 2.0F),
                        y / this.scale.getValue() + rowHeight + (this.positionY.getValue() != 0 && row != 0L ? 0.0F : 1.0F),
                        rgb
                     );
                     RenderHelper.fillRect(
                        x / this.scale.getValue() + (this.positionX.getValue() == 0 ? -2.0F : 2.0F),
                        y / this.scale.getValue() - (this.positionY.getValue() == 0 ? (row == 0L ? 1.0F : 0.0F) : 1.0F),
                        x / this.scale.getValue() + (this.positionX.getValue() == 0 ? -1.0F : 3.0F),
                        y / this.scale.getValue() + rowHeight + (this.positionY.getValue() != 0 && row != 0L ? 0.0F : 1.0F),
                        (rgb & 16579836) >> 2 | rgb & 0xFF000000
                     );
                  } else {
                     RenderHelper.fillRect(
                        x / this.scale.getValue() + (this.positionX.getValue() == 0 ? -2.0F : 1.0F),
                        y / this.scale.getValue() - (this.positionY.getValue() == 0 && row == 0L ? 1.0F : 0.0F),
                        x / this.scale.getValue() + (this.positionX.getValue() == 0 ? -1.0F : 2.0F),
                        y / this.scale.getValue() + rowHeight + (this.positionY.getValue() != 0 && row == 0L ? 1.0F : 0.0F),
                        rgb
                     );
                  }
               }

               RenderHelper.endOverlay();
               GlStateManager.func_179097_i();
               this.drawLabel(name, x, y, rowWidth, false, 0.0F, rgb);
               if (this.suffixes.getValue() && values.length > 0) {
                  float suffixOffset = minecraft.field_71466_p.func_78256_a(name) + 3.0F;

                  for (String suffix : values) {
                     this.drawLabel(suffix, x, y, rowWidth, true, suffixOffset, ChatFormat.GRAY.getRgb());
                     suffixOffset += minecraft.field_71466_p.func_78256_a(suffix) + (this.shadow.getValue() ? 3.0F : 2.0F);
                  }
               }

               y += (rowHeight + (this.shadow.getValue() ? 1.0F : 0.0F)) * this.scale.getValue() * (this.positionY.getValue() == 0 ? 1.0F : -1.0F);
               row++;
            }

            if (this.blinkTimer.getValue()) {
               OutboundQueueOwner mode = ClientContext.outgoingPacketBuffer.owner();
               if (mode != OutboundQueueOwner.NONE && mode != OutboundQueueOwner.AUTO_BLOCK) {
                  long elapsed = ClientContext.outgoingPacketBuffer.movementPacketCount();
                  if (elapsed > 0L) {
                     GlStateManager.func_179147_l();
                     GlStateManager.func_179112_b(770, 771);
                     minecraft.field_71466_p
                        .func_175065_a(
                           String.valueOf(elapsed),
                           new ScaledResolution(minecraft).func_78326_a() / 2.0F / this.scale.getValue()
                              - minecraft.field_71466_p.func_78256_a(String.valueOf(elapsed)) / 2.0F,
                           new ScaledResolution(minecraft).func_78328_b() / 5.0F * 3.0F / this.scale.getValue(),
                           this.getColorForRow(now, row).getRGB() & 16777215 | -1090519040,
                           this.shadow.getValue()
                        );
                     GlStateManager.func_179084_k();
                  }
               }
            }

            GlStateManager.func_179126_j();
            GlStateManager.func_179121_F();
         }
      }
   }

   private void drawLabel(String text, float x, float y, float rowWidth, boolean isSuffix, float suffixOffset, int rgb) {
      if (this.shadow.getValue()) {
         minecraft.field_71466_p.func_175063_a(text, this.labelX(x, rowWidth, isSuffix, suffixOffset), y / this.scale.getValue(), rgb);
      } else {
         minecraft.field_71466_p
            .func_175065_a(
               text, this.labelX(x, rowWidth, isSuffix, suffixOffset), y / this.scale.getValue() + (this.positionY.getValue() == 1 ? 1.0F : 0.0F), rgb, false
            );
      }
   }

   private float labelX(float x, float rowWidth, boolean isSuffix, float suffixOffset) {
      float position = x / this.scale.getValue() - (this.positionX.getValue() == 1 ? rowWidth : 0.0F);
      return isSuffix ? position + suffixOffset : position;
   }

   private static boolean shouldDisplay(ClientModule module) {
      boolean enabled = module.isEnabled();
      return enabled && !module.isHidden();
   }

   private boolean usesFirstCustomColor() {
      int mode = this.color.getValue();
      return mode == 3 || this.color.getValue() == 4 || this.color.getValue() == 5;
   }

   private boolean usesSecondCustomColor() {
      int mode = this.color.getValue();
      return mode == 4 || this.color.getValue() == 5;
   }

   private boolean usesThirdCustomColor() {
      int mode = this.color.getValue();
      return mode == 5;
   }
}
