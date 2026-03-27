package dev.ujhhgtg.wekit.utils

import dev.ujhhgtg.nameof.nameof
import dev.ujhhgtg.wekit.utils.WeLogger

fun logStackTrace() {
    Thread.currentThread().stackTrace
        .drop(2) // drop getStackTrace() and logStackTrace() itself
        .joinToString("\n") { "  at ${it.className}.${it.methodName}(${it.fileName}:${it.lineNumber})" }
        .let { WeLogger.d(nameof(logStackTrace()), "\n$it") }
}
