package top.neila.polychrome_it.main.handlers

import com.seibel.distanthorizons.api.DhApi
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBlockColorOverrideEvent
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiEventParam
import net.minecraft.core.BlockPos
import net.minecraft.util.ARGB
import top.neila.polychrome_it.PolychromeItBlock

object DhApiBlockColorOverrideEventHandler : DhApiBlockColorOverrideEvent() {
    private val IMAGE_PIXEL_BLOCK_NAMESPACE = PolychromeItBlock.image_pixel_block.registeredName

    override fun onBlockColorOverridden(event: DhApiEventParam<EventParam>) {
        val param = event.value

        val rightWrapper = DhApi.Delayed.wrapperFactory.getDefaultBlockStateWrapper(
            IMAGE_PIXEL_BLOCK_NAMESPACE,
            param.levelWrapper
        )
        val thisWrapper = param.blockStateWrapper
        if (rightWrapper.serialString == thisWrapper.serialString) {
            val color = PolychromeItBlock.image_pixel_block.tint(BlockPos(
                param.blockPosX,
                param.blockPosY,
                param.blockPosZ
            ))
            param.setColor(
                ARGB.red(color),
                ARGB.green(color),
                ARGB.blue(color)
            )
        }
    }
}
