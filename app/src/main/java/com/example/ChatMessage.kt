package com.example

data class ChatMessage(
    val role: String, // "user" or "assistant"
    val text: String,
    val imageUris: List<String> = emptyList() // Uri strings or URLs
)
