import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

# Replace BotanyViewModel
start_idx = content.find("class BotanyViewModel : ViewModel() {")
end_idx = content.find("@Composable", start_idx)

new_vm = """class BotanyViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<BotanyState>(BotanyState.Idle)
    val uiState: StateFlow<BotanyState> = _uiState.asStateFlow()

    private val apiMessages = mutableListOf<Message>()
    private val uiMessages = mutableListOf<ChatMessage>()

    private fun updateChatState(isTyping: Boolean, error: String? = null) {
        _uiState.value = BotanyState.Chat(uiMessages.toList(), isTyping, error)
    }

    fun dismissError() {
        if (_uiState.value is BotanyState.Chat) {
            updateChatState(isTyping = false, error = null)
        } else {
            _uiState.value = BotanyState.Idle
        }
    }

    fun analyzePlants(base64Images: List<String>, locationContext: String? = null) {
        val promptText = if (locationContext != null) {
            "Lütfen bu bitkileri analiz et ve teşhisini yap. Bulunduğum il/ilçe konumu: $locationContext. Konuma göre bitki teşhisini ve bu bölgedeki endemik/yaygın durumunu dikkate al. Bu bitkilerin belirgin özelliklerini, başka hangi bölgelerde yetiştiğini ve ne işe yaradıklarını (faydalarını) detaylıca Türkçe olarak açıkla."
        } else {
            "Lütfen bu bitkileri analiz et ve teşhisini yap. Belirgin özelliklerini, nerelerde yetiştiğini ve ne işe yaradıklarını detaylıca Türkçe olarak açıkla."
        }

        uiMessages.add(ChatMessage(isUser = true, text = promptText, imageBase64s = base64Images))
        
        val contentList = mutableListOf<Content>()
        contentList.add(Content(type = "text", text = promptText))
        for (base64 in base64Images) {
            contentList.add(Content(type = "image_url", image_url = ImageUrl(url = "data:image/jpeg;base64,$base64")))
        }
        apiMessages.add(Message(role = "user", content = contentList))

        sendRequest()
    }

    fun discoverLocalPlants(locationContext: String) {
        val promptText = "Bulunduğum il ve ilçe konumu: $locationContext. Sadece bulunduğum ülkeye göre değil, doğrudan bu il ve ilçe sınırları içerisinde yetişen bitkilere odaklan. Bu bölgeye özgü (endemik) olan veya yaygın olarak bulunan önemli bitkileri listele. Bu bitkilerin başka nerelerde yetiştiğini, belirgin özelliklerini ve ne işe yaradıklarını (faydalarını) liste biçiminde detaylıca Türkçe olarak anlat."
        
        uiMessages.add(ChatMessage(isUser = true, text = promptText))
        val contentList = listOf(Content(type = "text", text = promptText))
        apiMessages.add(Message(role = "user", content = contentList))
        
        sendRequest()
    }

    fun sendMessage(text: String) {
        uiMessages.add(ChatMessage(isUser = true, text = text))
        apiMessages.add(Message(role = "user", content = listOf(Content(type = "text", text = text))))
        sendRequest()
    }

    private fun sendRequest() {
        updateChatState(isTyping = true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val apiKey = BuildConfig.OPENROUTER_API_KEY
                if (apiKey.isNullOrEmpty() || apiKey.contains("MY_OPENROUTER_KEY") || apiKey == "null") {
                    withContext(Dispatchers.Main) {
                        updateChatState(isTyping = false, error = "OpenRouter API anahtarı eksik. Lütfen AI Studio'da sol menüdeki 'Secrets' paneline OPENROUTER_API_KEY ekleyin.")
                    }
                    return@launch
                }

                val request = OpenRouterRequest(
                    model = "google/gemini-2.5-flash",
                    messages = apiMessages.toList(),
                    max_tokens = 2000
                )

                val response = OpenRouterClient.api.generateContent("Bearer $apiKey", request)
                val textResponse = response.choices?.firstOrNull()?.message?.content ?: "Sonuç alınamadı."
                
                withContext(Dispatchers.Main) {
                    apiMessages.add(Message(role = "assistant", content = listOf(Content(type = "text", text = textResponse))))
                    uiMessages.add(ChatMessage(isUser = false, text = textResponse))
                    updateChatState(isTyping = false)
                }
            } catch (e: retrofit2.HttpException) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    val errorMsg = when (e.code()) {
                        401 -> "API Anahtarı geçersiz veya yetkisiz (HTTP 401)."
                        402 -> "OpenRouter bakiyeniz yetersiz (HTTP 402)."
                        404 -> "OpenRouter modeli bulunamadı (HTTP 404)."
                        else -> "HTTP Hatası: ${e.code()} - ${e.message()}"
                    }
                    apiMessages.removeLastOrNull()
                    updateChatState(isTyping = false, error = errorMsg)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    apiMessages.removeLastOrNull()
                    updateChatState(isTyping = false, error = e.message ?: "Bilinmeyen bir hata oluştu")
                }
            }
        }
    }
}

"""

content = content[:start_idx] + new_vm + content[end_idx:]

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
