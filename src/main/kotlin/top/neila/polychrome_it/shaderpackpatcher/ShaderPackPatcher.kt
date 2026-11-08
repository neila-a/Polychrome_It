@file:OptIn(ExperimentalPathApi::class)

package top.neila.polychrome_it.shaderpackpatcher

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

import top.neila.polychrome_it.*
import java.nio.file.Path
import kotlin.io.path.*

/**
 * 一个具体光影包的注入器（枚举项）。框架负责发现匹配的光影包、复制成派生包并调用 generate；
 * 各枚举项只需给出“如何识别”与“如何注入”。共享的复制 / 纹理打包 / lang / GLSL 注入工具见下方受保护方法。
 */
@Suppress("EnumEntryName", "ProtectedInFinal")
enum class ShaderPackPatcher {
    bliss {
        override fun injectShaders(derived: Path) = derived.runCatching {
            val props = resolve("shaders/shaders.properties")
            check(props.exists()) {
                logger.warn("未找到 shaders.properties，跳过。")
            }

            val text = props.readText()
            val idx = text.indexOf("iris.features.optional")
            check(idx >= 0) {
                logger.warn("shaders.properties 未找到 iris.features.optional，跳过 sampler 注册。")
            }

            val lineEnd = text.indexOf('\n', idx)

            val startText = text.substring(lineEnd + 1)
            val endText = text.substring(0, lineEnd + 1)

            val builder = StringBuilder(startText + "\n")
            PolychromeItTexture.entries.forEach {
                builder.appendLine(
                    "customTexture.polychromeIt${
                        it.name.replaceFirstChar { char ->
                            char.uppercaseChar()
                        }
                    } = texture/world/${it.imageFilename}"
                )
            }
            builder.appendLine().append(endText)

            // 把诊断开关放到 Misc/Debugging 设置页，紧挨 Bliss 自带 DEBUG_VIEW 之后
            props.writeText(
                builder.toString()
                    .replace("DEBUG_VIEW [the_end_orb]", "DEBUG_VIEW POLYCHROME_IT_DEBUG [the_end_orb]")
            )

            // 全精度地形（all_solid）：Bliss 已通过 flat varying float blockID（= mc_Entity.x，即本 mod 在
            // block.properties 中为本方块指定的固定 ID）把方块 ID 传到片元着色器，故只需定义 POLYCHROME_IT_HAS_BLOCKID，
            // polychrome_it.glsl 直接用 blockID 与注入的 #define 比对识别。
            // include 锚点放在 blockID 声明之后，确保函数定义时 blockID 已在其作用域内。
            resolve("shaders/dimensions/all_solid.fsh").inject(
                includeAnchor = "flat varying float blockID;",
                callBefore = "vec4 data1 = clamp( encode(viewToWorld(normal), PackLightmaps), 0.0, 1.0);",
                callText = loadPatch("call_injection.glsl"),
                preInclude = "#define POLYCHROME_IT_HAS_BLOCKID"
            )
            // DH LOD 地形（DH_solid）：无 blockID，polychrome_it.glsl 自动退化为洋红后备判定
            // （DH 方块仍由 DhApiBlockColorOverrideEvent 回传洋红）。
            resolve("shaders/dimensions/DH_solid.fsh").inject(
                includeAnchor = "#include \"/lib/settings.glsl\"",
                callBefore = "gl_FragData[0] = vec4(encodeVec2(Albedo.x,data1.x),",
                callText = loadPatch("dh_call_injection.glsl")
            )

            // 往 block.properties 注册本 mod 方块固定 ID，使 Iris 把该 ID 写入 mc_Entity.x；
            // 着色器据此 ID 精确识别（见下方 #define）。必须在写 polychrome_it.glsl 之前调用。
            val blockIds = AppendBlockPropertyIds(this)

            val libFile = resolve("shaders/lib/polychrome_it.glsl")
            libFile.parent.toFile().mkdir()
            val diffuseSize = PolychromeItTexture.diffuse.size
            // 把各方块在 block.properties 里分配到的固定 ID 以 #define 注入（mc_Entity.x 携带的就是这个 ID）。
            val idDefines = buildString {
                PolychromeItBlock.entries.forEach { block ->
                    blockIds[block]?.let {
                        appendLine("#define POLYCHROME_IT_ID_${block.name.uppercase()} $it")
                    }
                }

                appendLine()
                append(
                    loadPatch("polychrome_it.glsl")
                        .replace("__SIZE_X__", "${diffuseSize.first}f")
                        .replace("__SIZE_Z__", "${diffuseSize.second}f")
                )
            }
            libFile.writeText(idDefines)

            // 诊断开关：注入 settings.glsl（让 Iris 在光影设置里显示可切换项），并补全 lang 显示名。
            resolve("shaders/lib/settings.glsl").injectAfterLine(
                "#define DEBUG_VIEW debug_OFF",
                loadPatch("settings_injection.glsl")
            )
            patchLang(this)
        }
    };

    /** 在派生包里注入着色器片段（include / 调用 / 设置开关 / lang 显示名等），由各枚举项实现。 */
    abstract fun injectShaders(derived: Path): Any

    /** 判定给定 shaderpack 目录是否匹配此注入器。 */
    fun matches(packDir: Path) = packDir.isDirectory() && packDir.fileName.toString()
        .let { n -> n.contains(name, ignoreCase = true) && !n.contains(DERIVED_SUFFIX) }

    /** 由框架调用：复制原包并跑完整注入流程。子类无需重写。 */
    fun generate(packDir: Path) = packDir.run {
        val name = fileName.toString() + DERIVED_SUFFIX
        val derived = parent.resolve(name)
        if (derived.exists()) derived.deleteRecursively()
        logger.info("开始生成派生光影包：$name")
        toFile().copyRecursively(derived.toFile(), true)
        injectShaders(derived)
        packWorldTextures(derived)
        logger.info("派生包生成完成：$derived 。请选择光影 “$name”，并在光影设置里切换 Polychrome It! 调试开关。")
    }

    /** 从本注入器专属资源目录读取注入片段，统一去除尾部换行。 */
    protected fun loadPatch(patchName: String) =
        javaClass.getResourceAsStream("/top/neila/polychrome_it/shaderpackpatcher/patches/$name/$patchName")!!
            .bufferedReader().use { it.readText() }
            .trimEnd('\n')


    /** 追加 en_us / zh_cn 显示名到派生包的 lang 文件（各光影包通用布局）。 */
    protected fun patchLang(derived: Path) = listOf("en_us", "zh_cn").forEach {
        derived.resolve("shaders/lang/$it.lang").toFile().appendText("\n" + loadPatch("$it.lang"))
    }

    /**
     * 世界纹理打包（labPBR：diffuse / normal / roughness）。
     */
    protected fun packWorldTextures(derived: Path) = derived.resolve("shaders/texture/world").run {
        toFile().mkdir()

        val mcmeta = loadPatch("image.mcmeta")
        PolychromeItTexture.entries.filter {
            it.exists
        }.forEach {
            it.write(this, mcmeta)
        }
        logger.info("世界纹理打包完成：$this")
    }
}
