package utils

import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.renderers.OutputWriter
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.DokkaPluginApiPreview
import org.jetbrains.dokka.plugability.PluginApiPreviewAcknowledgement
import java.util.*

class TestOutputWriterPlugin(failOnOverwrite: Boolean = true) : DokkaPlugin() {
    val writer = TestOutputWriter(failOnOverwrite)

    private val dokkaBase by lazy { plugin<DokkaBase>() }

    val testWriter by extending {
        (dokkaBase.outputWriter
                with writer
                override dokkaBase.fileWriter)
    }

    @OptIn(DokkaPluginApiPreview::class)
    override fun pluginApiPreviewAcknowledgement(): PluginApiPreviewAcknowledgement =
        PluginApiPreviewAcknowledgement
}

class TestOutputWriter(private val failOnOverwrite: Boolean = true) : OutputWriter {
    val contents: Map<String, String> get() = _contents

    private val _contents = Collections.synchronizedMap(mutableMapOf<String, String>())
    override suspend fun write(path: String, text: String, ext: String) {
        val fullPath = "$path$ext"
        _contents.putIfAbsent(fullPath, text)?.also {
            if (failOnOverwrite) throw AssertionError("File $fullPath is being overwritten.")
        }
    }

    override suspend fun writeResources(pathFrom: String, pathTo: String) =
        write(pathTo, "*** content of $pathFrom ***", "")
}
