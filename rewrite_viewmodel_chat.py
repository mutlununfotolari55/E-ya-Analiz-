import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

# Add ChatMessage data class
chat_msg_class = """data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val imageBase64s: List<String> = emptyList()
)

sealed class BotanyState {
    object Idle : BotanyState()
    data class Chat(val messages: List<ChatMessage>, val isTyping: Boolean, val error: String? = null) : BotanyState()
}"""

content = re.sub(
    r"sealed class BotanyState \{.*?\n\}",
    chat_msg_class,
    content,
    flags=re.DOTALL
)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
