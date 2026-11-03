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
    roughness {
        override fun copy(worldDir: Path): Path {
            val path = worldDir.resolveThis()
            val file = image.value ?: return path
            val spec = BufferedImage(file.width, file.height, BufferedImage.TYPE_INT_RGB)
            for (y in 0 until file.height) {
                for (x in 0 until file.width) {
                    val rgb = file.getRGB(x, y)
                    val rr = (rgb shr 16) and 0xFF
                    val gg = (rgb shr 8) and 0xFF
                    val bb = rgb and 0xFF
                    val roughVal = if (rr == gg && gg == bb) rr else (rr + gg + bb) / 3
                    spec.setRGB(x, y, (roughVal shl 16) or (255 shl 8) or 0)
                }
            }
            ImageIO.write(spec, "png", File(path.toString()))
            return path
        }
    },
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

    open fun copy(worldDir: Path) = worldDir.resolveThis().createSymbolicLinkPointingTo(path)
    fun write(worldDir: Path, mcmeta: String) {
        worldDir.resolve("$name.png.mcmeta").writeText(mcmeta)
        copy(worldDir)
    }

    private val defaultSize = 2048 to 2048

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
}