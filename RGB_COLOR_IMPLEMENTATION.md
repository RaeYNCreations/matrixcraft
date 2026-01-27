# RGB Color Support Implementation for MatrixCraft Trail Lights

## Overview
This document describes the implementation of RGB color support for MatrixCraft trail lights in the Java code.

## Changes Made

### File: `DynamicLightTextureManager.java`

#### 1. Texture Dimension Update (Line 82)
- **Before**: `new NativeImage(MAX_TRAIL_LIGHTS, 1, false)` (64x1 texture)
- **After**: `new NativeImage(MAX_TRAIL_LIGHTS, 2, false)` (64x2 texture)
- **Purpose**: Create 2-row texture to store both position data and color data

#### 2. Initialization - Clear Both Rows (Lines 83-86)
```java
for (int x = 0; x < MAX_TRAIL_LIGHTS; x++) {
    nativeImage.setPixelRGBA(x, 0, 0); // Row 0: position + intensity
    nativeImage.setPixelRGBA(x, 1, 0); // Row 1: color
}
```
- **Purpose**: Initialize both texture rows to zero

#### 3. Out-of-Range Handling (Lines 128-130)
```java
if (Math.abs(dx) > range || Math.abs(dy) > range || Math.abs(dz) > range) {
    nativeImage.setPixelRGBA(i, 0, 0); // Clear position row
    nativeImage.setPixelRGBA(i, 1, 0); // Clear color row
    continue;
}
```
- **Purpose**: Clear both rows when light is out of camera range

#### 4. Color Data Encoding (Lines 151-156)
```java
// Row 1: encode RGB color from light source
int colorR = (int) (ls.red * 255.0f) & 0xFF;
int colorG = (int) (ls.green * 255.0f) & 0xFF;
int colorB = (int) (ls.blue * 255.0f) & 0xFF;
int colorPixel = (255 << 24) | (colorR << 16) | (colorG << 8) | colorB;
nativeImage.setPixelRGBA(i, 1, colorPixel);
```
- **Purpose**: Write RGB color from `LightSource.red/green/blue` to texture row 1
- **Data Source**: `BulletTrailLighting.LightSource` fields (lines 46-56 in BulletTrailLighting.java)

#### 5. Clear Both Rows - No Active Light (Lines 160-161)
```java
} else {
    nativeImage.setPixelRGBA(i, 0, 0); // Clear position row
    nativeImage.setPixelRGBA(i, 1, 0); // Clear color row
}
```
- **Purpose**: Clear both rows when no light is active at this index

#### 6. Update clearTexture() Method (Lines 181-184)
```java
for (int x = 0; x < MAX_TRAIL_LIGHTS; x++) {
    nativeImage.setPixelRGBA(x, 0, 0); // Clear position row
    nativeImage.setPixelRGBA(x, 1, 0); // Clear color row
}
```
- **Purpose**: Clear both rows when clearing the entire texture

## Texture Layout

### Row 0 (Position + Intensity)
- **R channel**: Normalized X position relative to camera (-256 to +256 range → 0.0 to 1.0)
- **G channel**: Normalized Y position relative to camera (-256 to +256 range → 0.0 to 1.0)
- **B channel**: Normalized Z position relative to camera (-256 to +256 range → 0.0 to 1.0)
- **A channel**: Light intensity (0.0 to 1.0, based on brightness 1-15)

### Row 1 (RGB Color)
- **R channel**: Light red color (0.0 to 1.0 from LightSource.red)
- **G channel**: Light green color (0.0 to 1.0 from LightSource.green)
- **B channel**: Light blue color (0.0 to 1.0 from LightSource.blue)
- **A channel**: Full opacity (255 / 1.0) - unused

## Memory Impact
- **Before**: 64 pixels × 4 bytes = 256 bytes
- **After**: 64 × 2 pixels × 4 bytes = 512 bytes
- **Overhead**: +256 bytes (negligible)

## Integration Points

### Data Source
Color data comes from `BulletTrailLighting.LightSource`:
- `red` (float, 0.0-1.0)
- `green` (float, 0.0-1.0)
- `blue` (float, 0.0-1.0)

These values are set based on:
- Config trail color (`MatrixCraftConfig.TRAIL_COLOR_R/G/B`)
- Custom colors passed to `addLightSourceWithColor()`

### Shader Integration (Separate Repository)
The shader changes must be made in the `Spooklementary-MatrixCraft-Fixed-Test` repository:
- File: `Spooklementary-main/shaders/lib/matrix_trail_lights.glsl`
- Function: `applyMatrixTrailLights()`

The shader needs to:
1. Sample both rows: `texelFetch(matrixcraft_trail_lights, ivec2(i, 0), 0)` for position
2. Sample color row: `texelFetch(matrixcraft_trail_lights, ivec2(i, 1), 0)` for color
3. Use `colorTexel.rgb * intensity` instead of hardcoded white light

## Expected Results

✅ **Blocks will light up** - Shader now has color data to apply to blocks  
✅ **RGB color lighting works** - Green bullets create green light, red creates red, etc.  
✅ **Entities/items continue working** - They use `getLightColor()` which returns RGB values  
✅ **ACL integration preserved** - Texture-based approach still works with shaders  
✅ **Minimal performance impact** - Only 256 additional bytes of texture memory  

## Testing Notes

To test this implementation:
1. Build and run the mod with these Java changes
2. Update the shader in the Spooklementary repository with corresponding changes
3. Enable trail dynamic lighting in config
4. Set a distinctive trail color (e.g., pure red: R=255, G=0, B=0)
5. Shoot bullets near blocks and verify:
   - Blocks light up as bullets pass
   - Light color matches trail color
   - Light fades as configured
