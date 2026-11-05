package top.neila.polychrome_it.shaderpackpatcher

/** 在位置 at 处插入一整段 text（其后补一个换行）。 */
fun String.insertString(at: Int, text: String) =
    substring(0, at) + text + "\n" + substring(at)
