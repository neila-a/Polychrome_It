package top.neila.polychrome_it.datagen.providers

import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput
import net.minecraft.client.data.models.BlockModelGenerators
import net.minecraft.client.data.models.ItemModelGenerators
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator
import net.minecraft.client.data.models.model.ModelTemplate
import net.minecraft.client.data.models.model.TextureMapping
import net.minecraft.resources.Identifier
import top.neila.polychrome_it.MOD_ID
import top.neila.polychrome_it.PolychromeItBlock
import java.util.Optional

class PolychromeItModelProvider(output: FabricPackOutput) : FabricModelProvider(output) {
    /**
     * 指向 resources 中手写的父模型 all_white.json：
     * 六面均带 tintindex 0 的白色立方体，供客户端 BlockColorRegistry 运行时染色。
     *
     * 不声明任何纹理槽——父模型自身不引用 #占位符，
     * 因此生成结果就是 {"parent": "polychrome_it:block/all_white"}。
     * 若改用 createTrivialCube（cube_all + 显式贴图），则没有 tintindex，
     * 染色不会生效，且会引用不存在的贴图。
     */
    private val allWhiteTemplate = ModelTemplate(
        Optional.of(Identifier.fromNamespaceAndPath(MOD_ID, "block/all_white")),
        Optional.empty()
    )

    override fun getName() = "PolychromeModelProvider"

    override fun generateBlockStateModels(blockModelGenerators: BlockModelGenerators) = with(blockModelGenerators) {
        for (block in PolychromeItBlock.entries) {
            val model = allWhiteTemplate.create(block.registered, TextureMapping(), modelOutput)
            blockStateOutput.accept(
                MultiVariantGenerator.dispatch(block.registered, BlockModelGenerators.plainVariant(model))
            )
            registerSimpleItemModel(block.registered, model)
        }
    }

    override fun generateItemModels(itemModelGenerators: ItemModelGenerators) {
    }
}
