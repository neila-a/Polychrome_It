package top.neila.polychrome_it.datagen

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator
import top.neila.polychrome_it.MOD_ID
import top.neila.polychrome_it.datagen.providers.*

object PolychromeItDataGenerator : DataGeneratorEntrypoint {
    override fun onInitializeDataGenerator(fabricDataGenerator: FabricDataGenerator) =
        fabricDataGenerator.createPack().run {
            addProvider(::PolychromeItModelProvider)
            addProvider(::PolychromeItLootTableProvider)
            return@run
        }

    override fun getEffectiveModId() = MOD_ID
}
