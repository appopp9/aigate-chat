package com.aigate.chat.ui

import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.aigate.chat.net.WebSessionClient
import com.aigate.chat.ui.components.NeoIconButton

/**
 * safheye vorood be site: karbar inja login mikonad va cookie ha zakhire mishavand.
 */
@Composable
fun WebLoginScreen(url: String, onBack: () -> Unit) {
	val context = LocalContext.current
	val webView = remember {
		WebView(context).also { view ->
			WebSessionClient.configure(view)
			view.layoutParams = ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT,
			)
			view.loadUrl(url)
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
				onClick = {
					WebSessionClient.persistCookies()
					onBack()
				},
			)
			Column(modifier = Modifier.weight(1f)) {
				Text(
					"ورود به سایت",
					style = MaterialTheme.typography.titleMedium,
					fontWeight = FontWeight.Bold,
					color = MaterialTheme.colorScheme.onBackground,
				)
				Text(
					"بعد از لاگین کامل، با دکمه‌ی بازگشت خارج شو",
					style = MaterialTheme.typography.labelSmall,
					color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
				)
			}
			Spacer(modifier = Modifier.width(4.dp))
			NeoIconButton(
				icon = Icons.Filled.Refresh,
				contentDescription = "بازنشانی",
				onClick = { webView.reload() },
			)
		}
		Box(modifier = Modifier.fillMaxSize()) {
			AndroidView(
				factory = { webView },
				modifier = Modifier.fillMaxSize(),
			)
		}
	}
}
