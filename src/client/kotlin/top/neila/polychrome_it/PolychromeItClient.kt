package top.neila.polychrome_it

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry.register
import top.neila.polychrome_it.shaderpackpatcher.generatePatchedShaderpack
import kotlin.system.measureTimeMillis

/**
 * 客户端专用入口点。
 *
 * 着色注册（BlockColorRegistry / BlockTintSource）只能发生在客户端：
 * 这些是客户端类，在专用服务器上不可用，若在 common 入口点引用会导致服务器启动崩溃。
 */
object PolychromeItClient : ClientModInitializer {
    override fun onInitializeClient() {
        for (block in PolychromeItBlock.entries) {
            register(
                listOf(DerivedBlockTintSource(block)),
                block.registered
            )
        }

        // 轻量版：生成 "Bliss + Polychrome It" 派生光影包（不修改 Bliss 原文件）
        Thread {
            // 会花费约800ms，所以应创建线程。
            generatePatchedShaderpack()
        }.apply {
            name = "PolychromeIt shaderpack generator"
            start()
        }

    }
}
