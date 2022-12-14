[//]: # (title: Gradle plugin)

To generate documentation for a Gradle-based project, you can use the [Dokka Gradle plugin](#applying-the-plugin).

It comes with basic autoconfiguration (including multi-project and multiplatform builds), has convenient
[Gradle tasks](#generating-documentation) for generating documentation, and provides a great deal of
[configuration options](#configuration) to customize output.

You can play around with Dokka and see how it can be configured for various projects by visiting our
[Gradle example projects](https://github.com/Kotlin/dokka/tree/%dokkaVersion%/examples/gradle).

## Applying the plugin

The recommended way of applying the plugin is via 
[plugins DSL](https://docs.gradle.org/current/userguide/plugins.html#sec:plugins_block):

<tabs group="build-script">
<tab title="Kotlin" group-key="kotlin">

```kotlin
plugins {
    id("org.jetbrains.dokka") version "%dokkaVersion%"
}
```

</tab>
<tab title="Groovy" group-key="groovy">

```groovy
plugins {
    id 'org.jetbrains.dokka' version '%dokkaVersion%'
}
```

</tab>
</tabs>

When documenting [multi-project](gradle.md#multi-project-builds) builds, you need to apply the Dokka plugin within subprojects as well.
You can use `allprojects {}` and `subprojects {}` Gradle configurations to achieve that:

<tabs group="build-script">
<tab title="Gradle Kotlin DSL" group-key="kotlin">

```kotlin
subprojects {
    apply(plugin = "org.jetbrains.dokka")
}
```

</tab>
<tab title="Gradle Groovy DSL" group-key="groovy">

```groovy
subprojects {
    apply plugin: 'org.jetbrains.dokka'
}
```

</tab>
</tabs>

> Under the hood, Dokka uses the [Kotlin Gradle plugin](https://kotlinlang.org/docs/gradle.html) to perform
> autoconfiguration
> of [source sets](https://kotlinlang.org/docs/multiplatform-discover-project.html#source-sets) for documentation
> to be generated. Make sure to apply Kotlin Gradle Plugin or
> [configure source sets](#source-set-configuration) manually.
>
{type="note"}

> If you are using Dokka in a
> [precompiled script plugin](https://docs.gradle.org/current/userguide/custom_plugins.html#sec:precompiled_plugins),
> you need to add the [Kotlin Gradle plugin](https://kotlinlang.org/docs/gradle.html) as a dependency in order for
> it to work properly:
>
> <tabs group="build-script">
> <tab title="Kotlin" group-key="kotlin">
>
> ```kotlin
> implementation(kotlin("gradle-plugin", "%kotlinVersion%"))
> ```
>
> </tab>
> <tab title="Groovy" group-key="groovy">
>
> ```groovy
> implementation 'org.jetbrains.kotlin:kotlin-gradle-plugin:%kotlinVersion%'
> ```
>
> </tab>
> </tabs>
>
{type="note"}

If you cannot use the plugin DSL for some reason, you can use
[the legacy method](https://docs.gradle.org/current/userguide/plugins.html#sec:old_plugin_application) of applying
plugins.

## Generating documentation

Dokka's Gradle plugin comes with [HTML](html.md), [Markdown](markdown.md) and [Javadoc](javadoc.md) output formats built in.
It adds a number of tasks for generating documentation, both for [single](#single-project-builds)
and [multi-project](#multi-project-builds) builds.

### Single project builds

Use the following tasks to build documentation for simple, single project applications and libraries:

#### Stable formats

| **Task**       | **Description**                                                                     |
|----------------|-------------------------------------------------------------------------------------|
| `dokkaHtml`    | Generates documentation in [HTML](html.md) format.                                  |

#### Experimental formats

| **Task**       | **Description**                                                                     |
|----------------|-------------------------------------------------------------------------------------|
| `dokkaGfm`     | Generates documentation in [GitHub Flavored Markdown](markdown.md#gfm) format.      |
| `dokkaJavadoc` | Generates documentation in [Javadoc](javadoc.md) format.                            |
| `dokkaJekyll`  | Generates documentation in [Jekyll compatible Markdown](markdown.md#jekyll) format. |

By default, generated documentation is stored in the `build/dokka/{format}` directory of your project.
The output location, among other things, can be [configured](#configuration) separately.

### Multi-project builds

For documenting [multi-project builds](https://docs.gradle.org/current/userguide/multi_project_builds.html), make sure
that you apply the Dokka plugin within subprojects that you want to generate documentation for, as well as in their parent project.

#### MultiModule tasks

`MultiModule` tasks generate documentation for each subproject individually via [partial](#partial-tasks) tasks,
collect and process all outputs, and produce complete documentation with a common table of contents and resolved
cross-project references.

Dokka creates the following tasks for **parent** projects automatically:

#### Stable formats

| **Task**                 | **Description**                                                        |
|--------------------------|------------------------------------------------------------------------|
| `dokkaHtmlMultiModule`   | Generates multi-module documentation in [HTML](html.md) output format. |

#### Experimental formats

| **Task**                 | **Description**                                                                                         |
|--------------------------|---------------------------------------------------------------------------------------------------------|
| `dokkaGfmMultiModule`    | Generates multi-module documentation in [GitHub Flavored Markdown](markdown.md#gfm) output format.      |
| `dokkaJekyllMultiModule` | Generates multi-module documentation in [Jekyll compatible Markdown](markdown.md#jekyll) output format. |

> The [Javadoc](javadoc.md) output format does not have a MultiModule task, but a [Collector](#collector-tasks) task can
> be used instead.
>
{type="note"}

By default, you can find ready-to-use documentation under `{parentProject}/build/dokka/{format}MultiModule` directory.

#### MultiModule task example

Given a project with the following structure:

```text
parentProject
    └── childProjectA
        ├── demo
            ├── ChildProjectAClass
    └── childProjectB
        ├── demo
            ├── ChildProjectBClass
```

These pages are generated after running `dokkaHtmlMultiModule`:

![Screenshot for output of dokkaHtmlMultiModule task](dokkaHtmlMultiModule-example.png){width=600}

See our [multi-module project example](https://github.
com/Kotlin/dokka/tree/master/examples/gradle/dokka-multimodule-example)
for more details.

#### Partial tasks

Each subproject has _partial_ tasks created for it: `dokkaHtmlPartial`,`dokkaGfmPartial`,
and `dokkaJekyllPartial`.

These tasks are not intended to be used independently and exist only to be called by the parent's 
[MultiModule](#multimodule-tasks) task.

Output generated by partial tasks contains non-displayable formatting along with unresolved templates and references.

> If you want to generate documentation for a single subproject only, use
> [single project tasks](#single-project-builds). For example, `:subproject:dokkaHtml`.

#### Collector tasks

Similar to MultiModule tasks, _Collector_ tasks are created for each parent project: `dokkaHtmlCollector`,
`dokkaGfmCollector`, `dokkaJavadocCollector` and `dokkaJekyllCollector`.

A Collector task executes the corresponding [single project task](#single-project-builds) for each subproject (for 
example,
`dokkaHtml`), and merges all outputs into a single virtual project.

The resulting documentation looks as if you have a single project
build that contains all declarations from the subprojects.

> Use the `dokkaJavadocCollector` task if you need to create Javadoc documentation for your multi-project build.
>
{type="tip"}

#### Collector results

Given a project with the following structure:

```text
parentProject
    └── childProjectA
        ├── demo
            ├── ChildProjectAClass
    └── childProjectB
        ├── demo
            ├── ChildProjectBClass
```

These pages are generated after running `dokkaHtmlCollector`:

![Screenshot for output of dokkaHtmlCollector task](dokkaHtmlCollector-example.png){width=800}

See our [multi-module project example](https://github.
com/Kotlin/dokka/tree/master/examples/gradle/dokka-multimodule-example)
for more details.

## Building javadoc.jar

In order to publish your library to a repository, you may need to provide a `javadoc.jar` file that contains API
reference documentation.

Dokka's Gradle plugin does not provide any way to do this out of the box, but it can be achieved with custom Gradle
tasks. One for generating documentation in [HTML](html.md) format and another one for [Javadoc](javadoc.md) format:

<tabs group="build-script">
<tab title="Kotlin" group-key="kotlin">

```kotlin
tasks.register<Jar>("dokkaHtmlJar") {
    dependsOn(tasks.dokkaHtml)
    from(tasks.dokkaHtml.flatMap { it.outputDirectory })
    archiveClassifier.set("html-docs")
}

tasks.register<Jar>("dokkaJavadocJar") {
    dependsOn(tasks.dokkaJavadoc)
    from(tasks.dokkaJavadoc.flatMap { it.outputDirectory })
    archiveClassifier.set("javadoc")
}
```

</tab>
<tab title="Groovy" group-key="groovy">

```groovy
tasks.register('dokkaHtmlJar', Jar.class) {
    dependsOn(dokkaHtml)
    from(dokkaHtml)
    archiveClassifier.set("html-docs")
}

tasks.register('dokkaJavadocJar', Jar.class) {
    dependsOn(dokkaJavadoc)
    from(dokkaJavadoc)
    archiveClassifier.set("javadoc")
}
```

</tab>
</tabs>

> If you publish your library to Maven Central, you can use services like [javadoc.io](https://javadoc.io/) to
> host your library's API documentation for free and without any setup. It takes documentation pages straight
> from the artifact. It works with both HTML and Javadoc formats as demonstrated in
> [this example](https://javadoc.io/doc/com.trib3/server/latest/index.html).
>
{type="tip"}

## Configuration

You can configure tasks and output formats individually:

<tabs group="build-script">
<tab title="Kotlin" group-key="kotlin">

Applying the Dokka plugin via the [plugins DSL](#applying-the-plugin) block:

```kotlin
tasks.dokkaHtml {
    outputDirectory.set(buildDir.resolve("documentation/html"))
}

tasks.dokkaGfm {
    outputDirectory.set(buildDir.resolve("documentation/markdown"))
}

tasks.dokkaHtmlPartial {
    outputDirectory.set(buildDir.resolve("docs/partial"))
}
```

</tab>
<tab title="Groovy" group-key="groovy">

```groovy

dokkaHtml {
    outputDirectory.set(file("build/documentation/html"))
}

dokkaGfm {
    outputDirectory.set(file("build/documentation/markdown"))
}

dokkaHtmlPartial {
    outputDirectory.set(file("build/docs/partial"))
}
```

</tab>
</tabs>

Alternatively, you can configure all tasks and output formats at the same time, including [MultiModule](#multi-project-builds),
[Partial](#partial-tasks) and [Collector](#collector-tasks) tasks:

<tabs group="build-script">
<tab title="Kotlin" group-key="kotlin">

```kotlin
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.gradle.DokkaTaskPartial
import org.jetbrains.dokka.DokkaConfiguration.Visibility

// configure all dokka tasks, including multimodule, 
// partial and collector ones
tasks.withType<DokkaTask>().configureEach {
    dokkaSourceSets.configureEach {
        documentedVisibilities.set(
            setOf(
                Visibility.PUBLIC,
                Visibility.PROTECTED,
            )
        )

        perPackageOption {
            matchingRegex.set(".*internal.*")
            suppress.set(true)
        }
    }
}

// Configure partial tasks of all output formats. 
// These can have subproject-specific settings.
tasks.withType(DokkaTaskPartial::class).configureEach {
    dokkaSourceSets.configureEach {
        includes.from("README.md")
    }
}
```

</tab>
<tab title="Groovy" group-key="groovy">

```groovy
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.gradle.DokkaTaskPartial

// Configure all Dokka tasks, including multimodule, 
// partial and collector tasks
tasks.withType(DokkaTask.class) {
    dokkaSourceSets.configureEach {
        documentedVisibilities.set([
                DokkaConfiguration.Visibility.PUBLIC,
                DokkaConfiguration.Visibility.PROTECTED
        ])

        perPackageOption {
            matchingRegex.set(".*internal.*")
            suppress.set(true)
        }
    }
}

// Configure partial tasks of all output formats.
// These can have subproject-specific settings.
tasks.withType(DokkaTaskPartial.class) {
    dokkaSourceSets.configureEach {
        includes.from("README.md")
    }
}
```

</tab>
</tabs>

## Configuration options

Dokka has many configuration options to tailor your and your reader's experience. 

Below are some examples and detailed descriptions for each configuration section. You can also find an example
with [all configuration options](#complete-configuration) applied.

### General configuration

Here is an example of general configuration of any Dokka task, regardless of source set or package:

<tabs group="build-script">
<tab title="Kotlin" group-key="kotlin">

```kotlin
import org.jetbrains.dokka.gradle.DokkaTask

tasks.withType<DokkaTask>().configureEach {
    moduleName.set(project.name)
    moduleVersion.set(project.version.toString())
    outputDirectory.set(buildDir.resolve("dokka/$name"))
    failOnWarning.set(false)
    suppressObviousFunctions.set(true)
    suppressInheritedMembers.set(false)
    offlineMode.set(false)
    
    // ..
    // source set configuration section
    // ..
}
```

</tab>
<tab title="Groovy" group-key="groovy">

```groovy
import org.jetbrains.dokka.gradle.DokkaTask

tasks.withType(DokkaTask.class) {
    moduleName.set(project.name)
    moduleVersion.set(project.version.toString())
    outputDirectory.set(file("build/dokka/$name"))
    failOnWarning.set(false)
    suppressObviousFunctions.set(true)
    suppressInheritedMembers.set(false)
    offlineMode.set(false)

    // ..
    // source set configuration section
    // ..
}
```

</tab>
</tabs>

<deflist>
    <def title="moduleName">
        <p>The display name used to refer to the module. It is used for the table of contents, navigation, logging, etc.</p>
        <p>If set for a single-project build or a MultiModule task, it is used as the project name.</p>
        <p>The default is the Gradle project name.</p>
    </def>
    <def title="moduleVersion">
        <p>
            The module version. If set for a single-project build or a MultiModule task, it is used as the project version 
            by the versioning plugin.
        </p>
        <p>Default: Gradle project version.</p>
    </def>
    <def title="outputDirectory">
        <p>The directory where documentation is generated, regardless of format. It can be set on a per-task basis.</p>
        <p>
            The default is <code>project/buildDir/format</code>, where <code>format</code> is the task name with
            the "dokka" prefix removed. For the <code>dokkaHtmlMultiModule</code> task, it is 
            <code>project/buildDir/htmlMultiModule</code>.
        </p>
    </def>
    <def title="failOnWarning">
        <p>
            Whether to fail documentation generation if Dokka has emitted a warning or an error.
            The process waits until all errors and warnings have been emitted first.
        </p>
        <p>This setting works well with <code>reportUndocumented</code></p>
        <p>Default: <code>false</code>.</p>
    </def>
    <def title="suppressObviousFunctions">
        <p>Whether to suppress obvious functions.</p>
        <p>
            A function is considered to be obvious if it is:
            <list>
                <li>
                    Inherited from <code>kotlin.Any</code>, <code>Kotlin.Enum</code>, <code>java.lang.Object</code> or
                    <code>java.lang.Enum</code>, such as <code>equals</code>, <code>hashCode</code>, <code>toString</code>.
                </li>
                <li>
                    Synthetic (generated by the compiler) and does not have any documentation, such as
                    <code>dataClass.componentN</code> or <code>dataClass.copy</code>.
                </li>
            </list>
        </p>
        <p>Default: <code>true</code>.</p>
    </def>
    <def title="suppressInheritedMembers">
        <p>Whether to suppress inherited members that aren't explicitly overridden in a given class.</p>
        <p>
            Note: this can suppress functions such as <code>equals</code> / <code>hashCode</code> / <code>toString</code>, 
            but cannot suppress synthetic functions such as <code>dataClass.componentN</code> and 
            <code>dataClass.copy</code>. Use <code>suppressObviousFunctions</code>
            for that.
        </p>
        <p>Default: <code>false</code>.</p>
    </def>
    <def title="offlineMode">
        <p>Whether to resolve remote files/links over your network.</p>
        <p>
            This includes package-lists used for generating external documentation links. 
            For example, to make classes from standard library clickable. 
        </p>
        <p>
            Setting this to <code>true</code> can significantly speed up build times in certain cases,
            but can also worsen documentation quality and user experience. For example, by
            not resolving some dependency's class/member links.
        </p>
        <p>
            Note: you can cache fetched files locally and provide them to
            Dokka as local paths. See <code>externalDocumentationLinks</code> section.
        </p>
        <p>Default: <code>false</code>.</p>
    </def>
</deflist>

### Source set configuration

Here is an example of how to configure Kotlin [source sets](https://kotlinlang.org/docs/multiplatform-discover-project.html#source-sets).

<tabs group="build-script">
<tab title="Kotlin" group-key="kotlin">

```kotlin
import org.jetbrains.dokka.DokkaConfiguration.Visibility
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.Platform
import java.net.URL

tasks.withType<DokkaTask>().configureEach {
    // ..
    // general configuration section
    // ..
    
    dokkaSourceSets.configureEach {
        suppress.set(false)
        displayName.set(name)
        documentedVisibilities.set(setOf(Visibility.PUBLIC))
        reportUndocumented.set(false)
        skipEmptyPackages.set(true)
        skipDeprecated.set(false)
        suppressGeneratedFiles.set(true)
        jdkVersion.set(8)
        languageVersion.set("1.7")
        apiVersion.set("1.7")
        noStdlibLink.set(false)
        noJdkLink.set(false)
        noAndroidSdkLink.set(false)
        includes.from(project.files(), "packages.md", "extra.md")
        platform.set(Platform.DEFAULT)
        sourceRoots.from(file("src"))
        classpath.from(project.files(), file("libs/dependency.jar"))
        samples.from(project.files(), "samples/Basic.kt", "samples/Advanced.kt")

        sourceLink {
            // Source link section
        }
        externalDocumentationLink {
            // External documentation link section
        }
        perPackageOption {
            // Package options section
        }
    }
}
```

</tab>
<tab title="Groovy" group-key="groovy">

```groovy
import org.jetbrains.dokka.DokkaConfiguration.Visibility
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.Platform
import java.net.URL

tasks.withType(DokkaTask.class) {
    // ..
    // general configuration section
    // ..

    dokkaSourceSets.configureEach {
        suppress.set(false)
        displayName.set(name)
        documentedVisibilities.set([Visibility.PUBLIC])
        reportUndocumented.set(false)
        skipEmptyPackages.set(true)
        skipDeprecated.set(false)
        suppressGeneratedFiles.set(true)
        jdkVersion.set(8)
        languageVersion.set("1.7")
        apiVersion.set("1.7")
        noStdlibLink.set(false)
        noJdkLink.set(false)
        noAndroidSdkLink.set(false)
        includes.from(project.files(), "packages.md", "extra.md")
        platform.set(Platform.DEFAULT)
        sourceRoots.from(file("src"))
        classpath.from(project.files(), file("libs/dependency.jar"))
        samples.from(project.files(), "samples/Basic.kt", "samples/Advanced.kt")

        sourceLink {
            // Source link section
        }
        externalDocumentationLink {
            // External documentation link section
        }
        perPackageOption {
            // Package options section
        }
    }
}
```

</tab>
</tabs>

<deflist>
    <def title="suppress">
        <p>Whether this source set should be skipped when generating documentation.</p>
        <p>Default: <code>false</code>.</p>
    </def>
    <def title="displayName">
        <p>The display name used to refer to the source set.</p>
        <p>
            The name is used both externally (for example, as source set name visible to documentation readers) and 
            internally (for example, for logging messages of <code>reportUndocumented</code>).
        </p>
        <p>By default, the value is deduced from information provided by the Kotlin Gradle plugin.</p>
    </def>
    <def title="documentedVisibilities">
        <p>The set of visibility modifiers that should be documented.</p>
        <p>
            This can be used if you want to document protected/internal/private declarations,
            as well as if you want to exclude public declarations and only document internal API.
        </p>
        <p>Can be configured on per-package basis.</p>
        <p>Default: <code>DokkaConfiguration.Visibility.PUBLIC</code>.</p>
    </def>
    <def title="reportUndocumented">
        <p>
            Whether to emit warnings about visible undocumented declarations, that is declarations without KDocs
            after they have been filtered by <code>documentedVisibilities</code>.
        </p>
        <p>This setting works well with <code>failOnWarning</code>. It can be overridden for a specific package.</p>
        <p>Default: <code>false</code>.</p>
    </def>
    <def title="skipEmptyPackages">
        <p>
            Whether to skip packages that contain no visible declarations after
            various filters have been applied.
        </p>
        <p>
            For example, if <code>skipDeprecated</code> is set to <code>true</code> and your package contains only
            deprecated declarations, it is considered to be empty.
        </p>
        <p>Default: <code>true</code>.</p>
    </def>
    <def title="skipDeprecated">
        <p>Whether to document declarations annotated with <code>@Deprecated</code>.</p>
        <p>It can be overridden at package level.</p>
        <p>Default: <code>false</code>.</p>
    </def>
    <def title="suppressGeneratedFiles">
        <p>Whether to document/analyze generated files.</p>
        <p>
            Generated files are expected to be present under the <code>{project}/{buildDir}/generated</code> directory.
            If set to <code>true</code>, it effectively adds all files from that directory to the
            <code>suppressedFiles</code> option, so you can configure it manually.
        </p>
        <p>Default: <code>true</code>.</p>
    </def>
    <def title="jdkVersion">
        <p>The JDK version to use when generating external documentation links for Java types.</p>
        <p>
            For example, if you use <code>java.util.UUID</code> from the JDK in some public declaration signature,
            and this property is set to <code>8</code>, Dokka generates an external documentation link
            to <a href="https://docs.oracle.com/javase/8/docs/api/java/util/UUID.html">JDK 8 Javadocs</a> for it.
        </p>
        <p>Default: JDK 8.</p>
    </def>
    <def title="languageVersion">
        <p>
            <a href="https://kotlinlang.org/docs/compatibility-modes.html">The Kotlin language version</a>
            used for setting up analysis and <a href="https://kotlinlang.org/docs/kotlin-doc.html#sample-identifier">@sample</a>
            environment.
        </p>
        <p>By default, the latest language version available to Dokka's embedded compiler is used.</p>
    </def>
    <def title="apiVersion">
        <p>
            <a href="https://kotlinlang.org/docs/compatibility-modes.html">The Kotlin API version</a>
            used for setting up analysis and <a href="https://kotlinlang.org/docs/kotlin-doc.html#sample-identifier">@sample</a>
            environment.
        </p>
        <p>By default, it is deduced from <code>languageVersion</code>.</p>
    </def>
    <def title="noStdlibLink">
        <p>
            Whether to generate external documentation links that lead to API reference
            documentation for Kotlin's standard library when declarations from it are used.
        </p>
        <p>Links are generated when `noStdLibLink` is set to <code>false</code>.</p>
        <p>Default: <code>false</code>.</p>
    </def>
    <def title="noJdkLink">
        <p>Whether to generate external documentation links to JDK's Javadocs when declarations from it are used.</p>
        <p>The version of JDK Javadocs is determined by the <code>jdkVersion</code> property.</p>
        <p>Links are generated when `noJdkLink` is set to <code>false</code>.</p>
        <p>Default: <code>false</code>.</p>
    </def>
    <def title="noAndroidSdkLink">
        <p>
            Whether to generate external documentation links for Android SDK API reference
            when declarations from it are used.
        </p>
        <p>This is only relevant in Android projects, ignored otherwise.</p>
        <p>Links are generated when `noAndroidSdkLink` is set to <code>false</code>.</p>
        <p>Default: <code>false</code>.</p>
    </def>
    <def title="includes">
        <p>
            A list of Markdown files that contain
            <a href="https://kotlinlang.org/docs/reference/kotlin-doc.html#module-and-package-documentation">module and package documentation</a>.
        </p>
        <p>The contents of the specified files are parsed and embedded into documentation as module and package descriptions.</p>
        <p>
            See <a href="https://github.com/Kotlin/dokka/tree/master/examples/gradle/dokka-gradle-example">Dokka gradle example</a>
            for an example of how to use it and what it looks like.
        </p>
    </def>
    <def title="platform">
        <p>
            The platform to be used for setting up code analysis and 
            <a href="https://kotlinlang.org/docs/kotlin-doc.html#sample-identifier">@sample</a> environment.
        </p>
        <p>The default value is deduced from information provided by the Kotlin Gradle plugin.</p>
    </def>
    <def title="sourceRoots">
        <p>
            The source code roots to be analyzed and documented.
            Acceptable formats are directories and individual <code>.kt</code> / <code>.java</code> files.
        </p>
        <p>By default, source roots are deduced from information provided by the Kotlin Gradle plugin.</p>
    </def>
    <def title="classpath">
        <p>The classpath for analysis and interactive samples.</p>
        <p>
            This iu Useful if some types that come from dependencies are not resolved/picked up automatically.
            The property accepts both <code>.jar</code> and <code>.klib</code> files.
        </p>
        <p>By default, classpath is deduced from information provided by the Kotlin Gradle plugin.</p>
    </def>
    <def title="samples">
        <p>
            A list of directories or files that contain sample functions which are referenced via
            <a href="https://kotlinlang.org/docs/kotlin-doc.html#sample-identifier">@sample</a> KDoc tag.
        </p>
    </def>
</deflist>

### Source link configuration

The `sourceLinks` configuration block is where you can add a `source` link to each signature
that leads to a `remoteUrl` with a specific line number. (The line number is configurable by setting `remoteLineSuffix`).
This helps readers to find the source code for each declaration.

For an example, see the documentation for the
[`count()`](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/count.html)
function in `kotlinx.coroutines`.

<tabs group="build-script">
<tab title="Kotlin" group-key="kotlin">

```kotlin
import org.jetbrains.dokka.gradle.DokkaTask
import java.net.URL

tasks.withType<DokkaTask>().configureEach {
    // ..
    // general configuration section
    // ..
    
    dokkaSourceSets.configureEach {
        // ..
        // source set configuration section
        // ..
        
        sourceLink {
            localDirectory.set(projectDir.resolve("src"))
            remoteUrl.set(URL("https://github.com/kotlin/dokka/tree/master/src/main/kotlin"))
            remoteLineSuffix.set("#L")
        }
    }
}
```

</tab>
<tab title="Groovy" group-key="groovy">

```groovy
import org.jetbrains.dokka.gradle.DokkaTask
import java.net.URL

tasks.withType(DokkaTask.class) {
    // ..
    // general configuration section
    // ..
    
    dokkaSourceSets.configureEach {
        // ..
        // source set configuration section
        // ..
        
        sourceLink {
            localDirectory.set(file("src"))
            remoteUrl.set(new URL("https://github.com/kotlin/dokka/tree/master/src/main/kotlin"))
            remoteLineSuffix.set("#L")
        }
    }
}
```

</tab>
</tabs>

<deflist>
    <def title="localDirectory">
        <p>
            The path to the local source directory. The path must be relative to the root of 
            the current project.
        </p>
    </def>
    <def title="remoteUrl">
        <p>
            The URL of the source code hosting service that can be accessed by documentation readers,
            like GitHub, GitLab, Bitbucket, etc. This URL is used to generate
            source code links of declarations.
        </p>
    </def>
    <def title="remoteLineSuffix">
        <p>
            The suffix used to append the source code line number to the URL. This helps readers navigate
            not only to the file, but to the specific line number of the declaration.
        </p>
        <p>
            The number itself is appended to the specified suffix. For example,
            if this property is set to <code>#L</code> and the line number is 10, the resulting URL suffix
            is <code>#L10</code>.
        </p>
        <p>
            Suffixes used by popular services:
            <list>
                <li>GitHub: <code>#L</code></li>
                <li>GitLab: <code>#L</code></li>
                <li>Bitbucket: <code>#lines-</code></li>
            </list>
        </p>
        <p>Default: <code>#L</code>.</p>
    </def>
</deflist>

### Package options

Here is an example of a configuration block that allows setting some options for specific packages matched by `matchingRegex`.

<tabs group="build-script">
<tab title="Kotlin" group-key="kotlin">

```kotlin
import org.jetbrains.dokka.DokkaConfiguration.Visibility
import org.jetbrains.dokka.gradle.DokkaTask

tasks.withType<DokkaTask>().configureEach {
    // ..
    // general configuration section
    // ..
    
    dokkaSourceSets.configureEach {
        // ..
        // source set configuration section
        // ..
        
        perPackageOption {
            matchingRegex.set(".*api.*")
            suppress.set(false)
            skipDeprecated.set(false)
            reportUndocumented.set(false)
            documentedVisibilities.set(setOf(Visibility.PUBLIC))
        }
    }
}
```

</tab>
<tab title="Groovy" group-key="groovy">

```groovy
import org.jetbrains.dokka.DokkaConfiguration.Visibility
import org.jetbrains.dokka.gradle.DokkaTask

tasks.withType(DokkaTask.class) {
    // ..
    // General configuration section
    // ..
    
    dokkaSourceSets.configureEach {
        // ..
        // Source set configuration section
        // ..
        
        perPackageOption {
            matchingRegex.set(".*api.*")
            suppress.set(false)
            skipDeprecated.set(false)
            reportUndocumented.set(false)
            documentedVisibilities.set([Visibility.PUBLIC])
        }
    }
}
```

</tab>
</tabs>

<deflist>
    <def title="matchingRegex">
        <p>The regular expression that is used to match the package.</p>
        <p>Default: any string: <code>.*</code>.</p>
    </def>
    <def title="suppress">
        <p>Whether this package should be skipped when generating documentation.</p>
        <p>Default: <code>false</code>.</p>
    </def>
    <def title="skipDeprecated">
        <p>Whether to document declarations annotated with <code>@Deprecated</code>.</p>
        <p>This can be configured on source set level.</p>
        <p>Default: <code>false</code>.</p>
    </def>
    <def title="reportUndocumented">
        <p>
            Whether to emit warnings about visible undocumented declarations. That is declarations from
            this package and without KDocs, after they have been filtered by <code>documentedVisibilities</code>.
        </p>
        <p>This setting works well with <code>failOnWarning</code>.</p>
        <p>This can be configured on source set level.</p>
        <p>Default: <code>false</code>.</p>
    </def>
    <def title="documentedVisibilities">
        <p>The set of visibility modifiers that should be documented.</p>
        <p>
            This can be used if you want to document protected/internal/private declarations within a
            specific package, as well as if you want to exclude public declarations and only document internal API.
        </p>
        <p>This can be configured on source set level.</p>
        <p>Default: <code>DokkaConfiguration.Visibility.PUBLIC</code>.</p>
    </def>
</deflist>

### External documentation links configuration

The externalDocumentationLink` block allows the creation of links that lead to the externally hosted documentation of your dependencies.

For example, if you are using types from `kotlinx.serialization`, by default they are unclickable in your
documentation, as if they are unresolved. However, since the API reference for `kotlinx.serialization` is also built by Dokka and is
[published on kotlinlang.org](https://kotlinlang.org/api/kotlinx.serialization/), you can configure external
documentation links for it. Thus allowing Dokka to generate links for types, making them clickable
and resolve successfully.

<tabs group="build-script">
<tab title="Kotlin" group-key="kotlin">

```kotlin
import org.jetbrains.dokka.gradle.DokkaTask
import java.net.URL

tasks.withType<DokkaTask>().configureEach {
    // ..
    // general configuration section
    // ..
    
    dokkaSourceSets.configureEach {
        // ..
        // source set configuration section
        // ..
        
        externalDocumentationLink {
            url.set(URL("https://kotlinlang.org/api/kotlinx.serialization/"))
            packageListUrl.set(
                rootProject.projectDir.resolve("serialization.package.list").toURL()
            )
        }
    }
}
```

</tab>
<tab title="Groovy" group-key="groovy">

```groovy
import org.jetbrains.dokka.gradle.DokkaTask
import java.net.URL

tasks.withType(DokkaTask.class) {
    // ..
    // general configuration section
    // ..
    
    dokkaSourceSets.configureEach {
        // ..
        // source set configuration section
        // ..
        
        externalDocumentationLink {
            url.set(new URL("https://kotlinlang.org/api/kotlinx.serialization/"))
            packageListUrl.set(
                file("serialization.package.list").toURL()
            )
        }
    }
}
```

</tab>
</tabs>

<deflist>
    <def title="url">
        <p>The root URL of documentation to link with. It **must** contain a trailing slash.</p>
        <p>
            Dokka does its best to automatically find <code>package-list</code> for the given URL, 
            and link declarations together.
        </p>
        <p>
            If automatic resolution fails or if you want to use locally cached files instead, 
            consider providing <code>packageListUrl</code>.
        </p>
    </def>
    <def title="packageListUrl">
        <p>
            Specifies the exact location of a <code>package-list</code> instead of relying on Dokka
            automatically resolving it. It can also be a locally cached file to avoid network calls.
        </p>
    </def>
</deflist>

### Complete configuration

Below you can see all possible configuration options applied at the same time.

<tabs group="build-script">
<tab title="Kotlin" group-key="kotlin">

```kotlin
import org.jetbrains.dokka.DokkaConfiguration.Visibility
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.Platform
import java.net.URL

tasks.withType<DokkaTask>().configureEach {
    moduleName.set(project.name)
    moduleVersion.set(project.version.toString())
    outputDirectory.set(buildDir.resolve("dokka/$name"))
    failOnWarning.set(false)
    suppressObviousFunctions.set(true)
    suppressInheritedMembers.set(false)
    offlineMode.set(false)

    dokkaSourceSets {
        named("customSourceSet") {
            dependsOn("sourceSetDependency")
        }
        configureEach {
            suppress.set(false)
            displayName.set(name)
            documentedVisibilities.set(setOf(Visibility.PUBLIC))
            reportUndocumented.set(false)
            skipEmptyPackages.set(true)
            skipDeprecated.set(false)
            suppressGeneratedFiles.set(true)
            jdkVersion.set(8)
            languageVersion.set("1.7")
            apiVersion.set("1.7")
            noStdlibLink.set(false)
            noJdkLink.set(false)
            noAndroidSdkLink.set(false)
            includes.from(project.files(), "packages.md", "extra.md")
            platform.set(Platform.DEFAULT)
            sourceRoots.from(file("src"))
            classpath.from(project.files(), file("libs/dependency.jar"))
            samples.from(project.files(), "samples/Basic.kt", "samples/Advanced.kt")
            
            sourceLink {
                localDirectory.set(projectDir.resolve("src"))
                remoteUrl.set(URL("https://github.com/kotlin/dokka/tree/master/src/main/kotlin"))
                remoteLineSuffix.set("#L")
            }

            externalDocumentationLink {
                url.set(URL("https://kotlinlang.org/api/latest/jvm/stdlib/"))
                packageListUrl.set(
                    rootProject.projectDir.resolve("stdlib.package.list").toURL()
                )
            }

            perPackageOption {
                matchingRegex.set(".*api.*")
                suppress.set(false)
                skipDeprecated.set(false)
                reportUndocumented.set(false)
                documentedVisibilities.set(
                    setOf(
                        Visibility.PUBLIC,
                        Visibility.PRIVATE,
                        Visibility.PROTECTED,
                        Visibility.INTERNAL,
                        Visibility.PACKAGE
                    )
                )
            }
        }
    }
}
```

</tab>
<tab title="Groovy" group-key="groovy">

```groovy
import org.jetbrains.dokka.DokkaConfiguration.Visibility
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.Platform
import java.net.URL

tasks.withType(DokkaTask.class) {
    moduleName.set(project.name)
    moduleVersion.set(project.version.toString())
    outputDirectory.set(file("build/dokka/$name"))
    failOnWarning.set(false)
    suppressObviousFunctions.set(true)
    suppressInheritedMembers.set(false)
    offlineMode.set(false)

    dokkaSourceSets {
        named("customSourceSet") {
            dependsOn("sourceSetDependency")
        }
        configureEach {
            suppress.set(false)
            displayName.set(name)
            documentedVisibilities.set([Visibility.PUBLIC])
            reportUndocumented.set(false)
            skipEmptyPackages.set(true)
            skipDeprecated.set(false)
            suppressGeneratedFiles.set(true)
            jdkVersion.set(8)
            languageVersion.set("1.7")
            apiVersion.set("1.7")
            noStdlibLink.set(false)
            noJdkLink.set(false)
            noAndroidSdkLink.set(false)
            includes.from(project.files(), "packages.md", "extra.md")
            platform.set(Platform.DEFAULT)
            sourceRoots.from(file("src"))
            classpath.from(project.files(), file("libs/dependency.jar"))
            samples.from(project.files(), "samples/Basic.kt", "samples/Advanced.kt")

            sourceLink {
                localDirectory.set(file("src"))
                remoteUrl.set(new URL("https://github.com/kotlin/dokka/tree/master/src/main/kotlin"))
                remoteLineSuffix.set("#L")
            }

            externalDocumentationLink {
                url.set(new URL("https://kotlinlang.org/api/latest/jvm/stdlib/"))
                packageListUrl.set(
                        file("stdlib.package.list").toURL()
                )
            }

            perPackageOption {
                matchingRegex.set(".*api.*")
                suppress.set(false)
                skipDeprecated.set(false)
                reportUndocumented.set(false)
                documentedVisibilities.set([Visibility.PUBLIC])
            }
        }
    }
}
```

</tab>
</tabs>