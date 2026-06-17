package com.yourname.voicetest

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject

class TokenFetcher(private val backendUrl: String, private val appSecret: String) {

    private val client = OkHttpClient()

    suspend fun fetchToken(): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$backendUrl/api/app/mint-token")
            .post(RequestBody.create(null, ByteArray(0)))
            .addHeader("x-app-secret", appSecret)
            .build()

        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: throw Exception("Empty token response")
        JSONObject(body).getString("token")
    }
}
