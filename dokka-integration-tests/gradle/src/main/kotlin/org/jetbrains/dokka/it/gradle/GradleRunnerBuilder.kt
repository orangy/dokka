/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.it.gradle

import org.gradle.testkit.runner.GradleRunner
import org.gradle.util.GradleVersion
import java.io.File
import java.net.URI
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.DurationUnit.MILLISECONDS
import kotlin.time.DurationUnit.SECONDS
import kotlin.time.toDuration

fun GradleRunner(
    projectDir: File,
    testKitDir: File = File("build", "gradle-test-kit").absoluteFile,
    configure: GradleRunnerBuilder.() -> Unit
): GradleRunner {
    return GradleRunnerBuilder(
        projectDir =projectDir,
        testKitDir = testKitDir,
    ).apply(configure)
        .build()
}

class GradleRunnerBuilder(
    val projectDir: File,
    private val testKitDir: File ,
) {

    var gradleVersion: GradleVersion = GradleVersion.current()
    var debug: Boolean = false
    var forwardOutput: Boolean = false

    /**
     * Primarily used to set Gradle tasks.
     *
     * For consistency and legibility please use [gradleProperties] to set
     * system or Gradle properties, or add one if it is missing.
     */
    val arguments: MutableList<String> = mutableListOf()

    val gradleProperties = GradlePropertiesBuilder()
    fun gradleProperties(configure: GradlePropertiesBuilder.() -> Unit) {
        gradleProperties.configure()
    }

    init {
        gradleProperties {
            gradleParallel = false
            gradleConfigurationCache = false
            gradleBuildCache = false
            gradleMaxWorkers = 1

            useAndroidX = false
            useK2 = false
            dokkaVersion = System.getenv("DOKKA_VERSION") ?: error("DOKKA_VERSION not defined as environment variable")

            kotlinVersion

            gradleDaemonIdle = 60.seconds
            kotlinDaemonIdleShutdown = 60.seconds

            gradleJvmArgs {
                fileEncoding = "UTF-8"
                heapDumpOnOutOfMemoryError = true
            }
        }
    }

    fun build(): GradleRunner {
        return GradleRunner.create()
            .withProjectDir(projectDir)
            .forwardOutput(forwardOutput)
            .withJetBrainsCachedGradleVersion(gradleVersion)
            .withTestKitDir(testKitDir)
            .withDebug(debug)
            .withGradleProperties(gradleProperties)
            .withArguments(arguments)
//            .withArguments(
//                listOfNotNull(
//                    "-Porg.gradle.workers.max=1", // try to keep Gradle memory under control to prevent Metaspace OOMs (I'm not sure if this is effective!)
//                    "-Pdokka_it_dokka_version=${System.getenv("DOKKA_VERSION")}",
//                    "-Pdokka_it_kotlin_version=${kotlinVersion}",
//                    buildVersions.androidGradlePluginVersion?.let { androidVersion ->
//                        "-Pdokka_it_android_gradle_plugin_version=$androidVersion"
//                    },
//
//                    // Decrease Gradle daemon idle timeout, to help with OOM on CI because agents have limited memory
//                    "-Dorg.gradle.daemon.idletimeout=" + 60.seconds.inWholeMilliseconds, // default is 3 hours!
//                    "-Pkotlin.daemon.options.autoshutdownIdleSeconds=60",
//
//                    // property flag to use K2
//                    if (TestEnvironment.shouldUseK2())
//                        "-P${TestEnvironment.TRY_K2}=true"
//                    else
//                        null,
//
//                    * arguments
//                )
//            )
    }

    companion object {
        private fun GradleRunner.withJetBrainsCachedGradleVersion(version: GradleVersion): GradleRunner =
            withGradleDistribution(
                URI.create(
                    "https://cache-redirector.jetbrains.com/services.gradle.org/distributions/gradle-${version.version}-bin.zip"
                )
            )

        private fun GradleRunner.withGradleProperties(
            gradleProperties: GradlePropertiesBuilder
        ): GradleRunner = apply {
            projectDir.apply {
                mkdirs()
                val entries = gradleProperties.build()
                writeText( /* language=properties */ """
                    |# generated file - manual edits will be overwritten
                    |
                    |$entries
                    |
                    """.trimMargin()
                )
            }
        }

        private fun GradleRunner.forwardOutput(enabled: Boolean): GradleRunner =
            if (enabled) forwardOutput() else this
    }
}


class GradlePropertiesBuilder(
    private val content: MutableMap<String, String> = mutableMapOf()
) : MutableMap<String, String> by content {

    var gradleParallel: Boolean by flag("org.gradle.parallel")
    var gradleConfigurationCache: Boolean by flag("org.gradle.configuration-cache")
    var gradleBuildCache: Boolean by flag("org.gradle.caching")
    var gradleMaxWorkers: Int? by value("org.gradle.workers.max").int()
    var gradleDaemonIdle: Duration? by value("org.gradle.daemon.idletimeout").duration(MILLISECONDS)

    var kotlinDaemonIdleShutdown: Duration? by value("kotlin.daemon.options.autoshutdownIdleSeconds").duration(SECONDS)

    var dokkaVersion: String? by value("dokka_it_dokka_version")
    var kotlinVersion: String? by value("dokka_it_kotlin_version")
    var androidGradlePluginVersion: String? by value("dokka_it_android_gradle_plugin_version")

    var useK2: Boolean by flag("org.jetbrains.dokka.experimental.tryK2")

    var useAndroidX: Boolean by flag("android.useAndroidX")

    /** Define `org.gradle.jvmargs` */
    val gradleJvmArgs = JvmArgsBuilder()

    /** Define `org.gradle.jvmargs` */
    fun gradleJvmArgs(configure: JvmArgsBuilder.() -> Unit) {
        gradleJvmArgs.configure()
    }

    fun build(): String {
        val allContent = content + ("org.gradle.jvmargs" to gradleJvmArgs.build())
        return allContent.entries.joinToString("\n") { (k, v) -> "$k=$v" }
    }
}


class JvmArgsBuilder(
    private val content: MutableMap<String, String> = mutableMapOf()
) : MutableMap<String, String> by content {
    var fileEncoding by value("-D" + "file.encoding=")

    @Suppress("PropertyName")
    var Xmx: String? by value("-Xmx")
    var maxMetaspaceSize: String? by value("-XX:MaxMetaspaceSize=")
    var heapDumpOnOutOfMemoryError: Boolean by flag("-XX:+HeapDumpOnOutOfMemoryError")
    var abortVMOnCompilationFailure: Boolean by flag("-XX:+AbortVMOnCompilationFailure")

    fun build(): String =
        content.entries.joinToString(" ") { (k, v) ->
            if (v == "true" || v == "false") {
                k
            } else {
                "$k$v"
            }
        }
}

//private fun MutableMap<String, String>.value(
//    key: String,
//): MutableMapPropertyDelegateProvider<String?> =
//    value(key, encode = { it }, decode = { it })


private fun MutableMap<String, String>.value(
    key: String,
): MutableMapPropertyDelegate<String?> =
    MutableMapPropertyDelegate(
        key = key,
        get = { get(key) },
        set = { value -> if (value == null) remove(key) else set(key, value) },
    )

private fun MutableMap<String, String>.flag(
    key: String,
): MutableMapPropertyDelegate<Boolean> =
    MutableMapPropertyDelegate(
        key = key,
        get = { get(key) == "true" },
        set = { value -> set(key, value.toString()) },
    )

private class MutableMapPropertyDelegate<T>(
    val key: String,
    private val get: Context.() -> T,
    private val set: Context.(value: T) -> Unit,
) : PropertyDelegateProvider<MutableMap<String, String>, ReadWriteProperty<Any?, T>> {

    override fun provideDelegate(
        thisRef: MutableMap<String, String>,
        property: KProperty<*>
    ): ReadWriteProperty<Any?, T> {
        val context = Context(key = key, map = thisRef, property = property)

        return object : ReadWriteProperty<Any?, T> {
            override fun getValue(thisRef: Any?, property: KProperty<*>): T {
                return context.get()
            }

            override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
                if (value == null) {
                    context.map.remove(key)
                } else {
                    context.set(value)
                }
            }
        }
    }

    data class Context(
        val key: String,
        val map: MutableMap<String, String>,
        val property: KProperty<*>,
    )
}


private fun MutableMapPropertyDelegate<String?>.int(): ReadWriteProperty<MutableMap<String, String>, Int?> =
    map(encode = Int::toString, decode = String::toInt)

private fun MutableMapPropertyDelegate<String?>.duration(
    unit: DurationUnit
): ReadWriteProperty<MutableMap<String, String>, Duration?> =
    map(
        encode = { it.toLong(unit).toString() },
        decode = { it.toLong().toDuration(unit) }
    )


private fun <T> MutableMapPropertyDelegate<String?>.map(
    decode: (value: String) -> T,
    encode: (value: T) -> String,
): ReadWriteProperty<MutableMap<String, String>, T?> {
    return object : ReadWriteProperty<MutableMap<String, String>, T?> {
        override fun getValue(thisRef: MutableMap<String, String>, property: KProperty<*>): T? {
            val value = thisRef[key] ?: return null
            return decode(value)
        }

        override fun setValue(thisRef: MutableMap<String, String>, property: KProperty<*>, value: T?) {
            if (value == null) thisRef.remove(key) else thisRef[key] = encode(value)
        }
    }
}

//
//private fun <R, T> PropertyDelegateProvider(
//    get: ReadWritePropertyContext<R>.() -> T,
//    set: ReadWritePropertyContext<R>.(value: T) -> Unit,
//): PropertyDelegateProvider<R, ReadWriteProperty<R, T>> =
//    PropertyDelegateProvider { thisRef: R, property: KProperty<*> ->
//
//        val context = ReadWritePropertyContext(thisRef, property)
//
//        object : ReadWriteProperty<R, T> {
//            override fun getValue(thisRef: R, property: KProperty<*>): T =
//                context.get()
//
//            override fun setValue(thisRef: R, property: KProperty<*>, value: T) =
//                context.set(value)
//        }
//    }
//
//private data class ReadWritePropertyContext<R>(
//    val thisRef: R,
//    val property: KProperty<*>,
//)

//sealed interface JvmArg {
//    @JvmInline
//    value class Value(val value: String) : JvmArg
//    @JvmInline
//    value class Flag(val name: String) : JvmArg
//}
