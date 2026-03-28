package dev.ujhhgtg.wekit.utils

import dev.ujhhgtg.nameof.nameof

fun logStackTrace(tag: String = nameof(logStackTrace())) {
    Thread.currentThread().stackTrace
        .drop(2) // drop getStackTrace() and logStackTrace() itself
        .joinToString("\n") { "  at ${it.className}.${it.methodName}(${it.fileName}:${it.lineNumber})" }
        .let { WeLogger.d(tag, "\n$it") }
}
