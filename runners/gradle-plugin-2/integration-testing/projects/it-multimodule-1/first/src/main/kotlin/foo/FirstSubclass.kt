package foo

/**
 * Subclass description
 * @property surname Surname description
 */
class FirstSubclass(var surname: String) : FirstClass() {
    /**
     * printNewLine description
     */
    fun printNewline() = print("\r\n")
}