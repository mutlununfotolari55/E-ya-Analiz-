with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

# Add imports
imports = """import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.blur
import com.example.R
"""
if "import com.example.R" not in content:
    content = content.replace("import androidx.compose.ui.graphics.Color", "import androidx.compose.ui.graphics.Color\n" + imports)

# Update IdleScreen
old_idle_box = """    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {"""

new_idle_box = """    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.bg_grass_blurred),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)))
"""
content = content.replace(old_idle_box, new_idle_box)

# Update ChatScreen
old_chat_column = """    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {"""

new_chat_column = """    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.bg_grass_blurred),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)))
        
        Column(
            modifier = Modifier.fillMaxSize()
        ) {"""

content = content.replace(old_chat_column, new_chat_column)

# Need to close the extra Box in ChatScreen
# Find the end of ChatScreen. It ends with:
old_chat_end = """        }
    }
}

@Composable"""

new_chat_end = """        }
    }
    }
}

@Composable"""

# Carefully replace the end of ChatScreen only once
# We can just find the end by looking for ChatMessageItem
end_idx = content.find("@Composable\nfun ChatMessageItem")
# go back and add an extra brace
content = content[:end_idx-2] + "    }\n" + content[end_idx-2:]

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
