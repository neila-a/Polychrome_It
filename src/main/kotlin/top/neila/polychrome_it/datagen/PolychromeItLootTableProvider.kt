package top.neila.polychrome_it.datagen

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootSubProvider
import net.minecraft.core.HolderLookup
import top.neila.polychrome_it.PolychromeBlock
import java.util.concurrent.CompletableFuture

class PolychromeItLootTableProvider(
    packOutput: FabricPackOutput,
    registriesFuture: CompletableFuture<HolderLookup.Provider>
) : FabricBlockLootSubProvider(packOutput, registriesFuture) {
    override fun generate() {
        for (block in PolychromeBlock.entries) {
            dropSelf(block.registered)
        }
    }
}