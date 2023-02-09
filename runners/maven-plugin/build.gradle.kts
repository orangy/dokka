import org.jetbrains.CrossPlatformExec
import org.jetbrains.SetupMaven
import org.jetbrains.registerDokkaArtifactPublication

plugins {
    org.jetbrains.conventions.`kotlin-jvm`
    org.jetbrains.conventions.`maven-publish`
}

val setupMaven by tasks.register<SetupMaven>("setupMaven")

dependencies {
    implementation(project(":core"))
    implementation("org.apache.maven:maven-core:${setupMaven.mavenVersion}")
    implementation("org.apache.maven:maven-plugin-api:${setupMaven.mavenVersion}")
    implementation("org.apache.maven.plugin-tools:maven-plugin-annotations:${setupMaven.mavenPluginToolsVersion}")
    implementation("org.apache.maven:maven-archiver:2.5")
    implementation(kotlin("stdlib-jdk8"))
}

val mavenBuildDir = setupMaven.mavenBuildDir
val mavenBinDir = setupMaven.mavenBinDir

tasks.clean {
    delete(mavenBuildDir)
    delete(mavenBinDir)
}

val generatePom by tasks.registering(Copy::class) {
    description = "Generate pom.xml for Maven Plugin Plugin"

    val dokka_version: String by project
    inputs.property("dokka_version", dokka_version)

    from("$projectDir/pom.tpl.xml") {
        rename("(.*).tpl.xml", "$1.xml")
    }
    into(setupMaven.mavenBuildDir)

    eachFile {
        filter { line ->
            line.replace("<maven.version></maven.version>", "<maven.version>${setupMaven.mavenVersion}</maven.version>")
        }
        filter { line ->
            line.replace("<version>dokka_version</version>", "<version>$dokka_version</version>")
        }
        filter { line ->
            line.replace(
                "<version>maven-plugin-plugin</version>",
                "<version>${setupMaven.mavenPluginToolsVersion}</version>"
            )
        }
    }
}

val syncClasses by tasks.registering(Sync::class) {
    description = "Copy compiled classes to the Maven build dir, for Maven Plugin task execution"

    dependsOn(tasks.compileKotlin, tasks.compileJava)
    from("$buildDir/classes/kotlin", "$buildDir/classes/java")
    into("${setupMaven.mavenBuildDir}/classes/java")

    preserve {
        include("**/*.class")
    }
}

val helpMojo by tasks.registering(CrossPlatformExec::class) {
    dependsOn(setupMaven, generatePom, syncClasses)
    workingDir(setupMaven.mavenBuildDir)
    commandLine(setupMaven.mvn, "-e", "-B", "org.apache.maven.plugins:maven-plugin-plugin:helpmojo")

    outputs.dir(layout.buildDirectory.dir("maven"))
}

val pluginDescriptor by tasks.registering(CrossPlatformExec::class) {
    dependsOn(setupMaven, generatePom, syncClasses)
    workingDir(setupMaven.mavenBuildDir)
    commandLine(setupMaven.mvn, "-e", "-B", "org.apache.maven.plugins:maven-plugin-plugin:descriptor")

    outputs.dir(layout.buildDirectory.dir("maven/classes/java/main/META-INF/maven"))
}

val sourceJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(java.sourceSets["main"].allSource)
}

tasks.jar {
    dependsOn(pluginDescriptor, helpMojo)
    metaInf {
        from("${setupMaven.mavenBuildDir}/classes/java/main/META-INF")
    }
    manifest {
        attributes("Class-Path" to configurations.runtimeClasspath.map { configuration ->
            configuration.resolve().joinToString(" ") { it.name }
        })
    }
    duplicatesStrategy = DuplicatesStrategy.WARN
}


registerDokkaArtifactPublication("dokkaMavenPlugin") {
    artifactId = "dokka-maven-plugin"
}
