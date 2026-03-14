//! JNI entry points

#![allow(non_snake_case)]

mod crash_handler;
mod crash_triggerer;
mod logging;
mod shared;
mod utils;

use std::ffi::CString;

use crash_handler::{install_crash_handler, uninstall_crash_handler};
use crash_triggerer::trigger_test_crash;

use jni::sys::{
    JNI_FALSE, JNI_TRUE, JNI_VERSION_1_6, JNIEnv as RawJNIEnv, JavaVM, jboolean, jint, jobject,
    jstring,
};
use libc::c_void;

use crate::utils::with_jstring;

// ─────────────────────────────────────────────────────────────────────────────
// JNI exports
// ─────────────────────────────────────────────────────────────────────────────

/// Install the native crash handler.
///
/// Java signature: `(Ljava/lang/String;)Z`
#[unsafe(no_mangle)]
pub unsafe extern "C" fn Java_moe_ouom_wekit_utils_crash_NativeCrashHandler_installNative(
    env: *mut RawJNIEnv,
    _thiz: jobject,
    crash_log_dir: jstring,
) -> jboolean {
    unsafe {
        with_jstring(env, crash_log_dir, |dir| {
            if install_crash_handler(dir) {
                JNI_TRUE
            } else {
                JNI_FALSE
            }
        })
        .unwrap_or(JNI_FALSE)
    }
}

/// Uninstall the native crash handler.
///
/// Java signature: `()V`
#[unsafe(no_mangle)]
pub unsafe extern "C" fn Java_moe_ouom_wekit_utils_crash_NativeCrashHandler_uninstallNative(
    _env: *mut RawJNIEnv,
    _thiz: jobject,
) {
    uninstall_crash_handler();
}

/// Trigger a deliberate test crash.
///
/// Java signature: `(I)V`
#[unsafe(no_mangle)]
pub unsafe extern "C" fn Java_moe_ouom_wekit_utils_crash_NativeCrashHandler_triggerTestCrashNative(
    _env: *mut RawJNIEnv,
    _thiz: jobject,
    crash_type: jint,
) {
    trigger_test_crash(crash_type);
}

/// Convert a Markdown string to HTML.
///
/// Java signature: `(Ljava/lang/String;)Ljava/lang/String;`
#[unsafe(no_mangle)]
pub unsafe extern "C" fn Java_moe_ouom_wekit_hooks_items_chat_MarkdownRendering_convertMarkdownToHtmlNative(
    env: *mut RawJNIEnv,
    _thiz: jobject,
    markdown_string: jstring,
) -> jstring {
    let html_option = unsafe {
        with_jstring(env, markdown_string, |md_text| {
            // 2. Convert Markdown to HTML
            markdown::to_html_with_options(md_text, &markdown::Options::gfm())
        })
    };

    // 3. Convert the resulting String back to a Java jstring
    match html_option {
        Some(html_result) => unsafe {
            match html_result {
                Ok(html) => {
                    let fns = *env;
                    let c_str = CString::new(html).unwrap_or_default();
                    ((*fns).v1_6.NewStringUTF)(env, c_str.as_ptr())
                }
                Err(_) => std::ptr::null_mut(),
            }
        },
        None => std::ptr::null_mut(),
    }
}

/// Required JNI library entry point — returns the JNI version we target.
#[unsafe(no_mangle)]
pub extern "C" fn JNI_OnLoad(_vm: *mut JavaVM, _reserved: *mut c_void) -> jint {
    JNI_VERSION_1_6
}
