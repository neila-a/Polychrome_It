package top.neila.polychrome_it.main.handlers

import com.seibel.distanthorizons.api.DhApi
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBlockColorOverrideEvent
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiEventParam
import net.minecraft.core.BlockPos
import net.minecraft.util.ARGB
import top.neila.polychrome_it.PolychromeItBlock

object DhApiBlockColorOverrideEventHandler : DhApiBlockColorOverrideEvent() {
    override fun onBlockColorOverridden(event: DhApiEventParam<EventParam>) = with(event) {
        val thisWrapper = value.blockStateWrapper
        for (block in PolychromeItBlock.entries) {
            val rightWrapper = DhApi.Delayed.wrapperFactory.getDefaultBlockStateWrapper(
                block.registeredName,
                value.levelWrapper
            )
            if (rightWrapper.serialString == thisWrapper.serialString) {
                val color = block.tint(
                    BlockPos(
                        value.blockPosX,
                        value.blockPosY,
                        value.blockPosZ
                    )
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
