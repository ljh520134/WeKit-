package dev.ujhhgtg.wekit.hooks.core

import android.content.Context
import dev.ujhhgtg.wekit.preferences.WePrefs
import dev.ujhhgtg.wekit.utils.logging.WeLogger

abstract class SwitchHookItem : BaseHookItem() {

    private var _isEnabled = false
    var isEnabled
        get() = _isEnabled
        set(value) {
            if (_isEnabled == value) return
            _isEnabled = value
            if (!value) {
                if (isLoaded) {
                    WeLogger.i("disabling $path...")
                    disable()
                    isLoaded = false
                }
            } else {
                WeLogger.i("enabling $path...")
                enable()
                isLoaded = true
            }
        }

    fun setEnabledSilently(value: Boolean) {
        _isEnabled = value
    }

    private var isLoaded: Boolean = false
    private var toggleCompletionCallback: Runnable? = null

    open fun onBeforeToggle(newState: Boolean, context: Context): Boolean = true

    fun setToggleCompletionCallback(callback: Runnable) {
        toggleCompletionCallback = callback
    }

    fun applyToggle(newState: Boolean) {
        WePrefs.putBool(path, newState)
        isEnabled = newState
        toggleCompletionCallback?.run()
    }
}
