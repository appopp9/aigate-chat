package com.aigate.chat.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.CompositionLocalProvider
import coil.compose.AsyncImage
import com.aigate.chat.ui.components.NeoBox
import com.aigate.chat.ui.components.NeoIconButton
import com.aigate.chat.util.CodeColors
import com.aigate.chat.util.CodeHighlighter
import com.aigate.chat.util.ContentBlock
import com.aigate.chat.util.ContentParser
import com.aigate.chat.util.FileSaver
import kotlinx.coroutines.launch

fun copyToClipboard(context: Context, text: String) {
	val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
	manager?.setPrimaryClip(ClipData.newPlainText("AiGate", text))
}

private fun isPreviewable(lang: String): Boolean {
	val l = lang.lowercase()
	return l == "html" || l == "svg" || l == "xml" && false || l == "htm"
}

@Composable
fun MessageContent(
	raw: String,
	textColor: Color,
	fontScale: Float = 1f,
	onToast: (String) -> Unit = {},
) {
	val context = LocalContext.current
	val scope = rememberCoroutineScope()
	val blocks = remember(raw) { ContentParser.parse(raw) }
	var previewHtml by remember { mutableStateOf<String?>(null) }

	Column(modifier = Modifier.fillMaxWidth()) {
		for (block in blocks) {
			when (block) {
				is ContentBlock.Text -> {
					Text(
						text = block.text,
						style = MaterialTheme.typography.bodyLarge.copy(
							fontSize = MaterialTheme.typography.bodyLarge.fontSize * fontScale,
						),
						color = textColor,
						modifier = Modifier.fillMaxWidth(),
					)
					Spacer(Modifier.height(6.dp))
				}

				is ContentBlock.Code -> {
					CodeBlockView(
						lang = block.lang,
						code = block.code,
						onCopy = {
							copyToClipboard(context, block.code)
							onToast("کد کپی شد")
						},
						onSave = {
							scope.launch {
								val name = "aigate_" + System.currentTimeMillis() + "." +
									FileSaver.extForLang(block.lang)
								val path = FileSaver.saveText(context, name, block.code)
								onToast(if (path != null) "ذخیره شد: " + path else "ذخیره ناموفق بود")
							}
						},
						onPreview = if (isPreviewable(block.lang)) {
							{ previewHtml = block.code }
						} else {
							null
						},
					)
					Spacer(Modifier.height(10.dp))
				}

				is ContentBlock.Image -> {
					NeoBox(
						modifier = Modifier.fillMaxWidth(),
						background = MaterialTheme.colorScheme.surface,
					) {
						Column {
							AsyncImage(
								model = block.url,
								contentDescription = block.alt,
								contentScale = ContentScale.FillWidth,
								modifier = Modifier
									.fillMaxWidth()
									.heightIn(min = 120.dp, max = 420.dp),
							)
							Row(
								modifier = Modifier
									.fillMaxWidth()
									.padding(8.dp),
								horizontalArrangement = Arrangement.End,
								verticalAlignment = Alignment.CenterVertically,
							) {
								NeoIconButton(
									icon = Icons.Filled.Download,
									boxSize = 38.dp,
									containerColor = MaterialTheme.colorScheme.primaryContainer,
									onClick = {
										scope.launch {
											val path = FileSaver.saveFromUrl(context, block.url)
											onToast(if (path != null) "ذخیره شد: " + path else "دانلود ناموفق بود")
										}
									},
								)
							}
						}
					}
					Spacer(Modifier.height(10.dp))
				}

				is ContentBlock.FileLink -> {
					NeoBox(
						modifier = Modifier.fillMaxWidth(),
						background = MaterialTheme.colorScheme.secondaryContainer,
					) {
						Row(
							modifier = Modifier
								.fillMaxWidth()
								.padding(10.dp),
							verticalAlignment = Alignment.CenterVertically,
						) {
							Icon(
								Icons.Filled.InsertDriveFile,
								contentDescription = null,
								tint = MaterialTheme.colorScheme.onSecondaryContainer,
								modifier = Modifier.size(22.dp),
							)
							Spacer(Modifier.size(8.dp))
							Text(
								text = block.label,
								style = MaterialTheme.typography.labelLarge,
								color = MaterialTheme.colorScheme.onSecondaryContainer,
								modifier = Modifier.weight(1f),
							)
							NeoIconButton(
								icon = Icons.Filled.Download,
								boxSize = 38.dp,
								onClick = {
									scope.launch {
										val path = FileSaver.saveFromUrl(context, block.url, block.label)
										onToast(if (path != null) "ذخیره شد: " + path else "دانلود ناموفق بود")
									}
								},
							)
						}
					}
					Spacer(Modifier.height(10.dp))
				}
			}
		}
	}

	val html = previewHtml
	if (html != null) {
		LivePreviewDialog(html = html, onDismiss = { previewHtml = null })
	}
}

@Composable
private fun CodeBlockView(
	lang: String,
	code: String,
	onCopy: () -> Unit,
	onSave: () -> Unit,
	onPreview: (() -> Unit)?,
) {
	val scheme = MaterialTheme.colorScheme
	val colors = remember(scheme) {
		CodeColors(
			plain = Color(0xFFE8E6DF),
			keyword = Color(0xFFFF7BAC),
			type = Color(0xFF7FDBFF),
			string = Color(0xFFB5F44A),
			comment = Color(0xFF8A8F98),
			number = Color(0xFFFFD93D),
			punctuation = Color(0xFFC7A6FF),
		)
	}
	val highlighted = remember(code, lang) { CodeHighlighter.highlight(code, lang, colors) }
	val shape = RoundedCornerShape(8.dp)

	Column(
		modifier = Modifier
			.fillMaxWidth()
			.clip(shape)
			.background(Color(0xFF14141A))
			.border(2.5.dp, scheme.outline, shape)
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.background(Color(0xFF23232C))
				.padding(horizontal = 10.dp, vertical = 6.dp),
			verticalAlignment = Alignment.CenterVertically,
		) {
			Text(
				text = lang.ifBlank { "code" },
				style = MaterialTheme.typography.labelSmall,
				color = Color(0xFFB5F44A),
				modifier = Modifier.weight(1f),
			)
			if (onPreview != null) {
				NeoIconButton(
					icon = Icons.Filled.PlayArrow,
					boxSize = 32.dp,
					containerColor = Color(0xFFB5F44A),
					contentColor = Color(0xFF101010),
					onClick = onPreview,
					contentDescription = "پیش‌نمایش زنده",
				)
				Spacer(Modifier.size(6.dp))
			}
			NeoIconButton(
				icon = Icons.Filled.Download,
				boxSize = 32.dp,
				containerColor = Color(0xFF3A3A46),
				contentColor = Color(0xFFF0EDE6),
				onClick = onSave,
				contentDescription = "ذخیره",
			)
			Spacer(Modifier.size(6.dp))
			NeoIconButton(
				icon = Icons.Filled.ContentCopy,
				boxSize = 32.dp,
				containerColor = Color(0xFF3A3A46),
				contentColor = Color(0xFFF0EDE6),
				onClick = onCopy,
				contentDescription = "کپی",
			)
		}
		CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
			Box(
				modifier = Modifier
					.fillMaxWidth()
					.horizontalScroll(rememberScrollState())
					.padding(12.dp)
			) {
				Text(
					text = highlighted,
					fontFamily = FontFamily.Monospace,
					fontSize = 13.sp,
					lineHeight = 20.sp,
					textAlign = TextAlign.Start,
				)
			}
		}
	}
}

@Composable
private fun LivePreviewDialog(html: String, onDismiss: () -> Unit) {
	Dialog(onDismissRequest = onDismiss) {
		NeoBox(
			modifier = Modifier.fillMaxWidth(),
			background = MaterialTheme.colorScheme.surface,
		) {
			Column(modifier = Modifier.padding(12.dp)) {
				Row(verticalAlignment = Alignment.CenterVertically) {
					Text(
						"پیش‌نمایش زنده",
						style = MaterialTheme.typography.titleMedium,
						color = MaterialTheme.colorScheme.onSurface,
						modifier = Modifier.weight(1f),
					)
					NeoIconButton(
						icon = Icons.Filled.Close,
						boxSize = 36.dp,
						onClick = onDismiss,
					)
				}
				Spacer(Modifier.height(10.dp))
				Box(
					modifier = Modifier
						.fillMaxWidth()
						.height(420.dp)
						.clip(RoundedCornerShape(8.dp))
						.border(2.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
				) {
					AndroidView(
						modifier = Modifier.fillMaxWidth(),
						factory = { ctx ->
							WebView(ctx).apply {
								settings.javaScriptEnabled = true
								settings.domStorageEnabled = true
								setBackgroundColor(0xFFFFFFFF.toInt())
							}
						},
						update = { view ->
							view.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
						},
					)
				}
			}
		}
	}
}
