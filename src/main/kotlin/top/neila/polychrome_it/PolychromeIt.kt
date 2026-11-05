package top.neila.polychrome_it

import com.seibel.distanthorizons.api.methods.events.DhApiEventRegister
import net.fabricmc.api.ModInitializer
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBlockStateWrapperCreatedEvent
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBlockColorOverrideEvent
import top.neila.polychrome_it.handlers.DhApiBlockColorOverrideEventHandler
import top.neila.polychrome_it.handlers.DhApiBlockStateWrapperCreatedEventHandler
import java.lang.NoClassDefFoundError

object PolychromeIt : ModInitializer {
    override fun onInitialize() {
        // 防止懒加载
        PolychromeItBlock.simple_gradient_block.registered

        try {
            DhApiEventRegister.on(
                DhApiBlockStateWrapperCreatedEvent::class.java,
                DhApiBlockStateWrapperCreatedEventHandler
            )
            DhApiEventRegister.on(
                DhApiBlockColorOverrideEvent::class.java,
                DhApiBlockColorOverrideEventHandler
            )
        } catch (_: NoClassDefFoundError) {
            // Distant Horizons didn't provide.
            // It doesn't matter.
        }

        PolychromeItTexture.diffuse.watch()
    }
}
