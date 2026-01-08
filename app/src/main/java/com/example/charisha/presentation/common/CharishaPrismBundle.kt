package com.example.charisha.presentation.common

import io.noties.prism4j.annotations.PrismBundle

/**
 * Prism4j 语法定义 - 支持常用编程语言的代码高亮
 * KSP 将生成 CharishaGrammarLocator 类
 */
@PrismBundle(
    include = [
        "c",
        "clike",
        "clojure",
        "cpp",
        "csharp",
        "css",
        "dart",
        "go",
        "groovy",
        "java",
        "javascript",
        "json",
        "kotlin",
        "latex",
        "makefile",
        "markdown",
        "markup",
        "python",
        "ruby",
        "rust",
        "scala",
        "sql",
        "swift",
        "yaml"
    ],
    grammarLocatorClassName = ".CharishaGrammarLocator"
)
class CharishaPrismBundle
