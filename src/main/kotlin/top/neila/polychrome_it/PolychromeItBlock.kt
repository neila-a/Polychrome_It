package top.neila.polychrome_it

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents
import net.fabricmc.loader.api.FabricLoader
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
import java.awt.image.BufferedImage
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.math.abs

val black = ARGB.opaque(0x000000)
const val MOD_ID = "polychrome_it"

const val imageFilename = "${MOD_ID}_image"
val imagePath: Path =
    FabricLoader.getInstance().configDir.resolve(imageFilename)
var readImage: BufferedImage? = getImage()
fun getImage() =
    try {
        ImageIO.read(imagePath.toFile())
    } catch (e: Exception) {
        null
    }

/*
 * 采用枚举，是因为它有键  
 * 否则完全可以用密封类
 */
enum class PolychromeItBlock : Tinter {
    image_pixel_block {
        private val grassBlockTopAverageColor = 0x91
        override fun tint(pos: BlockPos) = readImage?.run {
            val x = abs(pos.x) % width
            val y = abs(pos.z) % height
            try {
                getRGB(x, y)
                /*return ARGB.color(
                    ARGB.red(originalColor) * 255 / grassBlockTopAverageColor,
                    ARGB.green(originalColor) * 255 / grassBlockTopAverageColor,
                    ARGB.blue(originalColor) * 255 / grassBlockTopAverageColor
                )*/
            } catch (e: ArrayIndexOutOfBoundsException) {
                black
            }
        } ?: black
    },
    simple_gradient_block {
        /**
         * 将数字缩进RGB范围
         * 原理：`and 255` == `% 256`
         */
        private fun numberToRGBScale(number: Int) = abs(number) and 255

        override fun tint(pos: BlockPos) = with(pos) {
            ARGB.color(numberToRGBScale(x), numberToRGBScale(y), numberToRGBScale(z))
        }
    };

    val registered = run {
        val identifier = Identifier.fromNamespaceAndPath(MOD_ID, name)
        val blockKey = ResourceKey.create(Registries.BLOCK, identifier)

        Registry.register(
            BuiltInRegistries.BLOCK,
            blockKey,

            Block(BlockBehaviour.Properties.of().strength(4.0f).setId(blockKey))
        ).apply {
            val itemKey = ResourceKey.create(Registries.ITEM, identifier)
            Registry.register(
                BuiltInRegistries.ITEM,
                itemKey,
                BlockItem(this, Item.Properties().setId(itemKey).useBlockDescriptionPrefix())
            ).run {
                registerBlocks(Item.BY_BLOCK, this)
            }

            CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COLORED_BLOCKS).register {
                it.accept(this)
            }
        }
    }

    /**
     * 参考：{@link Block#toString())}
     */
    val registeredName: String
        get() = BuiltInRegistries.BLOCK.wrapAsHolder(registered).registeredName
}
