package top.neila.polychrome_it

class Freshenable<T>(val getter: () -> T) {
    var value = getter()
    fun freshen() {
        value = getter()
    }
}
