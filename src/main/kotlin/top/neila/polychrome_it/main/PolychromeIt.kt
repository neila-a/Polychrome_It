package top.neila.polychrome_it.main

import com.seibel.distanthorizons.api.methods.events.DhApiEventRegister
import net.fabricmc.api.ModInitializer
import top.neila.polychrome_it.PolychromeBlocks
import top.neila.polychrome_it.main.events.*
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBlockStateWrapperCreatedEvent
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBlockColorOverrideEvent

class PolychromeIt : ModInitializer {
    override fun onInitialize() {
        PolychromeBlocks.init()

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
