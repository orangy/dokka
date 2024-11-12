package demo

/**
 * This class supports greeting people by name.
 *
 * @property name The name of the person to be greeted.
 */
class Greeter(
    val name: String
) {

    /**
     * Prints the greeting to the standard output.
     */
    fun greet() {
        println("Hello $name!")
    }
}

/**
 * The entry point for [Greeter].
 */
@HideFromDokka
fun main(args: Array<String>) {
    Greeter(args[0]).greet()
}

/**
 * The custom `HideInternalApi` Dokka plugin is configured to exclude code
 * with this annotation from the generated docs.
 */
@MustBeDocumented
annotation class HideFromDokka
