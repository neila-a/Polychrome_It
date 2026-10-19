package top.neila.polychrome_it.main

import net.fabricmc.api.ModInitializer
import top.neila.polychrome_it.PolychromeBlocks

class PolychromeIt : ModInitializer {
    override fun onInitialize() {
        PolychromeBlocks.init()
    }
}
