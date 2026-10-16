import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

start_idx = content.find("@Composable\nfun BotanyScreen(")

content = content[:start_idx]

new_ui = '''@Composable
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

    when (val state = uiState) {
        is BotanyState.Idle -> {
            IdleScreen(
                modifier = modifier,
                selectedImageUris = selectedImageUris,
                locationName = locationName,
                onLocationClick = {
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
                onGalleryClick = {
                    photoPickerLauncher.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                },
                onCameraClick = {
                    val uri = context.createImageFileUri()
                    tempImageUri = uri
                    takePictureLauncher.launch(uri)
                },
                onAnalyzeClick = {
                    val base64List = selectedImageUris.mapNotNull { it.toBase64(context) }
                    if (base64List.isNotEmpty()) {
                        viewModel.analyzePlants(base64List, locationName)
                    }
                },
                onRemoveImage = { selectedImageUris.remove(it) }
            )
        }
        is BotanyState.Chat -> {
            ChatScreen(
                modifier = modifier,
                state = state,
                onSendMessage = { viewModel.sendMessage(it) },
                onDismissError = { viewModel.dismissError() }
            )
        }
        else -> {}
    }
}

@Composable
fun IdleScreen(
    modifier: Modifier,
    selectedImageUris: List<Uri>,
    locationName: String?,
    onLocationClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onCameraClick: () -> Unit,
    onAnalyzeClick: () -> Unit,
    onRemoveImage: (Uri) -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
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
                    .clickable(onClick = onCameraClick),
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
                        .clickable(onClick = onGalleryClick),
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
                        .clickable(onClick = onLocationClick),
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
                        Text("ÇEVREMDEKİLER", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (locationName != null) {
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
                                onClick = { onRemoveImage(uri) },
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
                    onClick = onAnalyzeClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.background),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text("ANALİZİ BAŞLAT", fontSize = 16.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    modifier: Modifier,
    state: BotanyState.Chat,
    onSendMessage: (String) -> Unit,
    onDismissError: () -> Unit
) {
    var textInput by remember { mutableStateOf("") }
    
    // Auto scroll to bottom
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    LaunchedEffect(state.messages.size, state.isTyping) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Chat Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "BİTKİ ANALİZİ - BAĞLANTI AKTİF",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    letterSpacing = 1.sp
                )
            }
        }

        // Messages List
        androidx.compose.foundation.lazy.LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(state.messages) { message ->
                ChatMessageItem(message = message)
            }
            if (state.isTyping) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.secondary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "VERİLER İŞLENİYOR...",
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
            
            if (state.error != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "SİSTEM HATASI",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.error,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = onDismissError) {
                                Text("TAMAM", color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                }
            }
        }

        // Input field
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Bir şeyler sor...") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.secondary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(8.dp),
                    maxLines = 3,
                    enabled = !state.isTyping
                )
                Spacer(modifier = Modifier.width(12.dp))
                IconButton(
                    onClick = {
                        if (textInput.isNotBlank()) {
                            onSendMessage(textInput)
                            textInput = ""
                        }
                    },
                    enabled = !state.isTyping && textInput.isNotBlank(),
                    modifier = Modifier
                        .background(
                            if (!state.isTyping && textInput.isNotBlank()) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(8.dp)
                        )
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Gönder",
                        tint = if (!state.isTyping && textInput.isNotBlank()) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(message: ChatMessage) {
    val isUser = message.isUser
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bgColor = if (isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
    val borderColor = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
    val textColor = if (isUser) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.secondary

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(if (isUser) 0.85f else 0.95f),
            colors = CardDefaults.outlinedCardColors(containerColor = bgColor),
            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
            shape = RoundedCornerShape(
                topStart = 8.dp,
                topEnd = 8.dp,
                bottomStart = if (isUser) 8.dp else 0.dp,
                bottomEnd = if (isUser) 0.dp else 8.dp
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (message.imageBase64s.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        items(message.imageBase64s) { base64 ->
                            val imageBytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
                            val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                            if (bitmap != null) {
                                androidx.compose.foundation.Image(
                                    bitmap = androidx.compose.ui.graphics.asImageBitmap(bitmap),
                                    contentDescription = "Analiz Edilen Resim",
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(1.dp, borderColor, RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
                
                Text(
                    text = if (isUser) ">> KULLANICI GİRDİSİ" else ">> SİSTEM ÇIKTISI",
                    style = MaterialTheme.typography.labelSmall,
                    color = borderColor,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = textColor,
                    lineHeight = 26.sp
                )
            }
        }
    }
}
'''

content += new_ui

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
