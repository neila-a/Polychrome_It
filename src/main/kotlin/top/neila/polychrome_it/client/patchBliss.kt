package top.neila.polychrome_it.client

/**
 * Polychrome It! 轻量版注入器。
 *
 * 不修改 Bliss 的任何文件：把 Bliss 复制成 "Bliss + Polychrome It" 派生光影包，
 * 在副本里注入世界空间 PBR 的 GLSL 与世界纹理。这是对 Chocapic13/Bliss 的修改，
 * 原始光影包保持字节不变、署名保留。做法参考 Euphoria Patches（派生包而非改原包）。
 *
 * 诊断模式：在 settings.glsl 注入
 * POLYCHROME_DEBUG 开关，Iris 把它渲染成可切换的光影设置项；选定状态在着色器编译期
 * 以 #define 形式注入，从而决定 applyPolychromeIt 里的 #if 分支，无需重建派生包。
 */

import net.fabricmc.loader.api.FabricLoader
import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory
import top.neila.polychrome_it.MOD_ID
import top.neila.polychrome_it.PolychromeItConfig
import java.awt.image.BufferedImage
import java.nio.file.*
import javax.imageio.ImageIO
import java.io.File
import java.util.stream.Collectors
import kotlin.io.path.exists
import kotlin.io.path.name

private const val DERIVED_NAME = "Bliss + Polychrome It"
private val gameDir: Path get() = FabricLoader.getInstance().gameDir

private val logger = LoggerFactory.getLogger(MOD_ID)

// region GLSL / 配置片段（对 Chocapic13/Bliss 的修改，原署名保留）
// 这些片段原本作为字符串内联在 kotlin 文件中；现已拆分为 resources/polychrome_it/ 下的
// 独立文件，运行时由类加载器读取，便于单独编辑/语法高亮。
//   polychrome_it.glsl        -> 写入派生包 shaders/lib/polychrome_it.glsl（含 __SIZE_X__/__SIZE_Z__ 占位符）
//   settings_injection.glsl   -> 注入 settings.glsl 的诊断开关
//   call_injection.glsl       -> 注入 all_solid.fsh 的调用
//   dh_call_injection.glsl    -> 注入 DH_solid.fsh 的调用
//   properties_addition.properties -> 追加进 shaders.properties 的 sampler 注册
//   en_us.lang / zh_cn.lang   -> 追加进派生包 lang 文件的显示名

/** 从打包进 mod 的资源目录（resources/polychrome_it/）读取注入片段，统一去除尾部换行。 */
private object PatchResources {
    fun load(name: String): String =
        PatchResources::class.java.getResourceAsStream("/assets/polychrome_it/patches/$name")!!
            .bufferedReader().use { it.readText() }
            .trimEnd('\n')
}
// endregion

/** 客户端初始化时调用：每次启动都（重新）生成派生光影包。 */
fun generateIfNeeded() {
    val shaderpacks = gameDir.resolve("shaderpacks")
    if (!shaderpacks.exists()) {
        logger.warn("未找到 shaderpacks/ 目录，跳过派生包生成。")
        return
    }
    val bliss = findBliss(shaderpacks)
    if (bliss == null) {
        logger.warn("未在 shaderpacks/ 中找到 Bliss，跳过。请先把 Bliss 放进 shaderpacks/ 后重启游戏。")
        return
    }
    val derived = shaderpacks.resolve(DERIVED_NAME)
    val cfg = PolychromeItConfig.load()
    generate(bliss, derived, cfg)
}

private fun generate(bliss: Path, derived: Path, cfg: PolychromeItConfig) {
    logger.info("开始生成派生光影包：$DERIVED_NAME（基于 ${bliss.fileName}）")
    if (derived.exists()) FileUtils.deleteDirectory(derived.toFile())
    try {
        FileUtils.copyDirectory(bliss.toFile(), derived.toFile())
        injectShaders(derived, cfg)
        packWorldTextures(derived)
        logger.info("派生包生成完成：$derived 。请选择光影 “$DERIVED_NAME”，并在光影设置里切换 Polychrome It! 调试开关。")
    } catch (e: Exception) {
        logger.error("生成失败：${e.message}")
        e.printStackTrace()
    }
}

private fun findBliss(shaderpacks: Path): Path? {
    val list = runCatching {
        Files.list(shaderpacks).collect(Collectors.toList())
    }.getOrNull() ?: return null
    return list.firstOrNull {
        it.name.contains("bliss", ignoreCase = true) // 名字含 "Bliss"
            && it.fileName.toString() != DERIVED_NAME // 排除我们自己的派生包（名字也含 "Bliss"）
            && Files.isDirectory(it) // 只支持文件夹形式的 Bliss
    }
}

// region 着色器注入
private fun injectShaders(derived: Path, cfg: PolychromeItConfig) {
    val props = derived.resolve("shaders/shaders.properties")
    if (props.exists()) {
        var text = Files.readString(props)
        val idx = text.indexOf("iris.features.optional")
        if (idx >= 0) {
            val lineEnd = text.indexOf('\n', idx)
            text = text.substring(0, lineEnd + 1) + PatchResources.load("properties_addition.properties") + "\n" + text.substring(lineEnd + 1)
            // 把诊断开关放到 Misc/Debugging 设置页，紧挨 Bliss 自带 DEBUG_VIEW 之后
            text = text.replace("DEBUG_VIEW [the_end_orb]", "DEBUG_VIEW POLYCHROME_DEBUG [the_end_orb]")
            Files.writeString(props, text)
        } else {
            logger.warn("shaders.properties 未找到 iris.features.optional，跳过 sampler 注册。")
        }
    } else {
        logger.warn("未找到 shaders.properties，跳过。")
    }

    // 全精度地形（gbuffers_terrain）与 DH LOD 地形（dh_terrain -> DH_solid）都要注入，
    // 否则 DH 远景 LOD 区块里我们的方块仍是原始品红染色。
    injectIntoFsh(
        derived.resolve("shaders/dimensions/all_solid.fsh"),
        includeAnchor = "#include \"/lib/items.glsl\"",
        callBefore = "vec4 data1 = clamp( encode(viewToWorld(normal), PackLightmaps), 0.0, 1.0);",
        callText = PatchResources.load("call_injection.glsl")
    )
    injectIntoFsh(
        derived.resolve("shaders/dimensions/DH_solid.fsh"),
        includeAnchor = "#include \"/lib/settings.glsl\"",
        callBefore = "gl_FragData[0] = vec4(encodeVec2(Albedo.x,data1.x),",
        callText = PatchResources.load("dh_call_injection.glsl")
    )

    val libFile = derived.resolve("shaders/lib/polychrome_it.glsl")
    Files.createDirectories(libFile.parent)
    Files.writeString(
        libFile,
        PatchResources.load("polychrome_it.glsl")
            .replace("__SIZE_X__", "${cfg.sizeX}f")
            .replace("__SIZE_Z__", "${cfg.sizeZ}f")
    )

    // 诊断开关：注入 settings.glsl（让 Iris 在光影设置里显示可切换项），并补全 lang 显示名。
    injectAfterLine(derived.resolve("shaders/lib/settings.glsl"), "#define DEBUG_VIEW debug_OFF", PatchResources.load("settings_injection.glsl"))
    appendToFile(derived.resolve("shaders/lang/en_us.lang"), PatchResources.load("en_us.lang"))
    appendToFile(derived.resolve("shaders/lang/zh_cn.lang"), PatchResources.load("zh_cn.lang"))
}

/** 在第一个包含 anchor 的行之后插入 text（整段）。 */
private fun injectAfterLine(file: Path, anchor: String, text: String) {
    if (!file.exists()) {
        logger.warn("未找到 ${file.fileName}，跳过注入。")
        return
    }
    val content = Files.readString(file)
    val idx = content.indexOf(anchor)
    if (idx < 0) {
        logger.warn("${file.fileName} 未找到锚点 '$anchor'，跳过诊断开关注入。")
        return
    }
    val lineEnd = content.indexOf('\n', idx)
    val insertAt = if (lineEnd < 0) content.length else lineEnd + 1
    Files.writeString(file, content.substring(0, insertAt) + text + "\n" + content.substring(insertAt))
}

/** 在文件末尾追加 text（派生包每次生成都会重建，无需去重）。 */
private fun appendToFile(file: Path, text: String) {
    if (!file.exists()) {
        logger.warn("未找到 ${file.fileName}，跳过 lang 注入。")
        return
    }
    val content = Files.readString(file)
    val sep = if (content.endsWith("\n")) "" else "\n"
    Files.writeString(file, content + sep + text + "\n")
}

private fun injectIntoFsh(fsh: Path, includeAnchor: String, callBefore: String, callText: String) {
    if (!fsh.exists()) {
        logger.warn("未找到 ${fsh.fileName}，跳过 GLSL 注入。")
        return
    }
    var text = Files.readString(fsh)
    if (text.contains(includeAnchor)) {
        text = text.replaceFirst(includeAnchor, "$includeAnchor\n#include \"/lib/polychrome_it.glsl\"")
    } else {
        logger.warn("${fsh.fileName} 未找到 include 锚点，跳过 include 注入。")
        return
    }
    val k = text.indexOf(callBefore)
    if (k >= 0) {
        text = text.substring(0, k) + callText + "\n" + text.substring(k)
    } else {
        logger.warn("${fsh.fileName} 未找到调用锚点，跳过调用注入。")
        return
    }
    Files.writeString(fsh, text)
}
// endregion

// region 世界纹理打包（labPBR：diffuse / normal / specular.R=粗糙度）
private fun packWorldTextures(derived: Path) {
    val worldDir = derived.resolve("shaders/texture/world")
    Files.createDirectories(worldDir)
    val d = PolychromeItConfig.diffuseFile
    val n = PolychromeItConfig.normalFile
    val r = PolychromeItConfig.roughnessFile
    if (!d.exists() || !n.exists() || !r.exists()) {
        logger.error(
            "缺少世界纹理图（diffuse/normal/roughness.png），跳过纹理打包。请把 WM 导出的图片放到 ${PolychromeItConfig.dir}。"
        )
        return
    }
    // diffuse / normal 直接复制（无损）
    Files.copy(d, worldDir.resolve("diffuse.png"), StandardCopyOption.REPLACE_EXISTING)
    Files.copy(n, worldDir.resolve("normal.png"), StandardCopyOption.REPLACE_EXISTING)

    // specular：R=粗糙度，G=255（无 AO 压暗），B=0
    val rough = ImageIO.read(File(r.toString()))
    val spec = BufferedImage(rough.width, rough.height, BufferedImage.TYPE_INT_RGB)
    for (y in 0 until rough.height) {
        for (x in 0 until rough.width) {
            val rgb = rough.getRGB(x, y)
            val rr = (rgb shr 16) and 0xFF
            val gg = (rgb shr 8) and 0xFF
            val bb = rgb and 0xFF
            val roughVal = if (rr == gg && gg == bb) rr else (rr + gg + bb) / 3
            spec.setRGB(x, y, (roughVal shl 16) or (255 shl 8) or 0)
        }
    }
    ImageIO.write(spec, "png", File(worldDir.resolve("specular.png").toString()))

    val mcmeta = "{\"texture\":{\"blur\":false,\"clamp\":true}}"
    for (name in listOf("diffuse", "normal", "specular")) {
        Files.writeString(worldDir.resolve("$name.png.mcmeta"), mcmeta)
    }
    logger.info("世界纹理打包完成：$worldDir")
}
// endregion

