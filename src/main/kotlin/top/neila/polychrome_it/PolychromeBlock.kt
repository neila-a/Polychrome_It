package top.neila.polychrome_it

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents
import net.minecraft.core.BlockPos
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.util.ARGB
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.CreativeModeTabs
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockBehaviour
import kotlin.math.abs

val black = ARGB.opaque(0x000000)
const val MOD_ID = "polychrome_it"

/*
 * 采用枚举，是因为它有键  
 * 否则完全可以用密封类
 */
enum class PolychromeBlock : Tinter {
    image_pixel_block {
        private val grassBlockTopAverageColor = 0x91
        override fun tint(pos: BlockPos): Int {
            val image = cachedImage ?: return black
            val x = abs(pos.x) % image.width
            val y = abs(pos.z) % image.height
            try {
                val originalColor = image.getRGB(x, y)
                return originalColor
                return ARGB.color(
                    ARGB.red(originalColor) * 255 / grassBlockTopAverageColor,
                    ARGB.green(originalColor) * 255 / grassBlockTopAverageColor,
                    ARGB.blue(originalColor) * 255 / grassBlockTopAverageColor
                )
            } catch (e: ArrayIndexOutOfBoundsException) {
                return black
            }
        }
    },
    simple_gradient_block {
        /**
         * 将数字缩进RGB范围  
         * 原理：`and 255` == `% 256`
         */
        private fun numberToRGBScale(number: Int) = abs(number) and 255

        override fun tint(pos: BlockPos) =
            ARGB.color(numberToRGBScale(pos.x), numberToRGBScale(pos.y), numberToRGBScale(pos.z))
    };

    val registered: Block

    /**
     * 参考：{@link Block#toString())}
     */
    val registeredName: String
        get() = BuiltInRegistries.BLOCK.wrapAsHolder(registered).registeredName

    init {
        val identifier = Identifier.fromNamespaceAndPath(MOD_ID, name)
        val blockKey = ResourceKey.create(Registries.BLOCK, identifier)

        registered = Registry.register(
            BuiltInRegistries.BLOCK,
            blockKey,

            Block(BlockBehaviour.Properties.of().strength(4.0f).setId(blockKey))
        )

        val itemKey = ResourceKey.create(Registries.ITEM, identifier)
        val blockItem = Registry.register(
            BuiltInRegistries.ITEM,
            itemKey,
            BlockItem(registered, Item.Properties().setId(itemKey).useBlockDescriptionPrefix())
        )
        blockItem.registerBlocks(Item.BY_BLOCK, blockItem)

        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COLORED_BLOCKS).register {
            it.accept(registered)
        }
    }
}
