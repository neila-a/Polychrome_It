package top.neila.polychrome_it

import net.minecraft.client.color.block.BlockTintSource
import net.minecraft.client.renderer.block.BlockAndTintGetter
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.BlockState

/**
 * {@see PolychromeItClient}
 *
 * 始终返回 image_pixel_block 的真实染色（来自世界图片的像素颜色）。
 *
 * 不再用洋红作为“识别色”：启用光影时，着色器通过 mc_Entity 携带的方块数字 ID
 * 来识别本 mod 方块（见 all_solid.vsh 注入与 polychrome_it.glsl），
 * 因此小地图（只读 RGB）自然显示真实色，不会再出现洋红。
 */
class DerivedBlockTintSource(private val block: PolychromeItBlock) : BlockTintSource {
    override fun color(state: BlockState) = black

    override fun colorInWorld(state: BlockState, level: BlockAndTintGetter, pos: BlockPos) =
        tintSources[block]!!(pos)
}
