package com.example

import android.content.Context
import android.content.SharedPreferences

object EvrenPreferences {
    private const val PREFS_NAME = "evren_settings"
    private const val KEY_API_KEY = "api_key"
    private const val KEY_BASE_URL = "base_url"
    private const val KEY_MODEL = "selected_model"

    const val DEFAULT_API_KEY = "evren_llm_PePGMP9OqfFs8lOvovy5o-oJ84jLCL76AAM532MWc-4"
    const val DEFAULT_BASE_URL = "https://evren-llmapi.ssyz.org.tr/v1/"
    const val DEFAULT_MODEL = "qwen3-vl-30b"

    val AVAILABLE_MODELS = listOf(
        "qwen3-vl-30b" to "Qwen 3 VL 30B (Evren LLM - Vizyon Uzmanı)",
        "google/gemini-2.5-flash" to "Gemini 2.5 Flash (Google AI Studio - Hızlı & Vizyon)",
        "google/gemini-2.5-pro" to "Gemini 2.5 Pro (Google AI Studio - Gelişmiş Vizyon)",
        "google/gemini-2.0-flash-001" to "Gemini 2.0 Flash (Google AI Studio)",
        "gemini-3.8-flash" to "Gemini 3.8 Flash (AI Studio Direct)",
        "deepseek-v4.1-flash" to "DeepSeek V4.1 Flash (Evren LLM - Derin Analiz)",
        "mimo-v2.6-pro" to "Mimo V2.6 Pro (Evren LLM - Görsel & Mantık)",
        "gemma-4-31b" to "Gemma 4 31B (Evren LLM - Görsel & Çok Dilli)",
        "qwen3.8-flash-next" to "Qwen 3.8 Flash Next (Evren LLM - Hızlı)",
        "deepseek-v4-flash" to "DeepSeek V4 Flash (Evren LLM - Metin)",
        "glm-5.3" to "GLM 5.3 (Evren LLM - Metin)"
    )

    fun getDefaultsForModel(modelId: String): Pair<String, String> {
        return when {
            modelId == "gemini-3.8-flash" -> {
                Pair(
                    "https://generativelanguage.googleapis.com/v1beta/openai/",
                    BuildConfig.GEMINI_API_KEY
                )
            }
            modelId.startsWith("google/gemini") || modelId.startsWith("gemini") -> {
                Pair(
                    "https://openrouter.ai/api/v1/",
                    BuildConfig.OPENROUTER_API_KEY
                )
            }
            else -> {
                Pair(
                    DEFAULT_BASE_URL,
                    DEFAULT_API_KEY
                )
            }
        }
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getApiKey(context: Context): String {
        val savedKey = getPrefs(context).getString(KEY_API_KEY, "") ?: ""
        if (savedKey.isNotBlank()) {
            return savedKey.trim()
        }
        val currentModel = getModel(context)
        return getDefaultsForModel(currentModel).second
    }

    fun setApiKey(context: Context, key: String) {
        getPrefs(context).edit().putString(KEY_API_KEY, key.trim()).apply()
    }

    fun getBaseUrl(context: Context): String {
        val savedUrl = getPrefs(context).getString(KEY_BASE_URL, "") ?: ""
        if (savedUrl.isNotBlank()) {
            var url = savedUrl.trim()
            if (!url.endsWith("/")) url += "/"
            return url
        }
        val currentModel = getModel(context)
        return getDefaultsForModel(currentModel).first
    }

    fun setBaseUrl(context: Context, url: String) {
        var cleanUrl = url.trim()
        if (cleanUrl.isNotBlank() && !cleanUrl.endsWith("/")) {
            cleanUrl += "/"
        }
        getPrefs(context).edit().putString(KEY_BASE_URL, cleanUrl).apply()
    }

    fun getModel(context: Context): String {
        val saved = getPrefs(context).getString(KEY_MODEL, "") ?: ""
        if (saved.isNotBlank() && AVAILABLE_MODELS.any { it.first == saved }) {
            return saved
        }
        return DEFAULT_MODEL
    }

    fun setModel(context: Context, model: String, autoUpdateCredentials: Boolean = true) {
        getPrefs(context).edit().putString(KEY_MODEL, model.trim()).apply()
        if (autoUpdateCredentials) {
            val (url, key) = getDefaultsForModel(model)
            setBaseUrl(context, url)
            setApiKey(context, key)
        }
    }

    fun getModelDisplayName(modelId: String): String {
        return AVAILABLE_MODELS.firstOrNull { it.first == modelId }?.second ?: modelId
    }

    fun resetToDefaults(context: Context) {
        getPrefs(context).edit()
            .putString(KEY_API_KEY, DEFAULT_API_KEY)
            .putString(KEY_BASE_URL, DEFAULT_BASE_URL)
            .putString(KEY_MODEL, DEFAULT_MODEL)
            .apply()
    }

    fun isConfigured(context: Context): Boolean {
        return getApiKey(context).isNotBlank()
    }
}
