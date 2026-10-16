package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PlusJakartaSansFamily

// Executive palette for Professional Analysis View
private val CardBackground = Color(0xFF111722)
private val CardBorderColor = Color(0xFF233147)
private val AccentGold = Color(0xFFE2B857)
private val AccentTeal = Color(0xFF00E5FF)
private val PoisonAlertRed = Color(0xFFFF4D4D)
private val BenefitGreen = Color(0xFF10B981)
private val TextPrimary = Color(0xFFF1F5F9)
private val TextSecondary = Color(0xFF94A3B8)

sealed class ParsedBlock {
    data class MainHeading(val text: String) : ParsedBlock()
    data class SubHeading(val text: String) : ParsedBlock()
    data class StatusAlert(val title: String, val text: String, val isDanger: Boolean) : ParsedBlock()
    data class BulletItem(val text: String) : ParsedBlock()
    data class Paragraph(val text: String) : ParsedBlock()
}

@Composable
fun ProfessionalAnalysisCard(
    text: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    val parsedBlocks = remember(text) { parseAnalysisText(text) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = CardBackground,
        border = androidx.compose.foundation.BorderStroke(
            1.2.dp,
            Brush.verticalGradient(
                listOf(
                    Color(0xFF334A6E),
                    CardBorderColor,
                    Color(0xFF1E2838)
                )
            )
        ),
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Executive Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF1E3A8A), Color(0xFF0F766E))
                                )
                            )
                            .border(1.dp, AccentTeal.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = AccentTeal,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "EVREN ANALİZ RAPORU",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            text = "Kuantum Vizyon Motoru • Doğrulanmış Veri",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                // Copy Action Button
                FilledTonalIconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(text))
                        copied = true
                        Toast.makeText(context, "Analiz panoya kopyalandı", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = Color(0xFF1E293B),
                        contentColor = if (copied) BenefitGreen else AccentTeal
                    )
                ) {
                    Icon(
                        imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = "Kopyala",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(
                color = Color.White.copy(alpha = 0.08f),
                thickness = 1.dp
            )
            Spacer(modifier = Modifier.height(18.dp))

            // Formatted Blocks
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                parsedBlocks.forEach { block ->
                    when (block) {
                        is ParsedBlock.MainHeading -> {
                            // Ana Başlıklar: Ortalanmış, belirgin ve şık
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    color = Color(0xFF172033),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentTeal.copy(alpha = 0.35f))
                                ) {
                                    Text(
                                        text = block.text.uppercase(),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentTeal,
                                        textAlign = TextAlign.Center,
                                        letterSpacing = 1.5.sp,
                                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }

                        is ParsedBlock.SubHeading -> {
                            // Alt Başlıklar: Ortalanmış veya merkez vurgulu
                            Text(
                                text = block.text,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = AccentGold,
                                textAlign = TextAlign.Center,
                                letterSpacing = 0.8.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp, bottom = 2.dp)
                            )
                        }

                        is ParsedBlock.StatusAlert -> {
                            // Zehirli / Faydalı veya Kritik Durum Rozeti
                            val bgColor = if (block.isDanger) PoisonAlertRed.copy(alpha = 0.12f) else BenefitGreen.copy(alpha = 0.12f)
                            val borderColor = if (block.isDanger) PoisonAlertRed.copy(alpha = 0.5f) else BenefitGreen.copy(alpha = 0.5f)
                            val iconTint = if (block.isDanger) PoisonAlertRed else BenefitGreen

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = bgColor,
                                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (block.isDanger) Icons.Default.Warning else Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = iconTint,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = block.title,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = iconTint
                                        )
                                        if (block.text.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = block.text,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        is ParsedBlock.BulletItem -> {
                            // Madde işaretleri
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 8.dp, end = 10.dp)
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(AccentTeal)
                                )
                                Text(
                                    text = block.text,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = PlusJakartaSansFamily,
                                        lineHeight = 24.sp
                                    ),
                                    color = TextPrimary,
                                    textAlign = TextAlign.Justify,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        is ParsedBlock.Paragraph -> {
                            // Paragraflar: Sağa ve sola yaslanmış (Justify) + Paragraf başı girintisi (TextIndent)
                            Text(
                                text = block.text,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = PlusJakartaSansFamily,
                                    fontSize = 15.sp,
                                    lineHeight = 26.sp,
                                    letterSpacing = 0.2.sp,
                                    textIndent = TextIndent(firstLine = 24.sp)
                                ),
                                color = TextPrimary,
                                textAlign = TextAlign.Justify,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Parses markdown-like or formatted text into structured blocks:
 * - Detects poison / benefit / safety signals and highlights them
 * - Recognizes main headings (# or **Title**) and centers them
 * - Extracts bullet items
 * - Formats standard paragraphs with justify and indent
 */
private fun parseAnalysisText(rawText: String): List<ParsedBlock> {
    val blocks = mutableListOf<ParsedBlock>()
    val lines = rawText.split("\n")
    val currentParagraph = StringBuilder()

    fun flushParagraph() {
        val paragraphText = currentParagraph.toString().trim()
        if (paragraphText.isNotEmpty()) {
            // Check if this paragraph contains a prominent poison or beneficial status note
            val lower = paragraphText.lowercase()
            if ((lower.contains("zehirli") || lower.contains("zehirli değil") || lower.contains("faydalı") || lower.contains("toksik")) && paragraphText.length < 220) {
                val isDanger = lower.contains("zehirli") && !lower.contains("zehirli değil") || lower.contains("toksik")
                val title = if (isDanger) "DİKKAT: ZEHİRLİ / TOKSİK ÖZELLİK" else "GÜVENLİ & FAYDALI"
                blocks.add(ParsedBlock.StatusAlert(title = title, text = paragraphText, isDanger = isDanger))
            } else {
                blocks.add(ParsedBlock.Paragraph(paragraphText))
            }
            currentParagraph.clear()
        }
    }

    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) {
            flushParagraph()
            continue
        }

        // Markdown Headings: #, ##, ###
        if (trimmed.startsWith("#")) {
            flushParagraph()
            val level = trimmed.takeWhile { it == '#' }.length
            val headingText = trimmed.removePrefix("#".repeat(level)).trim()
            if (level <= 2) {
                blocks.add(ParsedBlock.MainHeading(headingText))
            } else {
                blocks.add(ParsedBlock.SubHeading(headingText))
            }
            continue
        }

        // Standalone bold line as heading: **Heading** or numbered heading like 1. Tanım:
        if ((trimmed.startsWith("**") && trimmed.endsWith("**") && trimmed.length < 80) ||
            (trimmed.matches(Regex("^[0-9]+\\.\\s+[A-ZÇĞİÖŞÜ].*")) && trimmed.length < 80 && !trimmed.endsWith("."))
        ) {
            flushParagraph()
            val cleanHeading = trimmed.replace("**", "").trim()
            blocks.add(ParsedBlock.MainHeading(cleanHeading))
            continue
        }

        // Subheading with colon at end: e.g. "Faydaları:" or "Kullanım Alanları:"
        if (trimmed.endsWith(":") && trimmed.length < 50 && !trimmed.contains(" ")) {
            flushParagraph()
            blocks.add(ParsedBlock.SubHeading(trimmed.removeSuffix(":")))
            continue
        }

        // Bullet point lines: -, *, •, or numbered points
        if (trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ")) {
            flushParagraph()
            val bulletText = trimmed.substring(2).trim().replace("**", "")
            blocks.add(ParsedBlock.BulletItem(bulletText))
            continue
        }

        // Accumulate into paragraph
        if (currentParagraph.isNotEmpty()) {
            currentParagraph.append(" ")
        }
        currentParagraph.append(trimmed.replace("**", ""))
    }
    flushParagraph()

    // If completely empty or no blocks, fallback to single paragraph
    if (blocks.isEmpty() && rawText.isNotBlank()) {
        blocks.add(ParsedBlock.Paragraph(rawText.trim()))
    }
    return blocks
}
