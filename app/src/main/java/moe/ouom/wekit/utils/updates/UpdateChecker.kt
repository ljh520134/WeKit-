package moe.ouom.wekit.utils.updates

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.ouom.wekit.BuildConfig
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException


object UpdateChecker {
    private const val REPO = "Ujhhgtg/WeKit"
    private const val COMMITS_URL = "https://api.github.com/repos/$REPO/commits/master"
    const val DOWNLOAD_URL = "https://nightly.link/$REPO/workflows/ci/master/wekit-apk.zip"

    suspend fun checkForUpdate(): UpdateResult? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(COMMITS_URL)
            .header("User-Agent", "WeKit")
            .build()

        HttpClient.client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code ${response.code}")
            val obj = JSONObject(response.body.string())
            val latestSha = obj.getString("sha").take(7)
            val message = obj.getJSONObject("commit").getString("message").lines().first()

            if (latestSha != BuildConfig.GIT_HASH)
                UpdateResult(latestSha, message, DOWNLOAD_URL)
            else
                null
        }
    }
}
