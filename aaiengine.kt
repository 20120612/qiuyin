package com.qiuyin.pet

import android.os.Handler
import android.os.Looper
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * DeepSeek AI 台词引擎
 * 根据「场景描述」让 DeepSeek 生成一句符合秋隐性格、有情绪的台词。
 * 纯本地调用，失败时回调 onFail，由 PetEngine 用预设话兜底。
 */
class AiEngine(
    private var apiKey: String,
    private val handler: Handler = Handler(Looper.getMainLooper())
) {

interface AiCallback {
        fun onSuccess(text: String)
        fun onFail(reason: String)
    }

private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
    }

fun setApiKey(key: String) {
        apiKey = key
    }

fun generate(scene: String, mood: String, cb: AiCallback) {
        if (apiKey.isBlank()) {
            cb.onFail("no_key")
            return
        }
        val prompt = buildPrompt(scene, mood)
        val bodyJson = JSONObject()
            .put("model", "deepseek-chat")
            .put("temperature", 1.3)
            .put("max_tokens", 60)
            .put("messages", JSONArray()
                .put(JSONObject().put("role", "user").put("content", prompt))
            )

val request = Request.Builder()
            .url("https://api.deepseek.com/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                handler.post { cb.onFail("network:${e.message}") }
            }

override fun onResponse(call: Call, response: okhttp3.Response) {
                try {
                    if (!response.isSuccessful) {
                        handler.post { cb.onFail("http_${response.code}") }
                        response.close()
                        return
                    }
                    val text = response.body?.string()
                    response.close()
                    val content = parseContent(text)
                    if (content.isNullOrBlank()) {
                        handler.post { cb.onFail("empty") }
                    } else {
                        handler.post { cb.onSuccess(clean(content)) }
                    }
                } catch (e: Exception) {
                    handler.post { cb.onFail("parse:${e.message}") }
                }
            }
        })
    }
