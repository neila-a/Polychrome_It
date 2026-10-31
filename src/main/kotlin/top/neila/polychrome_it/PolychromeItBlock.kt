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

val black = ARGB.black(0xFF)
// 用于识别 image_pixel_block 的染色：品红 (R=255,G=0,B=255)。着色器用“红≈蓝、绿≈0”的比例判定。
val magenta = ARGB.opaque(0xff00ff)

const val MOD_ID = "polychrome_it"

/**
 * {@see <a href="https://zh.minecraft.wiki/w/无效纹理与模型">Minecraft Wiki</a>}
 */
private val missingno = ARGB.opaque(0xf800f8)

/*
 * 采用枚举，是因为它有键  
 * 否则完全可以用密封类
 */
enum class PolychromeItBlock(val tint: BlockPos.() -> Int) {
    image_pixel_block({
        val x = abs(x)
        val z = abs(z)
        try {
            image!!.run {
                val x = x % width
                val y = z % height
                getRGB(x, y)
                /*private val grassBlockTopAverageColor = 0x91
                return ARGB.color(
                    ARGB.red(originalColor) * 255 / grassBlockTopAverageColor,
                    ARGB.green(originalColor) * 255 / grassBlockTopAverageColor,
                    ARGB.blue(originalColor) * 255 / grassBlockTopAverageColor
                )*/
            }
        } catch (_: Exception) {
            val xForUse = if (z and 1 == 0) x else x + 1
            if (xForUse and 1 == 0)
                black
            else
                missingno
        }
    }),
    simple_gradient_block({
        // ARGB.color 会自动and 256
        ARGB.color(x, y, z)
    });

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
    val registeredName: String by lazy {
        BuiltInRegistries.BLOCK.wrapAsHolder(registered).registeredName
    }
}
