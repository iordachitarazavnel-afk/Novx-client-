package foure.dev.module.impl.donut;

import com.google.common.eventbus.Subscribe;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import foure.dev.event.impl.game.EventUpdate;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.impl.BooleanSetting;
import foure.dev.module.setting.impl.EnchantmentSetting;
import foure.dev.module.setting.impl.ItemSetting;
import foure.dev.module.setting.impl.ModeSetting;
import foure.dev.module.setting.impl.NumberSetting;
import foure.dev.module.setting.impl.StringSetting;
import foure.dev.util.render.utils.ChatUtils;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.Item.TooltipContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

@ModuleInfo(
   name = "AuctionSniper",
   category = Category.DONUT,
   desc = "Snipes items on auction house for cheap"
)
public class AuctionSniper extends Function {
   private final ItemSetting snipingItem;
   private final StringSetting price;
   private final ModeSetting mode;
   private final StringSetting apiKey;
   private final NumberSetting refreshDelay;
   private final NumberSetting buyDelay;
   private final NumberSetting apiRefreshRate;
   private final BooleanSetting showApiNotifications;
   private final EnchantmentSetting requiredEnchantments;
   private final EnchantmentSetting forbiddenEnchantments;
   private final NumberSetting minEnchantmentLevel;
   private final BooleanSetting exactEnchantmentMatch;
   private final StringSetting minSelfDestructTime;
   private final HttpClient httpClient;
   private final Gson gson;
   private final Map<String, Double> snipingItems;
   private int delayCounter;
   private boolean isProcessing;
   private long lastApiCallTimestamp;
   private boolean isApiQueryInProgress;
   private boolean isAuctionSniping;
   private int auctionPageCounter;
   private String currentSellerName;

   public AuctionSniper() {
      this.snipingItem = new ItemSetting("Sniping Item", this, Items.AIR);
      this.price = new StringSetting("Price", this, "1k");
      this.mode = new ModeSetting("Mode", this, "Manual", new String[]{"API", "Manual"});
      this.apiKey = new StringSetting("Api Key", this, "");
      this.refreshDelay = new NumberSetting("Refresh Delay", this, 2.0D, 0.0D, 100.0D, 1.0D);
      this.buyDelay = new NumberSetting("Buy Delay", this, 2.0D, 0.0D, 100.0D, 1.0D);
      this.apiRefreshRate = new NumberSetting("API Refresh Rate", this, 250.0D, 10.0D, 5000.0D, 10.0D);
      this.showApiNotifications = new BooleanSetting("Show API Notifications", this, true);
      this.requiredEnchantments = new EnchantmentSetting("Required Enchantments", this);
      this.forbiddenEnchantments = new EnchantmentSetting("Forbidden Enchantments", this);
      this.minEnchantmentLevel = new NumberSetting("Min Enchantment Level", this, 1.0D, 1.0D, 10.0D, 1.0D);
      this.exactEnchantmentMatch = new BooleanSetting("Exact Enchantment Match", this, false);
      this.minSelfDestructTime = new StringSetting("Min Self Destruct", this, "intermediary");
      this.snipingItems = new HashMap();
      this.lastApiCallTimestamp = 0L;
      this.isApiQueryInProgress = false;
      this.isAuctionSniping = false;
      this.auctionPageCounter = -1;
      this.currentSellerName = "";
      this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5L)).build();
      this.gson = new Gson();
   }

   public void onEnable() {
      double d = this.parsePrice((String)this.price.getValue());
      if (d == -1.0D) {
         ChatUtils.sendMessage("Invalid Price");
         this.toggle();
      } else {
         if (this.snipingItem.getItem() != Items.AIR) {
            this.snipingItems.put(this.snipingItem.getItem().toString(), d);
         }

         this.lastApiCallTimestamp = 0L;
         this.isApiQueryInProgress = false;
         this.isAuctionSniping = false;
         this.currentSellerName = "";
      }
   }

   public void onDisable() {
      this.isAuctionSniping = false;
   }

   @Subscribe
   public void onUpdate(EventUpdate e) {
      if (mc.player != null) {
         if (this.delayCounter > 0) {
            --this.delayCounter;
         } else if (this.mode.is("API")) {
            this.handleApiMode();
         } else if (this.mode.is("Manual")) {
            ScreenHandler screenHandler = mc.player.currentScreenHandler;
            if (!(mc.currentScreen instanceof GenericContainerScreen)) {
               String searchCommand = this.constructSearchCommand();
               mc.player.networkHandler.sendChatCommand(searchCommand);
               this.delayCounter = 20;
            } else {
               if (screenHandler instanceof GenericContainerScreenHandler) {
                  GenericContainerScreenHandler containerHandler = (GenericContainerScreenHandler)screenHandler;
                  if (containerHandler.getRows() == 6) {
                     this.processSixRowAuction(containerHandler);
                  } else if (containerHandler.getRows() == 3) {
                     this.processThreeRowAuction(containerHandler);
                  }
               }

            }
         }
      }
   }

   private String constructSearchCommand() {
      String translationKey;
      if (!this.requiredEnchantments.getAmethystEnchants().isEmpty()) {
         translationKey = (String)this.requiredEnchantments.getAmethystEnchants().iterator().next();
         return "ah " + translationKey;
      } else {
         translationKey = this.snipingItem.getItem().getTranslationKey();
         String[] stringArray = translationKey.split("\\.");
         String string2 = stringArray[stringArray.length - 1];
         String formattedItemName = (String)Arrays.stream(string2.replace("_", " ").split(" ")).map((string) -> {
            String var10000 = string.substring(0, 1).toUpperCase();
            return var10000 + string.substring(1);
         }).collect(Collectors.joining(" "));
         if (!this.requiredEnchantments.isEmpty()) {
            List<String> enchantNames = new ArrayList();
            Iterator var6 = this.requiredEnchantments.getEnchantments().iterator();

            while(var6.hasNext()) {
               RegistryKey<Enchantment> enchKey = (RegistryKey)var6.next();
               String enchIdStr = enchKey.getValue().toString();
               if (enchIdStr.contains(":")) {
                  enchIdStr = enchIdStr.substring(enchIdStr.lastIndexOf(58) + 1);
               } else {
                  enchIdStr = enchIdStr.substring(enchIdStr.lastIndexOf(47) + 1);
               }

               String formattedEnchant = (String)Arrays.stream(enchIdStr.replace("_", " ").split(" ")).map((s) -> {
                  String var10000 = s.substring(0, 1).toUpperCase();
                  return var10000 + s.substring(1);
               }).collect(Collectors.joining(" "));
               enchantNames.add(formattedEnchant);
            }

            String enchantString = String.join(" ", enchantNames);
            return "ah " + formattedItemName + " " + enchantString;
         } else {
            return "ah " + formattedItemName;
         }
      }
   }

   private void handleApiMode() {
      if (!this.isAuctionSniping) {
         if (this.auctionPageCounter != -1) {
            if (this.auctionPageCounter <= 40) {
               ++this.auctionPageCounter;
            } else {
               this.isAuctionSniping = false;
               this.currentSellerName = "";
            }
         } else {
            mc.player.networkHandler.sendChatCommand("ah " + this.currentSellerName);
            this.auctionPageCounter = 0;
         }
      } else {
         ScreenHandler screenHandler = mc.player.currentScreenHandler;
         if (!(mc.currentScreen instanceof GenericContainerScreen)) {
            if (mc.currentScreen instanceof GenericContainerScreen && mc.currentScreen.getTitle().getString().contains("Page")) {
               mc.player.closeHandledScreen();
               this.delayCounter = 20;
               return;
            }

            if (this.isApiQueryInProgress) {
               return;
            }

            long l = System.currentTimeMillis();
            long l2 = l - this.lastApiCallTimestamp;
            if (l2 > (long)((Double)this.apiRefreshRate.getValue()).intValue()) {
               this.lastApiCallTimestamp = l;
               if (((String)this.apiKey.getValue()).isEmpty()) {
                  if ((Boolean)this.showApiNotifications.getValue()) {
                     ChatUtils.sendMessage("§cAPI key is not set. Set it using /api in-game.");
                  }

                  return;
               }

               this.isApiQueryInProgress = true;
               this.queryApi().thenAccept(this::processApiResponse);
            }

            return;
         }

         this.auctionPageCounter = -1;
         if (screenHandler instanceof GenericContainerScreenHandler) {
            GenericContainerScreenHandler containerHandler = (GenericContainerScreenHandler)screenHandler;
            if (containerHandler.getRows() == 6) {
               this.processSixRowAuction(containerHandler);
            } else if (containerHandler.getRows() == 3) {
               this.processThreeRowAuction(containerHandler);
            }
         }
      }

   }

   private CompletableFuture<List<JsonObject>> queryApi() {
      return CompletableFuture.supplyAsync(() -> {
         try {
            String string = "https://api.donutsmp.net/v1/auctions/search";
            HttpResponse<String> httpResponse = this.httpClient.send(HttpRequest.newBuilder().uri(URI.create(string)).header("Authorization", "Bearer " + (String)this.apiKey.getValue()).header("Content-Type", "application/json").POST(BodyPublishers.ofString("{\"sort\": \"recently_listed\"}")).build(), BodyHandlers.ofString());
            if (httpResponse.statusCode() != 200) {
               if ((Boolean)this.showApiNotifications.getValue() && mc.player != null) {
                  ChatUtils.sendMessage("§cAPI Error: " + httpResponse.statusCode());
               }

               ArrayList<JsonObject> arrayList = new ArrayList();
               this.isApiQueryInProgress = false;
               return arrayList;
            } else {
               JsonArray jsonArray = ((JsonObject)this.gson.fromJson((String)httpResponse.body(), JsonObject.class)).getAsJsonArray("result");
               ArrayList<JsonObject> arrayListx = new ArrayList();
               Iterator var5 = jsonArray.iterator();

               while(var5.hasNext()) {
                  JsonElement jsonElement = (JsonElement)var5.next();
                  arrayListx.add(jsonElement.getAsJsonObject());
               }

               this.isApiQueryInProgress = false;
               return arrayListx;
            }
         } catch (Throwable var7) {
            return List.of();
         }
      });
   }

   private void processApiResponse(List<JsonObject> list) {
      Iterator var2 = list.iterator();

      while(var2.hasNext()) {
         JsonObject e = (JsonObject)var2.next();

         try {
            double d = 0.0D;
            String string = "";
            String string2 = e.getAsJsonObject("item").get("id").getAsString();
            long l = e.get("price").getAsLong();
            String string3 = e.getAsJsonObject("seller").get("name").getAsString();
            Iterator iterator = this.snipingItems.entrySet().iterator();

            do {
               if (iterator.hasNext()) {
                  Entry<String, Double> entry = (Entry)iterator.next();
                  string = (String)entry.getKey();
                  d = (Double)entry.getValue();
               }
            } while(!string2.contains(string) || !((double)l <= d));

            if ((Boolean)this.showApiNotifications.getValue() && mc.player != null) {
               ChatUtils.sendMessage("§aFound " + string2 + " for " + this.formatPrice((double)l) + " §r(threshold: " + this.formatPrice(d) + ") §afrom seller: " + string3);
            }

            this.isAuctionSniping = true;
            this.currentSellerName = string3;
            return;
         } catch (Exception var13) {
            if ((Boolean)this.showApiNotifications.getValue() && mc.player != null) {
               ChatUtils.sendMessage("§cError processing auction: " + var13.getMessage());
            }
         }
      }

   }

   private void processSixRowAuction(GenericContainerScreenHandler genericContainerScreenHandler) {
      ItemStack itemStack = genericContainerScreenHandler.getSlot(47).getStack();
      if (itemStack.isOf(Items.AIR)) {
         this.delayCounter = 2;
      } else {
         Iterator var3 = itemStack.getTooltip(TooltipContext.DEFAULT, mc.player, TooltipType.BASIC).iterator();

         Text text;
         String string;
         do {
            if (!var3.hasNext()) {
               for(int i = 0; i < 44; ++i) {
                  ItemStack itemStack2 = genericContainerScreenHandler.getSlot(i).getStack();
                  if (this.isValidAuctionItem(itemStack2)) {
                     if (this.isProcessing) {
                        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, i, 1, SlotActionType.QUICK_MOVE, mc.player);
                        this.isProcessing = false;
                        return;
                     }

                     this.isProcessing = true;
                     this.delayCounter = ((Double)this.buyDelay.getValue()).intValue();
                     return;
                  }
               }

               if (this.isAuctionSniping) {
                  this.isAuctionSniping = false;
                  this.currentSellerName = "";
                  mc.player.closeHandledScreen();
               } else {
                  mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 49, 1, SlotActionType.QUICK_MOVE, mc.player);
                  this.delayCounter = ((Double)this.refreshDelay.getValue()).intValue();
               }

               return;
            }

            text = (Text)var3.next();
            string = text.getString();
         } while(!string.contains("Recently Listed") || !text.getStyle().toString().contains("white") || string.contains("white"));

         mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 47, 1, SlotActionType.QUICK_MOVE, mc.player);
         this.delayCounter = 5;
      }
   }

   private void processThreeRowAuction(GenericContainerScreenHandler genericContainerScreenHandler) {
      ItemStack confirmationItem = genericContainerScreenHandler.getSlot(13).getStack();
      if (!confirmationItem.isOf(Items.AIR) && this.isValidAuctionItem(confirmationItem)) {
         mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 15, 0, SlotActionType.PICKUP, mc.player);
         this.delayCounter = 20;
      }

      if (this.isAuctionSniping) {
         this.isAuctionSniping = false;
         this.currentSellerName = "";
      }

   }

   private double parseTooltipPrice(List<Text> list) {
      String string = "";
      String string2 = "";
      if (list != null && !list.isEmpty()) {
         Iterator iterator = list.iterator();

         while(iterator.hasNext()) {
            String string3 = ((Text)iterator.next()).getString();
            if (string3.matches("(?i).*price\\s*:\\s*\\$.*")) {
               String string4 = string3.replaceAll("[,$]", "");
               Matcher matcher = Pattern.compile("([\\d]+(?:\\.[\\d]+)?)\\s*([KMB])?", 2).matcher(string4);
               if (matcher.find()) {
                  string2 = matcher.group(1);
                  string = matcher.group(2) != null ? matcher.group(2).toUpperCase() : "";
                  break;
               }
            }
         }

         return this.parsePrice(string2 + string);
      } else {
         return -1.0D;
      }
   }

   private boolean isValidAuctionItem(ItemStack itemStack) {
      List<Text> list = itemStack.getTooltip(TooltipContext.DEFAULT, mc.player, TooltipType.BASIC);
      if (!this.requiredEnchantments.getAmethystEnchants().isEmpty()) {
         boolean isValid = this.isValidAmethystItem(itemStack, list);
         return isValid;
      } else {
         if (!this.requiredEnchantments.isEmpty()) {
            if (!itemStack.isOf(this.snipingItem.getItem())) {
               return false;
            }
         } else if (!itemStack.isOf(this.snipingItem.getItem())) {
            return false;
         }

         double d = this.parseTooltipPrice(list) / (double)itemStack.getCount();
         double d2 = this.parsePrice((String)this.price.getValue());
         if (d2 == -1.0D) {
            ChatUtils.sendMessage("Invalid Price");
            this.toggle();
            return false;
         } else if (d == -1.0D) {
            return false;
         } else {
            boolean priceValid = d <= d2;
            return !priceValid ? false : this.checkEnchantmentFilter(itemStack);
         }
      }
   }

   private boolean isValidAmethystItem(ItemStack itemStack, List<Text> list) {
      Item item = itemStack.getItem();
      boolean isNetheritePickaxe = item == Items.NETHERITE_PICKAXE;
      boolean isNetheriteAxe = item == Items.NETHERITE_AXE;
      boolean isNetheriteShovel = item == Items.NETHERITE_SHOVEL;
      String itemName = itemStack.getName().getString();
      if (this.requiredEnchantments.hasAmethystPickaxe()) {
         if (!isNetheritePickaxe || !itemName.contains("ᴀᴍᴇᴛʜʏsᴛ")) {
            return false;
         }
      } else if (this.requiredEnchantments.hasAmethystAxe()) {
         if (!isNetheriteAxe || !itemName.contains("ᴀᴍᴇᴛʜʏsᴛ")) {
            return false;
         }
      } else if (this.requiredEnchantments.hasAmethystSellAxe()) {
         if (!isNetheriteAxe || !itemName.contains("ᴀᴍᴇᴛʜʏsᴛ")) {
            return false;
         }
      } else if (this.requiredEnchantments.hasAmethystShovel()) {
         if (!isNetheriteShovel || !itemName.contains("ᴀᴍᴇᴛʜʏsᴛ")) {
            return false;
         }
      } else {
         String amethystEnchant = (String)this.requiredEnchantments.getAmethystEnchants().iterator().next();
         if (!itemName.contains(amethystEnchant)) {
            return false;
         }

         if (amethystEnchant.contains("Pickaxe") && !isNetheritePickaxe) {
            return false;
         }

         if (amethystEnchant.contains("Axe") && !isNetheriteAxe) {
            return false;
         }

         if (amethystEnchant.contains("Shovel") && !isNetheriteShovel) {
            return false;
         }
      }

      double minTime = this.parsePrice((String)this.minSelfDestructTime.getValue());
      double selfDestructMins;
      if (minTime > 0.0D) {
         selfDestructMins = this.parseSelfDestructTime(list);
         if (selfDestructMins < minTime) {
            return false;
         }
      }

      selfDestructMins = this.parseTooltipPrice(list) / (double)itemStack.getCount();
      double d2 = this.parsePrice((String)this.price.getValue());
      boolean isValid = selfDestructMins != -1.0D && d2 != -1.0D && selfDestructMins <= d2;
      return isValid;
   }

   private double parseSelfDestructTime(List<Text> list) {
      Pattern pattern = Pattern.compile("Self Destruct:");
      Pattern timePattern = Pattern.compile("(\\d+)d\\s*(\\d+)h\\s*(\\d+)m");
      boolean foundSelfDestruct = false;
      Iterator var5 = list.iterator();

      while(var5.hasNext()) {
         Text text = (Text)var5.next();
         String line = text.getString();
         if (pattern.matcher(line).find()) {
            foundSelfDestruct = true;
         } else if (foundSelfDestruct) {
            Matcher timeMatcher = timePattern.matcher(line);
            if (timeMatcher.find()) {
               int days = Integer.parseInt(timeMatcher.group(1));
               int hours = Integer.parseInt(timeMatcher.group(2));
               int mins = Integer.parseInt(timeMatcher.group(3));
               return (double)(days * 24 * 60 + hours * 60 + mins);
            }
            break;
         }
      }

      return 0.0D;
   }

   private boolean checkEnchantmentFilter(ItemStack itemStack) {
      try {
         if (this.requiredEnchantments.getEnchantments().isEmpty() && this.forbiddenEnchantments.isEmpty()) {
            return true;
         } else {
            Map<RegistryKey<Enchantment>, Integer> itemEnchantments = new HashMap();
            itemStack.getEnchantments().getEnchantments().forEach((enchantment) -> {
               enchantment.getKey().ifPresent((key) -> {
                  itemEnchantments.put(key, itemStack.getEnchantments().getLevel(enchantment));
               });
            });
            Iterator var3 = this.forbiddenEnchantments.getEnchantments().iterator();

            RegistryKey required;
            while(var3.hasNext()) {
               required = (RegistryKey)var3.next();
               if (itemEnchantments.containsKey(required)) {
                  return false;
               }
            }

            if (!this.requiredEnchantments.getEnchantments().isEmpty()) {
               if (!(Boolean)this.exactEnchantmentMatch.getValue()) {
                  boolean hasRequired = false;
                  Iterator var9 = this.requiredEnchantments.getEnchantments().iterator();

                  while(var9.hasNext()) {
                     required = (RegistryKey)var9.next();
                     if (itemEnchantments.containsKey(required)) {
                        int level = (Integer)itemEnchantments.get(required);
                        if (level >= ((Double)this.minEnchantmentLevel.getValue()).intValue()) {
                           hasRequired = true;
                           break;
                        }
                     }
                  }

                  return hasRequired;
               }

               if (itemEnchantments.size() != this.requiredEnchantments.getEnchantments().size()) {
                  return false;
               }

               var3 = this.requiredEnchantments.getEnchantments().iterator();

               while(var3.hasNext()) {
                  required = (RegistryKey)var3.next();
                  if (!itemEnchantments.containsKey(required)) {
                     return false;
                  }

                  int level = (Integer)itemEnchantments.get(required);
                  if (level < ((Double)this.minEnchantmentLevel.getValue()).intValue()) {
                     return false;
                  }
               }
            }

            return true;
         }
      } catch (Exception var7) {
         return false;
      }
   }

   private double parsePrice(String string) {
      if (string == null) {
         return -1.0D;
      } else if (string.isEmpty()) {
         return -1.0D;
      } else {
         String string2 = string.trim().toUpperCase();
         double d = 1.0D;
         if (string2.endsWith("B")) {
            d = 1.0E9D;
            string2 = string2.substring(0, string2.length() - 1);
         } else if (string2.endsWith("M")) {
            d = 1000000.0D;
            string2 = string2.substring(0, string2.length() - 1);
         } else if (string2.endsWith("K")) {
            d = 1000.0D;
            string2 = string2.substring(0, string2.length() - 1);
         }

         try {
            return Double.parseDouble(string2) * d;
         } catch (NumberFormatException var6) {
            return -1.0D;
         }
      }
   }

   private String formatPrice(double d) {
      if (d >= 1.0E9D) {
         return String.format("%.2fB", d / 1.0E9D);
      } else if (d >= 1000000.0D) {
         return String.format("%.2fM", d / 1000000.0D);
      } else {
         return d >= 1000.0D ? String.format("%.2fK", d / 1000.0D) : String.format("%.2f", d);
      }
   }
}
