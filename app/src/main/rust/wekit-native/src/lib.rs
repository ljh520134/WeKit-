//! JNI entry points

mod crash_handler;
mod crash_triggerer;
mod logging;
mod shared;
mod silk_codec;
mod utils;

use std::{ffi::CString, fs::File};

use anyhow::Result;

use crash_handler::{install_crash_handler, uninstall_crash_handler};
use crash_triggerer::trigger_test_crash;

use jni::sys::{
    JNI_FALSE, JNI_TRUE, JNI_VERSION_1_6, JNIEnv as RawJNIEnv, JavaVM, jboolean, jint, jobject,
    jstring,
};
use libc::c_void;

use crate::{
    silk_codec::{mp3_to_pcm_mono, pcm_bytes_2_silk, resample_to},
    utils::with_jstring,
};

// ─────────────────────────────────────────────────────────────────────────────
// JNI exports
// ─────────────────────────────────────────────────────────────────────────────

/// Install the native crash handler.
///
/// Java signature: `(Ljava/lang/String;)Z`
#[unsafe(no_mangle)]
pub extern "C" fn Java_dev_ujhhgtg_wekit_utils_crash_NativeCrashHandler_installNative(
    env: *mut RawJNIEnv,
    _thiz: jobject,
    crash_log_dir: jstring,
) -> jboolean {
    with_jstring(env, crash_log_dir, |dir| {
        if install_crash_handler(dir) {
            JNI_TRUE
        } else {
            JNI_FALSE
        }
    })
}

/// Uninstall the native crash handler.
///
/// Java signature: `()V`
#[unsafe(no_mangle)]
pub extern "C" fn Java_dev_ujhhgtg_wekit_utils_crash_NativeCrashHandler_uninstallNative(
    _env: *mut RawJNIEnv,
    _thiz: jobject,
) {
    uninstall_crash_handler();
}

/// Trigger a deliberate test crash.
///
/// Java signature: `(I)V`
#[unsafe(no_mangle)]
pub extern "C" fn Java_dev_ujhhgtg_wekit_utils_crash_NativeCrashHandler_triggerTestCrashNative(
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
pub unsafe extern "C" fn Java_dev_ujhhgtg_wekit_hooks_items_chat_MarkdownRendering_convertMarkdownToHtmlNative(
    env: *mut RawJNIEnv,
    _thiz: jobject,
    markdown_string: jstring,
) -> jstring {
    let result = with_jstring(env, markdown_string, |md_text| {
        markdown::to_html_with_options(md_text, &markdown::Options::gfm())
    });

    match result {
        Ok(html) => unsafe {
            let fns = *env;
            let c_str = CString::new(html).unwrap_or_default();
            ((*fns).v1_6.NewStringUTF)(env, c_str.as_ptr())
        },
        Err(_) => std::ptr::null_mut(),
    }
}

#[unsafe(no_mangle)]
pub extern "C" fn Java_dev_ujhhgtg_wekit_utils_SilkCodec_mp3ToSilk(
    env: *mut RawJNIEnv,
    _thiz: jobject,
    mp3_path: jstring,
    silk_path: jstring,
) -> jboolean {
    logi!("running mp3ToSilk...");
    with_jstring(env, mp3_path, |mp3| {
        with_jstring(env, silk_path, |silk| {
            logi!("converting {} to {}", mp3, silk);
            match mp3_to_silk(mp3, silk) {
                Ok(_) => {
                    logi!("mp3ToSilk succeeded");
                    JNI_TRUE
                }
                Err(err) => {
                    logi!("mp3ToSilk failed: {:?}", err);
                    JNI_FALSE
                }
            }
        })
    })
}

#[unsafe(no_mangle)]
pub extern "C" fn Java_dev_ujhhgtg_wekit_utils_SilkCodec_silkToPcm(
    env: *mut RawJNIEnv,
    _thiz: jobject,
    silk_path: jstring,
    pcm_path: jstring,
) -> jboolean {
    logi!("running silkToPcm...");
    with_jstring(env, silk_path, |silk| {
        with_jstring(env, pcm_path, |pcm| {
            logi!("converting {} to {}", silk, pcm);
            match silk_codec::silk_to_pcm(silk, pcm, 24000) {
                Ok(_) => {
                    logi!("silkToPcm succeeded");
                    JNI_TRUE
                }
                Err(err) => {
                    logi!("silkToPcm failed: {:?}", err);
                    JNI_FALSE
                }
            }
        })
    })
}

#[unsafe(no_mangle)]
pub extern "C" fn Java_dev_ujhhgtg_wekit_utils_SilkCodec_pcmToMp3(
    env: *mut RawJNIEnv,
    _thiz: jobject,
    pcm_path: jstring,
    mp3_path: jstring,
) -> jboolean {
    logi!("running pcmToMp3...");
    with_jstring(env, pcm_path, |pcm| {
        with_jstring(env, mp3_path, |mp3| {
            logi!("converting {} to {}", pcm, mp3);
            if silk_codec::pcm_to_mp3(pcm, mp3, 24000, 128) {
                logi!("pcmToMp3 succeeded");
                JNI_TRUE
            } else {
                logi!("pcmToMp3 failed");
                JNI_FALSE
            }
        })
    })
}

fn mp3_to_silk(mp3_path: &str, silk_path: &str) -> Result<()> {
    let (pcm, src_rate) = mp3_to_pcm_mono(mp3_path)?;
    logi!("mp3_to_pcm_mono done");
    let pcm = resample_to(&pcm, src_rate, 24000);
    logi!("resample_to done");
    let out_file = File::create(silk_path)?;
    pcm_bytes_2_silk(&pcm, out_file)?;
    logi!("encode_to_silk done");
    Ok(())
}

/// Required JNI library entry point — returns the JNI version we target.
#[unsafe(no_mangle)]
pub extern "C" fn JNI_OnLoad(_vm: *mut JavaVM, _reserved: *mut c_void) -> jint {
    JNI_VERSION_1_6
}
