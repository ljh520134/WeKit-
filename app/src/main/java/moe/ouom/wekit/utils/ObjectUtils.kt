package moe.ouom.wekit.utils

import de.robv.android.xposed.XposedHelpers

@Suppress("UNCHECKED_CAST")
fun <T> Any.getAdditionalField(key: String): T? {
    return XposedHelpers.getAdditionalInstanceField(this, key) as T?
}

fun Any.setAdditionalField(key: String, value: Any) {
    XposedHelpers.setAdditionalInstanceField(this, key, value)
}