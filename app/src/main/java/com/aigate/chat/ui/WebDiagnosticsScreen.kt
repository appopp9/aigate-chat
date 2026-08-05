package com.aigate.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aigate.chat.model.ProviderType
import com.aigate.chat.net.WebSessionClient
import com.aigate.chat.ui.components.NeoBox
import com.aigate.chat.ui.components.NeoButton
import com.aigate.chat.ui.components.NeoIconButton
import kotlinx.coroutines.launch

/**
 * safheye checkup e neshaste web: dah marhale ra test mikonad.
 */
@Composable
fun WebDiagnosticsScreen(viewModel: ChatViewModel, onBack: () -> Unit) {
	val state by viewModel.state.collectAsStateCompat()
	val context = LocalContext.current
	val scope = rememberCoroutineScope()
	val clipboard = LocalClipboardManager.current

	var running by remember { mutableStateOf(false) }
	var checks by remember { mutableStateOf(listOf<WebSessionClient.WebCheck>()) }

	val webProvider = state.providers.firstOrNull { it.type == ProviderType.WEB }

	Column(
		modifier = Modifier
			.fillMaxSize()
			.background(MaterialTheme.colorScheme.background)
			.statusBarsPadding()
			.padding(horizontal = 14.dp),
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(vertical = 10.dp),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(10.dp),
		) {
			NeoIconButton(
				icon = Icons.Filled.ArrowBack,
				contentDescription = "بازگشت",
				onClick = onBack,
			)
			Column(modifier = Modifier.weight(1f)) {
				Text(
					"چک‌آپ نشست وب",
					style = MaterialTheme.typography.titleMedium,
					fontWeight = FontWeight.Bold,
					color = MaterialTheme.colorScheme.onBackground,
				)
				Text(
					"ده مرحله را تست می‌کند و دقیق می‌گوید کدام حلقه پاره است",
					style = MaterialTheme.typography.labelSmall,
					color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
				)
			}
		}

		if (webProvider == null) {
			NeoBox(
				modifier = Modifier.fillMaxWidth(),
				background = MaterialTheme.colorScheme.errorContainer,
				contentPadding = PaddingValues(14.dp),
			) {
				Text(
					"هیچ API از نوع «نشست وب» نساخته‌ای. اول از بخش API‌ها یکی بساز و لاگین کن.",
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onErrorContainer,
				)
			}
			return@Column
		}

		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(bottom = 10.dp),
			horizontalArrangement = Arrangement.spacedBy(10.dp),
		) {
			NeoButton(
				text = if (running) "در حال بررسی..." else "شروع چک‌آپ کامل",
				enabled = !running,
				modifier = Modifier.weight(1f),
				onClick = {
					running = true
					checks = emptyList()
					scope.launch {
						checks = try {
							WebSessionClient.runDiagnostics(context, webProvider, true)
						} catch (t: Throwable) {
							listOf(
								WebSessionClient.WebCheck(
									"خطای اجرا",
									false,
									t.message ?: "نامشخص",
								)
							)
						}
						running = false
					}
				},
			)
			NeoButton(
				text = "بررسی سریع",
				enabled = !running,
				containerColor = MaterialTheme.colorScheme.secondary,
				contentColor = MaterialTheme.colorScheme.onSecondary,
				onClick = {
					running = true
					checks = emptyList()
					scope.launch {
						checks = try {
							WebSessionClient.runDiagnostics(context, webProvider, false)
						} catch (t: Throwable) {
							listOf(
								WebSessionClient.WebCheck(
									"خطای اجرا",
									false,
									t.message ?: "نامشخص",
								)
							)
						}
						running = false
					}
				},
			)
		}

		if (running) {
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.padding(bottom = 10.dp),
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.spacedBy(10.dp),
			) {
				CircularProgressIndicator(
					modifier = Modifier.padding(4.dp),
					color = MaterialTheme.colorScheme.primary,
				)
				Text(
					"چک‌آپ کامل تا ۲ دقیقه طول می‌کشد؛ یک پیام آزمایشی واقعی هم به سایت می‌فرستد",
					style = MaterialTheme.typography.labelSmall,
					color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
				)
			}
		}

		if (checks.isNotEmpty()) {
			val failed = checks.count { !it.ok }
			NeoBox(
				modifier = Modifier
					.fillMaxWidth()
					.padding(bottom = 10.dp),
				background = if (failed == 0) MaterialTheme.colorScheme.primaryContainer
				else MaterialTheme.colorScheme.errorContainer,
				contentPadding = PaddingValues(12.dp),
			) {
				Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
					Text(
						if (failed == 0) "همه‌ی مراحل سالم است" else "تعداد مراحل ناموفق: " + failed,
						style = MaterialTheme.typography.titleSmall,
						fontWeight = FontWeight.Bold,
						color = if (failed == 0) MaterialTheme.colorScheme.onPrimaryContainer
						else MaterialTheme.colorScheme.onErrorContainer,
					)
					NeoButton(
						text = "کپی گزارش کامل",
						onClick = {
							val report = checks.joinToString("\n") { item ->
								(if (item.ok) "[OK] " else "[FAIL] ") + item.title + " :: " + item.detail
							}
							clipboard.setText(AnnotatedString(report))
							viewModel.showToast("گزارش کپی شد")
						},
					)
				}
			}
		}

		LazyColumn(
			modifier = Modifier.fillMaxSize(),
			verticalArrangement = Arrangement.spacedBy(10.dp),
			contentPadding = PaddingValues(bottom = 24.dp),
		) {
			items(checks) { item ->
				NeoBox(
					modifier = Modifier.fillMaxWidth(),
					contentPadding = PaddingValues(12.dp),
				) {
					Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
						Row(
							modifier = Modifier.fillMaxWidth(),
							verticalAlignment = Alignment.CenterVertically,
							horizontalArrangement = Arrangement.spacedBy(8.dp),
						) {
							Text(
								if (item.ok) "✔" else "✖",
								style = MaterialTheme.typography.titleMedium,
								fontWeight = FontWeight.Bold,
								color = if (item.ok) Color(0xFF2E7D32) else Color(0xFFC62828),
							)
							Text(
								item.title,
								modifier = Modifier.weight(1f),
								style = MaterialTheme.typography.bodyMedium,
								fontWeight = FontWeight.Bold,
								color = MaterialTheme.colorScheme.onSurface,
							)
						}
						Text(
							item.detail,
							style = MaterialTheme.typography.labelSmall,
							color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
						)
					}
				}
			}
		}
	}
}
