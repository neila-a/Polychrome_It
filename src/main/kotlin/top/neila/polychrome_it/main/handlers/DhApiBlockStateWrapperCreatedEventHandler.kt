package top.neila.polychrome_it.main.handlers

import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBlockStateWrapperCreatedEvent
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiEventParam

/**
 * Enable color override.
 */
object DhApiBlockStateWrapperCreatedEventHandler : DhApiBlockStateWrapperCreatedEvent() {
    override fun blockStateWrapperCreated(event: DhApiEventParam<EventParam>) {
        event.value.allowApiColorOverride = true
    }
}
