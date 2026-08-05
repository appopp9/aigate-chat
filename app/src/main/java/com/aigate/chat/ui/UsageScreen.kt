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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aigate.chat.model.Conversation
import com.aigate.chat.ui.components.NeoBox
import com.aigate.chat.ui.components.NeoIconButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar

private data class ModelUsage(val model: String, val cost: Double, val tokens: Int)

private data class DayUsage(val label: String, val cost: Double)

@Composable
fun UsageScreen(viewModel: ChatViewModel, onBack: () -> Unit) {
	val state by viewModel.state.collectAsStateCompat()
	val monthKey = remember { SimpleDateFormat("yyyy-MM", Locale.US).format(Date()) }
	val records = state.usage.filter { it.month == monthKey }
	val totalCost = records.sumOf { it.costUsd }
	val totalTokens = records.sumOf { it.promptTokens + it.completionTokens }
	val perModel = records.groupBy { it.model }
		.map { entry ->
			ModelUsage(
				entry.key,
				entry.value.sumOf { it.costUsd },
				entry.value.sumOf { it.promptTokens + it.completionTokens },
			)
		}
		.sortedByDescending { it.cost }
	val daily = remember(state.conversations) { lastDays(state.conversations) }
	val maxDay = daily.maxOfOrNull { it.cost } ?: 0.0
	val maxModel = perModel.maxOfOrNull { it.cost } ?: 0.0
	val budget = state.settings.monthlyBudgetUsd

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
			Text(
				"داشبورد مصرف",
				style = MaterialTheme.typography.titleMedium,
				fontWeight = FontWeight.Bold,
				color = MaterialTheme.colorScheme.onBackground,
			)
		}

		LazyColumn(
			modifier = Modifier.fillMaxSize(),
			contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
			verticalArrangement = Arrangement.spacedBy(12.dp),
		) {
			item {
				NeoBox(modifier = Modifier.fillMaxWidth()) {
					Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
						Text(
							"این ماه (" + monthKey + ")",
							style = MaterialTheme.typography.labelMedium,
							color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
						)
						Text(
							"$" + money(totalCost),
							style = MaterialTheme.typography.headlineMedium,
							fontWeight = FontWeight.Bold,
							color = MaterialTheme.colorScheme.onSurface,
						)
						Text(
							"توکن مصرف‌شده: " + totalTokens,
							style = MaterialTheme.typography.labelMedium,
							color = MaterialTheme.colorScheme.onSurface,
						)
						if (budget > 0.0) {
							val frac = (totalCost / budget).coerceIn(0.0, 1.0).toFloat()
							Text(
								"بودجه: $" + money(budget) + "  •  " + (frac * 100).toInt() + "%",
								style = MaterialTheme.typography.labelMedium,
								color = MaterialTheme.colorScheme.onSurface,
							)
							Box(
								modifier = Modifier
									.fillMaxWidth()
									.height(12.dp)
									.clip(RoundedCornerShape(6.dp))
									.background(MaterialTheme.colorScheme.surfaceVariant)
							) {
								Box(
									modifier = Modifier
										.fillMaxWidth(if (frac < 0.02f) 0.02f else frac)
										.height(12.dp)
										.background(
											if (frac >= 0.9f) MaterialTheme.colorScheme.error
											else MaterialTheme.colorScheme.primary
										)
								)
							}
						}
					}
				}
			}

			item {
				NeoBox(modifier = Modifier.fillMaxWidth()) {
					Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
						Text(
							"۱۴ روز گذشته",
							style = MaterialTheme.typography.titleSmall,
							fontWeight = FontWeight.Bold,
							color = MaterialTheme.colorScheme.onSurface,
						)
						Row(
							modifier = Modifier
								.fillMaxWidth()
								.height(110.dp),
							verticalAlignment = Alignment.Bottom,
							horizontalArrangement = Arrangement.spacedBy(4.dp),
						) {
							for (day in daily) {
								val frac = if (maxDay <= 0.0) 0.0 else day.cost / maxDay
								val barHeight = (8 + (92 * frac)).toInt()
								Box(
									modifier = Modifier
										.weight(1f)
										.height(barHeight.dp)
										.clip(RoundedCornerShape(4.dp))
										.background(
											if (day.cost > 0.0) MaterialTheme.colorScheme.primary
											else MaterialTheme.colorScheme.surfaceVariant
										)
								)
							}
						}
						Text(
							text = (daily.firstOrNull()?.label ?: "") + "  ←  " + (daily.lastOrNull()?.label ?: ""),
							style = MaterialTheme.typography.labelSmall,
							color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
						)
					}
				}
			}

			item {
				Text(
					"هزینه به تفکیک مدل",
					style = MaterialTheme.typography.titleSmall,
					fontWeight = FontWeight.Bold,
					color = MaterialTheme.colorScheme.onBackground,
				)
			}

			if (perModel.isEmpty()) {
				item {
					Text(
						"هنوز مصرفی ثبت نشده است.",
						style = MaterialTheme.typography.bodySmall,
						color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
					)
				}
			} else {
				items(perModel.size) { index ->
					val row = perModel[index]
					val frac = if (maxModel <= 0.0) 0.0 else row.cost / maxModel
					NeoBox(modifier = Modifier.fillMaxWidth()) {
						Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
							Row(
								modifier = Modifier.fillMaxWidth(),
								verticalAlignment = Alignment.CenterVertically,
							) {
								Text(
									text = row.model,
									style = MaterialTheme.typography.bodyMedium,
									fontWeight = FontWeight.Bold,
									color = MaterialTheme.colorScheme.onSurface,
									modifier = Modifier.weight(1f),
								)
								Text(
									text = "$" + money(row.cost),
									style = MaterialTheme.typography.labelMedium,
									color = MaterialTheme.colorScheme.onSurface,
								)
							}
							Box(
								modifier = Modifier
									.fillMaxWidth()
									.height(10.dp)
									.clip(RoundedCornerShape(5.dp))
									.background(MaterialTheme.colorScheme.surfaceVariant)
							) {
								Box(
									modifier = Modifier
										.fillMaxWidth(if (frac < 0.03) 0.03f else frac.toFloat())
										.height(10.dp)
										.background(MaterialTheme.colorScheme.primary)
								)
							}
							Text(
								text = "توکن: " + row.tokens,
								style = MaterialTheme.typography.labelSmall,
								color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
							)
						}
					}
				}
			}

			item { Spacer(Modifier.height(24.dp)) }
		}
	}
}

private fun money(value: Double): String = String.format(Locale.US, "%.4f", value)

private fun lastDays(conversations: List<Conversation>): List<DayUsage> {
	val fmt = SimpleDateFormat("MM/dd", Locale.US)
	val buckets = LinkedHashMap<String, Double>()
	for (offset in 13 downTo 0) {
		val cal = Calendar.getInstance()
		cal.add(Calendar.DAY_OF_YEAR, -offset)
		buckets[fmt.format(cal.time)] = 0.0
	}
	for (conversation in conversations) {
		for (message in conversation.messages) {
			val key = fmt.format(Date(message.createdAt))
			if (buckets.containsKey(key)) {
				buckets[key] = (buckets[key] ?: 0.0) + message.costUsd
			}
		}
	}
	return buckets.map { DayUsage(it.key, it.value) }
}
