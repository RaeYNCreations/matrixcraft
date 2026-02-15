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
            
            this.lifetime = MatrixCraftConfig.TRAIL_LENGTH.get() + 5;
            float width = MatrixCraftConfig.TRAIL_WIDTH.get().floatValue();
            
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
            float r = MatrixCraftConfig.TRAIL_COLOR_R.get() / 255f;
            float g = MatrixCraftConfig.TRAIL_COLOR_G.get() / 255f;
            float b = MatrixCraftConfig.TRAIL_COLOR_B.get() / 255f;
            
            // HDR boost - values > 1.0 create bloom effect with shaders
            float hdrBoost = 1.8f;
            this.rCol = r * hdrBoost;
            this.gCol = g * hdrBoost;
            this.bCol = b * hdrBoost;
            
            // Alpha from config
            this.initialAlpha = MatrixCraftConfig.TRAIL_ALPHA.get().floatValue();
            this.alpha = this.initialAlpha;
            
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
