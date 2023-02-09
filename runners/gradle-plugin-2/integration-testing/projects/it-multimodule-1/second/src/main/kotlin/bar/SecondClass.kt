package bar
/**
 * Second class in second module description [foo.FirstClass]
 * @author John Doe
 * @version 0.1.3
 * @param  name Name description text text text.
*/
class SecondClass(val name: String) {
    val firstProperty = "propertystring"
    /**
     * Second property in second module description [foo.FirstSubclass]
     */
    var secondProperty: String = ""
        set(value) {
            println("Second property not set")
        }

    init {
        println("InitializerBlock")
    }
}
