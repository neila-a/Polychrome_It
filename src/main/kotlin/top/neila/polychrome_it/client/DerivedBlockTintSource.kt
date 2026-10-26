package top.neila.polychrome_it.client

import net.minecraft.client.color.block.BlockTintSource
import net.minecraft.client.renderer.block.BlockAndTintGetter
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.BlockState
import top.neila.polychrome_it.PolychromeItBlock
import top.neila.polychrome_it.black

/**
 * {@see PolychromeItClient}
 */
class DerivedBlockTintSource(private val block: PolychromeItBlock) : BlockTintSource {
    override fun color(state: BlockState) = black
    override fun colorInWorld(state: BlockState, level: BlockAndTintGetter, pos: BlockPos) =
        block.tint(pos)
}
