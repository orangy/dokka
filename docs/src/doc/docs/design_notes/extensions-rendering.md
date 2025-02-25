# Rendering extensions

### Subject
How should dokka treat extensions and where should they be rendered?

### Current state

Currently, dokka treats extension functions as normal functions and renders them in place of declaration with 2 caveats:

- Extensions are not a subject of receiver's inheritance
- Extensions can be 'inherited' if their declaration is in parent class

Given above:
```kotlin
package foo

open class A

class B : A

fun A.bar() {
    
}
```
Dokka will treat an extension `bar` as an extension of type `A` and not `B`. 
Therefore `bar` will be rendered in `extensions` tab of documentation `A` but not in `B`.

```kotlin
package foo

open class A {
    fun String.bar(){
        
    }
}

class B : A
```
In this case class `B` will inherit the extension as normal function

### Language limitations

Kotlin doesn't allow for proper extensions inheritance since it lacks an ability to override extensions.

### Description

From the UX perspective it might be worth to include all extensions for a given type on its page. 
This allows user to get a more comprehensive view of a given library since they can be used as normal 
functions or properties. 
Difference is present if the user would like to override the value, so the fact, that it is an extension should be clearly visible.

While thinking about the details those things come to mind:

- Rendering all extensions starting from a base type up to the root of types tree can result in large amounts of items depending on a scope
- Some extensions can be visible only in a limited visibility (private/protected/public modifiers)
- Libraries can define extensions that user can import but dokka can't analyse
- Extensions can be defined in a different modules and sourcesets
- Extensions can have generic parameters

From an implementation point of view it is worth noting that right now there is no API get all extensions for a type, 
so process is quite expensive both CPU and memory wise.

Assuming that such implementation exists there still is a UI/UX task of displaying those members.

### Possible implementation

It is fair to say that user might expect all extensions to be displayed including a set visibility (mainly `includeNonPublic` setting).
Modules should also oppose a natural boundary of displaying extensions, eg: extensions from different modules should not
be included in the receiver's documentation with an exception to the situation where receiver is defined in the same module as extension.
Sourcesets should also follow similar rules, therefore an extension should be defined in a given sourceset or a sourceset that one has dependency on.
Generic in receiver can be displayed for all matching types but they shall have the lowest priority of the list

Implementation should probably be based on a `documentableTransformer` that would pass all documentables tree 2 times:

1. Collect all extensions
2. Add extensions to subtypes of a receiver type

In html-based formats this can be achieved by creating a separate tab with a collapsable,
accordion-like list that would be sorted in order of receiver's type specificity.
There are out-of-the-box solution for that, eg: [material-ui](https://material-ui.com/components/accordion/) 
or creating own implementation using [ring-ui](https://jetbrains.github.io/ring-ui/master/index.html?path=/story/components-list--with-custom-items)

For gfm-based formats dokka can either:

- render all in a separate section
- render all on separate page
- render a small subset of extensions for a given type (eg. only 1 level of inheritance) and create a separate page will all

with the last approach being probably the most appealing heuristic

### Related materials:
[Github issue](https://github.com/Kotlin/dokka/issues/1908)
[Implicits in scala have same issues](https://dotty.epfl.ch/api/scala/collection/immutable/List$.html)

