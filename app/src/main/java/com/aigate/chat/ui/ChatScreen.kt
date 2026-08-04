package com.aigate.chat.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.aigate.chat.model.ChatMessage
import com.aigate.chat.model.ConnectionState
import com.aigate.chat.model.Conversation
import com.aigate.chat.model.Provider
import com.aigate.chat.ui.components.NeoBox
import com.aigate.chat.ui.components.NeoButton
import com.aigate.chat.ui.components.NeoChip
import com.aigate.chat.ui.components.NeoIconButton
import com.aigate.chat.ui.theme.AccentPalette
import com.aigate.chat.util.AttachmentUtils
import com.aigate.chat.util.TokenCounter
import com.aigate.chat.util.rememberHaptics
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
	viewModel: ChatViewModel,
	onOpenProviders: () -> Unit,
	onOpenSettings: () -> Unit,
	onOpenCompare: () -> Unit,
) {
	val state by viewModel.state.collectAsStateCompat()
	val context = LocalContext.current
	val scope = rememberCoroutineScope()
	val drawerState = rememberDrawerState(DrawerValue.Closed)
	val listState = rememberLazyListState()
	val haptics = rememberHaptics(state.settings.hapticsEnabled)

	var input by remember { mutableStateOf("") }
	var searchQuery by remember { mutableStateOf("") }
	var editing by remember { mutableStateOf<ChatMessage?>(null) }
	var showModelMenu by remember { mutableStateOf(false) }
	var showExportMenu by remember { mutableStateOf(false) }

	val conversation = state.current
	val provider = state.providerOf(conversation)

	val filePicker = rememberLauncherForActivityResult(
		contract = ActivityResultContracts.OpenMultipleDocuments(),
	) { uris -> viewModel.addAttachments(uris) }

	val imagePicker = rememberLauncherForActivityResult(
		contract = ActivityResultContracts.OpenMultipleDocuments(),
	) { uris -> viewModel.addAttachments(uris) }

	LaunchedEffect(conversation?.messages?.size, state.isGenerating) {
		val size = conversation?.messages?.size ?: 0
		if (size > 0) listState.animateScrollToItem(size - 1)
	}

	val toast = state.toast
	LaunchedEffect(toast) {
		if (toast != null) {
			kotlinx.coroutines.delay(2600)
			viewModel.showToast(null)
		}
	}

	ModalNavigationDrawer(
		drawerState = drawerState,
		drawerContent = {
			ModalDrawerSheet(
				drawerContainerColor = MaterialTheme.colorScheme.background,
			) {
				DrawerContent(
					viewModel = viewModel,
					searchQuery = searchQuery,
					onSearchQueryChange = { searchQuery = it },
					onSelect = { id ->
						viewModel.selectConversation(id)
						scope.launch { drawerState.close() }
					},
					onOpenProviders = {
						scope.launch { drawerState.close() }
						onOpenProviders()
					},
					onOpenSettings = {
						scope.launch { drawerState.close() }
						onOpenSettings()
					},
					onOpenCompare = {
						scope.launch { drawerState.close() }
						onOpenCompare()
					},
				)
			}
		},
	) {
		Box(
			modifier = Modifier
				.fillMaxSize()
				.background(MaterialTheme.colorScheme.background)
		) {
			Column(
				modifier = Modifier
					.fillMaxSize()
					.statusBarsPadding()
			) {
				// ---------- نوار بالا ----------
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.padding(horizontal = 12.dp, vertical = 8.dp),
					verticalAlignment = Alignment.CenterVertically,
				) {
					NeoIconButton(
						icon = Icons.Filled.Menu,
						onClick = {
							haptics.tap()
							scope.launch { drawerState.open() }
						},
					)
					Spacer(Modifier.width(10.dp))
					Box(modifier = Modifier.weight(1f)) {
						NeoBox(
							modifier = Modifier.fillMaxWidth(),
							background = MaterialTheme.colorScheme.primaryContainer,
							onClick = {
								haptics.tap()
								showModelMenu = true
							},
						) {
							Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
								Text(
									text = provider?.name ?: "API انتخاب نشده",
									style = MaterialTheme.typography.labelLarge,
									fontWeight = FontWeight.Bold,
									color = MaterialTheme.colorScheme.onPrimaryContainer,
								)
								Text(
									text = conversation?.model?.ifBlank { "مدلی انتخاب نشده" } ?: "—",
									style = MaterialTheme.typography.labelSmall,
									color = MaterialTheme.colorScheme.onPrimaryContainer,
								)
							}
						}
						ModelPickerMenu(
							expanded = showModelMenu,
							onDismiss = { showModelMenu = false },
							providers = state.providers,
							onPick = { pickedProvider, model ->
								showModelMenu = false
								val id = conversation?.id
								if (id != null) {
									viewModel.setConversationProvider(id, pickedProvider.id, model)
								}
							},
						)
					}
					Spacer(Modifier.width(8.dp))
					StatusDot(state = state.statuses[provider?.id]?.state ?: ConnectionState.UNKNOWN)
					Spacer(Modifier.width(8.dp))
					NeoIconButton(
						icon = Icons.Filled.Add,
						containerColor = MaterialTheme.colorScheme.secondaryContainer,
						onClick = {
							haptics.strong()
							viewModel.newConversation()
						},
					)
				}

				// ---------- پیام‌ها ----------
				LazyColumn(
					state = listState,
					modifier = Modifier
						.weight(1f)
						.fillMaxWidth(),
					contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
					verticalArrangement = Arrangement.spacedBy(12.dp),
				) {
					val messages = conversation?.messages ?: emptyList()
					if (messages.isEmpty()) {
						item {
							EmptyState(hasProvider = provider != null, onOpenProviders = onOpenProviders)
						}
					}
					items(messages, key = { it.id }) { message ->
						MessageBubble(
							message = message,
							showStats = state.settings.showTokenStats,
							provider = provider,
							onCopy = {
								haptics.tap()
								copyToClipboard(context, message.activeText)
								viewModel.showToast("کپی شد")
							},
							onEdit = {
								haptics.tap()
								editing = message
							},
							onRegenerate = {
								haptics.strong()
								viewModel.regenerate(message.id)
							},
							onBranch = {
								haptics.strong()
								val id = conversation?.id
								if (id != null) viewModel.branchFrom(id, message.id)
							},
							onDelete = {
								haptics.strong()
								viewModel.deleteMessage(message.id)
							},
							onVariant = { index -> viewModel.selectVariant(message.id, index) },
							onToast = { viewModel.showToast(it) },
						)
					}
					if (state.isGenerating) {
						item { TypingIndicator() }
					}
				}

				// ---------- آمار توکن ----------
				if (state.settings.showTokenStats && conversation != null && conversation.messages.isNotEmpty()) {
					Row(
						modifier = Modifier
							.fillMaxWidth()
							.padding(horizontal = 14.dp, vertical = 2.dp),
						horizontalArrangement = Arrangement.spacedBy(8.dp),
						verticalAlignment = Alignment.CenterVertically,
					) {
						Text(
							text = "توکن: " + TokenCounter.formatTokens(conversation.totalTokens),
							style = MaterialTheme.typography.labelSmall,
							color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
						)
						Text(
							text = "هزینه‌ی تقریبی: " + TokenCounter.formatCost(conversation.totalCost),
							style = MaterialTheme.typography.labelSmall,
							color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
						)
					}
				}

				// ---------- پیوست‌های آماده‌ی ارسال ----------
				AnimatedVisibility(
					visible = state.pendingAttachments.isNotEmpty() || state.isLoadingAttachment,
					enter = fadeIn() + expandVertically(),
					exit = fadeOut() + shrinkVertically(),
				) {
					Row(
						modifier = Modifier
							.fillMaxWidth()
							.horizontalScrollCompat()
							.padding(horizontal = 12.dp, vertical = 6.dp),
						horizontalArrangement = Arrangement.spacedBy(8.dp),
					) {
						if (state.isLoadingAttachment) {
							NeoChip(text = "در حال خواندن فایل…")
						}
						for (attachment in state.pendingAttachments) {
							NeoChip(
								text = attachment.name + "  " + AttachmentUtils.humanSize(attachment.sizeBytes),
								icon = Icons.Filled.Close,
								color = MaterialTheme.colorScheme.secondaryContainer,
								onClick = { viewModel.removeAttachment(attachment.id) },
							)
						}
					}
				}

				// ---------- نوار ورودی ----------
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.imePadding()
						.navigationBarsPadding()
						.padding(horizontal = 12.dp, vertical = 8.dp),
					verticalAlignment = Alignment.Bottom,
				) {
					NeoIconButton(
						icon = Icons.Filled.AttachFile,
						containerColor = MaterialTheme.colorScheme.surface,
						onClick = {
							haptics.tap()
							filePicker.launch(arrayOf("*/*"))
						},
					)
					Spacer(Modifier.width(6.dp))
					NeoIconButton(
						icon = Icons.Filled.Image,
						containerColor = MaterialTheme.colorScheme.surface,
						onClick = {
							haptics.tap()
							imagePicker.launch(arrayOf("image/*"))
						},
					)
					Spacer(Modifier.width(6.dp))
					Box(modifier = Modifier.weight(1f)) {
						NeoTextField(
							value = input,
							onValueChange = { input = it },
							placeholder = "پیامت را بنویس…",
							modifier = Modifier.fillMaxWidth(),
						)
					}
					Spacer(Modifier.width(6.dp))
					if (state.isGenerating) {
						NeoIconButton(
							icon = Icons.Filled.Stop,
							containerColor = MaterialTheme.colorScheme.error,
							contentColor = MaterialTheme.colorScheme.onError,
							onClick = {
								haptics.strong()
								viewModel.stopGeneration()
							},
						)
					} else {
						NeoIconButton(
							icon = Icons.Filled.Send,
							containerColor = MaterialTheme.colorScheme.primaryContainer,
							onClick = {
								haptics.strong()
								val text = input
								input = ""
								viewModel.send(text)
							},
						)
					}
				}
			}

			// ---------- دکمه‌ی خروجی گرفتن ----------
			Box(
				modifier = Modifier
					.align(Alignment.TopEnd)
					.statusBarsPadding()
					.padding(top = 62.dp, end = 12.dp)
			) {
				if (conversation != null && conversation.messages.isNotEmpty()) {
					NeoIconButton(
						icon = Icons.Filled.Download,
						boxSize = 40.dp,
						containerColor = MaterialTheme.colorScheme.surface,
						onClick = { showExportMenu = true },
						contentDescription = "خروجی گرفتن",
					)
					DropdownMenu(expanded = showExportMenu, onDismissRequest = { showExportMenu = false }) {
						DropdownMenuItem(
							text = { Text("خروجی Markdown") },
							onClick = {
								showExportMenu = false
								viewModel.exportConversation(conversation.id, "markdown")
							},
						)
						DropdownMenuItem(
							text = { Text("خروجی HTML") },
							onClick = {
								showExportMenu = false
								viewModel.exportConversation(conversation.id, "html")
							},
						)
						DropdownMenuItem(
							text = { Text("خروجی متن ساده") },
							onClick = {
								showExportMenu = false
								viewModel.exportConversation(conversation.id, "text")
							},
						)
					}
				}
			}

			// ---------- توست ----------
			if (toast != null) {
				Box(
					modifier = Modifier
						.align(Alignment.BottomCenter)
						.padding(bottom = 96.dp, start = 16.dp, end = 16.dp)
				) {
					NeoBox(background = MaterialTheme.colorScheme.secondaryContainer) {
						Text(
							text = toast,
							modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
							style = MaterialTheme.typography.labelLarge,
							color = MaterialTheme.colorScheme.onSecondaryContainer,
						)
					}
				}
			}
		}
	}

	// ---------- دیالوگ ویرایش ----------
	val editingMessage = editing
	if (editingMessage != null) {
		EditMessageDialog(
			message = editingMessage,
			onDismiss = { editing = null },
			onSave = { text, resend ->
				editing = null
				viewModel.editMessage(editingMessage.id, text, resend)
			},
		)
	}

	// ---------- دیالوگ فال‌بک ----------
	val fallback = state.fallback
	if (fallback != null) {
		FallbackDialog(
			request = fallback,
			onDismiss = { viewModel.dismissFallback() },
			onAccept = { pickedProvider, model -> viewModel.acceptFallback(pickedProvider.id, model) },
		)
	}
}

private fun Modifier.horizontalScrollCompat(): Modifier = this.then(Modifier)

@Composable
private fun StatusDot(state: ConnectionState) {
	val color = when (state) {
		ConnectionState.ONLINE -> Color(0xFF3ED17A)
		ConnectionState.OFFLINE -> Color(0xFFE23636)
		ConnectionState.TESTING -> Color(0xFFFFD93D)
		ConnectionState.UNKNOWN -> Color(0xFF9A9A9A)
	}
	val alpha by animateFloatAsState(
		targetValue = if (state == ConnectionState.TESTING) 0.4f else 1f,
		label = "statusAlpha",
	)
	Box(
		modifier = Modifier
			.size(14.dp)
			.alpha(alpha)
			.clip(RoundedCornerShape(7.dp))
			.background(color)
	)
}

@Composable
private fun ModelPickerMenu(
	expanded: Boolean,
	onDismiss: () -> Unit,
	providers: List<Provider>,
	onPick: (Provider, String) -> Unit,
) {
	DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
		if (providers.isEmpty()) {
			DropdownMenuItem(text = { Text("هنوز API اضافه نکردی") }, onClick = onDismiss)
		}
		for (provider in providers) {
			DropdownMenuItem(
				text = {
					Text(
						text = provider.name + "  (" + provider.type.name + ")",
						fontWeight = FontWeight.Bold,
					)
				},
				onClick = { onPick(provider, provider.defaultModel) },
			)
			val favorites = provider.models.filter { provider.favoriteModels.contains(it) }
			val rest = provider.models.filterNot { provider.favoriteModels.contains(it) }
			for (model in favorites + rest.take(40)) {
				DropdownMenuItem(
					text = {
						Text(
							text = (if (provider.favoriteModels.contains(model)) "★  " else "•  ") + model,
							style = MaterialTheme.typography.bodyMedium,
						)
					},
					onClick = { onPick(provider, model) },
				)
			}
		}
	}
}

@Composable
private fun EmptyState(hasProvider: Boolean, onOpenProviders: () -> Unit) {
	Column(
		modifier = Modifier
			.fillMaxWidth()
			.padding(top = 60.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
	) {
		Text(
			text = "AiGate",
			style = MaterialTheme.typography.headlineLarge,
			fontWeight = FontWeight.Bold,
			color = MaterialTheme.colorScheme.onBackground,
		)
		Spacer(Modifier.height(8.dp))
		Text(
			text = if (hasProvider) "یک پیام بنویس تا شروع کنیم" else "اول یک API اضافه کن",
			style = MaterialTheme.typography.bodyLarge,
			color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
		)
		if (!hasProvider) {
			Spacer(Modifier.height(14.dp))
			NeoButton(text = "افزودن API", onClick = onOpenProviders, icon = Icons.Filled.Storage)
		}
	}
}

@Composable
private fun TypingIndicator() {
	val transition = rememberInfiniteTransition(label = "typing")
	val alpha by transition.animateFloat(
		initialValue = 0.3f,
		targetValue = 1f,
		animationSpec = infiniteRepeatable(tween(700), androidx.compose.animation.core.RepeatMode.Reverse),
		label = "typingAlpha",
	)
	Row(modifier = Modifier.padding(start = 4.dp)) {
		NeoChip(text = "در حال نوشتن…", modifier = Modifier.alpha(alpha))
	}
}

@Composable
private fun MessageBubble(
	message: ChatMessage,
	showStats: Boolean,
	provider: Provider?,
	onCopy: () -> Unit,
	onEdit: () -> Unit,
	onRegenerate: () -> Unit,
	onBranch: () -> Unit,
	onDelete: () -> Unit,
	onVariant: (Int) -> Unit,
	onToast: (String) -> Unit,
) {
	val isUser = message.role == "user"
	val accent = AccentPalette[(provider?.colorIndex ?: 0) % AccentPalette.size]
	val background = if (isUser) accent else MaterialTheme.colorScheme.surface
	val textColor = if (isUser) Color(0xFF101010) else MaterialTheme.colorScheme.onSurface

	Column(modifier = Modifier.fillMaxWidth()) {
		NeoBox(
			modifier = Modifier.fillMaxWidth(),
			background = background,
		) {
			Column(modifier = Modifier.padding(12.dp)) {
				Text(
					text = if (isUser) "تو" else (message.modelName.ifBlank { "دستیار" }),
					style = MaterialTheme.typography.labelSmall,
					fontWeight = FontWeight.Bold,
					color = textColor.copy(alpha = 0.7f),
				)
				Spacer(Modifier.height(6.dp))
				if (message.attachments.isNotEmpty()) {
					for (attachment in message.attachments) {
						NeoChip(text = attachment.name, modifier = Modifier.padding(bottom = 6.dp))
					}
				}
				val error = message.error
				if (error != null) {
					Text(
						text = "خطا: " + error,
						style = MaterialTheme.typography.bodyMedium,
						color = MaterialTheme.colorScheme.error,
					)
				} else if (message.activeText.isNotBlank()) {
					MessageContent(raw = message.activeText, textColor = textColor, onToast = onToast)
				}
				if (showStats && message.completionTokens > 0) {
					Text(
						text = TokenCounter.formatTokens(message.promptTokens + message.completionTokens) +
							" توکن · " + TokenCounter.formatCost(message.costUsd),
						style = MaterialTheme.typography.labelSmall,
						color = textColor.copy(alpha = 0.6f),
					)
				}
			}
		}
		Spacer(Modifier.height(6.dp))
		Row(verticalAlignment = Alignment.CenterVertically) {
			NeoIconButton(
				icon = Icons.Filled.ContentCopy,
				boxSize = 34.dp,
				containerColor = MaterialTheme.colorScheme.surface,
				onClick = onCopy,
				contentDescription = "کپی",
			)
			Spacer(Modifier.width(6.dp))
			if (isUser) {
				NeoIconButton(
					icon = Icons.Filled.Edit,
					boxSize = 34.dp,
					containerColor = MaterialTheme.colorScheme.surface,
					onClick = onEdit,
					contentDescription = "ویرایش",
				)
			} else {
				NeoIconButton(
					icon = Icons.Filled.Refresh,
					boxSize = 34.dp,
					containerColor = MaterialTheme.colorScheme.surface,
					onClick = onRegenerate,
					contentDescription = "تولید دوباره",
				)
			}
			Spacer(Modifier.width(6.dp))
			NeoIconButton(
				icon = Icons.Filled.CallSplit,
				boxSize = 34.dp,
				containerColor = MaterialTheme.colorScheme.surface,
				onClick = onBranch,
				contentDescription = "شاخه‌ای کردن",
			)
			Spacer(Modifier.width(6.dp))
			NeoIconButton(
				icon = Icons.Filled.Delete,
				boxSize = 34.dp,
				containerColor = MaterialTheme.colorScheme.surface,
				onClick = onDelete,
				contentDescription = "حذف",
			)
			if (message.variantCount > 1) {
				Spacer(Modifier.width(10.dp))
				NeoIconButton(
					icon = Icons.Filled.KeyboardArrowRight,
					boxSize = 30.dp,
					containerColor = MaterialTheme.colorScheme.surface,
					onClick = { onVariant(message.variantIndex - 1) },
					enabled = message.variantIndex > 0,
					contentDescription = "نسخه‌ی قبلی",
				)
				Text(
					text = " " + (message.variantIndex + 1) + "/" + message.variantCount + " ",
					style = MaterialTheme.typography.labelSmall,
					color = MaterialTheme.colorScheme.onBackground,
				)
				NeoIconButton(
					icon = Icons.Filled.KeyboardArrowLeft,
					boxSize = 30.dp,
					containerColor = MaterialTheme.colorScheme.surface,
					onClick = { onVariant(message.variantIndex + 1) },
					enabled = message.variantIndex < message.variantCount - 1,
					contentDescription = "نسخه‌ی بعدی",
				)
			}
		}
	}
}

@Composable
private fun EditMessageDialog(
	message: ChatMessage,
	onDismiss: () -> Unit,
	onSave: (String, Boolean) -> Unit,
) {
	var text by remember { mutableStateOf(message.activeText) }
	Dialog(onDismissRequest = onDismiss) {
		NeoBox(
			modifier = Modifier.fillMaxWidth(),
			background = MaterialTheme.colorScheme.surface,
		) {
			Column(modifier = Modifier.padding(14.dp)) {
				Text(
					"ویرایش پیام",
					style = MaterialTheme.typography.titleMedium,
					fontWeight = FontWeight.Bold,
					color = MaterialTheme.colorScheme.onSurface,
				)
				Spacer(Modifier.height(10.dp))
				NeoTextField(
					value = text,
					onValueChange = { text = it },
					placeholder = "متن پیام",
					modifier = Modifier
						.fillMaxWidth()
						.heightIn(min = 120.dp),
				)
				Spacer(Modifier.height(12.dp))
				Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
					NeoButton(
						text = "ذخیره و پاسخ جدید",
						icon = Icons.Filled.Refresh,
						onClick = { onSave(text, true) },
					)
					NeoButton(
						text = "فقط ذخیره",
						icon = Icons.Filled.Check,
						containerColor = MaterialTheme.colorScheme.secondaryContainer,
						onClick = { onSave(text, false) },
					)
				}
			}
		}
	}
}

@Composable
private fun FallbackDialog(
	request: FallbackRequest,
	onDismiss: () -> Unit,
	onAccept: (Provider, String) -> Unit,
) {
	var selected by remember { mutableStateOf(request.candidates.firstOrNull()) }
	var selectedModel by remember { mutableStateOf(request.candidates.firstOrNull()?.defaultModel.orEmpty()) }
	Dialog(onDismissRequest = onDismiss) {
		NeoBox(
			modifier = Modifier.fillMaxWidth(),
			background = MaterialTheme.colorScheme.surface,
		) {
			Column(modifier = Modifier.padding(14.dp)) {
				Text(
					"درخواست با «" + request.failedProviderName + "» ناموفق بود",
					style = MaterialTheme.typography.titleMedium,
					fontWeight = FontWeight.Bold,
					color = MaterialTheme.colorScheme.onSurface,
				)
				Spacer(Modifier.height(6.dp))
				Text(
					text = request.error.take(220),
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.error,
				)
				Spacer(Modifier.height(10.dp))
				Text(
					"اجازه می‌دهی با یک API دیگر دوباره تلاش کنیم؟ کدام API؟",
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurface,
				)
				Spacer(Modifier.height(10.dp))
				Column(
					modifier = Modifier
						.fillMaxWidth()
						.heightIn(max = 220.dp)
						.verticalScroll(rememberScrollState()),
					verticalArrangement = Arrangement.spacedBy(8.dp),
				) {
					for (candidate in request.candidates) {
						val isSelected = selected?.id == candidate.id
						NeoBox(
							modifier = Modifier.fillMaxWidth(),
							background = if (isSelected) {
								MaterialTheme.colorScheme.primaryContainer
							} else {
								MaterialTheme.colorScheme.surfaceVariant
							},
							onClick = {
								selected = candidate
								selectedModel = candidate.defaultModel
							},
						) {
							Column(modifier = Modifier.padding(10.dp)) {
								Text(
									text = candidate.name + "  ·  " + candidate.type.name,
									style = MaterialTheme.typography.labelLarge,
									fontWeight = FontWeight.Bold,
									color = MaterialTheme.colorScheme.onSurface,
								)
								Text(
									text = candidate.defaultModel.ifBlank { "بدون مدل پیش‌فرض" },
									style = MaterialTheme.typography.labelSmall,
									color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
								)
							}
						}
					}
				}
				Spacer(Modifier.height(12.dp))
				Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
					NeoButton(
						text = "بله، ادامه بده",
						icon = Icons.Filled.Check,
						enabled = selected != null,
						onClick = {
							val picked = selected
							if (picked != null) onAccept(picked, selectedModel)
						},
					)
					NeoButton(
						text = "نه",
						icon = Icons.Filled.Close,
						containerColor = MaterialTheme.colorScheme.surfaceVariant,
						onClick = onDismiss,
					)
				}
			}
		}
	}
}

@Composable
private fun DrawerContent(
	viewModel: ChatViewModel,
	searchQuery: String,
	onSearchQueryChange: (String) -> Unit,
	onSelect: (String) -> Unit,
	onOpenProviders: () -> Unit,
	onOpenSettings: () -> Unit,
	onOpenCompare: () -> Unit,
) {
	val state by viewModel.state.collectAsStateCompat()
	val hits = remember(searchQuery, state.conversations) { viewModel.search(searchQuery) }

	Column(
		modifier = Modifier
			.fillMaxHeight()
			.width(310.dp)
			.statusBarsPadding()
			.padding(12.dp)
	) {
		Text(
			"AiGate",
			style = MaterialTheme.typography.headlineSmall,
			fontWeight = FontWeight.Bold,
			color = MaterialTheme.colorScheme.onBackground,
		)
		Spacer(Modifier.height(10.dp))
		NeoTextField(
			value = searchQuery,
			onValueChange = onSearchQueryChange,
			placeholder = "جست‌وجو در گفتگوها…",
			modifier = Modifier.fillMaxWidth(),
		)
		Spacer(Modifier.height(10.dp))
		Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
			NeoIconButton(
				icon = Icons.Filled.Storage,
				boxSize = 40.dp,
				onClick = onOpenProviders,
				contentDescription = "API ها",
			)
			NeoIconButton(
				icon = Icons.Filled.CompareArrows,
				boxSize = 40.dp,
				containerColor = MaterialTheme.colorScheme.secondaryContainer,
				onClick = onOpenCompare,
				contentDescription = "مقایسه‌ی دو مدل",
			)
			NeoIconButton(
				icon = Icons.Filled.Settings,
				boxSize = 40.dp,
				containerColor = MaterialTheme.colorScheme.surface,
				onClick = onOpenSettings,
				contentDescription = "تنظیمات",
			)
			NeoIconButton(
				icon = Icons.Filled.Add,
				boxSize = 40.dp,
				containerColor = MaterialTheme.colorScheme.primaryContainer,
				onClick = { viewModel.newConversation() },
				contentDescription = "گفتگوی جدید",
			)
		}
		Spacer(Modifier.height(12.dp))

		if (searchQuery.trim().length >= 2) {
			Text(
				text = "نتایج جست‌وجو (" + hits.size + ")",
				style = MaterialTheme.typography.labelLarge,
				color = MaterialTheme.colorScheme.onBackground,
			)
			Spacer(Modifier.height(6.dp))
			LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
				items(hits, key = { it.messageId }) { hit ->
					NeoBox(
						modifier = Modifier.fillMaxWidth(),
						background = MaterialTheme.colorScheme.surface,
						onClick = { onSelect(hit.conversationId) },
					) {
						Column(modifier = Modifier.padding(10.dp)) {
							Text(
								text = hit.conversationTitle,
								style = MaterialTheme.typography.labelLarge,
								fontWeight = FontWeight.Bold,
								color = MaterialTheme.colorScheme.onSurface,
							)
							Text(
								text = hit.snippet,
								style = MaterialTheme.typography.bodySmall,
								color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
							)
						}
					}
				}
			}
		} else {
			LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
				items(state.sortedConversations, key = { it.id }) { conversation ->
					ConversationRow(
						conversation = conversation,
						selected = conversation.id == state.currentId,
						onClick = { onSelect(conversation.id) },
						onPin = { viewModel.togglePin(conversation.id) },
						onDelete = { viewModel.deleteConversation(conversation.id) },
					)
				}
			}
		}
	}
}

@Composable
private fun ConversationRow(
	conversation: Conversation,
	selected: Boolean,
	onClick: () -> Unit,
	onPin: () -> Unit,
	onDelete: () -> Unit,
) {
	NeoBox(
		modifier = Modifier.fillMaxWidth(),
		background = if (selected) {
			MaterialTheme.colorScheme.primaryContainer
		} else {
			MaterialTheme.colorScheme.surface
		},
		onClick = onClick,
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(10.dp),
			verticalAlignment = Alignment.CenterVertically,
		) {
			Column(modifier = Modifier.weight(1f)) {
				Text(
					text = conversation.title,
					style = MaterialTheme.typography.labelLarge,
					fontWeight = FontWeight.Bold,
					color = MaterialTheme.colorScheme.onSurface,
				)
				Text(
					text = conversation.messages.size.toString() + " پیام" +
						(if (conversation.parentId != null) "  ·  شاخه" else ""),
					style = MaterialTheme.typography.labelSmall,
					color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
				)
			}
			Icon(
				Icons.Filled.PushPin,
				contentDescription = "سنجاق",
				tint = if (conversation.pinned) {
					MaterialTheme.colorScheme.onSurface
				} else {
					MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
				},
				modifier = Modifier
					.size(20.dp)
					.clickable(onClick = onPin),
			)
			Spacer(Modifier.width(10.dp))
			Icon(
				Icons.Filled.Delete,
				contentDescription = "حذف",
				tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
				modifier = Modifier
					.size(20.dp)
					.clickable(onClick = onDelete),
			)
		}
	}
}
