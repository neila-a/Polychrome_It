package top.neila.polychrome_it.shaderpackpatcher

import net.fabricmc.loader.api.FabricLoader.getInstance
import kotlin.io.path.*
import top.neila.polychrome_it.logger

/** 派生包名统一后缀。所有注入器产生的派生包都以此结尾，便于框架识别并跳过自家的派生包。 */
const val DERIVED_SUFFIX = " + Polychrome It"

/**
 * 客户端初始化时调用：为 shaderpacks/ 下每个受支持的光影包生成派生包。
 * 派生包（名字含后缀）会被跳过，避免重复处理自家产物。
 */
fun generatePatchedShaderpack() {
    val shaderpacks = getInstance().gameDir.resolve("shaderpacks")
    if (!shaderpacks.exists()) {
        logger.warn("未找到 shaderpacks/ 目录，跳过派生包生成。")
        return
    }
    val entries = shaderpacks.listDirectoryEntries()
        .filter { !it.fileName.toString().contains(DERIVED_SUFFIX) }
    var handled = 0
    for (pack in entries) {
        val patcher = ShaderPackPatcher.entries.firstOrNull { it.matches(pack) } ?: continue
        runCatching {
            patcher.generate(pack)
            handled++
        }.onFailure {
            logger.error("为 ${pack.fileName} 生成派生包失败：${it.message}")
            it.printStackTrace()
        }
    }
    if (handled == 0) {
        logger.warn(
            "未在 shaderpacks/ 中找到任何支持的光影包（支持：${ShaderPackPatcher.entries.map { it.name }}）。" +
                    "请先放入对应光影包后重启游戏。"
        )
    }
}