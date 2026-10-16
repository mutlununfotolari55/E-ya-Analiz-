@file:OptIn(ExperimentalPermissionsApi::class)
package com.example

import android.Manifest
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import com.example.ui.ProfessionalAnalysisCard
import com.example.ui.ScanHistoryCardItem
import com.example.ui.ScanHistoryDialog
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextLight
import com.example.ui.theme.TextMuted
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize().imePadding()) { innerPadding ->
                    EvrenScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

fun android.content.Context.createImageFileUri(): Uri {
    val imageFile = File(cacheDir, "evren_capture_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(
        this,
        "${packageName}.fileprovider",
        imageFile
    )
}

fun Uri.toBase64(context: android.content.Context): String? {
    return try {
        val bytes = context.contentResolver.openInputStream(this)?.readBytes()
        if (bytes != null) {
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } else null
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

sealed class EvrenState {
    object Idle : EvrenState()
    object Loading : EvrenState()
    data class Success(val messages: List<ChatMessage>, val isChatLoading: Boolean = false) : EvrenState()
    data class Error(val message: String, val isApiKeyError: Boolean = false) : EvrenState()
}

class EvrenViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<EvrenState>(EvrenState.Idle)
    val uiState: StateFlow<EvrenState> = _uiState.asStateFlow()

    private val apiMessages = mutableListOf<Message>()
    private val uiMessages = mutableListOf<ChatMessage>()
    var activeLocation: String? = null
        private set

    fun setRegionContext(location: String) {
        activeLocation = location
        val welcomeText = "📍 Bölge tespit edildi: $location\n\nBu konum hafızaya alındı. Artık bu bölgenin coğrafyası, yerel özellikleri, tarihi hakkında soru sorabilir veya fotoğraf çekerek analiz alabilirsiniz. Size nasıl yardımcı olabilirim?"
        
        uiMessages.add(ChatMessage(role = "assistant", text = welcomeText))
        apiMessages.add(
            Message(
                role = "system",
                content = listOf(
                    Content(
                        type = "text",
                        text = "Kullanıcının bulunduğu aktif konum/bölge: '$location'. Kullanıcının soracağı tüm soruları veya göndereceği görselleri değerlendirirken gerektiğinde bu bölgeyi ve yerel şartları dikkate al."
                    )
                )
            )
        )
        apiMessages.add(
            Message(
                role = "assistant",
                content = listOf(Content(type = "text", text = welcomeText))
            )
        )
        _uiState.value = EvrenState.Success(uiMessages.toList())
    }

    fun analyzeOmni(
        base64Images: List<String>, 
        locationContext: String? = null,
        userImageUris: List<String> = emptyList(),
        context: android.content.Context
    ) {
        val apiKey = EvrenPreferences.getApiKey(context)
        val selectedModel = EvrenPreferences.getModel(context)

        if (apiKey.isBlank()) {
            _uiState.value = EvrenState.Error(
                "API Anahtarı eksik veya tanımlanmamış. Lütfen sağ üstteki Anahtar (🔑) simgesine dokunarak API anahtarınızı girin.",
                isApiKeyError = true
            )
            return
        }

        _uiState.value = EvrenState.Loading
        apiMessages.clear()
        uiMessages.clear()

        val activeLoc = locationContext ?: activeLocation

        // User message with attached images displayed in chat
        val userPromptLabel = if (userImageUris.size > 1) {
            "📸 Görsel Analiz İsteği (${userImageUris.size} fotoğraf)"
        } else {
            "📸 Görsel Analiz İsteği"
        }
        uiMessages.add(ChatMessage(role = "user", text = userPromptLabel, imageUris = userImageUris))
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val promptBuilder = StringBuilder()
                promptBuilder.append("Sen 'Evren' adında, her türlü nesneyi, ortamı, teknolojiyi, canlıyı ve durumu analiz eden profesyonel bir yapay zeka vizyon sistemisin.\n")
                promptBuilder.append("ÖNEMLİ KURAL: Yanıtların KISA, ÖZ, NET ve DOĞRUDAN ODAKLI olmalıdır. Aşırı uzun cümleler ve gereksiz laf kalabalığı yapma. Doğrudan konuya gir.\n")
                promptBuilder.append("1. EĞER GÖRSELDE BİR BİTKİ, MANTAR VEYA GIDA VARSA: İlk satırda ZEHİRLİ mi, TÜKETİLEBİLİR mi, FAYDALI mı açıkça belirt.\n")
                promptBuilder.append("2. Görseldeki nesnenin/durumun ana tanımını net bir başlıkla belirt (örn: # BAŞLIK).\n")
                promptBuilder.append("3. Ne işe yaradığını veya özelliğini 2-3 kısa maddede ya da tek bir öz paragrafla açıkla.\n")
                promptBuilder.append("4. Varsa 1-2 kritik pratik ipucu ekle.\n")
                if (!activeLoc.isNullOrBlank()) {
                    promptBuilder.append("5. Not: Kullanıcının bulunduğu aktif konum: '$activeLoc'. Gerekirse yerel şartları kısaca dikkate al.\n")
                }
                promptBuilder.append("Başlıkları belirgin yap (# veya ## ile), paragrafları okunaklı, net ve kısa tut.")

                val contentList = mutableListOf<Content>()
                contentList.add(Content(type = "text", text = promptBuilder.toString()))
                for (base64 in base64Images) {
                    contentList.add(Content(type = "image_url", image_url = ImageUrl(url = "data:image/jpeg;base64,$base64")))
                }

                apiMessages.add(Message(role = "user", content = contentList))

                val request = OpenRouterRequest(
                    model = selectedModel,
                    messages = apiMessages.toList(),
                    max_tokens = 2000
                )

                val baseUrl = EvrenPreferences.getBaseUrl(context)
                val response = OpenRouterClient.getApi(baseUrl).generateContent("Bearer $apiKey", request)
                val textResponse = response.choices?.firstOrNull()?.message?.content ?: "Analiz yanıtı üretilemedi."
                
                apiMessages.add(Message(role = "assistant", content = listOf(Content(type = "text", text = textResponse))))
                uiMessages.add(ChatMessage(role = "assistant", text = textResponse))
                
                // Auto-save scan into persistent history!
                try {
                    val firstCleanLine = textResponse.lines()
                        .firstOrNull { it.isNotBlank() }
                        ?.replace("#", "")
                        ?.replace("*", "")
                        ?.trim() ?: "Görsel Analizi"
                    val title = if (firstCleanLine.length > 35) firstCleanLine.take(32) + "..." else firstCleanLine
                    ScanHistoryManager.addScan(
                        context = context,
                        item = ScanHistoryItem(
                            title = title,
                            previewText = textResponse.take(120).replace("\n", " "),
                            fullResponse = textResponse,
                            imageUris = userImageUris,
                            location = activeLoc
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                withContext(Dispatchers.Main) {
                    _uiState.value = EvrenState.Success(uiMessages.toList())
                }
            } catch (e: retrofit2.HttpException) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    val errorBody = try { e.response()?.errorBody()?.string() } catch(ex: Exception) { null }
                    when (e.code()) {
                        401 -> _uiState.value = EvrenState.Error("API Anahtarı yetkisiz (HTTP 401). Lütfen anahtarı kontrol edin.", isApiKeyError = true)
                        402 -> _uiState.value = EvrenState.Error("API Bakiyesi yetersiz (HTTP 402).")
                        404 -> _uiState.value = EvrenState.Error("Model veya endpoint bulunamadı (HTTP 404). Lütfen ayarları kontrol edin.", isApiKeyError = true)
                        else -> _uiState.value = EvrenState.Error("API Hatası (${e.code()}): ${errorBody ?: e.message()}")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _uiState.value = EvrenState.Error(e.message ?: "Bilinmeyen bir hata oluştu")
                }
            }
        }
    }

    fun loadScanHistoryItem(item: ScanHistoryItem) {
        uiMessages.clear()
        apiMessages.clear()

        val userPrompt = if (item.imageUris.size > 1) {
            "📸 Görsel Analiz İsteği (${item.imageUris.size} fotoğraf)"
        } else if (item.imageUris.isNotEmpty()) {
            "📸 Görsel Analiz İsteği"
        } else {
            "📍 Bölge Taraması ve Analiz"
        }

        uiMessages.add(ChatMessage(role = "user", text = userPrompt, imageUris = item.imageUris))
        uiMessages.add(ChatMessage(role = "assistant", text = item.fullResponse))

        apiMessages.add(Message(role = "user", content = listOf(Content(type = "text", text = userPrompt))))
        apiMessages.add(Message(role = "assistant", content = listOf(Content(type = "text", text = item.fullResponse))))

        _uiState.value = EvrenState.Success(uiMessages.toList())
    }

    private val stagedImageUris = mutableListOf<Uri>()

    fun startChatWithImages(
        uris: List<Uri>,
        locationContext: String? = null
    ) {
        val activeLoc = locationContext ?: activeLocation
        stagedImageUris.clear()
        stagedImageUris.addAll(uris)
        uiMessages.clear()
        apiMessages.clear()

        val uriStrings = uris.map { it.toString() }
        val userLabel = if (uris.size > 1) {
            "📸 ${uris.size} fotoğraf yüklendi"
        } else {
            "📸 Fotoğraf yüklendi"
        }
        uiMessages.add(ChatMessage(role = "user", text = userLabel, imageUris = uriStrings))

        val welcomeText = if (uris.size > 1) {
            "Görseller hazır! 🎯\nBu fotoğraflar hakkında ne öğrenmek veya analiz ettirmek istersiniz? Yukarıdan veya aşağıdan hemen sorunuzu sorabilirsiniz."
        } else {
            "Fotoğraf hazır! 🎯\nBu görsel hakkında ne öğrenmek veya analiz ettirmek istersiniz? Yukarıdan veya aşağıdan hemen sorunuzu sorabilirsiniz."
        }
        uiMessages.add(ChatMessage(role = "assistant", text = welcomeText))

        _uiState.value = EvrenState.Success(uiMessages.toList(), isChatLoading = false)
    }

    fun resetToHome() {
        stagedImageUris.clear()
        uiMessages.clear()
        apiMessages.clear()
        _uiState.value = EvrenState.Idle
    }
    
    fun sendChatMessage(
        text: String,
        attachedBase64Images: List<String> = emptyList(),
        attachedUris: List<String> = emptyList(),
        context: android.content.Context
    ) {
        val apiKey = EvrenPreferences.getApiKey(context)
        val selectedModel = EvrenPreferences.getModel(context)

        if (apiKey.isBlank()) {
            _uiState.value = EvrenState.Error(
                "API Anahtarı eksik. Lütfen sağ üstteki Anahtar (🔑) simgesinden anahtarınızı girin.",
                isApiKeyError = true
            )
            return
        }

        uiMessages.add(ChatMessage(role = "user", text = text, imageUris = attachedUris))
        // Placeholder assistant message for gradual token reveal
        uiMessages.add(ChatMessage(role = "assistant", text = ""))
        val currentMessages = uiMessages.toList()
        _uiState.value = EvrenState.Success(currentMessages, isChatLoading = true)
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (apiMessages.none { it.role == "system" }) {
                    val activeLoc = activeLocation
                    val systemPrompt = buildString {
                        append("Sen 'Evren' adında, görseldeki nesneleri, ortamları, teknolojiyi, canlıları ve metinleri analiz eden yüksek düzeyli bir yapay zeka vizyon asistanısın.\n")
                        append("ÖNEMLİ KURAL: Yanıtların KISA, ÖZ, NET ve DOĞRUDAN ODAKLI olmalıdır. Aşırı uzun, gereksiz cümleler ve laf kalabalığı yapma. Doğrudan konuya gir.\n")
                        append("1. EĞER GÖRSELDE BİR BİTKİ, MANTAR VEYA GIDA VARSA: İlk satırda ZEHİRLİ mi, TÜKETİLEBİLİR mi, FAYDALI mı açıkça belirt.\n")
                        append("2. Tanımı net bir başlıkla belirt (örn: # BAŞLIK).\n")
                        append("3. Ne işe yaradığını veya özelliğini 2-3 kısa maddede ya da tek bir öz paragrafla açıkla.\n")
                        append("4. Varsa 1-2 kritik pratik ipucu ekle.\n")
                        if (!activeLoc.isNullOrBlank()) {
                            append("Kullanıcının aktif konumu: '$activeLoc'. Gerekirse yerel bağlamı belirt.\n")
                        }
                        append("Başlıkları belirgin yap (# veya ## ile), paragrafları okunaklı, net ve kısa tut.")
                    }
                    apiMessages.add(Message(role = "system", content = listOf(Content(type = "text", text = systemPrompt))))
                }

                val contentList = mutableListOf<Content>()
                contentList.add(Content(type = "text", text = text))

                // Convert staged images to base64 if not sent yet
                val imagesToSend = mutableListOf<String>()
                if (stagedImageUris.isNotEmpty()) {
                    for (u in stagedImageUris) {
                        u.toBase64(context)?.let { imagesToSend.add(it) }
                    }
                    stagedImageUris.clear()
                }
                imagesToSend.addAll(attachedBase64Images)

                for (b64 in imagesToSend) {
                    contentList.add(Content(type = "image_url", image_url = ImageUrl(url = "data:image/jpeg;base64,$b64")))
                }

                apiMessages.add(Message(role = "user", content = contentList))
                
                val baseUrl = EvrenPreferences.getBaseUrl(context)
                val request = OpenRouterRequest(
                    model = selectedModel,
                    messages = apiMessages.toList(),
                    max_tokens = 1500,
                    stream = true
                )

                val accumulatedText = StringBuilder()
                var streamedSuccessfully = false

                try {
                    val responseBody = OpenRouterClient.getApi(baseUrl).streamContent("Bearer $apiKey", request)
                    val reader = responseBody.byteStream().bufferedReader()
                    var line: String? = reader.readLine()
                    while (line != null) {
                        val trimmed = line.trim()
                        if (trimmed.startsWith("data: ")) {
                            val data = trimmed.removePrefix("data: ").trim()
                            if (data == "[DONE]") {
                                break
                            }
                            try {
                                val json = org.json.JSONObject(data)
                                val choices = json.optJSONArray("choices")
                                if (choices != null && choices.length() > 0) {
                                    val delta = choices.getJSONObject(0).optJSONObject("delta")
                                    val chunk = delta?.optString("content")
                                    if (!chunk.isNullOrEmpty()) {
                                        accumulatedText.append(chunk)
                                        val currentText = accumulatedText.toString()
                                        streamedSuccessfully = true
                                        withContext(Dispatchers.Main) {
                                            if (uiMessages.isNotEmpty() && uiMessages.last().role == "assistant") {
                                                uiMessages[uiMessages.lastIndex] = ChatMessage(role = "assistant", text = currentText)
                                                _uiState.value = EvrenState.Success(uiMessages.toList(), isChatLoading = false)
                                            }
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                // Ignore json parse errors for metadata lines
                            }
                        }
                        line = reader.readLine()
                    }
                } catch (streamEx: Exception) {
                    // Fallback to non-streaming if stream connection fails
                    if (!streamedSuccessfully) {
                        val nonStreamRequest = request.copy(stream = false)
                        val nonStreamResp = OpenRouterClient.getApi(baseUrl).generateContent("Bearer $apiKey", nonStreamRequest)
                        val content = nonStreamResp.choices?.firstOrNull()?.message?.content ?: "Cevap üretilemedi."
                        accumulatedText.append(content)
                    }
                }

                val finalText = accumulatedText.toString().ifBlank { "Analiz yanıtı üretilemedi." }
                apiMessages.add(Message(role = "assistant", content = listOf(Content(type = "text", text = finalText))))
                
                withContext(Dispatchers.Main) {
                    if (uiMessages.isNotEmpty() && uiMessages.last().role == "assistant") {
                        uiMessages[uiMessages.lastIndex] = ChatMessage(role = "assistant", text = finalText)
                    } else {
                        uiMessages.add(ChatMessage(role = "assistant", text = finalText))
                    }
                    _uiState.value = EvrenState.Success(uiMessages.toList(), isChatLoading = false)
                }

                // Auto-save scan into persistent history
                try {
                    val firstCleanLine = finalText.lines()
                        .firstOrNull { it.isNotBlank() }
                        ?.replace("#", "")
                        ?.replace("*", "")
                        ?.trim() ?: text.take(30)
                    val title = if (firstCleanLine.length > 35) firstCleanLine.take(32) + "..." else firstCleanLine
                    val savedUris = uiMessages.flatMap { it.imageUris }.distinct()
                    ScanHistoryManager.addScan(
                        context = context,
                        item = ScanHistoryItem(
                            title = title,
                            previewText = finalText.take(120).replace("\n", " "),
                            fullResponse = finalText,
                            imageUris = savedUris,
                            location = activeLocation
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } catch (e: retrofit2.HttpException) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    val errorBody = try { e.response()?.errorBody()?.string() } catch(ex: Exception) { null }
                    _uiState.value = EvrenState.Error("API Hatası (${e.code()}): ${errorBody ?: e.message()}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _uiState.value = EvrenState.Error(e.message ?: "Bilinmeyen bir hata oluştu")
                }
            }
        }
    }

    fun clearState() {
        stagedImageUris.clear()
        uiMessages.clear()
        apiMessages.clear()
        _uiState.value = EvrenState.Idle
    }
}

@Composable
fun EvrenScreen(
    modifier: Modifier = Modifier, 
    viewModel: EvrenViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var tempImageUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var locationName by remember { mutableStateOf<String?>(null) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var previewImageUri by remember { mutableStateOf<String?>(null) }
    
    val locationPermissionState = rememberPermissionState(
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    var showPermissionExplanation by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5),
        onResult = { uris ->
            if (uris.isNotEmpty()) {
                viewModel.startChatWithImages(uris, locationName)
            }
        }
    )

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success && tempImageUri != null) {
                viewModel.startChatWithImages(listOf(tempImageUri!!), locationName)
            }
        }
    )

    // Permission Explanation Dialog
    if (showPermissionExplanation) {
        AlertDialog(
            onDismissRequest = { showPermissionExplanation = false },
            title = { Text("Konum İzni Gerekli", color = NeonCyan) },
            text = { Text("Bulunduğunuz bölgeyi doğru tespit edebilmemiz ve bölgeye özel analiz yapabilmemiz için konum iznine ihtiyaç duyulmaktadır. Lütfen gelen sistem penceresinde 'İzin ver' veya 'Allow' seçeneğine dokununuz.", color = MaterialTheme.colorScheme.onSurface) },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionExplanation = false
                    locationPermissionState.launchPermissionRequest()
                }) {
                    Text("Anladım", color = NeonCyan)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = NeonCyan,
            textContentColor = MaterialTheme.colorScheme.onSurface
        )
    }

    // Image Full Preview Dialog
    if (previewImageUri != null) {
        Dialog(onDismissRequest = { previewImageUri = null }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xF20B0F19),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth().wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                            .clip(RoundedCornerShape(14.dp))
                    ) {
                        AsyncImage(
                            model = previewImageUri,
                            contentDescription = "Görsel Önizleme",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { previewImageUri = null },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("KAPAT", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // API Key & Provider Settings Dialog
    if (showApiKeyDialog) {
        ApiKeySettingsDialog(
            context = context,
            onDismiss = { showApiKeyDialog = false },
            onSaved = { showApiKeyDialog = false }
        )
    }

    var showHistoryDialog by remember { mutableStateOf(false) }
    var historyItems by remember { mutableStateOf(ScanHistoryManager.getHistory(context)) }
    var isHistorySearchActive by remember { mutableStateOf(false) }
    var historySearchQuery by remember { mutableStateOf("") }

    LaunchedEffect(uiState) {
        historyItems = ScanHistoryManager.getHistory(context)
    }

    if (showHistoryDialog) {
        ScanHistoryDialog(
            historyItems = historyItems,
            onDismiss = { showHistoryDialog = false },
            onSelectItem = { item ->
                viewModel.loadScanHistoryItem(item)
                showHistoryDialog = false
            },
            onDeleteItem = { id ->
                ScanHistoryManager.deleteScan(context, id)
                historyItems = ScanHistoryManager.getHistory(context)
            },
            onClearAll = {
                ScanHistoryManager.clearHistory(context)
                historyItems = emptyList()
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // High-tech futuristic quantum grid background
        Image(
            painter = painterResource(id = R.drawable.future_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Subtle dark translucent futuristic gradient vignette
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xCC06090F),
                            Color(0x9906090F),
                            Color(0xF006090F)
                        )
                    )
                )
        )
        
        val isSuccessState = uiState is EvrenState.Success

        if (isSuccessState) {
            val successState = uiState as EvrenState.Success
            var topQuickQuery by remember { mutableStateOf("") }
            val listState = rememberLazyListState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                // Navigation Header for Active Analysis Report
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = { viewModel.resetToHome() },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFF1E2838),
                            contentColor = NeonCyan
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("YENİ TARAMA", fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { showHistoryDialog = true },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E2838))
                        ) {
                            BadgedBox(badge = {
                                if (historyItems.isNotEmpty()) {
                                    Badge(containerColor = NeonCyan, contentColor = Color.Black) {
                                        Text(historyItems.size.toString(), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }) {
                                Icon(Icons.Default.History, contentDescription = "Geçmiş", tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        val isConfigured = EvrenPreferences.isConfigured(context)
                        IconButton(
                            onClick = { showApiKeyDialog = true },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isConfigured) Color(0x3300E5FF) else Color(0x33FFA000))
                                .border(1.dp, if (isConfigured) NeonCyan.copy(alpha = 0.5f) else Color(0xFFFFA000), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = "API Yapılandırması",
                                tint = if (isConfigured) NeonCyan else Color(0xFFFFA000),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // TOP QUICK CHAT & QUESTION BAR (En aşağı inmek zorunda kalmadan yukarıdan anında erişim!)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xF20F172A),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.45f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            BasicTextField(
                                value = topQuickQuery,
                                onValueChange = { topQuickQuery = it },
                                modifier = Modifier.weight(1f),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(onSend = {
                                    if (topQuickQuery.isNotBlank() && !successState.isChatLoading) {
                                        viewModel.sendChatMessage(text = topQuickQuery, context = context)
                                        topQuickQuery = ""
                                    }
                                }),
                                decorationBox = { innerTextField ->
                                    if (topQuickQuery.isEmpty()) {
                                        Text("Yukarıdan hemen soru sor...", color = TextMuted, fontSize = 13.sp)
                                    }
                                    innerTextField()
                                }
                            )
                            if (topQuickQuery.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        if (!successState.isChatLoading) {
                                            viewModel.sendChatMessage(text = topQuickQuery, context = context)
                                            topQuickQuery = ""
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Send, contentDescription = "Gönder", tint = NeonCyan, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        // Quick suggestion chips right at top
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(listOf(
                                "❓ Bu Nedir?",
                                "⚡ Kısa Özeti Ver",
                                "🌿 Faydalı mı / Zararlı mı?",
                                "🛠️ Ne İşe Yarar?",
                                "📝 Metinleri Oku"
                            )) { chipText ->
                                Surface(
                                    modifier = Modifier.clickable {
                                        if (!successState.isChatLoading) {
                                            viewModel.sendChatMessage(text = chipText, context = context)
                                        }
                                    },
                                    color = Color(0xFF1E293B),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(0.7.dp, NeonCyan.copy(alpha = 0.35f))
                                ) {
                                    Text(
                                        text = chipText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NeonCyan,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Scrollable Chat Messages (LazyColumn fills available height)
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(successState.messages) { msg ->
                        val isUser = msg.role == "user"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                        ) {
                            if (isUser) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(0.9f),
                                    shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp),
                                    color = Color(0xF0131E2F),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.45f))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(
                                            text = "SİZ",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = NeonCyan,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        
                                        if (msg.imageUris.isNotEmpty()) {
                                            if (msg.imageUris.size == 1) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(180.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .border(1.dp, NeonCyan.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                                        .clickable { previewImageUri = msg.imageUris.first() }
                                                ) {
                                                    AsyncImage(
                                                        model = msg.imageUris.first(),
                                                        contentDescription = "Ekli Fotoğraf",
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))
                                            } else {
                                                LazyRow(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    modifier = Modifier.padding(bottom = 8.dp)
                                                ) {
                                                    items(msg.imageUris) { uriString ->
                                                        Box(
                                                            modifier = Modifier
                                                                .size(76.dp)
                                                                .clip(RoundedCornerShape(10.dp))
                                                                .border(1.dp, NeonCyan.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                                                                .clickable { previewImageUri = uriString }
                                                        ) {
                                                            AsyncImage(
                                                                model = uriString,
                                                                contentDescription = "Ekli Fotoğraf",
                                                                modifier = Modifier.fillMaxSize(),
                                                                contentScale = ContentScale.Crop
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        Text(
                                            text = msg.text,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White,
                                            lineHeight = 22.sp
                                        )
                                    }
                                }
                            } else {
                                if (msg.text.isNotEmpty()) {
                                    ProfessionalAnalysisCard(
                                        text = msg.text,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    if (successState.isChatLoading) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = NeonCyan,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Evren yanıtlıyor...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonCyan,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                // PINNED BOTTOM CHAT INPUT (ALWAYS VISIBLE, NEVER SCROLLED AWAY!)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xF20B101B),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E2838))
                ) {
                    Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                        ChatInputField(
                            onSendMessage = { text, attachedUris ->
                                val base64List = attachedUris.mapNotNull { it.toBase64(context) }
                                viewModel.sendChatMessage(
                                    text = text,
                                    attachedBase64Images = base64List,
                                    attachedUris = attachedUris.map { it.toString() },
                                    context = context
                                )
                            },
                            isLoading = successState.isChatLoading,
                            onLaunchCamera = {
                                val uri = context.createImageFileUri()
                                tempImageUri = uri
                                takePictureLauncher.launch(uri)
                            },
                            onLaunchGallery = {
                                photoPickerLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            }
                        )
                    }
                }
            }
        } else {
            // HOME / SCANNER SCREEN (STABLE, FIXED CONTROLS, ONLY HISTORY SCROLLS)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(14.dp))

                // 1. Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(NeonCyan.copy(alpha = 0.5f), ElectricPurple.copy(alpha = 0.2f), Color.Transparent)
                                    )
                                )
                                .border(1.dp, NeonCyan.copy(alpha = 0.6f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Evren İkonu",
                                modifier = Modifier.size(22.dp),
                                tint = NeonCyan
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "EVREN",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                letterSpacing = 2.5.sp
                            )
                            val activeModelName = if (EvrenPreferences.getModel(context) == "qwen3-vl-30b") {
                                "QWEN 3 VL 30B • AKTİF"
                            } else {
                                "${EvrenPreferences.getModel(context).uppercase()} • AKTİF"
                            }
                            Text(
                                text = activeModelName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = NeonCyan,
                                letterSpacing = 1.sp,
                                fontSize = 9.sp
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { showHistoryDialog = true },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF131D2E))
                                .border(1.dp, NeonCyan.copy(alpha = 0.3f), CircleShape)
                        ) {
                            BadgedBox(badge = {
                                if (historyItems.isNotEmpty()) {
                                    Badge(containerColor = NeonCyan, contentColor = Color.Black) {
                                        Text(historyItems.size.toString(), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "Geçmiş Taramalar",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        val isConfigured = EvrenPreferences.isConfigured(context)
                        IconButton(
                            onClick = { showApiKeyDialog = true },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isConfigured) Color(0x3300E5FF) else Color(0x33FFA000)
                                )
                                .border(
                                    1.dp,
                                    if (isConfigured) NeonCyan.copy(alpha = 0.5f) else Color(0xFFFFA000),
                                    CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = "API Yapılandırması",
                                tint = if (isConfigured) NeonCyan else Color(0xFFFFA000),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Main Scanner Controls (Taller downwards as requested)
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(106.dp)
                        .clickable {
                            val uri = context.createImageFileUri()
                            tempImageUri = uri
                            takePictureLauncher.launch(uri)
                        },
                    colors = CardDefaults.outlinedCardColors(containerColor = GlassSurface),
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        Brush.horizontalGradient(listOf(NeonCyan, ElectricPurple))
                    ),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(NeonCyan.copy(alpha = 0.15f))
                                .border(1.2.dp, NeonCyan.copy(alpha = 0.7f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "Kamera",
                                modifier = Modifier.size(26.dp),
                                tint = NeonCyan
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(verticalArrangement = Arrangement.Center) {
                            Text(
                                "KAMERAYI AÇ",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                letterSpacing = 2.sp,
                                fontSize = 17.sp
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                "Anlık vizör taraması başlat ve doğrudan analiz et",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Gallery Button (Taller downwards)
                    OutlinedCard(
                        modifier = Modifier
                            .weight(1f)
                            .height(82.dp)
                            .clickable {
                                photoPickerLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                        colors = CardDefaults.outlinedCardColors(containerColor = GlassSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(NeonCyan.copy(alpha = 0.12f))
                                    .border(1.dp, NeonCyan.copy(alpha = 0.4f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = "Galeri", modifier = Modifier.size(20.dp), tint = NeonCyan)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(verticalArrangement = Arrangement.Center) {
                                Text("GALERİDEN SEÇ", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 0.5.sp, fontSize = 11.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Fotoğraf yükle", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 10.sp)
                            }
                        }
                    }

                    // Region Scan Button (Taller downwards)
                    OutlinedCard(
                        modifier = Modifier
                            .weight(1f)
                            .height(82.dp)
                            .clickable {
                                if (locationPermissionState.status.isGranted) {
                                    coroutineScope.launch {
                                        val loc = getCurrentLocationName(context)
                                        if (loc != null) {
                                            locationName = loc
                                            viewModel.setRegionContext(loc)
                                        } else {
                                            showPermissionExplanation = true
                                        }
                                    }
                                } else {
                                    showPermissionExplanation = true
                                }
                            },
                        colors = CardDefaults.outlinedCardColors(containerColor = GlassSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ElectricPurple.copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(ElectricPurple.copy(alpha = 0.12f))
                                    .border(1.dp, ElectricPurple.copy(alpha = 0.4f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Explore, contentDescription = "Bölge Taraması", modifier = Modifier.size(20.dp), tint = ElectricPurple)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(verticalArrangement = Arrangement.Center) {
                                Text("BÖLGE TARAMASI", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 0.5.sp, fontSize = 11.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Konum algıla", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 10.sp)
                            }
                        }
                    }
                }

                // Active Location Badge
                if (locationName != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = ElectricPurple.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ElectricPurple.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = ElectricPurple, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "AKTİF BÖLGE: $locationName",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonCyan,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Global Loading Indicator on Home
                if (uiState is EvrenState.Loading) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xD90D1524),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = NeonCyan,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "EVREN ANALİZ EDİYOR...",
                                style = MaterialTheme.typography.labelMedium,
                                color = NeonCyan,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }

                // Error banner on Home
                if (uiState is EvrenState.Error) {
                    val errorState = uiState as EvrenState.Error
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0x33FF5252),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF5252)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = errorState.message,
                                color = TextLight,
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (errorState.isApiKeyError) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { showApiKeyDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("API ANAHTARINI GİRİN", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 4. "GEÇMİŞ TARAMALAR" Section Header with Search Button & Clear/All
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "GEÇMİŞ TARAMALAR",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                        if (historyItems.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = Color(0xFF1E293B),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = historyItems.size.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonCyan,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Search Button for past scans
                        IconButton(
                            onClick = {
                                isHistorySearchActive = !isHistorySearchActive
                                if (!isHistorySearchActive) historySearchQuery = ""
                            },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (isHistorySearchActive) NeonCyan.copy(alpha = 0.2f) else Color(0xFF141F30))
                                .border(1.dp, if (isHistorySearchActive) NeonCyan else Color(0xFF24364F), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Taramalarda Ara",
                                tint = if (isHistorySearchActive) NeonCyan else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        if (historyItems.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            TextButton(
                                onClick = { showHistoryDialog = true },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Tümünü Gör", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                // Search Bar for Past Scans
                AnimatedVisibility(visible = isHistorySearchActive) {
                    OutlinedTextField(
                        value = historySearchQuery,
                        onValueChange = { historySearchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        placeholder = {
                            Text("Taramalarda ara...", color = TextMuted, fontSize = 13.sp)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (historySearchQuery.isNotEmpty()) {
                                IconButton(onClick = { historySearchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Temizle", tint = TextMuted, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = Color(0xFF24364F),
                            focusedContainerColor = Color(0xCC0D1522),
                            unfocusedContainerColor = Color(0x990D1522),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                val filteredHistoryItems = remember(historyItems, historySearchQuery) {
                    if (historySearchQuery.isBlank()) {
                        historyItems
                    } else {
                        historyItems.filter {
                            it.title.contains(historySearchQuery, ignoreCase = true) ||
                            it.previewText.contains(historySearchQuery, ignoreCase = true) ||
                            (it.location?.contains(historySearchQuery, ignoreCase = true) == true)
                        }
                    }
                }

                // 5. INDEPENDENT SCROLLABLE CONTAINER: ONLY HISTORY ITEMS SCROLL!
                // "geçmiş taramaları ise aşağıya doğru kaydırdığımda sadece geçmiş taramalar kaysın bütün arayüz aşağıya kaymasın."
                if (filteredHistoryItems.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 90.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredHistoryItems, key = { it.id }) { item ->
                            ScanHistoryCardItem(
                                item = item,
                                onClick = { viewModel.loadScanHistoryItem(item) },
                                onDelete = {
                                    ScanHistoryManager.deleteScan(context, item.id)
                                    historyItems = ScanHistoryManager.getHistory(context)
                                }
                            )
                        }
                    }
                } else if (historyItems.isNotEmpty() && historySearchQuery.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(top = 20.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Text(
                            text = "\"$historySearchQuery\" ile eşleşen tarama bulunamadı.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xD90D1524),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E2838))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.History,
                                    contentDescription = null,
                                    tint = TextMuted.copy(alpha = 0.6f),
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Henüz geçmiş tarama bulunmuyor",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = "Kamera veya galeriden görsel seçerek ilk analizinizi başlatabilirsiniz.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatInputField(
    onSendMessage: (String, List<Uri>) -> Unit,
    isLoading: Boolean,
    onLaunchCamera: () -> Unit,
    onLaunchGallery: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    val attachedUris = remember { mutableStateListOf<Uri>() }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Attached thumbnails inside chat input
        if (attachedUris.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                items(attachedUris) { uri ->
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, NeonCyan, RoundedCornerShape(8.dp))
                    ) {
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { attachedUris.remove(uri) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(18.dp)
                                .background(Color.Black.copy(alpha = 0.8f), CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Kaldır", tint = Color.White, modifier = Modifier.size(10.dp))
                        }
                    }
                }
            }
        }

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Evren'e soru sorun veya detay isteyin...", color = TextMuted) },
            shape = RoundedCornerShape(26.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xF20F172A),
                unfocusedContainerColor = Color(0xCC0F172A),
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = NeonCyan.copy(alpha = 0.35f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            leadingIcon = {
                Row(modifier = Modifier.padding(start = 8.dp)) {
                    IconButton(onClick = onLaunchCamera, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Fotoğraf Çek", tint = NeonCyan, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onLaunchGallery, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = "Fotoğraf Seç", tint = NeonCyan, modifier = Modifier.size(20.dp))
                    }
                }
            },
            trailingIcon = {
                IconButton(
                    onClick = {
                        if ((text.isNotBlank() || attachedUris.isNotEmpty()) && !isLoading) {
                            onSendMessage(text, attachedUris.toList())
                            text = ""
                            attachedUris.clear()
                        }
                    }
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Gönder",
                        tint = if (text.isNotBlank() || attachedUris.isNotEmpty()) NeonCyan else TextMuted
                    )
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = {
                if ((text.isNotBlank() || attachedUris.isNotEmpty()) && !isLoading) {
                    onSendMessage(text, attachedUris.toList())
                    text = ""
                    attachedUris.clear()
                }
            })
        )
    }
}

@Composable
fun ApiKeySettingsDialog(
    context: android.content.Context,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    var apiUrl by remember { mutableStateOf(EvrenPreferences.getBaseUrl(context)) }
    var apiKey by remember { mutableStateOf(EvrenPreferences.getApiKey(context)) }
    var selectedModel by remember { mutableStateOf(EvrenPreferences.getModel(context)) }
    var passwordVisible by remember { mutableStateOf(false) }
    var showSuccessMessage by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xF80B0F19),
            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(NeonCyan.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Key, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "EVREN API & MODEL AYARLARI",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Kapat", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Provider Status Card
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0x2200E5FF),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Evren LLM API Servisi (Entegre Edildi)",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonCyan,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // API URL field
                OutlinedTextField(
                    value = apiUrl,
                    onValueChange = { apiUrl = it },
                    label = { Text("API Servis Adresi (Base URL)") },
                    placeholder = { Text("https://evren-llmapi.ssyz.org.tr/v1") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0x660F172A),
                        unfocusedContainerColor = Color(0x330F172A),
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // API Key input field
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Anahtarı") },
                    placeholder = { Text("evren_llm_...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Gizle" else "Göster",
                                tint = NeonCyan
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0x660F172A),
                        unfocusedContainerColor = Color(0x330F172A),
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Reset to default button
                TextButton(
                    onClick = {
                        apiUrl = EvrenPreferences.DEFAULT_BASE_URL
                        apiKey = EvrenPreferences.DEFAULT_API_KEY
                        selectedModel = EvrenPreferences.DEFAULT_MODEL
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Varsayılanlara Sıfırla", color = NeonCyan, fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Model Selection
                Text(
                    text = "Kullanılacak Yapay Zeka Modeli:",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                EvrenPreferences.AVAILABLE_MODELS.forEach { (modelId, modelName) ->
                    val isSelected = selectedModel == modelId
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { selectedModel = modelId },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) Color(0x3300E5FF) else Color(0x221E293B),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) NeonCyan else Color.Transparent
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = modelName,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSelected) NeonCyan else Color.White,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                if (showSuccessMessage) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Ayarlar başarıyla kaydedildi!",
                        color = Color(0xFF10B981),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("İPTAL", color = TextMuted)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            EvrenPreferences.setBaseUrl(context, apiUrl)
                            EvrenPreferences.setApiKey(context, apiKey)
                            EvrenPreferences.setModel(context, selectedModel)
                            showSuccessMessage = true
                            onSaved()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("KAYDET & ETKİNLEŞTİR", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
