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
    
    public static void register(IEventBus eventBus) {
        PARTICLES.register(eventBus);
    }
    
    /**
     * Bullet Trail Particle - reads color, size, alpha from config
     */
    public static class BulletTrailParticle extends TextureSheetParticle {
        private final float initialAlpha;
        private final float initialSize;
        
        protected BulletTrailParticle(ClientLevel level, double x, double y, double z, 
                                     double xSpeed, double ySpeed, double zSpeed) {
            super(level, x, y, z, xSpeed, ySpeed, zSpeed);
            
            // Read from config with safe defaults
            int lifetime = getConfigInt(MatrixCraftConfig.TRAIL_LENGTH, 20);
            float width = getConfigFloat(MatrixCraftConfig.TRAIL_WIDTH, 0.05f);
            float configAlpha = getConfigFloat(MatrixCraftConfig.TRAIL_ALPHA, 0.8f);
            
            // Color from config (0-255 -> 0.0-2.0 for HDR glow effect)
            float r = getConfigInt(MatrixCraftConfig.TRAIL_COLOR_R, 0) / 127.5f;
            float g = getConfigInt(MatrixCraftConfig.TRAIL_COLOR_G, 255) / 127.5f;
            float b = getConfigInt(MatrixCraftConfig.TRAIL_COLOR_B, 0) / 127.5f;
            
            this.lifetime = lifetime + 5;
            this.gravity = 0.0F;
            this.hasPhysics = false;
            this.friction = 1.0F;
            
            // No movement - trail stays in place
            this.xd = 0;
            this.yd = 0;
            this.zd = 0;
            
            // Size - reduced by half (was 3.0F, now 1.5F base multiplier)
            this.initialSize = width * 1.5F;
            this.quadSize = this.initialSize;
            
            // Apply colors from config
            this.rCol = r;
            this.gCol = g;
            this.bCol = b;
            
            this.initialAlpha = configAlpha;
            this.alpha = this.initialAlpha;
        }
        
        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
        
        @Override
        public int getLightColor(float partialTick) {
            // Full brightness - makes particle glow
            return 0xF000F0;
        }
        
        @Override
        public void tick() {
            super.tick();
            
            float lifeProgress = (float) this.age / (float) this.lifetime;
            
            // Grow slightly over time
            this.quadSize = this.initialSize * (1.0F + lifeProgress * 1.5F);
            
            // Fade out in the last 40% of life
            float fadeStart = 0.6F;
            if (lifeProgress > fadeStart) {
                float fadeProgress = (lifeProgress - fadeStart) / (1.0F - fadeStart);
                this.alpha = this.initialAlpha * (1.0F - fadeProgress);
            }
        }
        
        // Safe config getters with defaults
        private static int getConfigInt(Object configValue, int defaultVal) {
            try {
                if (configValue instanceof net.neoforged.neoforge.common.ModConfigSpec.IntValue intVal) {
                    return intVal.get();
                }
            } catch (Exception e) {
                // Config not loaded yet
            }
            return defaultVal;
        }
        
        private static float getConfigFloat(Object configValue, float defaultVal) {
            try {
                if (configValue instanceof net.neoforged.neoforge.common.ModConfigSpec.DoubleValue doubleVal) {
                    return doubleVal.get().floatValue();
                }
            } catch (Exception e) {
                // Config not loaded yet
            }
            return defaultVal;
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
