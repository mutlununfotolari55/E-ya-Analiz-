with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

imports = """import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.material.icons.filled.Send
"""

if "asImageBitmap" not in content[:1500]:
    content = content.replace("import androidx.compose.ui.graphics.Color", "import androidx.compose.ui.graphics.Color\n" + imports)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
