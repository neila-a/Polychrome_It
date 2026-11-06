package top.neila.polychrome_it.shaderpackpatcher

import top.neila.polychrome_it.logger
import java.nio.file.Path
import kotlin.io.path.*

/** 在第一个包含 anchor 的行之后插入 text（整段）。 */
fun Path.injectAfterLine(anchor: String, text: String) {
    if (!exists()) {
        logger.warn("未找到 ${fileName}，跳过注入。")
        return
    }
    val content = readText()
    val idx = content.indexOf(anchor)
    if (idx < 0) {
        logger.warn("$fileName 未找到锚点 '$anchor'，跳过注入。")
        return
    }
    val lineEnd = content.indexOf('\n', idx)
    val insertAt = if (lineEnd < 0) content.length else lineEnd + 1
    writeText(StringBuilder(content).insert(insertAt, text))
}
