import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

old_analyze = """    fun analyzePlants(base64Images: List<String>, locationContext: String? = null) {
        _uiState.value = BotanyState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val promptText = if (locationContext != null) {
                    "Bu bitkileri analiz et. Bulunduğum konum: $locationContext. Konuma göre bitki teşhisini ve bu bölgedeki endemik/yaygın durumunu da dikkate al."
                } else {
                    "Bu bitkileri analiz et."
                }

                val contentList = mutableListOf<Content>()
                contentList.add(Content(type = "text", text = promptText))
                for (base64 in base64Images) {
                    contentList.add(Content(type = "image_url", image_url = ImageUrl(url = "data:image/jpeg;base64,$base64")))
                }

                val request = OpenRouterRequest(
                    model = "google/gemini-2.0-flash-001",
                    messages = listOf(Message(role = "user", content = contentList))
                )

                val response = OpenRouterClient.api.generateContent("Bearer ${BuildConfig.OPENROUTER_API_KEY}", request)
                val textResponse = response.choices?.firstOrNull()?.message?.content ?: "Sonuç alınamadı."
                withContext(Dispatchers.Main) {
                    _uiState.value = BotanyState.Success(textResponse)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _uiState.value = BotanyState.Error(e.message ?: "Bilinmeyen bir hata oluştu")
                }
            }
        }
    }"""

new_analyze = """    fun analyzePlants(base64Images: List<String>, locationContext: String? = null) {
        _uiState.value = BotanyState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val apiKey = BuildConfig.OPENROUTER_API_KEY
                if (apiKey.isNullOrEmpty() || apiKey.contains("MY_OPENROUTER_KEY") || apiKey == "null") {
                    withContext(Dispatchers.Main) {
                        _uiState.value = BotanyState.Error("OpenRouter API anahtarı eksik. Lütfen AI Studio'da sol menüdeki 'Secrets' (Gizli Anahtarlar) paneline OPENROUTER_API_KEY ekleyin.")
                    }
                    return@launch
                }

                val promptText = if (locationContext != null) {
                    "Bu bitkileri analiz et. Bulunduğum konum: $locationContext. Konuma göre bitki teşhisini ve bu bölgedeki endemik/yaygın durumunu da dikkate al."
                } else {
                    "Bu bitkileri analiz et."
                }

                val contentList = mutableListOf<Content>()
                contentList.add(Content(type = "text", text = promptText))
                for (base64 in base64Images) {
                    contentList.add(Content(type = "image_url", image_url = ImageUrl(url = "data:image/jpeg;base64,$base64")))
                }

                val request = OpenRouterRequest(
                    model = "google/gemini-2.0-flash-001",
                    messages = listOf(Message(role = "user", content = contentList))
                )

                val response = OpenRouterClient.api.generateContent("Bearer $apiKey", request)
                val textResponse = response.choices?.firstOrNull()?.message?.content ?: "Sonuç alınamadı."
                withContext(Dispatchers.Main) {
                    _uiState.value = BotanyState.Success(textResponse)
                }
            } catch (e: retrofit2.HttpException) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    if (e.code() == 401) {
                        _uiState.value = BotanyState.Error("API Anahtarı geçersiz veya yetkisiz (HTTP 401). Lütfen Secrets paneline doğru anahtarı girdiğinizden emin olun.")
                    } else {
                        _uiState.value = BotanyState.Error("HTTP Hatası: ${e.code()} - ${e.message()}")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _uiState.value = BotanyState.Error(e.message ?: "Bilinmeyen bir hata oluştu")
                }
            }
        }
    }"""

content = content.replace(old_analyze, new_analyze)

old_discover = """    fun discoverLocalPlants(locationContext: String) {
        _uiState.value = BotanyState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val promptText = "Bulunduğum bölge: $locationContext. Bu bölgede yetişen önemli, endemik veya yaygın bitkileri, faydalarını ve özelliklerini liste biçiminde anlat."
                
                val contentList = listOf(Content(type = "text", text = promptText))

                val request = OpenRouterRequest(
                    model = "google/gemini-2.0-flash-001",
                    messages = listOf(Message(role = "user", content = contentList))
                )

                val response = OpenRouterClient.api.generateContent("Bearer ${BuildConfig.OPENROUTER_API_KEY}", request)
                val textResponse = response.choices?.firstOrNull()?.message?.content ?: "Sonuç alınamadı."
                withContext(Dispatchers.Main) {
                    _uiState.value = BotanyState.Success(textResponse)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _uiState.value = BotanyState.Error(e.message ?: "Bilinmeyen bir hata oluştu")
                }
            }
        }
    }"""

new_discover = """    fun discoverLocalPlants(locationContext: String) {
        _uiState.value = BotanyState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val apiKey = BuildConfig.OPENROUTER_API_KEY
                if (apiKey.isNullOrEmpty() || apiKey.contains("MY_OPENROUTER_KEY") || apiKey == "null") {
                    withContext(Dispatchers.Main) {
                        _uiState.value = BotanyState.Error("OpenRouter API anahtarı eksik. Lütfen AI Studio'da sol menüdeki 'Secrets' (Gizli Anahtarlar) paneline OPENROUTER_API_KEY ekleyin.")
                    }
                    return@launch
                }

                val promptText = "Bulunduğum bölge: $locationContext. Bu bölgede yetişen önemli, endemik veya yaygın bitkileri, faydalarını ve özelliklerini liste biçiminde anlat."
                
                val contentList = listOf(Content(type = "text", text = promptText))

                val request = OpenRouterRequest(
                    model = "google/gemini-2.0-flash-001",
                    messages = listOf(Message(role = "user", content = contentList))
                )

                val response = OpenRouterClient.api.generateContent("Bearer $apiKey", request)
                val textResponse = response.choices?.firstOrNull()?.message?.content ?: "Sonuç alınamadı."
                withContext(Dispatchers.Main) {
                    _uiState.value = BotanyState.Success(textResponse)
                }
            } catch (e: retrofit2.HttpException) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    if (e.code() == 401) {
                        _uiState.value = BotanyState.Error("API Anahtarı geçersiz veya yetkisiz (HTTP 401). Lütfen Secrets paneline doğru anahtarı girdiğinizden emin olun.")
                    } else {
                        _uiState.value = BotanyState.Error("HTTP Hatası: ${e.code()} - ${e.message()}")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _uiState.value = BotanyState.Error(e.message ?: "Bilinmeyen bir hata oluştu")
                }
            }
        }
    }"""

content = content.replace(old_discover, new_discover)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
