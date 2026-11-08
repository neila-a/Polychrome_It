package top.neila.polychrome_it.handlers

import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBlockStateWrapperCreatedEvent
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiEventParam
import com.seibel.distanthorizons.api.objects.DhApiResult
import com.seibel.distanthorizons.api.methods.events.DhApiEventRegister.on

/**
 * Enable color override.
 */
object DhApiBlockStateWrapperCreatedEventHandler : DhApiBlockStateWrapperCreatedEvent() {
    override fun blockStateWrapperCreated(event: DhApiEventParam<EventParam>) {
        event.value.allowApiColorOverride = true
    }

    fun register() = runCatching {
        on(DhApiBlockStateWrapperCreatedEvent::class.java, this)
    }
}
