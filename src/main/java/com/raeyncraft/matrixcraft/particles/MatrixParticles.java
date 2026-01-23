package com.raeyncraft.matrixcraft.particle;

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
        PARTICLES.register("bullet_trail", () -> new SimpleParticleType(false));
    
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BULLET_IMPACT = 
        PARTICLES.register("bullet_impact", () -> new SimpleParticleType(false));
    
    public static void register(IEventBus eventBus) {
        PARTICLES.register(eventBus);
    }
    
    // Client-side particle factory registration
    public static class BulletTrailParticle extends TextureSheetParticle {
        protected BulletTrailParticle(ClientLevel level, double x, double y, double z, 
                                     double xSpeed, double ySpeed, double zSpeed) {
            super(level, x, y, z, xSpeed, ySpeed, zSpeed);
            this.lifetime = 20;
            this.gravity = 0.0F;
            this.hasPhysics = false;
            this.xd = xSpeed * 0.01;
            this.yd = ySpeed * 0.01;
            this.zd = zSpeed * 0.01;
        }
        
        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
        
        @Override
        public void tick() {
            super.tick();
            // Fade out over lifetime
            this.alpha = 1.0F - ((float)this.age / (float)this.lifetime);
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
            this.lifetime = 10;
            this.gravity = 0.3F;
            this.hasPhysics = true;
            this.xd = xSpeed;
            this.yd = ySpeed;
            this.zd = zSpeed;
            this.quadSize = 0.1F;
        }
        
        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
        }
        
        @Override
        public void tick() {
            super.tick();
            // Fade and shrink over lifetime
            this.alpha = 1.0F - ((float)this.age / (float)this.lifetime);
            this.quadSize *= 0.95F;
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