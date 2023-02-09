import org.gradle.configurationcache.extensions.serviceOf
import org.jetbrains.*

plugins {
    `kotlin-dsl`
    org.jetbrains.conventions.`maven-publish`
    id("com.gradle.plugin-publish")
}

dependencies {
    api(project(":core"))

    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin")
    compileOnly("com.android.tools.build:gradle:4.0.1")
    compileOnly(gradleKotlinDsl())
    testImplementation(project(":test-utils"))
    testImplementation(gradleKotlinDsl())
    testImplementation("org.jetbrains.kotlin:kotlin-gradle-plugin")
    testImplementation("com.android.tools.build:gradle:4.0.1")

    // Fix https://github.com/gradle/gradle/issues/16774
    testImplementation(
        files(
            serviceOf<org.gradle.api.internal.classpath.ModuleRegistry>().getModule("gradle-tooling-api-builders")
                .classpath.asFiles.first()
        )
    )

    val jackson_version: String by project
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jackson_version")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:$jackson_version")
    val jackson_databind_version: String by project
    constraints {
        implementation("com.fasterxml.jackson.core:jackson-databind:$jackson_databind_version") {
            because("CVE-2022-42003")
        }
    }
}

// Gradle will put its own version of the stdlib in the classpath, so not pull our own we will end up with
// warnings like 'Runtime JAR files in the classpath should have the same version'
configurations.api.configure {
    excludeGradleCommonDependencies()
}

/**
 * These dependencies will be provided by Gradle, and we should prevent version conflict
 * Code taken from the Kotlin Gradle plugin:
 * https://github.com/JetBrains/kotlin/blob/70e15b281cb43379068facb82b8e4bcb897a3c4f/buildSrc/src/main/kotlin/GradleCommon.kt#L72
 */
fun Configuration.excludeGradleCommonDependencies() {
    dependencies
        .withType<ModuleDependency>()
        .configureEach {
            exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
            exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk7")
            exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
            exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-common")
            exclude(group = "org.jetbrains.kotlin", module = "kotlin-reflect")
            exclude(group = "org.jetbrains.kotlin", module = "kotlin-script-runtime")
        }
}

val sourceJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets["main"].allSource)
}

gradlePlugin {
    plugins {
        create("dokkaGradlePlugin") {
            id = "org.jetbrains.dokka"
            displayName = "Dokka plugin"
            description = "Dokka, the Kotlin documentation tool"
            implementationClass = "org.jetbrains.dokka.gradle.DokkaPlugin"
            version = dokkaVersion
            isAutomatedPublishing = true
        }
    }
}

pluginBundle {
    website = "https://www.kotlinlang.org/"
    vcsUrl = "https://github.com/kotlin/dokka.git"
    tags = listOf("dokka", "kotlin", "kdoc", "android", "documentation")

    mavenCoordinates {
        groupId = "org.jetbrains.dokka"
        artifactId = "dokka-gradle-plugin"
    }
}

val javadocJar by tasks.creating(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assembles java doc to jar"
    archiveClassifier.set("javadoc")
    from(tasks.javadoc)
}

publishing {
    publications {
        register<MavenPublication>("dokkaGradlePluginForIntegrationTests") {
            artifactId = "dokka-gradle-plugin"
            from(components["java"])
            version = "for-integration-tests-SNAPSHOT"
        }

        register<MavenPublication>("pluginMaven") {
            configurePom("Dokka ${project.name}")
            artifactId = "dokka-gradle-plugin"
            artifact(javadocJar)
        }

        afterEvaluate {
            named<MavenPublication>("dokkaGradlePluginPluginMarkerMaven") {
                configurePom("Dokka plugin")
            }
        }
    }
}

tasks.validatePlugins {
    enableStricterValidation.set(true)
}

tasks.withType<PublishToMavenRepository>().configureEach {
    onlyIf { publication != publishing.publications["dokkaGradlePluginForIntegrationTests"] }
}

afterEvaluate { // Workaround for an interesting design choice https://github.com/gradle/gradle/blob/c4f935f77377f1783f70ec05381c8182b3ade3ea/subprojects/plugin-development/src/main/java/org/gradle/plugin/devel/plugins/MavenPluginPublishPlugin.java#L49
    configureSpacePublicationIfNecessary("pluginMaven", "dokkaGradlePluginPluginMarkerMaven")
    configureSonatypePublicationIfNecessary("pluginMaven", "dokkaGradlePluginPluginMarkerMaven")
    createDokkaPublishTaskIfNecessary()
}

tasks.processResources {
    duplicatesStrategy = DuplicatesStrategy.WARN
}
