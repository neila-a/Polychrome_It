package top.neila.polychrome_it

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.modifyOutputEvent
import net.minecraft.core.BlockPos
import net.minecraft.core.Registry.register
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier.fromNamespaceAndPath
import net.minecraft.resources.ResourceKey.create
import net.minecraft.util.ARGB.*
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.CreativeModeTabs
import net.minecraft.world.item.Item.*
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.material.MapColor
import kotlin.math.abs

val black = black(0xFF)

const val MOD_ID = "polychrome_it"

/**
 * {@see <a href="https://zh.minecraft.wiki/w/无效纹理与模型">Minecraft Wiki</a>}
 */
private val missingno = opaque(0xf800f8)

/*
 * 采用枚举，是因为它有键  
 * 否则完全可以用密封类
 */
enum class PolychromeItBlock(val tint: BlockPos.() -> Int) {
    image_pixel_block({
        val x = abs(x)
        val z = abs(z)
        try {
            PolychromeItTexture.diffuse.image.value!!.run {
                val x = x % width
                val y = z % height
                getRGB(x, y)
                /*private val grassBlockTopAverageColor = 0x91
                return color(
                    red(originalColor) * 255 / grassBlockTopAverageColor,
                    green(originalColor) * 255 / grassBlockTopAverageColor,
                    blue(originalColor) * 255 / grassBlockTopAverageColor
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
        color(x, y, z)
    });

    val registered = run {
        val identifier = fromNamespaceAndPath(MOD_ID, name)
        val blockKey = create(Registries.BLOCK, identifier)

        register(
            BuiltInRegistries.BLOCK,
            blockKey,
            Block(
                // 必须设置 mapColor
                // 否则 Xaero 地图无法正确显示颜色
                // 尽管我们并不需要 mapColor 来提供颜色
                Blocks.STONE.properties()
            )
        ).apply {
            val itemKey = create(Registries.ITEM, identifier)
            register(
                BuiltInRegistries.ITEM,
                itemKey,
                BlockItem(this, Properties().setId(itemKey).useBlockDescriptionPrefix())
            ).run {
                registerBlocks(BY_BLOCK, this)
            }

            modifyOutputEvent(CreativeModeTabs.COLORED_BLOCKS).register {
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
