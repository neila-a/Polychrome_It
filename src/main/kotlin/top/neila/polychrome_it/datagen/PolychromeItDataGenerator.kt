package top.neila.polychrome_it.datagen

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator
import top.neila.polychrome_it.datagen.providers.*

object PolychromeItDataGenerator : DataGeneratorEntrypoint {
    override fun onInitializeDataGenerator(fabricDataGenerator: FabricDataGenerator) =
        with(fabricDataGenerator.createPack()) {
            addProvider(::PolychromeItLootTableProvider)
            addProvider(::PolychromeItModelProvider)
            Unit
        }
}