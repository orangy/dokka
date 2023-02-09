apply(from = "../template.settings.gradle.kts")
rootProject.name = "it-multimodule-0"
include(":moduleA")
include(":moduleA:moduleB")
include(":moduleA:moduleC")
include(":moduleA:moduleD")
