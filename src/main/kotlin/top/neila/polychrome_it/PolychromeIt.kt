package top.neila.polychrome_it

import net.fabricmc.api.ModInitializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PolychromeIt : ModInitializer {
    companion object {
        const val MOD_ID = "polychrome_it"
        val LOGGER: Logger = LoggerFactory.getLogger(MOD_ID)
    }

    override fun onInitialize() {
        LOGGER.info("Initializing Polychrome It!...")
    }
}
