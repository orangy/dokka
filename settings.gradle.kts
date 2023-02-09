rootProject.name = "dokka"

include(
    ":core",
    ":core:test-api",
    ":core:content-matcher-test-utils",

    ":kotlin-analysis",
    ":kotlin-analysis:intellij-dependency",
    ":kotlin-analysis:compiler-dependency",

    ":runners:gradle-plugin",
    ":runners:cli",
    ":runners:maven-plugin",

    ":plugins:base",
    ":plugins:base:frontend",
    ":plugins:base:search-component",
    ":plugins:base:base-test-utils",
    ":plugins:all-modules-page",
    ":plugins:templating",
    ":plugins:versioning",
    ":plugins:android-documentation",

    ":plugins:mathjax",
    ":plugins:gfm",
    ":plugins:gfm:gfm-template-processing",
    ":plugins:jekyll",
    ":plugins:jekyll:jekyll-template-processing",
    ":plugins:kotlin-as-java",
    ":plugins:javadoc",

    ":integration-tests",
    ":integration-tests:gradle",
    ":integration-tests:cli",
    ":integration-tests:maven",

    ":test-utils",

    ":mkdocs",
)

includeBuild("runners/gradle-plugin-2")

val isCiBuild = System.getenv("GITHUB_ACTIONS") != null || System.getenv("TEAMCITY_VERSION") != null

plugins {
    `gradle-enterprise`
}

gradleEnterprise {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
        publishAlwaysIf(isCiBuild)
    }
}

@Suppress("UnstableApiUsage") // Central declaration of repositories is an incubating feature
dependencyResolutionManagement {

    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)

    repositories {
        mavenCentral()
        google()

        // Declare the Node.js & Yarn download repositories
        ivy("https://nodejs.org/dist/") {
            name = "Node Distributions at $url"
            patternLayout { artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]") }
            metadataSources { artifact() }
            content { includeModule("org.nodejs", "node") }
        }
        ivy("https://github.com/yarnpkg/yarn/releases/download") {
            name = "Yarn Distributions at $url"
            patternLayout { artifact("v[revision]/[artifact](-v[revision]).[ext]") }
            metadataSources { artifact() }
            content { includeModule("com.yarnpkg", "yarn") }
        }

        maven("https://www.jetbrains.com/intellij-repository/snapshots")
        maven("https://www.jetbrains.com/intellij-repository/releases")
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide")
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide-plugin-dependencies")
        maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
        maven("https://www.myget.org/F/rd-snapshots/maven/")
    }

    pluginManagement {
        repositories {
            gradlePluginPortal()
            mavenCentral()
        }
    }
}
