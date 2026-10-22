package top.neila.polychrome_it.main.events

import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBlockStateWrapperCreatedEvent
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiEventParam

object DhApiBlockStateWrapperCreatedEventHandler : DhApiBlockStateWrapperCreatedEvent() {

    override fun blockStateWrapperCreated(event: DhApiEventParam<EventParam>) {
        val param = event.value
        param.allowApiColorOverride = true
    }
}