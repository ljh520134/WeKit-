package dev.ujhhgtg.wekit.utils.logging

import dev.ujhhgtg.wekit.utils.KnownPaths
import dev.ujhhgtg.wekit.utils.createDirectoriesNoThrow
import dev.ujhhgtg.wekit.utils.formatEpoch
import java.nio.file.Path
import kotlin.io.path.appendText
import kotlin.io.path.createFile
import kotlin.io.path.div
import kotlin.io.path.exists

object LogUtils {

    private val rootDir: Path by lazy { (KnownPaths.moduleData / "logs").createDirectoriesNoThrow() }

    private val runLogDirectory: Path by lazy { (rootDir / "run").createDirectoriesNoThrow() }

    private val errorLogDirectory: Path by lazy { (rootDir / "error").createDirectoriesNoThrow() }

    /**
     * 获取堆栈跟踪
     *
     * @param throwable new Throwable || Exception
     * @return 堆栈跟踪
     */
    fun getStackTrace(throwable: Throwable): String {
        val result = StringBuilder()
        result.append(throwable).append("\n")
        val stackTraceElements = throwable.stackTrace
        for (stackTraceElement in stackTraceElements) {
            //不把当前类加入结果中
            if (stackTraceElement!!.className == LogUtils::class.java.name) continue
            result.append(stackTraceElement).append("\n")
        }
        return result.toString()
    }

    /**
     * 记录运行日志 确保能走到那一行代码
     *
     * @param tag(文件名) 内容
     */
    fun addRunLog(tag: String, content: Any?) {
        addLog(tag, content.toString(), content, false)
    }

    /**
     * 记录异常
     */
    fun addError(tag: String, e: Throwable) {
        addLog(tag, e.toString(), e, true)
    }

    private fun addLog(fileName: String, desc: String?, content: Any?, isError: Boolean) {
        val directory = if (isError) errorLogDirectory else runLogDirectory

        val path = directory.resolve("$fileName.log")
        val stringBuffer = StringBuilder(time)
        stringBuffer.append("\n").append(desc)
        if (content is Exception) {
            stringBuffer.append("\n").append(getStackTrace(content))
        }
        stringBuffer.append("\n\n")
        if (!path.exists()) path.createFile()
        path.appendText(stringBuffer)
    }

    val time: String
        get() = formatEpoch(System.currentTimeMillis(), true)

    fun addError(tag: String, msg: String?) {
        addLog(tag, msg, null, true)
    }
}
