package foure.dev.module.api;

import com.google.common.eventbus.Subscribe;
import foure.dev.FourEClient;
import foure.dev.event.impl.input.EventMouseScroll;
import foure.dev.event.impl.presss.EventMouseButton;
import foure.dev.event.impl.presss.EventPress;
import foure.dev.module.setting.api.Setting;
import foure.dev.ui.notification.NotificationManager;
import foure.dev.ui.notification.NotificationType;
import foure.dev.util.wrapper.Wrapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import lombok.Generated;

public class Function implements Wrapper {
   private final String name;
   private final String desc;
   private final Category category;
   private int key = -1;
   private boolean state;
   private boolean binding;
   private Function.BindMode bindMode;
   private final boolean isVisual;
   private final List<Setting<?>> settings;

   public Function() {
      this.bindMode = Function.BindMode.TOGGLE;
      this.settings = new ArrayList();
      ModuleInfo info = (ModuleInfo)this.getClass().getAnnotation(ModuleInfo.class);
      Objects.requireNonNull(info, "ModuleInfo annotation is required");
      this.name = info.name();
      this.desc = info.desc();
      this.category = info.category();
      this.isVisual = info.visual();
   }

   public void onEnable() {
      FourEClient.getInstance().getEventBus().register(this);
   }

   public void onDisable() {
      FourEClient.getInstance().getEventBus().unregister(this);
   }

   public void toggle() {
      this.setState(!this.state);
   }

   public void setState(boolean newState) {
      if (this.state != newState) {
         this.state = newState;
         if (this.state) {
            this.onEnable();
            NotificationManager.add(this.getName(), "Enable", NotificationType.SUCCESS);
         } else {
            this.onDisable();
            NotificationManager.add(this.getName(), "Disabled", NotificationType.DISABLE);
         }
      }
   }

   // ── Icon support ──────────────────────────────────────────────────────
   public String getIcon() {
      ModuleInfo info = this.getClass().getAnnotation(ModuleInfo.class);
      if (info == null) return "";
      return info.icon();
   }

   public boolean hasIcon() {
      return !this.getIcon().isEmpty();
   }
   // ─────────────────────────────────────────────────────────────────────

   @Subscribe
   public void onKey(EventPress e) {
      if (mc.currentScreen != null) return;
      if (this.binding && e.getAction() == 1) {
         this.key = e.getKey();
         if (this.key == 256 || this.key == 261) {
            this.key = -1;
         }
         this.binding = false;
      } else if (!this.binding) {
         if (this.key != -1) {
            if (e.getKey() == this.key) {
               if (this.bindMode == Function.BindMode.TOGGLE) {
                  if (e.getAction() == 1) {
                     this.toggle();
                  }
               } else if (e.getAction() == 1) {
                  this.setState(true);
               } else if (e.getAction() == 0) {
                  this.setState(false);
               }
            }
         }
      }
   }

   @Subscribe
   public void onMouse(EventMouseButton e) {
      if (this.binding && e.getAction() == 1) {
         this.key = 1000 + e.getButton();
         this.binding = false;
      } else {
         if (!this.binding && this.key == 1000 + e.getButton()) {
            if (this.bindMode == Function.BindMode.TOGGLE) {
               if (e.getAction() == 1) {
                  this.toggle();
               }
            } else if (e.getAction() == 1) {
               this.setState(true);
            } else if (e.getAction() == 0) {
               this.setState(false);
            }
         }
      }
   }

   @Subscribe
   public void onScroll(EventMouseScroll e) {
      if (this.binding) {
         this.key = e.getDelta() > 0.0D ? 2000 : 2001;
         this.binding = false;
      }
   }

   @Subscribe
   public void onKeyToggle(EventPress e) {
      if (!this.binding) {
         if (e.getAction() == 1 && e.getKey() == this.key) {
            this.toggle();
         }
      }
   }

   @Subscribe
   public void onMouseToggle(EventMouseButton e) {
      if (!this.binding) {
         if (e.getAction() == 1) {
            if (this.key >= 1000 && this.key == 1000 + e.getButton()) {
               this.toggle();
            }
         }
      }
   }

   @Subscribe
   public void onScrollToggle(EventMouseScroll e) {
      if (!this.binding) {
         if (this.key == 2000 && e.getDelta() > 0.0D) {
            this.toggle();
         }
         if (this.key == 2001 && e.getDelta() < 0.0D) {
            this.toggle();
         }
      }
   }

   public void startBinding() {
      this.binding = true;
   }

   public void stopBinding() {
      this.binding = false;
   }

   public void setKey(int key) {
      this.key = key;
   }

   public int getKey() {
      return this.key;
   }

   public boolean isBinding() {
      return this.binding;
   }

   public boolean isToggled() {
      return this.state;
   }

   public boolean canBindToKey(int key) {
      return key != 256;
   }

   protected void addSettings(Setting<?>... settings) {
      this.settings.addAll(Arrays.asList(settings));
      FourEClient.getInstance().getSettingManager().addSettings(settings);
   }

   public static boolean fullNullCheck() {
      return mc.player == null || mc.world == null;
   }

   @Generated
   public void setBinding(boolean binding) {
      this.binding = binding;
   }

   @Generated
   public void setBindMode(Function.BindMode bindMode) {
      this.bindMode = bindMode;
   }

   @Generated
   public String getName() {
      return this.name;
   }

   @Generated
   public String getDesc() {
      return this.desc;
   }

   @Generated
   public Category getCategory() {
      return this.category;
   }

   @Generated
   public boolean isState() {
      return this.state;
   }

   @Generated
   public Function.BindMode getBindMode() {
      return this.bindMode;
   }

   @Generated
   public boolean isVisual() {
      return this.isVisual;
   }

   @Generated
   public List<Setting<?>> getSettings() {
      return this.settings;
   }

   public static enum BindMode {
      TOGGLE,
      HOLD;

      private static Function.BindMode[] $values() {
         return new Function.BindMode[]{TOGGLE, HOLD};
      }
   }
}
