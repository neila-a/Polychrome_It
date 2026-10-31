package top.neila.polychrome_it.client

import net.minecraft.client.color.block.BlockTintSource
import net.minecraft.client.renderer.block.BlockAndTintGetter
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.BlockState
import top.neila.polychrome_it.PolychromeItBlock
import top.neila.polychrome_it.black
import top.neila.polychrome_it.magenta

/**
 * {@see PolychromeItClient}
 *
 * 把 image_pixel_block 的染色设为品红 (R=255,G=0,B=255)，供着色器里的世界空间 PBR 覆盖
 * 识别“这是我们的方块”。着色器用“红≈蓝、绿≈0”的比例判定，受面遮蔽/AO 缩放也不变。
 *
 * 洋红只是给着色器的“识别色”：只要启用了光影（本 mod 的派生包），着色器就会把洋红
 * 识别成“我们的方块”并做 PBR 覆盖。若未启用光影，洋红会原样显示成错误颜色，
 * 故此时回落到 PolychromeBlock.tint 的真实染色。
 */
class DerivedBlockTintSource(private val block: PolychromeItBlock) : BlockTintSource {
    override fun color(state: BlockState) = black

    override fun colorInWorld(state: BlockState, level: BlockAndTintGetter, pos: BlockPos) =
        if (IrisShaderDetector.isShaderPackInUse()) magenta else block.tint(pos)
}
