package top.neila.polychrome_it.client

import net.minecraft.client.color.block.BlockTintSource
import net.minecraft.client.renderer.block.BlockAndTintGetter
import net.minecraft.core.BlockPos
import net.minecraft.util.ARGB
import net.minecraft.world.level.block.state.BlockState
import top.neila.polychrome_it.ImagePathClientStore
import java.awt.image.BufferedImage
import kotlin.math.abs

val black = ARGB.opaque(0x000000)
const val grassBlockTopAverageColor = 0x91

class ImagePixelTintSource : BlockTintSource {
    override fun colorInWorld(state: BlockState, level: BlockAndTintGetter, pos: BlockPos): Int {
        val image: BufferedImage = ImagePathClientStore.cachedImage ?: return black
        val x = abs(pos.x) % image.width
        val y = abs(pos.z) % image.height
        try {
            val originalColor = image.getRGB(x, y)
            return originalColor
            return ARGB.color(
                ARGB.red(originalColor) * 255 / grassBlockTopAverageColor,
                ARGB.green(originalColor) * 255 / grassBlockTopAverageColor,
                ARGB.blue(originalColor) * 255 / grassBlockTopAverageColor
            )
        } catch (e: ArrayIndexOutOfBoundsException) {
            return black
        }
    }

    override fun color(state: BlockState) = black
}