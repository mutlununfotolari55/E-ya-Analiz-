import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

# Change model to gemini-2.5-flash and add max_tokens
content = content.replace(
    """val request = OpenRouterRequest(
                    model = "google/gemini-2.5-flash",
                    messages = listOf(Message(role = "user", content = contentList))
                )""",
    """val request = OpenRouterRequest(
                    model = "google/gemini-2.5-flash",
                    messages = listOf(Message(role = "user", content = contentList)),
                    max_tokens = 2000
                )"""
)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
