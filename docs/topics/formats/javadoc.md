[//]: # (title: Javadoc)

> The Javadoc output format is still in Alpha so you may find bugs and experience migration issues when using it. **You use it at your own risk.**
> Successful integration with tools that accept Java's Javadoc HTML as input is not guaranteed.
>
{type="warning"}

Dokka's Javadoc output format is similar to Java's
[Javadoc HTML format](https://docs.oracle.com/en/java/javase/19/docs/api/index.html). 

It tries to visually mimic HTML pages generated by the Javadoc tool, but it's not a direct implementation
or an exact copy.

![Screenshot of javadoc output format](javadoc-format-example.png){height=750}

All Kotlin code and signatures are rendered as seen from Java's perspective. This is achieved with our
[Kotlin as Java plugin](https://github.com/Kotlin/dokka/tree/master/plugins/kotlin-as-java), which comes bundled and
applied by default for this format.

The Javadoc output format is generated by our [Dokka plugin](plugins_introduction.md), which we actively maintain.
It is open source and you can find the source code on [GitHub](https://github.com/Kotlin/dokka/tree/master/plugins/javadoc).

## Generating Javadoc documentation

> The Javadoc format is not supported for multiplatform projects.
>
{type="warning"}


<tabs group="build-script">
<tab title="Gradle" group-key="kotlin">

Dokka's [Gradle plugin](gradle.md) comes with the Javadoc output format included. You can use the following tasks:

| **Task**                | **Description**                                                                                                                                                                                            |
|-------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `dokkaJavadoc`          | Generates Javadoc documentation for a single project.                                                                                                                                                      |
| `dokkaJavadocCollector` | A [Collector](gradle.md#collector-tasks) task created only for parent projects in multi-project builds. It calls `dokkaJavadoc` for every subproject and merges all outputs into a single virtual project. |

The `javadoc.jar` file can be generated separately. For more information, see [Building `javadoc.jar`](gradle.md#building-javadoc-jar).

</tab>
<tab title="Maven" group-key="groovy">

Dokka's [Maven plugin](maven.md) comes with the Javadoc output format built in. You can generate documentation
by using the following goals:

| **Goal**           | **Description**                                                              |
|--------------------|------------------------------------------------------------------------------|
| `dokka:javadoc`    | Generates documentation in Javadoc format                                    |
| `dokka:javadocJar` | Generates a `javadoc.jar` file that contains documentation in Javadoc format |


</tab>
<tab title="CLI" group-key="cli">

Since the Javadoc output format is generated by a [Dokka plugin](plugins_introduction.md#applying-dokka-plugins), you need to download the plugin as a
[JAR file](https://mvnrepository.com/artifact/org.jetbrains.dokka/javadoc-plugin/%dokkaVersion%).

The Javadoc output format has two dependencies that you need to provide as additional JAR files:

* [kotlin-as-java plugin](https://mvnrepository.com/artifact/org.jetbrains.dokka/kotlin-as-java-plugin/%dokkaVersion%)
* [korte-jvm](https://mvnrepository.com/artifact/com.soywiz.korlibs.korte/korte-jvm/3.3.0)

Via [command line arguments](cli.md#running-with-command-line-arguments):

```Bash
java -jar dokka-cli-%dokkaVersion%.jar \
     -pluginsClasspath "./dokka-base-%dokkaVersion%.jar;...;./javadoc-plugin-%dokkaVersion%.jar" \
     ...
```

Via [JSON configuration](cli.md#running-with-json-configuration):

```json
{
  ...
  "pluginsClasspath": [
    "./dokka-base-%dokkaVersion%.jar",
    "...",
    "./kotlin-as-java-plugin-%dokkaVersion%.jar",
    "./korte-jvm-3.3.0.jar",
    "./javadoc-plugin-%dokkaVersion%.jar"
  ],
  ...
}
```

For more information, see [Other output formats](cli.md#other-output-formats) in the CLI runner documentation.

</tab>
</tabs>