package top.neila.polychrome_it.handlers

import com.seibel.distanthorizons.api.DhApi.Delayed.wrapperFactory
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBlockColorOverrideEvent
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiEventParam
import net.minecraft.core.BlockPos
import com.seibel.distanthorizons.api.methods.events.DhApiEventRegister.on
import com.seibel.distanthorizons.api.objects.DhApiResult
import net.minecraft.util.ARGB.*
import net.irisshaders.iris.api.v0.IrisApi.getInstance
import top.neila.polychrome_it.PolychromeItBlock

// 用于识别 image_pixel_block 的染色：品红 (R=255,G=0,B=255)。着色器用“红≈蓝、绿≈0”的比例判定。
private val magenta = opaque(0xff00ff)

object DhApiBlockColorOverrideEventHandler : DhApiBlockColorOverrideEvent() {
    override fun onBlockColorOverridden(event: DhApiEventParam<EventParam>) = event.run {
        value.blockStateWrapper.let { thisWrapper ->
            PolychromeItBlock.entries.firstOrNull {
                wrapperFactory.getDefaultBlockStateWrapper(
                    it.registeredName,
                    value.levelWrapper
                ).serialString == thisWrapper.serialString
            }?.let {
                runCatching {
                    getInstance().isShaderPackInUse
                }.getOrDefault(false).let { isShaderPackInUse ->
                    if (isShaderPackInUse) magenta else it.tint(
                        BlockPos(value.blockPosX, value.blockPosY, value.blockPosZ)
                    )
                }.let { color ->
                    // 启用了光影（派生包）时才用洋红供着色器 PBR 识别；否则用真实染色，
                    // 避免未启用光影时 DH 远景 LOD 露出大片洋红。
                    value.setColor(
                        red(color),
                        green(color),
                        blue(color)
                    )
                }
            }
        }
    } ?: Unit

    fun register() = runCatching {
        on(DhApiBlockColorOverrideEvent::class.java, this)
    }
}
