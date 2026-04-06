package foure.dev.module.impl.donut;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.game.EventUpdate;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.impl.BooleanSetting;
import foure.dev.module.setting.impl.ModeSetting;
import foure.dev.module.setting.impl.StringSetting;
import foure.dev.util.render.utils.ChatUtils;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.Item.TooltipContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

@ModuleInfo(
   name = "AutoShulker",
   category = Category.DONUT,
   desc = "Automatically buys/sells shulkers and shulker shells with player targeting"
)
public class AutoShulker extends Function {
   private static final long WAIT_TIME_MS = 50L;
   private static final int MAX_BULK_BUY = 5;
   private final ModeSetting itemMode = new ModeSetting("Item Mode", this, "Shulkers", new String[]{"Shulkers", "Shulker Shells"});
   private final ModeSetting action = new ModeSetting("Action", this, "Buy And Sell", new String[]{"Buy And Sell", "Buy Only", "Sell Only", "Order Only"});
   private final StringSetting minPrice = new StringSetting("Min Price", this, "850");
   private final BooleanSetting notifications = new BooleanSetting("Notifications", this, true);
   private final BooleanSetting speedMode = new BooleanSetting("Speed Mode", this, true);
   private final BooleanSetting enableTargeting = new BooleanSetting("Enable Targeting", this, false);
   private final StringSetting targetPlayerName = new StringSetting("Target Player", this, "");
   private final BooleanSetting targetOnlyMode = new BooleanSetting("Target Only Mode", this, false);
   private final BooleanSetting autoDrop = new BooleanSetting("Auto Drop", this, true);
   private AutoShulker.Stage stage;
   private long stageStart;
   private int itemMoveIndex;
   private long lastItemMoveTime;
   private int exitCount;
   private int finalExitCount;
   private long finalExitStart;
   private int bulkBuyCount;
   private String targetPlayer;
   private boolean isTargetingActive;

   public AutoShulker() {
      this.stage = AutoShulker.Stage.NONE;
      this.stageStart = 0L;
      this.itemMoveIndex = 0;
      this.lastItemMoveTime = 0L;
      this.exitCount = 0;
      this.finalExitCount = 0;
      this.finalExitStart = 0L;
      this.bulkBuyCount = 0;
      this.targetPlayer = "";
      this.isTargetingActive = false;
   }

   public void onEnable() {
      double parsedPrice = this.parsePrice((String)this.minPrice.getValue());
      if (parsedPrice == -1.0D && !(Boolean)this.enableTargeting.getValue()) {
         if ((Boolean)this.notifications.getValue()) {
            ChatUtils.sendMessage("Invalid minimum price format!");
         }

         this.toggle();
      } else {
         this.updateTargetPlayer();
         if (!this.action.is("Sell Only") && !this.action.is("Order Only")) {
            this.stage = AutoShulker.Stage.SHOP;
         } else {
            this.stage = AutoShulker.Stage.WAIT;
         }

         this.stageStart = System.currentTimeMillis();
         this.itemMoveIndex = 0;
         this.lastItemMoveTime = 0L;
         this.exitCount = 0;
         this.finalExitCount = 0;
         this.bulkBuyCount = 0;
         if ((Boolean)this.notifications.getValue()) {
            String modeInfo = this.isTargetingActive ? String.format(" | Targeting: %s", this.targetPlayer) : "";
            String itemInfo = this.itemMode.is("Shulkers") ? "Shulkers" : "Shulker Shells";
            ChatUtils.sendMessage(String.format("Activated! Mode: %s | Action: %s | Min: %s%s", itemInfo, this.action.getValue(), this.minPrice.getValue(), modeInfo));
         }

      }
   }

   public void onDisable() {
      this.stage = AutoShulker.Stage.NONE;
   }

   private void updateTargetPlayer() {
      this.targetPlayer = "";
      this.isTargetingActive = false;
      if ((Boolean)this.enableTargeting.getValue() && !((String)this.targetPlayerName.getValue()).trim().isEmpty()) {
         this.targetPlayer = ((String)this.targetPlayerName.getValue()).trim();
         this.isTargetingActive = true;
         if ((Boolean)this.notifications.getValue()) {
            ChatUtils.sendMessage("Targeting enabled for player: " + this.targetPlayer);
         }
      }

   }

   @Subscribe
   public void onUpdate(EventUpdate e) {
      if (mc.player != null && mc.world != null) {
         long now = System.currentTimeMillis();
         long exitDelay;
         Screen var5;
         Iterator var6;
         Slot slot;
         int batch;
         ItemStack stack;
         boolean shouldTakeOrder;
         int playerSlotId;
         GenericContainerScreen screen;
         ScreenHandler handler;
         boolean foundOrder;
         Iterator var22;
         boolean isCorrectItem;
         int maxClicks;
         switch(this.stage.ordinal()) {
         case 0:
         default:
            break;
         case 1:
            if (!this.action.is("Sell Only") && !this.action.is("Order Only")) {
               mc.player.networkHandler.sendChatCommand("shop");
               this.stage = AutoShulker.Stage.SHOP_END;
               this.stageStart = now;
               break;
            }

            this.stage = AutoShulker.Stage.WAIT;
            this.stageStart = now;
            return;
         case 2:
            var5 = mc.currentScreen;
            if (var5 instanceof GenericContainerScreen) {
               screen = (GenericContainerScreen)var5;
               handler = screen.getScreenHandler();
               var6 = handler.slots.iterator();

               while(var6.hasNext()) {
                  slot = (Slot)var6.next();
                  stack = slot.getStack();
                  if (!stack.isEmpty() && this.isEndStone(stack)) {
                     mc.interactionManager.clickSlot(handler.syncId, slot.id, 0, SlotActionType.PICKUP, mc.player);
                     this.stage = AutoShulker.Stage.SHOP_ITEM;
                     this.stageStart = now;
                     this.bulkBuyCount = 0;
                     return;
                  }
               }

               if (now - this.stageStart > (long)((Boolean)this.speedMode.getValue() ? 1000 : 3000)) {
                  mc.player.closeHandledScreen();
                  this.stage = AutoShulker.Stage.SHOP;
                  this.stageStart = now;
               }
            }
            break;
         case 3:
            var5 = mc.currentScreen;
            if (var5 instanceof GenericContainerScreen) {
               screen = (GenericContainerScreen)var5;
               handler = screen.getScreenHandler();
               foundOrder = false;
               var22 = handler.slots.iterator();

               while(var22.hasNext()) {
                  slot = (Slot)var22.next();
                  stack = slot.getStack();
                  isCorrectItem = this.itemMode.is("Shulkers") && this.isShulkerBox(stack) || this.itemMode.is("Shulker Shells") && this.isShulkerShell(stack);
                  if (!stack.isEmpty() && isCorrectItem) {
                     if (this.itemMode.is("Shulkers")) {
                        maxClicks = (Boolean)this.speedMode.getValue() ? 10 : 5;

                        for(playerSlotId = 0; playerSlotId < maxClicks; ++playerSlotId) {
                           mc.interactionManager.clickSlot(handler.syncId, slot.id, 0, SlotActionType.PICKUP, mc.player);
                        }

                        this.stage = AutoShulker.Stage.SHOP_CONFIRM;
                     } else {
                        mc.interactionManager.clickSlot(handler.syncId, slot.id, 0, SlotActionType.PICKUP, mc.player);
                        this.stage = AutoShulker.Stage.SHOP_GLASS_PANE;
                     }

                     foundOrder = true;
                     ++this.bulkBuyCount;
                     break;
                  }
               }

               if (foundOrder) {
                  this.stageStart = now;
                  return;
               }

               if (now - this.stageStart > (long)((Boolean)this.speedMode.getValue() ? 500 : 1500)) {
                  mc.player.closeHandledScreen();
                  this.stage = AutoShulker.Stage.SHOP;
                  this.stageStart = now;
               }
            }
            break;
         case 4:
            var5 = mc.currentScreen;
            if (var5 instanceof GenericContainerScreen) {
               screen = (GenericContainerScreen)var5;
               handler = screen.getScreenHandler();
               var6 = handler.slots.iterator();

               while(var6.hasNext()) {
                  slot = (Slot)var6.next();
                  stack = slot.getStack();
                  if (!stack.isEmpty() && this.isGlassPane(stack) && stack.getCount() == 64) {
                     mc.interactionManager.clickSlot(handler.syncId, slot.id, 0, SlotActionType.PICKUP, mc.player);
                     this.stage = AutoShulker.Stage.SHOP_BUY;
                     this.stageStart = now;
                     return;
                  }
               }

               if (now - this.stageStart > (long)((Boolean)this.speedMode.getValue() ? 300 : 1000)) {
                  mc.player.closeHandledScreen();
                  this.stage = AutoShulker.Stage.SHOP;
                  this.stageStart = now;
               }
            }
            break;
         case 5:
            exitDelay = (Boolean)this.speedMode.getValue() ? 500L : 1000L;
            if (now - this.stageStart >= exitDelay) {
               Screen var24 = mc.currentScreen;
               if (var24 instanceof GenericContainerScreen) {
                  screen = (GenericContainerScreen)var24;
                  handler = screen.getScreenHandler();
                  Iterator var29 = handler.slots.iterator();

                  while(var29.hasNext()) {
                     slot = (Slot)var29.next();
                     stack = slot.getStack();
                     if (!stack.isEmpty() && this.isGreenGlass(stack) && stack.getCount() == 1) {
                        maxClicks = (Boolean)this.speedMode.getValue() ? 50 : 30;

                        for(playerSlotId = 0; playerSlotId < maxClicks; ++playerSlotId) {
                           mc.interactionManager.clickSlot(handler.syncId, slot.id, 0, SlotActionType.PICKUP, mc.player);
                           if (this.isInventoryFull()) {
                              break;
                           }
                        }

                        this.stage = AutoShulker.Stage.SHOP_CHECK_FULL;
                        this.stageStart = now;
                        return;
                     }
                  }

                  if (now - this.stageStart > (long)((Boolean)this.speedMode.getValue() ? 2000 : 3000)) {
                     this.stage = AutoShulker.Stage.SHOP_GLASS_PANE;
                     this.stageStart = now;
                  }
               }
            }
            break;
         case 6:
            var5 = mc.currentScreen;
            if (var5 instanceof GenericContainerScreen) {
               screen = (GenericContainerScreen)var5;
               handler = screen.getScreenHandler();
               foundOrder = false;
               var22 = handler.slots.iterator();

               while(var22.hasNext()) {
                  slot = (Slot)var22.next();
                  stack = slot.getStack();
                  if (!stack.isEmpty() && this.isGreenGlass(stack)) {
                     for(int i = 0; i < ((Boolean)this.speedMode.getValue() ? 3 : 2); ++i) {
                        mc.interactionManager.clickSlot(handler.syncId, slot.id, 0, SlotActionType.PICKUP, mc.player);
                     }

                     foundOrder = true;
                     break;
                  }
               }

               if (foundOrder) {
                  this.stage = AutoShulker.Stage.SHOP_CHECK_FULL;
                  this.stageStart = now;
                  return;
               }

               if (now - this.stageStart > (long)((Boolean)this.speedMode.getValue() ? 200 : 800)) {
                  this.stage = AutoShulker.Stage.SHOP_ITEM;
                  this.stageStart = now;
               }
            }
            break;
         case 7:
            if (now - this.stageStart > (long)((Boolean)this.speedMode.getValue() ? 100 : 200)) {
               if (!this.isInventoryFull() && !this.action.is("Buy Only")) {
                  if (now - this.stageStart > (long)((Boolean)this.speedMode.getValue() ? 200 : 400)) {
                     this.stage = AutoShulker.Stage.SHOP_ITEM;
                     this.stageStart = now;
                  }
               } else {
                  mc.player.closeHandledScreen();
                  this.stage = AutoShulker.Stage.SHOP_EXIT;
                  this.stageStart = now;
               }
            }
            break;
         case 8:
            if (mc.currentScreen == null) {
               if (this.action.is("Buy Only")) {
                  if ((Boolean)this.autoDrop.getValue()) {
                     mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(Action.DROP_ALL_ITEMS, BlockPos.ORIGIN, Direction.DOWN));
                  }

                  this.stage = AutoShulker.Stage.CYCLE_PAUSE;
               } else {
                  this.stage = AutoShulker.Stage.WAIT;
               }

               this.stageStart = now;
            }

            if (now - this.stageStart > (long)((Boolean)this.speedMode.getValue() ? 1000 : 5000)) {
               mc.player.closeHandledScreen();
               this.stage = AutoShulker.Stage.SHOP;
               this.stageStart = now;
            }
            break;
         case 9:
            if (this.action.is("Buy Only")) {
               this.stage = AutoShulker.Stage.SHOP;
               this.stageStart = now;
               return;
            }

            exitDelay = (Boolean)this.speedMode.getValue() ? 25L : 50L;
            if (now - this.stageStart >= exitDelay) {
               if (this.isTargetingActive && !this.targetPlayer.isEmpty()) {
                  this.stage = AutoShulker.Stage.TARGET_ORDERS;
               } else {
                  String orderCommand = this.itemMode.is("Shulkers") ? "orders shulker" : "orders shulker shell";
                  mc.player.networkHandler.sendChatCommand(orderCommand);
                  this.stage = AutoShulker.Stage.ORDERS;
               }

               this.stageStart = now;
            }
            break;
         case 10:
            var5 = mc.currentScreen;
            if (var5 instanceof GenericContainerScreen) {
               screen = (GenericContainerScreen)var5;
               handler = screen.getScreenHandler();
               foundOrder = false;
               if ((Boolean)this.speedMode.getValue() && now - this.stageStart < 200L) {
                  return;
               }

               var22 = handler.slots.iterator();

               do {
                  String orderPlayer;
                  double orderPrice;
                  do {
                     do {
                        do {
                           if (!var22.hasNext()) {
                              if (!foundOrder && now - this.stageStart > (long)((Boolean)this.speedMode.getValue() ? 3000 : 5000)) {
                                 mc.player.closeHandledScreen();
                                 if (this.action.is("Order Only")) {
                                    this.stage = AutoShulker.Stage.CYCLE_PAUSE;
                                 } else {
                                    this.stage = AutoShulker.Stage.SHOP;
                                 }

                                 this.stageStart = now;
                              }

                              return;
                           }

                           slot = (Slot)var22.next();
                           stack = slot.getStack();
                           isCorrectItem = this.itemMode.is("Shulkers") && this.isShulkerBox(stack) && this.isPurple(stack) || this.itemMode.is("Shulker Shells") && this.isShulkerShell(stack);
                        } while(stack.isEmpty());
                     } while(!isCorrectItem);

                     shouldTakeOrder = false;
                     orderPlayer = this.getOrderPlayerName(stack);
                     orderPrice = this.getOrderPrice(stack);
                  } while(this.itemMode.is("Shulker Shells") && orderPrice > 1500.0D);

                  boolean isTargetedOrder = this.isTargetingActive && orderPlayer != null && orderPlayer.equalsIgnoreCase(this.targetPlayer);
                  if (isTargetedOrder) {
                     shouldTakeOrder = true;
                     if ((Boolean)this.notifications.getValue()) {
                        ChatUtils.sendMessage(String.format("Found TARGET order from %s: %s", orderPlayer, orderPrice > 0.0D ? this.formatPrice(orderPrice) : "Unknown price"));
                     }
                  } else if (!(Boolean)this.targetOnlyMode.getValue()) {
                     double minPriceValue = this.parsePrice((String)this.minPrice.getValue());
                     if (orderPrice >= minPriceValue) {
                        shouldTakeOrder = true;
                        if ((Boolean)this.notifications.getValue()) {
                           String var10000 = this.formatPrice(orderPrice);
                           ChatUtils.sendMessage("Found order: " + var10000);
                        }
                     }
                  }
               } while(!shouldTakeOrder);

               mc.interactionManager.clickSlot(handler.syncId, slot.id, 0, SlotActionType.PICKUP, mc.player);
               this.stage = AutoShulker.Stage.ORDERS_SELECT;
               this.stageStart = now + (long)((Boolean)this.speedMode.getValue() ? 100 : 50);
               this.itemMoveIndex = 0;
               this.lastItemMoveTime = 0L;
               foundOrder = true;
               if ((Boolean)this.notifications.getValue()) {
                  ChatUtils.sendMessage("Selected order, preparing to transfer items...");
               }

               return;
            }
            break;
         case 11:
            var5 = mc.currentScreen;
            if (var5 instanceof GenericContainerScreen) {
               screen = (GenericContainerScreen)var5;
               handler = screen.getScreenHandler();
               if (this.itemMoveIndex >= 36) {
                  mc.player.closeHandledScreen();
                  this.stage = AutoShulker.Stage.ORDERS_CONFIRM;
                  this.stageStart = now;
                  this.itemMoveIndex = 0;
                  return;
               }

               long moveDelay = (Boolean)this.speedMode.getValue() ? 10L : 100L;
               if (now - this.lastItemMoveTime >= moveDelay) {
                  int batchSize = (Boolean)this.speedMode.getValue() ? 3 : 1;

                  for(batch = 0; batch < batchSize && this.itemMoveIndex < 36; ++batch) {
                     stack = mc.player.getInventory().getStack(this.itemMoveIndex);
                     shouldTakeOrder = this.itemMode.is("Shulkers") && this.isShulkerBox(stack) || this.itemMode.is("Shulker Shells") && this.isShulkerShell(stack);
                     if (shouldTakeOrder) {
                        playerSlotId = -1;
                        Iterator var13 = handler.slots.iterator();

                        while(var13.hasNext()) {
                           slot = (Slot)var13.next();
                           if (slot.inventory == mc.player.getInventory() && slot.getIndex() == this.itemMoveIndex) {
                              playerSlotId = slot.id;
                              break;
                           }
                        }

                        if (playerSlotId != -1) {
                           mc.interactionManager.clickSlot(handler.syncId, playerSlotId, 0, SlotActionType.QUICK_MOVE, mc.player);
                        }
                     }

                     ++this.itemMoveIndex;
                  }

                  this.lastItemMoveTime = now;
               }
            }
            break;
         case 12:
            if (mc.currentScreen == null) {
               ++this.exitCount;
               if (this.exitCount < 2) {
                  mc.player.closeHandledScreen();
                  this.stageStart = now;
               } else {
                  this.exitCount = 0;
                  this.stage = AutoShulker.Stage.ORDERS_CONFIRM;
                  this.stageStart = now;
               }
            }
            break;
         case 13:
            var5 = mc.currentScreen;
            if (var5 instanceof GenericContainerScreen) {
               screen = (GenericContainerScreen)var5;
               handler = screen.getScreenHandler();
               var6 = handler.slots.iterator();

               while(var6.hasNext()) {
                  slot = (Slot)var6.next();
                  stack = slot.getStack();
                  if (!stack.isEmpty() && this.isGreenGlass(stack)) {
                     for(batch = 0; batch < ((Boolean)this.speedMode.getValue() ? 15 : 5); ++batch) {
                        mc.interactionManager.clickSlot(handler.syncId, slot.id, 0, SlotActionType.PICKUP, mc.player);
                     }

                     this.stage = AutoShulker.Stage.ORDERS_FINAL_EXIT;
                     this.stageStart = now;
                     this.finalExitCount = 0;
                     this.finalExitStart = now;
                     if ((Boolean)this.notifications.getValue()) {
                        String nextAction = this.action.is("Order Only") ? "checking for more orders..." : "going back to shop...";
                        ChatUtils.sendMessage("Order completed! " + nextAction);
                     }

                     return;
                  }
               }

               if (now - this.stageStart > (long)((Boolean)this.speedMode.getValue() ? 2000 : 5000)) {
                  mc.player.closeHandledScreen();
                  if (this.action.is("Order Only")) {
                     this.stage = AutoShulker.Stage.CYCLE_PAUSE;
                  } else {
                     this.stage = AutoShulker.Stage.SHOP;
                  }

                  this.stageStart = now;
               }
            }
            break;
         case 14:
            exitDelay = (Boolean)this.speedMode.getValue() ? 50L : 200L;
            if (this.finalExitCount == 0) {
               if (System.currentTimeMillis() - this.finalExitStart >= exitDelay) {
                  mc.player.closeHandledScreen();
                  ++this.finalExitCount;
                  this.finalExitStart = System.currentTimeMillis();
               }
            } else if (this.finalExitCount == 1) {
               if (System.currentTimeMillis() - this.finalExitStart >= exitDelay) {
                  mc.player.closeHandledScreen();
                  ++this.finalExitCount;
                  this.finalExitStart = System.currentTimeMillis();
               }
            } else {
               this.finalExitCount = 0;
               this.stage = AutoShulker.Stage.CYCLE_PAUSE;
               this.stageStart = System.currentTimeMillis();
            }
            break;
         case 15:
            exitDelay = (Boolean)this.speedMode.getValue() ? 10L : 25L;
            if (now - this.stageStart >= exitDelay) {
               this.updateTargetPlayer();
               if (!this.action.is("Order Only") && !this.action.is("Sell Only")) {
                  this.stage = AutoShulker.Stage.SHOP;
               } else {
                  this.stage = AutoShulker.Stage.WAIT;
               }

               this.stageStart = now;
            }
            break;
         case 16:
            mc.player.networkHandler.sendChatCommand("orders " + this.targetPlayer);
            this.stage = AutoShulker.Stage.ORDERS;
            this.stageStart = now;
            if ((Boolean)this.notifications.getValue()) {
               ChatUtils.sendMessage("Checking orders for: " + this.targetPlayer);
            }
         }

      }
   }

   private boolean isEndStone(ItemStack stack) {
      return stack.getItem() == Items.END_STONE || stack.getName().getString().toLowerCase(Locale.ROOT).contains("end");
   }

   private boolean isShulkerBox(ItemStack stack) {
      return !stack.isEmpty() && stack.getItem().getName().getString().toLowerCase(Locale.ROOT).contains("shulker box");
   }

   private boolean isShulkerShell(ItemStack stack) {
      return !stack.isEmpty() && stack.getItem() == Items.SHULKER_SHELL;
   }

   private boolean isPurple(ItemStack stack) {
      return stack.getItem() == Items.SHULKER_BOX;
   }

   private boolean isGreenGlass(ItemStack stack) {
      return stack.getItem() == Items.LIME_STAINED_GLASS_PANE || stack.getItem() == Items.GREEN_STAINED_GLASS_PANE;
   }

   private boolean isGlassPane(ItemStack stack) {
      String itemName = stack.getItem().getName().getString().toLowerCase();
      return itemName.contains("glass") && itemName.contains("pane");
   }

   private boolean isInventoryFull() {
      for(int i = 9; i <= 35; ++i) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.isEmpty()) {
            return false;
         }
      }

      return true;
   }

   private String getOrderPlayerName(ItemStack stack) {
      if (stack.isEmpty()) {
         return null;
      } else {
         TooltipContext tooltipContext = TooltipContext.create(mc.world);
         List<Text> tooltip = stack.getTooltip(tooltipContext, mc.player, TooltipType.BASIC);
         Iterator var4 = tooltip.iterator();

         while(var4.hasNext()) {
            Text line = (Text)var4.next();
            String text = line.getString();
            Pattern[] namePatterns = new Pattern[]{Pattern.compile("(?i)player\\s*:\\s*([a-zA-Z0-9_]+)"), Pattern.compile("(?i)from\\s*:\\s*([a-zA-Z0-9_]+)"), Pattern.compile("(?i)by\\s*:\\s*([a-zA-Z0-9_]+)"), Pattern.compile("(?i)seller\\s*:\\s*([a-zA-Z0-9_]+)"), Pattern.compile("(?i)owner\\s*:\\s*([a-zA-Z0-9_]+)"), Pattern.compile("\\b([a-zA-Z0-9_]{3,16})\\b")};
            Pattern[] var8 = namePatterns;
            int var9 = namePatterns.length;

            for(int var10 = 0; var10 < var9; ++var10) {
               Pattern pattern = var8[var10];
               Matcher matcher = pattern.matcher(text);
               if (matcher.find()) {
                  String playerName = matcher.group(1);
                  if (playerName.length() >= 3 && playerName.length() <= 16 && playerName.matches("[a-zA-Z0-9_]+")) {
                     return playerName;
                  }
               }
            }
         }

         return null;
      }
   }

   private double getOrderPrice(ItemStack stack) {
      if (stack.isEmpty()) {
         return -1.0D;
      } else {
         TooltipContext tooltipContext = TooltipContext.create(mc.world);
         List<Text> tooltip = stack.getTooltip(tooltipContext, mc.player, TooltipType.BASIC);
         return this.parseTooltipPrice(tooltip);
      }
   }

   private double parseTooltipPrice(List<Text> tooltip) {
      if (tooltip != null && !tooltip.isEmpty()) {
         Pattern[] pricePatterns = new Pattern[]{Pattern.compile("\\$([\\d,]+(?:\\.[\\d]+)?)([kmb])?", 2), Pattern.compile("(?i)price\\s*:\\s*([\\d,]+(?:\\.[\\d]+)?)([kmb])?", 2), Pattern.compile("(?i)pay\\s*:\\s*([\\d,]+(?:\\.[\\d]+)?)([kmb])?", 2), Pattern.compile("(?i)reward\\s*:\\s*([\\d,]+(?:\\.[\\d]+)?)([kmb])?", 2), Pattern.compile("([\\d,]+(?:\\.[\\d]+)?)([kmb])?\\s*coins?", 2), Pattern.compile("\\b([\\d,]+(?:\\.[\\d]+)?)([kmb])\\b", 2)};
         Iterator var3 = tooltip.iterator();

         while(var3.hasNext()) {
            Text line = (Text)var3.next();
            String text = line.getString();
            Pattern[] var6 = pricePatterns;
            int var7 = pricePatterns.length;

            for(int var8 = 0; var8 < var7; ++var8) {
               Pattern pattern = var6[var8];
               Matcher matcher = pattern.matcher(text);
               if (matcher.find()) {
                  String numberStr = matcher.group(1).replace(",", "");
                  String suffix = "";
                  if (matcher.groupCount() >= 2 && matcher.group(2) != null) {
                     suffix = matcher.group(2).toLowerCase();
                  }

                  try {
                     double basePrice = Double.parseDouble(numberStr);
                     double multiplier = 1.0D;
                     byte var18 = -1;
                     switch(suffix.hashCode()) {
                     case 98:
                        if (suffix.equals("elementCodec")) {
                           var18 = 2;
                        }
                        break;
                     case 107:
                        if (suffix.equals("k")) {
                           var18 = 0;
                        }
                        break;
                     case 109:
                        if (suffix.equals("m")) {
                           var18 = 1;
                        }
                     }

                     switch(var18) {
                     case 0:
                        multiplier = 1000.0D;
                        break;
                     case 1:
                        multiplier = 1000000.0D;
                        break;
                     case 2:
                        multiplier = 1.0E9D;
                     }

                     return basePrice * multiplier;
                  } catch (NumberFormatException var19) {
                  }
               }
            }
         }

         return -1.0D;
      } else {
         return -1.0D;
      }
   }

   private double parsePrice(String priceStr) {
      if (priceStr != null && !priceStr.isEmpty()) {
         String cleaned = priceStr.trim().toLowerCase().replace(",", "");
         double multiplier = 1.0D;
         if (cleaned.endsWith("elementCodec")) {
            multiplier = 1.0E9D;
            cleaned = cleaned.substring(0, cleaned.length() - 1);
         } else if (cleaned.endsWith("m")) {
            multiplier = 1000000.0D;
            cleaned = cleaned.substring(0, cleaned.length() - 1);
         } else if (cleaned.endsWith("k")) {
            multiplier = 1000.0D;
            cleaned = cleaned.substring(0, cleaned.length() - 1);
         }

         try {
            return Double.parseDouble(cleaned) * multiplier;
         } catch (NumberFormatException var6) {
            return -1.0D;
         }
      } else {
         return -1.0D;
      }
   }

   private String formatPrice(double price) {
      if (price >= 1.0E9D) {
         return String.format("$%.1fB", price / 1.0E9D);
      } else if (price >= 1000000.0D) {
         return String.format("$%.1fM", price / 1000000.0D);
      } else {
         return price >= 1000.0D ? String.format("$%.1fK", price / 1000.0D) : String.format("$%.0f", price);
      }
   }

   private static enum Stage {
      NONE,
      SHOP,
      SHOP_END,
      SHOP_ITEM,
      SHOP_GLASS_PANE,
      SHOP_BUY,
      SHOP_CONFIRM,
      SHOP_CHECK_FULL,
      SHOP_EXIT,
      WAIT,
      ORDERS,
      ORDERS_SELECT,
      ORDERS_EXIT,
      ORDERS_CONFIRM,
      ORDERS_FINAL_EXIT,
      CYCLE_PAUSE,
      TARGET_ORDERS;

      // $FF: synthetic method
      private static AutoShulker.Stage[] $values() {
         return new AutoShulker.Stage[]{NONE, SHOP, SHOP_END, SHOP_ITEM, SHOP_GLASS_PANE, SHOP_BUY, SHOP_CONFIRM, SHOP_CHECK_FULL, SHOP_EXIT, WAIT, ORDERS, ORDERS_SELECT, ORDERS_EXIT, ORDERS_CONFIRM, ORDERS_FINAL_EXIT, CYCLE_PAUSE, TARGET_ORDERS};
      }
   }
}
