package top.neila.polychrome_it

import net.minecraft.client.color.block.BlockTintSource
import net.minecraft.client.renderer.block.BlockAndTintGetter
import net.minecraft.core.BlockPos
import net.minecraft.util.ARGB
import net.minecraft.world.level.block.state.BlockState
import kotlin.math.abs

/**
 * 将数字缩进RGB范围
 * 原理：and 255 == % 256
 */
fun numberToRGBScale(number: Int) = abs(number) and 255

class PolychromeBlockTintSource : BlockTintSource {
    override fun colorInWorld(state: BlockState, level: BlockAndTintGetter, pos: BlockPos): Int {
        val color = ARGB.color(numberToRGBScale(pos.x), numberToRGBScale(pos.y), numberToRGBScale(pos.z))
        return color
    }

    override fun color(state: BlockState) = ARGB.opaque(0x000000)
}