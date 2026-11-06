package top.neila.polychrome_it.shaderpackpatcher

import top.neila.polychrome_it.logger
import java.nio.file.Path
import kotlin.io.path.*

/** 在 .fsh 里注入 include 与本 mod 的调用片段。preInclude 会在 include 之前写入（用于定义开关宏）。 */
fun Path.inject(
    includeAnchor: String,
    callBefore: String,
    callText: String,
    preInclude: String = ""
) {
    if (!exists()) {
        logger.warn("未找到 ${fileName}，跳过 GLSL 注入。")
        return
    }
    var text = readText()
    if (text.contains(includeAnchor)) {
        val inc = buildString {
            append(includeAnchor)
            appendLine()
            if (preInclude.isNotEmpty()) appendLine(preInclude)
            append("#include \"/lib/polychrome_it.glsl\"")
        }
        text = text.replaceFirst(includeAnchor, inc)
    } else {
        logger.warn("$fileName 未找到 include 锚点，跳过 include 注入。")
        return
    }
    val k = text.indexOf(callBefore)
    if (k >= 0) {
        text = StringBuilder(text).insert(k, callText).toString()
    } else {
        logger.warn("$fileName 未找到调用锚点，跳过调用注入。")
        return
    }
    writeText(text)
}
