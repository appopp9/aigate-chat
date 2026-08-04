package com.aigate.chat.ui

import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.aigate.chat.net.WebSessionClient

/**
 * WebView makhfi vali vasl-shode be panjere.
 * bedoone vasl budan, Chromium frame nemikeshad va site (React) hydrate nemishavad
 * pas hich payami ersal nemishod. in host hamishe dar background zende ast.
 */
@Composable
fun WebSessionHost(url: String) {
	val context = LocalContext.current
	val webView = remember {
		WebView(context).also { view ->
			WebSessionClient.configure(view)
			view.layoutParams = ViewGroup.LayoutParams(1, 1)
			view.loadUrl(url)
		}
	}

	DisposableEffect(webView) {
		WebSessionClient.attachHost(webView)
		onDispose { WebSessionClient.detachHost(webView) }
	}

	AndroidView(
		factory = { webView },
		modifier = Modifier
			.size(1.dp)
			.alpha(0.004f),
	)
}
