package top.neila.polychrome_it.main

import com.seibel.distanthorizons.api.methods.events.DhApiEventRegister
import net.fabricmc.api.ModInitializer
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBlockStateWrapperCreatedEvent
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBlockColorOverrideEvent
import top.neila.polychrome_it.PolychromeItBlock
import top.neila.polychrome_it.getImage
import top.neila.polychrome_it.imageFilename
import top.neila.polychrome_it.imagePath
import top.neila.polychrome_it.main.handlers.DhApiBlockColorOverrideEventHandler
import top.neila.polychrome_it.main.handlers.DhApiBlockStateWrapperCreatedEventHandler
import top.neila.polychrome_it.readImage
import java.nio.file.ClosedWatchServiceException
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchService
import kotlin.io.path.name

class PolychromeIt : ModInitializer {
    private val watcherService = FileSystems.getDefault().newWatchService()

    override fun onInitialize() {
        // 防止懒加载
        PolychromeItBlock.simple_gradient_block.registered

        DhApiEventRegister.on(
            DhApiBlockStateWrapperCreatedEvent::class.java,
            DhApiBlockStateWrapperCreatedEventHandler
        )
        DhApiEventRegister.on(
            DhApiBlockColorOverrideEvent::class.java,
            DhApiBlockColorOverrideEventHandler
        )

        watcherService?.run {
            Thread {
                var key = imagePath.parent.register(
                    this,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY
                )
                runCatching {
                    while (true) {
                        key = take()
                        for (event in key.pollEvents()) {
                            val path = event.context()
                            if (path is Path && path.fileName.name == imageFilename)
                                readImage = getImage()
                        }
                        if (!key.reset()) break
                    }
                }
            }.apply {
                isDaemon = true
                name = "PolychromeIt image watcher"
                start()
            }

            Runtime.getRuntime().addShutdownHook(
                Thread { runCatching { close() } }
            )
        }
    }
}
