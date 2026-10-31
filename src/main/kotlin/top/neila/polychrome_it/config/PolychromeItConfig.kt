package top.neila.polychrome_it.config

import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.name

/**
 * Polychrome It! 的配置文件约定（位于 Fabric 的 config 目录下）。
 *
 * 目录：<游戏目录>/config/polychrome_it/
 *   - config.json           本文件，描述世界纹理覆盖的 MC 世界范围
 *   - diffuse.png           WM 导出的反照率（RGB）
 *   - normal.png            WM 导出的法线（RGB，世界空间）
 *   - roughness.png         WM 导出的粗糙度（灰度）
 *   - height.png            WM 导出的高度（暂留待 POM v2，当前未用）
 *
 * 这是对 Chocapic13/Bliss 的修改所依赖的配置，原光影包署名保留、文件不被改动。
 */
data class PolychromeItConfig(
    val originX: Double,
    val originZ: Double,
    val sizeX: Double,
    val sizeZ: Double,
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

        val DEFAULT = PolychromeItConfig(0.0, 0.0, 2048.0, 2048.0, "off")

        fun load(): PolychromeItConfig {
            if (!configFile.exists()) return DEFAULT
            val text = runCatching { Files.readString(configFile) }.getOrNull() ?: return DEFAULT
            return PolychromeItConfig(
                originX = parse(text, "originX", DEFAULT.originX),
                originZ = parse(text, "originZ", DEFAULT.originZ),
                sizeX = parse(text, "sizeX", DEFAULT.sizeX),
                sizeZ = parse(text, "sizeZ", DEFAULT.sizeZ),
                debug = parseMode(text, "debug", DEFAULT.debug)
            )
        }

        private fun parse(text: String, key: String, default: Double): Double {
            val m = Regex("\"$key\"\\s*:\\s*([-+0-9.eE]+)").find(text)
            return m?.groupValues?.get(1)?.toDoubleOrNull() ?: default
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
