package foure.dev.mixin.game;

import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Scoreboard.class})
public abstract class RwMixin {
   @Shadow
   @Nullable
   public abstract Team getScoreHolderTeam(String var1);

   @Inject(
           method = {"removeScoreHolderFromTeam"},
           at = {@At("HEAD")},
           cancellable = true
   )
   public void fuckRw(String scoreHolderName, Team team, CallbackInfo ci) {
      if (this.getScoreHolderTeam(scoreHolderName) != team) {
         ci.cancel();
      }

   }
}
