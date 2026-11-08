package top.neila.polychrome_it.shaderpackpatcher

import top.neila.polychrome_it.PolychromeItBlock
import top.neila.polychrome_it.logger
import java.nio.file.Path
import java.util.Properties
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
class AppendBlockPropertyIds(derived: Path) : LinkedHashMap<PolychromeItBlock, Int>() {
    private val properties = Properties()
    private val file = derived.resolve("shaders/block.properties")

    init {
        file.takeIf {
            it.exists()
        }?.run {
            properties.load(reader())
        } ?: logger.warn("未找到 block.properties，跳过方块 ID 映射（世界内方块将无法被识别）。")

        var nowId = properties.stringPropertyNames().maxOf {
            it.runCatching {
                // 删除"block."
                substring(6)
            }.getOrNull()?.toIntOrNull() ?: 0
        }
        PolychromeItBlock.entries.forEach {
            properties.setProperty("block.${++nowId}", it.registeredName)
            this[it] = nowId
        }
        properties.store(file.writer(), "")
        logger.info("已在 block.properties 注册本 mod 方块 ID：$this")
    }
}
