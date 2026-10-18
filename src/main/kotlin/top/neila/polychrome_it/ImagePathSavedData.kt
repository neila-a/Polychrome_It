package top.neila.polychrome_it

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer
import net.minecraft.util.datafix.DataFixTypes
import net.minecraft.world.level.saveddata.SavedData
import net.minecraft.world.level.saveddata.SavedDataType

class ImagePathSavedData(var imagePath: String = "") : SavedData() {
    fun updateImagePath(path: String) {
        this.imagePath = path
        setDirty()
    }

    companion object {
        private val CODEC: Codec<ImagePathSavedData> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("imagePath").forGetter(ImagePathSavedData::imagePath)
            ).apply(instance, ::ImagePathSavedData)
        }

        private val TYPE: SavedDataType<ImagePathSavedData> = SavedDataType(
            Identifier.fromNamespaceAndPath("polychrome_it", "image_path"),
            ::ImagePathSavedData,
            CODEC,
            // 本模组的数据文件始终以当前数据版本写入，读取时版本相同，
            // DataFixer 会直接跳过（DataFixerUpper.update 的版本守卫），此处仅用于满足非空约束
            DataFixTypes.LEVEL
        )

        fun get(server: MinecraftServer): ImagePathSavedData {
            return server.overworld().dataStorage.computeIfAbsent(TYPE)
        }
    }
}
