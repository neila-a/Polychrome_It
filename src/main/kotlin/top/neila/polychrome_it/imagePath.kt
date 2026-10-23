package top.neila.polychrome_it

import net.fabricmc.loader.api.FabricLoader
import java.awt.image.BufferedImage
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO

private val configFile =
    FabricLoader.getInstance().configDir.resolve("polychrome_it_image_path.txt")

/** 当前图片的绝对路径；为空字符串表示未设置。 */
@Volatile
var imagePath = ""
    get() = try {
        if (Files.isRegularFile(configFile))
            Files.readString(configFile).trim()
        else
            ""
    } catch (e: IOException) {
        ""
    }
    set(path) {
        if (path == imagePath) return
        field = path

        try {
            Files.writeString(configFile, path)
        } catch (e: IOException) {
            // 写盘失败（如只读文件系统）时仅保留内存中的值
        }


        cachedImage = null
        if (path.isNotEmpty())
            try {
                cachedImage = ImageIO.read(Path.of(path).toFile())
            } catch (e: Exception) {
            }
    }

/** 当前图片的解码结果；路径为空或文件不可读时为 null。 */
var cachedImage: BufferedImage? = null
    private set
