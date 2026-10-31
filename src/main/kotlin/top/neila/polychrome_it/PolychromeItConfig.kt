package top.neila.polychrome_it

import net.fabricmc.loader.api.FabricLoader
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.io.path.exists

/**
 * Polychrome It! 的配置约定（位于 Fabric 的 config 目录下）。
 *
 * 目录：<游戏目录>/config/polychrome_it/
 *   - diffuse.png           WM 导出的反照率（RGB），覆盖范围的尺寸与原点由此图决定
 *   - normal.png            WM 导出的法线（RGB，世界空间）
 *   - roughness.png         WM 导出的粗糙度（灰度）
 *   - height.png            WM 导出的高度（暂留待 POM v2，当前未用）
 *
 * 这是对 Chocapic13/Bliss 的修改所依赖的配置，原光影包署名保留、文件不被改动。
 *
 * 诊断模式（debug）已迁移到光影设置（Shader Options）：在派生光影包的 settings.glsl 里
 * 注入为 POLYCHROME_DEBUG 开关，由 Iris 在着色器编译期决定分支，无需改动本 config。
 *
 * 世界纹理覆盖范围：原点固定为 (0, 0)，尺寸 (sizeX, sizeZ) 直接由 diffuse.png 的像素尺寸获取。
 */
data class PolychromeItConfig(
    val sizeX: Int,
    val sizeZ: Int,
) {
    companion object {
        val dir: Path = FabricLoader.getInstance().configDir.resolve("polychrome_it")
        val diffuseFile: Path = dir.resolve("diffuse.png")
        val normalFile: Path = dir.resolve("normal.png")
        val roughnessFile: Path = dir.resolve("roughness.png")
        val heightFile: Path = dir.resolve("height.png")

        val DEFAULT = PolychromeItConfig(2048, 2048)

        fun load(): PolychromeItConfig {
            val (w, h) = diffuseSize()
            return PolychromeItConfig(w, h)
        }

        /** 从 diffuse.png 读取像素尺寸；图缺失或解码失败时回退到默认 2048。 */
        private fun diffuseSize(): Pair<Int, Int> {
            if (!diffuseFile.exists()) return 2048 to 2048
            return runCatching {
                val img: BufferedImage? = ImageIO.read(File(diffuseFile.toString()))
                (img?.width ?: 2048) to (img?.height ?: 2048)
            }.getOrDefault(2048 to 2048)
        }
    }
}
