package top.neila.polychrome_it.shaderpackpatcher

import top.neila.polychrome_it.PolychromeItBlock
import top.neila.polychrome_it.logger
import java.nio.file.Path
import kotlin.io.path.*

/**
 * 往派生包的 shaders/block.properties 追加本 mod 方块的固定 ID。
 *
 * Iris 在加载光影时会解析 block.properties，把其中为方块指定的整数 ID 经
 * BlockMaterialMapping / MixinChunkMeshBuildTask 写入 mc_Entity.x，
 * 因此着色器里读出 blockID（= mc_Entity.x）就等于这里写入的 ID。
 * 用它取代注册表数字 ID，可避免与 Iris 内部 ID 体系不一致导致识别失败。
 *
 * 必须在派生包里 block.properties 已就位（复制自原包）之后调用。返回“枚举名大写 -> 分配到的 ID”，
 * 供生成 #define 使用。
 */
class AppendBlockPropertyIds(derived: Path) : LinkedHashMap<String, Int>() {
    init {
        val bp = derived.resolve("shaders/block.properties")
        if (!bp.exists()) {
            logger.warn("未找到 block.properties，跳过方块 ID 映射（世界内方块将无法被识别）。")
        } else {
            // 先读出现有最大 ID，再追加写入（保留原文件编码，避免破坏 Iris 的 ISO_8859_1 读取）。
            var maxId = -1
            val idRegex = Regex("""^block\.(\d+)\s*=""")
            for (line in bp.readLines()) {
                val m = idRegex.find(line)
                if (m != null) {
                    val id = m.groupValues[1].toIntOrNull()
                    if (id != null && id > maxId) maxId = id
                }
            }
            var nextId = maxId + 1
            val sb = StringBuilder()
            for (block in PolychromeItBlock.entries) {
                val key = block.registeredName
                sb.append("\nblock.$nextId=$key")
                this[block.name.uppercase()] = nextId
                nextId++
            }
            sb.append('\n')
            bp.appendText(sb.toString())
            logger.info("已在 block.properties 注册本 mod 方块 ID：$this")
        }
    }
}
        