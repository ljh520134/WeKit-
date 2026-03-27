package dev.ujhhgtg.wekit.utils

import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun showToast(ctx: Context, msg: String) {
    Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
}

fun showToast(msg: String) {
    Toast.makeText(HostInfo.application, msg, Toast.LENGTH_SHORT).show()
}

suspend fun showToastSuspend(msg: String) = withContext(Dispatchers.Main) {
    showToast(msg)
}
