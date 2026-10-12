package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CodeBlockBackgroundDark

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = false,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val context = LocalContext.current
    val sections = splitCodeBlocks(text)

    Column(modifier = modifier) {
        sections.forEach { section ->
            when (section) {
                is TextSection.CodeBlock -> {
                    CodeSnippetCard(
                        code = section.code,
                        language = section.language,
                        onCopy = {
                            copyToClipboard(context, section.code, "Code copied to clipboard")
                        }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
                is TextSection.RegularText -> {
                    val formatted = formatInlineMarkdown(section.content, textColor, MaterialTheme.colorScheme.primary)
                    Text(
                        text = formatted,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 22.sp,
                            color = textColor
                        ),
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CodeSnippetCard(
    code: String,
    language: String,
    onCopy: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .testTag("code_snippet_card"),
        color = CodeBlockBackgroundDark,
        tonalElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = language.ifEmpty { "code" }.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFFFFB1C8),
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = onCopy,
                    modifier = Modifier.testTag("copy_code_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy code",
                        tint = Color(0xFFFFB1C8)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF140B10), RoundedCornerShape(6.dp))
                    .horizontalScroll(rememberScrollState())
                    .padding(10.dp)
            ) {
                Text(
                    text = code.trimEnd(),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFF0E0E6),
                        lineHeight = 18.sp
                    )
                )
            }
        }
    }
}

private sealed class TextSection {
    data class RegularText(val content: String) : TextSection()
    data class CodeBlock(val language: String, val code: String) : TextSection()
}

private fun splitCodeBlocks(raw: String): List<TextSection> {
    val result = mutableListOf<TextSection>()
    val codeFenceRegex = Regex("```([a-zA-Z0-9_-]*)\\s*\\n([\\s\\S]*?)```")

    var lastIndex = 0
    codeFenceRegex.findAll(raw).forEach { matchResult ->
        val start = matchResult.range.first
        val end = matchResult.range.last + 1

        if (start > lastIndex) {
            val textBefore = raw.substring(lastIndex, start)
            if (textBefore.isNotBlank()) {
                result.add(TextSection.RegularText(textBefore))
            }
        }

        val lang = matchResult.groupValues[1].trim()
        val code = matchResult.groupValues[2]
        result.add(TextSection.CodeBlock(lang, code))

        lastIndex = end
    }

    if (lastIndex < raw.length) {
        val remaining = raw.substring(lastIndex)
        if (remaining.isNotBlank()) {
            result.add(TextSection.RegularText(remaining))
        }
    }

    if (result.isEmpty()) {
        result.add(TextSection.RegularText(raw))
    }

    return result
}

private fun formatInlineMarkdown(text: String, defaultColor: Color, highlightColor: Color) = buildAnnotatedString {
    val lines = text.split("\n")
    lines.forEachIndexed { index, line ->
        val trimmed = line.trimStart()

        when {
            trimmed.startsWith("### ") -> {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, color = highlightColor)) {
                    append(trimmed.removePrefix("### "))
                }
            }
            trimmed.startsWith("## ") -> {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp, color = highlightColor)) {
                    append(trimmed.removePrefix("## "))
                }
            }
            trimmed.startsWith("# ") -> {
                withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = highlightColor)) {
                    append(trimmed.removePrefix("# "))
                }
            }
            trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                withStyle(SpanStyle(color = highlightColor, fontWeight = FontWeight.Bold)) {
                    append(" • ")
                }
                appendInlineFormatting(trimmed.substring(2), defaultColor, highlightColor)
            }
            else -> {
                appendInlineFormatting(line, defaultColor, highlightColor)
            }
        }

        if (index < lines.size - 1) {
            append("\n")
        }
    }
}

private fun androidx.compose.ui.text.AnnotatedString.Builder.appendInlineFormatting(
    content: String,
    defaultColor: Color,
    highlightColor: Color
) {
    var cursor = 0
    val regex = Regex("(\\*\\*.*?\\*\\*)|(\\*.*?\\*)|(`.*?`)")
    val matches = regex.findAll(content)

    for (match in matches) {
        if (match.range.first > cursor) {
            append(content.substring(cursor, match.range.first))
        }
        val matchText = match.value
        when {
            matchText.startsWith("**") && matchText.endsWith("**") -> {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = defaultColor)) {
                    append(matchText.removeSurrounding("**"))
                }
            }
            matchText.startsWith("*") && matchText.endsWith("*") -> {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = defaultColor)) {
                    append(matchText.removeSurrounding("*"))
                }
            }
            matchText.startsWith("`") && matchText.endsWith("`") -> {
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = Color(0x33C2185B),
                        color = highlightColor,
                        fontWeight = FontWeight.SemiBold
                    )
                ) {
                    append(" ${matchText.removeSurrounding("`")} ")
                }
            }
            else -> append(matchText)
        }
        cursor = match.range.last + 1
    }

    if (cursor < content.length) {
        append(content.substring(cursor))
    }
}

private fun copyToClipboard(context: Context, text: String, message: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Copied Text", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
