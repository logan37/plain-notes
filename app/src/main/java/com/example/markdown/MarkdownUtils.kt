package com.example.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

sealed class MarkdownNode {
    data class Header(val level: Int, val text: String) : MarkdownNode()
    data class Paragraph(val text: String) : MarkdownNode()
    data class BulletItem(val text: String, val level: Int = 0) : MarkdownNode()
    data class TaskItem(val text: String, val isChecked: Boolean) : MarkdownNode()
    data class Quote(val text: String) : MarkdownNode()
    data class CodeBlock(val code: String, val language: String = "") : MarkdownNode()
    object Divider : MarkdownNode()
}

object MarkdownParser {
    fun parse(content: String): List<MarkdownNode> {
        val nodes = mutableListOf<MarkdownNode>()
        val lines = content.lines()
        var inCodeBlock = false
        val codeBuffer = StringBuilder()
        var codeLang = ""

        for (line in lines) {
            val trimmed = line.trim()

            if (trimmed.startsWith("```")) {
                if (inCodeBlock) {
                    nodes.add(MarkdownNode.CodeBlock(codeBuffer.toString().trimEnd(), codeLang))
                    codeBuffer.clear()
                    inCodeBlock = false
                } else {
                    inCodeBlock = true
                    codeLang = trimmed.removePrefix("```").trim()
                }
                continue
            }

            if (inCodeBlock) {
                codeBuffer.append(line).append("\n")
                continue
            }

            when {
                trimmed.isEmpty() -> {
                    // Blank lines can separate paragraphs
                }
                trimmed == "---" || trimmed == "***" || trimmed == "___" -> {
                    nodes.add(MarkdownNode.Divider)
                }
                trimmed.startsWith("### ") -> {
                    nodes.add(MarkdownNode.Header(3, trimmed.removePrefix("### ").trim()))
                }
                trimmed.startsWith("## ") -> {
                    nodes.add(MarkdownNode.Header(2, trimmed.removePrefix("## ").trim()))
                }
                trimmed.startsWith("# ") -> {
                    nodes.add(MarkdownNode.Header(1, trimmed.removePrefix("# ").trim()))
                }
                trimmed.startsWith("- [ ] ") || trimmed.startsWith("* [ ] ") -> {
                    nodes.add(MarkdownNode.TaskItem(trimmed.substring(6).trim(), false))
                }
                trimmed.startsWith("- [x] ", ignoreCase = true) || trimmed.startsWith("* [x] ", ignoreCase = true) -> {
                    nodes.add(MarkdownNode.TaskItem(trimmed.substring(6).trim(), true))
                }
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    val indent = line.takeWhile { it == ' ' }.length / 2
                    nodes.add(MarkdownNode.BulletItem(trimmed.substring(2).trim(), indent))
                }
                trimmed.matches(Regex("""^\d+\.\s.*""")) -> {
                    val text = trimmed.substringAfter(". ").trim()
                    nodes.add(MarkdownNode.BulletItem(text, 0))
                }
                trimmed.startsWith("> ") -> {
                    nodes.add(MarkdownNode.Quote(trimmed.removePrefix("> ").trim()))
                }
                else -> {
                    nodes.add(MarkdownNode.Paragraph(trimmed))
                }
            }
        }

        if (inCodeBlock && codeBuffer.isNotEmpty()) {
            nodes.add(MarkdownNode.CodeBlock(codeBuffer.toString().trimEnd(), codeLang))
        }

        return nodes
    }

    fun renderSpans(text: String, primaryColor: Color): androidx.compose.ui.text.AnnotatedString {
        return buildAnnotatedString {
            var i = 0
            val len = text.length

            while (i < len) {
                // Inline Code: `code`
                if (text[i] == '`') {
                    val endIdx = text.indexOf('`', i + 1)
                    if (endIdx != -1) {
                        pushStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = primaryColor.copy(alpha = 0.12f),
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        append(text.substring(i + 1, endIdx))
                        pop()
                        i = endIdx + 1
                        continue
                    }
                }

                // Bold: **text** or __text__
                if (i + 1 < len && (text.startsWith("**", i) || text.startsWith("__", i))) {
                    val marker = text.substring(i, i + 2)
                    val endIdx = text.indexOf(marker, i + 2)
                    if (endIdx != -1) {
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                        append(text.substring(i + 2, endIdx))
                        pop()
                        i = endIdx + 2
                        continue
                    }
                }

                // Italic: *text* or _text_
                if (text[i] == '*' || text[i] == '_') {
                    val marker = text[i].toString()
                    val endIdx = text.indexOf(marker, i + 1)
                    if (endIdx != -1) {
                        pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                        append(text.substring(i + 1, endIdx))
                        pop()
                        i = endIdx + 1
                        continue
                    }
                }

                // Strikethrough: ~~text~~
                if (i + 1 < len && text.startsWith("~~", i)) {
                    val endIdx = text.indexOf("~~", i + 2)
                    if (endIdx != -1) {
                        pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                        append(text.substring(i + 2, endIdx))
                        pop()
                        i = endIdx + 2
                        continue
                    }
                }

                append(text[i])
                i++
            }
        }
    }
}

@Composable
fun MarkdownRenderer(
    content: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    maxLines: Int = Int.MAX_VALUE,
    interactiveCheckboxes: Boolean = true,
    onCheckboxToggled: ((text: String, isChecked: Boolean) -> Unit)? = null
) {
    val nodes = MarkdownParser.parse(content)
    var linesRendered = 0

    Column(modifier = modifier) {
        for (node in nodes) {
            if (linesRendered >= maxLines) break

            when (node) {
                is MarkdownNode.Header -> {
                    val (fontSize, fontWeight) = when (node.level) {
                        1 -> 20.sp to FontWeight.Bold
                        2 -> 17.sp to FontWeight.Bold
                        else -> 15.sp to FontWeight.SemiBold
                    }
                    Text(
                        text = node.text,
                        fontSize = fontSize,
                        fontWeight = fontWeight,
                        color = textColor,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    linesRendered++
                }
                is MarkdownNode.Paragraph -> {
                    Text(
                        text = MarkdownParser.renderSpans(node.text, textColor),
                        fontSize = 14.sp,
                        color = textColor,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                    linesRendered++
                }
                is MarkdownNode.BulletItem -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = (node.level * 16).dp, top = 2.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 7.dp, end = 8.dp)
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(textColor.copy(alpha = 0.8f))
                        )
                        Text(
                            text = MarkdownParser.renderSpans(node.text, textColor),
                            fontSize = 14.sp,
                            color = textColor,
                            lineHeight = 20.sp
                        )
                    }
                    linesRendered++
                }
                is MarkdownNode.TaskItem -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 1.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (interactiveCheckboxes && onCheckboxToggled != null) {
                            Checkbox(
                                checked = node.isChecked,
                                onCheckedChange = { onCheckboxToggled(node.text, it) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary,
                                    uncheckedColor = textColor.copy(alpha = 0.6f)
                                ),
                                modifier = Modifier.size(32.dp)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(16.dp)
                                    .border(
                                        width = 1.5.dp,
                                        color = if (node.isChecked) MaterialTheme.colorScheme.primary else textColor.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(3.dp)
                                    )
                                    .then(
                                        if (node.isChecked) Modifier.background(
                                            MaterialTheme.colorScheme.primary,
                                            RoundedCornerShape(3.dp)
                                        ) else Modifier
                                    )
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = MarkdownParser.renderSpans(node.text, textColor),
                            fontSize = 14.sp,
                            color = if (node.isChecked) textColor.copy(alpha = 0.5f) else textColor,
                            textDecoration = if (node.isChecked) TextDecoration.LineThrough else TextDecoration.None
                        )
                    }
                    linesRendered++
                }
                is MarkdownNode.Quote -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(22.dp)
                                .background(textColor.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = MarkdownParser.renderSpans(node.text, textColor),
                            fontSize = 13.sp,
                            fontStyle = FontStyle.Italic,
                            color = textColor.copy(alpha = 0.85f)
                        )
                    }
                    linesRendered++
                }
                is MarkdownNode.CodeBlock -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(textColor.copy(alpha = 0.08f))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = node.code,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = textColor
                        )
                    }
                    linesRendered += 2
                }
                is MarkdownNode.Divider -> {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 6.dp),
                        thickness = 1.dp,
                        color = textColor.copy(alpha = 0.2f)
                    )
                    linesRendered++
                }
            }
        }
    }
}
