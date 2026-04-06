package foure.dev.util.Player;

import foure.dev.FourEClient;
import foure.dev.module.impl.combat.helper.Angle;
import foure.dev.module.impl.combat.helper.AngleUtil;
import foure.dev.util.math.MathUtil;
import foure.dev.util.others.ItemUtil;
import foure.dev.util.wrapper.Wrapper;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.IntPredicate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.Generated;
import net.minecraft.block.BlockState;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.screen.sync.ItemStackHash;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class PlayerInventoryUtil implements Wrapper {
   public static void moveItem(Slot from, int to) {
      if (from != null) {
         moveItem(from.id, to, false, false);
      }

   }

   public static void moveItem(Slot from, int to, boolean task) {
      moveItem(from, to, task, false);
   }

   public static void moveItem(Slot from, int to, boolean task, boolean updateInventory) {
      if (from != null) {
         moveItem(from.id, to, task, updateInventory);
      }

   }

   public static void moveItem(int from, int to, boolean task, boolean updateInventory) {
      if (from != to && from != -1) {
         int count = Math.toIntExact(slots().count()) - 10;
         if (from >= count && count == 36) {
            if (task) {
               PlayerInventoryComponent.addTask(() -> {
                  clickSlot(to, from - count, SlotActionType.SWAP, false);
               });
            } else {
               clickSlot(to, from - count, SlotActionType.SWAP, false);
            }

         } else {
            if (task) {
               PlayerInventoryComponent.addTask(() -> {
                  moveItem(from, to, updateInventory);
               });
            } else {
               moveItem(from, to, updateInventory);
            }

         }
      }
   }

   public static void moveItem(int from, int to, boolean updateInventory) {
      clickSlot(from, 0, SlotActionType.SWAP, false);
      clickSlot(to, 0, SlotActionType.SWAP, false);
      clickSlot(from, 0, SlotActionType.SWAP, false);
      if (updateInventory) {
         updateSlots();
      }

   }

   public static void swapHand(Slot slot, Hand hand, boolean task) {
      swapHand(slot, hand, task, false);
   }

   public static void swapHand(Slot slot, Hand hand, boolean task, boolean updateInventory) {
      if (slot != null && slot.id != -1 && (!hand.equals(Hand.OFF_HAND) || slot.inventory instanceof PlayerInventory || slot.inventory instanceof EnderChestInventory)) {
         int button = hand.equals(Hand.MAIN_HAND) ? mc.player.getInventory().selectedSlot : 40;
         if (task) {
            PlayerInventoryComponent.addTask(() -> {
               swap(slot, button, updateInventory);
            });
         } else {
            swap(slot, button, updateInventory);
         }

      }
   }

   public static void swap(Slot slot, int button, boolean updateInventory) {
      clickSlot(slot, button, SlotActionType.SWAP, false);
      if (updateInventory) {
         updateSlots();
      }

   }

   public static void swapAndUse(Slot slot, String text, boolean task) {
      if (slot != null) {
         if (task) {
            PlayerInventoryComponent.addTask(() -> {
               swapAndUse(slot, AngleUtil.cameraAngle());
            });
         } else {
            swapAndUse(slot, AngleUtil.cameraAngle());
         }

      }
   }

   public static void swapAndUse(Item item) {
      swapAndUse(item, AngleUtil.cameraAngle(), true);
   }

   public static void swapAndUse(Item item, Angle angle, boolean task) {
      float cooldownProgress = ItemUtil.getCooldownProgress(item);
      if (cooldownProgress > 0.0F) {
         String time = MathUtil.round((double)cooldownProgress, 0.1D) + "с";
      } else {
         Slot slot = getSlot(item);
         if (slot != null) {
            if (task) {
               PlayerInventoryComponent.addTask(() -> {
                  swapAndUse(slot, angle);
               });
            } else {
               swapAndUse(slot, angle);
            }

         }
      }
   }

   public static void swapAndUse(Slot slot, Angle angle) {
      swapHand(slot, Hand.MAIN_HAND, false);
      PlayerIntersectionUtil.interactItem(Hand.MAIN_HAND, angle);
      swapHand(slot, Hand.MAIN_HAND, false, true);
   }

   public static void updateSlots() {
      ScreenHandler screenHandler = mc.player.currentScreenHandler;
      mc.player.networkHandler.sendPacket(new ClickSlotC2SPacket(screenHandler.syncId, screenHandler.getRevision(), (short)0, (byte)0, SlotActionType.PICKUP_ALL, Int2ObjectMaps.emptyMap(), (ItemStackHash)null));
   }

   public static void closeScreen(boolean packet) {
      if (packet) {
         mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
      } else {
         mc.player.closeHandledScreen();
      }

   }

   public static void clickSlot(Slot slot, int button, SlotActionType clickType, boolean silent) {
      if (slot != null) {
         clickSlot(slot.id, button, clickType, silent);
      }

   }

   public static void clickSlot(int slotId, int buttonId, SlotActionType clickType, boolean silent) {
      clickSlot(mc.player.currentScreenHandler.syncId, slotId, buttonId, clickType, silent);
   }

   public static void clickSlot(int windowId, int slotId, int buttonId, SlotActionType clickType, boolean silent) {
      mc.interactionManager.clickSlot(windowId, slotId, buttonId, clickType, mc.player);
      if (silent) {
         mc.player.currentScreenHandler.onSlotClick(slotId, buttonId, clickType, mc.player);
      }

   }

   public static Slot getSlot(Item item) {
      return getSlot(item, (s) -> {
         return true;
      });
   }

   public static Slot getSlot(Item item, Predicate<Slot> filter) {
      return getSlot(item, Comparator.comparingInt((s) -> {
         return 0;
      }), filter);
   }

   public static Slot getSlot(Predicate<Slot> filter) {
      return (Slot)slots().filter(filter).findFirst().orElse((Slot)null);
   }

   public static Slot getSlot(Predicate<Slot> filter, Comparator<Slot> comparator) {
      return (Slot)slots().filter(filter).max(comparator).orElse((Slot)null);
   }

   public static Slot getSlot(Item item, Comparator<Slot> comparator, Predicate<Slot> filter) {
      return (Slot)slots().filter((s) -> {
         return s.getStack().getItem().equals(item);
      }).filter(filter).max(comparator).orElse((Slot)null);
   }

   public static Slot getFoodMaxSaturationSlot() {
      return (Slot)slots().filter((s) -> {
         return s.getStack().get(DataComponentTypes.FOOD) != null && !((FoodComponent)s.getStack().get(DataComponentTypes.FOOD)).canAlwaysEat();
      }).max(Comparator.comparingDouble((s) -> {
         return (double)((FoodComponent)s.getStack().get(DataComponentTypes.FOOD)).saturation();
      })).orElse((Slot)null);
   }

   public static Slot getSlot(List<Item> item) {
      return (Slot)slots().filter((s) -> {
         return item.contains(s.getStack().getItem());
      }).findFirst().orElse((Slot)null);
   }

   public static Slot getPotion(RegistryEntry<StatusEffect> effect) {
      return (Slot)slots().filter((s) -> {
         PotionContentsComponent component = (PotionContentsComponent)s.getStack().get(DataComponentTypes.POTION_CONTENTS);
         return component == null ? false : StreamSupport.stream(component.getEffects().spliterator(), false).anyMatch((e) -> {
            return e.getEffectType().equals(effect);
         });
      }).findFirst().orElse((Slot)null);
   }

   public static Slot getPotionFromCategory(StatusEffectCategory category) {
      return (Slot)slots().filter((s) -> {
         ItemStack stack = s.getStack();
         PotionContentsComponent component = (PotionContentsComponent)stack.get(DataComponentTypes.POTION_CONTENTS);
         if (stack.getItem().equals(Items.SPLASH_POTION) && component != null) {
            StatusEffectCategory category2 = category.equals(StatusEffectCategory.BENEFICIAL) ? StatusEffectCategory.HARMFUL : StatusEffectCategory.BENEFICIAL;
            long effects = StreamSupport.stream(component.getEffects().spliterator(), false).filter((e) -> {
               return ((StatusEffect)e.getEffectType().value()).getCategory().equals(category);
            }).count();
            long effects2 = StreamSupport.stream(component.getEffects().spliterator(), false).filter((e) -> {
               return ((StatusEffect)e.getEffectType().value()).getCategory().equals(category2);
            }).count();
            return effects >= effects2;
         } else {
            return false;
         }
      }).findFirst().orElse((Slot)null);
   }

   public static int getInventoryCount(Item item) {
      return IntStream.range(0, 45).filter((i) -> {
         return ((ClientPlayerEntity)Objects.requireNonNull(mc.player)).getInventory().getStack(i).getItem().equals(item);
      }).map((i) -> {
         return mc.player.getInventory().getStack(i).getCount();
      }).sum();
   }

   public static int getHotbarItems(List<Item> items) {
      return IntStream.range(0, 9).filter((i) -> {
         return items.contains(mc.player.getInventory().getStack(i).getItem());
      }).findFirst().orElse(-1);
   }

   public static int getHotbarSlotId(IntPredicate filter) {
      return IntStream.range(0, 9).filter(filter).findFirst().orElse(-1);
   }

   public static int getCount(Predicate<Slot> filter) {
      return slots().filter(filter).mapToInt((s) -> {
         return s.getStack().getCount();
      }).sum();
   }

   public static Slot mainHandSlot() {
      long count = slots().count();
      int i = count == 46L ? 10 : 9;
      return (Slot)slots().toList().get(Math.toIntExact(count - (long)i + (long)mc.player.getInventory().selectedSlot));
   }

   public static boolean isServerScreen() {
      if (mc.player == null) {
         return false;
      } else {
         return slots().toList().size() != 46;
      }
   }

   public static Stream<Slot> slots() {
      return mc.player == null ? Stream.empty() : mc.player.currentScreenHandler.slots.stream();
   }

   public static void jump() {
      if (mc.player.isSprinting()) {
         float g = mc.player.getYaw() * 0.017453292F;
         mc.player.addVelocityInternal(new Vec3d((double)(-MathHelper.sin((double)g) * 0.2F), 0.0D, (double)(MathHelper.cos((double)g) * 0.2F)));
      }

      mc.player.velocityDirty = true;
   }

   public static int indexToSlot(int index) {
      return index >= 0 && index <= 8 ? 36 + index : index;
   }

   public static void sendSequencedPacket(SequencedPacketCreator packetCreator) {
      assert mc.interactionManager != null;

      mc.interactionManager.sendSequencedPacket(mc.world, packetCreator);
   }

   public static void switchSlot(int slot, int previousSlot) {
      if (slot != -1 && previousSlot != -1 && slot != FourEClient.getInstance().getPlayerServis().getServerSlot()) {
         mc.player.getInventory().selectedSlot = slot;
         mc.interactionManager.syncSelectedSlot();
      }
   }

   public static void switchSlot(int slot) {
      if (slot != -1 && slot != FourEClient.getInstance().getPlayerServis().getServerSlot()) {
         mc.player.getInventory().selectedSlot = slot;
         mc.interactionManager.syncSelectedSlot();
      }
   }

   public static void switchBack(int slot, int previousSlot) {
      if (slot != -1 && previousSlot != -1 && slot != FourEClient.getInstance().getPlayerServis().getServerSlot()) {
         mc.player.getInventory().selectedSlot = previousSlot;
         mc.interactionManager.syncSelectedSlot();
         mc.player.getInventory().selectedSlot = slot;
      }
   }

   public static void swap(PlayerInventoryUtil.Swap mode, int slot, int targetSlot) {
      if (slot != -1 && targetSlot != -1) {
         switch(mode.ordinal()) {
         case 0:
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, indexToSlot(slot), 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, indexToSlot(targetSlot), 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, indexToSlot(slot), 0, SlotActionType.PICKUP, mc.player);
            break;
         case 1:
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot, targetSlot, SlotActionType.SWAP, mc.player);
         }

      }
   }

   public static void updateSlotss() {
      ScreenHandler screenHandler = mc.player.currentScreenHandler;
      mc.player.networkHandler.sendPacket(new ClickSlotC2SPacket(screenHandler.syncId, screenHandler.getRevision(), (short)0, (byte)0, SlotActionType.PICKUP_ALL, Int2ObjectMaps.emptyMap(), (ItemStackHash)null));
   }

   public static void closeScreens(boolean packet) {
      if (packet) {
         mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
      } else {
         mc.player.closeHandledScreen();
      }

   }

   public static void swap(int slot, int targetSlot) {
      if (slot != -1 && targetSlot != -1) {
         mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, indexToSlot(slot), 0, SlotActionType.PICKUP, mc.player);
         mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, targetSlot, 0, SlotActionType.PICKUP, mc.player);
         mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, indexToSlot(slot), 0, SlotActionType.PICKUP, mc.player);
      }
   }

   public static void swing() {
      mc.player.swingHand(Hand.MAIN_HAND);
   }

   public static int findBestSword(int start, int end) {
      int netheriteSlot = -1;
      int diamondSlot = -1;
      int ironSlot = -1;
      int goldenSlot = -1;
      int stoneSlot = -1;
      int woodenSlot = -1;

      for(int i = end; i >= start; --i) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.getItem() == Items.NETHERITE_SWORD) {
            netheriteSlot = i;
         } else if (stack.getItem() == Items.DIAMOND_SWORD) {
            diamondSlot = i;
         } else if (stack.getItem() == Items.IRON_SWORD) {
            ironSlot = i;
         } else if (stack.getItem() == Items.GOLDEN_SWORD) {
            goldenSlot = i;
         } else if (stack.getItem() == Items.STONE_SWORD) {
            stoneSlot = i;
         } else if (stack.getItem() == Items.WOODEN_SWORD) {
            woodenSlot = i;
         }
      }

      if (netheriteSlot != -1) {
         return netheriteSlot;
      } else if (diamondSlot != -1) {
         return diamondSlot;
      } else if (ironSlot != -1) {
         return ironSlot;
      } else if (goldenSlot != -1) {
         return goldenSlot;
      } else if (stoneSlot != -1) {
         return stoneSlot;
      } else {
         return woodenSlot;
      }
   }

   public static int findBestAxe(int start, int end) {
      int netheriteSlot = -1;
      int diamondSlot = -1;
      int ironSlot = -1;
      int goldenSlot = -1;
      int stoneSlot = -1;
      int woodenSlot = -1;

      for(int i = end; i >= start; --i) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.getItem() == Items.NETHERITE_AXE) {
            netheriteSlot = i;
         } else if (stack.getItem() == Items.DIAMOND_AXE) {
            diamondSlot = i;
         } else if (stack.getItem() == Items.IRON_AXE) {
            ironSlot = i;
         } else if (stack.getItem() == Items.GOLDEN_AXE) {
            goldenSlot = i;
         } else if (stack.getItem() == Items.STONE_AXE) {
            stoneSlot = i;
         } else if (stack.getItem() == Items.WOODEN_AXE) {
            woodenSlot = i;
         }
      }

      if (netheriteSlot != -1) {
         return netheriteSlot;
      } else if (diamondSlot != -1) {
         return diamondSlot;
      } else if (ironSlot != -1) {
         return ironSlot;
      } else if (goldenSlot != -1) {
         return goldenSlot;
      } else if (stoneSlot != -1) {
         return stoneSlot;
      } else {
         return woodenSlot;
      }
   }

   public static int findFastItem(BlockState blockState, int start, int end) {
      double bestScore = -1.0D;
      int bestSlot = -1;

      for(int i = start; i <= end; ++i) {
         double score = (double)mc.player.getInventory().getStack(i).getMiningSpeedMultiplier(blockState);
         if (score > bestScore) {
            bestScore = score;
            bestSlot = i;
         }
      }

      return bestSlot;
   }

   public static int findBestChestplate(int start, int end) {
      int leatherSlot = -1;
      int chainmail = -1;
      int ironSlot = -1;
      int goldenSlot = -1;
      int diamondSlot = -1;
      int netheriteSlot = -1;

      for(int i = end; i >= start; --i) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.getItem() == Items.LEATHER_CHESTPLATE) {
            leatherSlot = i;
         } else if (stack.getItem() == Items.CHAINMAIL_CHESTPLATE) {
            chainmail = i;
         } else if (stack.getItem() == Items.IRON_CHESTPLATE) {
            ironSlot = i;
         } else if (stack.getItem() == Items.GOLDEN_CHESTPLATE) {
            goldenSlot = i;
         } else if (stack.getItem() == Items.DIAMOND_CHESTPLATE) {
            diamondSlot = i;
         } else if (stack.getItem() == Items.NETHERITE_CHESTPLATE) {
            netheriteSlot = i;
         }
      }

      if (chainmail != -1) {
         return chainmail;
      } else if (ironSlot != -1) {
         return ironSlot;
      } else if (goldenSlot != -1) {
         return goldenSlot;
      } else if (diamondSlot != -1) {
         return diamondSlot;
      } else if (netheriteSlot != -1) {
         return netheriteSlot;
      } else {
         return leatherSlot;
      }
   }

   public static int getArmorColor(PlayerEntity entity, int slot) {
      ItemStack stack = entity.getInventory().getStack(36 + slot);
      return stack.isIn(ItemTags.DYEABLE) ? DyedColorComponent.getColor(stack, -6265536) : -1;
   }

   public static int find(Item item) {
      return find((Item)item, 0, 35);
   }

   public static int findHotbar(Item item) {
      return find((Item)item, 0, 8);
   }

   public static int findInventory(Item item) {
      return find((Item)item, 9, 35);
   }

   public static int find(Class<? extends Item> item) {
      return find((Class)item, 0, 35);
   }

   public static int findHotbar(Class<? extends Item> item) {
      return find((Class)item, 0, 8);
   }

   public static int findInventory(Class<? extends Item> item) {
      return find((Class)item, 9, 35);
   }

   public static int find(Item item, int start, int end) {
      for(int i = end; i >= start; --i) {
         if (mc.player.getInventory().getStack(i).getItem() == item) {
            return i;
         }
      }

      return -1;
   }

   public static int find(Class<? extends Item> item, int start, int end) {
      for(int i = end; i >= start; --i) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.getItem().getClass().isAssignableFrom(item)) {
            return i;
         }
      }

      return -1;
   }

   public static int findEmptySlot(int start, int end) {
      for(int i = end; i >= start; --i) {
         if (mc.player.getInventory().getStack(i).isEmpty()) {
            return i;
         }
      }

      return -1;
   }

   @Generated
   private PlayerInventoryUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

   public static enum Swap {
      Pickup,
      Swap;

      // $FF: synthetic method
      private static PlayerInventoryUtil.Swap[] $values() {
         return new PlayerInventoryUtil.Swap[]{Pickup, Swap};
      }
   }
}
