import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

# Remove Firebase Auth imports
content = re.sub(r'import com\.google\.firebase\.auth\..*\n', '', content)
content = re.sub(r'import com\.google\.firebase\.Firebase.*\n', '', content)

# Replace AppNavigation entirely
old_nav = """@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    // Check if Firebase is initialized and user is logged in
    var isUserLoggedIn by remember { mutableStateOf(false) }
    
    // Quick check if Firebase is configured (to avoid crashes if google-services.json is missing)
    var firebaseInitialized by remember { mutableStateOf(true) }
    var auth: FirebaseAuth? = null
    
    LaunchedEffect(Unit) {
        try {
            auth = Firebase.auth
            isUserLoggedIn = auth?.currentUser != null
        } catch (e: Exception) {
            firebaseInitialized = false
        }
    }

    if (!firebaseInitialized) {
        // Fallback UI if google-services.json is missing
        Box(modifier = modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Text(
                "Firebase başlatılamadı. Lütfen projenize google-services.json dosyasını ekleyin (AI Studio'dan Firebase yapılandırmasını yapın).",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    } else {
        if (isUserLoggedIn) {
            BotanyScreen(
                modifier = modifier,
                onSignOut = {
                    Firebase.auth.signOut()
                    isUserLoggedIn = false
                }
            )
        } else {
            LoginScreen(
                modifier = modifier,
                onLoginSuccess = { isUserLoggedIn = true }
            )
        }
    }
}"""
new_nav = """@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    BotanyScreen(modifier = modifier)
}"""
content = content.replace(old_nav, new_nav)

# Remove LoginScreen
old_login = """@Composable
fun LoginScreen(modifier: Modifier = Modifier, onLoginSuccess: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Yard,
            contentDescription = "Botanik İkonu",
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Bitki Analizi",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 16.dp, bottom = 32.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("E-posta Adresi") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Şifre") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (email.isBlank() || password.isBlank()) {
                    errorMessage = "Lütfen e-posta ve şifrenizi girin."
                    return@Button
                }
                isLoading = true
                errorMessage = null
                Firebase.auth.signInWithEmailAndPassword(email.trim(), password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            onLoginSuccess()
                        } else {
                            // If sign in fails, try to register them for ease of use
                            Firebase.auth.createUserWithEmailAndPassword(email.trim(), password)
                                .addOnCompleteListener { regTask ->
                                    isLoading = false
                                    if (regTask.isSuccessful) {
                                        onLoginSuccess()
                                    } else {
                                        errorMessage = "Giriş yapılamadı: ${task.exception?.localizedMessage}"
                                    }
                                }
                        }
                    }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            contentPadding = PaddingValues(16.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Giriş Yap / Kayıt Ol", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}"""
content = content.replace(old_login, '')

# Change BotanyScreen signature
old_botany_sig = """@Composable
fun BotanyScreen(
    modifier: Modifier = Modifier, 
    viewModel: BotanyViewModel = viewModel(),
    onSignOut: () -> Unit
)"""
new_botany_sig = """@Composable
fun BotanyScreen(
    modifier: Modifier = Modifier, 
    viewModel: BotanyViewModel = viewModel()
)"""
content = content.replace(old_botany_sig, new_botany_sig)

# Change BotanyScreen top bar
old_top_bar = """        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Yard,
                    contentDescription = "Botanik İkonu",
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Bitki Analizi",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            TextButton(onClick = onSignOut) {
                Text("Çıkış Yap", color = MaterialTheme.colorScheme.error)
            }
        }"""
new_top_bar = """        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Yard,
                contentDescription = "Botanik İkonu",
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Bitki Analizi",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }"""
content = content.replace(old_top_bar, new_top_bar)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
