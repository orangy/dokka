# Using the Gradle plugin

!!! important
    If you are upgrading from 0.10.x to a current release of Dokka, please have a look at our
    [migration guide](https://github.com/Kotlin/dokka/blob/master/runners/gradle-plugin/MIGRATION.md)

### Supported versions
Dokka should work on gradle newer than 5.6

### Setup

The preferred way is to use `plugins` block.

build.gradle.kts:
```kotlin
plugins {
    id("org.jetbrains.dokka") version "1.7.20"
}

repositories {
    mavenCentral()
}
```

You can also use the legacy plugin application method with `buildscript` block.
Note that by using the `buildscript` way type-safe accessors are not available in Gradle Kotlin DSL,
eg. you'll have to use `named<DokkaTask>("dokkaHtml")` instead of `dokkaHtml`:

```kotlin
buildscript {
    dependencies {
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:${dokka_version}")
    }
}

apply(plugin="org.jetbrains.dokka")
```

The plugin adds `dokkaHtml`, `dokkaJavadoc`, `dokkaGfm` and `dokkaJekyll` tasks to the project.

Each task corresponds to one output format, so you should run `dokkaGfm` when you want to have a documentation in `GFM` format.
Output formats are explained in [the introduction](../introduction.md#output-formats)

If you encounter any problems when migrating from older versions of Dokka, please see the [FAQ](https://github.com/Kotlin/dokka/wiki/faq).

Minimal configuration (with custom output directory only):

Kotlin DSL
```kotlin
tasks.dokkaHtml.configure {
    outputDirectory.set(buildDir.resolve("dokka"))
}
```


Groovy DSL
```groovy
dokkaHtml {
    outputDirectory.set(file("${buildDir}/dokka"))
}
```

!!! note
    Dokka extracts the information about sourcesets from the Kotlin Gradle plugin.
    Therefore, if you are using Dokka in a [precompiled script plugin](https://docs.gradle.org/current/userguide/custom_plugins.html#sec:precompiled_plugins),
    you will have to add a dependency to the Kotlin Gradle Plugin as well
    (`implementation(kotlin("gradle-plugin", "<kotlin-version>"))` resp. `implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:<kotlin-version>")`).

## Configuration options

Dokka documents single-platform as well as multi-platform projects.
Most of the configuration options are set per one source set.
The available configuration options are shown below:

```kotlin
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.gradle.tasks.DokkaTask

val dokkaHtml by getting(DokkaTask::class) {
    outputDirectory.set(buildDir.resolve("dokka"))

    // Set module name displayed in the final output
    moduleName.set("moduleName")

    // Use default or set to custom path to cache directory
    // to enable package-list caching
    // When this is set to default, caches are stored in $USER_HOME/.cache/dokka
    cacheRoot.set(file("default"))

    // Suppress obvious functions like default toString or equals. Defaults to true
    suppressObviousFunctions.set(false)

    // Suppress all inherited members that were not overriden in a given class. 
    // Eg. using it you can suppress toString or equals functions but you can't suppress componentN or copy on data class. To do that use with suppressObviousFunctions
    // Defaults to false
    suppressInheritedMembers.set(true)

    // Used to prevent resolving package-lists online. When this option is set to true, only local files are resolved  
    offlineMode.set(false)
    
    dokkaSourceSets {
        configureEach { // Or source set name, for single-platform the default source sets are `main` and `test`

            // Used when configuring source sets manually for declaring which source sets this one depends on
            dependsOn("otherSourceSetName")
        
            // Used to remove a source set from documentation, test source sets are suppressed by default  
            suppress.set(false)

            // Deprecated. Prefer using documentedVisibilities.
            includeNonPublic.set(false)

            // A set of visibility modifiers that should be documented
            // If set by user, overrides includeNonPublic. Default is PUBLIC
            documentedVisibilities.set(
                setOf(
                    DokkaConfiguration.Visibility.PUBLIC, // Same for both Kotlin and Java
                    DokkaConfiguration.Visibility.PRIVATE, // Same for both Kotlin and Java
                    DokkaConfiguration.Visibility.PROTECTED, // Same for both Kotlin and Java
                    DokkaConfiguration.Visibility.INTERNAL, // Kotlin-specific internal modifier
                    DokkaConfiguration.Visibility.PACKAGE, // Java-specific package-private visibility
                )
            )

            // Do not output deprecated members. Applies globally, can be overridden by packageOptions
            skipDeprecated.set(false)

            // Emit warnings about not documented members. Applies globally, also can be overridden by packageOptions
            reportUndocumented.set(true)

            // Do not create index pages for empty packages
            skipEmptyPackages.set(true)

            // This name will be shown in the final output
            displayName.set("JVM")

            // Platform used for code analysis. See the "Platforms" section of this readme
            platform.set(org.jetbrains.dokka.Platform.jvm)

            // Property used for manual addition of files to the classpath
            // This property does not override the classpath collected automatically but appends to it
            classpath.from(file("libs/dependency.jar"))

            // List of files with module and package documentation
            // https://kotlinlang.org/docs/reference/dokka-module-and-package-docs.html
            includes.from("packages.md", "extra.md")

            // List of files or directories containing sample code (referenced with @sample tags)
            samples.from("samples/basic.kt", "samples/advanced.kt")

            // By default, sourceRoots are taken from Kotlin Plugin and kotlinTasks, following roots will be appended to them
            // Repeat for multiple sourceRoots
            sourceRoots.from(file("src"))

            // Specifies the location of the project source code on the Web.
            // If provided, Dokka generates "source" links for each declaration.
            // Repeat for multiple mappings
            sourceLink {
                // Unix based directory relative path to the root of the project (where you execute gradle respectively). 
                localDirectory.set(file("src/main/kotlin"))

                // URL showing where the source code can be accessed through the web browser
                remoteUrl.set(java.net.URL(
                    "https://github.com/cy6erGn0m/vertx3-lang-kotlin/blob/master/src/main/kotlin"))
                // Suffix which is used to append the line number to the URL. Use #L for GitHub
                remoteLineSuffix.set("#L")
            }

            // Used for linking to JDK documentation
            jdkVersion.set(8)

            // Disable linking to online kotlin-stdlib documentation
            noStdlibLink.set(false)

            // Disable linking to online JDK documentation
            noJdkLink.set(false)

            // Disable linking to online Android documentation (only applicable for Android projects)
            noAndroidSdkLink.set(false)

            // Allows linking to documentation of the project"s dependencies (generated with Javadoc or Dokka)
            // Repeat for multiple links
            externalDocumentationLink {
                // Root URL of the generated documentation to link with. The trailing slash is required!
                url.set(URL("https://example.com/docs/"))

                // If package-list file is located in non-standard location
                // packageListUrl = URL("file:///home/user/localdocs/package-list")
            }

            // Allows to customize documentation generation options on a per-package basis
            // Repeat for multiple packageOptions
            // If multiple packages match the same matchingRegex, the longest matchingRegex will be used
            perPackageOption {
                // will match kotlin and all sub-packages of it
                matchingRegex.set("kotlin($|\\.).*") 
                
                // All options are optional
                skipDeprecated.set(false)
                reportUndocumented.set(true) // Emit warnings about not documented members 
                includeNonPublic.set(false) // Deprecated, prefer using documentedVisibilities

                // Visibilities that should be included in the documentation
                // If set by user, overrides includeNonPublic. Default is PUBLIC
                documentedVisibilities.set(
                    setOf(
                        DokkaConfiguration.Visibility.PUBLIC, // Same for both Kotlin and Java
                        DokkaConfiguration.Visibility.PRIVATE, // Same for both Kotlin and Java
                        DokkaConfiguration.Visibility.PROTECTED, // Same for both Kotlin and Java
                        DokkaConfiguration.Visibility.INTERNAL, // Kotlin-specific internal modifier
                        DokkaConfiguration.Visibility.PACKAGE, // Java-specific package-private visibility
                    )
                )
            }
            // Suppress a package
            perPackageOption {
                matchingRegex.set(""".*\.internal.*""") // will match all .internal packages and sub-packages 
                suppress.set(true)
            }

            // Include generated files in documentation
            // By default Dokka will omit all files in folder named generated that is a child of buildDir
            suppressGeneratedFiles.set(false)
        }
        // Configures a plugin separately from the global configuration
        pluginConfiguration<PluginClass, ConfigurationClass>{
            // values
        }
    }
}
```

## Multiplatform
Dokka supports single-platform and multi-platform projects using source sets abstraction. For most mutli-platform projects
you should assume that Dokka's source sets correspond to Kotlin plugin's source sets. All source sets are by default registered
and configured automatically although test source sets are suppressed

Kotlin
```kotlin
kotlin {  // Kotlin Multiplatform plugin configuration
    jvm()
    js("customName")
}

tasks.withType<DokkaTask>().configureEach {
    // custom output directory
    outputDirectory.set(buildDir.resolve("dokka"))

    dokkaSourceSets { 
         named("customNameMain") { // The same name as in Kotlin Multiplatform plugin, so the sources are fetched automatically
            includes.from("packages.md", "extra.md")
            samples.from("samples/basic.kt", "samples/advanced.kt")
        }

        register("differentName") { // Different name, so source roots must be passed explicitly
            displayName.set("JVM")
            platform.set(org.jetbrains.dokka.Platform.jvm)
            sourceRoots.from(kotlin.sourceSets.getByName("jvmMain").kotlin.srcDirs)
            sourceRoots.from(kotlin.sourceSets.getByName("commonMain").kotlin.srcDirs)
        }
    }
}
```

!!! note
    If you want to share the configuration between source sets, you can use Gradle's `configureEach`

## Applying plugins
Dokka plugin creates Gradle configuration for each output format in the form of `dokka${format}Plugin` (or `dokka${format}PartialPlugin` for multi-module tasks) :

```kotlin
dependencies {
    dokkaHtmlPlugin("org.jetbrains.dokka:kotlin-as-java-plugin:1.7.20")
}
``` 

You can also create a custom Dokka task and add plugins directly inside:

```kotlin
val customDokkaTask by creating(DokkaTask::class) {
    dependencies {
        plugins("org.jetbrains.dokka:kotlin-as-java-plugin:1.7.20")
    }
}
```

!!! important
    Please note that `dokkaJavadoc` task will properly document only single `jvm` source set

To generate the documentation, use the appropriate `dokka${format}` Gradle task:

```bash
./gradlew dokkaHtml
```

Some plugins can be configured separately using a plugin class and configuration class. For example:

```kotlin
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.DokkaBaseConfiguration

pluginConfiguration<DokkaBase, DokkaBaseConfiguration> {
    customAssets = listOf(file("<path to asset>"))
    customStyleSheets = listOf(file("<path to custom stylesheet>"))
}
```

Keep in mind, that this only works when using a buildscript (with the configured plugin on classpath) since it is not possible to import plugin's class without it.
For example, you can add `DokkaBase` to gain access to aforementioned configuration:

```kotlin
buildscript {
    dependencies {
        // classpath("<plugin coordinates>:<plugin version>")
        classpath("org.jetbrains.dokka:dokka-base:1.7.20")
    }
}
```

If you don't want to use a buildscript or use Kotlin version lower than 1.3.50 you can achieve the same behaviour manually:
```kotlin
pluginsMapConfiguration.set(mapOf("<fully qualified plugin's name>" to """<json configuration>"""))
```
## Android

!!! important
    Make sure you apply Dokka after `com.android.library` and `kotlin-android`.

```kotlin
buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${kotlin_version}")
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:${dokka_version}")
    }
}
repositories {
    mavenCentral()
}
apply(plugin= "com.android.library")
apply(plugin= "kotlin-android")
apply(plugin= "org.jetbrains.dokka")
```

```kotlin
dokkaHtml.configure {
    dokkaSourceSets {
        named("main") {
            noAndroidSdkLink.set(false)
        }   
    }
}
```

## Multi-module projects
For documenting Gradle multi-module projects, you can use `dokka${format}MultiModule` tasks.
Dokka plugin adds `dokkaHtmlMultiModule`, `dokkaGfmMultiModule` and `dokkaJekyllMultiModule` tasks to
all Gradle parent projects (all projects that have some child projects) as well as
`dokkaHtmlPartial`, `dokkaGfmPartial` and `dokkaJekyllPartial` to all projects that have a parent.
If you want eg. to add an external link to some dependency you should do so in respective `dokka${format}Partial` tasks,
or configure them all at once using the `subprojects` block and `configureEach` method.

```kotlin
tasks.dokkaHtmlMultiModule.configure {
    outputDirectory.set(buildDir.resolve("dokkaCustomMultiModuleOutput"))
}
```

`DokkaMultiModule` depends on all Dokka tasks in the subprojects named `dokka${format}Partial`, runs them, and creates a top-level page
with links to all generated (sub)documentations. It is possible to configure each of them:
```kotlin
tasks.dokkaHtmlPartial.configure {
    failOnWarning.set(true)
}
```

## Example projects

Please see the [Dokka Gradle single module example project](https://github.com/Kotlin/dokka/tree/master/examples/gradle/dokka-gradle-example) or [multimodule](https://github.com/Kotlin/dokka/tree/master/examples/gradle/dokka-multimodule-example) for an example.

Also see [generated documentation](https://Kotlin.github.io/dokka/examples/dokka-gradle-example/html) in `HTML` format.
