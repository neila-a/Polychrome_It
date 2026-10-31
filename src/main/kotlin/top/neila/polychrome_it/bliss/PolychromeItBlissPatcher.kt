package top.neila.polychrome_it.bliss

import net.fabricmc.loader.api.FabricLoader
import top.neila.polychrome_it.config.PolychromeItConfig
import java.awt.image.BufferedImage
import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import javax.imageio.ImageIO
import java.io.File
import kotlin.io.path.exists
import kotlin.io.path.name

/**
 * Polychrome It! 轻量版注入器。
 *
 * 不修改 Bliss 的任何文件：把 Bliss 复制成 "Bliss + Polychrome It" 派生光影包，
 * 在副本里注入世界空间 PBR 的 GLSL 与世界纹理。这是对 Chocapic13/Bliss 的修改，
 * 原始光影包保持字节不变、署名保留。做法参考 Euphoria Patches（派生包而非改原包）。
 */
object PolychromeItBlissPatcher {

    private const val DERIVED_NAME = "Bliss + Polychrome It"
    private const val MARKER = ".polychrome_it_marker"
    private const val PATCH_VERSION = 2 // 改注入逻辑时 +1，强制重生派生包（避免用到旧包）
    private val gameDir: Path get() = FabricLoader.getInstance().gameDir

    private fun log(msg: String) = println("[Polychrome It!] $msg")

    // region GLSL 片段（对 Chocapic13/Bliss 的修改，原署名保留）
    // 识别 image_pixel_block 改用顶点色染色（见 glslLib）：把方块染色设为品红，着色器用
    // “红≈蓝、绿≈0”的比例判定（面遮蔽/AO 缩放下比例不变，比精确比对稳健）。
    // 之所以不用 mc_Entity.x：Iris 的 blockStateIds 只认 block.properties，而对 mod 方块
    // 解析返回 AIR（实测 mc_Entity.x = -1），无法用方块 id 区分。

    private fun glslLib(c: PolychromeItConfig): String {
        // 诊断分支：off=真实 PBR（不插入）；detect=隔离识别；normal/specular=把对应通道画成颜色/灰度。
        val diag = when (c.debug) {
            "detect" -> """
        // 隔离诊断（detect）：洋红=命中我们的方块；青色=其它方块；黄色=color 近乎全黑
        if (isOurBlock)       albedo.rgb = vec3(1.0, 0.0, 1.0);
        else if (cmax < 0.02) albedo.rgb = vec3(1.0, 1.0, 0.0);
        else                  albedo.rgb = vec3(0.0, 1.0, 1.0);
        return;
"""
            "normal" -> """
        // 通道可视化（normal）：把世界法线编码成 RGB（[-1,1] -> [0,1]），确认法线图被采样
        if (isOurBlock) {
            vec2 uv = (worldpos.xz - vec2(${c.originX}f, ${c.originZ}f)) / vec2(${c.sizeX}f, ${c.sizeZ}f);
            if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) albedo.rgb = vec3(0.0);
            else {
                vec3 wn = texture2D(polychromeNormal, uv).xyz * 2.0 - 1.0;
                albedo.rgb = wn * 0.5 + 0.5;
            }
            return;
        }
"""
            "specular" -> """
        // 通道可视化（specular）：把粗糙度输出成灰度，确认 specular 图被采样
        if (isOurBlock) {
            vec2 uv = (worldpos.xz - vec2(${c.originX}f, ${c.originZ}f)) / vec2(${c.sizeX}f, ${c.sizeZ}f);
            if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) albedo.rgb = vec3(0.0);
            else albedo.rgb = vec3(texture2D(polychromeSpecular, uv).r);
            return;
        }
"""
            else -> "" // off：真实 PBR，不插入诊断分支
        }
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
$diag
    if (!isOurBlock) return;
    vec2 uv = (worldpos.xz - vec2(${c.originX}f, ${c.originZ}f)) / vec2(${c.sizeX}f, ${c.sizeZ}f);
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

    private val propertiesAddition = """
# Polychrome It! — 世界空间 PBR（对 Chocapic13/Bliss 的修改，原署名保留）
# 用 customTexture.<sampler> 注册自定义纹理（直接按名绑给 terrain 程序，绕开 texture.<stage> 拦截器）。
customTexture.polychromeDiffuse   = texture/world/rocks_diffuse.png
customTexture.polychromeNormal   = texture/world/rocks_normal.png
customTexture.polychromeSpecular = texture/world/rocks_specular.png
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

    /** 客户端初始化时调用：若派生包缺失/过期则（重新）生成。 */
    fun generateIfNeeded() {
        val shaderpacks = gameDir.resolve("shaderpacks")
        if (!shaderpacks.exists()) {
            log("未找到 shaderpacks/ 目录，跳过派生包生成。")
            return
        }
        val bliss = findBliss(shaderpacks)
        if (bliss == null) {
            log("未在 shaderpacks/ 中找到 Bliss，跳过。请先把 Bliss 放进 shaderpacks/ 后重启游戏。")
            return
        }
        val derived = shaderpacks.resolve(DERIVED_NAME)
        val cfg = PolychromeItConfig.load()
        if (derived.exists() && markerValid(derived, bliss, cfg)) {
            log("派生包已是最新，跳过生成：$DERIVED_NAME")
            return
        }
        generate(bliss, derived, cfg)
    }

    /** 强制重新生成（供未来命令/调试调用）。 */
    fun generate(force: Boolean = false) {
        val shaderpacks = gameDir.resolve("shaderpacks")
        val bliss = findBliss(shaderpacks) ?: run { log("未找到 Bliss。"); return }
        generate(bliss, shaderpacks.resolve(DERIVED_NAME), PolychromeItConfig.load())
    }

    private fun generate(bliss: Path, derived: Path, cfg: PolychromeItConfig) {
        log("开始生成派生光影包：$DERIVED_NAME（基于 ${bliss.fileName}）")
        if (derived.exists()) deleteRecursively(derived)
        try {
            copyPack(bliss, derived)
            injectShaders(derived, cfg)
            packWorldTextures(derived)
            writeMarker(derived, bliss, cfg)
            log("派生包生成完成：$derived 。请在视频设置里选择 “$DERIVED_NAME”。")
        } catch (e: Exception) {
            log("生成失败：${e.message}")
            e.printStackTrace()
        }
    }

    private fun findBliss(shaderpacks: Path): Path? {
        val list = runCatching {
            Files.list(shaderpacks).collect(java.util.stream.Collectors.toList())
        }.getOrNull() ?: return null
        return list
            .filter { it.name.contains("bliss", ignoreCase = true) }
            .filter { it.fileName.toString() != DERIVED_NAME } // 排除我们自己的派生包（名字也含 "Bliss"）
            .filter { Files.isDirectory(it) || isZip(it) } // 排除 .txt 等非光影包文件
            .minByOrNull { if (Files.isDirectory(it)) 0 else 1 } // 优先目录而非 zip
    }

    /** 按文件头（PK\x03\x04）识别 zip，兼容 .zip / .zip.txt 等任意后缀。 */
    private fun isZip(path: Path): Boolean {
        if (!Files.isRegularFile(path)) return false
        return runCatching {
            Files.newInputStream(path).use { ins ->
                val head = ByteArray(4)
                val n = ins.read(head)
                n == 4 && head[0] == 0x50.toByte() && head[1] == 0x4B.toByte()
                        && head[2] == 0x03.toByte() && head[3] == 0x04.toByte()
            }
        }.getOrDefault(false)
    }

    // region 文件复制
    private fun copyPack(src: Path, dst: Path) {
        when {
            Files.isDirectory(src) -> copyDirectory(src, dst)
            isZip(src) ->
                FileSystems.newFileSystem(src, null as ClassLoader?).use { fs -> copyDirectory(fs.getPath("/"), dst) }
            else -> throw IllegalArgumentException("不支持的 Bliss 包格式（既不是目录也不是 zip）：$src")
        }
    }

    private fun copyDirectory(src: Path, dst: Path) {
        Files.walkFileTree(src, object : SimpleFileVisitor<Path>() {
            override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                Files.createDirectories(dst.resolve(src.relativize(dir).toString()))
                return FileVisitResult.CONTINUE
            }

            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                Files.copy(file, dst.resolve(src.relativize(file).toString()), StandardCopyOption.REPLACE_EXISTING)
                return FileVisitResult.CONTINUE
            }
        })
    }

    private fun deleteRecursively(dir: Path) {
        if (!Files.exists(dir)) return
        if (Files.isDirectory(dir)) {
            Files.walkFileTree(dir, object : SimpleFileVisitor<Path>() {
                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    Files.delete(file)
                    return FileVisitResult.CONTINUE
                }

                override fun postVisitDirectory(d: Path, exc: IOException?): FileVisitResult {
                    Files.delete(d)
                    return FileVisitResult.CONTINUE
                }
            })
        } else {
            Files.deleteIfExists(dir)
        }
    }
    // endregion

    // region 着色器注入
    private fun injectShaders(derived: Path, cfg: PolychromeItConfig) {
        val props = derived.resolve("shaders/shaders.properties")
        if (props.exists()) {
            var text = Files.readString(props)
            val idx = text.indexOf("iris.features.optional")
            if (idx >= 0) {
                val lineEnd = text.indexOf('\n', idx)
                text = text.substring(0, lineEnd + 1) + propertiesAddition + "\n" + text.substring(lineEnd + 1)
                Files.writeString(props, text)
            } else {
                log("shaders.properties 未找到 iris.features.optional，跳过 sampler 注册。")
            }
        } else {
            log("未找到 shaders.properties，跳过。")
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
    }

    private fun injectIntoFsh(fsh: Path, includeAnchor: String, callBefore: String, callText: String) {
        if (!fsh.exists()) {
            log("未找到 ${fsh.fileName}，跳过 GLSL 注入。")
            return
        }
        var text = Files.readString(fsh)
        if (text.contains(includeAnchor)) {
            text = text.replaceFirst(includeAnchor, "$includeAnchor\n#include \"/lib/polychrome_it.glsl\"")
        } else {
            log("${fsh.fileName} 未找到 include 锚点，跳过 include 注入。")
            return
        }
        val k = text.indexOf(callBefore)
        if (k >= 0) {
            text = text.substring(0, k) + callText + "\n" + text.substring(k)
        } else {
            log("${fsh.fileName} 未找到调用锚点，跳过调用注入。")
            return
        }
        Files.writeString(fsh, text)
    }
    // endregion

    // region 世界纹理打包（labPBR：diffuse / normal / specular.R=粗糙度）
    private fun packWorldTextures(derived: Path) {
        val worldDir = derived.resolve("shaders/texture/world")
        Files.createDirectories(worldDir)
        val cfg = PolychromeItConfig
        val d = cfg.diffuseFile
        val n = cfg.normalFile
        val r = cfg.roughnessFile
        if (!d.exists() || !n.exists() || !r.exists()) {
            log("缺少世界纹理图（diffuse/normal/roughness.png），跳过纹理打包。请把 WM 导出的图片放到 ${cfg.dir}。")
            return
        }
        // diffuse / normal 直接复制（无损）
        Files.copy(d, worldDir.resolve("rocks_diffuse.png"), StandardCopyOption.REPLACE_EXISTING)
        Files.copy(n, worldDir.resolve("rocks_normal.png"), StandardCopyOption.REPLACE_EXISTING)

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
        ImageIO.write(spec, "png", File(worldDir.resolve("rocks_specular.png").toString()))

        val mcmeta = "{\"texture\":{\"blur\":false,\"clamp\":true}}"
        for (name in listOf("rocks_diffuse", "rocks_normal", "rocks_specular")) {
            Files.writeString(worldDir.resolve("$name.png.mcmeta"), mcmeta)
        }
        log("世界纹理打包完成：$worldDir")
    }
    // endregion

    // region 增量标记
    private fun markerContent(bliss: Path, cfg: PolychromeItConfig): String {
        val lastMod = runCatching { Files.getLastModifiedTime(bliss).toMillis() }.getOrElse { 0L }
        val imgHash = listOf(
            PolychromeItConfig.diffuseFile,
            PolychromeItConfig.normalFile,
            PolychromeItConfig.roughnessFile,
            PolychromeItConfig.heightFile,
            PolychromeItConfig.configFile
        )
            .joinToString("|") { f ->
                "${f.name}:${runCatching { Files.getLastModifiedTime(f).toMillis() }.getOrElse { -1L }}"
            }
        // 把 debug 开关与补丁版本纳入标记：切开关/改注入逻辑时强制重生派生包
        return "${bliss.fileName}|$lastMod|$imgHash|${cfg.debug}|$PATCH_VERSION"
    }

    private fun markerValid(derived: Path, bliss: Path, cfg: PolychromeItConfig): Boolean {
        val marker = derived.resolve(MARKER)
        if (!marker.exists()) return false
        val content = runCatching { Files.readString(marker) }.getOrNull() ?: return false
        return content == markerContent(bliss, cfg)
    }

    private fun writeMarker(derived: Path, bliss: Path, cfg: PolychromeItConfig) {
        Files.writeString(derived.resolve(MARKER), markerContent(bliss, cfg))
    }
    // endregion
}
