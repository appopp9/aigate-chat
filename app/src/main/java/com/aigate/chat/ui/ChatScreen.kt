package com.aigate.chat.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.aigate.chat.model.ChatMessage
import com.aigate.chat.model.ConnectionState
import com.aigate.chat.model.Conversation
import com.aigate.chat.model.FontScale
import com.aigate.chat.model.Persona
import com.aigate.chat.model.Provider
import com.aigate.chat.model.ProviderStatus
import com.aigate.chat.ui.components.NeoBox
import com.aigate.chat.ui.components.NeoButton
import com.aigate.chat.ui.components.NeoChip
import com.aigate.chat.ui.components.NeoIconButton
import com.aigate.chat.ui.theme.AccentPalette
import com.aigate.chat.util.AttachmentUtils
import com.aigate.chat.util.TokenCounter
import com.aigate.chat.util.rememberHaptics
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(
	viewModel: ChatViewModel,
	onOpenProviders: () -> Unit,
	onOpenSettings: () -> Unit,
	onOpenCompare: () -> Unit,
	onOpenSearch: () -> Unit = {},
	onOpenPrompts: () -> Unit = {},
	onOpenPersonas: () -> Unit = {},
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
	var showOverflow by remember { mutableStateOf(false) }
	var showExportMenu by remember { mutableStateOf(false) }
	var showStatusSheet by remember { mutableStateOf(false) }
	var showPromptSheet by remember { mutableStateOf(false) }
	var showPersonaSheet by remember { mutableStateOf(false) }
	var selectedMessageId by remember { mutableStateOf("") }

	val conversation = state.current
	val provider = state.providerOf(conversation)
	val persona = state.personaOf(conversation)
	val accent = AccentPalette[(provider?.colorIndex ?: 0) % AccentPalette.size]

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
			ModalDrawerSheet(drawerContainerColor = MaterialTheme.colorScheme.background) {
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
					onOpenSearch = {
						scope.launch { drawerState.close() }
						onOpenSearch()
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
				// ---------- navare bala ----------
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.padding(horizontal = 12.dp, vertical = 8.dp),
					verticalAlignment = Alignment.CenterVertically,
				) {
					NeoIconButton(
						icon = Icons.Filled.Menu,
						boxSize = 48.dp,
						contentDescription = "menu",
						onClick = {
							haptics.tap()
							scope.launch { drawerState.open() }
						},
					)
					Spacer(Modifier.width(10.dp))
					Column(
						modifier = Modifier.weight(1f),
						horizontalAlignment = Alignment.CenterHorizontally,
					) {
						Text(
							text = conversation?.title.orEmpty().ifBlank { "AiGate" },
							style = MaterialTheme.typography.titleMedium,
							fontWeight = FontWeight.Bold,
							maxLines = 1,
							textAlign = TextAlign.Center,
							color = MaterialTheme.colorScheme.onBackground,
						)
						Spacer(Modifier.height(4.dp))
						Box {
							Row(verticalAlignment = Alignment.CenterVertically) {
								StatusDot(
									state = state.statuses[provider?.id]?.state ?: ConnectionState.UNKNOWN,
									accent = accent,
									onClick = {
										haptics.tap()
										showStatusSheet = true
									},
								)
								Spacer(Modifier.width(6.dp))
								NeoChip(
									text = conversation?.model?.ifBlank { "model entekhab kon" }
										?: "model entekhab kon",
									icon = Icons.Filled.KeyboardArrowDown,
									color = accent,
									onClick = {
										haptics.tap()
										showModelMenu = true
									},
								)
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
					}
					Spacer(Modifier.width(8.dp))
					Box {
						NeoIconButton(
							icon = Icons.Filled.MoreVert,
							boxSize = 48.dp,
							contentDescription = "more",
							onClick = {
								haptics.tap()
								showOverflow = true
							},
						)
						DropdownMenu(expanded = showOverflow, onDismissRequest = { showOverflow = false }) {
							DropdownMenuItem(
								text = { Text("goftogooye jadid") },
								onClick = {
									showOverflow = false
									viewModel.newConversation()
								},
							)
							DropdownMenuItem(
								text = { Text("jost-o-jooye sartasari") },
								onClick = {
									showOverflow = false
									onOpenSearch()
								},
							)
							DropdownMenuItem(
								text = { Text("prompt haye amade") },
								onClick = {
									showOverflow = false
									onOpenPrompts()
								},
							)
							DropdownMenuItem(
								text = { Text("naghsh ha") },
								onClick = {
									showOverflow = false
									onOpenPersonas()
								},
							)
							DropdownMenuItem(
								text = { Text("moghayeseye do model") },
								onClick = {
									showOverflow = false
									onOpenCompare()
								},
							)
							DropdownMenuItem(
								text = { Text("khorooji gereftan") },
								onClick = {
									showOverflow = false
									showExportMenu = true
								},
							)
							DropdownMenuItem(
								text = { Text("tanzimat") },
								onClick = {
									showOverflow = false
									onOpenSettings()
								},
							)
						}
						DropdownMenu(expanded = showExportMenu, onDismissRequest = { showExportMenu = false }) {
							DropdownMenuItem(
								text = { Text("Markdown") },
								onClick = {
									showExportMenu = false
									val id = conversation?.id
									if (id != null) viewModel.exportConversation(id, "markdown")
								},
							)
							DropdownMenuItem(
								text = { Text("HTML") },
								onClick = {
									showExportMenu = false
									val id = conversation?.id
									if (id != null) viewModel.exportConversation(id, "html")
								},
							)
							DropdownMenuItem(
								text = { Text("matne sade") },
								onClick = {
									showExportMenu = false
									val id = conversation?.id
									if (id != null) viewModel.exportConversation(id, "text")
								},
							)
						}
					}
				}

				// ---------- navare naghsh va payam haye pin shode ----------
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.horizontalScroll(rememberScrollState())
						.padding(horizontal = 12.dp),
					horizontalArrangement = Arrangement.spacedBy(6.dp),
					verticalAlignment = Alignment.CenterVertically,
				) {
					NeoChip(
						text = if (persona == null) "bedoone naghsh" else persona.emoji + " " + persona.name,
						icon = Icons.Filled.Face,
						color = MaterialTheme.colorScheme.secondaryContainer,
						onClick = {
							haptics.tap()
							showPersonaSheet = true
						},
					)
					val pinned = conversation?.messages?.filter { it.pinned } ?: emptyList()
					for (message in pinned) {
						NeoChip(
							text = message.activeText.take(28).replace('\n', ' '),
							icon = Icons.Filled.PushPin,
							color = MaterialTheme.colorScheme.surface,
							onClick = { viewModel.toggleMessagePin(message.id) },
						)
					}
				}

				// ---------- payam ha ----------
				LazyColumn(
					state = listState,
					modifier = Modifier
						.weight(1f)
						.fillMaxWidth(),
					contentPadding = PaddingValues(12.dp),
					verticalArrangement = Arrangement.spacedBy(10.dp),
				) {
					val messages = conversation?.messages ?: emptyList()
					if (messages.isEmpty()) {
						item {
							EmptyState(
								hasProvider = provider != null,
								modelName = conversation?.model.orEmpty(),
								accent = accent,
								suggestions = state.prompts.take(3).map { it.title to it.body },
								onSuggestion = { body -> input = body },
								onOpenProviders = onOpenProviders,
							)
						}
					}
					itemsIndexed(messages, key = { _, item -> item.id }) { index, message ->
						val previous = if (index == 0) null else messages[index - 1]
						if (needsDateSeparator(previous, message)) {
							DateSeparator(message.createdAt)
							Spacer(Modifier.height(10.dp))
						}
						MessageBubble(
							message = message,
							showStats = state.settings.showTokenStats,
							fontScale = state.settings.fontScale,
							accent = accent,
							streaming = state.isGenerating && index == messages.size - 1 &&
								message.role == "assistant",
							expanded = selectedMessageId == message.id,
							onToggleExpand = {
								haptics.tap()
								selectedMessageId = if (selectedMessageId == message.id) "" else message.id
							},
							onCopy = {
								haptics.tap()
								copyToClipboard(context, message.activeText)
								viewModel.showToast("copy shod")
							},
							onEdit = {
								haptics.tap()
								editing = message
							},
							onRegenerate = {
								haptics.strong()
								viewModel.regenerate(message.id)
							},
							onContinue = {
								haptics.strong()
								viewModel.continueMessage(message.id)
							},
							onBranch = {
								haptics.strong()
								val id = conversation?.id
								if (id != null) viewModel.branchFrom(id, message.id)
							},
							onPin = {
								haptics.tap()
								viewModel.toggleMessagePin(message.id)
							},
							onDelete = {
								haptics.strong()
								viewModel.deleteMessage(message.id)
							},
							onRetry = {
								haptics.strong()
								viewModel.regenerate(message.id)
							},
							onChangeModel = { showModelMenu = true },
							onVariant = { variantIndex -> viewModel.selectVariant(message.id, variantIndex) },
							onToast = { viewModel.showToast(it) },
						)
					}
					if (state.isGenerating) {
						item { TypingIndicator() }
					}
				}

				// ---------- peyvast haye amadeye ersal ----------
				AnimatedVisibility(
					visible = state.pendingAttachments.isNotEmpty() || state.isLoadingAttachment,
					enter = fadeIn() + expandVertically(),
					exit = fadeOut() + shrinkVertically(),
				) {
					Row(
						modifier = Modifier
							.fillMaxWidth()
							.horizontalScroll(rememberScrollState())
							.padding(horizontal = 12.dp, vertical = 6.dp),
						horizontalArrangement = Arrangement.spacedBy(8.dp),
					) {
						if (state.isLoadingAttachment) {
							SkeletonChip()
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

				// ---------- navare vorudi ----------
				Column(
					modifier = Modifier
						.fillMaxWidth()
						.imePadding()
						.navigationBarsPadding()
						.padding(horizontal = 12.dp, vertical = 8.dp)
				) {
					val showStats = state.settings.showTokenStats &&
						conversation != null && conversation.totalTokens > 0
					if (showStats && conversation != null) {
						Row(
							modifier = Modifier.padding(bottom = 6.dp),
							horizontalArrangement = Arrangement.spacedBy(6.dp),
						) {
							NeoChip(
								text = TokenCounter.formatTokens(conversation.totalTokens) + " token · " +
									TokenCounter.formatCost(conversation.totalCost),
								color = MaterialTheme.colorScheme.surface,
							)
							val budget = state.settings.monthlyBudgetUsd
							if (budget > 0.0) {
								val percent = ((state.monthCost / budget) * 100).toInt()
								NeoChip(
									text = "budje: " + percent + "%",
									color = if (percent >= state.settings.budgetWarnPercent) {
										MaterialTheme.colorScheme.errorContainer
									} else {
										MaterialTheme.colorScheme.surface
									},
								)
							}
						}
					}
					Row(verticalAlignment = Alignment.Bottom) {
						NeoIconButton(
							icon = Icons.Filled.AttachFile,
							boxSize = 48.dp,
							contentDescription = "file",
							onClick = {
								haptics.tap()
								filePicker.launch(arrayOf("*/*"))
							},
						)
						Spacer(Modifier.width(6.dp))
						NeoIconButton(
							icon = Icons.Filled.Image,
							boxSize = 48.dp,
							contentDescription = "image",
							onClick = {
								haptics.tap()
								imagePicker.launch(arrayOf("image/*"))
							},
						)
						Spacer(Modifier.width(6.dp))
						NeoIconButton(
							icon = Icons.Filled.Lightbulb,
							boxSize = 48.dp,
							containerColor = MaterialTheme.colorScheme.secondaryContainer,
							contentDescription = "prompt ha",
							onClick = {
								haptics.tap()
								showPromptSheet = true
							},
						)
						Spacer(Modifier.width(6.dp))
						Box(modifier = Modifier.weight(1f)) {
							NeoTextField(
								value = input,
								onValueChange = { input = it },
								placeholder = "payamet ra benevis…",
								modifier = Modifier.fillMaxWidth(),
							)
						}
						Spacer(Modifier.width(6.dp))
						if (state.isGenerating) {
							NeoIconButton(
								icon = Icons.Filled.Stop,
								boxSize = 48.dp,
								containerColor = MaterialTheme.colorScheme.error,
								contentColor = MaterialTheme.colorScheme.onError,
								contentDescription = "stop",
								onClick = {
									haptics.strong()
									viewModel.stopGeneration()
								},
							)
						} else {
							NeoIconButton(
								icon = Icons.Filled.Send,
								boxSize = 48.dp,
								containerColor = accent,
								contentDescription = "send",
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
			}

			// ---------- toast ----------
			if (toast != null) {
				Box(
					modifier = Modifier
						.align(Alignment.BottomCenter)
						.padding(bottom = 104.dp, start = 16.dp, end = 16.dp)
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

	// ---------- dialog haye komaki ----------
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

	val fallback = state.fallback
	if (fallback != null) {
		FallbackDialog(
			request = fallback,
			onDismiss = { viewModel.dismissFallback() },
			onAccept = { pickedProvider, model -> viewModel.acceptFallback(pickedProvider.id, model) },
		)
	}

	if (showStatusSheet) {
		StatusDialog(
			providerName = provider?.name.orEmpty(),
			status = state.statuses[provider?.id] ?: ProviderStatus(),
			onRetest = {
				val id = provider?.id
				if (id != null) viewModel.testProvider(id)
			},
			onDismiss = { showStatusSheet = false },
		)
	}

	if (showPromptSheet) {
		PromptPickerDialog(
			prompts = state.prompts.map { it.title to it.body },
			onPick = { body ->
				input = if (input.isBlank()) body else input + "\n" + body
				showPromptSheet = false
			},
			onManage = {
				showPromptSheet = false
				onOpenPrompts()
			},
			onDismiss = { showPromptSheet = false },
		)
	}

	if (showPersonaSheet) {
		PersonaPickerDialog(
			personas = state.personas,
			selectedId = conversation?.personaId.orEmpty(),
			onPick = { id ->
				val conversationId = conversation?.id
				if (conversationId != null) viewModel.applyPersona(conversationId, id)
				showPersonaSheet = false
			},
			onManage = {
				showPersonaSheet = false
				onOpenPersonas()
			},
			onDismiss = { showPersonaSheet = false },
		)
	}
}

// ---------------- helper ha ----------------

private fun needsDateSeparator(previous: ChatMessage?, current: ChatMessage): Boolean {
	if (previous == null) return true
	val format = SimpleDateFormat("yyyyMMdd", Locale.US)
	return format.format(Date(previous.createdAt)) != format.format(Date(current.createdAt))
}

@Composable
private fun DateSeparator(timestamp: Long) {
	val today = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
	val day = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(timestamp))
	val label = if (day == today) {
		"emrooz"
	} else {
		SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date(timestamp))
	}
	Row(
		modifier = Modifier.fillMaxWidth(),
		horizontalArrangement = Arrangement.Center,
		verticalAlignment = Alignment.CenterVertically,
	) {
		Box(
			modifier = Modifier
				.weight(1f)
				.height(3.dp)
				.background(MaterialTheme.colorScheme.outline)
		)
		NeoChip(text = label, modifier = Modifier.padding(horizontal = 8.dp))
		Box(
			modifier = Modifier
				.weight(1f)
				.height(3.dp)
				.background(MaterialTheme.colorScheme.outline)
		)
	}
}

@Composable
private fun StatusDot(state: ConnectionState, accent: Color, onClick: () -> Unit) {
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
			.size(26.dp)
			.clickable(onClick = onClick),
		contentAlignment = Alignment.Center,
	) {
		Box(
			modifier = Modifier
				.size(16.dp)
				.alpha(alpha)
				.clip(RoundedCornerShape(8.dp))
				.background(color)
				.border(2.dp, accent, RoundedCornerShape(8.dp))
		)
	}
}

@Composable
private fun SkeletonChip() {
	val transition = rememberInfiniteTransition(label = "skeleton")
	val alpha by transition.animateFloat(
		initialValue = 0.25f,
		targetValue = 0.7f,
		animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
		label = "skeletonAlpha",
	)
	Box(
		modifier = Modifier
			.width(140.dp)
			.height(34.dp)
			.alpha(alpha)
			.clip(RoundedCornerShape(17.dp))
			.background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f))
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
			DropdownMenuItem(text = { Text("hanooz API ezafe nakardi") }, onClick = onDismiss)
		}
		for (provider in providers) {
			DropdownMenuItem(
				text = {
					Text(text = provider.name + "  (" + provider.type.name + ")", fontWeight = FontWeight.Bold)
				},
				onClick = { onPick(provider, provider.defaultModel) },
			)
			val favorites = provider.models.filter { provider.favoriteModels.contains(it) }
			val rest = provider.models.filterNot { provider.favoriteModels.contains(it) }
			for (model in favorites + rest.take(60)) {
				val pricing = provider.pricingFor(model)
				val priceLabel = if (pricing.inputPricePerM > 0.0 || pricing.outputPricePerM > 0.0) {
					"  ·  $" + pricing.inputPricePerM + "/" + pricing.outputPricePerM
				} else {
					""
				}
				DropdownMenuItem(
					text = {
						Text(
							text = (if (provider.favoriteModels.contains(model)) "★  " else "•  ") +
								model + priceLabel,
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
private fun EmptyState(
	hasProvider: Boolean,
	modelName: String,
	accent: Color,
	suggestions: List<Pair<String, String>>,
	onSuggestion: (String) -> Unit,
	onOpenProviders: () -> Unit,
) {
	Column(
		modifier = Modifier
			.fillMaxWidth()
			.padding(top = 50.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
	) {
		NeoBox(background = accent) {
			Text(
				text = "AiGate",
				modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp),
				style = MaterialTheme.typography.headlineMedium,
				fontWeight = FontWeight.Bold,
				color = Color(0xFF101010),
			)
		}
		Spacer(Modifier.height(12.dp))
		Text(
			text = if (hasProvider) {
				if (modelName.isBlank()) "yek model entekhab kon" else "model: " + modelName
			} else {
				"aval yek API ezafe kon"
			},
			style = MaterialTheme.typography.bodyLarge,
			color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
		)
		if (!hasProvider) {
			Spacer(Modifier.height(14.dp))
			NeoButton(text = "ezafe kardane API", onClick = onOpenProviders, icon = Icons.Filled.Storage)
		} else if (suggestions.isNotEmpty()) {
			Spacer(Modifier.height(16.dp))
			Column(
				verticalArrangement = Arrangement.spacedBy(8.dp),
				horizontalAlignment = Alignment.CenterHorizontally,
			) {
				for (suggestion in suggestions) {
					NeoChip(
						text = suggestion.first,
						icon = Icons.Filled.Lightbulb,
						color = MaterialTheme.colorScheme.secondaryContainer,
						onClick = { onSuggestion(suggestion.second) },
					)
				}
			}
		}
	}
}

@Composable
private fun TypingIndicator() {
	val transition = rememberInfiniteTransition(label = "typing")
	val alpha by transition.animateFloat(
		initialValue = 0.3f,
		targetValue = 1f,
		animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
		label = "typingAlpha",
	)
	Row(modifier = Modifier.padding(start = 4.dp)) {
		NeoChip(text = "dar hale neveshtan…", modifier = Modifier.alpha(alpha))
	}
}

@Composable
private fun StreamCursor() {
	val transition = rememberInfiniteTransition(label = "cursor")
	val alpha by transition.animateFloat(
		initialValue = 0f,
		targetValue = 1f,
		animationSpec = infiniteRepeatable(tween(520), RepeatMode.Reverse),
		label = "cursorAlpha",
	)
	Box(
		modifier = Modifier
			.padding(top = 4.dp)
			.size(width = 12.dp, height = 18.dp)
			.alpha(alpha)
			.background(MaterialTheme.colorScheme.onSurface)
	)
}

@Composable
private fun MessageBubble(
	message: ChatMessage,
	showStats: Boolean,
	fontScale: FontScale,
	accent: Color,
	streaming: Boolean,
	expanded: Boolean,
	onToggleExpand: () -> Unit,
	onCopy: () -> Unit,
	onEdit: () -> Unit,
	onRegenerate: () -> Unit,
	onContinue: () -> Unit,
	onBranch: () -> Unit,
	onPin: () -> Unit,
	onDelete: () -> Unit,
	onRetry: () -> Unit,
	onChangeModel: () -> Unit,
	onVariant: (Int) -> Unit,
	onToast: (String) -> Unit,
) {
	val isUser = message.role == "user"
	val background = if (isUser) accent else MaterialTheme.colorScheme.surface
	val textColor = if (isUser) Color(0xFF101010) else MaterialTheme.colorScheme.onSurface
	val scale = when (fontScale) {
		FontScale.SMALL -> 0.9f
		FontScale.LARGE -> 1.18f
		FontScale.MEDIUM -> 1f
	}

	Column(
		modifier = Modifier.fillMaxWidth(),
		horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
	) {
		Box(modifier = Modifier.fillMaxWidth(0.9f)) {
			NeoBox(
				modifier = Modifier.fillMaxWidth(),
				background = background,
				onClick = onToggleExpand,
			) {
				Column(modifier = Modifier.padding(12.dp)) {
					Row(verticalAlignment = Alignment.CenterVertically) {
						Text(
							text = if (isUser) "to" else message.modelName.ifBlank { "dastyar" },
							style = MaterialTheme.typography.labelSmall,
							fontWeight = FontWeight.Bold,
							color = textColor.copy(alpha = 0.7f),
						)
						if (message.pinned) {
							Spacer(Modifier.width(6.dp))
							Icon(
								Icons.Filled.PushPin,
								contentDescription = "pin",
								tint = textColor.copy(alpha = 0.8f),
								modifier = Modifier.size(14.dp),
							)
						}
					}
					Spacer(Modifier.height(6.dp))
					for (attachment in message.attachments) {
						NeoChip(text = attachment.name, modifier = Modifier.padding(bottom = 6.dp))
					}
					val error = message.error
					if (error != null) {
						ErrorCard(error = error, onRetry = onRetry, onChangeModel = onChangeModel)
					} else if (message.activeText.isNotBlank()) {
						MessageContent(
							raw = message.activeText,
							textColor = textColor,
							fontScale = scale,
							onToast = onToast,
						)
					}
					if (streaming) StreamCursor()
					if (showStats && message.completionTokens > 0) {
						Spacer(Modifier.height(4.dp))
						Text(
							text = TokenCounter.formatTokens(message.promptTokens + message.completionTokens) +
								" token · " + TokenCounter.formatCost(message.costUsd),
							style = MaterialTheme.typography.labelSmall,
							color = textColor.copy(alpha = 0.6f),
						)
					}
				}
			}
		}

		if (!isUser && message.truncated && message.error == null) {
			Spacer(Modifier.height(6.dp))
			NeoButton(
				text = "edameye pasokh",
				icon = Icons.Filled.PlayArrow,
				containerColor = MaterialTheme.colorScheme.secondaryContainer,
				onClick = onContinue,
			)
		}

		AnimatedVisibility(
			visible = expanded,
			enter = fadeIn() + expandVertically(),
			exit = fadeOut() + shrinkVertically(),
		) {
			Row(
				modifier = Modifier.padding(top = 6.dp),
				verticalAlignment = Alignment.CenterVertically,
			) {
				NeoIconButton(
					icon = Icons.Filled.ContentCopy,
					boxSize = 44.dp,
					onClick = onCopy,
					contentDescription = "copy",
				)
				Spacer(Modifier.width(6.dp))
				if (isUser) {
					NeoIconButton(
						icon = Icons.Filled.Edit,
						boxSize = 44.dp,
						onClick = onEdit,
						contentDescription = "edit",
					)
				} else {
					NeoIconButton(
						icon = Icons.Filled.Refresh,
						boxSize = 44.dp,
						onClick = onRegenerate,
						contentDescription = "regenerate",
					)
				}
				Spacer(Modifier.width(6.dp))
				NeoIconButton(
					icon = Icons.Filled.PushPin,
					boxSize = 44.dp,
					containerColor = if (message.pinned) {
						MaterialTheme.colorScheme.primaryContainer
					} else {
						MaterialTheme.colorScheme.surface
					},
					onClick = onPin,
					contentDescription = "pin",
				)
				Spacer(Modifier.width(6.dp))
				NeoIconButton(
					icon = Icons.Filled.CallSplit,
					boxSize = 44.dp,
					onClick = onBranch,
					contentDescription = "branch",
				)
				Spacer(Modifier.width(6.dp))
				NeoIconButton(
					icon = Icons.Filled.Delete,
					boxSize = 44.dp,
					onClick = onDelete,
					contentDescription = "delete",
				)
				if (message.variantCount > 1) {
					Spacer(Modifier.width(8.dp))
					NeoIconButton(
						icon = Icons.Filled.KeyboardArrowRight,
						boxSize = 40.dp,
						onClick = { onVariant(message.variantIndex - 1) },
						enabled = message.variantIndex > 0,
						contentDescription = "prev",
					)
					Text(
						text = " " + (message.variantIndex + 1) + "/" + message.variantCount + " ",
						style = MaterialTheme.typography.labelSmall,
						color = MaterialTheme.colorScheme.onBackground,
					)
					NeoIconButton(
						icon = Icons.Filled.KeyboardArrowLeft,
						boxSize = 40.dp,
						onClick = { onVariant(message.variantIndex + 1) },
						enabled = message.variantIndex < message.variantCount - 1,
						contentDescription = "next",
					)
				}
			}
		}
	}
}

/** karte khataye jam-o-joor ba jozeiate ghabele baz kardan */
@Composable
private fun ErrorCard(error: String, onRetry: () -> Unit, onChangeModel: () -> Unit) {
	var showDetails by remember { mutableStateOf(false) }
	val friendly = friendlyError(error)
	NeoBox(
		modifier = Modifier.fillMaxWidth(),
		background = MaterialTheme.colorScheme.errorContainer,
	) {
		Column(modifier = Modifier.padding(10.dp)) {
			Text(
				text = friendly,
				style = MaterialTheme.typography.bodyMedium,
				fontWeight = FontWeight.Bold,
				color = MaterialTheme.colorScheme.onErrorContainer,
			)
			Spacer(Modifier.height(8.dp))
			Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
				NeoChip(
					text = "talashe dobare",
					icon = Icons.Filled.Refresh,
					color = MaterialTheme.colorScheme.surface,
					onClick = onRetry,
				)
				NeoChip(
					text = "taghire model",
					color = MaterialTheme.colorScheme.surface,
					onClick = onChangeModel,
				)
				NeoChip(
					text = if (showDetails) "bastane jozeiat" else "jozeiate fanni",
					color = MaterialTheme.colorScheme.surface,
					onClick = { showDetails = !showDetails },
				)
			}
			AnimatedVisibility(visible = showDetails) {
				Text(
					text = error,
					modifier = Modifier.padding(top = 8.dp),
					style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
					color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
				)
			}
		}
	}
}

private fun friendlyError(error: String): String {
	val low = error.lowercase()
	return when {
		low.contains("do_request_failed") || low.contains("upstream error") ->
			"in model rooye server dar dastres nist — model digari entekhab kon"
		low.contains("http 401") || low.contains("http 403") -> "kelide API motabar nist"
		low.contains("http 404") -> "Base URL ya name model eshtebah ast"
		low.contains("http 429") -> "mahdoodiate nerkh ya etmame etebar"
		low.contains("timeout") -> "server javab nadad (timeout)"
		low.contains("model entekhab nashode") -> "aval yek model entekhab kon"
		else -> "darkhast na-movafagh bood"
	}
}

@Composable
private fun StatusDialog(
	providerName: String,
	status: ProviderStatus,
	onRetest: () -> Unit,
	onDismiss: () -> Unit,
) {
	Dialog(onDismissRequest = onDismiss) {
		NeoBox(modifier = Modifier.fillMaxWidth(), background = MaterialTheme.colorScheme.surface) {
			Column(modifier = Modifier.padding(14.dp)) {
				Text(
					text = providerName.ifBlank { "API entekhab nashode" },
					style = MaterialTheme.typography.titleMedium,
					fontWeight = FontWeight.Bold,
					color = MaterialTheme.colorScheme.onSurface,
				)
				Spacer(Modifier.height(8.dp))
				Text(
					text = "vaz'iat: " + status.state.name,
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurface,
				)
				Text(
					text = "ta'khir: " + status.latencyMs + " ms",
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurface,
				)
				if (status.message.isNotBlank()) {
					Spacer(Modifier.height(6.dp))
					Text(
						text = status.message,
						style = MaterialTheme.typography.bodySmall,
						color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
					)
				}
				Spacer(Modifier.height(12.dp))
				Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
					NeoButton(text = "test dobare", icon = Icons.Filled.Refresh, onClick = onRetest)
					NeoButton(
						text = "bastan",
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
private fun PromptPickerDialog(
	prompts: List<Pair<String, String>>,
	onPick: (String) -> Unit,
	onManage: () -> Unit,
	onDismiss: () -> Unit,
) {
	Dialog(onDismissRequest = onDismiss) {
		NeoBox(modifier = Modifier.fillMaxWidth(), background = MaterialTheme.colorScheme.surface) {
			Column(modifier = Modifier.padding(14.dp)) {
				Text(
					"prompt haye amade",
					style = MaterialTheme.typography.titleMedium,
					fontWeight = FontWeight.Bold,
					color = MaterialTheme.colorScheme.onSurface,
				)
				Spacer(Modifier.height(10.dp))
				Column(
					modifier = Modifier
						.fillMaxWidth()
						.heightIn(max = 320.dp)
						.verticalScroll(rememberScrollState()),
					verticalArrangement = Arrangement.spacedBy(8.dp),
				) {
					if (prompts.isEmpty()) {
						Text(
							"hanooz prompti nasakhti",
							style = MaterialTheme.typography.bodyMedium,
							color = MaterialTheme.colorScheme.onSurface,
						)
					}
					for (prompt in prompts) {
						NeoBox(
							modifier = Modifier.fillMaxWidth(),
							background = MaterialTheme.colorScheme.surfaceVariant,
							onClick = { onPick(prompt.second) },
						) {
							Column(modifier = Modifier.padding(10.dp)) {
								Text(
									text = prompt.first,
									style = MaterialTheme.typography.labelLarge,
									fontWeight = FontWeight.Bold,
									color = MaterialTheme.colorScheme.onSurface,
								)
								Text(
									text = prompt.second.take(70).replace('\n', ' '),
									style = MaterialTheme.typography.bodySmall,
									color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
								)
							}
						}
					}
				}
				Spacer(Modifier.height(12.dp))
				Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
					NeoButton(text = "modiriat", icon = Icons.Filled.Edit, onClick = onManage)
					NeoButton(
						text = "bastan",
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
private fun PersonaPickerDialog(
	personas: List<Persona>,
	selectedId: String,
	onPick: (String) -> Unit,
	onManage: () -> Unit,
	onDismiss: () -> Unit,
) {
	Dialog(onDismissRequest = onDismiss) {
		NeoBox(modifier = Modifier.fillMaxWidth(), background = MaterialTheme.colorScheme.surface) {
			Column(modifier = Modifier.padding(14.dp)) {
				Text(
					"naghshe in goftogoo",
					style = MaterialTheme.typography.titleMedium,
					fontWeight = FontWeight.Bold,
					color = MaterialTheme.colorScheme.onSurface,
				)
				Spacer(Modifier.height(10.dp))
				Column(
					modifier = Modifier
						.fillMaxWidth()
						.heightIn(max = 320.dp)
						.verticalScroll(rememberScrollState()),
					verticalArrangement = Arrangement.spacedBy(8.dp),
				) {
					NeoBox(
						modifier = Modifier.fillMaxWidth(),
						background = if (selectedId.isBlank()) {
							MaterialTheme.colorScheme.primaryContainer
						} else {
							MaterialTheme.colorScheme.surfaceVariant
						},
						onClick = { onPick("") },
					) {
						Text(
							"bedoone naghsh",
							modifier = Modifier.padding(10.dp),
							style = MaterialTheme.typography.labelLarge,
							color = MaterialTheme.colorScheme.onSurface,
						)
					}
					for (persona in personas) {
						NeoBox(
							modifier = Modifier.fillMaxWidth(),
							background = if (selectedId == persona.id) {
								MaterialTheme.colorScheme.primaryContainer
							} else {
								MaterialTheme.colorScheme.surfaceVariant
							},
							onClick = { onPick(persona.id) },
						) {
							Column(modifier = Modifier.padding(10.dp)) {
								Text(
									text = persona.emoji + "  " + persona.name,
									style = MaterialTheme.typography.labelLarge,
									fontWeight = FontWeight.Bold,
									color = MaterialTheme.colorScheme.onSurface,
								)
								Text(
									text = persona.systemPrompt.take(70).replace('\n', ' '),
									style = MaterialTheme.typography.bodySmall,
									color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
								)
							}
						}
					}
				}
				Spacer(Modifier.height(12.dp))
				Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
					NeoButton(text = "modiriat", icon = Icons.Filled.Edit, onClick = onManage)
					NeoButton(
						text = "bastan",
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
private fun EditMessageDialog(
	message: ChatMessage,
	onDismiss: () -> Unit,
	onSave: (String, Boolean) -> Unit,
) {
	var text by remember { mutableStateOf(message.activeText) }
	Dialog(onDismissRequest = onDismiss) {
		NeoBox(modifier = Modifier.fillMaxWidth(), background = MaterialTheme.colorScheme.surface) {
			Column(modifier = Modifier.padding(14.dp)) {
				Text(
					"virayeshe payam",
					style = MaterialTheme.typography.titleMedium,
					fontWeight = FontWeight.Bold,
					color = MaterialTheme.colorScheme.onSurface,
				)
				Spacer(Modifier.height(10.dp))
				NeoTextField(
					value = text,
					onValueChange = { text = it },
					placeholder = "matne payam",
					modifier = Modifier
						.fillMaxWidth()
						.heightIn(min = 120.dp),
				)
				Spacer(Modifier.height(12.dp))
				Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
					NeoButton(
						text = "zakhire va pasokhe jadid",
						icon = Icons.Filled.Refresh,
						onClick = { onSave(text, true) },
					)
					NeoButton(
						text = "faghat zakhire",
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
		NeoBox(modifier = Modifier.fillMaxWidth(), background = MaterialTheme.colorScheme.surface) {
			Column(modifier = Modifier.padding(14.dp)) {
				Text(
					"darkhast ba " + request.failedProviderName + " na-movafagh bood",
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
					"ba yek API digar dobare talash konim? kodam API?",
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
									text = candidate.defaultModel.ifBlank { "bedoone modele pishfarz" },
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
						text = "bale, edame bede",
						icon = Icons.Filled.Check,
						enabled = selected != null,
						onClick = {
							val picked = selected
							if (picked != null) onAccept(picked, selectedModel)
						},
					)
					NeoButton(
						text = "na",
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
	onOpenSearch: () -> Unit,
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
			placeholder = "jost-o-joo dar goftogooha…",
			modifier = Modifier.fillMaxWidth(),
		)
		Spacer(Modifier.height(10.dp))
		Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
			NeoIconButton(
				icon = Icons.Filled.Storage,
				boxSize = 48.dp,
				onClick = onOpenProviders,
				contentDescription = "API ha",
			)
			NeoIconButton(
				icon = Icons.Filled.Search,
				boxSize = 48.dp,
				onClick = onOpenSearch,
				contentDescription = "jost-o-jooye sartasari",
			)
			NeoIconButton(
				icon = Icons.Filled.CompareArrows,
				boxSize = 48.dp,
				containerColor = MaterialTheme.colorScheme.secondaryContainer,
				onClick = onOpenCompare,
				contentDescription = "moghayese",
			)
			NeoIconButton(
				icon = Icons.Filled.Settings,
				boxSize = 48.dp,
				onClick = onOpenSettings,
				contentDescription = "tanzimat",
			)
			NeoIconButton(
				icon = Icons.Filled.Add,
				boxSize = 48.dp,
				containerColor = MaterialTheme.colorScheme.primaryContainer,
				onClick = { viewModel.newConversation() },
				contentDescription = "goftogooye jadid",
			)
		}
		Spacer(Modifier.height(12.dp))

		if (searchQuery.trim().length >= 2) {
			Text(
				text = "natayej (" + hits.size + ")",
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
					text = conversation.messages.size.toString() + " payam" +
						(if (conversation.parentId != null) "  ·  shakhe" else ""),
					style = MaterialTheme.typography.labelSmall,
					color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
				)
			}
			Icon(
				Icons.Filled.PushPin,
				contentDescription = "pin",
				tint = if (conversation.pinned) {
					MaterialTheme.colorScheme.onSurface
				} else {
					MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
				},
				modifier = Modifier
					.size(24.dp)
					.clickable(onClick = onPin),
			)
			Spacer(Modifier.width(10.dp))
			Icon(
				Icons.Filled.Delete,
				contentDescription = "delete",
				tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
				modifier = Modifier
					.size(24.dp)
					.clickable(onClick = onDelete),
			)
		}
	}
}
