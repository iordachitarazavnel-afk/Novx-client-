package foure.dev.util.others;

import java.util.ArrayList;
import java.util.List;
import lombok.Generated;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

public final class Friends {
   public static final List<Friend> friends = new ArrayList();

   public static void addFriend(PlayerEntity player) {
      addFriend(player.getName().getString());
   }

   public static void addFriend(String name) {
      friends.add(new Friend(name));
   }

   public static void removeFriend(PlayerEntity player) {
      removeFriend(player.getName().getString());
   }

   public static void removeFriend(String name) {
      friends.removeIf((friend) -> {
         return friend.getName().equalsIgnoreCase(name);
      });
   }

   public static boolean isFriend(Entity entity) {
      if (entity instanceof PlayerEntity) {
         PlayerEntity player = (PlayerEntity)entity;
         return isFriend(player.getName().getString());
      } else {
         return false;
      }
   }

   public static boolean isFriend(String friend) {
      return friends.stream().anyMatch((isFriend) -> {
         return isFriend.getName().equals(friend);
      });
   }

   public static void clear() {
      friends.clear();
   }

   @Generated
   private Friends() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

   @Generated
   public static List<Friend> getFriends() {
      return friends;
   }
}
