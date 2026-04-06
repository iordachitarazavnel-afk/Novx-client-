package foure.dev.module.impl.render;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.render.RenderEvent;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.impl.BooleanSetting;
import foure.dev.util.Player.PlayerIntersectionUtil;
import foure.dev.util.math.ProjectionUtil;
import foure.dev.util.render.core.Renderer2D;
import foure.dev.util.render.text.FontRegistry;
import foure.dev.util.render.utils.ColorUtils;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Rarity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(
   name = "NameTags",
   category = Category.RENDER,
   visual = true
)
public class NameTags extends Function {
   private final BooleanSetting armor = new BooleanSetting("Armor", true);
   private final BooleanSetting items = new BooleanSetting("Items", true);
   public final BooleanSetting hp = new BooleanSetting("HP", true);
   private final BooleanSetting pings = new BooleanSetting("Ping", true);
   private final BooleanSetting background = new BooleanSetting("Background", true);

   public NameTags() {
      this.addSettings(new Setting[]{this.armor, this.items, this.hp, this.pings, this.background});
   }

   @Subscribe
   public void onRender(RenderEvent event) {
      if (!fullNullCheck()) {
         Renderer2D renderer = event.renderer();
         float tickDelta = mc.getRenderTickCounter().getTickProgress(true);
         Iterator var4 = mc.world.getPlayers().iterator();

         while(true) {
            PlayerEntity player;
            do {
               if (!var4.hasNext()) {
                  if ((Boolean)this.items.getValue()) {
                     var4 = mc.world.getEntities().iterator();

                     while(var4.hasNext()) {
                        Entity entity = (Entity)var4.next();
                        if (entity instanceof ItemEntity) {
                           ItemEntity itemEntity = (ItemEntity)entity;
                           this.renderItemTag(itemEntity, renderer, tickDelta);
                        }
                     }
                  }

                  return;
               }

               player = (PlayerEntity)var4.next();
            } while(player == mc.player && mc.options.getPerspective().isFirstPerson());

            this.renderPlayerNameTag(player, renderer, tickDelta);
         }
      }
   }

   private void renderPlayerNameTag(PlayerEntity player, Renderer2D r, float tickDelta) {
      Vec3d worldPos = ProjectionUtil.interpolateEntity(player, tickDelta).add(0.0D, (double)player.getHeight() + 0.5D, 0.0D);
      Vec3d screen = ProjectionUtil.toScreen(worldPos);
      if (screen != null) {
         double distance = mc.player.getCameraPosVec(tickDelta).distanceTo(worldPos);
         float fov = mc.gameRenderer.getFov(mc.gameRenderer.getCamera(), tickDelta, true);
         float baseScale = (float)(1.0D / Math.tan(Math.toRadians((double)fov * 0.5D)));
         float distanceFactor = MathHelper.clamp((float)distance / 12.0F, 1.2F, 1.2F);
         float scale = baseScale * distanceFactor;
         float x = (float)screen.x;
         float y = (float)screen.y;
         r.pushScale(scale, scale, x, y);
         Text text = this.getTextPlayer(player, false);
         float fontSize = 9.0F;
         float width = r.getStringWidth(FontRegistry.INTER_MEDIUM, (Text)text, fontSize) + 12.0F;
         float height = 15.0F;
         r.rect(x - width / 2.0F, y, width, height, 0.0F, (new Color(0, 0, 0, 140)).getRGB());
         r.text(FontRegistry.INTER_MEDIUM, x, y + height / 2.0F + 3.0F, fontSize, (Text)text, -1, "c");
         r.popScale();
      }
   }

   private void renderItemTag(ItemEntity item, Renderer2D r, float tickDelta) {
      Vec3d pos = ProjectionUtil.interpolateEntity(item, tickDelta).add(0.0D, 0.5D, 0.0D);
      Vec3d screen = ProjectionUtil.toScreen(pos);
      if (screen != null) {
         double distance = mc.player.getCameraPosVec(tickDelta).distanceTo(pos);
         float scale = (float)MathHelper.clamp(1.0D / (distance * 0.08D), 0.7D, 1.0D);
         float x = (float)screen.x;
         float y = (float)screen.y;
         r.pushScale(scale, scale, x, y);
         ItemStack stack = item.getStack();
         String var10000 = stack.getName().getString();
         String text = var10000 + (stack.getCount() > 1 ? " x" + stack.getCount() : "");
         float width = r.getStringWidth(FontRegistry.SF_REGULAR, text, 8.0F) + 6.0F;
         r.rect(x - width / 2.0F, y - 4.0F, width, 12.0F, 0.0F, (new Color(0, 0, 0, 120)).getRGB());
         r.text(FontRegistry.SF_REGULAR, x, y + 4.0F, 8.0F, text, this.getRarityColor(stack), "c");
         r.popScale();
      }
   }

   private void renderArmor(PlayerEntity player, float x, float y) {
      List<ItemStack> stacks = new ArrayList();
      stacks.add(player.getMainHandStack());
      stacks.add(player.getOffHandStack());
      stacks.removeIf(ItemStack::isEmpty);
      if (!stacks.isEmpty()) {
         float itemSize = 16.0F;
         float gap = 2.0F;
         float totalWidth = (float)stacks.size() * itemSize + (float)(stacks.size() - 1) * gap;
         float startX = x - totalWidth / 2.0F;
         int i = 0;

         for(Iterator var10 = stacks.iterator(); var10.hasNext(); ++i) {
            ItemStack stack = (ItemStack)var10.next();
            float var10000 = startX + (float)i * (itemSize + gap);
         }

      }
   }

   private MutableText getTextPlayer(PlayerEntity player, boolean friend) {
      float health = PlayerIntersectionUtil.getHealth(player);
      MutableText text = Text.empty();
      String var10001;
      if ((Boolean)this.pings.getValue()) {
         float ping = (float)this.getPing(player);
         if (ping >= 0.0F && ping <= 10000.0F) {
            var10001 = String.valueOf(Formatting.RESET);
            text.append(var10001 + " " + String.valueOf(this.getPingColor(this.getPing(player))) + this.getPing(player) + String.valueOf(Formatting.RESET) + " ");
         }
      }

      String rawName = player.getDisplayName().getString();
      text.append(replaceSymbolsPreserveStyle(player.getDisplayName()));
      ItemStack stack = player.getOffHandStack();
      MutableText itemName = stack.getName().copy();
      if (stack.isOf(Items.PLAYER_HEAD) || stack.isOf(Items.TOTEM_OF_UNDYING)) {
         itemName.formatted(stack.getRarity().getFormatting());
         text.append(itemName);
      }

      if (health >= 0.0F && health <= player.getMaxHealth()) {
         var10001 = String.valueOf(Formatting.RESET);
         text.append(var10001 + " [" + String.valueOf(Formatting.RED) + PlayerIntersectionUtil.getHealthString(player) + String.valueOf(Formatting.RESET) + "] ");
      }

      return text;
   }

   public static MutableText replaceSymbolsPreserveStyle(Text original) {
      MutableText result = Text.empty();
      original.visit((style, string) -> {
         String replaced = replaceSymbolsDonate(string);
         result.append(Text.literal(replaced).setStyle(style));
         return Optional.empty();
      }, Style.EMPTY);
      return result;
   }

   private Formatting getPingColor(int ping) {
      if (ping <= 50) {
         return Formatting.GREEN;
      } else if (ping <= 100) {
         return Formatting.YELLOW;
      } else {
         return ping <= 150 ? Formatting.GOLD : Formatting.RED;
      }
   }

   private int getPing(PlayerEntity player) {
      if (mc.getNetworkHandler() == null) {
         return 0;
      } else {
         PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
         return entry == null ? 0 : entry.getLatency();
      }
   }

   private Color getRarityColor(ItemEntity item) {
      switch(item.getStack().getRarity()) {
      case COMMON:
         return new Color(1.0F, 1.0F, 1.0F, 1.0F);
      case UNCOMMON:
         return Color.YELLOW;
      case RARE:
         return new Color(0.0F, 0.9F, 1.0F, 1.0F);
      case EPIC:
         return new Color(0.6F, 0.1F, 0.7F, 1.0F);
      default:
         return new Color(1.0F, 1.0F, 1.0F, 1.0F);
      }
   }

   private int getRarityColor(ItemStack stack) {
      if (stack.getRarity() == Rarity.EPIC) {
         return -6291201;
      } else if (stack.getRarity() == Rarity.RARE) {
         return -16711681;
      } else {
         return stack.getRarity() == Rarity.UNCOMMON ? -171 : -1;
      }
   }

   public static String replaceSymbolsDonate(String string) {
      String var10000 = string.replaceAll("\\[", "").replaceAll("⚡", "★").replaceAll("]", "").replaceAll("ᴀ", "keyCodec").replaceAll("ʙ", "elementCodec").replaceAll("ᴄ", "c").replaceAll("ᴅ", "d").replaceAll("ᴇ", "e").replaceAll("ғ", "f").replaceAll("ɢ", "g").replaceAll("ʜ", "h").replaceAll("ɪ", "i").replaceAll("ᴊ", "j").replaceAll("ᴋ", "k").replaceAll("ʟ", "l").replaceAll("ᴍ", "m").replaceAll("ɴ", "n").replaceAll("ᴏ", "o").replaceAll("ᴘ", "p").replaceAll("ǫ", "q").replaceAll("ʀ", "r").replaceAll("s", "s").replaceAll("ᴛ", "t").replaceAll("ᴜ", "u").replaceAll("ᴠ", "v").replaceAll("ᴡ", "w").replaceAll("x", "x").replaceAll("ʏ", "y").replaceAll("ꔲ", String.valueOf(Formatting.DARK_PURPLE) + "BULL").replaceAll("ꕒ", String.valueOf(Formatting.WHITE) + "RABBIT").replaceAll("ꔨ", String.valueOf(Formatting.DARK_PURPLE) + "DRAGON").replaceAll("ꔶ", String.valueOf(Formatting.GOLD) + "TIGER").replaceAll("ꕠ", String.valueOf(Formatting.YELLOW) + "D.HELPER").replaceAll("ꕖ", String.valueOf(Formatting.DARK_GRAY) + "BUNNY").replaceAll("ꔠ", String.valueOf(Formatting.GOLD) + "MAGISTER").replaceAll("ꔤ", String.valueOf(Formatting.RED) + "IMPERATOR").replaceAll("ꕀ", String.valueOf(Formatting.DARK_GREEN) + "HYDRA").replaceAll("ꕄ", String.valueOf(Formatting.DARK_RED) + "DRACULA").replaceAll("ꕗ", String.valueOf(Formatting.DARK_RED) + "D.ADMIN").replaceAll("ꔈ", String.valueOf(Formatting.YELLOW) + "TITAN").replaceAll("ꕓ", String.valueOf(Formatting.GRAY) + "GHOST").replaceAll("ꔨ", String.valueOf(Formatting.GOLD) + "GOD").replaceAll("ꔈ", String.valueOf(Formatting.YELLOW) + "TITAN").replaceAll("ꕈ", String.valueOf(Formatting.GREEN) + "COBRA").replaceAll("ꔲ", String.valueOf(Formatting.BLUE) + "MODER").replaceAll("ꔘ", String.valueOf(Formatting.BLUE) + "D.ST.MODER").replaceAll("ꔐ", String.valueOf(Formatting.BLUE) + "D.GL.MODER").replaceAll("ꔷ", String.valueOf(Formatting.DARK_RED) + "ADMIN").replaceAll("ꔩ", String.valueOf(Formatting.BLUE) + "Gl Moder").replaceAll("ꔥ", String.valueOf(Formatting.DARK_RED) + "St.Moder").replaceAll("ꔡ", String.valueOf(Formatting.DARK_BLUE) + "Moder+").replaceAll("ꔗ", String.valueOf(Formatting.BLUE) + "Moder").replaceAll("ꔳ", String.valueOf(Formatting.AQUA) + "Ml.Admin").replaceAll("ꔓ", String.valueOf(Formatting.AQUA) + "Ml.Moder").replaceAll("ꔒ", String.valueOf(Formatting.GREEN) + "AVENGER").replaceAll("ꔉ", String.valueOf(Formatting.GOLD) + "HELPER").replaceAll("ꔖ ", String.valueOf(Formatting.AQUA) + "OVERLORD ").replaceAll("ꔁ", String.valueOf(Formatting.DARK_RED) + "Media").replaceAll("ꔦ", String.valueOf(Formatting.RED) + "D.ML.ADMIN").replaceAll("ꔀ", String.valueOf(Formatting.GRAY) + "Player").replaceAll("ꔀ", String.valueOf(Formatting.GRAY) + "PLAYER");
      String var10002 = String.valueOf(Formatting.WHITE);
      return var10000.replaceAll("ꔅ", var10002 + "Y" + String.valueOf(Formatting.RED) + "T").replaceAll("ꔄ", ColorUtils.getUltraSmoothGradient(new Color(21, 140, 204), new Color(85, 255, 255), "HERO").replaceAll("ᴢ", "z"));
   }
}
