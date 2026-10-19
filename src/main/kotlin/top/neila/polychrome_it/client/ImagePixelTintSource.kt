package top.neila.polychrome_it.client

import net.minecraft.client.color.block.BlockTintSource
import net.minecraft.client.renderer.block.BlockAndTintGetter
import net.minecraft.core.BlockPos
import net.minecraft.util.ARGB
import net.minecraft.world.level.block.state.BlockState
import java.awt.image.BufferedImage
import kotlin.math.abs

val black = ARGB.opaque(0x000000)

class ImagePixelTintSource : BlockTintSource {
    override fun colorInWorld(state: BlockState, level: BlockAndTintGetter, pos: BlockPos): Int {
        val image: BufferedImage = ImagePathClientStore.cachedImage ?: return black
        val x = abs(pos.x) % image.width
        val y = abs(pos.z) % image.height
        return try {
            image.getRGB(x, y)
        } catch (e: ArrayIndexOutOfBoundsException) {
            black
        }
    }

    override fun color(state: BlockState) = black
}