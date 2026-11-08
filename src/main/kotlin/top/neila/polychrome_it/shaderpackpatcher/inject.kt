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
) = runCatching {
    check(exists()) {
        logger.warn("未找到 ${fileName}，跳过 GLSL 注入。")
    }

    var text = readText()
    check(text.contains(includeAnchor)) {
        logger.warn("$fileName 未找到 include 锚点，跳过 include 注入。")
    }

    text = text.replaceFirst(includeAnchor, buildString {
        append(includeAnchor)
        appendLine()
        if (preInclude.isNotEmpty()) appendLine(preInclude)
        append("#include \"/lib/polychrome_it.glsl\"")
    })

    text.indexOf(callBefore).let {
        check(it >= 0) {
            logger.warn("$fileName 未找到调用锚点，跳过调用注入。")
        }
        text = StringBuilder(text).insert(it, callText).toString()
    }

    writeText(text)
}
