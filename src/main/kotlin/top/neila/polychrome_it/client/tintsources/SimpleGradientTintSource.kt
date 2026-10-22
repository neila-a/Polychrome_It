package top.neila.polychrome_it.client.tintsources

import net.minecraft.client.color.block.BlockTintSource
import net.minecraft.client.renderer.block.BlockAndTintGetter
import net.minecraft.core.BlockPos
import net.minecraft.util.ARGB
import net.minecraft.world.level.block.state.BlockState
import top.neila.polychrome_it.black
import kotlin.math.abs

object SimpleGradientTintSource : BlockTintSource {
    /**
     * 将数字缩进RGB范围
     * 原理：and 255 == % 256
     */
    private fun numberToRGBScale(number: Int) = abs(number) and 255

    override fun colorInWorld(state: BlockState, level: BlockAndTintGetter, pos: BlockPos) =
        ARGB.color(numberToRGBScale(pos.x), numberToRGBScale(pos.y), numberToRGBScale(pos.z))

    override fun color(state: BlockState) = black
}