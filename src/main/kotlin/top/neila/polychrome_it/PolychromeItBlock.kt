package top.neila.polychrome_it

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.modifyOutputEvent
import net.minecraft.core.Registry.register
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier.fromNamespaceAndPath
import net.minecraft.resources.ResourceKey.create
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.CreativeModeTabs
import net.minecraft.world.item.Item.*
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks

const val MOD_ID = "polychrome_it"

/*
 * 采用枚举，是因为它有键  
 * 否则完全可以用密封类
 */
@Suppress("EnumEntryName")
enum class PolychromeItBlock {
    image_pixel_block,
    simple_gradient_block;

    val registered = fromNamespaceAndPath(MOD_ID, name).let { identifier ->
        create(Registries.BLOCK, identifier).let {
            register(
                BuiltInRegistries.BLOCK,
                it,
                Block(
                    // 必须设置 mapColor
                    // 否则 Xaero 地图无法正确显示颜色
                    // 尽管我们并不需要 mapColor 来提供颜色
                    Blocks.STONE.properties()
                )
            )
        }.apply {
            create(Registries.ITEM, identifier).let {
                register(
                    BuiltInRegistries.ITEM,
                    it,
                    BlockItem(this, Properties().setId(it).useBlockDescriptionPrefix())
                )
            }.run {
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

    companion object : ModInitializer {
        override fun onInitialize() = Unit
    }
}
