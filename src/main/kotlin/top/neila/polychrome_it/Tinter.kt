package top.neila.polychrome_it

import net.minecraft.core.BlockPos

fun interface Tinter {
    fun tint(pos: BlockPos): Int
}
