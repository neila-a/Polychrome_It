package top.neila.polychrome_it.main

import com.seibel.distanthorizons.api.methods.events.DhApiEventRegister
import net.fabricmc.api.ModInitializer
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBlockStateWrapperCreatedEvent
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBlockColorOverrideEvent
import top.neila.polychrome_it.PolychromeBlock
import top.neila.polychrome_it.main.handlers.DhApiBlockColorOverrideEventHandler
import top.neila.polychrome_it.main.handlers.DhApiBlockStateWrapperCreatedEventHandler

class PolychromeIt : ModInitializer {
    override fun onInitialize() {
        // 防止懒加载
        PolychromeBlock.simple_gradient_block

        DhApiEventRegister.on(
            DhApiBlockStateWrapperCreatedEvent::class.java,
            DhApiBlockStateWrapperCreatedEventHandler
        )
        DhApiEventRegister.on(
            DhApiBlockColorOverrideEvent::class.java,
            DhApiBlockColorOverrideEventHandler
        )
    }
}
