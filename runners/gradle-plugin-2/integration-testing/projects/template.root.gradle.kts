allprojects {
    repositories {
        maven("https://cache-redirector.jetbrains.com/jcenter.bintray.com")
        mavenLocal()
        mavenCentral()
        google()
        maven("https://cache-redirector.jetbrains.com/dl.bintray.com/kotlin/kotlin-eap")
        maven("https://cache-redirector.jetbrains.com/dl.bintray.com/kotlin/kotlin-dev")
        maven("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven") {
            content {
                includeGroup("org.jetbrains.kotlinx")
            }
        }
    }
}

afterEvaluate {
    logger.quiet("Gradle version: ${gradle.gradleVersion}")
    logger.quiet("Kotlin version: ${properties["dokka_it_kotlin_version"]}")
    properties["dokka_it_android_gradle_plugin_version"]?.let { androidVersion ->
        logger.quiet("Android version: $androidVersion")
    }
}
