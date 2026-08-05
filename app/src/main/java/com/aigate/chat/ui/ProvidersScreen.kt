package com.aigate.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aigate.chat.model.ConnectionState
import com.aigate.chat.model.ModelPricing
import com.aigate.chat.model.Provider
import com.aigate.chat.model.ProviderType
import com.aigate.chat.net.WebSessionClient
import com.aigate.chat.ui.components.NeoBox
import com.aigate.chat.ui.components.NeoButton
import com.aigate.chat.ui.components.NeoChip
import com.aigate.chat.ui.components.NeoIconButton
import com.aigate.chat.util.rememberHaptics

/** فیلد متنی با استایل Neobrutalism */
@Composable
fun NeoTextField(
	value: String,
	onValueChange: (String) -> Unit,
	placeholder: String,
	modifier: Modifier = Modifier,
) {
	val shape = RoundedCornerShape(6.dp)
	Box(
		modifier = modifier
			.background(MaterialTheme.colorScheme.surface, shape)
			.border(2.5.dp, MaterialTheme.colorScheme.outline, shape)
			.padding(horizontal = 12.dp, vertical = 10.dp)
	) {
		if (value.isEmpty()) {
			Text(
				text = placeholder,
				style = MaterialTheme.typography.bodyMedium,
				color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
			)
		}
		BasicTextField(
			value = value,
			onValueChange = onValueChange,
			modifier = Modifier.fillMaxWidth(),
			textStyle = TextStyle(
				color = MaterialTheme.colorScheme.onSurface,
				fontSize = MaterialTheme.typography.bodyMedium.fontSize,
				fontFamily = MaterialTheme.typography.bodyMedium.fontFamily,
			),
			cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
		)
	}
}

@Composable
fun ProvidersScreen(
	viewModel: ChatViewModel,
	onBack: () -> Unit,
	onOpenWebLogin: (String) -> Unit = {},
	onOpenWebCheck: () -> Unit = {},
) {
	val state by viewModel.state.collectAsStateCompat()
	val haptics = rememberHaptics(state.settings.hapticsEnabled)

	var name by remember { mutableStateOf("") }
	var baseUrl by remember { mutableStateOf("https://api.aigate.shop/v1") }
	var authKey by remember { mutableStateOf("") }
	var type by remember { mutableStateOf(ProviderType.OPENAI) }
	var inputPrice by remember { mutableStateOf("") }
	var outputPrice by remember { mutableStateOf("") }
	var fetchedModels by remember { mutableStateOf<List<String>>(emptyList()) }

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
				"مدیریت API ها",
				style = MaterialTheme.typography.headlineSmall,
				fontWeight = FontWeight.Bold,
				color = MaterialTheme.colorScheme.onBackground,
				modifier = Modifier.weight(1f),
			)
			NeoIconButton(
				icon = Icons.Filled.Refresh,
				containerColor = MaterialTheme.colorScheme.secondaryContainer,
				onClick = { viewModel.testAllProviders() },
				contentDescription = "تست همه",
			)
		}

		LazyColumn(
			modifier = Modifier.fillMaxSize(),
			contentPadding = PaddingValues(12.dp),
			verticalArrangement = Arrangement.spacedBy(12.dp),
		) {
			item {
				NeoBox(
					modifier = Modifier.fillMaxWidth(),
					background = MaterialTheme.colorScheme.surface,
				) {
					Column(modifier = Modifier.padding(12.dp)) {
						Text(
							"افزودن API جدید",
							style = MaterialTheme.typography.titleMedium,
							fontWeight = FontWeight.Bold,
							color = MaterialTheme.colorScheme.onSurface,
						)
						Spacer(Modifier.height(10.dp))
						Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
							NeoChip(
								text = "OpenAI Compatible",
								selected = type == ProviderType.OPENAI,
								onClick = { type = ProviderType.OPENAI },
							)
							NeoChip(
								text = "Anthropic Compatible",
								selected = type == ProviderType.ANTHROPIC,
								onClick = { type = ProviderType.ANTHROPIC },
							)
							NeoChip(
								text = "نشست وب (DeepSeek)",
								selected = type == ProviderType.WEB,
								onClick = {
									type = ProviderType.WEB
									baseUrl = WebSessionClient.DEFAULT_SITE
								},
							)
						}
						if (type == ProviderType.WEB) {
							Spacer(Modifier.height(10.dp))
							Text(
								"در این حالت کلید API لازم نیست. یک بار در سایت لاگین کن؛ بعد از این پیام‌ها خودکار در همان سایت ارسال می‌شود و پاسخ در چت نشان داده می‌شود.",
								style = MaterialTheme.typography.labelSmall,
								color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
							)
							Spacer(Modifier.height(6.dp))
							Text(
								"هشدار: این روش به ساختار صفحه‌ی سایت وابسته است، با تغییر سایت می‌شکند، گاهی کپچا می‌خواهد و ممکن است خلاف قوانین سرویس باشد.",
								style = MaterialTheme.typography.labelSmall,
								color = MaterialTheme.colorScheme.error,
							)
							Spacer(Modifier.height(8.dp))
							NeoButton(
								text = "ورود به سایت و لاگین",
								onClick = {
									haptics.tap()
									onOpenWebLogin(baseUrl.ifBlank { WebSessionClient.DEFAULT_SITE })
								},
							)
							Spacer(Modifier.height(8.dp))
							NeoButton(
								text = "چک‌آپ نشست وب (۱۰ مرحله)",
								containerColor = MaterialTheme.colorScheme.secondary,
								contentColor = MaterialTheme.colorScheme.onSecondary,
								onClick = {
									haptics.tap()
									onOpenWebCheck()
								},
							)
						}
						Spacer(Modifier.height(10.dp))
						NeoTextField(
							value = name,
							onValueChange = { name = it },
							placeholder = "اسم دلخواه (مثلاً AiGate)",
							modifier = Modifier.fillMaxWidth(),
						)
						Spacer(Modifier.height(8.dp))
						NeoTextField(
							value = baseUrl,
							onValueChange = { baseUrl = it },
							placeholder = "Base URL",
							modifier = Modifier.fillMaxWidth(),
						)
						Spacer(Modifier.height(8.dp))
						NeoTextField(
							value = authKey,
							onValueChange = { authKey = it },
							placeholder = "API Key",
							modifier = Modifier.fillMaxWidth(),
						)
						Spacer(Modifier.height(8.dp))
						Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
							Box(modifier = Modifier.weight(1f)) {
								NeoTextField(
									value = inputPrice,
									onValueChange = { inputPrice = it },
									placeholder = "قیمت ورودی /1M",
									modifier = Modifier.fillMaxWidth(),
								)
							}
							Box(modifier = Modifier.weight(1f)) {
								NeoTextField(
									value = outputPrice,
									onValueChange = { outputPrice = it },
									placeholder = "قیمت خروجی /1M",
									modifier = Modifier.fillMaxWidth(),
								)
							}
						}
						Spacer(Modifier.height(10.dp))
						Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
							NeoButton(
								text = if (state.isFetchingModels) "در حال دریافت…" else "دریافت مدل‌ها",
								icon = Icons.Filled.Download,
								enabled = !state.isFetchingModels,
								containerColor = MaterialTheme.colorScheme.secondaryContainer,
								onClick = {
									haptics.tap()
									viewModel.fetchModels(baseUrl, authKey, type) { models ->
										fetchedModels = models
									}
								},
							)
							NeoButton(
								text = "ذخیره",
								icon = Icons.Filled.Check,
								onClick = {
									haptics.strong()
									viewModel.addProvider(
										name = name,
										baseUrl = baseUrl,
										authKey = authKey,
										type = type,
										models = fetchedModels,
										inputPrice = inputPrice.toDoubleOrNull() ?: 0.0,
										outputPrice = outputPrice.toDoubleOrNull() ?: 0.0,
									)
									name = ""
									authKey = ""
									inputPrice = ""
									outputPrice = ""
									fetchedModels = emptyList()
								},
							)
						}
						if (fetchedModels.isNotEmpty()) {
							Spacer(Modifier.height(8.dp))
							Text(
								text = fetchedModels.size.toString() + " مدل دریافت شد و با ذخیره ثبت می‌شود",
								style = MaterialTheme.typography.labelSmall,
								color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
							)
						}
					}
				}
			}

			items(state.providers, key = { it.id }) { provider ->
				ProviderCard(
					provider = provider,
					statusState = state.statuses[provider.id]?.state ?: ConnectionState.UNKNOWN,
					statusMessage = state.statuses[provider.id]?.message.orEmpty(),
					latency = state.statuses[provider.id]?.latencyMs ?: 0L,
					onTest = {
						haptics.tap()
						viewModel.testProvider(provider.id)
					},
					onRefreshModels = { viewModel.refreshProviderModels(provider.id) },
					onDelete = { viewModel.deleteProvider(provider.id) },
					onToggleFavorite = { model -> viewModel.toggleFavoriteModel(provider.id, model) },
					onSetModelPrice = { model, inPrice, outPrice ->
						val rest = provider.modelPricing.filter { row -> row.model != model }
						viewModel.updateProvider(
							provider.copy(
								modelPricing = rest + ModelPricing(
									model = model,
									inputPricePerM = inPrice,
									outputPricePerM = outPrice,
								)
							)
						)
					},
					onSetDefaultModel = { model ->
						viewModel.updateProvider(provider.copy(defaultModel = model))
					},
				)
			}
		}
	}
}

@Composable
private fun ProviderCard(
	provider: Provider,
	statusState: ConnectionState,
	statusMessage: String,
	latency: Long,
	onTest: () -> Unit,
	onRefreshModels: () -> Unit,
	onDelete: () -> Unit,
	onToggleFavorite: (String) -> Unit,
	onSetModelPrice: (String, Double, Double) -> Unit,
	onSetDefaultModel: (String) -> Unit,
) {
	var modelQuery by remember { mutableStateOf("") }
	var expanded by remember { mutableStateOf(false) }

	val statusColor = when (statusState) {
		ConnectionState.ONLINE -> Color(0xFF3ED17A)
		ConnectionState.OFFLINE -> Color(0xFFE23636)
		ConnectionState.TESTING -> Color(0xFFFFD93D)
		ConnectionState.UNKNOWN -> Color(0xFF9A9A9A)
	}
	val statusLabel = when (statusState) {
		ConnectionState.ONLINE -> "متصل · " + latency + "ms"
		ConnectionState.OFFLINE -> "قطع"
		ConnectionState.TESTING -> "در حال تست…"
		ConnectionState.UNKNOWN -> "نامشخص"
	}

	var priceModel by remember { mutableStateOf("") }

	val filtered = remember(modelQuery, provider.models, provider.favoriteModels) {
		val q = modelQuery.trim()
		val list = if (q.isEmpty()) provider.models else provider.models.filter { it.contains(q, true) }
		list.sortedByDescending { provider.favoriteModels.contains(it) }
	}

	NeoBox(
		modifier = Modifier.fillMaxWidth(),
		background = MaterialTheme.colorScheme.surface,
	) {
		Column(modifier = Modifier.padding(12.dp)) {
			Row(verticalAlignment = Alignment.CenterVertically) {
				Box(
					modifier = Modifier
						.size(14.dp)
						.background(statusColor, RoundedCornerShape(7.dp))
				)
				Spacer(Modifier.width(8.dp))
				Column(modifier = Modifier.weight(1f)) {
					Text(
						text = provider.name,
						style = MaterialTheme.typography.titleMedium,
						fontWeight = FontWeight.Bold,
						color = MaterialTheme.colorScheme.onSurface,
					)
					Text(
						text = provider.type.name + "  ·  " + statusLabel,
						style = MaterialTheme.typography.labelSmall,
						color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
					)
				}
				NeoIconButton(
					icon = Icons.Filled.Refresh,
					boxSize = 38.dp,
					containerColor = MaterialTheme.colorScheme.primaryContainer,
					onClick = onTest,
					contentDescription = "تست اتصال",
				)
				Spacer(Modifier.width(6.dp))
				NeoIconButton(
					icon = Icons.Filled.Download,
					boxSize = 38.dp,
					containerColor = MaterialTheme.colorScheme.secondaryContainer,
					onClick = onRefreshModels,
					contentDescription = "به‌روزرسانی مدل‌ها",
				)
				Spacer(Modifier.width(6.dp))
				NeoIconButton(
					icon = Icons.Filled.Delete,
					boxSize = 38.dp,
					containerColor = MaterialTheme.colorScheme.error,
					contentColor = MaterialTheme.colorScheme.onError,
					onClick = onDelete,
					contentDescription = "حذف",
				)
			}
			if (statusMessage.isNotBlank() && statusState == ConnectionState.OFFLINE) {
				Spacer(Modifier.height(6.dp))
				Text(
					text = statusMessage,
					style = MaterialTheme.typography.labelSmall,
					color = MaterialTheme.colorScheme.error,
				)
			}
			Spacer(Modifier.height(8.dp))
			Text(
				text = provider.baseUrl,
				style = MaterialTheme.typography.labelSmall,
				color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
			)
			Spacer(Modifier.height(8.dp))
			Text(
				text = "مدل پیش‌فرض: " + provider.defaultModel.ifBlank { "—" },
				style = MaterialTheme.typography.labelLarge,
				color = MaterialTheme.colorScheme.onSurface,
			)
			Spacer(Modifier.height(8.dp))
			NeoButton(
				text = "لیست مدل‌ها (" + provider.models.size + ")",
				containerColor = MaterialTheme.colorScheme.surfaceVariant,
				onClick = { expanded = !expanded },
			)
			if (expanded) {
				Spacer(Modifier.height(8.dp))
				Text(
					text = "روی چیپ قیمت کنار هر مدل بزن تا برای همان مدل قیمت جداگانه ثبت کنی",
					style = MaterialTheme.typography.labelSmall,
					color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
				)
				Spacer(Modifier.height(6.dp))
				NeoTextField(
					value = modelQuery,
					onValueChange = { modelQuery = it },
					placeholder = "جست‌وجو در مدل‌ها…",
					modifier = Modifier.fillMaxWidth(),
				)
				Spacer(Modifier.height(8.dp))
				Column(
					modifier = Modifier
						.fillMaxWidth()
						.heightIn(max = 320.dp)
						.verticalScroll(rememberScrollState()),
					verticalArrangement = Arrangement.spacedBy(6.dp),
				) {
					for (model in filtered) {
						val isFavorite = provider.favoriteModels.contains(model)
						Row(
							modifier = Modifier.fillMaxWidth(),
							verticalAlignment = Alignment.CenterVertically,
						) {
							Icon(
								if (isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
								contentDescription = "علاقه‌مندی",
								tint = if (isFavorite) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurface,
								modifier = Modifier
									.size(22.dp)
									.clickable { onToggleFavorite(model) },
							)
							Spacer(Modifier.width(8.dp))
							Text(
								text = model,
								style = MaterialTheme.typography.bodyMedium,
								color = MaterialTheme.colorScheme.onSurface,
								modifier = Modifier
									.weight(1f)
									.clickable { onSetDefaultModel(model) },
							)
							if (provider.defaultModel == model) {
								NeoChip(text = "پیش‌فرض")
							}
							Spacer(Modifier.width(6.dp))
							val price = provider.pricingFor(model)
							NeoChip(
								text = "$" + price.inputPricePerM + "/" + price.outputPricePerM,
								onClick = { priceModel = if (priceModel == model) "" else model },
							)
						}
						if (priceModel == model) {
							val price = provider.pricingFor(model)
							var inText by remember(model) { mutableStateOf(price.inputPricePerM.toString()) }
							var outText by remember(model) { mutableStateOf(price.outputPricePerM.toString()) }
							Column(
								modifier = Modifier.fillMaxWidth(),
								verticalArrangement = Arrangement.spacedBy(6.dp),
							) {
								Text(
									text = "قیمت این مدل (دلار به ازای ۱ میلیون توکن)",
									style = MaterialTheme.typography.labelSmall,
									color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
								)
								Row(verticalAlignment = Alignment.CenterVertically) {
									Box(modifier = Modifier.weight(1f)) {
										NeoTextField(
											value = inText,
											onValueChange = { inText = it },
											placeholder = "ورودی",
											modifier = Modifier.fillMaxWidth(),
										)
									}
									Spacer(Modifier.width(6.dp))
									Box(modifier = Modifier.weight(1f)) {
										NeoTextField(
											value = outText,
											onValueChange = { outText = it },
											placeholder = "خروجی",
											modifier = Modifier.fillMaxWidth(),
										)
									}
									Spacer(Modifier.width(6.dp))
									NeoIconButton(
										icon = Icons.Filled.Check,
										boxSize = 40.dp,
										contentDescription = "ذخیره قیمت",
										onClick = {
											onSetModelPrice(
												model,
												inText.trim().toDoubleOrNull() ?: 0.0,
												outText.trim().toDoubleOrNull() ?: 0.0,
											)
											priceModel = ""
										},
									)
								}
							}
						}
					}
					if (filtered.isEmpty()) {
						Text(
							"مدلی پیدا نشد",
							style = MaterialTheme.typography.bodySmall,
							color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
						)
					}
				}
			}
		}
	}
}
