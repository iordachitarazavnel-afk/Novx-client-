package foure.dev.module.impl.render;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.game.EventUpdate;
import foure.dev.event.impl.player.EventAttackEntity;
import foure.dev.event.impl.render.RenderEvent;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.impl.BooleanSetting;
import foure.dev.module.setting.impl.ColorSetting;
import foure.dev.module.setting.impl.ModeSetting;
import foure.dev.module.setting.impl.NumberSetting;
import foure.dev.util.math.ProjectionUtil;
import foure.dev.util.render.core.Renderer2D;
import java.awt.Color;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(
   name = "HitParticles",
   category = Category.RENDER,
   desc = "Spawns glowing particles on attack"
)
public class HitParticles extends Function {
   private final ModeSetting physics = new ModeSetting("Physics", this, "Fling", new String[]{"Fling", "Drop", "Float"});
   private final ColorSetting color = new ColorSetting("Color", this, new Color(255, 200, 50, 255));
   private final NumberSetting amount = new NumberSetting("Amount", this, 3.0D, 1.0D, 20.0D, 1.0D);
   private final NumberSetting size = new NumberSetting("Size", this, 3.0D, 0.5D, 10.0D, 0.5D);
   private final NumberSetting speed = new NumberSetting("Speed", this, 1.0D, 0.1D, 3.0D, 0.1D);
   private final NumberSetting maxLife = new NumberSetting("MaxLife", this, 1.0D, 0.1D, 3.0D, 0.1D);
   private final BooleanSetting bubblesEnabled = new BooleanSetting("Bubbles", true);
   private final List<HitParticles.Particle> particles = new ArrayList();
   private final List<HitParticles.HitBubble> bubbles = new CopyOnWriteArrayList();
   private final Identifier bubbleTexture = Identifier.of("textures/swirl.png");
   private boolean lastAttackKeyPressed = false;
   private final Identifier starTexture = Identifier.of("textures/star.png");

   public HitParticles() {
      this.addSettings(new Setting[]{this.physics, this.color, this.amount, this.size, this.speed, this.maxLife, this.bubblesEnabled});
   }

   public void onEnable() {
      super.onEnable();
      this.particles.clear();
      this.bubbles.clear();
   }

   public void onDisable() {
      super.onDisable();
      this.particles.clear();
      this.bubbles.clear();
   }

   @Subscribe
   public void onTick(EventUpdate event) {
      if ((Boolean)this.bubblesEnabled.getValue()) {
         boolean currentAttack = mc.options.attackKey.isPressed();
         if (currentAttack && !this.lastAttackKeyPressed && mc.player != null) {
            LivingEntity target = this.getTarget();
            if (target != null) {
               Vec3d bubblePos = this.getHitPosition(target);
               this.bubbles.add(new HitParticles.HitBubble(bubblePos, System.currentTimeMillis()));
            }
         }

         this.lastAttackKeyPressed = currentAttack;
         this.bubbles.removeIf((b) -> {
            return System.currentTimeMillis() - b.startTime > 3000L;
         });
      }
   }

   @Subscribe
   public void onAttack(EventAttackEntity event) {
      if (event.getTarget() != null) {
         Entity target = event.getTarget();
         Vec3d pos = new Vec3d(target.getX(), target.getY() + (double)target.getHeight() / 2.0D, target.getZ());
         int count = ((Double)this.amount.getValue()).intValue();

         for(int i = 0; i < count; ++i) {
            this.spawnParticle(pos);
         }

      }
   }

   private void spawnParticle(Vec3d pos) {
      double px = pos.x + (Math.random() - 0.5D) * 0.5D;
      double py = pos.y + (Math.random() - 0.5D) * 0.5D;
      double pz = pos.z + (Math.random() - 0.5D) * 0.5D;
      double vx = (Math.random() - 0.5D) * (Double)this.speed.getValue();
      double vy = (Math.random() - 0.5D) * (Double)this.speed.getValue();
      double vz = (Math.random() - 0.5D) * (Double)this.speed.getValue();
      String mode = (String)this.physics.getValue();
      if ("Drop".equals(mode)) {
         vy = Math.abs(vy) * 0.5D;
      } else if ("Float".equals(mode)) {
         vy = Math.abs(vy) * 0.5D + 0.1D;
      }

      this.particles.add(new HitParticles.Particle(new Vec3d(px, py, pz), new Vec3d(vx, vy, vz), System.currentTimeMillis()));
   }

   private LivingEntity getTarget() {
      if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == Type.ENTITY) {
         EntityHitResult entityHit = (EntityHitResult)mc.crosshairTarget;
         Entity entity = entityHit.getEntity();
         if (entity instanceof LivingEntity) {
            LivingEntity livingEntity = (LivingEntity)entity;
            return livingEntity;
         }
      }

      return null;
   }

   private Vec3d getHitPosition(LivingEntity target) {
      if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == Type.ENTITY) {
         EntityHitResult entityHit = (EntityHitResult)mc.crosshairTarget;
         return entityHit.getPos();
      } else {
         return new Vec3d(target.getX(), target.getY() + (double)(target.getHeight() / 2.0F), target.getZ());
      }
   }

   @Subscribe
   public void onRender2D(RenderEvent event) {
      Renderer2D r = event.renderer();
      AbstractTexture starTex;
      int starTexId;
      float age;
      float life;
      float alphaPct;
      float pSize;
      int rgb;
      if ((Boolean)this.bubblesEnabled.getValue() && !this.bubbles.isEmpty()) {
         starTex = mc.getTextureManager().getTexture(this.bubbleTexture);
         starTexId = -1;
         if (starTex != null) {
            starTexId = this.getGlId(starTex);
         }

         if (starTexId != -1) {
            Iterator var5 = this.bubbles.iterator();

            while(var5.hasNext()) {
               HitParticles.HitBubble bubble = (HitParticles.HitBubble)var5.next();
               long passedMs = System.currentTimeMillis() - bubble.startTime;
               float progress = (float)passedMs / 3000.0F;
               if (!(progress >= 1.0F)) {
                  float easeOut = (float)Math.pow((double)progress, 0.5D);
                  age = easeOut * 4.0F;
                  life = 1.0F - (float)Math.pow((double)progress, 2.0D);
                  Vec3d screenPos = ProjectionUtil.toScreen(bubble.pos);
                  if (screenPos != null) {
                     float sizeVal = this.size.getValueFloat() * 10.0F * age;
                     alphaPct = (float)screenPos.x - sizeVal / 2.0F;
                     float y = (float)screenPos.y - sizeVal / 2.0F;
                     pSize = (float)passedMs / 5.0F * (1.0F - progress);
                     r.getTransformStack().pushTranslation((float)screenPos.x, (float)screenPos.y);
                     r.getTransformStack().pushRotation(pSize);
                     Color c = (Color)this.color.getValue();
                     rgb = (int)(life * 255.0F) << 24 | c.getRed() << 16 | c.getGreen() << 8 | c.getBlue();
                     r.drawRgbaTexture(starTexId, -sizeVal / 2.0F, -sizeVal / 2.0F, sizeVal, sizeVal, rgb);
                     r.getTransformStack().pop();
                     r.getTransformStack().pop();
                  }
               }
            }
         }
      }

      if (!this.particles.isEmpty()) {
         starTex = mc.getTextureManager().getTexture(this.starTexture);
         starTexId = -1;
         if (starTex != null) {
            starTexId = this.getGlId(starTex);
         }

         long now = System.currentTimeMillis();
         double dt = 0.016666666666666666D;
         Iterator it = this.particles.iterator();

         while(it.hasNext()) {
            HitParticles.Particle p = (HitParticles.Particle)it.next();
            age = (float)(now - p.birthTime) / 1000.0F;
            life = this.maxLife.getValueFloat();
            if (age >= life) {
               it.remove();
            } else {
               p.pos = p.pos.add(p.velocity.multiply(dt));
               String mode = (String)this.physics.getValue();
               if ("Drop".equals(mode)) {
                  p.velocity = p.velocity.add(0.0D, -9.8D * dt * 0.5D, 0.0D);
               } else if ("Fling".equals(mode)) {
                  p.velocity = p.velocity.multiply(0.95D);
               }

               Vec3d screenPos = ProjectionUtil.toScreen(p.pos);
               if (screenPos != null) {
                  alphaPct = 1.0F - age / life;
                  Color baseColor = (Color)this.color.getValue();
                  pSize = this.size.getValueFloat() * alphaPct * 2.0F;
                  if (starTexId != -1) {
                     r.getTransformStack().pushTranslation((float)screenPos.x, (float)screenPos.y);
                     float rot = (float)(now % 3600L) / 5.0F + (float)(p.hashCode() % 360);
                     r.getTransformStack().pushRotation(rot);
                     rgb = (int)(alphaPct * 255.0F) << 24 | baseColor.getRed() << 16 | baseColor.getGreen() << 8 | baseColor.getBlue();
                     r.drawRgbaTexture(starTexId, -pSize / 2.0F, -pSize / 2.0F, pSize, pSize, rgb);
                     r.getTransformStack().pop();
                     r.getTransformStack().pop();
                  } else {
                     int alpha = (int)((float)baseColor.getAlpha() * alphaPct);
                     rgb = alpha << 24 | baseColor.getRed() << 16 | baseColor.getGreen() << 8 | baseColor.getBlue();
                     r.circle((float)screenPos.x, (float)screenPos.y, pSize, 10.0F, 1.0F, rgb);
                  }
               }
            }
         }

      }
   }

   private int getGlId(AbstractTexture tex) {
      try {
         try {
            return (Integer)tex.getClass().getMethod("getGlId").invoke(tex);
         } catch (NoSuchMethodException var8) {
            Method[] var3 = tex.getClass().getMethods();
            int var4 = var3.length;

            for(int var5 = 0; var5 < var4; ++var5) {
               Method m = var3[var5];
               if (m.getParameterCount() == 0 && m.getReturnType() == Integer.TYPE) {
                  String name = m.getName();
                  if (name.equals("getGlId") || name.equals("getId") || name.contains("textureId") || name.equals("getGlRef")) {
                     return (Integer)m.invoke(tex);
                  }
               }
            }
         }
      } catch (Exception var9) {
         var9.printStackTrace();
      }

      return -1;
   }

   private static class HitBubble {
      Vec3d pos;
      long startTime;

      public HitBubble(Vec3d pos, long startTime) {
         this.pos = pos;
         this.startTime = startTime;
      }
   }

   private static class Particle {
      Vec3d pos;
      Vec3d velocity;
      long birthTime;

      public Particle(Vec3d pos, Vec3d velocity, long birthTime) {
         this.pos = pos;
         this.velocity = velocity;
         this.birthTime = birthTime;
      }
   }
}
