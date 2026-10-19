package top.neila.polychrome_it.client

import net.minecraft.client.color.block.BlockTintSource
import net.minecraft.client.renderer.block.BlockAndTintGetter
import net.minecraft.core.BlockPos
import net.minecraft.util.ARGB
import net.minecraft.world.level.block.state.BlockState
import top.neila.polychrome_it.main.bufferedImage
import kotlin.math.abs

val black = ARGB.opaque(0x000000)

class ImagePixelTintSource : BlockTintSource {
    override fun colorInWorld(state: BlockState, level: BlockAndTintGetter, pos: BlockPos): Int {
        if (bufferedImage == null) return black
        val x = abs(pos.x) % bufferedImage!!.width
        val height = bufferedImage!!.height
        val y = (height - abs(pos.y)) % height
        return try {
            bufferedImage!!.getRGB(x, y)
        } catch (e: ArrayIndexOutOfBoundsException) {
            black
        }
    }

    override fun color(state: BlockState) = black
}