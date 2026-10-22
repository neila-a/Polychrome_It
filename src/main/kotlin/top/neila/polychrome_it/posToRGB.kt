package top.neila.polychrome_it

import kotlin.math.abs
import net.minecraft.util.ARGB

val black = ARGB.opaque(0x000000)

private const val grassBlockTopAverageColor = 0x91
fun posToARGB(x: Int, z: Int): Int {
    val image = cachedImage ?: return black
    val x = abs(x) % image.width
    val y = abs(z) % image.height
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
