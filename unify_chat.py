import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

# 1. Update BotanyState
old_state = """sealed class BotanyState {
    object Idle : BotanyState()
    data class Chat(val messages: List<ChatMessage>, val isTyping: Boolean, val error: String? = null) : BotanyState()
}"""
new_state = """sealed class BotanyState {
    data class Chat(val messages: List<ChatMessage>, val isTyping: Boolean, val error: String? = null) : BotanyState()
}"""
content = content.replace(old_state, new_state)

# 2. Update BotanyViewModel initialization
content = content.replace(
    "private val _uiState = MutableStateFlow<BotanyState>(BotanyState.Idle)",
    "private val _uiState = MutableStateFlow<BotanyState>(BotanyState.Chat(emptyList(), false))"
)
content = content.replace(
    "_uiState.value = BotanyState.Idle",
    "_uiState.value = BotanyState.Chat(uiMessages.toList(), false)"
)

# 3. Update sendMessage to take base64 images
old_send_msg = """    fun sendMessage(text: String) {
        uiMessages.add(ChatMessage(isUser = true, text = text))
        apiMessages.add(Message(role = "user", content = listOf(Content(type = "text", text = text))))
        sendRequest()
    }"""
new_send_msg = """    fun sendMessage(text: String, base64Images: List<String> = emptyList()) {
        uiMessages.add(ChatMessage(isUser = true, text = text, imageBase64s = base64Images))
        
        val contentList = mutableListOf<Content>()
        if (text.isNotBlank()) contentList.add(Content(type = "text", text = text))
        for (base64 in base64Images) {
            contentList.add(Content(type = "image_url", image_url = ImageUrl(url = "data:image/jpeg;base64,$base64")))
        }
        apiMessages.add(Message(role = "user", content = contentList))
        sendRequest()
    }"""
content = content.replace(old_send_msg, new_send_msg)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
