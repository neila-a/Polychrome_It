package top.neila.polychrome_it.client.tintsources

import net.minecraft.client.color.block.BlockTintSource
import net.minecraft.client.renderer.block.BlockAndTintGetter
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.BlockState
import top.neila.polychrome_it.black
import top.neila.polychrome_it.posToARGB

object ImagePixelTintSource : BlockTintSource {
    override fun colorInWorld(state: BlockState, level: BlockAndTintGetter, pos: BlockPos) =
        posToARGB(pos.x, pos.z)

    override fun color(state: BlockState) = black
}