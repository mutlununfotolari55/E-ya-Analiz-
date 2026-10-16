import re

content = """@file:OptIn(ExperimentalPermissionsApi::class)
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.ui.theme.MyApplicationTheme
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
                    BotanyScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

fun android.content.Context.createImageFileUri(): Uri {
    val imageFile = File(cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
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

sealed class BotanyState {
    object Idle : BotanyState()
    object Loading : BotanyState()
    data class Success(val result: String) : BotanyState()
    data class Error(val message: String) : BotanyState()
}

class BotanyViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<BotanyState>(BotanyState.Idle)
    val uiState: StateFlow<BotanyState> = _uiState.asStateFlow()

    fun analyzePlants(base64Images: List<String>, locationContext: String? = null) {
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
                    "Lütfen bu bitkileri analiz et ve teşhisini yap. Bulunduğum il/ilçe konumu: $locationContext. Konuma göre bitki teşhisini ve bu bölgedeki endemik/yaygın durumunu dikkate al. Bu bitkilerin belirgin özelliklerini, başka hangi bölgelerde yetiştiğini ve ne işe yaradıklarını (faydalarını) detaylıca Türkçe olarak açıkla."
                } else {
                    "Lütfen bu bitkileri analiz et ve teşhisini yap. Belirgin özelliklerini, nerelerde yetiştiğini ve ne işe yaradıklarını detaylıca Türkçe olarak açıkla."
                }

                val contentList = mutableListOf<Content>()
                contentList.add(Content(type = "text", text = promptText))
                for (base64 in base64Images) {
                    contentList.add(Content(type = "image_url", image_url = ImageUrl(url = "data:image/jpeg;base64,$base64")))
                }

                val request = OpenRouterRequest(
                    model = "google/gemini-2.5-flash",
                    messages = listOf(Message(role = "user", content = contentList)),
                    max_tokens = 2000
                )

                val response = OpenRouterClient.api.generateContent("Bearer $apiKey", request)
                val textResponse = response.choices?.firstOrNull()?.message?.content ?: "Sonuç alınamadı."
                withContext(Dispatchers.Main) {
                    _uiState.value = BotanyState.Success(textResponse)
                }
            } catch (e: retrofit2.HttpException) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    when (e.code()) {
                        401 -> _uiState.value = BotanyState.Error("API Anahtarı geçersiz veya yetkisiz (HTTP 401). Lütfen Secrets paneline doğru anahtarı girdiğinizden emin olun.")
                        402 -> _uiState.value = BotanyState.Error("OpenRouter bakiyeniz yetersiz (HTTP 402).")
                        404 -> _uiState.value = BotanyState.Error("OpenRouter modeli bulunamadı (HTTP 404).")
                        else -> _uiState.value = BotanyState.Error("HTTP Hatası: ${e.code()} - ${e.message()}")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _uiState.value = BotanyState.Error(e.message ?: "Bilinmeyen bir hata oluştu")
                }
            }
        }
    }

    fun discoverLocalPlants(locationContext: String) {
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

                val promptText = "Bulunduğum il ve ilçe konumu: $locationContext. Sadece bulunduğum ülkeye göre değil, doğrudan bu il ve ilçe sınırları içerisinde yetişen bitkilere odaklan. Bu bölgeye özgü (endemik) olan veya yaygın olarak bulunan önemli bitkileri listele. Bu bitkilerin başka nerelerde yetiştiğini, belirgin özelliklerini ve ne işe yaradıklarını (faydalarını) liste biçiminde detaylıca Türkçe olarak anlat."
                
                val contentList = listOf(Content(type = "text", text = promptText))

                val request = OpenRouterRequest(
                    model = "google/gemini-2.5-flash",
                    messages = listOf(Message(role = "user", content = contentList)),
                    max_tokens = 2000
                )

                val response = OpenRouterClient.api.generateContent("Bearer $apiKey", request)
                val textResponse = response.choices?.firstOrNull()?.message?.content ?: "Sonuç alınamadı."
                withContext(Dispatchers.Main) {
                    _uiState.value = BotanyState.Success(textResponse)
                }
            } catch (e: retrofit2.HttpException) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    when (e.code()) {
                        401 -> _uiState.value = BotanyState.Error("API Anahtarı geçersiz veya yetkisiz (HTTP 401). Lütfen Secrets paneline doğru anahtarı girdiğinizden emin olun.")
                        402 -> _uiState.value = BotanyState.Error("OpenRouter bakiyeniz yetersiz (HTTP 402).")
                        404 -> _uiState.value = BotanyState.Error("OpenRouter modeli bulunamadı (HTTP 404).")
                        else -> _uiState.value = BotanyState.Error("HTTP Hatası: ${e.code()} - ${e.message()}")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _uiState.value = BotanyState.Error(e.message ?: "Bilinmeyen bir hata oluştu")
                }
            }
        }
    }
}

@Composable
fun BotanyScreen(
    modifier: Modifier = Modifier, 
    viewModel: BotanyViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedImageUris = remember { mutableStateListOf<Uri>() }
    var tempImageUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var locationName by remember { mutableStateOf<String?>(null) }
    
    val locationPermissionState = rememberPermissionState(
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    var showPermissionExplanation by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5),
        onResult = { uris ->
            selectedImageUris.addAll(uris)
        }
    )

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success && tempImageUri != null) {
                selectedImageUris.add(tempImageUri!!)
            }
        }
    )

    // Permission Explanation Dialog
    if (showPermissionExplanation) {
        AlertDialog(
            onDismissRequest = { showPermissionExplanation = false },
            title = { Text("Konum İzni Gerekli", color = MaterialTheme.colorScheme.secondary) },
            text = { Text("Çevrenizdeki bitkileri doğru bir şekilde analiz edebilmemiz için konum bilginize ihtiyacımız var. Android sisteminin göstereceği izin penceresi cihazınızın dilinde (örneğin İngilizce) olabilir. Lütfen 'Allow' veya 'İzin ver' seçeneğine tıklayınız.", color = MaterialTheme.colorScheme.onSurface) },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionExplanation = false
                    locationPermissionState.launchPermissionRequest()
                }) {
                    Text("Anladım", color = MaterialTheme.colorScheme.secondary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.secondary,
            textContentColor = MaterialTheme.colorScheme.onSurface
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Blur background image here
        Image(
            painter = painterResource(id = R.drawable.grass_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().blur(16.dp),
            contentScale = ContentScale.Crop,
            alpha = 0.5f
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // Header with Sci-Fi look
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Botanik İkonu",
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Bitki Analizi",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    letterSpacing = 2.sp
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Main Camera Scanner Button
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(160.dp)
                    .clickable {
                        val uri = context.createImageFileUri()
                        tempImageUri = uri
                        takePictureLauncher.launch(uri)
                    },
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.secondary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Kamera", modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("KAMERAYI AÇ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.secondary, letterSpacing = 1.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(0.9f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Gallery Button
                OutlinedCard(
                    modifier = Modifier
                        .weight(1f)
                        .height(90.dp)
                        .clickable {
                            photoPickerLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = "Galeri", modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("GALERİ", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp)
                    }
                }

                // Location Button
                OutlinedCard(
                    modifier = Modifier
                        .weight(1f)
                        .height(90.dp)
                        .clickable {
                            if (locationPermissionState.status.isGranted) {
                                coroutineScope.launch {
                                    val loc = getCurrentLocationName(context)
                                    if (loc != null) {
                                        locationName = loc
                                        viewModel.discoverLocalPlants(loc)
                                    } else {
                                        showPermissionExplanation = true
                                    }
                                }
                            } else {
                                showPermissionExplanation = true
                            }
                        },
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Place, contentDescription = "Konum", modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("BÖLGE TARAMASI", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (locationName != null && uiState !is BotanyState.Loading) {
                Text("AKTİF KOORDİNAT: $locationName", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary, letterSpacing = 2.sp)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Selected Images
            if (selectedImageUris.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(selectedImageUris) { uri ->
                        Box(modifier = Modifier
                            .size(110.dp)
                            .border(1.dp, MaterialTheme.colorScheme.secondary, RoundedCornerShape(8.dp))) {
                            AsyncImage(
                                model = uri,
                                contentDescription = "Seçilen Bitki",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { selectedImageUris.remove(uri) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(24.dp)
                                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Kaldır", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            if (selectedImageUris.isNotEmpty()) {
                Button(
                    onClick = {
                        val base64List = selectedImageUris.mapNotNull { it.toBase64(context) }
                        if (base64List.isNotEmpty()) {
                            viewModel.analyzePlants(base64List, locationName)
                        }
                    },
                    enabled = uiState !is BotanyState.Loading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.background),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    if (uiState is BotanyState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.background,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text("ANALİZİ BAŞLAT", fontSize = 16.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            } else if (uiState is BotanyState.Loading) { 
                CircularProgressIndicator(
                    modifier = Modifier.size(56.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    strokeWidth = 4.dp
                )
                Spacer(modifier = Modifier.height(32.dp))
            }

            when (uiState) {
                is BotanyState.Success -> {
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = Color.Black.copy(alpha = 0.6f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "ANALİZ SONUCU",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = (uiState as BotanyState.Success).result,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                                lineHeight = 28.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
                is BotanyState.Error -> {
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "SİSTEM HATASI",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = (uiState as BotanyState.Error).message,
                                color = MaterialTheme.colorScheme.onBackground,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
                else -> {}
            }
        }
    }
}
"""

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
