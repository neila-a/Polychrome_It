package top.neila.polychrome_it

import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents
import net.minecraft.client.color.block.BlockTintSource
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.CreativeModeTabs
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockBehaviour

object PolychromeBlocks {
    private const val MOD_ID = "polychrome_it"

    val POLYCHROME_BLOCK = registerBlock(
        "polychrome_block",
        ::PolychromeBlockTintSource
    )

    private fun registerBlock(
        path: String,
        blockTintSource: () -> BlockTintSource
    ): Block {
        val identifier = Identifier.fromNamespaceAndPath(MOD_ID, path)
        val blockKey = ResourceKey.create(Registries.BLOCK, identifier)

        val block = Registry.register(
            BuiltInRegistries.BLOCK,
            blockKey,

            Block(BlockBehaviour.Properties.of().strength(4.0f).setId(blockKey))
        )

        val itemKey = ResourceKey.create(Registries.ITEM, identifier)
        val blockItem = Registry.register(
            BuiltInRegistries.ITEM,
            itemKey,
            BlockItem(block, Item.Properties().setId(itemKey).useBlockDescriptionPrefix())
        )
        blockItem.registerBlocks(Item.BY_BLOCK, blockItem)

        BlockColorRegistry.register(listOf(blockTintSource()), block)

        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COLORED_BLOCKS).register { output ->
            output.accept(block)
        }

        return block
    }

    fun init() {
    }
}
