package top.neila.polychrome_it

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.commands.Commands
import net.minecraft.server.permissions.Permissions

object PolychromeCommands {
    fun register() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            dispatcher.register(
                Commands.literal("polychrome:setimagepath")
                    .requires { source ->
                        source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR)
                    }
                    .then(
                        Commands.argument("path", StringArgumentType.greedyString())
                            .executes { context ->
                                val path = StringArgumentType.getString(context, "path")
                                val server = context.source.server
                                ImagePathSavedData.get(server).updateImagePath(path)
                                Command.SINGLE_SUCCESS
                            }
                    )
            )
        }
    }
}
