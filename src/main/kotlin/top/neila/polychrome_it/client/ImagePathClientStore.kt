package top.neila.polychrome_it.client

import net.fabricmc.loader.api.FabricLoader
import java.awt.image.BufferedImage
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO

/**
 * 图像路径的客户端本地存储。
 *
 * 路径持久化在游戏目录的 `config/polychrome_it/image_path.txt`（单行纯文本，内容为图片的绝对路径），
 * 取代原先存放在服务器存档（SavedData）中的方式：图像像素方块的取色完全发生在客户端，
 * 路径也只应影响本地，不再随存档同步、也不需要在服务端存在。
 *
 * 同时缓存已解码的 [BufferedImage]，避免每帧重复读盘；
 * [setImagePath] 更换路径时会一并刷新缓存。
 */
object ImagePathClientStore {
    private val configFile: Path =
        FabricLoader.getInstance().configDir.resolve("polychrome_it/image_path.txt")

    /** 当前图片的绝对路径；为空字符串表示未设置。 */
    @Volatile
    var imagePath: String = load()
        private set

    /** 当前图片的解码结果；路径为空或文件不可读时为 null。 */
    @Volatile
    var cachedImage: BufferedImage? = null
        private set

    init {
        refreshImage()
    }

    fun setImagePath(path: String) {
        if (path == imagePath) return
        imagePath = path
        save()
        refreshImage()
    }

    private fun refreshImage() {
        cachedImage = if (imagePath.isEmpty())
            null
        else
            try {
                ImageIO.read(Path.of(imagePath).toFile())
            } catch (e: Exception) {
                null
            }
    }

    private fun load(): String =
        try {
            if (Files.isRegularFile(configFile)) Files.readString(configFile).trim() else ""
        } catch (e: IOException) {
            ""
        }

    private fun save() {
        try {
            Files.createDirectories(configFile.parent)
            Files.writeString(configFile, imagePath)
        } catch (e: IOException) {
            // 写盘失败（如只读文件系统）时仅保留内存中的值
        }
    }
}
