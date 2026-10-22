package top.neila.polychrome_it.main.events

import com.seibel.distanthorizons.api.DhApi
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBlockColorOverrideEvent
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiEventParam
import net.minecraft.util.ARGB
import top.neila.polychrome_it.PolychromeBlocks
import top.neila.polychrome_it.posToARGB

object DhApiBlockColorOverrideEventHandler : DhApiBlockColorOverrideEvent() {
    private const val IMAGE_PIXEL_BLOCK_NAMESPACE =
        PolychromeBlocks.MOD_ID + ":" + PolychromeBlocks.IMAGE_PIXEL_BLOCK_ID

    override fun onBlockColorOverridden(event: DhApiEventParam<EventParam>) {
        val param = event.value

        val rightWrapper = DhApi.Delayed.wrapperFactory.getDefaultBlockStateWrapper(
            IMAGE_PIXEL_BLOCK_NAMESPACE,
            param.levelWrapper
        )
        val thisWrapper = param.blockStateWrapper
        if (rightWrapper.serialString == thisWrapper.serialString) {
            val color = posToARGB(param.blockPosX, param.blockPosZ)
            param.setColor(
                ARGB.red(color),
                ARGB.green(color),
                ARGB.blue(color)
            )
        }
    }
}
