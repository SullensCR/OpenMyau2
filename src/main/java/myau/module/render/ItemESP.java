package myau.module.render;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import myau.event.Listen;
import myau.event.render.OnRenderWorld;
import myau.mixin.accessor.RenderManagerAccessor;
import myau.module.ClientModule;
import myau.setting.BooleanOption;
import myau.setting.PercentOption;
import myau.util.chat.ChatFormat;
import myau.util.entity.EntityHelper;
import myau.util.math.Maths;
import myau.util.render.RenderHelper;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;

public class ItemESP extends ClientModule {
   private static final Minecraft minecraft = Minecraft.func_71410_x();
   public final PercentOption opacity = new PercentOption("opacity", 25);
   public final BooleanOption outline = new BooleanOption("outline", false);
   public final BooleanOption itemCount = new BooleanOption("item-count", true);
   public final BooleanOption autoScale = new BooleanOption("auto-scale", true);
   public final BooleanOption emeralds = new BooleanOption("emeralds", true);
   public final BooleanOption diamonds = new BooleanOption("diamonds", true);
   public final BooleanOption gold = new BooleanOption("gold", true);
   public final BooleanOption iron = new BooleanOption("iron", true);

   public ItemESP() {
      super("ItemESP", false, 0);
   }

   private boolean shouldShowItem(int id) {
      if (this.emeralds.getValue() && this.isEmerald(id)) {
         return true;
      } else if (this.diamonds.getValue() && this.isDiamond(id)) {
         return true;
      } else {
         return this.gold.getValue() && this.isGold(id) ? true : this.iron.getValue() && this.isIron(id);
      }
   }

   private boolean isEmerald(int id) {
      Item item = Item.func_150899_d(id);
      Block block = Block.func_149634_a(item);
      return item == Items.field_151166_bC || block == Blocks.field_150475_bE || block == Blocks.field_150412_bA;
   }

   private boolean isDiamond(int id) {
      Item item = Item.func_150899_d(id);
      Block block = Block.func_149634_a(item);
      return item == Items.field_151045_i
         || item == Items.field_151048_u
         || item == Items.field_151046_w
         || item == Items.field_151047_v
         || item == Items.field_151056_x
         || item == Items.field_151012_L
         || item == Items.field_151161_ac
         || item == Items.field_151163_ad
         || item == Items.field_151173_ae
         || item == Items.field_151175_af
         || block == Blocks.field_150484_ah
         || block == Blocks.field_150482_ag;
   }

   private boolean isGold(int id) {
      Item item = Item.func_150899_d(id);
      Block block = Block.func_149634_a(item);
      return item == Items.field_151043_k
         || item == Items.field_151074_bl
         || item == Items.field_151153_ao
         || block == Blocks.field_150340_R
         || block == Blocks.field_150352_o;
   }

   private boolean isIron(int id) {
      Item item = Item.func_150899_d(id);
      Block block = Block.func_149634_a(item);
      return item == Items.field_151042_j || block == Blocks.field_150339_S || block == Blocks.field_150366_p;
   }

   private Color colorFor(int id) {
      if (this.isEmerald(id)) {
         return new Color(ChatFormat.GREEN.getRgb());
      } else if (this.isDiamond(id)) {
         return new Color(ChatFormat.AQUA.getRgb());
      } else {
         return this.isGold(id) ? new Color(ChatFormat.YELLOW.getRgb()) : new Color(this.isIron(id) ? ChatFormat.WHITE.getRgb() : ChatFormat.GRAY.getRgb());
      }
   }

   private int sortPriority(int id) {
      if (this.isEmerald(id)) {
         return 4;
      } else if (this.isDiamond(id)) {
         return 3;
      } else if (this.isGold(id)) {
         return 2;
      } else {
         return this.isIron(id) ? 1 : 0;
      }
   }

   private int compareItemEntries(Entry<DroppedItemKey, Integer> first, Entry<DroppedItemKey, Integer> second) {
      int firstPriority = this.sortPriority(first.getKey().itemId);
      int secondPriority = this.sortPriority(second.getKey().itemId);
      return Integer.compare(firstPriority, secondPriority);
   }

   @Listen
   public void onRenderWorld(OnRenderWorld event) {
      if (this.isEnabled()) {
         LinkedHashMap<DroppedItemKey, Integer> counts = new LinkedHashMap<>();

         for (Entity entity : EntityHelper.entitiesBackToFront()) {
            if (entity.field_70173_aa >= 3 && (entity.field_70158_ak || RenderHelper.isEntityInFrustum(entity, 0.125)) && entity instanceof EntityItem) {
               EntityItem droppedItem = (EntityItem)entity;
               ItemStack stack = droppedItem.func_92059_d();
               if (stack.field_77994_a > 0) {
                  int itemId = Item.func_150891_b(stack.func_77973_b());
                  if (this.shouldShowItem(itemId)) {
                     double worldX = Maths.lerpDouble(droppedItem.field_70142_S, droppedItem.field_70165_t, event.getPartialTicks());
                     double worldY = Maths.lerpDouble(droppedItem.field_70137_T, droppedItem.field_70163_u, event.getPartialTicks());
                     double worldZ = Maths.lerpDouble(droppedItem.field_70136_U, droppedItem.field_70161_v, event.getPartialTicks());
                     DroppedItemKey group = new DroppedItemKey(itemId, worldX, worldY, worldZ);
                     Integer previousCount = counts.get(group);
                     counts.put(group, stack.field_77994_a + (previousCount == null ? 0 : previousCount));
                  }
               }
            }
         }

         for (Entry<DroppedItemKey, Integer> entry : counts.entrySet().stream().sorted(this::compareItemEntries).collect(Collectors.toList())) {
            DroppedItemKey group = entry.getKey();
            Color tint = this.colorFor(group.itemId);
            double x = group.x - ((RenderManagerAccessor)minecraft.func_175598_ae()).getRenderPosX();
            double y = group.y - ((RenderManagerAccessor)minecraft.func_175598_ae()).getRenderPosY();
            double z = group.z - ((RenderManagerAccessor)minecraft.func_175598_ae()).getRenderPosZ();
            double distance = minecraft.func_175606_aa().func_70011_f(group.x, group.y, group.z);
            double size = 0.5 + 0.375 * ((Math.max(6.0, this.autoScale.getValue() ? distance : 6.0) - 6.0) / 28.0);
            AxisAlignedBB bounds = new AxisAlignedBB(x - size * 0.5, y, z - size * 0.5, x + size * 0.5, y + size, z + size * 0.5);
            RenderHelper.beginOverlay();
            if (this.opacity.getValue() > 0) {
               RenderHelper.fillBounds(bounds, tint.getRed(), tint.getGreen(), tint.getBlue(), (int)(this.opacity.getValue().intValue() / 100.0F * 255.0F));
               GlStateManager.func_179117_G();
            }

            if (this.outline.getValue()) {
               RenderHelper.outlineBounds(bounds, tint.getRed(), tint.getGreen(), tint.getBlue(), 255, 1.5F);
               GlStateManager.func_179117_G();
            }

            RenderHelper.endOverlay();
            if (this.itemCount.getValue()) {
               GlStateManager.func_179094_E();
               GlStateManager.func_179137_b(x, y + size * 0.5, z);
               GlStateManager.func_179114_b(minecraft.func_175598_ae().field_78735_i * -1.0F, 0.0F, 1.0F, 0.0F);
               float var10001 = minecraft.field_71474_y.field_74320_O == 2 ? -1.0F : 1.0F;
               GlStateManager.func_179114_b(minecraft.func_175598_ae().field_78732_j, var10001, 0.0F, 0.0F);
               double textScale = -0.04375 - 0.0328125 * ((Math.max(6.0, this.autoScale.getValue() ? distance : 6.0) - 6.0) / 28.0);
               GlStateManager.func_179139_a(textScale, textScale, 1.0);
               GlStateManager.func_179097_i();
               String label = String.format("%d", entry.getValue());
               RenderHelper.drawOutlinedText(
                  label,
                  (minecraft.field_71466_p.func_78256_a(label) / 2.0F - 0.5F) * -1.0F,
                  (minecraft.field_71466_p.field_78288_b / 2 - 0.5F) * -1.0F,
                  tint.getRGB()
               );
               GlStateManager.func_179126_j();
               GlStateManager.func_179117_G();
               GlStateManager.func_179121_F();
            }
         }
      }
   }
}
