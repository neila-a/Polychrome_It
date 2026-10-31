package top.neila.polychrome_it

import net.irisshaders.iris.api.v0.IrisApi
import java.lang.NoClassDefFoundError

@Volatile
private var cachedInUse: Boolean? = null
private var lastCheck = 0L

/**
 * 探测 Iris 当前是否启用了光影（isShaderPackInUse）。
 *
 * 洋红只是给着色器的“这是我们的方块”识别色；只要启用了任意光影（本 mod 的派生包），
 * 就返回洋红，否则回落到 PolychromeBlock.tint 的真实染色，避免方块直接显示成洋红。
 *
 * Iris 是可选依赖：未安装 Iris 时，引用 IrisApi 的类解析会抛 NoClassDefFoundError，
 * 被这里捕获后视为“未启用光影”，安全回落。Iris 由用户运行时自行安装，不随本 mod 打包。
 * 当前是否启用了光影。结果带 2 秒缓存，避免区块重建时逐方块调用；切换光影后最多延迟 2 秒生效。
 */
fun isShaderPackInUse(): Boolean {
    val now = System.currentTimeMillis()
    if (cachedInUse != null && now - lastCheck < 2000) return cachedInUse!!
    lastCheck = now
    return try {
        IrisApi.getInstance().isShaderPackInUse
    } catch (_: NoClassDefFoundError) {
        false
    }
}
