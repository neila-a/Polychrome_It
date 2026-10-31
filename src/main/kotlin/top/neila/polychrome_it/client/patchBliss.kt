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

// region GLSL 片段（对 Chocapic13/Bliss 的修改，原署名保留）
// 识别 image_pixel_block 改用顶点色染色（见 glslLib）：把方块染色设为品红，着色器用
// “红≈蓝、绿≈0”的比例判定（面遮蔽/AO 缩放下比例不变，比精确比对稳健）。
// 之所以不用 mc_Entity.x：Iris 的 blockStateIds 只认 block.properties，而对 mod 方块
// 解析返回 AIR（实测 mc_Entity.x = -1），无法用方块 id 区分。

private fun glslLib(c: PolychromeItConfig): String {
    // 诊断分支改由运行时光影设置 POLYCHROME_DEBUG 决定（编译期 #if 选择）。
    //   detect   = 隔离识别（洋红/青/黄）
    //   normal   = 把世界法线画成 RGB
    //   specular = 把粗糙度画成灰度
    // off（默认）不插入任何诊断分支，走真实 PBR。
    return """
// Polychrome It! — 世界空间 PBR 覆盖（对 Chocapic13/Bliss 的修改，原署名保留）
uniform sampler2D polychromeDiffuse;
uniform sampler2D polychromeNormal;
uniform sampler2D polychromeSpecular;
// 复用 Bliss 已定义的 worldToView（内部用 gbufferModelView），避免重声明/未声明冲突
vec3 worldToView(vec3 worldPos);

// 识别我们的方块 image_pixel_block：其染色（顶点色 color varying）被设为品红 (1,0,1)。
// 面遮蔽 / AO 会等比例缩放 color，但 r≈b、g≈0 的比例关系保持不变，故用比例判定而非精确值，更稳健。
void applyPolychromeIt(inout vec4 albedo, inout vec3 normal, inout float roughness, vec3 worldpos, vec4 blockColor) {
    float cmax = max(max(blockColor.r, blockColor.g), blockColor.b);
    bool isOurBlock = (cmax > 0.02)
        && (blockColor.g < 0.25 * cmax)
        && (abs(blockColor.r - blockColor.b) < 0.25 * cmax);
#if POLYCHROME_DEBUG == POLYCHROME_DETECT
    // 隔离诊断（detect）：洋红=命中我们的方块；青色=其它方块；黄色=color 近乎全黑
    if (isOurBlock)       albedo.rgb = vec3(1.0, 0.0, 1.0);
    else if (cmax < 0.02) albedo.rgb = vec3(1.0, 1.0, 0.0);
    else                  albedo.rgb = vec3(0.0, 1.0, 1.0);
    return;
#elif POLYCHROME_DEBUG == POLYCHROME_NORMAL
    // 通道可视化（normal）：把世界法线编码成 RGB（[-1,1] -> [0,1]），确认法线图被采样
    if (isOurBlock) {
        vec2 uv = worldpos.xz / vec2(${c.sizeX}f, ${c.sizeZ}f);
        if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) albedo.rgb = vec3(0.0);
        else {
            vec3 wn = texture2D(polychromeNormal, uv).xyz * 2.0 - 1.0;
            albedo.rgb = wn * 0.5 + 0.5;
        }
        return;
    }
#elif POLYCHROME_DEBUG == POLYCHROME_SPECULAR
    // 通道可视化（specular）：把粗糙度输出成灰度，确认 specular 图被采样
    if (isOurBlock) {
        vec2 uv = worldpos.xz / vec2(${c.sizeX}f, ${c.sizeZ}f);
        if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) albedo.rgb = vec3(0.0);
        else albedo.rgb = vec3(texture2D(polychromeSpecular, uv).r);
        return;
    }
#endif
    if (!isOurBlock) return;
    vec2 uv = worldpos.xz / vec2(${c.sizeX}f, ${c.sizeZ}f);
    if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) {
        // UV 越界：撤销品红识别染色（Bliss 在计算 Albedo 时已把顶点色乘进去），
        // 还原为白方块 × 面遮蔽/AO（识别染色是 (1,0,1)，blockColor.r 即缩放系数）。
        // 否则范围外的方块会露出大片品红，DH 的 LOD 顶点色也可能被误检放大。
        albedo.rgb = vec3(blockColor.r);
        return;
    }

    albedo.rgb = texture2D(polychromeDiffuse, uv).rgb;
    vec3 worldNormal = texture2D(polychromeNormal, uv).xyz * 2.0 - 1.0;
    // 退化保护：法线图平坦区若编码为 (0.5,0.5,0.5) 会得到 (0,0,0)，normalize 会出 NaN 让整块变黑
    if (dot(worldNormal, worldNormal) < 1e-4) worldNormal = vec3(0.0, 1.0, 0.0);
    vec3 viewNormal = worldToView(worldNormal);
    if (dot(viewNormal, viewNormal) > 1e-4) {
        normal = normalize(viewNormal); // 变换后仍退化则保留 Bliss 原法线，避免 NaN 传播
    }
    roughness = texture2D(polychromeSpecular, uv).r;
}
""".trimIndent()
}

// 注入 settings.glsl 的诊断开关：Iris 自动把它识别为可切换的光影设置项（// [states] 注解）。
// 各状态名对应一个整数宏，shader 里用 #if POLYCHROME_DEBUG == POLYCHROME_xxx 选择分支。
private val settingsInjection = """
// Polychrome It! — 诊断模式开关（由光影设置控制，编译期 #if 选择分支；无需改动 config）
#define POLYCHROME_OFF 0
#define POLYCHROME_DETECT 1
#define POLYCHROME_NORMAL 2
#define POLYCHROME_SPECULAR 3
#define POLYCHROME_DEBUG POLYCHROME_OFF // [POLYCHROME_OFF POLYCHROME_DETECT POLYCHROME_NORMAL POLYCHROME_SPECULAR]
""".trimIndent()

// 光影设置界面里该选项的显示名与各个状态名（追加进派生包的 lang 文件）。
private val langInjectionEnUs = """
option.POLYCHROME_DEBUG = Polychrome It! Debug
    value.POLYCHROME_DEBUG.POLYCHROME_OFF = Off
    value.POLYCHROME_DEBUG.POLYCHROME_DETECT = Detect
    value.POLYCHROME_DEBUG.POLYCHROME_NORMAL = Normal
    value.POLYCHROME_DEBUG.POLYCHROME_SPECULAR = Specular
""".trimIndent()

private val langInjectionZhCn = """
option.POLYCHROME_DEBUG = Polychrome It! 调试
    value.POLYCHROME_DEBUG.POLYCHROME_OFF = 关闭
    value.POLYCHROME_DEBUG.POLYCHROME_DETECT = 识别隔离
    value.POLYCHROME_DEBUG.POLYCHROME_NORMAL = 法线可视化
    value.POLYCHROME_DEBUG.POLYCHROME_SPECULAR = 粗糙度可视化
""".trimIndent()

private val propertiesAddition = """
# Polychrome It! — 世界空间 PBR（对 Chocapic13/Bliss 的修改，原署名保留）
# 用 customTexture.<sampler> 注册自定义纹理（直接按名绑给 terrain 程序，绕开 texture.<stage> 拦截器）。
customTexture.polychromeDiffuse   = texture/world/diffuse.png
customTexture.polychromeNormal   = texture/world/normal.png
customTexture.polychromeSpecular = texture/world/specular.png
""".trimIndent()

private val callInjection = """
    // Polychrome It! — 世界空间 PBR 覆盖（对 Chocapic13/Bliss 的修改，原署名保留）
    {
        float pcRoughness = gl_FragData[1].r;
        applyPolychromeIt(Albedo, normal, pcRoughness, worldpos, color);
        gl_FragData[1].r = pcRoughness;
    }
""".trimIndent()

// DH LOD 地形程序（dimensions/DH_solid.fsh）的调用：顶点色叫 gcolor，世界坐标 = localPos + cameraPosition。
// DH 下 normals 已在注入点之前编码进 data1，故这里只覆盖 albedo（解决 LOD 区块露品红的主因）；
// roughness 在 DH 下无对应写入通道，pcRoughness 仅占位、不生效。
private val dhCallInjection = """
    // Polychrome It! — DH LOD 世界空间 PBR 覆盖（对 Chocapic13/Bliss 的修改，原署名保留）
    {
        vec3 pcWorldPos = localPos.xyz + cameraPosition;
        float pcRoughness = 0.0;
        applyPolychromeIt(Albedo, normals, pcRoughness, pcWorldPos, gcolor);
    }
""".trimIndent()
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
            text = text.substring(0, lineEnd + 1) + propertiesAddition + "\n" + text.substring(lineEnd + 1)
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
        callText = callInjection
    )
    injectIntoFsh(
        derived.resolve("shaders/dimensions/DH_solid.fsh"),
        includeAnchor = "#include \"/lib/settings.glsl\"",
        callBefore = "gl_FragData[0] = vec4(encodeVec2(Albedo.x,data1.x),",
        callText = dhCallInjection
    )

    val libFile = derived.resolve("shaders/lib/polychrome_it.glsl")
    Files.createDirectories(libFile.parent)
    Files.writeString(libFile, glslLib(cfg))

    // 诊断开关：注入 settings.glsl（让 Iris 在光影设置里显示可切换项），并补全 lang 显示名。
    injectAfterLine(derived.resolve("shaders/lib/settings.glsl"), "#define DEBUG_VIEW debug_OFF", settingsInjection)
    appendToFile(derived.resolve("shaders/lang/en_us.lang"), langInjectionEnUs)
    appendToFile(derived.resolve("shaders/lang/zh_cn.lang"), langInjectionZhCn)
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

