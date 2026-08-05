package com.aigate.chat.ui

import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import com.aigate.chat.net.WebSessionClient
import com.aigate.chat.net.WebSites

/**
 * WebView e makhfi vali vasl-shode be panjere.
 *
 * bedoone vasl budan, Chromium frame nemikeshad va site (React) hydrate nemishavad.
 * bedoone andaazeye vaghei ham layout anjam nemishavad va textContent/innerText
 * khali barmigardad. pas andaazeash tamam-safhe ast vali posht e UI keshide mishavad
 * (dar Box ghabl az NavHost) ta lams ha be an naresad.
 */
@Composable
fun WebSessionHost(url: String) {
	val context = LocalContext.current
	val siteKey = WebSites.forUrl(url).host
	val webView = remember(url) {
		WebView(context).also { view ->
			WebSessionClient.configure(view)
			view.layoutParams = ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT,
			)
			view.loadUrl(url)
		}
	}

	DisposableEffect(webView) {
		WebSessionClient.attachHost(siteKey, webView)
		onDispose { WebSessionClient.detachHost(siteKey, webView) }
	}

	AndroidView(
		factory = { webView },
		modifier = Modifier
			.fillMaxSize()
			.alpha(0.004f),
	)
}
