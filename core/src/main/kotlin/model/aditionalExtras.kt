package org.jetbrains.dokka.model

import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.properties.ExtraProperty
import org.jetbrains.dokka.model.properties.MergeStrategy

class AdditionalModifiers(val content: Set<ExtraModifiers>) : ExtraProperty<Documentable> {
    companion object : ExtraProperty.Key<Documentable, AdditionalModifiers> {
        override fun mergeStrategyFor(
            left: AdditionalModifiers,
            right: AdditionalModifiers
        ): MergeStrategy<Documentable> = MergeStrategy.Replace(AdditionalModifiers(left.content + right.content))
    }

    override fun equals(other: Any?): Boolean =
        if (other is AdditionalModifiers) other.content == content else false

    override fun hashCode() = content.hashCode()
    override val key: ExtraProperty.Key<Documentable, *> = AdditionalModifiers
}

class Annotations(val content: List<Annotation>) : ExtraProperty<Documentable> {
    companion object : ExtraProperty.Key<Documentable, Annotations> {
        override fun mergeStrategyFor(left: Annotations, right: Annotations): MergeStrategy<Documentable> =
            MergeStrategy.Replace(Annotations((left.content + right.content).distinct()))
    }

    override val key: ExtraProperty.Key<Documentable, *> = Annotations

    data class Annotation(val dri: DRI, val params: Map<String, AnnotationParameterValue>) {
        override fun equals(other: Any?): Boolean = when (other) {
            is Annotation -> dri == other.dri
            else -> false
        }

        override fun hashCode(): Int = dri.hashCode()
    }
}

sealed class AnnotationParameterValue
data class AnnotationValue(val annotation: Annotations.Annotation) : AnnotationParameterValue()
data class ArrayValue(val value: List<AnnotationParameterValue>) : AnnotationParameterValue()
data class EnumValue(val enumName: String, val enumDri: DRI) : AnnotationParameterValue()
data class ClassValue(val className: String, val classDRI: DRI) : AnnotationParameterValue()
data class StringValue(val value: String) : AnnotationParameterValue()


object PrimaryConstructorExtra : ExtraProperty<DFunction>, ExtraProperty.Key<DFunction, PrimaryConstructorExtra> {
    override val key: ExtraProperty.Key<DFunction, *> = this
}

data class ActualTypealias(val underlyingType: SourceSetDependent<Bound>) : ExtraProperty<DClasslike> {
    companion object : ExtraProperty.Key<DClasslike, ActualTypealias> {
        override fun mergeStrategyFor(
            left: ActualTypealias,
            right: ActualTypealias
        ) =
            MergeStrategy.Replace(ActualTypealias(left.underlyingType + right.underlyingType))
    }

    override val key: ExtraProperty.Key<DClasslike, ActualTypealias> = ActualTypealias
}