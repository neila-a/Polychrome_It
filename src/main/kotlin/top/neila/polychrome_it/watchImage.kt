package top.neila.polychrome_it

import java.awt.image.BufferedImage
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import javax.imageio.ImageIO
import kotlin.io.path.name
import net.fabricmc.loader.api.FabricLoader
import java.io.File
import kotlin.io.path.exists

val dir: Path = FabricLoader.getInstance().configDir.resolve("polychrome_it")
val diffuseFile: Path = dir.resolve("diffuse.png")
val normalFile: Path = dir.resolve("normal.png")
val roughnessFile: Path = dir.resolve("roughness.png")
val heightFile: Path = dir.resolve("height.png")

/** 从 diffuse.png 读取像素尺寸；图缺失或解码失败时回退到默认 2048。 */
val diffuseSize =
    runCatching {
        val img: BufferedImage? = ImageIO.read(File(diffuseFile.toString()))
        (img?.width ?: 2048) to (img?.height ?: 2048)
    }.getOrDefault(2048 to 2048)

private const val imageFilename = "${MOD_ID}_image"

@JvmName("getImageForInitialValueAndReset")
private fun getImage() =
    try {
        ImageIO.read(diffuseFile.toFile())
    } catch (e: Exception) {
        null
    }

var image: BufferedImage? = getImage()

fun watchImage() =
    FileSystems.getDefault().newWatchService()?.run {
        Thread {
            var key = dir.parent.register(
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
