/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.it.gradle.junit

import org.gradle.testkit.runner.GradleRunner
import org.jetbrains.dokka.it.gradle.junit.DokkaGradlePluginTestExtension.CloseablePath.Companion.tmpDirCleanupMode
import org.jetbrains.dokka.it.gradle.junit.TestedVersions.Companion.dashSeparatedId
import org.jetbrains.dokka.it.gradle.junit.TestedVersions.Companion.displayName
import org.jetbrains.dokka.it.gradle.utils.SemVer
import org.jetbrains.dokka.it.gradle.withJetBrainsCachedGradleVersion
import org.jetbrains.dokka.it.gradle.withReadOnlyDependencyCache
import org.jetbrains.dokka.it.systemProperty
import org.junit.jupiter.api.extension.*
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource
import org.junit.jupiter.api.extension.support.TypeBasedParameterResolver
import org.junit.jupiter.api.io.CleanupMode
import org.junit.jupiter.api.io.TempDir
import org.junit.platform.commons.logging.Logger
import org.junit.platform.commons.logging.LoggerFactory
import org.junit.platform.commons.support.AnnotationSupport
import org.junit.platform.commons.support.AnnotationSupport.findAnnotation
import org.junit.platform.commons.support.AnnotationSupport.isAnnotated
import org.junit.platform.commons.support.ReflectionSupport
import org.opentest4j.TestAbortedException
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Stream
import kotlin.io.path.*
import kotlin.jvm.optionals.getOrNull
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import kotlin.streams.asStream

/**
 * JUnit Extension providing support for Dokka Gradle Plugin integration tests.
 *
 * To use this extension annotate the test function with [DokkaGradlePluginTest].
 *
 * The test function will be called multiple times, with different parameters.
 */
class DokkaGradlePluginTestExtension :
    TestTemplateInvocationContextProvider,
    BeforeAllCallback,
    BeforeEachCallback,
    TestWatcher {

    override fun beforeAll(context: ExtensionContext) {
        installFailureTracker(context)
    }

    override fun beforeEach(context: ExtensionContext) {
        installFailureTracker(context)
    }

    /**
     * Log the reason for aborted tests.
     *
     * Workaround for https://github.com/gradle/gradle/issues/5511.
     */
    override fun testAborted(context: ExtensionContext, cause: Throwable) {
        if (cause is TestAbortedException) {
            logger.warn { cause.message ?: "Test was aborted without reason." }
        }
    }

    override fun supportsTestTemplate(context: ExtensionContext): Boolean = true

    /**
     * Provide a stream of all [GradleProjectTestTemplate] invocations.
     * JUnit will run the test multiple times, once for each invocation.
     */
    override fun provideTestTemplateInvocationContexts(
        context: ExtensionContext
    ): Stream<TestTemplateInvocationContext> {

        // put all invocations into the same directory, with a human-readable name (based on the test display name)
        val baseDgpTestDir = Files.createTempDirectory("gradle-test")
            .resolve(context.displayName.replace(Regex("[^A-Za-z0-9]+"), "-"))
            .createDirectories()

        val dgpTest = findAnnotation(context.element, DokkaGradlePluginTest::class.java).get()

        val projectInitializer = ReflectionSupport.newInstance(dgpTest.projectInitializer.java)
        val sourceProjectDir = dgpTest.sourceProjectName

        val isAndroidTest = context.hasOrParentHasAnnotation<TestsAndroid>()
        val isAndroidComposeTest = context.hasOrParentHasAnnotation<TestsAndroidCompose>()

        val gradleProperties = computeGradleProperties(
            context,
            dgpTest.gradlePropertiesProvider,
        )

        val testedVersionsSource = when {
            isAndroidComposeTest -> TestedVersionsSource.AndroidCompose
            isAndroidTest -> TestedVersionsSource.Android
            else -> TestedVersionsSource.Default
        }

        return testedVersionsSource.get().map { testedVersions ->

            // Use a separate directory for each invocation, named after the tested versions.
            val projectTmpDir = baseDgpTestDir.resolve(testedVersions.dashSeparatedId())
            context.gradleTestStore.put(
                "project-tmp-dir-${projectTmpDir.invariantSeparatorsPathString}",
                CloseablePath(projectTmpDir, context),
            )

            projectInitializer.initialize(
                source = templateProjectsDir.resolve(sourceProjectDir),
                destination = projectTmpDir,
                testedVersions = testedVersions,
                gradleProperties = gradleProperties,
            )

            // log a clickable URI link to console, so it's easier to view the tested project.
            logger.info { "Testing project ${projectTmpDir.toUri()}" }

            GradleProjectTestTemplate(
                projectDir = projectTmpDir,
                testedVersions = testedVersions,
            )
        }
            .asStream()
    }

    /**
     * A single invocation of a [DokkaGradlePluginTest] test.
     *
     * JUnit will run the test with the provided [projectDir] and [testedVersions].
     */
    private class GradleProjectTestTemplate(
        private val projectDir: Path,
        private val testedVersions: TestedVersions,
    ) : TestTemplateInvocationContext {
        override fun getDisplayName(invocationIndex: Int): String =
            "[$invocationIndex] ${testedVersions.displayName()}"

        override fun getAdditionalExtensions(): List<Extension> =
            listOf(
                GradleProjectParameterResolver(projectDir, testedVersions),
                TestedVersionsParameterResolver(testedVersions),
            )
    }

    /**
     * When a `@DokkaGradlePluginTest` function has a parameter of type [DokkaGradleProjectRunner],
     * this [ParameterResolver] will provide a value.
     */
    private class GradleProjectParameterResolver(
        private val projectDir: Path,
        private val testedVersions: TestedVersions,
    ) : TypeBasedParameterResolver<DokkaGradleProjectRunner>() {
        override fun resolveParameter(
            parameterContext: ParameterContext,
            extensionContext: ExtensionContext,
        ): DokkaGradleProjectRunner {
            val gradleRunner = setupGradleRunner(projectDir, testedVersions.gradle)
            return DokkaGradleProjectRunner(projectDir, gradleRunner)
        }
    }

    /**
     * When a `@DokkaGradlePluginTest` function has a parameter of type [TestedVersions],
     * this [ParameterResolver] will provide a value.
     */
    private class TestedVersionsParameterResolver(
        private val testedVersions: TestedVersions,
    ) : ParameterResolver {
        override fun supportsParameter(
            parameterContext: ParameterContext,
            extensionContext: ExtensionContext,
        ): Boolean = parameterContext.parameter.type.kotlin.isSubclassOf(TestedVersions::class)

        override fun resolveParameter(
            parameterContext: ParameterContext,
            extensionContext: ExtensionContext,
        ): TestedVersions = testedVersions
    }

    /**
     * Aggregate all provided Gradle properties into a single [Map].
     */
    private fun computeGradleProperties(
        context: ExtensionContext,
        gradlePropertiesProviderType: KClass<out GradlePropertiesProvider>,
    ): Map<String, String> {

        val baseProperties = ReflectionSupport.newInstance(gradlePropertiesProviderType.java).get()

        val testClassProviders = AnnotationSupport.findRepeatableAnnotations(
            context.requiredTestClass,
            WithGradleProperties::class.java,
        )

        val testFuncProviders = AnnotationSupport.findRepeatableAnnotations(
            context.requiredTestMethod,
            WithGradleProperties::class.java,
        )

        val allProviders = sequence {
            yieldAll(testClassProviders)
            yieldAll(testFuncProviders)
        }

        val additionalProperties = allProviders
            .distinct()
            .flatMap { it.providers.asList() }
            .map { provider -> ReflectionSupport.newInstance(provider.java) }
            .map { it.get() }
            .fold(emptyMap<String, String>()) { acc, map -> acc + map }

        return baseProperties + additionalProperties
    }

    /**
     * JUnit will automatically delete [path] on failures,
     * depending on the configured [tmpDirCleanupMode].
     *
     * (Re-implementation of [org.junit.jupiter.api.io.TempDir], because JUnit does not allow
     * combining extensions.)
     */
    private class CloseablePath(
        private val path: Path,
        private val context: ExtensionContext,
    ) : CloseableResource {
        override fun close() {
            if (
                tmpDirCleanupMode == CleanupMode.NEVER
                || (tmpDirCleanupMode == CleanupMode.ON_SUCCESS && context.selfOrChildFailed())
            ) {
                logger.info { "Skipping cleanup of temp dir $path due to cleanup mode:$tmpDirCleanupMode" }
                return
            }
            path.deleteRecursively()
        }

        companion object {
            private val logger: Logger = LoggerFactory.getLogger(CloseablePath::class.java)

            /**
             * Configure whether JUnit will delete [CloseablePath]s.
             * Re-uses the same property as [TempDir].
             */
            private val tmpDirCleanupMode: CleanupMode =
                System.getProperty(TempDir.DEFAULT_CLEANUP_MODE_PROPERTY_NAME)
                    ?.let { CleanupMode.valueOf(it) }
                    ?: CleanupMode.DEFAULT
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(DokkaGradlePluginTestExtension::class.java)

        private val NAMESPACE: ExtensionContext.Namespace =
            ExtensionContext.Namespace.create(DokkaGradlePluginTestExtension::class.java)

        /** Shortcut for getting the JUnit context store for [DokkaGradlePluginTestExtension]. */
        private val ExtensionContext.gradleTestStore: ExtensionContext.Store
            get() = getStore(NAMESPACE)

        //region test failure tracker
        /**
         * Keep track of any test failures, or if any child of a [ExtensionContext] has failed.
         *
         * Used to determine if the temporary project directory should be deleted after successes.
         * (E.g. don't delete failures when running on a local machine, to allow for investigation.)
         *
         * Inspired by [TempDirectory](https://github.com/junit-team/junit5/blob/r5.11.3/junit-jupiter-engine/src/main/java/org/junit/jupiter/engine/extension/TempDirectory.java).
         *
         * @see CloseablePath
         */
        private fun installFailureTracker(context: ExtensionContext) {
            context.gradleTestStore.put(FAILURE_TRACKER, CloseableResource {
                context.parent.ifPresent { parent: ExtensionContext ->
                    if (context.selfOrChildFailed()) {
                        parent.gradleTestStore.put(CHILD_FAILED, true)
                    }
                }
            })
        }

        /** @see [installFailureTracker] */
        private const val FAILURE_TRACKER: String = "failure.tracker"

        /** @see [installFailureTracker] */
        private const val CHILD_FAILED: String = "child.failed"

        /**
         * Determine if this [ExtensionContext] has an exception, or if a child context had an exception.
         *
         * @see [installFailureTracker] */
        private fun ExtensionContext.selfOrChildFailed(): Boolean {
            return executionException.isPresent
                    || gradleTestStore.getOrDefault(CHILD_FAILED, Boolean::class.javaPrimitiveType, false)
        }
        //endregion

        private fun setupGradleRunner(projectDir: Path, gradle: SemVer): GradleRunner {
            return GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withJetBrainsCachedGradleVersion(
                    // Gradle doesn't strictly follow SemVer (fun fact: Gradle is older than SemVer).
                    // If the patch is zero, Gradle doesn't include it.
                    if (gradle.patch == 0) gradle.majorAndMinorVersions else gradle.version
                )
                .withReadOnlyDependencyCache()
                .forwardOutput()
        }

        internal fun getAndroidSdkDir(): Path {
            val androidSdkValue = System.getenv("ANDROID_SDK_ROOT") ?: System.getenv("ANDROID_HOME")
            requireNotNull(androidSdkValue) { "ANDROID_SDK_ROOT or ANDROID_HOME must be set" }
            val androidSdkPath = Path(androidSdkValue)
            require(androidSdkPath.isDirectory()) { "Provided Android SDK dir '$androidSdkValue' is not a valid directory." }
            return androidSdkPath
        }

        /**
         * Root directory for all template projects.
         */
        internal val templateProjectsDir by systemProperty(::Path)

        /**
         * Check if this [ExtensionContext] or any of its parents is annotated with [T].
         */
        private inline fun <reified T : Annotation> ExtensionContext.hasOrParentHasAnnotation(): Boolean =
            generateSequence(this) { it.parent.getOrNull() }
                .any { isAnnotated(it.element, T::class.java) }
    }
}
