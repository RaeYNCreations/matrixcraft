package com.raeyncraft.matrixcraft.particle;

import com.raeyncraft.matrixcraft.MatrixCraftConfig;
import com.raeyncraft.matrixcraft.MatrixCraftMod;
import com.raeyncraft.matrixcraft.client.BulletTrailLighting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

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
    
    /**
     * Bullet Trail Particle - Configurable color and dynamic lighting support
     * 
     * Features:
     * - Color from config (TRAIL_COLOR_R/G/B)
     * - Full brightness for shader glow effects
     * - Registers with BulletTrailLighting for dynamic light mods
     * - Smooth fade out animation
     */
    public static class BulletTrailParticle extends TextureSheetParticle {
        private final float initialAlpha;
        private final float initialSize;
        private final boolean emitsLight;
        
        protected BulletTrailParticle(ClientLevel level, double x, double y, double z, 
                                     double xSpeed, double ySpeed, double zSpeed) {
            super(level, x, y, z, xSpeed, ySpeed, zSpeed);
            
            // Safe config access with defaults
            try {
                Integer lifetimeConfig = MatrixCraftConfig.TRAIL_LENGTH.get();
                this.lifetime = (lifetimeConfig != null ? lifetimeConfig : 20) + 5;
            } catch (Exception e) {
                this.lifetime = 25; // Default: 20 + 5
            }
            
            float width;
            try {
                Number widthConfig = MatrixCraftConfig.TRAIL_WIDTH.get();
                width = widthConfig != null ? widthConfig.floatValue() : 0.5f;
            } catch (Exception e) {
                width = 0.5f; // Default
            }
            
            this.gravity = 0.0F;
            this.hasPhysics = false;
            this.friction = 1.0F;
            
            // No movement - trail stays in place
            this.xd = 0;
            this.yd = 0;
            this.zd = 0;
            
            this.initialSize = width * 3.0F;
            this.quadSize = this.initialSize;
            
            // Get color from config - apply HDR boost for glow
            // Clamp values to valid range [0-255] before normalization
            int r, g, b;
            try {
                r = Math.max(0, Math.min(255, MatrixCraftConfig.TRAIL_COLOR_R.get()));
                g = Math.max(0, Math.min(255, MatrixCraftConfig.TRAIL_COLOR_G.get()));
                b = Math.max(0, Math.min(255, MatrixCraftConfig.TRAIL_COLOR_B.get()));
            } catch (Exception e) {
                // Default: green
                r = 0;
                g = 255;
                b = 0;
            }
            
            float rNorm = r / 255f;
            float gNorm = g / 255f;
            float bNorm = b / 255f;
            
            // HDR boost - values > 1.0 create bloom effect with shaders
            float hdrBoost = 1.8f;
            this.rCol = rNorm * hdrBoost;
            this.gCol = gNorm * hdrBoost;
            this.bCol = bNorm * hdrBoost;
            
            // Alpha from config
            float alphaValue = 1.0f; // Default: fully opaque
            try {
                Number alphaConfig = MatrixCraftConfig.TRAIL_ALPHA.get();
                alphaValue = alphaConfig != null ? alphaConfig.floatValue() : 1.0f;
            } catch (Exception e) {
                // Keep default value
            }
            this.initialAlpha = alphaValue;
            this.alpha = this.initialAlpha;;
            
            // Check if this particle should emit light
            this.emitsLight = BulletTrailLighting.isDynamicLightingEnabled();
        }
        
        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
        
        @Override
        public int getLightColor(float partialTick) {
            // Full brightness - makes particle visible in darkness and enables shader bloom
            // Format: upper 4 bits = sky light, lower 4 bits = block light
            // 0xF000F0 = 15 sky light, 15 block light
            return 0xF000F0;
        }
        
        @Override
        public void tick() {
            super.tick();
            
            float lifeProgress = (float) this.age / (float) this.lifetime;
            
            // Grow slightly over time
            this.quadSize = this.initialSize * (1.0F + lifeProgress * 2.0F);
            
            // Fade out in the last 40% of life
            float fadeStart = 0.6F;
            if (lifeProgress > fadeStart) {
                float fadeProgress = (lifeProgress - fadeStart) / (1.0F - fadeStart);
                this.alpha = this.initialAlpha * (1.0F - fadeProgress);
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
}
