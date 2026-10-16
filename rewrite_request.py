import re

with open('app/src/main/java/com/example/OpenRouterApi.kt', 'r') as f:
    content = f.read()

old_req = """@JsonClass(generateAdapter = true)
data class OpenRouterRequest(
    val model: String,
    val messages: List<Message>
)"""

new_req = """@JsonClass(generateAdapter = true)
data class OpenRouterRequest(
    val model: String,
    val messages: List<Message>,
    val max_tokens: Int? = null
)"""

content = content.replace(old_req, new_req)

with open('app/src/main/java/com/example/OpenRouterApi.kt', 'w') as f:
    f.write(content)
