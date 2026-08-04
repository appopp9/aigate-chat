package com.aigate.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import com.aigate.chat.model.PromptItem
import com.aigate.chat.ui.components.NeoBox
import com.aigate.chat.ui.components.NeoButton
import com.aigate.chat.ui.components.NeoIconButton

@Composable
fun PromptsScreen(viewModel: ChatViewModel, onBack: () -> Unit) {
	val state by viewModel.state.collectAsStateCompat()
	var title by remember { mutableStateOf("") }
	var body by remember { mutableStateOf("") }
	var shortcut by remember { mutableStateOf("") }
	var editingId by remember { mutableStateOf("") }

	Column(
		modifier = Modifier
			.fillMaxSize()
			.background(MaterialTheme.colorScheme.background)
			.statusBarsPadding()
			.imePadding()
			.verticalScroll(rememberScrollState())
			.padding(14.dp),
		verticalArrangement = Arrangement.spacedBy(12.dp),
	) {
		Row(verticalAlignment = Alignment.CenterVertically) {
			NeoIconButton(
				icon = Icons.Filled.ArrowBack,
				boxSize = 48.dp,
				onClick = onBack,
				contentDescription = "bazgasht",
			)
			Spacer(Modifier.width(10.dp))
			Text(
				text = "prompt haye amade",
				style = MaterialTheme.typography.headlineSmall,
				fontWeight = FontWeight.Bold,
				color = MaterialTheme.colorScheme.onBackground,
			)
		}

		NeoBox(modifier = Modifier.fillMaxWidth(), background = MaterialTheme.colorScheme.surface) {
			Column(
				modifier = Modifier.padding(12.dp),
				verticalArrangement = Arrangement.spacedBy(8.dp),
			) {
				Text(
					text = if (editingId.isBlank()) "prompte jadid" else "virayeshe prompt",
					style = MaterialTheme.typography.titleSmall,
					fontWeight = FontWeight.Bold,
					color = MaterialTheme.colorScheme.onSurface,
				)
				NeoTextField(
					value = title,
					onValueChange = { title = it },
					placeholder = "onvan",
					modifier = Modifier.fillMaxWidth(),
				)
				NeoTextField(
					value = shortcut,
					onValueChange = { shortcut = it },
					placeholder = "meyanbor (masalan sum)",
					modifier = Modifier.fillMaxWidth(),
				)
				NeoTextField(
					value = body,
					onValueChange = { body = it },
					placeholder = "matne prompt",
					modifier = Modifier
						.fillMaxWidth()
						.heightIn(min = 120.dp),
				)
				Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
					NeoButton(
						text = if (editingId.isBlank()) "ezafe kon" else "zakhire",
						icon = if (editingId.isBlank()) Icons.Filled.Add else Icons.Filled.Check,
						enabled = title.isNotBlank() && body.isNotBlank(),
						onClick = {
							if (editingId.isBlank()) {
								viewModel.addPrompt(
									PromptItem(title = title.trim(), body = body, shortcut = shortcut.trim())
								)
							} else {
								val old = state.prompts.firstOrNull { it.id == editingId }
								if (old != null) {
									viewModel.updatePrompt(
										old.copy(
											title = title.trim(),
											body = body,
											shortcut = shortcut.trim(),
										)
									)
								}
							}
							title = ""
							body = ""
							shortcut = ""
							editingId = ""
						},
					)
					if (editingId.isNotBlank()) {
						NeoButton(
							text = "enseraf",
							containerColor = MaterialTheme.colorScheme.surfaceVariant,
							onClick = {
								title = ""
								body = ""
								shortcut = ""
								editingId = ""
							},
						)
					}
				}
			}
		}

		for (prompt in state.prompts) {
			NeoBox(modifier = Modifier.fillMaxWidth(), background = MaterialTheme.colorScheme.surface) {
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.padding(12.dp),
					verticalAlignment = Alignment.CenterVertically,
				) {
					Column(modifier = Modifier.weight(1f)) {
						Text(
							text = prompt.title +
								(if (prompt.shortcut.isBlank()) "" else "  ·  /" + prompt.shortcut),
							style = MaterialTheme.typography.labelLarge,
							fontWeight = FontWeight.Bold,
							color = MaterialTheme.colorScheme.onSurface,
						)
						Spacer(Modifier.height(4.dp))
						Text(
							text = prompt.body.take(90).replace('\n', ' '),
							style = MaterialTheme.typography.bodySmall,
							color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
						)
					}
					NeoIconButton(
						icon = Icons.Filled.Edit,
						boxSize = 44.dp,
						contentDescription = "virayesh",
						onClick = {
							editingId = prompt.id
							title = prompt.title
							body = prompt.body
							shortcut = prompt.shortcut
						},
					)
					Spacer(Modifier.width(6.dp))
					NeoIconButton(
						icon = Icons.Filled.Delete,
						boxSize = 44.dp,
						containerColor = MaterialTheme.colorScheme.errorContainer,
						contentDescription = "hazf",
						onClick = { viewModel.deletePrompt(prompt.id) },
					)
				}
			}
		}
	}
}
