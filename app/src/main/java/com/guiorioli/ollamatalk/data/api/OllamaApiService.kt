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

        // Hardcoded list of known cloud models that support tool calling
        // Verified against https://ollama.com/search?c=cloud&c=tools on 2026-05-18
        val KNOWN_TOOLS_MODELS = setOf(
            "kimi-k2.6",
            "deepseek-v4-flash",
            "deepseek-v4-pro",
            "gemma4",
            "qwen3.5",
            "glm-5.1",
            "minimax-m2.7",
            "nemotron-3-super",
            "glm-5",
            "minimax-m2.5",
            "qwen3-coder-next",
            "glm-4.7",
            "gemini-3-flash-preview",
            "minimax-m2.1",
            "deepseek-v3.2",
            "ministral-3",
            "devstral-small-2",
            "qwen3-next",
            "nemotron-3-nano",
            "rnj-1"
        )

        val WEB_SEARCH_TOOL = Tool(
            function = ToolFunction(
                name = "web_search",
                description = "Search the web for current information, news, facts, or data that may not be in the model's training data. Use when the user asks about recent events, current data, or anything that requires up-to-date information.",
                parameters = ToolParameters(
                    required = listOf("query"),
                    properties = mapOf(
                        "query" to ToolProperty(
                            type = "string",
                            description = "The search query string. Be specific and include relevant keywords."
                        ),
                        "max_results" to ToolProperty(
                            type = "integer",
                            description = "Maximum number of results to return (1-10). Default is 5.",
                            default = 5
                        )
                    )
                )
            )
        )
    }

    @Volatile
    private var currentCall: Call? = null

    @Volatile
    private var currentWebSearchCall: Call? = null

    private fun buildAuthorizedRequest(path: String, apiKey: String): Request.Builder {
        return Request.Builder()
            .url("$BASE_URL$path")
            .header("Authorization", "Bearer $apiKey")
    }

    fun chat(
        model: String,
        messages: List<ChatMessage>,
        apiKey: String,
        tools: List<Tool>? = null
    ): Result<ChatResponse> {
        val requestBody = ChatRequest(
            model = model,
            messages = messages,
            stream = false,
            tools = tools
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
                val message = when (response.code) {
                    401 -> "Invalid API Key"
                    403 -> "Access denied"
                    else -> "Error ${response.code}: ${response.message}"
                }
                Result.failure(IOException(message))
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

    fun cancelWebSearch() {
        currentWebSearchCall?.cancel()
        currentWebSearchCall = null
    }

    fun cancelAllCalls() {
        cancelChat()
        cancelWebSearch()
    }

    fun chatAsFlow(
        model: String,
        messages: List<ChatMessage>,
        apiKey: String,
        tools: List<Tool>? = null
    ): Flow<String> = flow {
        val requestBody = ChatRequest(
            model = model,
            messages = messages,
            stream = true,
            tools = tools
        )
        val jsonBody = gson.toJson(requestBody)

        val request = buildAuthorizedRequest("/api/chat", apiKey)
            .post(jsonBody.toRequestBody(jsonMediaType))
            .build()

        val call = client.newCall(request)
        currentCall = call

        val response = call.execute()
        if (!response.isSuccessful) {
            val message = when (response.code) {
                401 -> "Invalid API Key. Please check your key in Settings."
                403 -> "Access denied. Your API Key may be invalid or expired."
                429 -> "Rate limit exceeded. Please wait a moment and try again."
                else -> "Server error ${response.code}: ${response.message}"
            }
            throw IOException(message)
        }

        val source = response.body?.source() ?: throw IOException("Empty body")
        val fullTextBuilder = StringBuilder()

        while (!source.exhausted()) {
            val line = source.readUtf8Line() ?: break
            if (line.isBlank()) continue
            val chunk = gson.fromJson(line, ChatStreamChunk::class.java)
            val content = chunk.message.content
            if (!content.isNullOrEmpty()) {
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

    // --- Web Search ---

    fun webSearch(query: String, maxResults: Int = 5, apiKey: String): Result<WebSearchResponse> {
        val requestBody = mapOf(
            "query" to query,
            "max_results" to maxResults.coerceIn(1, 10)
        )
        val jsonBody = gson.toJson(requestBody)

        val request = buildAuthorizedRequest("/api/web_search", apiKey)
            .post(jsonBody.toRequestBody(jsonMediaType))
            .build()

        return try {
            val call = client.newCall(request)
            currentWebSearchCall = call
            val response = call.execute()
            currentWebSearchCall = null
            val body = response.body?.string()
            if (response.isSuccessful && body != null) {
                val searchResponse = gson.fromJson(body, WebSearchResponse::class.java)
                Result.success(searchResponse)
            } else {
                Result.failure(IOException("Web search error ${response.code}: ${response.message}"))
            }
        } catch (e: Exception) {
            currentWebSearchCall = null
            Result.failure(e)
        }
    }

    // --- Model Tool Support Verification (Scraping) ---

    fun checkModelSupportsTools(modelName: String): Result<Boolean> {
        return try {
            val request = Request.Builder()
                .url("$BASE_URL/search?c=cloud&c=tools")
                .header("User-Agent", "OllamaTalk/1.0")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return Result.failure(IOException("Empty response body"))

            // Simple check: is the model name contained anywhere in the HTML?
            val isSupported = body.contains("/library/$modelName", ignoreCase = true)
            Result.success(isSupported)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
