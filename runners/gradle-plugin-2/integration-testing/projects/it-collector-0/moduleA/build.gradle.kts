plugins {
    // TODO: File bug report for gradle: :moduleA:moduleB:dokkaHtml is missing kotlin gradle plugin from
    //  the runtime classpath during execution without this plugin in the parent project
    kotlin("jvm")
    id("org.jetbrains.dokka")
}
