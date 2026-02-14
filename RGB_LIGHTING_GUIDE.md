# MatrixCraft RGB Dynamic Lighting Guide

## Overview

MatrixCraft features a fully RGB-configurable dynamic lighting system for bullets and hit entities. All lighting automatically matches your bullet trail color configuration.

## Configuration

### Location
RGB colors are configured in your client config file:
- Path: `.minecraft/config/matrixcraft-client.toml`

### RGB Color Settings

```toml
[trails]
    # Trail color - Red (0-255)
    colorR = 0
    
    # Trail color - Green (0-255)
    colorG = 255
    
    # Trail color - Blue (0-255)
    colorB = 0
```

## What Uses These Colors?

The RGB configuration affects **THREE** lighting systems:

1. **Bullet Trail Particles** - Visual particle trails
2. **Bullet Dynamic Lights** - Dynamic lighting on the bullets themselves
3. **Hit Entity Lights** - Entities glow when hit by bullets (1 second duration)

All three systems use the **same RGB values** for perfect visual consistency.

## Color Presets

### Matrix Green (Default)
```toml
colorR = 0
colorG = 255
colorB = 0
```
Classic Matrix movie green color.

### Blood Red
```toml
colorR = 255
colorG = 0
colorB = 0
```
Intense red for aggressive combat visuals.

### Ice Blue
```toml
colorR = 0
colorG = 128
colorB = 255
```
Cool blue with slight cyan tint.

### Electric Purple
```toml
colorR = 128
colorG = 0
colorB = 255
```
Vibrant purple energy effect.

### Flame Orange
```toml
colorR = 255
colorG = 128
colorB = 0
```
Fiery orange glow.

### Cyber Cyan
```toml
colorR = 0
colorG = 255
colorB = 255
```
Bright cyan/aqua cyberpunk style.

### Hot Pink
```toml
colorR = 255
colorG = 0
colorB = 128
```
Vibrant pink for style points.

### Toxic Yellow
```toml
colorR = 255
colorG = 255
colorB = 0
```
Bright yellow for high visibility.

### White/Pure Light
```toml
colorR = 255
colorG = 255
colorB = 255
```
Pure white light for maximum brightness.

## Additional Lighting Settings

### Light Level
```toml
# Brightness of dynamic lights (1-15, Minecraft light levels)
lightLevel = 12
```
Controls how bright the lights are. 15 is brightest, 1 is dimmest.

### Light Duration
```toml
# How long trail lights last (in ticks, 20 ticks = 1 second)
lightDurationTicks = 40
```
Controls how long light sources persist after bullets pass.

### Chain Lighting
```toml
# Enable chain of lights trailing bullets
chainEnabled = false

# Number of lights in the chain (1-8)
chainCount = 4

# Spacing between chain lights in blocks
chainSpacing = 0.5
```
Creates a trail of multiple lights behind each bullet.

## LambDynLights Integration

The RGB system requires **LambDynLights** mod to display dynamic lighting in-game:
- Download: https://modrinth.com/mod/lambdynamiclights
- Version: For Minecraft 1.21.1

### Without LambDynLights
- Bullet trail **particles** still show with RGB color
- Dynamic world lighting will **not** appear
- Entities won't glow when hit

### With LambDynLights
- ✅ RGB colored particles
- ✅ RGB dynamic world lighting from bullets
- ✅ RGB glow on entities when hit
- ✅ Colored light affects surrounding blocks

## Troubleshooting

### Lights Not Appearing
1. Check if `TRAIL_DYNAMIC_LIGHTING` is enabled in config
2. Verify LambDynLights is installed
3. Ensure `TRAIL_GLOW` is set to `true`
4. Check light level is not set to 0

### Wrong Colors
1. Verify RGB values are in 0-255 range
2. Check the config file was saved properly
3. Restart Minecraft after changing colors

### Performance Issues
1. Reduce `chainCount` if using chain lighting
2. Increase `TRAIL_LIGHT_SPACING` to reduce light sources
3. Reduce `lightDurationTicks` for faster cleanup
4. Consider disabling chain lighting if FPS drops

## Technical Details

### Color Normalization
- Config stores RGB as integers (0-255)
- Internally normalized to floats (0.0-1.0) for rendering
- Automatically converted for LambDynLights API

### Memory Management
- Maximum 1000 position-based lights
- Maximum 500 entity-attached lights
- Automatic cleanup when limits exceeded
- 3-second timeout for inactive entity lights

### Thread Safety
- Uses ConcurrentHashMap for thread-safe access
- Snapshot-based iteration prevents race conditions
- Safe for concurrent rendering and game ticks

## Examples in Action

### Combat Scenario
```toml
colorR = 255  # Red
colorG = 0
colorB = 0
lightLevel = 15
chainEnabled = true
chainCount = 6
```
Creates intense red bullet trails with bright chain lighting for dramatic combat visuals.

### Stealth Scenario
```toml
colorR = 0
colorG = 64
colorB = 128
lightLevel = 8
chainEnabled = false
```
Dim blue lights for tactical/stealth gameplay.

### Rainbow Mode
Change colors periodically for variety - the system updates instantly when config is reloaded!

## Credits

RGB Dynamic Lighting System developed for MatrixCraft mod.
- Supports both bullet trails and hit entity lighting
- Full LambDynLights integration
- Configurable and performance-optimized
