package top.neila.polychrome_it.config

import net.fabricmc.loader.api.FabricLoader
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.io.path.exists
import kotlin.io.path.name

/**
 * Polychrome It! 的配置文件约定（位于 Fabric 的 config 目录下）。
 *
 * 目录：<游戏目录>/config/polychrome_it/
 *   - config.json           本文件，仅描述诊断模式开关（debug）
 *   - diffuse.png           WM 导出的反照率（RGB），覆盖范围的尺寸与原点由此图决定
 *   - normal.png            WM 导出的法线（RGB，世界空间）
 *   - roughness.png         WM 导出的粗糙度（灰度）
 *   - height.png            WM 导出的高度（暂留待 POM v2，当前未用）
 *
 * 这是对 Chocapic13/Bliss 的修改所依赖的配置，原光影包署名保留、文件不被改动。
 *
 * 世界纹理覆盖范围：原点固定为 (0, 0)，尺寸 (sizeX, sizeZ) 直接由 diffuse.png 的像素尺寸获取。
 */
data class PolychromeItConfig(
    val sizeX: Int,
    val sizeZ: Int,
    // 诊断模式：off=关闭（真实 PBR）；detect=隔离识别（洋红/青/黄）；
    // normal=把世界法线画成 RGB；specular=把粗糙度画成灰度。后两者用于确认法线/粗糙度通道被采样应用。
    val debug: String = "off"
) {
    companion object {
        val dir: Path = FabricLoader.getInstance().configDir.resolve("polychrome_it")
        val configFile: Path = dir.resolve("config.json")
        val diffuseFile: Path = dir.resolve("diffuse.png")
        val normalFile: Path = dir.resolve("normal.png")
        val roughnessFile: Path = dir.resolve("roughness.png")
        val heightFile: Path = dir.resolve("height.png")

        val DEFAULT = PolychromeItConfig(2048, 2048, "off")

        /** 原点固定为 (0, 0)。 */
        const val ORIGIN_X = 0.0
        const val ORIGIN_Z = 0.0

        fun load(): PolychromeItConfig {
            val (w, h) = diffuseSize()
            val debug = if (configFile.exists()) {
                val text = runCatching { Files.readString(configFile) }.getOrNull()
                text?.let { parseMode(it, "debug", DEFAULT.debug) } ?: DEFAULT.debug
            } else DEFAULT.debug
            return PolychromeItConfig(w, h, debug)
        }

        /** 从 diffuse.png 读取像素尺寸；图缺失或解码失败时回退到默认 2048。 */
        private fun diffuseSize(): Pair<Int, Int> {
            if (!diffuseFile.exists()) return 2048 to 2048
            return runCatching {
                val img: BufferedImage? = ImageIO.read(File(diffuseFile.toString()))
                (img?.width ?: 2048) to (img?.height ?: 2048)
            }.getOrDefault(2048 to 2048)
        }

        private fun parseMode(text: String, key: String, default: String): String {
            // 兼容旧版 "debug": true -> "detect"
            val boolM = Regex("\"$key\"\\s*:\\s*(true|false)").find(text)
            if (boolM != null) return if (boolM.groupValues[1] == "true") "detect" else default
            val strM = Regex("\"$key\"\\s*:\\s*\"([^\"]+)\"").find(text)
            val v = strM?.groupValues?.get(1)?.lowercase() ?: default
            return when (v) {
                "detect", "normal", "specular" -> v
                else -> default
            }
        }
    }
}
