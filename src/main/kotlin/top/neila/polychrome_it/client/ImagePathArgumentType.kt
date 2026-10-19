package top.neila.polychrome_it.client

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.network.chat.Component
import java.awt.image.BufferedImage
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import javax.imageio.ImageIO

/**
 * 解析为 [Path] 的命令参数类型，替代 StringArgumentType.greedyString()：
 *
 * - [parse]：读取路径（可带引号，允许空格），基于游戏目录解析，并依次校验：
 *   不是目录、文件存在、且能被 ImageIO 解码为 `BufferedImage`（PNG/JPEG/GIF/BMP/WBMP）。
 * - [listSuggestions]：根据已输入的部分路径，补全游戏目录下的文件与目录（目录带 `/` 后缀以便继续导航）。
 *
 * 注意：parse 抛出异常不会导致命令补全失效 —— Brigadier 的建议上下文会回退到
 * 上一个已解析节点（字面量 `polychrome:setimagepath`），其子节点仍包含本参数节点，
 * ArgumentCommandNode.listSuggestions 会调用到这里的 [listSuggestions]。
 */
class ImagePathArgumentType : ArgumentType<Path> {
    private val basePath: Path = FabricLoader.getInstance().gameDir

    override fun parse(reader: StringReader): Path {
        // 贪婪读取剩余全部输入，随后消费到输入末尾（允许不带引号的空格）
        val raw = reader.remaining.trim()
        reader.cursor = reader.totalLength

        val path = basePath.resolve(unquote(raw)).normalize()

        when {
            Files.isDirectory(path) -> throw ERROR_IS_DIRECTORY.createWithContext(reader, raw)
            !Files.isRegularFile(path) -> throw ERROR_NOT_FOUND.createWithContext(reader, raw)
            !isImageReadable(path) -> throw ERROR_NOT_AN_IMAGE.createWithContext(reader, raw)
        }

        return path
    }

    override fun getExamples(): Collection<String> = EXAMPLES

    override fun <S> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        val remaining = builder.remaining.replace('\\', '/')
        val separatorIndex = remaining.lastIndexOf('/')
        val dirPart = if (separatorIndex >= 0)
            remaining.substring(0, separatorIndex + 1)
        else ""
        val prefix = if (separatorIndex >= 0)
            remaining.substring(separatorIndex + 1)
        else remaining

        val searchDir = basePath.resolve(dirPart).normalize()
        if (!Files.isDirectory(searchDir)) return builder.buildFuture()

        try {
            val entries = Files.list(searchDir).use { stream ->
                stream
                    .filter { !it.fileName.toString().startsWith(".") }
                    .filter { it.fileName.toString().startsWith(prefix, ignoreCase = true) }
                    .map { it to Files.isDirectory(it) }
                    .toList()
            }

            entries
                .sortedWith(
                    compareBy({ !it.second }, { it.first.fileName.toString().lowercase() })
                )
                .take(MAX_SUGGESTIONS)
                .forEach { (entry, isDirectory) ->
                    val suggestion = dirPart + entry.fileName.toString() + if (isDirectory) "/" else ""
                    builder.suggest(quoteIfNeeded(suggestion))
                }
        } catch (e: IOException) {
            // 目录读取失败时返回空建议
        }

        return builder.buildFuture()
    }

    private fun unquote(raw: String) =
        if (raw.length >= 2 && raw.startsWith('"') && raw.endsWith('"'))
            raw.substring(1, raw.length - 1)
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
        else
            raw

    /**
     * 尝试将文件解码为 BufferedImage：
     * [ImageIO.read] 在没有已注册的 ImageReader 能识别该格式时返回 null，
     * 读取失败时抛出异常——两种情况都视为“不是可处理的图片”。
     */
    private fun isImageReadable(path: Path) =
        try {
            ImageIO.read(path.toFile()) != null
        } catch (e: Exception) {
            false
        }


    private fun quoteIfNeeded(value: String) =
        if (value.all { StringReader.isAllowedInUnquotedString(it) })
            value
        else
            "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"") + "\""


    companion object {
        private val ERROR_NOT_FOUND = DynamicCommandExceptionType { raw ->
            Component.translatable("polychrome_it.commands.imagepath.not_found", raw)
        }

        private val ERROR_IS_DIRECTORY = DynamicCommandExceptionType { raw ->
            Component.translatable("polychrome_it.commands.imagepath.is_directory", raw)
        }

        private val ERROR_NOT_AN_IMAGE = DynamicCommandExceptionType { raw ->
            Component.translatable("polychrome_it.commands.imagepath.not_an_image", raw)
        }

        private val EXAMPLES = listOf("/path/to/a/vaild/image")

        private const val MAX_SUGGESTIONS = 100
    }
}
