@file:Suppress("TestFunctionName")

package org.jetbrains.dokka.gradle

import com.android.build.gradle.internal.api.DefaultAndroidSourceSet
import org.gradle.api.Project
import org.gradle.kotlin.dsl.closureOf
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.dokka.*
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet
import java.net.URL
import kotlin.test.*

class GradleDokkaSourceSetBuilderTest {


    private val project = ProjectBuilder.builder().withName("root").build()

    @Test
    fun sourceSetId() {
        val sourceSet = GradleDokkaSourceSetBuilder("myName", project, "scopeId")
        assertEquals(
            DokkaSourceSetID("scopeId", "myName"), sourceSet.sourceSetID,
            "Expected sourceSet.sourceSetID to match output of DokkaSourceSetID factory function"
        )

        assertEquals(
            "scopeId/myName", sourceSet.sourceSetID.toString(),
            "Expected SourceSetId's string representation"
        )
    }

    @Test
    fun classpath() {
        val sourceSet = GradleDokkaSourceSetBuilder("", project)
        sourceSet.classpath.from(project.file("path/to/file.jar"))
        sourceSet.classpath.from(project.file("path/to/other.jar"))

        assertEquals(
            listOf(project.file("path/to/file.jar"), project.file("path/to/other.jar")), sourceSet.classpath.toList(),
            "Expected both file paths being present in classpath"
        )

        assertEquals(
            listOf(project.file("path/to/file.jar"), project.file("path/to/other.jar")),
            sourceSet.build().classpath.toList(),
            "Expected both file paths being present in built classpath"
        )
    }

    @Test
    fun displayName() {
        val sourceSet = GradleDokkaSourceSetBuilder("myName", project)
        assertNull(
            sourceSet.displayName.getSafe(),
            "Expected no ${GradleDokkaSourceSetBuilder::displayName.name} being set by default"
        )

        assertEquals(
            "myName", sourceSet.build().displayName,
            "Expected source set name being used for ${DokkaConfiguration.DokkaSourceSet::displayName.name} " +
                    "after building source set with no ${GradleDokkaSourceSetBuilder::displayName.name} being set"
        )

        sourceSet.displayName.set("displayName")
        assertEquals(
            "displayName", sourceSet.build().displayName,
            "Expected previously set ${GradleDokkaSourceSetBuilder::displayName.name} to be present after build"
        )
    }

    @Test
    fun `displayName default for sourceSet ending with Main`() {
        val sourceSet = GradleDokkaSourceSetBuilder("jvmMain", project)
        assertEquals(
            "jvm", sourceSet.build().displayName,
            "Expected 'Main' being stripped for source set display name after build"
        )
    }

    @Test
    fun sourceRoots() {
        val sourceSet = GradleDokkaSourceSetBuilder("", project)
        sourceSet.sourceRoots.from(project.file("root1"))
        sourceSet.sourceRoot(project.file("root2"))
        sourceSet.sourceRoot(project.file("root3").absolutePath)
        sourceSet.sourceRoot("root4")

        assertEquals(
            listOf("root1", "root2", "root3", "root4").map(project::file).toSet(),
            sourceSet.build().sourceRoots,
            "Expected all files being present"
        )

        sourceSet.build().sourceRoots.forEach { root ->
            assertTrue(
                root.startsWith(project.projectDir),
                "Expected all roots to be inside the projectDir\n" +
                        "projectDir: ${project.projectDir}\n" +
                        "root: ${root.absolutePath})"
            )
        }
    }

    @Test
    fun dependentSourceSets() {
        val sourceSet = GradleDokkaSourceSetBuilder("", project)
        assertEquals(emptySet(), sourceSet.build().dependentSourceSets, "Expected no dependent sourceSets by default")

        sourceSet.dependentSourceSets.add(sourceSet.DokkaSourceSetID("s1"))
        sourceSet.dependsOn("s2")
        sourceSet.dependsOn(sourceSet.DokkaSourceSetID("s3"))
        sourceSet.dependsOn(GradleDokkaSourceSetBuilder("s4", project))
        sourceSet.dependsOn(GradleDokkaSourceSetBuilder("s5", project).build())
        sourceSet.dependsOn(createDefaultKotlinSourceSet("s6"))
        sourceSet.dependsOn(DefaultAndroidSourceSet("s7", project, false))

        assertEquals(
            listOf(":/s1", ":/s2", ":/s3", ":/s4", ":/s5", ":/s6", ":/s7"),
            sourceSet.build().dependentSourceSets.map { it.toString() },
            "Expected all source sets being registered"
        )
    }

    private fun createDefaultKotlinSourceSet(displayName: String): DefaultKotlinSourceSet {
        return project.objects.newInstance(DefaultKotlinSourceSet::class.java, project, displayName)
    }

    @Test
    fun samples() {
        val sourceSet = GradleDokkaSourceSetBuilder("", project)
        assertEquals(emptySet(), sourceSet.build().samples, "Expected no default samples")
        sourceSet.samples.from(project.file("s1"))
        sourceSet.samples.from(project.file("s2"))
        assertEquals(
            setOf(project.file("s1"), project.file("s2")), sourceSet.build().samples,
            "Expected all samples being present after build"
        )
    }

    @Test
    fun includes() {
        val sourceSet = GradleDokkaSourceSetBuilder("", project)
        assertEquals(emptySet(), sourceSet.build().includes, "Expected no default includees")
        sourceSet.includes.from(project.file("i1"))
        sourceSet.includes.from(project.file("i2"))
        assertEquals(
            setOf(project.file("i1"), project.file("i2")), sourceSet.build().includes,
            "Expected all includes being present after build"
        )
    }

    @Test
    @Suppress("DEPRECATION")
    fun includeNonPublic() {
        val sourceSet = GradleDokkaSourceSetBuilder("", project)
        assertEquals(
            DokkaDefaults.includeNonPublic, sourceSet.build().includeNonPublic,
            "Expected default value for ${GradleDokkaSourceSetBuilder::includeNonPublic.name}"
        )

        sourceSet.includeNonPublic.set(!DokkaDefaults.includeNonPublic)
        assertEquals(
            !DokkaDefaults.includeNonPublic, sourceSet.build().includeNonPublic,
            "Expected flipped value for ${GradleDokkaSourceSetBuilder::includeNonPublic.name}"
        )
    }

    @Test
    fun documentedVisibilities() {
        val sourceSet = GradleDokkaSourceSetBuilder("", project)
        assertEquals(
            DokkaDefaults.documentedVisibilities, sourceSet.build().documentedVisibilities,
            "Expected default value for ${GradleDokkaSourceSetBuilder::documentedVisibilities.name}"
        )

        val visibilities = setOf(DokkaConfiguration.Visibility.PRIVATE, DokkaConfiguration.Visibility.INTERNAL)
        sourceSet.documentedVisibilities.set(visibilities)
        assertEquals(
            visibilities, sourceSet.build().documentedVisibilities,
            "Expected to see previously set value for ${GradleDokkaSourceSetBuilder::includeNonPublic.name}"
        )
    }

    @Test
    fun reportUndocumented() {
        val sourceSet = GradleDokkaSourceSetBuilder("", project)
        assertEquals(
            DokkaDefaults.reportUndocumented, sourceSet.build().reportUndocumented,
            "Expected default value for ${GradleDokkaSourceSetBuilder::reportUndocumented.name}"
        )

        sourceSet.reportUndocumented.set(!DokkaDefaults.reportUndocumented)
        assertEquals(
            !DokkaDefaults.reportUndocumented, sourceSet.build().reportUndocumented,
            "Expected flipped value for ${GradleDokkaSourceSetBuilder::reportUndocumented.name}"
        )
    }

    @Test
    fun jdkVersion() {
        val sourceSet = GradleDokkaSourceSetBuilder("", project)
        assertEquals(
            DokkaDefaults.jdkVersion, sourceSet.build().jdkVersion,
            "Expected default value for ${GradleDokkaSourceSetBuilder::jdkVersion.name}"
        )

        sourceSet.jdkVersion.set(DokkaDefaults.jdkVersion + 1)
        assertEquals(
            DokkaDefaults.jdkVersion + 1, sourceSet.build().jdkVersion,
            "Expected increased value for ${GradleDokkaSourceSetBuilder::jdkVersion.name}"
        )
    }

    @Test
    fun sourceLinks() {
        val sourceSet = GradleDokkaSourceSetBuilder("", project)
        assertEquals(emptySet(), sourceSet.build().sourceLinks, "Expected no default source links")

        sourceSet.sourceLinks.add(
            GradleSourceLinkBuilder(project).apply {
                this.remoteLineSuffix.set("ls1")
                this.localDirectory.set(project.file("p1"))
                this.remoteUrl.set(URL("https://u1"))
            })

        sourceSet.sourceLink {
            remoteLineSuffix.set("ls2")
            localDirectory.set(project.file("p2"))
            remoteUrl.set(URL("https://u2"))
        }

        sourceSet.sourceLink(project.closureOf<GradleSourceLinkBuilder> {
            this.remoteLineSuffix.set("ls3")
            this.localDirectory.set(project.file("p3"))
            this.remoteUrl.set(URL("https://u3"))
        })

        assertEquals(
            setOf(
                SourceLinkDefinitionImpl(
                    remoteLineSuffix = "ls1",
                    localDirectory = project.file("p1").absolutePath,
                    remoteUrl = URL("https://u1")
                ),
                SourceLinkDefinitionImpl(
                    remoteLineSuffix = "ls2",
                    localDirectory = project.file("p2").absolutePath,
                    remoteUrl = URL("https://u2")
                ),
                SourceLinkDefinitionImpl(
                    remoteLineSuffix = "ls3",
                    localDirectory = project.file("p3").absolutePath,
                    remoteUrl = URL("https://u3")
                )
            ),
            sourceSet.build().sourceLinks,
            "Expected all source links being present after build"
        )
    }

    @Test
    fun perPackageOptions() {
        val sourceSet = GradleDokkaSourceSetBuilder("", project)
        assertEquals(emptyList(), sourceSet.build().perPackageOptions, "Expected no default per package options")

        sourceSet.perPackageOptions.add(GradlePackageOptionsBuilder(project).apply {
            this.matchingRegex.set("p1.*")
        })

        sourceSet.perPackageOption {
            matchingRegex.set("p2.*")
        }

        sourceSet.perPackageOption(project.closureOf<GradlePackageOptionsBuilder> {
            this.matchingRegex.set("p3.*")
        })

        assertEquals(
            listOf("p1.*", "p2.*", "p3.*").map { matchingRegex ->
                PackageOptionsImpl(
                    matchingRegex = matchingRegex,
                    includeNonPublic = DokkaDefaults.includeNonPublic,
                    documentedVisibilities = DokkaDefaults.documentedVisibilities,
                    reportUndocumented = DokkaDefaults.reportUndocumented,
                    skipDeprecated = DokkaDefaults.skipDeprecated,
                    suppress = DokkaDefaults.suppress
                )
            },
            sourceSet.build().perPackageOptions,
            "Expected all package options being present after build"
        )
    }

    @Test
    fun externalDocumentationLink() {
        val sourceSet = GradleDokkaSourceSetBuilder("", project)
        sourceSet.noAndroidSdkLink.set(true)
        sourceSet.noJdkLink.set(true)
        sourceSet.noStdlibLink.set(true)
        assertEquals(
            emptySet(), sourceSet.build().externalDocumentationLinks,
            "Expected no default external documentation links"
        )

        sourceSet.externalDocumentationLinks.add(
            GradleExternalDocumentationLinkBuilder(project).apply {
                this.url.set(URL("https://u1"))
                this.packageListUrl.set(URL("https://pl1"))
            }
        )

        sourceSet.externalDocumentationLink {
            url.set(URL("https://u2"))
        }

        sourceSet.externalDocumentationLink(project.closureOf<GradleExternalDocumentationLinkBuilder> {
            url.set(URL("https://u3"))
        })

        sourceSet.externalDocumentationLink(url = "https://u4", packageListUrl = "https://pl4")
        sourceSet.externalDocumentationLink(url = URL("https://u5"))

        assertEquals(
            setOf(
                ExternalDocumentationLinkImpl(URL("https://u1"), URL("https://pl1")),
                ExternalDocumentationLinkImpl(URL("https://u2"), URL("https://u2/package-list")),
                ExternalDocumentationLinkImpl(URL("https://u3"), URL("https://u3/package-list")),
                ExternalDocumentationLinkImpl(URL("https://u4"), URL("https://pl4")),
                ExternalDocumentationLinkImpl(URL("https://u5"), URL("https://u5/package-list"))
            ),
            sourceSet.build().externalDocumentationLinks,
            "Expected all external documentation links being present after build"
        )
    }

    @Test
    fun languageVersion() {
        val sourceSet = GradleDokkaSourceSetBuilder("", project)
        assertNull(sourceSet.build().languageVersion, "Expected no language version being set by default")

        sourceSet.languageVersion.set("JAVA_20")
        assertEquals(
            "JAVA_20", sourceSet.build().languageVersion,
            "Expected previously set language version to be present after build"
        )
    }

    @Test
    fun apiVersion() {
        val sourceSet = GradleDokkaSourceSetBuilder("", project)
        assertNull(sourceSet.build().apiVersion, "Expected no api version being set by default")

        sourceSet.apiVersion.set("20")
        assertEquals(
            "20", sourceSet.build().apiVersion,
            "Expected previously set api version to be present after build"
        )
    }

    @Test
    fun noStdlibLink() {
        val sourceSet = GradleDokkaSourceSetBuilder("", project)
        assertFalse(sourceSet.noStdlibLink.getSafe(), "Expected 'noStdlibLink' to be set to false by default")

        assertEquals(1, sourceSet.build().externalDocumentationLinks.count {
            "https://kotlinlang.org/api" in it.url.toURI().toString()
        }, "Expected kotlin stdlib in external documentation links")

        sourceSet.noStdlibLink.set(true)

        assertEquals(
            0, sourceSet.build().externalDocumentationLinks.count {
                "https://kotlinlang.org/api" in it.url.toURI().toString()
            }, "Expected no stdlib in external documentation link"
        )
    }

    @Test
    fun noJdkLink() {
        val sourceSet = GradleDokkaSourceSetBuilder("", project)
        assertFalse(sourceSet.noJdkLink.getSafe(), "Expected 'noJdkLink' to be set to false by default")

        assertEquals(1, sourceSet.build().externalDocumentationLinks.count {
            "https://docs.oracle.com/" in it.url.toURI().toString()
        }, "Expected java jdk in external documentation links")

        sourceSet.noJdkLink.set(true)

        assertEquals(
            0, sourceSet.build().externalDocumentationLinks.count {
                "https://docs.oracle.com/" in it.url.toURI().toString()
            }, "Expected no java jdk in external documentation link"
        )
    }


    @Test
    fun noAndroidSdkLink() {
        val sourceSet = GradleDokkaSourceSetBuilder("", project)
        assertFalse(sourceSet.noAndroidSdkLink.getSafe(), "Expected 'noAndroidSdkLink' to be set to false by default")

        assertEquals(0, sourceSet.build().externalDocumentationLinks.count {
            "https://developer.android.com/reference" in it.url.toURI().toString()
        }, "Expected no android sdk in external documentation links (without android plugin)")

        assertEquals(0, sourceSet.build().externalDocumentationLinks.count {
            "https://developer.android.com/reference/androidx" in it.packageListUrl.toURI().toString()
        }, "Expected no androidx in external documentation links (without android plugin)")


        project.plugins.apply("com.android.library")

        assertEquals(1, sourceSet.build().externalDocumentationLinks.count {
            "https://developer.android.com/reference/kotlin/package-list" in it.packageListUrl.toURI().toString()
        }, "Expected android sdk in external documentation links")

        assertEquals(1, sourceSet.build().externalDocumentationLinks.count {
            "https://developer.android.com/reference/kotlin/androidx/package-list" in it.packageListUrl.toURI()
                .toString()
        }, "Expected androidx in external documentation links")


        sourceSet.noAndroidSdkLink.set(true)

        assertEquals(0, sourceSet.build().externalDocumentationLinks.count {
            "https://developer.android.com/reference" in it.url.toURI().toString()
        }, "Expected no android sdk in external documentation links")

        assertEquals(0, sourceSet.build().externalDocumentationLinks.count {
            "https://developer.android.com/reference/kotlin/androidx/package-list" in it.packageListUrl.toURI()
                .toString()
        }, "Expected no androidx in external documentation links")
    }

    @Test
    fun suppressedFiles() {
        val sourceSet = GradleDokkaSourceSetBuilder("", project)
        assertTrue(sourceSet.build().suppressedFiles.isEmpty(), "Expected no suppressed files by default")

        sourceSet.suppressedFiles.from(project.file("f1"))
        sourceSet.suppressedFiles.from("f2")

        assertEquals(
            setOf(project.file("f1"), project.file("f2")), sourceSet.build().suppressedFiles,
            "Expected all suppressed files to be present after build"
        )
    }

    @Test
    fun suppressedFilesByDefault() {
        val sourceSet = GradleDokkaSourceSetBuilder("", project)
        assertTrue(sourceSet.build().suppressedFiles.isEmpty(), "Expected no suppressed files by default")

        val file = project.buildDir.resolve("generated").also { it.mkdirs() }
        file.resolve("suppressed.kt").writeText("class A")

        sourceSet.sourceRoots.from(project.buildDir.resolve("generated"))

        val suppressedConfiguration = sourceSet.build()
        sourceSet.suppressGeneratedFiles.set(false)
        val unsuppressedConfiguration = sourceSet.build()

        assertEquals(
            setOf(
                project.buildDir.resolve("generated"),
                project.buildDir.resolve("generated").resolve("suppressed.kt")
            ), suppressedConfiguration.suppressedFiles,
            "Expected all suppressed files to be present after build"
        )

        assertTrue(
            unsuppressedConfiguration.suppressedFiles.isEmpty(),
            "Expected no files to be suppressed by default"
        )
    }

    @Test
    fun platform() {
        val sourceSet = GradleDokkaSourceSetBuilder("", project)
        assertEquals(Platform.DEFAULT, sourceSet.build().analysisPlatform, "Expected default platform if not specified")

        sourceSet.platform.set(Platform.common)
        assertEquals(
            Platform.common, sourceSet.build().analysisPlatform,
            "Expected previously set analysis platform being present after build"
        )
    }
}

private fun GradleDokkaSourceSetBuilder(name: String, project: Project) =
    GradleDokkaSourceSetBuilder(name, project, project.path)
