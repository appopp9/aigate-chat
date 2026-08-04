package com.aigate.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import com.aigate.chat.model.Persona
import com.aigate.chat.ui.components.NeoBox
import com.aigate.chat.ui.components.NeoButton
import com.aigate.chat.ui.components.NeoChip
import com.aigate.chat.ui.components.NeoIconButton

private val EmojiChoices = listOf("\uD83E\uDD16", "\uD83D\uDC69\u200D\uD83D\uDCBB", "\uD83E\uDDD1\u200D\uD83C\uDFEB", "\uD83D\uDCDD", "\uD83E\uDDEA", "\uD83C\uDFA8", "\uD83D\uDCBC", "\uD83D\uDD0D")

@Composable
fun PersonasScreen(viewModel: ChatViewModel, onBack: () -> Unit) {
	val state by viewModel.state.collectAsStateCompat()
	var name by remember { mutableStateOf("") }
	var emoji by remember { mutableStateOf(EmojiChoices.first()) }
	var systemPrompt by remember { mutableStateOf("") }
	var providerId by remember { mutableStateOf("") }
	var model by remember { mutableStateOf("") }
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
				text = "naghsh ha va shakhsiat ha",
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
					text = if (editingId.isBlank()) "naghshe jadid" else "virayeshe naghsh",
					style = MaterialTheme.typography.titleSmall,
					fontWeight = FontWeight.Bold,
					color = MaterialTheme.colorScheme.onSurface,
				)
				Row(
					modifier = Modifier.horizontalScroll(rememberScrollState()),
					horizontalArrangement = Arrangement.spacedBy(6.dp),
				) {
					for (choice in EmojiChoices) {
						NeoChip(
							text = choice,
							selected = choice == emoji,
							color = MaterialTheme.colorScheme.surfaceVariant,
							onClick = { emoji = choice },
						)
					}
				}
				NeoTextField(
					value = name,
					onValueChange = { name = it },
					placeholder = "name naghsh",
					modifier = Modifier.fillMaxWidth(),
				)
				NeoTextField(
					value = systemPrompt,
					onValueChange = { systemPrompt = it },
					placeholder = "dastoore system baraye in naghsh",
					modifier = Modifier
						.fillMaxWidth()
						.heightIn(min = 110.dp),
				)
				Text(
					text = "API va model (ekhtiari)",
					style = MaterialTheme.typography.labelSmall,
					color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
				)
				Row(
					modifier = Modifier.horizontalScroll(rememberScrollState()),
					horizontalArrangement = Arrangement.spacedBy(6.dp),
				) {
					NeoChip(
						text = "pishfarz",
						selected = providerId.isBlank(),
						color = MaterialTheme.colorScheme.surfaceVariant,
						onClick = {
							providerId = ""
							model = ""
						},
					)
					for (provider in state.providers) {
						NeoChip(
							text = provider.name,
							selected = providerId == provider.id,
							color = MaterialTheme.colorScheme.surfaceVariant,
							onClick = {
								providerId = provider.id
								model = provider.defaultModel
							},
						)
					}
				}
				if (providerId.isNotBlank()) {
					NeoTextField(
						value = model,
						onValueChange = { model = it },
						placeholder = "name model",
						modifier = Modifier.fillMaxWidth(),
					)
				}
				Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
					NeoButton(
						text = if (editingId.isBlank()) "ezafe kon" else "zakhire",
						icon = if (editingId.isBlank()) Icons.Filled.Add else Icons.Filled.Check,
						enabled = name.isNotBlank() && systemPrompt.isNotBlank(),
						onClick = {
							if (editingId.isBlank()) {
								viewModel.addPersona(
									Persona(
										name = name.trim(),
										emoji = emoji,
										systemPrompt = systemPrompt,
										providerId = providerId,
										model = model.trim(),
									)
								)
							} else {
								val old = state.personas.firstOrNull { it.id == editingId }
								if (old != null) {
									viewModel.updatePersona(
										old.copy(
											name = name.trim(),
											emoji = emoji,
											systemPrompt = systemPrompt,
											providerId = providerId,
											model = model.trim(),
										)
									)
								}
							}
							name = ""
							systemPrompt = ""
							providerId = ""
							model = ""
							editingId = ""
						},
					)
					if (editingId.isNotBlank()) {
						NeoButton(
							text = "enseraf",
							containerColor = MaterialTheme.colorScheme.surfaceVariant,
							onClick = {
								name = ""
								systemPrompt = ""
								providerId = ""
								model = ""
								editingId = ""
							},
						)
					}
				}
			}
		}

		for (persona in state.personas) {
			NeoBox(modifier = Modifier.fillMaxWidth(), background = MaterialTheme.colorScheme.surface) {
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.padding(12.dp),
					verticalAlignment = Alignment.CenterVertically,
				) {
					Column(modifier = Modifier.weight(1f)) {
						Text(
							text = persona.emoji + "  " + persona.name,
							style = MaterialTheme.typography.labelLarge,
							fontWeight = FontWeight.Bold,
							color = MaterialTheme.colorScheme.onSurface,
						)
						Spacer(Modifier.height(4.dp))
						Text(
							text = persona.systemPrompt.take(90).replace('\n', ' '),
							style = MaterialTheme.typography.bodySmall,
							color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
						)
						if (persona.model.isNotBlank()) {
							Text(
								text = "model: " + persona.model,
								style = MaterialTheme.typography.labelSmall,
								color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
							)
						}
					}
					NeoIconButton(
						icon = Icons.Filled.Edit,
						boxSize = 44.dp,
						contentDescription = "virayesh",
						onClick = {
							editingId = persona.id
							name = persona.name
							emoji = persona.emoji
							systemPrompt = persona.systemPrompt
							providerId = persona.providerId
							model = persona.model
						},
					)
					Spacer(Modifier.width(6.dp))
					NeoIconButton(
						icon = Icons.Filled.Delete,
						boxSize = 44.dp,
						containerColor = MaterialTheme.colorScheme.errorContainer,
						contentDescription = "hazf",
						onClick = { viewModel.deletePersona(persona.id) },
					)
				}
			}
		}
	}
}
