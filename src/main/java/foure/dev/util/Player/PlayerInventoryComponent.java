package foure.dev.util.Player;

import foure.dev.event.impl.input.InputEvent;
import foure.dev.module.api.Function;
import foure.dev.module.impl.combat.helper.AngleUtil;
import foure.dev.module.impl.combat.helper.RotationConfig;
import foure.dev.module.impl.combat.helper.RotationController;
import foure.dev.util.Script.TaskPriority;
import foure.dev.util.Script.scripts.Script;
import foure.dev.util.others.ServerUtil;
import foure.dev.util.wrapper.Wrapper;
import java.util.List;
import java.util.Objects;
import lombok.Generated;
import net.minecraft.client.gui.screen.ingame.AbstractCommandBlockScreen;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.client.gui.screen.ingame.StructureBlockScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

public final class PlayerInventoryComponent implements Wrapper {
   public static final List<KeyBinding> moveKeys;
   public static final Script script;
   public static final Script postScript;
   public static boolean canMove;
   private static boolean wasSprinting;

   public static void tick() {
      script.update();
   }

   public static void postMotion() {
      postScript.update();
   }

   public static void input(InputEvent e) {
      if (!canMove) {
         e.inputNone();
      }

   }

   public static void addTask(Runnable task) {
      if (script.isFinished() && MobilityHandler.hasPlayerMovement()) {
         String var1 = ServerUtil.server;
         byte var2 = -1;
         switch(var1.hashCode()) {
         case -1669779471:
            if (var1.equals("LonyGrief")) {
               var2 = 1;
            }
            break;
         case -912404296:
            if (var1.equals("SpookyTime")) {
               var2 = 3;
            }
            break;
         case -441313278:
            if (var1.equals("CopyTime")) {
               var2 = 4;
            }
            break;
         case 935423623:
            if (var1.equals("ReallyWorld")) {
               var2 = 2;
            }
            break;
         case 1154553036:
            if (var1.equals("FunTime")) {
               var2 = 0;
            }
         }

         Script var10000;
         switch(var2) {
         case 0:
            script.cleanup().addTickStep(0, () -> {
               disableMoveKeys();
               rotateToCamera();
            }).addTickStep(1, () -> {
               task.run();
               enableMoveKeys();
            });
            return;
         case 1:
            script.cleanup().addTickStep(0, () -> {
               disableMoveKeys();
               rotateToCamera();
            }).addTickStep(1, () -> {
               task.run();
               enableMoveKeys();
            });
            return;
         case 2:
            if (mc.player.isOnGround()) {
               var10000 = script.cleanup().addTickStep(0, PlayerInventoryComponent::disableMoveKeys).addTickStep(2, PlayerInventoryComponent::rotateToCamera);
               Objects.requireNonNull(task);
               var10000.addTickStep(3, task::run).addTickStep(4, PlayerInventoryComponent::enableMoveKeys);
               return;
            }
            break;
         case 3:
         case 4:
            var10000 = script.cleanup().addTickStep(0, () -> {
               disableMoveKeys();
               rotateToCamera();
            });
            Objects.requireNonNull(task);
            var10000.addTickStep(1, task::run).addTickStep(2, PlayerInventoryComponent::enableMoveKeys);
            return;
         }
      }

      script.addTickStep(0, PlayerInventoryComponent::rotateToCamera);
      postScript.cleanup().addTickStep(0, () -> {
         task.run();
         PlayerInventoryUtil.closeScreen(true);
      });
   }

   private static void rotateToCamera() {
      Function module = null;
      RotationController.INSTANCE.rotateTo(AngleUtil.cameraAngle(), RotationConfig.DEFAULT, TaskPriority.HIGH_IMPORTANCE_3, (Function)null);
   }

   public static void disableMoveKeys() {
      if (mc.player != null) {
         wasSprinting = mc.player.isSprinting();
      }

      canMove = false;
      unPressMoveKeys();
   }

   public static void enableMoveKeys() {
      canMove = true;
      updateMoveKeys();
      if (mc.player != null && wasSprinting) {
         mc.player.setSprinting(true);
      }

   }

   public static void unPressMoveKeys() {
      moveKeys.forEach((keyBinding) -> {
         keyBinding.setPressed(false);
      });
   }

   public static void updateMoveKeys() {
      moveKeys.forEach((keyBinding) -> {
         keyBinding.setPressed(InputUtil.isKeyPressed(mc.getWindow(), keyBinding.getDefaultKey().getCode()));
      });
   }

   public static boolean shouldSkipExecution() {
      return mc.currentScreen != null && !PlayerIntersectionUtil.isChat(mc.currentScreen) && !(mc.currentScreen instanceof SignEditScreen) && !(mc.currentScreen instanceof AnvilScreen) && !(mc.currentScreen instanceof AbstractCommandBlockScreen) && !(mc.currentScreen instanceof StructureBlockScreen);
   }

   @Generated
   private PlayerInventoryComponent() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

   static {
      moveKeys = List.of(mc.options.forwardKey, mc.options.backKey, mc.options.leftKey, mc.options.rightKey, mc.options.jumpKey, mc.options.sprintKey);
      script = new Script();
      postScript = new Script();
      canMove = true;
      wasSprinting = false;
   }
}
