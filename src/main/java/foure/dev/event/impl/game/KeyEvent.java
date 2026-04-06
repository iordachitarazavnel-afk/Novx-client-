package foure.dev.event.impl.game;

import foure.dev.event.api.Event;
import foure.dev.util.wrapper.Wrapper;
import lombok.Generated;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil.Type;

public class KeyEvent extends Event implements Wrapper {
   private final Screen screen;
   private final Type type;
   private final int key;
   private final int action;

   public KeyEvent(Screen screen, Type type, int key, int action) {
      this.screen = screen;
      this.type = type;
      this.key = key;
      this.action = action;
   }

   public boolean isKeyDown(int key) {
      return this.isKeyDown(key, mc.currentScreen == null);
   }

   public boolean isKeyDown(int key, boolean screen) {
      return this.key == key && this.action == 1 && screen;
   }

   public boolean isKeyReleased(int key) {
      return this.isKeyReleased(key, mc.currentScreen == null);
   }

   public boolean isKeyReleased(int key, boolean screen) {
      return this.key == key && this.action == 0 && screen;
   }

   @Generated
   public Screen getScreen() {
      return this.screen;
   }

   @Generated
   public Type getType() {
      return this.type;
   }

   @Generated
   public int getKey() {
      return this.key;
   }

   @Generated
   public int getAction() {
      return this.action;
   }
}
