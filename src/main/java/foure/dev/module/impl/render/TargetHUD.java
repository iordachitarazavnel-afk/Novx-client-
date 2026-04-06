package foure.dev.module.impl.render;

import com.google.common.eventbus.Subscribe;
import foure.dev.FourEClient;
import foure.dev.event.impl.render.RenderEvent;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.HudModule;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.impl.combat.Killaura;
import foure.dev.util.render.core.Renderer2D;
import foure.dev.util.render.text.FontRegistry;
import java.awt.Color;
import java.util.Iterator;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;

@ModuleInfo(
   name = "TargetHUD",
   category = Category.RENDER,
   desc = "Displays target information"
)
public class TargetHUD extends HudModule {
   private Killaura killaura;
   private final int healthColor1 = (new Color(110, 60, 240)).getRGB();
   private final int healthColor2 = (new Color(70, 30, 180)).getRGB();
   private float animationHealth = 0.0F;
   private float scale = 0.0F;
   private long lastTime = System.currentTimeMillis();
   private LivingEntity renderTarget = null;

   public TargetHUD() {
      this.setX(200.0F);
      this.setY(200.0F);
      this.setWidth(150.0F);
      this.setHeight(55.0F);
   }

   @Subscribe
   public void onRender(RenderEvent event) {
      if (this.killaura == null) {
         this.getKillaura();
      }

      long now = System.currentTimeMillis();
      float delta = (float)(now - this.lastTime) / 1000.0F;
      this.lastTime = now;
      LivingEntity target = null;
      if (this.killaura != null && this.killaura.target != null) {
         target = this.killaura.target;
      } else if (mc.targetedEntity instanceof LivingEntity) {
         target = (LivingEntity)mc.targetedEntity;
      } else if (mc.currentScreen instanceof ChatScreen && mc.player != null) {
         target = mc.player;
      }

      if (target != null) {
         this.renderTarget = (LivingEntity)target;
         this.scale = MathHelper.lerp(delta * 5.0F, this.scale, 1.0F);
      } else {
         this.scale = MathHelper.lerp(delta * 5.0F, this.scale, 0.0F);
      }

      if (this.scale < 0.01F) {
         this.scale = 0.0F;
         if (target == null) {
            return;
         }
      } else if (this.scale > 0.99F) {
         this.scale = 1.0F;
      }

      LivingEntity displayEntity = target != null ? target : this.renderTarget;
      if (displayEntity != null) {
         if (this.scale != 0.0F || target != null) {
            Renderer2D r = event.renderer();
            float x = this.getX();
            float y = this.getY();
            float width = this.getWidth();
            float height = this.getHeight();
            float pivotX = x + width / 2.0F;
            float pivotY = y + height / 2.0F;
            r.pushScale(this.scale, pivotX, pivotY);
            r.pushAlpha(this.scale);
            if (((LivingEntity)displayEntity).hurtTime > 0) {
               float shakeIntensity = (float)((LivingEntity)displayEntity).hurtTime * 0.5F;
               x += (float)(Math.sin((double)System.currentTimeMillis() / 20.0D) * (double)shakeIntensity);
               y += (float)(Math.cos((double)System.currentTimeMillis() / 20.0D) * (double)shakeIntensity);
            }

            r.prepareBlurRegion(x, y, width, height, 8.0F);
            r.blurRegion(x, y, width, height, 10.0F, 1.0F);
            int baseR = 20;
            int baseG = 5;
            int baseB = 25;
            int baseA = 180;
            if (((LivingEntity)displayEntity).hurtTime > 0) {
               baseR = Math.min(255, baseR + ((LivingEntity)displayEntity).hurtTime * 10);
               baseA = Math.min(240, baseA + ((LivingEntity)displayEntity).hurtTime * 5);
            }

            int bgCol = (new Color(baseR, baseG, baseB, baseA)).getRGB();
            int bgCol2 = (new Color(baseR + 10, baseG + 15, baseB + 25, baseA)).getRGB();
            r.gradient(x, y, width, height, 10.0F, bgCol, bgCol2, bgCol2, bgCol);
            r.gradient(x + 1.0F, y + 1.0F, width - 2.0F, 15.0F, 10.0F, (new Color(150, 100, 200, 40)).getRGB(), (new Color(100, 50, 150, 20)).getRGB(), (new Color(100, 50, 150, 0)).getRGB(), (new Color(150, 100, 200, 0)).getRGB());
            float pulse = (float)(Math.sin((double)System.currentTimeMillis() / 500.0D) * 0.5D + 0.5D);
            int pulsingOutline = (new Color(110, 60, 240 + (int)(15.0F * pulse), 80 + (int)(60.0F * pulse))).getRGB();
            r.rectOutline(x, y, width, height, 10.0F, pulsingOutline, 1.5F);
            r.shadow(x, y, width, height, 15.0F, 6.0F, 2.0F, (new Color(50, 10, 80, 120)).getRGB());
            boolean rightSide = this.isRightSide();
            float padding = 8.0F;
            float headSize = 38.0F;
            float headX;
            float infoX;
            if (rightSide) {
               headX = x + width - padding - headSize;
               infoX = x + padding;
            } else {
               headX = x + padding;
               infoX = x + padding + headSize + 10.0F;
            }

            float headY = y + 8.5F;
            r.gradient(headX, headY, headSize, headSize, 6.0F, (new Color(140, 90, 240, 255)).getRGB(), (new Color(180, 130, 255, 255)).getRGB(), (new Color(100, 60, 200, 255)).getRGB(), (new Color(150, 110, 250, 255)).getRGB());
            r.rectOutline(headX, headY, headSize, headSize, 6.0F, (new Color(255, 255, 255, 100)).getRGB(), 1.0F);
            float eyeSize = 4.0F;
            r.rect(headX + 8.0F, headY + 12.0F, eyeSize, eyeSize, 1.0F, (new Color(255, 255, 255, 200)).getRGB());
            r.rect(headX + headSize - 8.0F - eyeSize, headY + 12.0F, eyeSize, eyeSize, 1.0F, (new Color(255, 255, 255, 200)).getRGB());
            float infoWidth = width - headSize - padding * 2.0F - 10.0F;
            float nameW = r.getStringWidth(FontRegistry.INTER_SEMIBOLD, ((LivingEntity)displayEntity).getName().getString(), 10.0F);
            float nameX = rightSide ? infoX + infoWidth - nameW : infoX;
            r.text(FontRegistry.INTER_SEMIBOLD, nameX, y + 12.0F, 10.0F, ((LivingEntity)displayEntity).getName().getString(), -1);
            float maxHealth = ((LivingEntity)displayEntity).getMaxHealth();
            float health = MathHelper.clamp(((LivingEntity)displayEntity).getHealth(), 0.0F, maxHealth);
            float absorb = ((LivingEntity)displayEntity).getAbsorptionAmount();
            this.animationHealth += (health - this.animationHealth) * 0.15F;
            float barY = y + 36.0F;
            float barH = 5.0F;
            String hpText = String.format("%.1f HP", health + absorb);
            String pctText = String.format("%.0f%%", Math.min(this.animationHealth / maxHealth, 1.0F) * 100.0F);
            float hpW = r.getStringWidth(FontRegistry.INTER_MEDIUM, hpText, 7.0F);
            float pctW = r.getStringWidth(FontRegistry.INTER_MEDIUM, pctText, 7.0F);
            if (rightSide) {
               r.text(FontRegistry.INTER_MEDIUM, infoX + infoWidth - hpW, barY - 8.0F, 7.0F, hpText, (new Color(180, 180, 200)).getRGB());
               r.text(FontRegistry.INTER_MEDIUM, infoX, barY - 8.0F, 7.0F, pctText, (new Color(180, 180, 200)).getRGB());
            } else {
               r.text(FontRegistry.INTER_MEDIUM, infoX, barY - 8.0F, 7.0F, hpText, (new Color(180, 180, 200)).getRGB());
               r.text(FontRegistry.INTER_MEDIUM, infoX + infoWidth - pctW, barY - 8.0F, 7.0F, pctText, (new Color(180, 180, 200)).getRGB());
            }

            r.rect(infoX, barY, infoWidth, barH, 2.5F, (new Color(30, 20, 40, 150)).getRGB());
            float healthPct = Math.min(this.animationHealth / maxHealth, 1.0F);
            float barFillW = infoWidth * healthPct;
            if (barFillW > 0.0F) {
               float barX = rightSide ? infoX + infoWidth - barFillW : infoX;
               int c1 = this.healthColor1;
               int c2 = this.healthColor2;
               r.gradient(barX, barY, barFillW, barH, 2.5F, c1, c2, c2, c1);
               if (barFillW > 5.0F) {
                  r.shadow(barX, barY, barFillW, barH, 5.0F, 4.0F, 1.0F, (new Color(110, 60, 240, 100)).getRGB());
               }
            }

            r.popAlpha();
            r.popScale();
         }
      }
   }

   private void getKillaura() {
      Iterator var1 = FourEClient.getInstance().getFunctionManager().getModules().iterator();

      while(var1.hasNext()) {
         Function f = (Function)var1.next();
         if (f instanceof Killaura) {
            this.killaura = (Killaura)f;
            break;
         }
      }

   }
}
