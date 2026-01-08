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

    private val colors: SyntaxColors = if (isDarkTheme) Dark else Light

    @ColorInt
    override fun background(): Int = colors.BACKGROUND

    @ColorInt
    override fun textColor(): Int = colors.TEXT

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

    private interface SyntaxColors {
        val BACKGROUND: Int
        val TEXT: Int
        val KEYWORD: Int
        val STRING: Int
        val NUMBER: Int
        val COMMENT: Int
        val FUNCTION: Int
        val CLASS_NAME: Int
        val OPERATOR: Int
        val BOOLEAN: Int
        val PROPERTY: Int
        val PUNCTUATION: Int
        val BUILTIN: Int
        val ATTR_NAME: Int
        val TAG: Int
        val SELECTOR: Int
        val REGEX: Int
    }

    private object Dark : SyntaxColors {
        override val BACKGROUND: Int = 0xFF1E1E2E.toInt()
        override val TEXT: Int = 0xFFCDD6F4.toInt()
        override val KEYWORD: Int = 0xFFCBA6F7.toInt()
        override val STRING: Int = 0xFFA6E3A1.toInt()
        override val NUMBER: Int = 0xFFFAB387.toInt()
        override val COMMENT: Int = 0xFF6C7086.toInt()
        override val FUNCTION: Int = 0xFF89B4FA.toInt()
        override val CLASS_NAME: Int = 0xFFF9E2AF.toInt()
        override val OPERATOR: Int = 0xFF89DCEB.toInt()
        override val BOOLEAN: Int = NUMBER
        override val PROPERTY: Int = 0xFFF5C2E7.toInt()
        override val PUNCTUATION: Int = TEXT
        override val BUILTIN: Int = FUNCTION
        override val ATTR_NAME: Int = PROPERTY
        override val TAG: Int = CLASS_NAME
        override val SELECTOR: Int = KEYWORD
        override val REGEX: Int = NUMBER
    }

    private object Light : SyntaxColors {
        override val BACKGROUND: Int = 0xFFEFF1F5.toInt()
        override val TEXT: Int = 0xFF4C4F69.toInt()
        override val KEYWORD: Int = 0xFF8839EF.toInt()
        override val STRING: Int = 0xFF40A02B.toInt()
        override val NUMBER: Int = 0xFFFE640B.toInt()
        override val COMMENT: Int = 0xFF9CA0B0.toInt()
        override val FUNCTION: Int = 0xFF1E66F5.toInt()
        override val CLASS_NAME: Int = 0xFFDF8E1D.toInt()
        override val OPERATOR: Int = 0xFF179299.toInt()
        override val BOOLEAN: Int = NUMBER
        override val PROPERTY: Int = 0xFFEA76CB.toInt()
        override val PUNCTUATION: Int = TEXT
        override val BUILTIN: Int = FUNCTION
        override val ATTR_NAME: Int = PROPERTY
        override val TAG: Int = CLASS_NAME
        override val SELECTOR: Int = KEYWORD
        override val REGEX: Int = NUMBER
    }

    companion object {
        fun create(isDarkTheme: Boolean): Prism4jTheme = CharishaPrism4jTheme(isDarkTheme)
    }
}
