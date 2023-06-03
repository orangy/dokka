package renderers

import org.jetbrains.dokka.testApi.context.MockContext

abstract class RenderingOnlyTestBase<T> {
    abstract val context: MockContext
    abstract val renderedContent: T
}
