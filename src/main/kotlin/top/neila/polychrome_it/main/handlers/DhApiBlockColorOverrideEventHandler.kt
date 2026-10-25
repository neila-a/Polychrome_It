package top.neila.polychrome_it.main.handlers

import com.seibel.distanthorizons.api.DhApi
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBlockColorOverrideEvent
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiEventParam
import net.minecraft.core.BlockPos
import net.minecraft.util.ARGB
import top.neila.polychrome_it.PolychromeItBlock

object DhApiBlockColorOverrideEventHandler : DhApiBlockColorOverrideEvent() {
    private val IMAGE_PIXEL_BLOCK_NAMESPACE = PolychromeItBlock.image_pixel_block.registeredName

    override fun onBlockColorOverridden(event: DhApiEventParam<EventParam>) = with(event) {
        val rightWrapper = DhApi.Delayed.wrapperFactory.getDefaultBlockStateWrapper(
            IMAGE_PIXEL_BLOCK_NAMESPACE,
            value.levelWrapper
        )
        val thisWrapper = value.blockStateWrapper
        if (rightWrapper.serialString == thisWrapper.serialString) {
            val color = PolychromeItBlock.image_pixel_block.tint(
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
        }
    }
}
