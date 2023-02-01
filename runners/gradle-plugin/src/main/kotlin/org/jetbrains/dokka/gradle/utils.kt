package org.jetbrains.dokka.gradle

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.UnknownDomainObjectException
import org.gradle.util.Path
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget


/** Parse a Gradle path, e.g. `:project:subproject:taskName` */
internal fun parsePath(path: String): Path = Path.path(path)

internal val Project.kotlinOrNull: KotlinProjectExtension?
    get() = try {
        project.extensions.findByType()
    } catch (e: NoClassDefFoundError) {
        null
    }

internal val Project.kotlin: KotlinProjectExtension
    get() = project.extensions.getByType()

internal fun Project.isAndroidProject() = try {
    project.extensions.getByName("android")
    true
} catch (e: UnknownDomainObjectException) {
    false
} catch (e: ClassNotFoundException) {
    false
}

internal fun KotlinTarget.isAndroidTarget() = this.platformType == KotlinPlatformType.androidJvm

internal fun <T : Any> NamedDomainObjectContainer<T>.maybeCreate(name: String, configuration: T.() -> Unit): T {
    return findByName(name) ?: create(name, configuration)
}
