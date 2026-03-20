package dev.ujhhgtg.wekit.utils

import android.content.Context
import android.widget.Toast
import dev.ujhhgtg.nameof.nameof

object ToastUtils {

    fun showToast(ctx: Context?, msg: String?) {
        Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
    }

    fun showToast(msg: String?) {
        Toast.makeText(HostInfo.application, msg, Toast.LENGTH_SHORT).show()
    }
}
