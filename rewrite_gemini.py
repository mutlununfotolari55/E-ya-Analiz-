import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

# Replace FirebaseVertexAI imports with Google AI generativeai imports
content = content.replace('import com.google.firebase.vertexai.FirebaseVertexAI\n', '')
content = content.replace('import com.google.firebase.vertexai.type.content', 'import com.google.ai.client.generativeai.type.content\nimport com.google.ai.client.generativeai.GenerativeModel')

# Replace the instantiation
content = content.replace('FirebaseVertexAI.getInstance(FirebaseApp.getInstance()).generativeModel("gemini-1.5-flash")', 'GenerativeModel(modelName = "gemini-1.5-flash", apiKey = BuildConfig.GEMINI_API_KEY)')

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
