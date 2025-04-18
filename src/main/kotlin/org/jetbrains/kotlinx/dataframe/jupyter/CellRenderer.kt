package org.jetbrains.kotlinx.dataframe.jupyter

import org.jetbrains.kotlinx.dataframe.io.DisplayConfiguration
import org.jetbrains.kotlinx.dataframe.io.internallyRenderable
import org.jetbrains.kotlinx.dataframe.io.renderValueForHtml
import org.jetbrains.kotlinx.dataframe.io.tooltipLimit
import org.jetbrains.kotlinx.jupyter.api.MimeTypedResult
import org.jetbrains.kotlinx.jupyter.api.Notebook
import org.jetbrains.kotlinx.jupyter.api.Renderable
import org.jetbrains.kotlinx.jupyter.api.libraries.ExecutionHost

public data class RenderedContent(
    val truncatedContent: String,
    val textLength: Int,
    val fullContent: String?,
    val isFormatted: Boolean
) {
    public companion object {

        public fun media(html: String): RenderedContent = RenderedContent(html, 0, null, false)

        public fun textWithLength(str: String, len: Int): RenderedContent = RenderedContent(str, len, null, false)

        public fun text(str: String): RenderedContent = RenderedContent(str, str.length, null, false)

        public fun truncatedText(str: String, fullText: String): RenderedContent = RenderedContent(str, str.length, fullText, false)
    }

    val isTruncated: Boolean
        get() = fullContent != null

    public operator fun plus(other: RenderedContent): RenderedContent = RenderedContent(truncatedContent + other.truncatedContent, textLength + other.textLength, fullContent?.plus(other.fullContent) ?: other.fullContent, isFormatted || other.isFormatted)
}

public interface CellRenderer {
    /**
     * Returns [value] rendered to HTML text, or null if such rendering is impossible
     */
    public fun content(value: Any?, configuration: DisplayConfiguration): RenderedContent

    /**
     * Returns cell tooltip for this [value]
     */
    public fun tooltip(value: Any?, configuration: DisplayConfiguration): String
}

public abstract class ChainedCellRenderer(
    private val parent: CellRenderer,
) : CellRenderer {
    public abstract fun maybeContent(value: Any?, configuration: DisplayConfiguration): RenderedContent?
    public abstract fun maybeTooltip(value: Any?, configuration: DisplayConfiguration): String?

    public override fun content(value: Any?, configuration: DisplayConfiguration): RenderedContent {
        return maybeContent(value, configuration) ?: parent.content(value, configuration)
    }

    public override fun tooltip(value: Any?, configuration: DisplayConfiguration): String {
        return maybeTooltip(value, configuration) ?: parent.tooltip(value, configuration)
    }
}

public object DefaultCellRenderer : CellRenderer {
    public override fun content(value: Any?, configuration: DisplayConfiguration): RenderedContent {
        return renderValueForHtml(value, configuration.cellContentLimit, configuration.precision)
    }

    public override fun tooltip(value: Any?, configuration: DisplayConfiguration): String {
        return renderValueForHtml(value, tooltipLimit, configuration.precision).truncatedContent
    }
}

internal class JupyterCellRenderer(
    private val notebook: Notebook,
    private val host: ExecutionHost,
) : ChainedCellRenderer(DefaultCellRenderer) {
    override fun maybeContent(value: Any?, configuration: DisplayConfiguration): RenderedContent? {
        val renderersProcessor = notebook.renderersProcessor
        if (internallyRenderable(value)) return null
        val renderedVal = renderersProcessor.renderValue(host, value)
        val finalVal = if (renderedVal is Renderable) renderedVal.render(notebook) else renderedVal
        if (finalVal is MimeTypedResult && "text/html" in finalVal) return RenderedContent.media(
            finalVal["text/html"] ?: ""
        )
        return renderValueForHtml(finalVal, configuration.cellContentLimit, configuration.precision)
    }

    override fun maybeTooltip(value: Any?, configuration: DisplayConfiguration): String? {
        return null
    }
}
