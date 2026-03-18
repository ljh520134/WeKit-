package moe.ouom.wekit.loader.entry.lsp100

import io.github.libxposed.api.XposedInterface
import moe.ouom.wekit.loader.entry.lsp100.codegen.Lsp100ProxyClassMaker
import moe.ouom.wekit.loader.utils.LibXposedApiByteCodeGenerator

object Lsp100ExtCmd {
    fun handleQueryExtension(cmd: String, arg: Array<Any?>?): Any? {
        when (cmd) {
            "GetXposedInterfaceClass" -> return XposedInterface::class.java
            "GetInitErrors" -> return emptyList<Throwable?>()
            LibXposedApiByteCodeGenerator.CMD_SET_WRAPPER -> {
                Lsp100ProxyClassMaker.setWrapperMethod((arg!![0] as java.lang.reflect.Method?)!!)
                return true
            }
            "GetLoadPackageParam", "GetInitZygoteStartupParam" -> return null
            else -> return null
        }
    }
}
