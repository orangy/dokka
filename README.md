# Dokka 

[![Kotlin Beta](https://kotl.in/badges/beta.svg)](https://kotlinlang.org/docs/components-stability.html)
[![JetBrains official project](https://jb.gg/badges/official.svg)](https://github.com/JetBrains#jetbrains-on-github)
[![Maven Central](https://img.shields.io/maven-central/v/org.jetbrains.dokka/org.jetbrains.dokka.gradle.plugin?label=MavenCentral&logo=apache-maven)](https://search.maven.org/artifact/org.jetbrains.dokka/org.jetbrains.dokka.gradle.plugin)
[![Gradle Plugin](https://img.shields.io/gradle-plugin-portal/v/org.jetbrains.dokka?label=Gradle&logo=gradle)](https://plugins.gradle.org/plugin/org.jetbrains.dokka)
[![License](https://img.shields.io/github/license/Kotlin/dokka.svg)](LICENSE.txt)
[![Revved up by Develocity](https://img.shields.io/badge/Revved%20up%20by-Develocity-06A0CE?logo=Gradle&labelColor=02303A)](https://ge.jetbrains.com/scans?search.rootProjectNames=dokka)

Dokka is an API documentation engine for Kotlin.

Just like Kotlin itself, Dokka supports mixed-language projects. It understands Kotlin's
[KDoc comments](https://kotlinlang.org/docs/kotlin-doc.html#kdoc-syntax) and Java's
[Javadoc comments](https://www.oracle.com/technical-resources/articles/java/javadoc-tool.html).

Dokka can generate documentation in multiple formats, including its own modern [HTML format](#html),
multiple flavors of [Markdown](#markdown), and Java's [Javadoc HTML](#javadoc).

Some libraries that use Dokka for their API reference documentation:

* [kotlinx.coroutines](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/)
* [Bitmovin](https://cdn.bitmovin.com/player/android/3/docs/index.html)
* [Hexagon](https://hexagontk.com/stable/api/)
* [Ktor](https://api.ktor.io/)
* [OkHttp](https://square.github.io/okhttp/5.x/okhttp/okhttp3/)
* [Gradle](https://docs.gradle.org/current/kotlin-dsl/index.html)

You can run Dokka using [Gradle](https://kotlinlang.org/docs/dokka-gradle.html), 
[Maven](https://kotlinlang.org/docs/dokka-maven.html) or from the [command line](https://kotlinlang.org/docs/dokka-cli.html). 
It is also [highly pluggable](https://kotlinlang.org/docs/dokka-plugins.html).

## Documentation

Comprehensive documentation for Dokka is available on [kotlinlang.org](https://kotlinlang.org/docs/dokka-introduction.html)

## Get started with Dokka

### Gradle

<details open>
<summary>Kotlin DSL</summary>

Apply the Gradle plugin for Dokka in the root build script of your project:

```kotlin
plugins {
    id("org.jetbrains.dokka") version "2.0.0"
}
```

When documenting [multi-project](https://docs.gradle.org/current/userguide/multi_project_builds.html) builds, you need 
to apply the Gradle plugin for Dokka within subprojects as well:

```kotlin
subprojects {
    apply(plugin = "org.jetbrains.dokka")
}
```

</details>

<details>
<summary>Groovy DSL</summary>

Apply Gradle plugin for Dokka in the root project:

```groovy
plugins {
    id 'org.jetbrains.dokka' version '2.0.0'
}
```

When documenting [multi-project](https://docs.gradle.org/current/userguide/multi_project_builds.html) builds, you need 
to apply the Gradle plugin for Dokka within subprojects as well:

```groovy
subprojects {
    apply plugin: 'org.jetbrains.dokka'
}
```

</details>

To generate documentation, run the following Gradle tasks:

* `dokkaHtml` for single-project builds
* `dokkaHtmlMultiModule` for multi-project builds

By default, the output directory is set to `/build/dokka/html` and `/build/dokka/htmlMultiModule` respectively.

To learn more about the Gradle plugin for Dokka, see [documentation for Gradle](https://kotlinlang.org/docs/dokka-gradle.html).

### Maven

Add the Dokka Maven plugin to the `plugins` section of your POM file:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.jetbrains.dokka</groupId>
            <artifactId>dokka-maven-plugin</artifactId>
            <version>2.0.0</version>
            <executions>
                <execution>
                    <phase>pre-site</phase>
                    <goals>
                        <goal>dokka</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

To generate documentation, run the `dokka:dokka` goal.

By default, the output directory is set to `target/dokka`.

To learn more about using Dokka with Maven, see [documentation for Maven](https://kotlinlang.org/docs/dokka-maven.html).

### CLI

It is possible to run Dokka from the command line without having to use any of the build tools, but it's more
difficult to set up and for that reason it is not covered in this section.

Please consult [documentation for the command line runner](https://kotlinlang.org/docs/dokka-cli.html)
to learn how to use it.

### Android

In addition to applying and configuring Dokka, you can apply Dokka's 
[Android documentation plugin](dokka-subprojects/plugin-android-documentation/README.md),
which aims to improve documentation experience on the Android platform:

<details open>
<summary>Gradle Kotlin DSL</summary>

```kotlin
dependencies {
    dokkaPlugin("org.jetbrains.dokka:android-documentation-plugin:2.0.0")
}
```

</details>

<details>
<summary>Gradle Groovy DSL</summary>

```groovy
dependencies {
    dokkaPlugin 'org.jetbrains.dokka:android-documentation-plugin:2.0.0'
}
```

</details>

<details>
<summary>Maven</summary>

```xml
<plugin>
    <groupId>org.jetbrains.dokka</groupId>
    <artifactId>dokka-maven-plugin</artifactId>
    ...
    <configuration>
        <dokkaPlugins>
            <plugin>
                <groupId>org.jetbrains.dokka</groupId>
                <artifactId>android-documentation-plugin</artifactId>
                <version>2.0.0</version>
            </plugin>
        </dokkaPlugins>
    </configuration>
</plugin>
```

</details>

## Output formats

### HTML

HTML is Dokka's default and recommended output format. You can see an example of the output by browsing documentation 
for [kotlinx.coroutines](https://kotlinlang.org/api/kotlinx.coroutines/).

HTML format is configurable and, among other things, allows you to modify stylesheets, add custom image assets, change
footer message and revamp the structure of the generated HTML pages through templates.

For more details and examples, see [documentation for HTML format](https://kotlinlang.org/docs/dokka-html.html).

### Markdown

Dokka is able to generate documentation in GitHub Flavored and Jekyll compatible Markdown. However, both of these
formats are still in Alpha, so you might encounter bugs and migration issues.

For more details and examples, see [documentation for Markdown formats](https://kotlinlang.org/docs/dokka-markdown.html).

### Javadoc

Dokka's Javadoc output format is a lookalike of Java's 
[Javadoc HTML format](https://docs.oracle.com/en/java/javase/19/docs/api/index.html). This format is still in Alpha,
so you might encounter bugs and migration issues.

Javadoc format tries to visually mimic HTML pages generated by the Javadoc tool, but it's not a direct implementation 
or an exact copy. In addition, all Kotlin signatures are translated to Java signatures.

For more details and examples, see [documentation for Javadoc format](https://kotlinlang.org/docs/dokka-javadoc.html).

## Dokka plugins

Dokka was built from the ground up to be easily extensible and highly customizable, which allows the community to 
implement plugins for missing or very specific features that are not provided out of the box.

Learn more about Dokka plugins and their configuration in [Dokka plugins](https://kotlinlang.org/docs/dokka-plugins.html).

If you want to learn how to develop Dokka plugins, see
[Developer guides](https://kotlin.github.io/dokka/2.0.0/developer_guide/introduction/).

Dokka allows you to extend its functionality by [configuring custom plugins](https://github.com/Kotlin/dokka/blob/ae3840edb4e4afd7b3e3768a5fddfe8ec0e08f31/examples/gradle-v2/custom-dokka-plugin-example/demo-library/build.gradle.kts),
which enable additional processing or modifications to the documentation generation process.

## Community

Dokka has a dedicated `#dokka` channel in [Kotlin Community Slack](https://surveys.jetbrains.com/s3/kotlin-slack-sign-up)
where you can chat about Dokka, its plugins and how to develop them, as well as get in touch with maintainers.

## Building and Contributing

See [Contributing Guidelines](CONTRIBUTING.md)
