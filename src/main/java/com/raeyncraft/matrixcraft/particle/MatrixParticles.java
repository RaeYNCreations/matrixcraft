package com.raeyncraft.matrixcraft.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.raeyncraft.matrixcraft.MatrixCraftConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import com.raeyncraft.matrixcraft.MatrixCraftMod;
import org.joml.Quaternionf;
import org.joml.Vector3f;

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
    
    // Matrix-style shockwave particle with proper emissive rendering
    public static class BulletTrailParticle extends TextureSheetParticle {
        private final float initialSize;
        
        protected BulletTrailParticle(ClientLevel level, double x, double y, double z, 
                                     double xSpeed, double ySpeed, double zSpeed) {
            super(level, x, y, z, xSpeed, ySpeed, zSpeed);
            
            // Get config values
            this.lifetime = MatrixCraftConfig.TRAIL_LENGTH.get();
            float width = MatrixCraftConfig.TRAIL_WIDTH.get().floatValue();
            
            // Shockwave properties
            this.gravity = 0.0F;
            this.hasPhysics = false;
            this.friction = 1.0F;
            
            // No motion - particles stay in place like a shockwave ripple
            this.xd = 0;
            this.yd = 0;
            this.zd = 0;
            
            // Start small and expand
            this.initialSize = width * 2.0F;
            this.quadSize = this.initialSize;
            
            // Get color from config
            float r = MatrixCraftConfig.TRAIL_RED.get() / 255f;
            float g = MatrixCraftConfig.TRAIL_GREEN.get() / 255f;
            float b = MatrixCraftConfig.TRAIL_BLUE.get() / 255f;
            
            this.rCol = r;
            this.gCol = g;
            this.bCol = b;
            this.alpha = MatrixCraftConfig.TRAIL_ALPHA.get().floatValue();
        }
        
        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
        
        @Override
        public int getLightColor(float partialTick) {
            // Maximum light level - makes particle fully bright/emissive
            // Format: (block light << 4) | (sky light << 20)
            // 15 (max) for both = 0xF000F0
            return 0xF000F0;
        }
        
        @Override
        public void render(VertexConsumer buffer, Camera camera, float partialTick) {
            // Custom rendering with forced brightness
            super.render(buffer, camera, partialTick);
        }
        
        @Override
        public void tick() {
            // Prevent premature culling
            this.removed = false;
            
            super.tick();
            
            float lifeProgress = (float)this.age / (float)this.lifetime;
            
            // Expand outward like a shockwave ripple
            this.quadSize = this.initialSize * (1.0F + lifeProgress * 3.0F);
            
            // Fade out smoothly
            float fadeStart = 0.6F;
            if (lifeProgress > fadeStart) {
                this.alpha = MatrixCraftConfig.TRAIL_ALPHA.get().floatValue() 
                    * (1.0F - (lifeProgress - fadeStart) / (1.0F - fadeStart));
            } else {
                this.alpha = MatrixCraftConfig.TRAIL_ALPHA.get().floatValue();
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
    
    // Impact particle with bright emissive sparks
    public static class BulletImpactParticle extends TextureSheetParticle {
        protected BulletImpactParticle(ClientLevel level, double x, double y, double z, 
                                       double xSpeed, double ySpeed, double zSpeed) {
            super(level, x, y, z, xSpeed, ySpeed, zSpeed);
            this.lifetime = 15;
            this.gravity = 0.2F;
            this.hasPhysics = true;
            this.friction = 0.95F;
            
            // Use impact speed
            double speed = MatrixCraftConfig.IMPACT_PARTICLE_SPEED.get();
            this.xd = xSpeed * speed;
            this.yd = ySpeed * speed;
            this.zd = zSpeed * speed;
            
            this.quadSize = 0.15F;
            
            // Bright spark colors
            this.rCol = 1.0F;
            this.gCol = 0.9F;
            this.bCol = 0.3F;
        }
        
        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
        
        @Override
        public int getLightColor(float partialTick) {
            // Full brightness for emissive sparks
            return 0xF000F0;
        }
        
        @Override
        public void tick() {
            this.removed = false;
            
            super.tick();
            
            float lifeProgress = (float)this.age / (float)this.lifetime;
            
            // Fade and shrink over lifetime
            this.alpha = 1.0F - lifeProgress;
            this.quadSize *= 0.96F;
            
            // Fade to darker red/orange
            this.gCol *= 0.95F;
            this.bCol *= 0.90F;
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
