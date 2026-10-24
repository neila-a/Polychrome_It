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
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent
import javax.imageio.ImageIO
import kotlin.io.path.name

class PolychromeIt : ModInitializer {
    override fun onInitialize() {
        // 防止懒加载
        PolychromeItBlock.simple_gradient_block

        DhApiEventRegister.on(
            DhApiBlockStateWrapperCreatedEvent::class.java,
            DhApiBlockStateWrapperCreatedEventHandler
        )
        DhApiEventRegister.on(
            DhApiBlockColorOverrideEvent::class.java,
            DhApiBlockColorOverrideEventHandler
        )

        Thread {
            val watcher = FileSystems.getDefault().newWatchService() ?: return@Thread
            var key = imagePath.parent.register(
                watcher,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE,
                StandardWatchEventKinds.ENTRY_MODIFY
            )
            while (true) {
                key = watcher.take()
                for (event in key.pollEvents()) {
                    val path = event.context()
                    if (path is Path && path.fileName.name == imageFilename) {
                        readImage = getImage()
                    }
                }
                if (!key.reset()) return@Thread
            }
        }.start()
    }
}
