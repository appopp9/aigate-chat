package com.aigate.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aigate.chat.model.Provider
import com.aigate.chat.ui.components.NeoBox
import com.aigate.chat.ui.components.NeoButton
import com.aigate.chat.ui.components.NeoIconButton
import com.aigate.chat.util.TokenCounter
import com.aigate.chat.util.rememberHaptics

@Composable
fun CompareScreen(viewModel: ChatViewModel, onBack: () -> Unit) {
	val state by viewModel.state.collectAsStateCompat()
	val haptics = rememberHaptics(state.settings.hapticsEnabled)
	val compare = state.compare

	Column(
		modifier = Modifier
			.fillMaxSize()
			.background(MaterialTheme.colorScheme.background)
			.statusBarsPadding()
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(12.dp),
			verticalAlignment = Alignment.CenterVertically,
		) {
			NeoIconButton(icon = Icons.Filled.ArrowBack, onClick = onBack)
			Spacer(Modifier.width(10.dp))
			Text(
				"مقایسه دو مدل",
				style = MaterialTheme.typography.headlineSmall,
				fontWeight = FontWeight.Bold,
				color = MaterialTheme.colorScheme.onBackground,
			)
		}

		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(horizontal = 12.dp)
		) {
			NeoTextField(
				value = compare.prompt,
				onValueChange = { viewModel.setComparePrompt(it) },
				placeholder = "پرسش مشترک برای هر دو مدل…",
				modifier = Modifier.fillMaxWidth(),
			)
			Spacer(Modifier.height(10.dp))
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.spacedBy(8.dp),
				verticalAlignment = Alignment.CenterVertically,
			) {
				Box(modifier = Modifier.weight(1f)) {
					SidePicker(
						label = "مدل ۱",
						providers = state.providers,
						providerId = compare.left.providerId,
						model = compare.left.model,
						onPick = { p, m -> viewModel.setCompareSide(true, p, m) },
					)
				}
				Box(modifier = Modifier.weight(1f)) {
					SidePicker(
						label = "مدل ۲",
						providers = state.providers,
						providerId = compare.right.providerId,
						model = compare.right.model,
						onPick = { p, m -> viewModel.setCompareSide(false, p, m) },
					)
				}
			}
			Spacer(Modifier.height(10.dp))
			NeoButton(
				text = if (compare.left.running || compare.right.running) "در حال اجرا…" else "اجرای مقایسه",
				icon = Icons.Filled.PlayArrow,
				modifier = Modifier.fillMaxWidth(),
				enabled = !(compare.left.running || compare.right.running),
				onClick = {
					haptics.strong()
					viewModel.runCompare()
				},
			)
			Spacer(Modifier.height(12.dp))
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.weight(1f),
				horizontalArrangement = Arrangement.spacedBy(8.dp),
			) {
				Box(modifier = Modifier.weight(1f)) {
					ResultColumn(
						title = compare.left.model.ifBlank { "مدل ۱" },
						text = compare.left.text,
						error = compare.left.error,
						running = compare.left.running,
						tokens = compare.left.tokens,
						cost = compare.left.cost,
						elapsed = compare.left.elapsedMs,
					)
				}
				Box(modifier = Modifier.weight(1f)) {
					ResultColumn(
						title = compare.right.model.ifBlank { "مدل ۲" },
						text = compare.right.text,
						error = compare.right.error,
						running = compare.right.running,
						tokens = compare.right.tokens,
						cost = compare.right.cost,
						elapsed = compare.right.elapsedMs,
					)
				}
			}
			Spacer(Modifier.height(12.dp))
		}
	}
}

@Composable
private fun SidePicker(
	label: String,
	providers: List<Provider>,
	providerId: String,
	model: String,
	onPick: (String, String) -> Unit,
) {
	var expanded by remember { mutableStateOf(false) }
	val provider = providers.firstOrNull { it.id == providerId }
	Box {
		NeoButton(
			text = if (model.isBlank()) label else model,
			modifier = Modifier.fillMaxWidth(),
			containerColor = MaterialTheme.colorScheme.secondaryContainer,
			onClick = { expanded = true },
		)
		DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
			providers.forEach { p ->
				DropdownMenuItem(
					text = {
						Text(
							p.name + " (" + p.type.name + ")",
							fontWeight = FontWeight.Bold,
						)
					},
					onClick = {},
					enabled = false,
				)
				val list = if (p.favoriteModels.isNotEmpty()) {
					p.favoriteModels + p.models.filter { !p.favoriteModels.contains(it) }
				} else {
					p.models
				}
				list.take(40).forEach { m ->
					DropdownMenuItem(
						text = { Text(m) },
						onClick = {
							onPick(p.id, m)
							expanded = false
						},
					)
				}
			}
			if (provider == null && providers.isEmpty()) {
				DropdownMenuItem(
					text = { Text("ابتدا یک API اضافه کنید") },
					onClick = { expanded = false },
				)
			}
		}
	}
}

@Composable
private fun ResultColumn(
	title: String,
	text: String,
	error: String,
	running: Boolean,
	tokens: Int,
	cost: Double,
	elapsed: Long,
) {
	NeoBox(
		modifier = Modifier.fillMaxSize(),
		background = MaterialTheme.colorScheme.surface,
	) {
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(10.dp)
		) {
			Text(
				text = title,
				style = MaterialTheme.typography.labelLarge,
				fontWeight = FontWeight.Bold,
				color = MaterialTheme.colorScheme.onSurface,
			)
			Spacer(Modifier.height(6.dp))
			Text(
				text = TokenCounter.formatTokens(tokens) + " توکن · " +
					TokenCounter.formatCost(cost) + " · " + elapsed + "ms",
				style = MaterialTheme.typography.labelSmall,
				color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
			)
			Spacer(Modifier.height(8.dp))
			Column(
				modifier = Modifier
					.fillMaxSize()
					.verticalScroll(rememberScrollState())
			) {
				if (running) {
					Text(
						"در حال دریافت پاسخ…",
						style = MaterialTheme.typography.bodySmall,
						color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
					)
				} else if (error.isNotBlank()) {
					Text(
						text = error,
						style = MaterialTheme.typography.bodySmall,
						color = MaterialTheme.colorScheme.error,
					)
				} else {
					MessageContent(raw = text, textColor = MaterialTheme.colorScheme.onSurface)
				}
			}
		}
	}
}
