package dev.ujhhgtg.wekit.utils

import android.util.Log
import dev.ujhhgtg.wekit.BuildConfig
import java.util.Locale
import kotlin.math.min

object WeLogger {

    private const val TAG = BuildConfig.TAG

    private const val CHUNK_SIZE = 4000
    private const val MAX_CHUNKS = 200

    // ========== String ==========

    fun e(tag: String?, msg: String) {
        Log.e(TAG, "$tag: $msg")
    }

    fun w(tag: String?, msg: String) {
        Log.w(TAG, "$tag: $msg")
    }

    fun i(tag: String?, msg: String) {
        Log.i(TAG, "$tag: $msg")
    }

    fun d(tag: String?, msg: String) {
        Log.d(TAG, "$tag: $msg")
    }

    fun v(tag: String?, msg: String) {
        Log.v(TAG, "$tag: $msg")
    }

    // ========== Tag + String + Throwable ==========
    fun e(tag: String?, msg: String, e: Throwable) {
        Log.e(TAG, "$tag: $msg", e)
    }

    fun w(tag: String?, msg: String, e: Throwable) {
        Log.w(TAG, "$tag: $msg", e)
    }

    fun i(tag: String?, msg: String, e: Throwable) {
        Log.i(TAG, "$tag: $msg", e)
    }

    fun d(tag: String?, msg: String, e: Throwable) {
        Log.d(TAG, "$tag: $msg", e)
    }

    fun v(tag: String?, msg: String, e: Throwable) {
        Log.v(TAG, "$tag: $msg", e)
    }

    // ========== String + Throwable ==========

    fun e(msg: String, e: Throwable) {
        Log.e(TAG, msg, e)
    }

    fun getStackTraceString(): String {
        val stackTrace =
            Thread.currentThread().stackTrace

        val stackTraceMsg = StringBuilder().append("\n")
        var startRecording = false

        for (element in stackTrace) {
            val className = element!!.className
            if (className.contains("LSPHooker")) {
                startRecording = true
                continue
            }

            // 如果还没遇到目标类，直接跳过
            if (!startRecording) {
                continue
            }

            // 过滤掉无关紧要的系统类或当前类（可选）
            if (className == Thread::class.java.name) {
                continue
            }

            stackTraceMsg.append(
                "  at %s.%s(%s:%d)\n".format(
                    Locale.ROOT,
                    element.className,
                    element.methodName,
                    element.fileName,
                    element.lineNumber
                )
            )
        }
        return stackTraceMsg.toString()
    }

    // ========== 分段打印 ==========
    fun logChunked(priority: Int, tag: String, msg: String) {
        if (msg.length <= CHUNK_SIZE) {
            Log.println(priority, BuildConfig.TAG, "$tag: $msg")
            return
        }

        val len = msg.length
        val chunkCount = (len + CHUNK_SIZE - 1) / CHUNK_SIZE
        if (chunkCount > MAX_CHUNKS) {
            val head = msg.substring(0, CHUNK_SIZE)
            Log.println(
                priority,
                BuildConfig.TAG,
                ("$tag: [chunked] too long ($len chars, $chunkCount chunks). head:\n$head")
            )
            Log.println(
                priority,
                BuildConfig.TAG,
                "$tag: [chunked] truncated. consider writing to file for full dump."
            )
            return
        }

        var i = 0
        var part = 1
        while (i < len) {
            val end = min(i + CHUNK_SIZE, len)
            val chunk = msg.substring(i, end)
            Log.println(
                priority,
                BuildConfig.TAG,
                "$tag: [part $part/$chunkCount] $chunk"
            )
            i += CHUNK_SIZE
            part++
        }
    }

    fun logChunkedI(tag: String, msg: String) {
        logChunked(Log.INFO, tag, msg)
    }

    fun logChunkedD(tag: String, msg: String) {
        logChunked(Log.DEBUG, tag, msg)
    }
}
