package dev.ujhhgtg.wekit.utils.polyfills

import android.os.Build
import java.util.stream.Stream

fun <T> Stream<T>.convToList(): List<T> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        this.toList()
    } else {
        @Suppress("UNCHECKED_CAST")
        this.toArray().toList() as List<T>
    }
}

