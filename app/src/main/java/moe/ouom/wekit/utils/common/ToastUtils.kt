package moe.ouom.wekit.utils.common

import android.content.Context
import android.widget.Toast
import dev.ujhhgtg.nameof.nameof
import moe.ouom.wekit.host.HostInfo.application
import moe.ouom.wekit.utils.log.WeLogger

object ToastUtils {
    private val TAG = nameof(ToastUtils)

    fun showToast(ctx: Context?, msg: String?) {
        WeLogger.i(TAG, "showToast: $msg")
        Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
    }

    fun showToast(msg: String?) {
        WeLogger.i(TAG, "showToast: $msg")
        try {
            Toast.makeText(application, msg, Toast.LENGTH_SHORT).show()
        } catch (e: NullPointerException) {
            WeLogger.e(TAG, "failed to show toast: " + e.message)
        }
    }
}
