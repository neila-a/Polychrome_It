package top.neila.polychrome_it

import net.fabricmc.api.ClientModInitializer
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer
import me.shedaniel.autoconfig.AutoConfig
import me.shedaniel.autoconfig.ConfigHolder
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry.register
import net.minecraft.world.InteractionResult
import top.neila.polychrome_it.shaderpackpatcher.generatePatchedShaderpack
import kotlin.concurrent.thread

/**
 * 客户端专用入口点。
 *
 * 着色注册（BlockColorRegistry / BlockTintSource）只能发生在客户端：
 * 这些是客户端类，在专用服务器上不可用，若在 common 入口点引用会导致服务器启动崩溃。
 */
object PolychromeItClient : ClientModInitializer {
    val registeredConfig: ConfigHolder<PolychromeItConfig> = AutoConfig.register(PolychromeItConfig::class.java, ::GsonConfigSerializer).apply {
        registerSaveListener { _, config ->
            if (config.blurTexture != lastBlurTexture) {
                lastBlurTexture = config.blurTexture
                generatePatchedShaderpack()
            }
            InteractionResult.SUCCESS
        }
        registerLoadListener { _, config ->
            lastBlurTexture = config.blurTexture
            generatePatchedShaderpack()
            InteractionResult.SUCCESS
        }
        load()
    }
    var lastBlurTexture: Boolean? = null

    override fun onInitializeClient() = PolychromeItBlock.entries.forEach {
        register(
            listOf(DerivedBlockTintSource(it)),
            it.registered
        )
    }
}
