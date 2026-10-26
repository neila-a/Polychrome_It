package top.neila.polychrome_it.datagen.providers

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootSubProvider
import net.minecraft.core.HolderLookup
import top.neila.polychrome_it.PolychromeItBlock
import java.util.concurrent.CompletableFuture

class PolychromeItLootTableProvider(
    packOutput: FabricPackOutput,
    registriesFuture: CompletableFuture<HolderLookup.Provider>
) : FabricBlockLootSubProvider(packOutput, registriesFuture) {
    override fun generate() {
        for (block in PolychromeItBlock.entries) {
            dropSelf(block.registered)
        }
    }
}