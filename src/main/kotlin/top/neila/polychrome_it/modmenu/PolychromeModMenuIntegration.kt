package top.neila.polychrome_it.modmenu

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import me.shedaniel.clothconfig2.api.ConfigBuilder
import net.minecraft.network.chat.Component
import top.neila.polychrome_it.imagePath

class PolychromeModMenuIntegration : ModMenuApi {
    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> = { parent ->
        val builder = ConfigBuilder.create()
        builder.parentScreen = parent
        builder.title = Component.translatable("title.polychrome_it.config")
        
        val general = builder.getOrCreateCategory(
            // Category Tabs are shown ONLY when you have more than 1 category.
            // So the title won't show.
            // It means that it need not to be translated.
            Component.literal("General")
        )
        val entry = builder.entryBuilder.startStrField(
            Component.translatable("polychrome_it.config.imagepath"),
            imagePath
        )
        entry.defaultValue = { "" }
        entry.saveConsumer = { path ->
            imagePath = path
        }
        general.addEntry(entry.build())
        
        builder.build()
    }
}