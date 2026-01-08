package com.example.charisha.presentation.common

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.annotation.ColorInt
import io.noties.markwon.syntax.Prism4jTheme
import io.noties.prism4j.Prism4j

/**
 * Charisha 代码高亮主题 - 支持明暗双模式
 * 基于 Catppuccin 配色方案，符合 UI/UX Pro Max 设计规范
 */
class CharishaPrism4jTheme private constructor(
    private val isDarkTheme: Boolean
) : Prism4jTheme {

    @ColorInt
    override fun background(): Int = if (isDarkTheme) Dark.BACKGROUND else Light.BACKGROUND

    @ColorInt
    override fun textColor(): Int = if (isDarkTheme) Dark.TEXT else Light.TEXT

    override fun apply(
        language: String,
        syntax: Prism4j.Syntax,
        builder: SpannableStringBuilder,
        start: Int,
        end: Int
    ) {
        val color = getColorForSyntax(syntax.type())
        if (color != 0) {
            builder.setSpan(
                ForegroundColorSpan(color),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    @ColorInt
    private fun getColorForSyntax(type: String): Int {
        val colors = if (isDarkTheme) Dark else Light
        return when (type) {
            "comment", "prolog", "doctype", "cdata" -> colors.COMMENT
            "punctuation" -> colors.PUNCTUATION
            "keyword", "atrule" -> colors.KEYWORD
            "string", "char", "attr-value" -> colors.STRING
            "number", "constant" -> colors.NUMBER
            "boolean" -> colors.BOOLEAN
            "operator", "entity", "url" -> colors.OPERATOR
            "function" -> colors.FUNCTION
            "class-name" -> colors.CLASS_NAME
            "property", "symbol", "variable" -> colors.PROPERTY
            "builtin" -> colors.BUILTIN
            "attr-name" -> colors.ATTR_NAME
            "tag" -> colors.TAG
            "selector" -> colors.SELECTOR
            "regex" -> colors.REGEX
            "important", "bold" -> colors.KEYWORD
            "italic" -> colors.COMMENT
            "namespace" -> colors.CLASS_NAME
            "deleted" -> colors.STRING
            "inserted" -> colors.STRING
            else -> colors.TEXT
        }
    }

    private object Dark {
        const val BACKGROUND: Int = 0xFF1E1E2E.toInt()
        const val TEXT: Int = 0xFFCDD6F4.toInt()
        const val KEYWORD: Int = 0xFFCBA6F7.toInt()
        const val STRING: Int = 0xFFA6E3A1.toInt()
        const val NUMBER: Int = 0xFFFAB387.toInt()
        const val COMMENT: Int = 0xFF6C7086.toInt()
        const val FUNCTION: Int = 0xFF89B4FA.toInt()
        const val CLASS_NAME: Int = 0xFFF9E2AF.toInt()
        const val OPERATOR: Int = 0xFF89DCEB.toInt()
        const val BOOLEAN: Int = 0xFFFAB387.toInt()
        const val PROPERTY: Int = 0xFFF5C2E7.toInt()
        const val PUNCTUATION: Int = TEXT
        const val BUILTIN: Int = FUNCTION
        const val ATTR_NAME: Int = PROPERTY
        const val TAG: Int = CLASS_NAME
        const val SELECTOR: Int = KEYWORD
        const val REGEX: Int = NUMBER
    }

    private object Light {
        const val BACKGROUND: Int = 0xFFEFF1F5.toInt()
        const val TEXT: Int = 0xFF4C4F69.toInt()
        const val KEYWORD: Int = 0xFF8839EF.toInt()
        const val STRING: Int = 0xFF40A02B.toInt()
        const val NUMBER: Int = 0xFFFE640B.toInt()
        const val COMMENT: Int = 0xFF9CA0B0.toInt()
        const val FUNCTION: Int = 0xFF1E66F5.toInt()
        const val CLASS_NAME: Int = 0xFFDF8E1D.toInt()
        const val OPERATOR: Int = 0xFF179299.toInt()
        const val BOOLEAN: Int = 0xFFFE640B.toInt()
        const val PROPERTY: Int = 0xFFEA76CB.toInt()
        const val PUNCTUATION: Int = TEXT
        const val BUILTIN: Int = FUNCTION
        const val ATTR_NAME: Int = PROPERTY
        const val TAG: Int = CLASS_NAME
        const val SELECTOR: Int = KEYWORD
        const val REGEX: Int = NUMBER
    }

    companion object {
        fun create(isDarkTheme: Boolean): Prism4jTheme = CharishaPrism4jTheme(isDarkTheme)
    }
}
