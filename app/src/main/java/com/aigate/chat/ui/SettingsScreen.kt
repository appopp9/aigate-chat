package com.aigate.chat.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
import com.aigate.chat.ui.components.NeoBox
import com.aigate.chat.ui.components.NeoButton
import com.aigate.chat.ui.components.NeoIconButton
import com.aigate.chat.ui.theme.NeoThemes
import com.aigate.chat.util.rememberHaptics
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(viewModel: ChatViewModel, onBack: () -> Unit) {
	val state by viewModel.state.collectAsStateCompat()
	val settings = state.settings
	val haptics = rememberHaptics(settings.hapticsEnabled)

	var newMemory by remember { mutableStateOf("") }

	val restoreLauncher = rememberLauncherForActivityResult(
		contract = ActivityResultContracts.OpenDocument()
	) { uri ->
		if (uri != null) viewModel.importBackupFromUri(uri)
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
				.padding(12.dp),
			verticalAlignment = Alignment.CenterVertically,
		) {
			NeoIconButton(icon = Icons.Filled.ArrowBack, onClick = onBack)
			Spacer(Modifier.width(10.dp))
			Text(
				"تنظیمات",
				style = MaterialTheme.typography.headlineSmall,
				fontWeight = FontWeight.Bold,
				color = MaterialTheme.colorScheme.onBackground,
			)
		}

		LazyColumn(
			modifier = Modifier.fillMaxSize(),
			contentPadding = PaddingValues(12.dp),
			verticalArrangement = Arrangement.spacedBy(12.dp),
		) {
			// پیام سیستمی
			item {
				SectionCard("پیام سیستمی") {
					NeoTextField(
						value = settings.systemPrompt,
						onValueChange = { viewModel.updateSettings(settings.copy(systemPrompt = it)) },
						placeholder = "مثلاً: تو یک دستیار فارسی‌زبان هستی…",
						modifier = Modifier.fillMaxWidth(),
					)
				}
			}

			// تم رنگی
			item {
				SectionCard("تم رنگی") {
					Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
						NeoThemes.forEachIndexed { index, spec ->
							Row(verticalAlignment = Alignment.CenterVertically) {
								Box(
									modifier = Modifier
										.size(22.dp)
										.background(spec.accent, RoundedCornerShape(4.dp))
								)
								Spacer(Modifier.width(10.dp))
								Text(
									text = spec.name,
									style = MaterialTheme.typography.bodyMedium,
									color = MaterialTheme.colorScheme.onSurface,
									modifier = Modifier.weight(1f),
								)
								Switch(
									checked = settings.themeIndex == index,
									onCheckedChange = {
										haptics.tap()
										viewModel.updateSettings(settings.copy(themeIndex = index))
									},
								)
							}
						}
					}
				}
			}

			// رفتار
			item {
				SectionCard("رفتار برنامه") {
					Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
						ToggleRow("حالت تاریک", settings.darkMode) {
							viewModel.updateSettings(settings.copy(darkMode = it))
						}
						ToggleRow("پاسخ استریمی (حرف به حرف)", settings.streaming) {
							viewModel.updateSettings(settings.copy(streaming = it))
						}
						ToggleRow("ارسال فایل‌ها به صورت Base64", settings.sendFilesAsBase64) {
							viewModel.updateSettings(settings.copy(sendFilesAsBase64 = it))
						}
						ToggleRow("لرزش لمسی (Haptic)", settings.hapticsEnabled) {
							viewModel.updateSettings(settings.copy(hapticsEnabled = it))
						}
						ToggleRow("نمایش توکن و هزینه تقریبی", settings.showTokenStats) {
							viewModel.updateSettings(settings.copy(showTokenStats = it))
						}
						ToggleRow("پرسیدن قبل از فال‌بک (تعویض API)", settings.askFallback) {
							viewModel.updateSettings(settings.copy(askFallback = it))
						}
					}
				}
			}

			// دما و حد توکن
			item {
				SectionCard("خلاقیت و طول پاسخ") {
					Column {
						Text(
							"Temperature: " + (settings.temperature * 100).roundToInt() / 100.0,
							style = MaterialTheme.typography.bodyMedium,
							color = MaterialTheme.colorScheme.onSurface,
						)
						Slider(
							value = settings.temperature,
							onValueChange = { viewModel.updateSettings(settings.copy(temperature = it)) },
							valueRange = 0f..2f,
						)
						Spacer(Modifier.height(6.dp))
						Text(
							"حداکثر توکن پاسخ: " + settings.maxTokens,
							style = MaterialTheme.typography.bodyMedium,
							color = MaterialTheme.colorScheme.onSurface,
						)
						Slider(
							value = settings.maxTokens.toFloat(),
							onValueChange = {
								viewModel.updateSettings(settings.copy(maxTokens = it.roundToInt()))
							},
							valueRange = 256f..16384f,
						)
					}
				}
			}

			// حافظه بلندمدت
			item {
				SectionCard("حافظه بلندمدت") {
					Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
						ToggleRow("فعال بودن حافظه", settings.memoryEnabled) {
							viewModel.updateSettings(settings.copy(memoryEnabled = it))
						}
						settings.memory.forEach { item ->
							Row(verticalAlignment = Alignment.CenterVertically) {
								Switch(
									checked = item.enabled,
									onCheckedChange = { viewModel.updateMemory(item.copy(enabled = it)) },
								)
								Spacer(Modifier.width(6.dp))
								Box(modifier = Modifier.weight(1f)) {
									NeoTextField(
										value = item.text,
										onValueChange = { viewModel.updateMemory(item.copy(text = it)) },
										placeholder = "متن حافظه",
										modifier = Modifier.fillMaxWidth(),
									)
								}
								Spacer(Modifier.width(6.dp))
								NeoIconButton(
									icon = Icons.Filled.Delete,
									boxSize = 38.dp,
									containerColor = MaterialTheme.colorScheme.error,
									contentColor = MaterialTheme.colorScheme.onError,
									onClick = { viewModel.deleteMemory(item.id) },
									contentDescription = "حذف",
								)
							}
						}
						Row(verticalAlignment = Alignment.CenterVertically) {
							Box(modifier = Modifier.weight(1f)) {
								NeoTextField(
									value = newMemory,
									onValueChange = { newMemory = it },
									placeholder = "مورد جدید برای حافظه…",
									modifier = Modifier.fillMaxWidth(),
								)
							}
							Spacer(Modifier.width(6.dp))
							NeoIconButton(
								icon = Icons.Filled.Add,
								boxSize = 38.dp,
								onClick = {
									viewModel.addMemory(newMemory)
									newMemory = ""
								},
								contentDescription = "افزودن",
							)
						}
					}
				}
			}

			// پشتیبان‌گیری
			item {
				SectionCard("پشتیبان‌گیری و بازیابی") {
					Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
						NeoButton(
							text = "گرفتن پشتیبان",
							onClick = {
								haptics.strong()
								viewModel.exportBackup()
							},
						)
						NeoButton(
							text = "بازیابی از فایل",
							containerColor = MaterialTheme.colorScheme.secondaryContainer,
							onClick = {
								haptics.tap()
								restoreLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
							},
						)
					}
				}
			}

			item {
				Text(
					"AiGate Chat · نسخه 3.0",
					style = MaterialTheme.typography.labelSmall,
					color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
				)
			}
		}
	}
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
	NeoBox(
		modifier = Modifier.fillMaxWidth(),
		background = MaterialTheme.colorScheme.surface,
	) {
		Column(modifier = Modifier.padding(12.dp)) {
			Text(
				text = title,
				style = MaterialTheme.typography.titleMedium,
				fontWeight = FontWeight.Bold,
				color = MaterialTheme.colorScheme.onSurface,
			)
			Spacer(Modifier.height(10.dp))
			content()
		}
	}
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
	Row(
		modifier = Modifier.fillMaxWidth(),
		verticalAlignment = Alignment.CenterVertically,
	) {
		Text(
			text = label,
			style = MaterialTheme.typography.bodyMedium,
			color = MaterialTheme.colorScheme.onSurface,
			modifier = Modifier.weight(1f),
		)
		Switch(checked = checked, onCheckedChange = onChange)
	}
}
