package top.neila.polychrome_it.client

import com.mojang.brigadier.Command
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import java.nio.file.Path

/**
 * 注册客户端指令。
 *
 * 图像路径的存储已从服务器存档移至客户端本地，指令也随之改为客户端指令：
 * 完全在本地执行，单人游戏与多人游戏中均可使用，且不需要任何权限。
 *
 * 客户端指令只存在于本地分发器中，不会随服务端指令树同步，
 * 因此自定义参数类型无需（也不应）再注册到 ArgumentTypeRegistry。
 */
fun registerClientCommands() {
    ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
        dispatcher.register(
            ClientCommands.literal("polychrome:setimagepath")
                .then(
                    ClientCommands.argument("path", ImagePathArgumentType())
                        .executes { context ->
                            val path = context.getArgument("path", Path::class.java)
                            ImagePathClientStore.setImagePath(path.toString())
                            Command.SINGLE_SUCCESS
                        }
                )
        )
    }
}
