package dev.ujhhgtg.wekit.hooks.api.net

import dev.ujhhgtg.wekit.hooks.api.net.abc.WeRequestCallback

class WeRequestDsl : WeRequestCallback {

    private var successHandler: ((String, ByteArray?) -> Unit)? = null
    private var failHandler: ((Int, Int, String) -> Unit)? = null

    fun onSuccess(handler: (json: String, bytes: ByteArray?) -> Unit) {
        this.successHandler = handler
    }

    fun onFailure(handler: (errType: Int, errCode: Int, errMsg: String) -> Unit) {
        this.failHandler = handler
    }

    override fun onSuccess(json: String, bytes: ByteArray?) {
        successHandler?.invoke(json, bytes)
    }

    override fun onFailure(errType: Int, errCode: Int, errMsg: String) {
        failHandler?.invoke(errType, errCode, errMsg)
    }
}
