[//]: # (title: Migrate to Dokka Gradle plugin v2)

> The Dokka Gradle plugin v2 is an [Experimental](https://kotlinlang.org/docs/components-stability.html#stability-levels-explained) feature. 
> It may be changed at any time. We appreciate your feedback on [GitHub](https://github.com/Kotlin/dokka/issues).
>
{style="warning"}

The Dokka Gradle plugin (DGP) is a tool for generating comprehensive API documentation for Kotlin projects built with Gradle.

DGP seamlessly processes both Kotlin's KDoc comments and Java's Javadoc comments to extract information and create 
structured documentation in [HTML or Javadoc](#select-documentation-output-format) format.

Starting with Dokka 2.0.0-Beta, you can try the Dokka Gradle plugin v2, the new version of DGP. With Dokka 2.0.0-Beta, you can use
the Dokka Gradle plugin either in v1 or v2 modes.

DGP v2 introduces significant improvements to DGP, aligning more closely with Gradle best practices:

* Adopts Gradle types, which leads to better performance.
* Uses an intuitive top-level DSL configuration instead of a low-level task-based setup, which simplifies the build scripts and their readability.
* Takes a more declarative approach to documentation aggregation, which makes multi-project documentation easier to manage.
* Uses a type-safe plugin configuration, which improves the reliability and maintainability of your build scripts.
* Fully supports Gradle [configuration cache](https://docs.gradle.org/current/userguide/configuration_cache.html) and 
  [build cache](https://docs.gradle.org/current/userguide/build_cache.html), which improves performance and simplifies build work.

## Before you start

Before starting the migration, complete the following steps.

### Verify supported versions

Ensure that your project meets the minimum version requirements:

| **Tool**              | **Version**   |
|-----------------------|---------------|
| [Gradle](https://docs.gradle.org/current/userguide/upgrading_version_8.html)                | 7.6 or higher |
| [Android Gradle plugin](https://developer.android.com/build/agp-upgrade-assistant) | 7.0 or higher |
| [Kotlin Gradle plugin](https://kotlinlang.org/docs/gradle-configure-project.html)  | 1.9 or higher |

### Enable the new Dokka Gradle plugin

1. Update the Dokka version to 2.0.0-Beta in the `plugins {}` block of your project’s `build.gradle.kts` file:

   ```kotlin
   plugins {
       kotlin("jvm") version "1.9.25"
       id("org.jetbrains.dokka") version "2.0.0-Beta"
   }
   ```

   Alternatively, you can use [version catalog](https://docs.gradle.org/current/userguide/platforms.html#sub:version-catalog) to enable the Dokka Gradle plugin v2.

2. In the project's `gradle.properties` file, set the following opt-in flag with helpers to activate the new plugin version:

   ```text
   org.jetbrains.dokka.experimental.gradle.pluginMode=V2EnabledWithHelpers
   ```

   This flag activates the DGP v2 plugin with migration helpers, which prevent compilation errors when build scripts reference
   tasks from DGP v1 that are no longer available in DGP v2. You have to [disable](#set-the-opt-in-flag) 
   the migration helpers after completing your migration.

   > If your project does not have a `gradle.properties` file, create one in the root directory of your project.
   >
   {style="tip"}

3. Sync your project with Gradle to ensure the new plugin is properly applied:

   * If you use IntelliJ IDEA, click the **Reload All Gradle Projects** ![Reload button](gradle-reload-button.png){width=30}{type="joined"} button from the Gradle tool window.
   * If you use Android Studio, select **File** | **Sync Project with Gradle Files**.

## Migrate your project

After updating the Dokka Gradle plugin to v2, follow the migration steps applicable to your project.

### Adjust configuration options

DGP v2 introduces some changes in the [Gradle configuration options](dokka-gradle.md#configuration-options). In the `build.gradle.kts` file, adjust the configuration 
options according to your project setup: 

* **New top-level DSL configuration:** Replace the old configuration syntax with the 
  new top-level `dokka {}` DSL configuration. For example:

  Previous configuration:

  ```kotlin
  tasks.withType<DokkaTask>().configureEach {
      dokkaSourceSets {
          named("main") {
              moduleName.set("Project Name")
              includes.from("README.md")
              sourceLink {
                  localDirectory.set(file("src/main/kotlin"))
                  remoteUrl.set(URL("https://example.com/src"))
                  remoteLineSuffix.set("#L")
              }
          }
      }
  }

  tasks.dokkaHtml {
      pluginConfiguration<DokkaBase, DokkaBaseConfiguration> {
          customStyleSheets.set(listOf("styles.css"))
          customAssets.set(listOf("logo.png"))
          footerMessage.set("(c) Your Company")
      }
  }
  ```
 
  New configuration:

  ```kotlin
  dokka {
      moduleName.set("Project Name")
      dokkaSourceSets.main {
          includes.from("README.md")
          sourceLink {
              localDirectory.set(file("src/main/kotlin"))
              remoteUrl("https://example.com/src")
              remoteLineSuffix.set("#L")
          }
      }
      pluginsConfiguration.html {
          customStyleSheets.from("styles.css")
          customAssets.from("logo.png")
          footerMessage.set("(c) Your Company")
      }
  }
  ```

* **Visibility settings:**  The `documentedVisibilities` property has changed from `Visibility.PUBLIC` to `VisibilityModifier.Public`.

  Previous configuration:

  ```kotlin
  import org.jetbrains.dokka.DokkaConfiguration.Visibility
  // ...
  documentedVisibilities.set(
      setOf(Visibility.PUBLIC)
  )  
  ```

  New configuration:

  ```kotlin
  import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier
  // ...
  documentedVisibilities.set(
      setOf(VisibilityModifier.Public)
  )
  
  // OR
  
  documentedVisibilities(VisibilityModifier.Public)
  ```

  Additionally, DGP v2 has a [utility function](https://github.com/Kotlin/dokka/blob/220922378e6c68eb148fda2ec80528a1b81478c9/dokka-runners/dokka-gradle-plugin/src/main/kotlin/engine/parameters/HasConfigurableVisibilityModifiers.kt#L14-L16) for adding documented visibilities:
 
  ```kotlin
  fun documentedVisibilities(vararg visibilities: VisibilityModifier): Unit =
      documentedVisibilities.set(visibilities.asList()) 
  ```

* **External documentation link:** The source link configuration has [changed](https://docs.gradle.org/current/userguide/upgrading_version_8.html#deprecated_invalid_url_decoding). 
    The remote URL is now specified using the `URI` class instead of the `URL` class.

  Previous configuration:
  
    ```kotlin
    remoteUrl.set(URL("https://github.com/your-repo"))
    ```
  
  New configuration:
  
    ```kotlin
    remoteUrl.set(URI("https://github.com/your-repo"))
    
    // OR
    
    remoteUrl("https://github.com/your-repo")
    ```

  Additionally, DGP v2 has two [utility functions](https://github.com/Kotlin/dokka/blob/220922378e6c68eb148fda2ec80528a1b81478c9/dokka-runners/dokka-gradle-plugin/src/main/kotlin/engine/parameters/DokkaSourceLinkSpec.kt#L82-L96) 
  for setting the URL:

  ```kotlin
  fun remoteUrl(@Language("http-url-reference") value: String): Unit =
      remoteUrl.set(URI(value))
  
  // and
  
  fun remoteUrl(value: Provider<String>): Unit =
      remoteUrl.set(value.map(::URI))
  ```


* **Custom assets:** The [`customAssets`](dokka-html.md#customize-assets) property now uses collections of files 
  ([`FileCollection`)](https://docs.gradle.org/8.10/userguide/lazy_configuration.html#working_with_files_in_lazy_properties) 
  instead of lists (`var List<File>`).

  Previous configuration:

    ```kotlin
    customAssets = listOf(file("example.png"), file("example2.png"))   
    ```

  New configuration:

    ```kotlin
    customAssets.from("example.png", "example2.png")
    ```

* **Output directory:** Use the `dokka {}` block to specify the output directory for generated Dokka documentation.

    Previous configuration:

    ```kotlin
    tasks.dokkaHtml {
        outputDirectory.set(layout.buildDirectory.dir("dokkaDir"))
    }
    ```

    New configuration:

    ```kotlin
    dokka {
        dokkaPublications.html {
            outputDirectory.set(layout.buildDirectory.dir("dokkaDir"))
        }
    }
    ```

### Share Dokka configuration across modules

DPG v2 moves away from using `subprojects {}` or `allprojects {}` to share configuration across modules. In future Gradle versions, 
using these approaches will [lead to errors](https://docs.gradle.org/current/userguide/isolated_projects.html).

Follow the steps below to properly share Dokka configuration in multi-module projects [with existing convention plugins](#multi-module-projects-with-convention-plugins)
or [without convention plugins](#multi-module-projects-without-convention-plugins).

After sharing the Dokka configuration, you can aggregate the documentation from multiple modules into a single output. For more information, see
[Update documentation aggregation in multi-module projects](#update-documentation-aggregation-in-multi-module-projects).

> For a multi-module project example, see the [Dokka GitHub repository](https://github.com/Kotlin/dokka/tree/master/examples/gradle-v2/multimodule-example).
>
{style="tip"}

#### Multi-module projects without convention plugins

To share the Dokka configuration in multi-module projects without convention plugins, you need to set up the `buildSrc` directory, 
set up the convention plugin, and then apply the plugin to your modules (subprojects).

##### Set up the buildSrc directory

1. In your project root, create a `buildSrc` directory containing two files:

   * `settings.gradle.kts`
   * `build.gradle.kts`
   
2. In the `buildSrc/settings.gradle.kts` file, add the following snippet:
   
   ```kotlin
   rootProject.name = "buildSrc"
   ```

3. In the `buildSrc/build.gradle.kts` file, add the following snippet:

    ```kotlin
    plugins {
        `kotlin-dsl`
    }
    
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    
    dependencies {
        implementation("org.jetbrains.dokka:dokka-gradle-plugin:2.0.0-Beta")
    }   
    ```

##### Set up the Dokka convention plugin

After setting up the `buildSrc` directory:
   
1. Create a `buildSrc/src/main/kotlin/dokka-convention.gradle.kts` file to host the [convention plugin](https://docs.gradle.org/current/userguide/custom_plugins.html#sec:convention_plugins).
2. In the `dokka-convention.gradle.kts` file, add the following snippet:

    ```kotlin
    plugins {
        id("org.jetbrains.dokka") 
    }
   
    dokka {
        // The shared configuration goes here
    }
    ```

   You need to add the shared Dokka [configuration](#adjust-configuration-options) common to all subprojects within the `dokka {}` block.
   Also, you don't need to specify a Dokka version. The version is already set in the `buildSrc/build.gradle.kts` file.

##### Apply the convention plugin to your modules

Apply the Dokka convention plugin across your modules (subprojects) by adding it to each subproject's `build.gradle.kts` file:

```kotlin
plugins { 
    id("dokka-convention")
}
```

#### Multi-module projects with convention plugins

If you already have convention plugins, create a dedicated Dokka convention plugin following [Gradle's documentation](https://docs.gradle.org/current/userguide/custom_plugins.html#sec:convention_plugins).

Then, follow the steps to [set up the Dokka convention plugin](#set-up-the-dokka-convention-plugin) and 
[apply it across your modules](#apply-the-convention-plugin-to-your-modules).

### Update documentation aggregation in multi-module projects

Dokka can aggregate the documentation from multiple modules (subprojects) into a single output or publication.

As [explained](#apply-the-convention-plugin-to-your-modules),
you must apply the Dokka plugin to all documentable subprojects before aggregating the documentation.

Aggregation in DGP v2 now uses the `dependencies {}` block instead of tasks, and can be added in any `build.gradle.kts` file. 

In DGP v1, aggregation was implicitly created in the root project. To replicate this behavior in DGP v2, add the `dependencies {}` block 
in the `build.gradle.kts` file  of the root project.

Previous aggregation:

```kotlin
tasks.dokkaHtmlMultiModule {
  // ...
}
```

New aggregation:

```kotlin
dependencies {
    dokka(project(":some-subproject:"))
    dokka(project(":another-subproject:"))
}
```

### Generate documentation with the updated task

DGP v2 has renamed the Gradle tasks that generate the API documentation.

Previous task:

```text
./gradlew dokkaHtml
    
// OR

./gradlew dokkaHtmlMultiModule
```

New task:

```text
./gradlew :dokkaGenerate
```

The `dokkaGenerate` task generates the API documentation in the `build/dokka/` directory.

In the new version, the `dokkaGenerate` task name works for both single and multi-module projects. You can use different tasks
to generate output in HTML, Javadoc or both HTML and Javadoc. For more information, see the next section.

### Select documentation output format

> The Javadoc output format is in [Alpha](https://kotlinlang.org/docs/components-stability.html#stability-levels-explained).
> You may find bugs and experience migration issues when using it. Successful integration with tools that accept Javadoc
> as input is not guaranteed. Use it at your own risk.
>
{style="note"}

The default output format for DGP v2 is HTML. However, you can choose to generate the API documentation in HTML, Javadoc, 
or both formats at the same time:

1. Place the corresponding plugin `id` in the `plugins {}` block of your project's `build.gradle.kts` file:

   ```kotlin
   plugins {
       // Generates HTML documentation
       id("org.jetbrains.dokka") version "2.0.0-Beta"
       
       // Generates Javadoc documentation
       id("org.jetbrains.dokka-javadoc") version "2.0.0-Beta"
   
       // Keeping both plugin IDs generates both formats
   }
   ```

2. Run the corresponding Gradle task.

Here's a list of the plugin `id` and Gradle task that correspond to each format:

|             | **HTML**                       | **Javadoc**                         | **Both**                          |
|-------------|--------------------------------|-------------------------------------|-----------------------------------|
| Plugin `id` | `id("org.jetbrains.dokka")`    | `id("org.jetbrains.dokka-javadoc")` | Use both HTML and Javadoc plugins |
| Gradle task | `./gradlew :dokkaGenerateHtml` | `./gradlew :dokkaGenerateJavadoc`   | `./gradlew :dokkaGenerate`        |

> The `dokkaGenerate` task generates documentation in all available formats based on the applied plugins. 
> If both the HTML and Javadoc plugins are applied, you can choose to generate only HTML by running the `dokkaGenerateHtml` task, 
> or only Javadoc by running the `dokkaGenerateJavadoc` task.
> 
{style="tip"}

### Address deprecations and removals

* **Output format support:** Dokka 2.0.0-Beta only supports HTML and Javadoc output. Experimental formats like Markdown and Jekyll are no longer supported.
* **Collector task:** `DokkaCollectorTask` has been removed. Now, you need to generate the documentation separately for
  each subproject and then [aggregate the documentation](#update-documentation-aggregation-in-multi-module-projects) if necessary.

## Finish up your migration

After you've migrated your project, perform these steps to wrap up and improve performance.

### Set the opt-in flag

After successful migration, set the following opt-in flag without helpers in the project's `gradle.properties` file:

```text
org.jetbrains.dokka.experimental.gradle.pluginMode=V2Enabled
```

If you removed references to Gradle tasks from DGP v1 that are no longer available in DGP v2, 
you shouldn't see compilation errors related to it.

### Enable build cache and configuration cache

DGP v2 now supports Gradle build cache and configuration cache, improving build performance.

* To enable build cache, follow instructions in the [Gradle build cache documentation](https://docs.gradle.org/current/userguide/build_cache.html#sec:build_cache_enable).
* To enable configuration cache, follow instructions in the [Gradle configuration cache documentation](https://docs.gradle.org/current/userguide/configuration_cache.html#config_cache:usage:enable ).

## Troubleshooting

In large projects, Dokka can consume a significant amount of memory to generate documentation.
This can exceed Gradle’s memory limits, especially when processing large volumes of data.

When Dokka generation runs out of memory, the build fails, and Gradle can throw exceptions like `java.lang.OutOfMemoryError: Metaspace`.

Active efforts are underway to improve Dokka's performance, although some limitations stem from Gradle.

If you encounter memory issues, try these workarounds:

* [Increasing heap space](#increase-heap-space)
* [Running Dokka within the Gradle process](#run-dokka-within-the-gradle-process)

### Increase heap space

One way to resolve memory issues is to increase the amount of Java heap memory for the Dokka generator process. 
In the `build.gradle.kts` file, adjust the 
following configuration option:

```kotlin
dokka {
    // Dokka generates a new process managed by Gradle
    dokkaGeneratorIsolation = ProcessIsolation {
        // Configures heap size
        maxHeapSize = "4g"
    }
}
```

In this example, the maximum heap size is set to 4 GB (`"4g"`). Adjust and test the value to find the optimal setting for your build.

If you find that Dokka requires a considerably expanded heap size, for example, significantly higher than Gradle's own memory usage, 
[create an issue on Dokka's GitHub repository](https://kotl.in/dokka-issues).

> You have to apply this configuration to each subproject. It's recommended that you configure Dokka in a convention 
> plugin applied to all subprojects.
>
{style="note"}

### Run Dokka within the Gradle process

When both the Gradle build and Dokka generation require a lot of memory, they may run as separate processes, 
consuming significant memory on a single machine.

To optimize memory usage, you can run Dokka within the same Gradle process instead of as a separate process. This 
allows you to configure the memory for Gradle once instead of allocating it separately for each process.

To run Dokka within the same Gradle process, adjust the following configuration option in the `build.gradle.kts` file:

```kotlin
dokka {
    // Runs Dokka in the current Gradle process
    dokkaGeneratorIsolation = ClassLoaderIsolation()
}
```

As with [increasing heap space](#increase-heap-space), test this configuration to confirm it works well for your project.

For more details on configuring Gradle's JVM memory, see the [Gradle documentation](https://docs.gradle.org/current/userguide/config_gradle.html#sec:configuring_jvm_memory).

> Changing the Java options for Gradle launches a new Gradle daemon, which may stay alive for a long time. You can [manually stop any other Gradle processes](https://docs.gradle.org/current/userguide/gradle_daemon.html#sec:stopping_an_existing_daemon).
> 
> Additionally, Gradle issues with the `ClassLoaderIsolation()` configuration may [cause memory leaks](https://github.com/gradle/gradle/issues/18313). 
>
{style="note"}

## What's next

* Explore more [DGP v2 project examples](https://github.com/Kotlin/dokka/tree/master/examples/gradle-v2). 
* [Get started with Dokka](dokka-get-started.md).
* [Learn more about Dokka plugins](dokka-plugins.md).