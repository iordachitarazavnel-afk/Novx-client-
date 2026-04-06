package foure.dev.module.impl.render;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.render.RenderEvent;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.impl.ColorSetting;
import foure.dev.util.math.ProjectionUtil;
import foure.dev.util.render.core.Renderer2D;
import foure.dev.util.render.text.FontRegistry;
import java.awt.Color;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;
import java.util.stream.Stream;
import net.minecraft.block.Block;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(
   name = "ShulkerVisible",
   category = Category.RENDER,
   desc = "Displays shulker box contents"
)
public class ShulkerVisible extends Function {
   private final ColorSetting bgColor = new ColorSetting("BgColor", this, new Color(20, 15, 30, 200));
   private final ColorSetting outlineColor = new ColorSetting("OutlineColor", this, new Color(100, 80, 180, 100));
   private static Method renderMethod;
   private static Object guiMode;

   public ShulkerVisible() {
      this.addSettings(new Setting[]{this.bgColor, this.outlineColor});
   }

   public void onEnable() {
      super.onEnable();
   }

   public void onDisable() {
      super.onDisable();
   }

   @Subscribe
   public void onRender2D(RenderEvent event) {
      Renderer2D r = event.renderer();
      MatrixStack matrices = new MatrixStack();

      for (Entity entity : mc.world.getEntities()) {
         double x = 0.0;
         double y = 0.0;
         double z = 0.0;
         ItemStack stack = null;

         // === PLAYER HAND CHECK ===
         if (entity instanceof PlayerEntity player) {
            if (player.getUuid().equals(mc.player.getUuid())) continue;

            double distSq = player.squaredDistanceTo(mc.player);
            if (distSq > 225.0) continue;

            ItemStack main = player.getMainHandStack();
            ItemStack off = player.getOffHandStack();

            if (isShulker(main)) {
               stack = main;
            } else if (isShulker(off)) {
               stack = off;
            }

            if (stack != null) {
               x = player.getX();
               y = player.getY() + player.getHeight() + 1.5;
               z = player.getZ();
            }
         }

         // === ITEM ENTITY CHECK ===
         else if (entity instanceof ItemEntity itemEntity) {
            double distSq = itemEntity.squaredDistanceTo(mc.player);
            if (distSq > 225.0) continue;

            ItemStack itemStack = itemEntity.getStack();

            if (isShulker(itemStack)) {
               stack = itemStack;
               x = itemEntity.getX();
               y = itemEntity.getY() + 0.75;
               z = itemEntity.getZ();
            }
         }

         if (stack == null) continue;

         ContainerComponent container = stack.get(DataComponentTypes.CONTAINER);
         if (container == null) continue;

         // Collect items safely
         ArrayList<ItemStack> items = new ArrayList<>();
         container.stream().forEach(e -> items.add((ItemStack) e));

         if (items.isEmpty()) continue;

         // Ensure not all empty
         if (items.stream().allMatch(ItemStack::isEmpty)) continue;

         Vec3d screenPos = ProjectionUtil.toScreen(new Vec3d(x, y, z));
         if (screenPos == null) continue;

         float width = 140.8F;
         float height = 75.0F;
         float startX = (float) screenPos.x - width / 2.0F;
         float startY = (float) screenPos.y - height / 2.0F;

         // Background
         r.rect(startX, startY, width, height, 5.0F, bgColor.getValue().getRGB());
         r.rectOutline(startX, startY, width, height, 5.0F, outlineColor.getValue().getRGB(), 1.0F);

         // Title
         r.text(
                 FontRegistry.INTER_MEDIUM,
                 startX + width / 2.0F,
                 startY + 5.0F,
                 8.0F,
                 stack.getName().getString(),
                 -1,
                 "c"
         );

         float itemX = startX + 8.0F;
         float itemY = startY + 15.0F;

         try {
            VertexConsumerProvider vertexConsumers = mc.getBufferBuilders().getEntityVertexConsumers();

            int i = 0;
            for (ItemStack item : items) {
               if (item.isEmpty()) {
                  i++;
                  continue;
               }

               if (i >= 27) break;

               float renderX = itemX + (i % 9) * 14.4F;
               float renderY = itemY + (i / 9) * 14.4F;

               matrices.push();
               matrices.translate(renderX, renderY, 0.0F);
               matrices.scale(0.8F, 0.8F, 1.0F);

               renderItemReflection(item, matrices, vertexConsumers, 0);

               matrices.pop();
               i++;
            }

         } catch (Exception ignored) {
         }
      }
   }

   private boolean isShulker(ItemStack stack) {
      return stack != null && !stack.isEmpty() ? Block.getBlockFromItem(stack.getItem()) instanceof ShulkerBoxBlock : false;
   }

   private void renderItemReflection(ItemStack item, MatrixStack matrices, VertexConsumerProvider consumers, int seed) {
      try {
         if (renderMethod == null) {
            Method[] var5 = mc.getItemRenderer().getClass().getMethods();
            int var6 = var5.length;

            label42:
            for(int var7 = 0; var7 < var6; ++var7) {
               Method m = var5[var7];
               if (m.getName().equals("renderItem") && m.getParameterCount() == 8) {
                  Class<?>[] types = m.getParameterTypes();
                  if (types[0] == ItemStack.class && types[1].isEnum()) {
                     renderMethod = m;
                     Object[] var10 = types[1].getEnumConstants();
                     int var11 = var10.length;
                     int var12 = 0;

                     while(true) {
                        if (var12 >= var11) {
                           break label42;
                        }

                        Object constant = var10[var12];
                        if (constant.toString().equals("GUI")) {
                           guiMode = constant;
                           break label42;
                        }

                        ++var12;
                     }
                  }
               }
            }
         }

         if (renderMethod != null && guiMode != null) {
            renderMethod.invoke(mc.getItemRenderer(), item, guiMode, 15728880, OverlayTexture.DEFAULT_UV, matrices, consumers, mc.world, seed);
         }
      } catch (Exception var14) {
         var14.printStackTrace();
      }

   }
}
