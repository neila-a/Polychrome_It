package top.neila.polychrome_it

import com.mojang.brigadier.Command
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.commands.Commands
import net.minecraft.commands.synchronization.SingletonArgumentInfo
import net.minecraft.resources.Identifier
import net.minecraft.server.permissions.Permissions
import java.nio.file.Path

object PolychromeCommands {

    fun register() {
        // 自定义参数类型必须在服务端与客户端都注册（模组为 "*" 环境，两端均会执行 onInitialize），
        // 否则客户端无法反序列化命令树。fabric-registry-sync-v0 会延迟内置注册表冻结，
        // 因此这里可以在 onInitialize 阶段注册。
        ArgumentTypeRegistry.registerArgumentType(
            Identifier.fromNamespaceAndPath("polychrome_it", "image_path"),
            ImagePathArgumentType::class.java,
            SingletonArgumentInfo.contextFree(::ImagePathArgumentType)
        )

        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            dispatcher.register(
                Commands.literal("polychrome:setimagepath")
                    .requires { source ->
                        source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR)
                    }
                    .then(
                        Commands.argument("path", ImagePathArgumentType())
                            .executes { context ->
                                val path = context.getArgument("path", Path::class.java)
                                val server = context.source.server
                                ImagePathSavedData.get(server).updateImagePath(path.toString())
                                Command.SINGLE_SUCCESS
                            }
                    )
            )
        }
    }
}
