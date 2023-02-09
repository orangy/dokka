package org.jetbrains.dokka.gradle

import org.gradle.api.NamedDomainObjectFactory
import org.jetbrains.dokka.gradle.tasks.AbstractDokkaTask

@Suppress("ObjectLiteralToLambda") // Will fail at runtime in Gradle versions <= 6.6
fun AbstractDokkaTask.gradleDokkaSourceSetBuilderFactory(): NamedDomainObjectFactory<GradleDokkaSourceSetBuilder> =
    NamedDomainObjectFactory { name -> GradleDokkaSourceSetBuilder(name, project, DokkaSourceSetIdFactory()) }

