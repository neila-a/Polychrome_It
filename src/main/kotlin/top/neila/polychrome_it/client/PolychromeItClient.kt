package top.neila.polychrome_it.client

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry
import top.neila.polychrome_it.PolychromeItBlock

/**
 * 客户端专用入口点。
 *
 * 着色注册（BlockColorRegistry / BlockTintSource）只能发生在客户端：
 * 这些是客户端类，在专用服务器上不可用，若在 common 入口点引用会导致服务器启动崩溃。
 */
class PolychromeItClient : ClientModInitializer {
    override fun onInitializeClient() {
        for (block in PolychromeItBlock.entries) {
            BlockColorRegistry.register(
                listOf(DerivedBlockTintSource(block)),
                block.registered
            )
        }
    }
}
