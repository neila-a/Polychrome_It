package top.neila.polychrome_it

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents
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
    const val MOD_ID = "polychrome_it"

    val SIMPLE_GRADIENT_BLOCK = registerBlock("simple_gradient_block")
    const val IMAGE_PIXEL_BLOCK_ID = "image_pixel_block"
    val IMAGE_PIXEL_BLOCK = registerBlock(IMAGE_PIXEL_BLOCK_ID)

    private fun registerBlock(path: String): Block {
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

        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COLORED_BLOCKS).register {
            it.accept(block)
        }

        return block
    }

    fun init() {
    }
}
