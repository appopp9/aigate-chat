package com.aigate.chat.ui

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.aigate.chat.ui.components.NeoIconButton

/** matn e artifact e jari. */
object ArtifactHolder {
	var code: String = ""

	fun asHtml(): String {
		val body = code.trim()
		val lower = body.lowercase()
		if (lower.contains("<html") || lower.contains("<!doctype")) return body
		return "<!doctype html><html><head><meta charset=\"utf-8\">" +
			"<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">" +
			"<style>body{margin:0;padding:12px;font-family:sans-serif;background:#fff;color:#111}</style>" +
			"</head><body>" + body + "</body></html>"
	}

	/** aval block e html, agar nabood aval block e code ra barmigardanad. */
	fun extract(text: String): String {
		val fences = Regex("```([a-zA-Z0-9+#-]*)\\n([\\s\\S]*?)```").findAll(text).toList()
		val html = fences.firstOrNull { it.groupValues[1].lowercase().contains("html") }
		if (html != null) return html.groupValues[2]
		val svg = fences.firstOrNull { it.groupValues[1].lowercase().contains("svg") }
		if (svg != null) return svg.groupValues[2]
		val any = fences.firstOrNull()
		if (any != null) {
			val lang = any.groupValues[1].lowercase()
			val body = any.groupValues[2]
			if (lang == "css") return "<style>" + body + "</style><div>پیش‌نمایش CSS</div>"
			if (lang == "js" || lang == "javascript") {
				return "<div id=\"app\"></div><script>" + body + "</script>"
			}
			return body
		}
		return text
	}

	fun hasPreviewable(text: String): Boolean {
		val lower = text.lowercase()
		return lower.contains("```html") || lower.contains("```svg") ||
			lower.contains("```css") || lower.contains("```js") ||
			lower.contains("```javascript") || lower.contains("<!doctype html")
	}
}

@Composable
@SuppressLint("SetJavaScriptEnabled")
fun ArtifactScreen(onBack: () -> Unit) {
	val context = LocalContext.current
	val clipboard = LocalClipboardManager.current
	val html = remember { ArtifactHolder.asHtml() }
	val webView = remember {
		WebView(context).also { view ->
			view.settings.javaScriptEnabled = true
			view.settings.domStorageEnabled = true
			view.layoutParams = ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT,
			)
			view.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
		}
	}

	Column(
		modifier = Modifier
			.fillMaxSize()
			.background(MaterialTheme.colorScheme.background)
			.statusBarsPadding()
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(horizontal = 12.dp, vertical = 8.dp),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(8.dp),
		) {
			NeoIconButton(
				icon = Icons.Filled.ArrowBack,
				contentDescription = "بازگشت",
				onClick = onBack,
			)
			Column(modifier = Modifier.weight(1f)) {
				Text(
					"اجرای خروجی",
					style = MaterialTheme.typography.titleMedium,
					fontWeight = FontWeight.Bold,
					color = MaterialTheme.colorScheme.onBackground,
				)
				Text(
					"کد زنده در داخل اپ اجرا می‌شود",
					style = MaterialTheme.typography.labelSmall,
					color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
				)
			}
			NeoIconButton(
				icon = Icons.Filled.ContentCopy,
				contentDescription = "کپی کد",
				onClick = { clipboard.setText(AnnotatedString(ArtifactHolder.code)) },
			)
			NeoIconButton(
				icon = Icons.Filled.Refresh,
				contentDescription = "اجرای دوباره",
				onClick = {
					webView.loadDataWithBaseURL(null, ArtifactHolder.asHtml(), "text/html", "utf-8", null)
				},
			)
		}
		Box(modifier = Modifier.fillMaxSize()) {
			AndroidView(factory = { webView }, modifier = Modifier.fillMaxSize())
		}
	}
}
