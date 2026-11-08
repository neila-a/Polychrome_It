package top.neila.polychrome_it.datagen.providers

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootSubProvider
import net.minecraft.core.HolderLookup.Provider
import top.neila.polychrome_it.PolychromeItBlock.entries
import java.util.concurrent.CompletableFuture

class PolychromeItLootTableProvider(
    packOutput: FabricPackOutput,
    registriesFuture: CompletableFuture<Provider>
) : FabricBlockLootSubProvider(packOutput, registriesFuture) {
    override fun generate() = entries.forEach {
        dropSelf(it.registered)
    }
}