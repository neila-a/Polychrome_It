package top.neila.polychrome_it

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import me.shedaniel.autoconfig.AutoConfigClient

object PolychromeItModMenuIntegration : ModMenuApi {
    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> = { parent ->
        AutoConfigClient.getConfigScreen(PolychromeItConfig::class.java, parent).get()
    }
}
