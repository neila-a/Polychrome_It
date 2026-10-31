package top.neila.polychrome_it

import java.awt.image.BufferedImage
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import javax.imageio.ImageIO
import kotlin.io.path.name

private const val imageFilename = "${MOD_ID}_image"

@JvmName("getImageForInitialValueAndReset")
private fun getImage() =
    try {
        ImageIO.read(PolychromeItConfig.diffuseFile.toFile())
    } catch (e: Exception) {
        null
    }

var image: BufferedImage? = getImage()

fun watchImage() =
    FileSystems.getDefault().newWatchService()?.run {
        Thread {
            var key = PolychromeItConfig.dir.parent.register(
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
                            image = getImage()
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
