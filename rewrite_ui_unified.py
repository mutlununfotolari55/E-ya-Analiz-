import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

start_idx = content.find("@Composable\nfun BotanyScreen(")

content = content[:start_idx]

new_ui = '''@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BotanyScreen(
    modifier: Modifier = Modifier, 
    viewModel: BotanyViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val state = uiState as? BotanyState.Chat ?: return
    
    val selectedImageUris = remember { mutableStateListOf<Uri>() }
    var tempImageUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
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

    if (showPermissionExplanation) {
        AlertDialog(
            onDismissRequest = { showPermissionExplanation = false },
            title = { Text("Konum İzni Gerekli", color = MaterialTheme.colorScheme.secondary) },
            text = { Text("Çevrenizdeki bitkileri doğru bir şekilde analiz edebilmemiz için konum bilginize ihtiyacımız var.", color = MaterialTheme.colorScheme.onSurface) },
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

    var textInput by remember { mutableStateOf("") }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    
    LaunchedEffect(state.messages.size, state.isTyping) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = R.drawable.bg_grass_blurred),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)))
        
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "BİTKİ ANALİZİ",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Messages
            androidx.compose.foundation.lazy.LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (state.messages.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 64.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Yard, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Sisteme hoş geldiniz.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                            Text("Bir fotoğraf yükleyerek veya soru sorarak başlayın.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                        }
                    }
                }
                
                items(state.messages) { message ->
                    ChatMessageItem(message = message)
                }
                
                if (state.isTyping) {
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.secondary, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("VERİLER İŞLENİYOR...", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                
                if (state.error != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("SİSTEM HATASI", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(state.error, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(onClick = { viewModel.dismissError() }) {
                                    Text("TAMAM", color = MaterialTheme.colorScheme.onErrorContainer)
                                }
                            }
                        }
                    }
                }
            }

            // Input Area
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                shadowElevation = 16.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                    if (selectedImageUris.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp, start = 8.dp, end = 8.dp)
                        ) {
                            items(selectedImageUris) { uri ->
                                Box(modifier = Modifier.size(64.dp).border(1.dp, MaterialTheme.colorScheme.secondary, RoundedCornerShape(8.dp))) {
                                    AsyncImage(
                                        model = uri,
                                        contentDescription = "Seçilen Bitki",
                                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    IconButton(
                                        onClick = { selectedImageUris.remove(uri) },
                                        modifier = Modifier.align(Alignment.TopEnd).padding(2.dp).size(20.dp).background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Kaldır", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                val uri = context.createImageFileUri()
                                tempImageUri = uri
                                takePictureLauncher.launch(uri)
                            },
                            enabled = !state.isTyping
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "Kamera", tint = MaterialTheme.colorScheme.primary)
                        }
                        
                        IconButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                            enabled = !state.isTyping
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = "Galeri", tint = MaterialTheme.colorScheme.primary)
                        }
                        
                        IconButton(
                            onClick = {
                                if (locationPermissionState.status.isGranted) {
                                    coroutineScope.launch {
                                        val loc = getCurrentLocationName(context)
                                        if (loc != null) {
                                            viewModel.discoverLocalPlants(loc)
                                        } else {
                                            showPermissionExplanation = true
                                        }
                                    }
                                } else {
                                    showPermissionExplanation = true
                                }
                            },
                            enabled = !state.isTyping
                        ) {
                            Icon(Icons.Default.Place, contentDescription = "Konum", tint = MaterialTheme.colorScheme.primary)
                        }

                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                            placeholder = { Text("Sor veya bitki gönder...", maxLines = 1) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            ),
                            maxLines = 3,
                            enabled = !state.isTyping
                        )
                        
                        val canSend = !state.isTyping && (textInput.isNotBlank() || selectedImageUris.isNotEmpty())
                        IconButton(
                            onClick = {
                                if (canSend) {
                                    val base64List = selectedImageUris.mapNotNull { it.toBase64(context) }
                                    viewModel.sendMessage(textInput, base64List)
                                    textInput = ""
                                    selectedImageUris.clear()
                                }
                            },
                            enabled = canSend,
                            modifier = Modifier.padding(end = 4.dp).background(if (canSend) MaterialTheme.colorScheme.secondary else Color.Transparent, RoundedCornerShape(8.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Gönder",
                                tint = if (canSend) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
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
                                    bitmap = bitmap.asImageBitmap(),
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
                if (message.text.isNotBlank()) {
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
}
'''

content += new_ui

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
