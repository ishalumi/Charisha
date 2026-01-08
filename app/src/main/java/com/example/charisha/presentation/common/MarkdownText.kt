package com.example.charisha.presentation.common

import android.content.Context
import android.widget.TextView
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.syntax.Prism4jThemeDefault
import io.noties.markwon.syntax.SyntaxHighlightPlugin
import io.noties.prism4j.Prism4j
import io.noties.prism4j.GrammarLocator

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: Float? = null
) {
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()
    val markwon = remember(context, isDarkTheme) { createMarkwon(context, isDarkTheme) }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextView(ctx).apply {
                fontSize?.let { textSize = it }
                if (color != Color.Unspecified) {
                    setTextColor(color.toArgb())
                }
            }
        },
        update = { textView ->
            if (color != Color.Unspecified) {
                textView.setTextColor(color.toArgb())
            }
            fontSize?.let { textView.textSize = it }
            if (textView.tag != markdown) {
                textView.tag = markdown
                markwon.setMarkdown(textView, markdown)
            }
        }
    )
}

private fun createMarkwon(context: Context, isDarkTheme: Boolean): Markwon {
    val prism4j = Prism4j(SimpleGrammarLocator())
    val theme = CharishaPrism4jTheme.create(isDarkTheme)
    return Markwon.builder(context)
        .usePlugin(StrikethroughPlugin.create())
        .usePlugin(TablePlugin.create(context))
        .usePlugin(SyntaxHighlightPlugin.create(prism4j, theme))
        .build()
}

/**
 * 简单的 GrammarLocator 实现，返回 null 表示使用默认处理
 */
private class SimpleGrammarLocator : GrammarLocator {
    override fun grammar(prism4j: Prism4j, language: String): Prism4j.Grammar? = null
    override fun languages(): MutableSet<String> = mutableSetOf()
}
