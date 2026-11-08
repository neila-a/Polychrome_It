package top.neila.polychrome_it

import net.minecraft.util.ARGB.*
import net.minecraft.core.BlockPos
import kotlin.math.abs

val black = black(0xFF)

/**
 * {@see <a href="https://zh.minecraft.wiki/w/无效纹理与模型">Minecraft Wiki</a>}
 */
private val missingno = opaque(0xf800f8)

val tintSources = mapOf<PolychromeItBlock, BlockPos.() -> Int>(
    PolychromeItBlock.image_pixel_block to {
        val x = abs(x)
        val z = abs(z)
        PolychromeItTexture.diffuse.image!!.runCatching {
            getRGB(x % width, z % height)
            /*private val grassBlockTopAverageColor = 0x91
            return color(
                red(originalColor) * 255 / grassBlockTopAverageColor,
                green(originalColor) * 255 / grassBlockTopAverageColor,
                blue(originalColor) * 255 / grassBlockTopAverageColor
            )*/
        }.getOrElse {
            (if (z and 1 == 0) x else x + 1).let { xForUse ->
                if (xForUse and 1 == 0)
                    black
                else
                    missingno
            }
        }
    },
    PolychromeItBlock.simple_gradient_block to {
        // ARGB.color 会自动and 256
        color(x, y, z)
    }
)
