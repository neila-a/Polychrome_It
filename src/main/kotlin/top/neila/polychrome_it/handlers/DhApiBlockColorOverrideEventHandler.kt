package top.neila.polychrome_it.handlers

import com.seibel.distanthorizons.api.DhApi
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBlockColorOverrideEvent
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiEventParam
import net.minecraft.core.BlockPos
import net.minecraft.util.ARGB
import top.neila.polychrome_it.PolychromeItBlock

// 用于识别 image_pixel_block 的染色：品红 (R=255,G=0,B=255)。着色器用“红≈蓝、绿≈0”的比例判定。
private val magenta = ARGB.opaque(0xff00ff)

object DhApiBlockColorOverrideEventHandler : DhApiBlockColorOverrideEvent() {
    override fun onBlockColorOverridden(event: DhApiEventParam<EventParam>) = with(event) {
        val thisWrapper = value.blockStateWrapper
        for (block in PolychromeItBlock.entries) {
            val rightWrapper = DhApi.Delayed.wrapperFactory.getDefaultBlockStateWrapper(
                block.registeredName,
                value.levelWrapper
            )
            if (rightWrapper.serialString == thisWrapper.serialString) {
                // 启用了光影（派生包）时才用洋红供着色器 PBR 识别；否则用真实染色，
                // 避免未启用光影时 DH 远景 LOD 露出大片洋红。
                val color = if (isShaderPackInUse()) magenta else block.tint(
                    BlockPos(value.blockPosX, value.blockPosY, value.blockPosZ)
                )
                value.setColor(
                    ARGB.red(color),
                    ARGB.green(color),
                    ARGB.blue(color)
                )
                break
            }
        }
    }
}
