package top.neila.polychrome_it

import net.fabricmc.api.ModInitializer
import net.fabricmc.loader.api.FabricLoader.getInstance
import org.slf4j.Logger
import org.slf4j.LoggerFactory.getLogger
import top.neila.polychrome_it.shaderpackpatcher.generatePatchedShaderpack
import java.nio.file.Path
import java.nio.file.FileSystems.getDefault
import java.io.File
import kotlin.io.path.name
import java.nio.file.StandardWatchEventKinds.*
import javax.imageio.ImageIO.read
import kotlin.concurrent.thread
import kotlin.io.path.createSymbolicLinkPointingTo
import kotlin.io.path.exists
import kotlin.io.path.writeText

val logger: Logger = getLogger(MOD_ID)
val dir: Path = getInstance().configDir.resolve(MOD_ID)

@Suppress("EnumEntryName", "ProtectedInFinal")
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

    private fun get() = path.runCatching {
        read(toFile())
    }.getOrNull()

    @Volatile
    var image = get()
    fun freshen() {
        image = get()
    }

    val size
        get() = image?.run {
            width to height
        } ?: (
                2048 to 2048 // 默认值
                )

    fun watch() =
        getDefault().newWatchService()?.run {
            thread(
                start = true,
                isDaemon = true,
                name = "Polychrome It! image watcher for $name"
            ) {
                generatePatchedShaderpack()
                var key = dir.parent.register(
                    this,
                    ENTRY_CREATE,
                    ENTRY_DELETE,
                    ENTRY_MODIFY
                )
                runCatching {
                    while (true) {
                        key = take()
                        key.pollEvents().forEach {
                            val path = it.context()
                            if (path is Path && path.fileName.name == imageFilename) {
                                freshen()
                                generatePatchedShaderpack()
                            }
                        }
                        if (!key.reset()) break
                    }
                }
            }

            Runtime.getRuntime().addShutdownHook(
                Thread { runCatching { close() } }
            )
        }

    companion object : ModInitializer {
        override fun onInitialize() = diffuse.watch() ?: Unit
    }
}