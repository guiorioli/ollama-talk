package com.guiorioli.ollamatalk.data.api

import com.google.gson.Gson
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.catch
import java.io.IOException
import java.util.concurrent.TimeUnit

class OllamaApiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMediaType = "application/json".toMediaType()

    companion object {
        private const val BASE_URL = "https://ollama.com"
    }

    @Volatile
    private var currentCall: Call? = null

    private fun buildAuthorizedRequest(path: String, apiKey: String): Request.Builder {
        return Request.Builder()
            .url("$BASE_URL$path")
            .header("Authorization", "Bearer $apiKey")
    }

    fun chat(
        model: String,
        messages: List<ChatMessage>,
        apiKey: String
    ): Result<ChatResponse> {
        val requestBody = ChatRequest(
            model = model,
            messages = messages,
            stream = false
        )
        val jsonBody = gson.toJson(requestBody)

        val request = buildAuthorizedRequest("/api/chat", apiKey)
            .post(jsonBody.toRequestBody(jsonMediaType))
            .build()

        return try {
            val call = client.newCall(request)
            currentCall = call
            val response = call.execute()
            currentCall = null
            val body = response.body?.string()
            if (response.isSuccessful && body != null) {
                val chatResponse = gson.fromJson(body, ChatResponse::class.java)
                Result.success(chatResponse)
            } else {
                Result.failure(IOException("Erro ${response.code}: ${response.message}"))
            }
        } catch (e: Exception) {
            currentCall = null
            Result.failure(e)
        }
    }

    fun cancelChat() {
        currentCall?.cancel()
        currentCall = null
    }

    fun chatAsFlow(
        model: String,
        messages: List<ChatMessage>,
        apiKey: String
    ): Flow<String> = flow {
        val requestBody = ChatRequest(
            model = model,
            messages = messages,
            stream = true
        )
        val jsonBody = gson.toJson(requestBody)

        val request = buildAuthorizedRequest("/api/chat", apiKey)
            .post(jsonBody.toRequestBody(jsonMediaType))
            .build()

        val call = client.newCall(request)
        currentCall = call

        val response = call.execute()
        if (!response.isSuccessful) {
            throw IOException("Erro ${response.code}: ${response.message}")
        }

        val source = response.body?.source() ?: throw IOException("Empty body")
        val fullTextBuilder = StringBuilder()

        while (!source.exhausted()) {
            val line = source.readUtf8Line() ?: break
            if (line.isBlank()) continue
            val chunk = gson.fromJson(line, ChatStreamChunk::class.java)
            val content = chunk.message.content
            if (content.isNotEmpty()) {
                fullTextBuilder.append(content)
                emit(content)
            }
            if (chunk.done) break
        }
        currentCall = null
    }.catch { e ->
        currentCall = null
        throw e
    }

    fun listModels(apiKey: String): Result<List<ModelInfo>> {
        val request = buildAuthorizedRequest("/api/tags", apiKey)
            .get()
            .build()

        return try {
            val response = client.newCall(request).execute()
            val body = response.body?.string()
            if (response.isSuccessful && body != null) {
                val tagsResponse = gson.fromJson(body, TagsResponse::class.java)
                Result.success(tagsResponse.models ?: emptyList())
            } else {
                Result.failure(IOException("Erro ${response.code}: ${response.message}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun validateApiKey(apiKey: String): Boolean {
        val result = listModels(apiKey)
        return result.isSuccess
    }
}
