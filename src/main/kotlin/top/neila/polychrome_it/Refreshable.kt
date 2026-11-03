package top.neila.polychrome_it

class Refreshable<T>(val getter: () -> T) {
    var value = getter()
    fun refresh() {
        value = getter()
    }
}
