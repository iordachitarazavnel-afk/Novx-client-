package foure.dev.mixin.game;

import foure.dev.util.others.ServerUtil;
import foure.dev.util.render.utils.ChatUtils;
import java.util.Set;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ClientPlayNetworkHandler.class})
public abstract class ClientPlayNetworkHandlerMixin {
   @Unique
   private static final Set<String> problem = Set.of("hub", "lobby", "рги", "дщиин", "дуфм", "дуфму", "leave", "leav", "logout");
   @Unique
   private String last = null;
   @Unique
   private long time = 0L;

   @Inject(
           method = {"sendChatCommand"},
           at = {@At("HEAD")},
           cancellable = true
   )
   public void onSendChatCommand(String command, CallbackInfo ci) {
      String fullCommand = command.trim();
      if (!fullCommand.isEmpty()) {
         String baseCommand = fullCommand.split(" ")[0].toLowerCase();
         if (problem.contains(baseCommand) && ServerUtil.inPvp()) {
            long now = System.currentTimeMillis();
            if (fullCommand.equalsIgnoreCase(this.last) && now - this.time <= 3000L) {
               this.last = null;
               this.time = 0L;
            } else {
               this.last = fullCommand;
               this.time = now;
               String var10000 = String.valueOf(Formatting.RED);
               ChatUtils.sendMessage(var10000 + "Вы в PvP! " + String.valueOf(Formatting.GRAY) + "Введите команду ещё раз для подтверждения: " + String.valueOf(Formatting.YELLOW) + "/" + fullCommand);
               ci.cancel();
            }
         }

      }
   }
}
