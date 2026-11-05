package top.neila.polychrome_it

import net.fabricmc.loader.api.FabricLoader
import org.slf4j.LoggerFactory
import java.awt.image.BufferedImage
import java.nio.file.Path
import java.nio.file.FileSystems
import java.io.File
import kotlin.io.path.name
import java.nio.file.StandardWatchEventKinds
import javax.imageio.ImageIO
import kotlin.io.path.copyTo
import kotlin.io.path.createSymbolicLinkPointingTo
import kotlin.io.path.exists
import kotlin.io.path.writeText

val logger = LoggerFactory.getLogger(MOD_ID)
val dir: Path = FabricLoader.getInstance().configDir.resolve(MOD_ID)

enum class PolychromeItTexture {
    diffuse,
    normal,
    roughness,
    ao;

    val imageFilename = "${name}.png"
    val path: Path = dir.resolveThis()

    val exists
        get() = path.exists().also {
            if (!it)
                logger.error(
                    "缺少世界纹理图 ${name}.png，跳过纹理打包。请把 WM 导出的图片放到 $dir。"
                )
        }

    protected fun Path.resolveThis(): Path = resolve(imageFilename)

    fun write(worldDir: Path, mcmeta: String) = worldDir.apply {
        resolve("$name.png.mcmeta").writeText(mcmeta)
        resolveThis().createSymbolicLinkPointingTo(path)
    }

    var size = Refreshable {
        runCatching {
            ImageIO.read(File(path.toString()))?.run {
                width to height
            }
        }.getOrDefault(defaultSize) ?: defaultSize
    }
    var image = Refreshable {
        try {
            ImageIO.read(File(path.toString()))
        } catch (e: Exception) {
            null
        }
    }

    fun watch() =
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
                            if (path is Path && path.fileName.name == imageFilename) {
                                size.refresh()
                                image.refresh()
                            }
                        }
                        if (!key.reset()) break
                    }
                }
            }.apply {
                isDaemon = true
                name = "PolychromeIt image watcher for $name"
                start()
            }

            Runtime.getRuntime().addShutdownHook(
                Thread { runCatching { close() } }
            )
        }

    companion object {
        private val defaultSize = 2048 to 2048
    }
}