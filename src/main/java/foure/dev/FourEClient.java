package foure.dev;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import foure.dev.config.ConfigManager;
import foure.dev.event.impl.presss.EventPress;
import foure.dev.event.impl.render.RenderEvent;
import foure.dev.module.FunctionManager;
import foure.dev.module.api.Function;
import foure.dev.module.impl.combat.helper.attack.AttackPerpetrator;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.api.SettingManager;
import foure.dev.module.setting.impl.BooleanSetting;
import foure.dev.ui.notification.NotificationManager;
import foure.dev.util.Player.PlayerServis;
import foure.dev.util.others.Lisener.ListenerRepository;
import foure.dev.util.render.animation.AnimationSystem;
import foure.dev.util.render.backends.gl.GlBackend;
import foure.dev.util.render.backends.gl.GlState;
import foure.dev.util.render.core.Renderer2D;
import foure.dev.util.render.text.FontObject;
import foure.dev.util.render.text.FontRegistry;
import java.util.Iterator;
import lombok.Generated;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;

public class FourEClient implements ModInitializer {
   private static FourEClient instance;
   public EventBus eventBus;
   public SettingManager settingManager;
   public FunctionManager functionManager;
   public ConfigManager configManager;
   public static GlBackend backend;
   public static Renderer2D renderer;
   public static FontObject uiFont;
   private PlayerServis playerServis;
   public static boolean initialized = false;
   ListenerRepository listenerRepository;
   AttackPerpetrator attackPerpetrator = new AttackPerpetrator();
   public boolean panic;

   public static FourEClient getInstance() {
      return instance;
   }

   public EventBus getEventBus() {
      return this.eventBus;
   }

   public SettingManager getSettingManager() {
      return this.settingManager;
   }

   public FunctionManager getFunctionManager() {
      return this.functionManager;
   }

   public ConfigManager getConfigManager() {
      return this.configManager;
   }

   public AttackPerpetrator getAttackPerpetrator() {
      return this.attackPerpetrator;
   }

   public Renderer2D getRenderer2D() {
      return renderer;
   }

   private static synchronized void onInit() {
      if (!initialized) {
         backend = new GlBackend();
         renderer = new Renderer2D(backend);
         FontRegistry.initialize(backend, renderer);
         uiFont = FontRegistry.INTER_MEDIUM;
         initialized = true;
      }
   }

   public FourEClient() {
      instance = this;
   }

   public void onInitialize() {
      this.eventBus = new EventBus();
      this.settingManager = new SettingManager();
      this.functionManager = new FunctionManager();
      this.configManager = new ConfigManager();
      this.playerServis = new PlayerServis();
      this.configManager.load("default");
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
         this.configManager.save("default");
      }));
      this.initListeners();
      this.eventBus.register(this);
   }

   @Subscribe
   public void onPresss(EventPress event) {
      if (event.getAction() == 1) {
         Iterator var2 = this.functionManager.getModules().iterator();

         while(var2.hasNext()) {
            Function module = (Function)var2.next();
            if (module.getKey() == event.getKey()) {
               module.toggle();
            }

            Iterator var4 = module.getSettings().iterator();

            while(var4.hasNext()) {
               Setting<?> setting = (Setting)var4.next();
               if (setting instanceof BooleanSetting) {
                  BooleanSetting boolSetting = (BooleanSetting)setting;
                  if (boolSetting.getKey() == event.getKey()) {
                     boolSetting.toggle();
                  }
               }
            }
         }
      }

   }

   private void initListeners() {
      this.listenerRepository = new ListenerRepository();
      this.listenerRepository.setup();
   }

   public static void onRender() {
      GlState.Snapshot snapshot = GlState.push();

      try {
         if (!initialized) {
            onInit();
         }

         MinecraftClient client = MinecraftClient.getInstance();
         if (client != null && client.getWindow() != null && client.player != null && client.world != null) {
            int width = client.getWindow().getFramebufferWidth();
            int height = client.getWindow().getFramebufferHeight();
            if (width <= 0 || height <= 0) {
               return;
            }

            AnimationSystem.getInstance().tick();

            try {
               renderer.begin(width, height);

               try {
                  float scaledWidth = (float)client.getWindow().getScaledWidth();
                  float scaledHeight = (float)client.getWindow().getScaledHeight();
                  RenderEvent renderEvent = new RenderEvent(client, renderer, uiFont, width, height, scaledWidth, scaledHeight);
                  renderEvent.call();
                  NotificationManager.render(renderer);
                  return;
               } finally {
                  renderer.end();
               }
            } catch (Exception var16) {
               throw new RuntimeException(var16);
            }
         }
      } finally {
         GlState.pop(snapshot);
      }

   }

   @Generated
   public PlayerServis getPlayerServis() {
      return this.playerServis;
   }

   @Generated
   public ListenerRepository getListenerRepository() {
      return this.listenerRepository;
   }

   @Generated
   public boolean isPanic() {
      return this.panic;
   }

   @Generated
   public void setPanic(boolean panic) {
      this.panic = panic;
   }
}
