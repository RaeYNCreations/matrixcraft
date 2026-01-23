package com.raeyncraft.matrixcraft.particle;

import com.raeyncraft.matrixcraft.MatrixCraftConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import com.raeyncraft.matrixcraft.MatrixCraftMod;

public class MatrixParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = 
        DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, MatrixCraftMod.MODID);
    
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BULLET_TRAIL = 
        PARTICLES.register("bullet_trail", () -> new SimpleParticleType(true));
    
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BULLET_IMPACT = 
        PARTICLES.register("bullet_impact", () -> new SimpleParticleType(true));
    
    public static void register(IEventBus eventBus) {
        PARTICLES.register(eventBus);
    }
    
    public static class BulletTrailParticle extends TextureSheetParticle {
        private final float initialSize;
        
        protected BulletTrailParticle(ClientLevel level, double x, double y, double z, 
                                     double xSpeed, double ySpeed, double zSpeed) {
            super(level, x, y, z, xSpeed, ySpeed, zSpeed);
            
            this.lifetime = MatrixCraftConfig.TRAIL_LENGTH.get();
            float width = MatrixCraftConfig.TRAIL_WIDTH.get().floatValue();
            
            this.gravity = 0.0F;
            this.hasPhysics = false;
            this.friction = 1.0F;
            
            this.xd = 0;
            this.yd = 0;
            this.zd = 0;
            
            this.initialSize = width * 3.0F;
            this.quadSize = this.initialSize;
            
            // GREEN color - bright enough for shader detection but not white
            // Total brightness = 0.8 + 1.5 + 0.8 = 3.1 (triggers shader at >2.5)
            // But green is dominant so it looks green not white
            this.rCol = 0.6f;  // Some red for cyan tint
            this.gCol = 1.9f;  // DOMINANT green
            this.bCol = 0.6f;  // Some blue for cyan tint
            this.alpha = 0.2f; // Slightly transparent
        }
        
        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
        
        @Override
        public int getLightColor(float partialTick) {
            return 0xF000F0;
        }
        
        @Override
        public void tick() {
            this.removed = false;
            super.tick();
            
            float lifeProgress = (float)this.age / (float)this.lifetime;
            this.quadSize = this.initialSize * (1.0F + lifeProgress * 3.0F);
            
            float fadeStart = 0.6F;
            if (lifeProgress > fadeStart) {
                this.alpha = 0.9f * (1.0F - (lifeProgress - fadeStart) / (1.0F - fadeStart));
            }
        }
        
        public static class Provider implements ParticleProvider<SimpleParticleType> {
            private final SpriteSet sprites;
            
            public Provider(SpriteSet sprites) {
                this.sprites = sprites;
            }
            
            @Override
            public Particle createParticle(SimpleParticleType type, ClientLevel level, 
                                          double x, double y, double z, 
                                          double xSpeed, double ySpeed, double zSpeed) {
                BulletTrailParticle particle = new BulletTrailParticle(level, x, y, z, xSpeed, ySpeed, zSpeed);
                particle.pickSprite(this.sprites);
                return particle;
            }
        }
    }
    
    public static class BulletImpactParticle extends TextureSheetParticle {
        protected BulletImpactParticle(ClientLevel level, double x, double y, double z, 
                                       double xSpeed, double ySpeed, double zSpeed) {
            super(level, x, y, z, xSpeed, ySpeed, zSpeed);
            this.lifetime = 15;
            this.gravity = 0.2F;
            this.hasPhysics = true;
            this.friction = 0.95F;
            
            double speed = MatrixCraftConfig.IMPACT_PARTICLE_SPEED.get();
            this.xd = xSpeed * speed;
            this.yd = ySpeed * speed;
            this.zd = zSpeed * speed;
            
            this.quadSize = 0.25F;
            
            // YELLOW-WHITE impact sparks
            // Total = 1.2 + 1.2 + 0.6 = 3.0 (triggers shader)
            this.rCol = 1.2F;  // Red
            this.gCol = 1.2F;  // Yellow = red + green
            this.bCol = 0.6F;  // Less blue = warmer yellow
        }
        
        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
        
        @Override
        public int getLightColor(float partialTick) {
            return 0xF000F0;
        }
        
        @Override
        public void tick() {
            this.removed = false;
            super.tick();
            
            float lifeProgress = (float)this.age / (float)this.lifetime;
            this.alpha = 1.0F - lifeProgress;
            this.quadSize *= 0.96F;
            
            // Fade to orange-red
            this.gCol *= 0.93F;
            this.bCol *= 0.85F;
        }
        
        public static class Provider implements ParticleProvider<SimpleParticleType> {
            private final SpriteSet sprites;
            
            public Provider(SpriteSet sprites) {
                this.sprites = sprites;
            }
            
            @Override
            public Particle createParticle(SimpleParticleType type, ClientLevel level, 
                                          double x, double y, double z, 
                                          double xSpeed, double ySpeed, double zSpeed) {
                BulletImpactParticle particle = new BulletImpactParticle(level, x, y, z, xSpeed, ySpeed, zSpeed);
                particle.pickSprite(this.sprites);
                return particle;
            }
        }
    }
}
