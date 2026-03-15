package moe.ouom.wekit.utils.updates

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object HttpClient {
    val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
}
