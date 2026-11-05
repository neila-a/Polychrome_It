@file:OptIn(ExperimentalPathApi::class)

package top.neila.polychrome_it

/**
 * 派生光影包注入框架（枚举实现）。
 *
 * 每个枚举项描述如何把 Polychrome It! 的能力注入某个特定光影包（例如 Bliss），
 * 而不修改原光影包的任何文件：把原包复制成 “原包名 + Polychrome It” 派生光影包，
 * 在副本里注入世界空间 PBR 的 GLSL 与世界纹理。
 *
 * 原始光影包保持字节不变、署名保留，做法参考 Euphoria Patches（派生包而非改原包）。
 * 诊断模式：在 settings.glsl 注入 POLYCHROME_DEBUG 开关，Iris 把它渲染成可切换的光影设置项；
 * 选定状态在着色器编译期以 #define 形式注入，从而决定 applyPolychromeIt 里的 #if 分支，无需重建派生包。
 *
 * 新增支持的光影包：在枚举里加一个项，提供 id / 资源子目录 / 检测谓词，并 override injectShaders
 * （必要时重写 packWorldTextures 指定纹理目录）即可，无需改动框架与其它项。
 * 枚举取封闭集（compileKotlin 期可知全部），天然提供 entries 遍历与穷举 when——与本项目
 * PolychromeItBlock 的枚举风格一致。
 */

import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Path
import kotlin.io.path.*

/** 派生包名统一后缀。所有注入器产生的派生包都以此结尾，便于框架识别并跳过自家的派生包。 */
private const val DERIVED_SUFFIX = " + Polychrome It"

private val gameDir: Path get() = FabricLoader.getInstance().gameDir

/**
 * 一个具体光影包的注入器（枚举项）。框架负责发现匹配的光影包、复制成派生包并调用 generate；
 * 各枚举项只需给出“如何识别”与“如何注入”。共享的复制 / 纹理打包 / lang / GLSL 注入工具见下方受保护方法。
 */
enum class ShaderPackPatcher {
    bliss {
        override fun injectShaders(derived: Path) {
            val props = derived.resolve("shaders/shaders.properties")
            if (props.exists()) {
                var text = props.readText()
                val idx = text.indexOf("iris.features.optional")
                if (idx >= 0) {
                    val lineEnd = text.indexOf('\n', idx)

                    val startText = text.substring(lineEnd + 1)
                    val endText = text.substring(0, lineEnd + 1)

                    text = startText + "\n"
                    for (texture in PolychromeItTexture.entries) {
                        text += "customTexture.polychrome${
                            texture.name.replaceFirstChar {
                                it.uppercaseChar()
                            }
                        } = texture/world/${texture.imageFilename}\n"
                    }
                    text += "\n" + endText
                    // 把诊断开关放到 Misc/Debugging 设置页，紧挨 Bliss 自带 DEBUG_VIEW 之后
                    text = text.replace("DEBUG_VIEW [the_end_orb]", "DEBUG_VIEW POLYCHROME_DEBUG [the_end_orb]")
                    props.writeText(text)
                } else {
                    logger.warn("shaders.properties 未找到 iris.features.optional，跳过 sampler 注册。")
                }
            } else {
                logger.warn("未找到 shaders.properties，跳过。")
            }

            // 全精度地形（all_solid）：Bliss 已通过 flat varying float blockID（= mc_Entity.x，即本 mod 在
            // block.properties 中为本方块指定的固定 ID）把方块 ID 传到片元着色器，故只需定义 POLYCHROME_HAS_BLOCKID，
            // polychrome_it.glsl 直接用 blockID 与注入的 #define 比对识别。
            // include 锚点放在 blockID 声明之后，确保函数定义时 blockID 已在其作用域内。
            injectIntoFsh(
                derived.resolve("shaders/dimensions/all_solid.fsh"),
                includeAnchor = "flat varying float blockID;",
                callBefore = "vec4 data1 = clamp( encode(viewToWorld(normal), PackLightmaps), 0.0, 1.0);",
                callText = loadPatch("call_injection.glsl"),
                preInclude = "#define POLYCHROME_HAS_BLOCKID"
            )
            // DH LOD 地形（DH_solid）：无 blockID，polychrome_it.glsl 自动退化为洋红后备判定
            // （DH 方块仍由 DhApiBlockColorOverrideEvent 回传洋红）。
            injectIntoFsh(
                derived.resolve("shaders/dimensions/DH_solid.fsh"),
                includeAnchor = "#include \"/lib/settings.glsl\"",
                callBefore = "gl_FragData[0] = vec4(encodeVec2(Albedo.x,data1.x),",
                callText = loadPatch("dh_call_injection.glsl")
            )

            // 往 block.properties 注册本 mod 方块固定 ID，使 Iris 把该 ID 写入 mc_Entity.x；
            // 着色器据此 ID 精确识别（见下方 #define）。必须在写 polychrome_it.glsl 之前调用。
            val blockIds = AppendBlockPropertyIds(derived)

            val libFile = derived.resolve("shaders/lib/polychrome_it.glsl")
            libFile.parent.toFile().mkdir()
            val diffuseSize = PolychromeItTexture.diffuse.size.value
            // 把各方块在 block.properties 里分配到的固定 ID 以 #define 注入（mc_Entity.x 携带的就是这个 ID）。
            val idDefines = buildString {
                for (block in PolychromeItBlock.entries) {
                    val id = blockIds[block.name.uppercase()]
                    if (id != null) appendLine("#define POLYCHROME_ID_${block.name.uppercase()} $id")
                }
            }
            libFile.writeText(
                idDefines + "\n" + loadPatch("polychrome_it.glsl")
                    .replace("__SIZE_X__", "${diffuseSize.first}f")
                    .replace("__SIZE_Z__", "${diffuseSize.second}f")
            )

            // 诊断开关：注入 settings.glsl（让 Iris 在光影设置里显示可切换项），并补全 lang 显示名。
            injectAfterLine(
                derived.resolve("shaders/lib/settings.glsl"),
                "#define DEBUG_VIEW debug_OFF",
                loadPatch("settings_injection.glsl")
            )
            patchLang(derived)
        }
    };

    /** 在派生包里注入着色器片段（include / 调用 / 设置开关 / lang 显示名等），由各枚举项实现。 */
    abstract fun injectShaders(derived: Path)

    /** 判定给定 shaderpack 目录是否匹配此注入器。 */
    fun matches(packDir: Path) = packDir.isDirectory() && packDir.fileName.toString()
        .let { n -> n.contains(name, ignoreCase = true) && !n.contains(DERIVED_SUFFIX) }

    /** 由框架调用：复制原包并跑完整注入流程。子类无需重写。 */
    fun generate(packDir: Path) {
        val name = packDir.fileName.toString() + DERIVED_SUFFIX
        val derived = packDir.parent.resolve(name)
        if (derived.exists()) derived.deleteRecursively()
        logger.info("开始生成派生光影包：$name")
        packDir.toFile().copyRecursively(derived.toFile(), true)
        injectShaders(derived)
        packWorldTextures(derived)
        logger.info("派生包生成完成：$derived 。请选择光影 “$name”，并在光影设置里切换 Polychrome It! 调试开关。")
    }

    /** 从本注入器专属资源目录读取注入片段，统一去除尾部换行。 */
    fun loadPatch(patchName: String) =
        ShaderPackPatcher::class.java.getResourceAsStream("/assets/polychrome_it/patches/$name/$patchName")!!
            .bufferedReader().use { it.readText() }
            .trimEnd('\n')

    /** 在位置 at 处插入一整段 text（其后补一个换行）。 */
    private fun String.insertString(at: Int, text: String) =
        substring(0, at) + text + "\n" + substring(at)

    /** 在第一个包含 anchor 的行之后插入 text（整段）。 */
    protected fun injectAfterLine(file: Path, anchor: String, text: String) {
        if (!file.exists()) {
            logger.warn("未找到 ${file.fileName}，跳过注入。")
            return
        }
        val content = file.readText()
        val idx = content.indexOf(anchor)
        if (idx < 0) {
            logger.warn("${file.fileName} 未找到锚点 '$anchor'，跳过注入。")
            return
        }
        val lineEnd = content.indexOf('\n', idx)
        val insertAt = if (lineEnd < 0) content.length else lineEnd + 1
        file.writeText(content.insertString(insertAt, text))
    }

    /** 在 .fsh 里注入 include 与本 mod 的调用片段。preInclude 会在 include 之前写入（用于定义开关宏）。 */
    protected fun injectIntoFsh(
        fsh: Path,
        includeAnchor: String,
        callBefore: String,
        callText: String,
        preInclude: String = ""
    ) {
        if (!fsh.exists()) {
            logger.warn("未找到 ${fsh.fileName}，跳过 GLSL 注入。")
            return
        }
        var text = fsh.readText()
        if (text.contains(includeAnchor)) {
            val inc = buildString {
                append(includeAnchor).appendLine()
                if (preInclude.isNotEmpty()) appendLine(preInclude)
                append("#include \"/lib/polychrome_it.glsl\"")
            }
            text = text.replaceFirst(includeAnchor, inc)
        } else {
            logger.warn("${fsh.fileName} 未找到 include 锚点，跳过 include 注入。")
            return
        }
        val k = text.indexOf(callBefore)
        if (k >= 0) {
            text = text.insertString(k, callText)
        } else {
            logger.warn("${fsh.fileName} 未找到调用锚点，跳过调用注入。")
            return
        }
        fsh.writeText(text)
    }

    /** 追加 en_us / zh_cn 显示名到派生包的 lang 文件（各光影包通用布局）。 */
    protected fun patchLang(derived: Path) {
        for (lang in listOf("en_us", "zh_cn")) {
            derived.resolve("shaders/lang/$lang.lang").toFile().appendText("\n" + loadPatch("$lang.lang"))
        }
    }

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
    private class AppendBlockPropertyIds(derived: Path) : LinkedHashMap<String, Int>() {
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

    /**
     * 世界纹理打包（labPBR：diffuse / normal / roughness）。
     */
    protected fun packWorldTextures(derived: Path) {
        val worldDir = derived.resolve("shaders/texture/world")
        worldDir.toFile().mkdir()

        val mcmeta = loadPatch("image.mcmeta")
        for (texture in PolychromeItTexture.entries) {
            if (texture.exists) {
                texture.write(worldDir, mcmeta)
            }
        }
        logger.info("世界纹理打包完成：$worldDir")
    }
}

/**
 * 客户端初始化时调用：为 shaderpacks/ 下每个受支持的光影包生成派生包。
 * 派生包（名字含后缀）会被跳过，避免重复处理自家产物。
 */
fun generatePatchedShaderpack() {
    val shaderpacks = gameDir.resolve("shaderpacks")
    if (!shaderpacks.exists()) {
        logger.warn("未找到 shaderpacks/ 目录，跳过派生包生成。")
        return
    }
    val entries = shaderpacks.listDirectoryEntries()
        .filter { !it.fileName.toString().contains(DERIVED_SUFFIX) }
    var handled = 0
    for (pack in entries) {
        val patcher = ShaderPackPatcher.entries.firstOrNull { it.matches(pack) } ?: continue
        try {
            patcher.generate(pack)
            handled++
        } catch (e: Exception) {
            logger.error("为 ${pack.fileName} 生成派生包失败：${e.message}")
            e.printStackTrace()
        }
    }
    if (handled == 0) {
        logger.warn(
            "未在 shaderpacks/ 中找到任何支持的光影包（支持：${ShaderPackPatcher.entries.map { it.name }}）。" +
                    "请先放入对应光影包后重启游戏。"
        )
    }
}
