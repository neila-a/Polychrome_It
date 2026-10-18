package top.neila.polychrome_it

import net.fabricmc.api.ModInitializer

class PolychromeIt : ModInitializer {
    override fun onInitialize() {
        PolychromeBlocks.init()
        PolychromeCommands.register()
    }
}
