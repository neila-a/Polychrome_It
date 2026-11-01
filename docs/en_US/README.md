# Polychrome It!

English | [大陆简体中文](/docs/zh_CN/README.md)

Make your Minecraft more colorful.  
Supports full RGB range.

## Usage

### Non-Physically Based Rendering

1. Export the heightmap and bitmap from *WorldMachine*.
1. Import the heightmap in *WorldPainter*.
    - Cover the ground with `polychrome_it:image_pixel_block`.
1. Place the bitmap in `.minecraft/config/polychrome_it/diffuse.png`.

#### Mod Support

- *Distant Horizons*: Supported.
- *Xaero's Map*: Not supported yet.
- *JourneyMap*: Supported.

### Physically Based Rendering

1. Export heightmap and material from *WorldMachine*.
1. Import the heightmap in *WorldPainter*.
    - Cover the ground with `polychrome_it:image_pixel_block`.
1. Put the material into `.minecraft/config/polychrome_it`:
    - `roughness.png`
    - `normal.png`
    - `diffuse.png`
    - Other images (like ambient occlusion) are not supported yet and not needed.
1. Install a compatible shaderpack in the game.  
   The shaderpack must be in folder format.  
   Currently supported shaderpacks:
    - *Bliss*
1. Launch the game and select the shaderpack with ` + Polychrome It!`.
1. In the shaderpack settings, make sure to enable Specular Reflections in Resource Pack Support.
1. Not required, but it's recommended to set the Sun Reflection Multiplier above 2.

#### Mod Support

- *Distant Horizons*: Can display, but support isn't great.
- *Xaero's Map*: Not supported yet.
- *JourneyMap*: Not supported yet.
