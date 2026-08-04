package com.aigate.chat.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aigate.chat.data.AppStore
import com.aigate.chat.model.AppSettings
import com.aigate.chat.model.AppState
import com.aigate.chat.model.Attachment
import com.aigate.chat.model.ChatMessage
import com.aigate.chat.model.ConnectionState
import com.aigate.chat.model.Conversation
import com.aigate.chat.model.MemoryItem
import com.aigate.chat.model.Persona
import com.aigate.chat.model.PromptItem
import com.aigate.chat.model.UsageRecord
import com.aigate.chat.model.Provider
import com.aigate.chat.model.ProviderStatus
import com.aigate.chat.model.ProviderType
import com.aigate.chat.net.AiClient
import com.aigate.chat.net.StreamEvent
import com.aigate.chat.net.WebSessionClient
import com.aigate.chat.service.GenerationService
import com.aigate.chat.util.AttachmentUtils
import com.aigate.chat.util.Exporter
import com.aigate.chat.util.FileSaver
import com.aigate.chat.util.TokenCounter
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FallbackRequest(
	val conversationId: String,
	val messageId: String,
	val failedProviderName: String,
	val error: String,
	val candidates: List<Provider>,
)

data class CompareSide(
	val providerId: String = "",
	val model: String = "",
	val text: String = "",
	val running: Boolean = false,
	val error: String? = null,
	val tokens: Int = 0,
	val cost: Double = 0.0,
	val elapsedMs: Long = 0L,
)

data class CompareState(
	val prompt: String = "",
	val left: CompareSide = CompareSide(),
	val right: CompareSide = CompareSide(),
)

data class UiState(
	val loaded: Boolean = false,
	val providers: List<Provider> = emptyList(),
	val conversations: List<Conversation> = emptyList(),
	val settings: AppSettings = AppSettings(),
	val currentId: String = "",
	val pendingAttachments: List<Attachment> = emptyList(),
	val isGenerating: Boolean = false,
	val isLoadingAttachment: Boolean = false,
	val isFetchingModels: Boolean = false,
	val toast: String? = null,
	val statuses: Map<String, ProviderStatus> = emptyMap(),
	val fallback: FallbackRequest? = null,
	val compare: CompareState = CompareState(),
	val personas: List<Persona> = emptyList(),
	val prompts: List<PromptItem> = emptyList(),
	val usage: List<UsageRecord> = emptyList(),
	val unlocked: Boolean = false,
	val budgetWarned: Boolean = false,
) {
	val current: Conversation?
		get() = conversations.firstOrNull { it.id == currentId }

	fun providerOf(conversation: Conversation?): Provider? {
		if (conversation == null) return null
		return providers.firstOrNull { it.id == conversation.providerId }
	}

	fun providerById(id: String): Provider? = providers.firstOrNull { it.id == id }

	fun personaOf(conversation: Conversation?): Persona? {
		if (conversation == null || conversation.personaId.isBlank()) return null
		return personas.firstOrNull { it.id == conversation.personaId }
	}

	/** hazineye mahe jari be dollar */
	val monthCost: Double
		get() {
			val month = currentMonthKey()
			return usage.filter { it.month == month }.sumOf { it.costUsd }
		}

	val sortedConversations: List<Conversation>
		get() = conversations.sortedWith(
			compareByDescending<Conversation> { it.pinned }.thenByDescending { it.updatedAt }
		)
}

/** نتیجه‌ی یک جست‌وجو در پیام‌ها */
data class SearchHit(
	val conversationId: String,
	val conversationTitle: String,
	val messageId: String,
	val snippet: String,
	val role: String,
)

/** kelide mahe jari mesle 2026-08 */
fun currentMonthKey(): String {
	val calendar = java.util.Calendar.getInstance()
	val year = calendar.get(java.util.Calendar.YEAR)
	val month = calendar.get(java.util.Calendar.MONTH) + 1
	return year.toString() + "-" + (if (month < 10) "0" else "") + month.toString()
}

class ChatViewModel(app: Application) : AndroidViewModel(app) {

	private val store = AppStore(app)
	private val client = AiClient()

	private val _state = MutableStateFlow(UiState())
	val state: StateFlow<UiState> = _state.asStateFlow()

	private var generationJob: Job? = null

	init {
		viewModelScope.launch {
			val loaded: AppState = store.load()
			val conversations = if (loaded.conversations.isEmpty()) {
				listOf(Conversation())
			} else {
				loaded.conversations
			}
			val currentId = conversations.firstOrNull { it.id == loaded.currentConversationId }?.id
				?: conversations.first().id
			_state.value = UiState(
				loaded = true,
				providers = loaded.providers,
				conversations = conversations,
				settings = loaded.settings,
				currentId = currentId,
				personas = loaded.personas,
				prompts = if (loaded.prompts.isEmpty()) defaultPrompts() else loaded.prompts,
				usage = loaded.usage,
				unlocked = !loaded.settings.appLockEnabled,
			)
		}
	}

	// ---------------- ذخیره ----------------

	private fun persist() {
		val snapshot = _state.value
		viewModelScope.launch {
			store.save(
				AppState(
					providers = snapshot.providers,
					conversations = snapshot.conversations,
					settings = snapshot.settings,
					currentConversationId = snapshot.currentId,
					personas = snapshot.personas,
					prompts = snapshot.prompts,
					usage = snapshot.usage,
				)
			)
		}
	}

	fun showToast(message: String?) {
		_state.value = _state.value.copy(toast = message)
	}

	private fun updateConversation(id: String, transform: (Conversation) -> Conversation) {
		val s = _state.value
		_state.value = s.copy(
			conversations = s.conversations.map { if (it.id == id) transform(it) else it }
		)
	}

	// ---------------- مدیریت API ----------------

	fun fetchModels(
		baseUrl: String,
		authKey: String,
		type: ProviderType,
		onResult: (List<String>) -> Unit,
	) {
		if (type == ProviderType.WEB) {
			val webModels = listOf("DeepSeek", "DeepSeek-R1 (DeepThink)")
			showToast("دو مدل وب اضافه شد")
			onResult(webModels)
			return
		}
		_state.value = _state.value.copy(isFetchingModels = true)
		viewModelScope.launch {
			val result = client.listModels(baseUrl, authKey, type)
			_state.value = _state.value.copy(isFetchingModels = false)
			result.fold(
				onSuccess = { models ->
					showToast(models.size.toString() + " مدل پیدا شد")
					onResult(models)
				},
				onFailure = { error ->
					showToast("دریافت مدل‌ها ناموفق: " + (error.message ?: "خطا"))
					onResult(emptyList())
				},
			)
		}
	}

	/** تست اتصال یک API ذخیره‌شده */
	fun testProvider(providerId: String) {
		val provider = _state.value.providerById(providerId) ?: return
		setStatus(providerId, ProviderStatus(ConnectionState.TESTING))
		viewModelScope.launch {
			val ping = if (provider.type == ProviderType.WEB) {
				WebSessionClient.checkSession(getApplication(), provider)
			} else {
				client.ping(provider.baseUrl, provider.authKey, provider.type)
			}
			setStatus(
				providerId,
				ProviderStatus(
					state = if (ping.ok) ConnectionState.ONLINE else ConnectionState.OFFLINE,
					latencyMs = ping.latencyMs,
					message = ping.message,
				)
			)
		}
	}

	fun testAllProviders() {
		for (provider in _state.value.providers) testProvider(provider.id)
	}

	private fun setStatus(providerId: String, status: ProviderStatus) {
		val s = _state.value
		val map = HashMap(s.statuses)
		map[providerId] = status
		_state.value = s.copy(statuses = map)
	}

	fun addProvider(
		name: String,
		baseUrl: String,
		authKey: String,
		type: ProviderType,
		models: List<String>,
		inputPrice: Double,
		outputPrice: Double,
	) {
		val s = _state.value
		val isWeb = type == ProviderType.WEB
		val webModels = listOf("DeepSeek", "DeepSeek-R1 (DeepThink)")
		val finalModels = if (isWeb && models.isEmpty()) webModels else models
		val finalUrl = if (isWeb) {
			baseUrl.trim().ifBlank { WebSessionClient.DEFAULT_SITE }
		} else {
			AiClient.normalizeBaseUrl(baseUrl)
		}
		val provider = Provider(
			name = name.ifBlank { if (isWeb) "DeepSeek (وب)" else "API " + (s.providers.size + 1) },
			baseUrl = finalUrl,
			authKey = authKey.trim(),
			type = type,
			models = finalModels,
			defaultModel = finalModels.firstOrNull().orEmpty(),
			colorIndex = s.providers.size % 6,
			inputPricePerM = inputPrice,
			outputPricePerM = outputPrice,
		)
		_state.value = s.copy(providers = s.providers + provider)
		// اگر گفتگوی فعلی API ندارد، همین را بگذار
		val current = _state.value.current
		if (current != null && current.providerId.isBlank()) {
			setConversationProvider(current.id, provider.id, provider.defaultModel)
		}
		persist()
		testProvider(provider.id)
		showToast("API اضافه شد")
	}

	fun updateProvider(provider: Provider) {
		val s = _state.value
		_state.value = s.copy(
			providers = s.providers.map { if (it.id == provider.id) provider else it }
		)
		persist()
	}

	fun toggleFavoriteModel(providerId: String, model: String) {
		val provider = _state.value.providerById(providerId) ?: return
		val favorites = if (provider.favoriteModels.contains(model)) {
			provider.favoriteModels - model
		} else {
			provider.favoriteModels + model
		}
		updateProvider(provider.copy(favoriteModels = favorites))
	}

	fun refreshProviderModels(providerId: String) {
		val provider = _state.value.providerById(providerId) ?: return
		_state.value = _state.value.copy(isFetchingModels = true)
		viewModelScope.launch {
			val result = client.listModels(provider.baseUrl, provider.authKey, provider.type)
			_state.value = _state.value.copy(isFetchingModels = false)
			result.fold(
				onSuccess = { models ->
					val updated = provider.copy(
						models = models,
						defaultModel = if (models.contains(provider.defaultModel)) {
							provider.defaultModel
						} else {
							models.firstOrNull().orEmpty()
						},
					)
					updateProvider(updated)
					showToast("لیست مدل‌ها به‌روز شد (" + models.size + ")")
				},
				onFailure = { error ->
					showToast("خطا: " + (error.message ?: "نامشخص"))
				},
			)
		}
	}

	fun deleteProvider(providerId: String) {
		val s = _state.value
		_state.value = s.copy(providers = s.providers.filterNot { it.id == providerId })
		persist()
	}

	// ---------------- گفتگوها ----------------

	fun newConversation() {
		val s = _state.value
		val last = s.current
		val conversation = Conversation(
			providerId = last?.providerId ?: s.providers.firstOrNull()?.id.orEmpty(),
			model = last?.model ?: s.providers.firstOrNull()?.defaultModel.orEmpty(),
		)
		_state.value = s.copy(
			conversations = listOf(conversation) + s.conversations,
			currentId = conversation.id,
			pendingAttachments = emptyList(),
		)
		persist()
	}

	fun selectConversation(id: String) {
		_state.value = _state.value.copy(currentId = id, pendingAttachments = emptyList())
		persist()
	}

	fun deleteConversation(id: String) {
		val s = _state.value
		val remaining = s.conversations.filterNot { it.id == id }
		val list = if (remaining.isEmpty()) listOf(Conversation()) else remaining
		val newCurrent = if (s.currentId == id) list.first().id else s.currentId
		_state.value = s.copy(conversations = list, currentId = newCurrent)
		persist()
	}

	fun togglePin(id: String) {
		updateConversation(id) { it.copy(pinned = !it.pinned) }
		persist()
	}

	fun renameConversation(id: String, title: String) {
		updateConversation(id) { it.copy(title = title.ifBlank { it.title }) }
		persist()
	}

	fun setConversationProvider(conversationId: String, providerId: String, model: String) {
		updateConversation(conversationId) { it.copy(providerId = providerId, model = model) }
		persist()
	}

	/** شاخه‌ای کردن: کپی گفتگو تا پیام موردنظر در یک گفتگوی جدید */
	fun branchFrom(conversationId: String, messageId: String) {
		val s = _state.value
		val source = s.conversations.firstOrNull { it.id == conversationId } ?: return
		val index = source.messages.indexOfFirst { it.id == messageId }
		if (index < 0) return
		val branch = Conversation(
			title = "شاخه — " + source.title,
			providerId = source.providerId,
			model = source.model,
			messages = source.messages.take(index + 1).map { it.copy(id = java.util.UUID.randomUUID().toString()) },
			parentId = source.id,
			branchedFromMessageId = messageId,
		)
		_state.value = s.copy(
			conversations = listOf(branch) + s.conversations,
			currentId = branch.id,
		)
		persist()
		showToast("شاخه‌ی جدید ساخته شد")
	}

	/** جست‌وجو در همه‌ی پیام‌ها */
	fun search(query: String): List<SearchHit> {
		val q = query.trim()
		if (q.length < 2) return emptyList()
		val hits = ArrayList<SearchHit>()
		for (conversation in _state.value.sortedConversations) {
			for (message in conversation.messages) {
				val text = message.activeText
				val at = text.indexOf(q, ignoreCase = true)
				if (at >= 0) {
					val start = if (at - 30 < 0) 0 else at - 30
					val end = if (at + 90 > text.length) text.length else at + 90
					hits.add(
						SearchHit(
							conversationId = conversation.id,
							conversationTitle = conversation.title,
							messageId = message.id,
							snippet = text.substring(start, end).replace('\n', ' '),
							role = message.role,
						)
					)
				}
				if (hits.size >= 80) return hits
			}
		}
		return hits
	}

	// ---------------- تنظیمات و حافظه ----------------

	fun updateSettings(settings: AppSettings) {
		_state.value = _state.value.copy(settings = settings)
		persist()
	}

	fun addMemory(text: String) {
		if (text.isBlank()) return
		val settings = _state.value.settings
		updateSettings(settings.copy(memory = settings.memory + MemoryItem(text = text.trim())))
	}

	fun updateMemory(item: MemoryItem) {
		val settings = _state.value.settings
		updateSettings(
			settings.copy(memory = settings.memory.map { if (it.id == item.id) item else it })
		)
	}

	fun deleteMemory(id: String) {
		val settings = _state.value.settings
		updateSettings(settings.copy(memory = settings.memory.filterNot { it.id == id }))
	}

	private fun buildSystemPrompt(): String {
		val s = _state.value
		val settings = s.settings
		val persona = s.personaOf(s.current)
		val base = if (persona != null && persona.systemPrompt.isNotBlank()) {
			persona.systemPrompt.trim()
		} else {
			settings.systemPrompt.trim()
		}
		val sb = StringBuilder(base)
		if (settings.memoryEnabled) {
			val active = settings.memory.filter { it.enabled && it.text.isNotBlank() }
			if (active.isNotEmpty()) {
				sb.append("\n\nLong-term memory about the user:\n")
				for (item in active) sb.append("- ").append(item.text).append("\n")
			}
		}
		return sb.toString().trim()
	}

	// ---------------- پیوست‌ها ----------------

	fun addAttachments(uris: List<Uri>) {
		if (uris.isEmpty()) return
		_state.value = _state.value.copy(isLoadingAttachment = true)
		viewModelScope.launch {
			val collected = ArrayList<Attachment>()
			for (uri in uris) {
				val attachment = AttachmentUtils.fromUri(getApplication(), uri)
				if (attachment != null) collected.add(attachment)
			}
			val s = _state.value
			_state.value = s.copy(
				pendingAttachments = s.pendingAttachments + collected,
				isLoadingAttachment = false,
				toast = if (collected.isEmpty()) "خواندن فایل ناموفق بود" else s.toast,
			)
		}
	}

	fun removeAttachment(id: String) {
		val s = _state.value
		_state.value = s.copy(pendingAttachments = s.pendingAttachments.filterNot { it.id == id })
	}

	// ---------------- ارسال و تولید ----------------

	fun send(text: String) {
		val s = _state.value
		val conversation = s.current ?: return
		val provider = s.providerOf(conversation)
		if (provider == null) {
			showToast("اول یک API اضافه و انتخاب کن")
			return
		}
		if (overBudget()) {
			showToast("بودجه ماهانه تمام شده — از تنزیمات تغییر بده")
			return
		}
		val body = text.trim()
		if (body.isEmpty() && s.pendingAttachments.isEmpty()) return

		val userMessage = ChatMessage(
			role = "user",
			content = body,
			attachments = s.pendingAttachments,
		)
		val title = if (conversation.messages.isEmpty() && body.isNotBlank()) {
			body.take(34)
		} else {
			conversation.title
		}
		updateConversation(conversation.id) {
			it.copy(
				messages = it.messages + userMessage,
				title = title,
				updatedAt = System.currentTimeMillis(),
			)
		}
		_state.value = _state.value.copy(pendingAttachments = emptyList())

		if (body.startsWith("/image ")) {
			generateImage(conversation.id, provider, body.removePrefix("/image ").trim())
			return
		}

		val assistant = ChatMessage(role = "assistant", content = "", modelName = conversation.model)
		updateConversation(conversation.id) { it.copy(messages = it.messages + assistant) }
		startGeneration(conversation.id, assistant.id, provider, conversation.model)
	}

	private fun historyFor(conversationId: String, assistantMessageId: String): List<ChatMessage> {
		val conversation = _state.value.conversations.firstOrNull { it.id == conversationId }
			?: return emptyList()
		val index = conversation.messages.indexOfFirst { it.id == assistantMessageId }
		return if (index < 0) conversation.messages else conversation.messages.take(index)
	}

	private fun startGeneration(
		conversationId: String,
		assistantMessageId: String,
		provider: Provider,
		model: String,
		asVariant: Boolean = false,
	) {
		val settings = _state.value.settings
		val history = historyFor(conversationId, assistantMessageId)
		val systemPrompt = buildSystemPrompt()
		val usedModel = model.ifBlank { provider.defaultModel }

		_state.value = _state.value.copy(isGenerating = true, fallback = null)
		GenerationService.start(
			getApplication(),
			"AiGate — " + provider.name,
			"در حال دریافت پاسخ از " + usedModel,
		)

		generationJob?.cancel()
		generationJob = viewModelScope.launch {
			val builder = StringBuilder()
			var failure: String? = null
			var usagePrompt = 0
			var usageCompletion = 0

			val webMode = provider.type == ProviderType.WEB
			val streamFlow = if (webMode) {
				WebSessionClient.streamChat(getApplication(), provider, usedModel, history)
			} else {
				client.streamChat(provider, usedModel, settings, systemPrompt, history)
			}
			if (settings.streaming || webMode) {
				streamFlow.collect { event ->
					when (event) {
						is StreamEvent.Delta -> {
							builder.append(event.text)
							setMessageText(conversationId, assistantMessageId, builder.toString(), asVariant)
						}

						is StreamEvent.Usage -> {
							if (event.promptTokens > 0) usagePrompt = event.promptTokens
							if (event.completionTokens > 0) usageCompletion = event.completionTokens
						}

						is StreamEvent.Replace -> {
							builder.setLength(0)
							builder.append(event.text)
							setMessageText(conversationId, assistantMessageId, builder.toString(), asVariant)
						}

						is StreamEvent.Failure -> failure = event.message
						is StreamEvent.Done -> Unit
					}
				}
			} else {
				val result = client.complete(provider, usedModel, settings, systemPrompt, history)
				result.fold(
					onSuccess = { text ->
						builder.append(text)
						setMessageText(conversationId, assistantMessageId, text, asVariant)
					},
					onFailure = { error -> failure = error.message ?: "خطا" },
				)
			}

			// agar stream shekast khord, yek bar ba halate gheyre stream talash kon
			if (failure != null && builder.isEmpty() && settings.streaming && !webMode) {
				val retry = client.complete(provider, usedModel, settings, systemPrompt, history)
				retry.fold(
					onSuccess = { text ->
						if (text.isNotEmpty()) {
							builder.append(text)
							setMessageText(conversationId, assistantMessageId, text, asVariant)
							failure = null
						}
					},
					onFailure = { error ->
						failure = (failure ?: "") + " | retry: " + (error.message ?: "khata")
					},
				)
			}

			val errorText = failure
			if (errorText != null && builder.isEmpty()) {
				onGenerationFailed(conversationId, assistantMessageId, provider, errorText)
			} else {
				finishGeneration(
					conversationId = conversationId,
					assistantMessageId = assistantMessageId,
					provider = provider,
					model = usedModel,
					history = history,
					answer = builder.toString(),
					usagePrompt = usagePrompt,
					usageCompletion = usageCompletion,
				)
			}
			_state.value = _state.value.copy(isGenerating = false)
			GenerationService.stop(getApplication())
			persist()
		}
	}

	private fun setMessageText(
		conversationId: String,
		messageId: String,
		text: String,
		asVariant: Boolean,
	) {
		updateConversation(conversationId) { conversation ->
			conversation.copy(
				messages = conversation.messages.map { message ->
					if (message.id != messageId) {
						message
					} else if (asVariant && message.variants.isNotEmpty()) {
						val updated = message.variants.toMutableList()
						val index = if (message.variantIndex in updated.indices) message.variantIndex else 0
						updated[index] = text
						message.copy(variants = updated, error = null)
					} else {
						message.copy(content = text, error = null)
					}
				}
			)
		}
	}

	private fun finishGeneration(
		conversationId: String,
		assistantMessageId: String,
		provider: Provider,
		model: String,
		history: List<ChatMessage>,
		answer: String,
		usagePrompt: Int,
		usageCompletion: Int,
	) {
		val promptTokens = if (usagePrompt > 0) {
			usagePrompt
		} else {
			TokenCounter.countHistory(history) + TokenCounter.countText(buildSystemPrompt())
		}
		val completionTokens = if (usageCompletion > 0) usageCompletion else TokenCounter.countText(answer)
		val cost = TokenCounter.estimateCost(provider, model, promptTokens, completionTokens)
		recordUsage(provider.id, model, promptTokens, completionTokens, cost)
		updateConversation(conversationId) { conversation ->
			conversation.copy(
				updatedAt = System.currentTimeMillis(),
				messages = conversation.messages.map { message ->
					if (message.id == assistantMessageId) {
						message.copy(
							promptTokens = promptTokens,
							completionTokens = completionTokens,
							costUsd = cost,
							modelName = model,
							truncated = looksTruncated(answer),
						)
					} else {
						message
					}
				}
			)
		}
	}

	/** در صورت خطا: اول از کاربر برای استفاده از API دیگر اجازه بگیر */
	private fun onGenerationFailed(
		conversationId: String,
		assistantMessageId: String,
		provider: Provider,
		error: String,
	) {
		setStatus(provider.id, ProviderStatus(ConnectionState.OFFLINE, 0L, error.take(160)))
		val s = _state.value
		val candidates = s.providers.filterNot { it.id == provider.id }
		updateConversation(conversationId) { conversation ->
			conversation.copy(
				messages = conversation.messages.map {
					if (it.id == assistantMessageId) it.copy(error = error) else it
				}
			)
		}
		if (s.settings.askFallback && candidates.isNotEmpty()) {
			_state.value = _state.value.copy(
				fallback = FallbackRequest(
					conversationId = conversationId,
					messageId = assistantMessageId,
					failedProviderName = provider.name,
					error = error,
					candidates = candidates,
				)
			)
		} else {
			showToast("خطا: " + error.take(120))
		}
	}

	/** کاربر API جایگزین را انتخاب کرد */
	fun acceptFallback(providerId: String, model: String) {
		val request = _state.value.fallback ?: return
		val provider = _state.value.providerById(providerId) ?: return
		_state.value = _state.value.copy(fallback = null)
		updateConversation(request.conversationId) { conversation ->
			conversation.copy(
				messages = conversation.messages.map {
					if (it.id == request.messageId) it.copy(error = null, content = "") else it
				}
			)
		}
		startGeneration(request.conversationId, request.messageId, provider, model.ifBlank { provider.defaultModel })
	}

	fun dismissFallback() {
		_state.value = _state.value.copy(fallback = null)
	}

	fun stopGeneration() {
		generationJob?.cancel()
		generationJob = null
		_state.value = _state.value.copy(isGenerating = false)
		GenerationService.stop(getApplication())
		persist()
	}

	// ---------------- ویرایش، تولید دوباره، حذف ----------------

	fun deleteMessage(messageId: String) {
		val conversation = _state.value.current ?: return
		updateConversation(conversation.id) {
			it.copy(messages = it.messages.filterNot { m -> m.id == messageId })
		}
		persist()
	}

	/** ویرایش پیام کاربر — پاسخ‌های بعدی حذف و دوباره تولید می‌شود */
	fun editMessage(messageId: String, newText: String, resend: Boolean) {
		val s = _state.value
		val conversation = s.current ?: return
		val index = conversation.messages.indexOfFirst { it.id == messageId }
		if (index < 0) return
		if (!resend) {
			updateConversation(conversation.id) { c ->
				c.copy(messages = c.messages.map { if (it.id == messageId) it.copy(content = newText) else it })
			}
			persist()
			return
		}
		val provider = s.providerOf(conversation)
		if (provider == null) {
			showToast("API انتخاب نشده")
			return
		}
		val trimmed = conversation.messages.take(index + 1).toMutableList()
		trimmed[index] = trimmed[index].copy(content = newText, variants = emptyList(), variantIndex = 0)
		val assistant = ChatMessage(role = "assistant", content = "", modelName = conversation.model)
		updateConversation(conversation.id) {
			it.copy(messages = trimmed + assistant, updatedAt = System.currentTimeMillis())
		}
		startGeneration(conversation.id, assistant.id, provider, conversation.model)
	}

	/** تولید دوباره‌ی پاسخ دستیار به صورت یک نسخه‌ی جدید */
	fun regenerate(messageId: String) {
		val s = _state.value
		val conversation = s.current ?: return
		val provider = s.providerOf(conversation)
		if (provider == null) {
			showToast("API انتخاب نشده")
			return
		}
		val message = conversation.messages.firstOrNull { it.id == messageId } ?: return
		val existing = if (message.variants.isEmpty()) listOf(message.content) else message.variants
		val variants = existing + ""
		updateConversation(conversation.id) { c ->
			c.copy(
				messages = c.messages.map {
					if (it.id == messageId) {
						it.copy(variants = variants, variantIndex = variants.size - 1, error = null)
					} else {
						it
					}
				}
			)
		}
		startGeneration(conversation.id, messageId, provider, conversation.model, asVariant = true)
	}

	fun selectVariant(messageId: String, index: Int) {
		val conversation = _state.value.current ?: return
		updateConversation(conversation.id) { c ->
			c.copy(
				messages = c.messages.map {
					if (it.id == messageId && index in it.variants.indices) {
						it.copy(variantIndex = index)
					} else {
						it
					}
				}
			)
		}
		persist()
	}

	// ---------------- تولید تصویر ----------------

	private fun generateImage(conversationId: String, provider: Provider, prompt: String) {
		val placeholder = ChatMessage(role = "assistant", content = "در حال ساخت تصویر…")
		updateConversation(conversationId) { it.copy(messages = it.messages + placeholder) }
		_state.value = _state.value.copy(isGenerating = true)
		GenerationService.start(getApplication(), "AiGate", "در حال ساخت تصویر…")
		generationJob?.cancel()
		generationJob = viewModelScope.launch {
			val model = _state.value.conversations.firstOrNull { it.id == conversationId }?.model.orEmpty()
			val result = client.generateImage(provider, model.ifBlank { "dall-e-3" }, prompt)
			result.fold(
				onSuccess = { url ->
					setMessageText(conversationId, placeholder.id, "![image](" + url + ")", false)
				},
				onFailure = { error ->
					updateConversation(conversationId) { c ->
						c.copy(
							messages = c.messages.map {
								if (it.id == placeholder.id) {
									it.copy(content = "", error = error.message ?: "خطا در ساخت تصویر")
								} else {
									it
								}
							}
						)
					}
				},
			)
			_state.value = _state.value.copy(isGenerating = false)
			GenerationService.stop(getApplication())
			persist()
		}
	}

	// ---------------- خروجی و پشتیبان ----------------

	/** format: markdown | text | html */
	fun exportConversation(conversationId: String, format: String) {
		val s = _state.value
		val conversation = s.conversations.firstOrNull { it.id == conversationId } ?: return
		val provider = s.providerOf(conversation)
		viewModelScope.launch {
			val base = Exporter.safeName(conversation.title) + "_" + System.currentTimeMillis()
			val path = when (format) {
				"markdown" -> FileSaver.saveText(
					getApplication(),
					base + ".md",
					Exporter.toMarkdown(conversation, provider),
					"text/markdown",
				)

				"html" -> FileSaver.saveText(
					getApplication(),
					base + ".html",
					Exporter.toHtml(conversation, provider),
					"text/html",
				)

				else -> FileSaver.saveText(
					getApplication(),
					base + ".txt",
					Exporter.toPlainText(conversation),
					"text/plain",
				)
			}
			showToast(if (path != null) "ذخیره شد: " + path else "خروجی ناموفق بود")
		}
	}

	fun exportBackup() {
		val s = _state.value
		val text = store.exportJson(
			AppState(
				providers = s.providers,
				conversations = s.conversations,
				settings = s.settings,
				currentConversationId = s.currentId,
			)
		)
		viewModelScope.launch {
			val name = "aigate_backup_" + System.currentTimeMillis() + ".json"
			val path = FileSaver.saveText(getApplication(), name, text, "application/json")
			showToast(if (path != null) "پشتیبان ذخیره شد: " + path else "پشتیبان‌گیری ناموفق بود")
		}
	}

	fun importBackupFromUri(uri: Uri) {
		viewModelScope.launch {
			val text = try {
				getApplication<Application>().contentResolver.openInputStream(uri)
					?.bufferedReader()?.use { it.readText() }
			} catch (t: Throwable) {
				null
			}
			if (text == null) {
				showToast("خواندن فایل ناموفق بود")
				return@launch
			}
			store.importJson(text).fold(
				onSuccess = { restored ->
					val conversations = if (restored.conversations.isEmpty()) {
						listOf(Conversation())
					} else {
						restored.conversations
					}
					_state.value = _state.value.copy(
						providers = restored.providers,
						conversations = conversations,
						settings = restored.settings,
						currentId = conversations.first().id,
						toast = "پشتیبان بازیابی شد",
					)
					persist()
				},
				onFailure = { showToast("فایل پشتیبان معتبر نیست") },
			)
		}
	}

	// ---------------- naghsh ha (persona) ----------------

	fun addPersona(persona: Persona) {
		val s = _state.value
		_state.value = s.copy(personas = s.personas + persona)
		persist()
		showToast("نقش ساخته شد")
	}

	fun updatePersona(persona: Persona) {
		val s = _state.value
		_state.value = s.copy(personas = s.personas.map { if (it.id == persona.id) persona else it })
		persist()
	}

	fun deletePersona(id: String) {
		val s = _state.value
		_state.value = s.copy(
			personas = s.personas.filterNot { it.id == id },
			conversations = s.conversations.map {
				if (it.personaId == id) it.copy(personaId = "") else it
			},
		)
		persist()
	}

	/** naghsh ra be goftogooye fa'al bede (model va API-e naghsh ham e'mal mishavad) */
	fun applyPersona(conversationId: String, personaId: String) {
		val s = _state.value
		val persona = s.personas.firstOrNull { it.id == personaId }
		updateConversation(conversationId) { conversation ->
			if (persona == null) {
				conversation.copy(personaId = "")
			} else {
				conversation.copy(
					personaId = persona.id,
					providerId = persona.providerId.ifBlank { conversation.providerId },
					model = persona.model.ifBlank { conversation.model },
				)
			}
		}
		persist()
	}

	// ---------------- prompt haye amade ----------------

	private fun defaultPrompts(): List<PromptItem> = listOf(
		PromptItem(title = "خلاصه کن", body = "matne zir ra dar 5 bande kootah خلاصه کن:\n\n", shortcut = "sum"),
		PromptItem(title = "ترجمه به فارسی", body = "matne zir ra ravan be farsi tarjome kon:\n\n", shortcut = "fa"),
		PromptItem(title = "بازبینی کد", body = "in code ra baresi kon va bug ha va behbood ha ra begoo:\n\n", shortcut = "code"),
	)

	fun addPrompt(item: PromptItem) {
		val s = _state.value
		_state.value = s.copy(prompts = s.prompts + item)
		persist()
	}

	fun updatePrompt(item: PromptItem) {
		val s = _state.value
		_state.value = s.copy(prompts = s.prompts.map { if (it.id == item.id) item else it })
		persist()
	}

	fun deletePrompt(id: String) {
		val s = _state.value
		_state.value = s.copy(prompts = s.prompts.filterNot { it.id == id })
		persist()
	}

	// ---------------- pin kardane پیام ----------------

	fun toggleMessagePin(messageId: String) {
		val conversation = _state.value.current ?: return
		updateConversation(conversation.id) { c ->
			c.copy(
				messages = c.messages.map {
					if (it.id == messageId) it.copy(pinned = !it.pinned) else it
				}
			)
		}
		persist()
	}

	// ---------------- ghofle app ----------------

	fun setUnlocked(value: Boolean) {
		_state.value = _state.value.copy(unlocked = value)
	}

	// ---------------- budje ----------------

	private fun recordUsage(
		providerId: String,
		model: String,
		promptTokens: Int,
		completionTokens: Int,
		cost: Double,
	) {
		val month = currentMonthKey()
		val s = _state.value
		val existing = s.usage.firstOrNull {
			it.month == month && it.providerId == providerId && it.model == model
		}
		val updated = if (existing == null) {
			s.usage + UsageRecord(month, providerId, model, promptTokens, completionTokens, cost)
		} else {
			s.usage.map {
				if (it === existing) {
					it.copy(
						promptTokens = it.promptTokens + promptTokens,
						completionTokens = it.completionTokens + completionTokens,
						costUsd = it.costUsd + cost,
					)
				} else {
					it
				}
			}
		}
		_state.value = _state.value.copy(usage = updated)
		checkBudget()
	}

	private fun checkBudget() {
		val s = _state.value
		val budget = s.settings.monthlyBudgetUsd
		if (budget <= 0.0) return
		val spent = s.monthCost
		val percent = (spent / budget) * 100.0
		if (percent >= 100.0) {
			showToast("بودجه ماهانه تمام شد: " + TokenCounter.formatCost(spent))
		} else if (percent >= s.settings.budgetWarnPercent && !s.budgetWarned) {
			_state.value = _state.value.copy(budgetWarned = true)
			showToast("هشدار: " + percent.toInt() + "٪ بودجه مصرف شده")
		}
	}

	fun resetMonthUsage() {
		val month = currentMonthKey()
		val s = _state.value
		_state.value = s.copy(usage = s.usage.filterNot { it.month == month }, budgetWarned = false)
		persist()
		showToast("مصرف این ماه صفر شد")
	}

	private fun overBudget(): Boolean {
		val s = _state.value
		val budget = s.settings.monthlyBudgetUsd
		if (budget <= 0.0 || !s.settings.blockOverBudget) return false
		return s.monthCost >= budget
	}

	// ---------------- edame dadane pasokhe nesfe ----------------

	private fun looksTruncated(text: String): Boolean {
		val trimmed = text.trimEnd()
		if (trimmed.length < 80) return false
		val last = trimmed.last()
		val enders = ".!?\u061F\u060C:;)]}\u00BB\"'\u06D4"
		val openFences = Regex("```").findAll(trimmed).count()
		if (openFences % 2 == 1) return true
		return !enders.contains(last)
	}

	/** ادامه‌ی پاسخi ke nesfe mande */
	fun continueMessage(messageId: String) {
		val s = _state.value
		val conversation = s.current ?: return
		val provider = s.providerOf(conversation)
		if (provider == null) {
			showToast("API انتخاب نشده")
			return
		}
		val message = conversation.messages.firstOrNull { it.id == messageId } ?: return
		val existing = message.activeText
		val history = historyFor(conversation.id, messageId) + message.copy(content = existing) +
			ChatMessage(role = "user", content = "Continue exactly from where you stopped. Do not repeat previous text.")
		val usedModel = conversation.model.ifBlank { provider.defaultModel }
		_state.value = _state.value.copy(isGenerating = true)
		GenerationService.start(getApplication(), "AiGate", "ادامه‌ی پاسخ")
		generationJob?.cancel()
		generationJob = viewModelScope.launch {
			val result = client.complete(
				provider,
				usedModel,
				s.settings,
				buildSystemPrompt(),
				history,
			)
			result.fold(
				onSuccess = { extra ->
					val joined = existing.trimEnd() + "\n" + extra.trimStart()
					setMessageText(conversation.id, messageId, joined, message.variants.isNotEmpty())
					updateConversation(conversation.id) { c ->
						c.copy(
							messages = c.messages.map {
								if (it.id == messageId) {
									it.copy(truncated = looksTruncated(joined))
								} else {
									it
								}
							}
						)
					}
				},
				onFailure = { error -> showToast("ادامه نشد: " + (error.message ?: "khata")) },
			)
			_state.value = _state.value.copy(isGenerating = false)
			GenerationService.stop(getApplication())
			persist()
		}
	}

	// ---------------- مقایسه دو مدل ----------------

	fun setComparePrompt(prompt: String) {
		_state.value = _state.value.copy(compare = _state.value.compare.copy(prompt = prompt))
	}

	fun setCompareSide(isLeft: Boolean, providerId: String, model: String) {
		val compare = _state.value.compare
		val side = if (isLeft) compare.left else compare.right
		val updated = side.copy(providerId = providerId, model = model)
		_state.value = _state.value.copy(
			compare = if (isLeft) compare.copy(left = updated) else compare.copy(right = updated)
		)
	}

	fun runCompare() {
		val s = _state.value
		val compare = s.compare
		if (compare.prompt.isBlank()) {
			showToast("اول پرامپت را بنویس")
			return
		}
		runCompareSide(true)
		runCompareSide(false)
	}

	private fun runCompareSide(isLeft: Boolean) {
		val s = _state.value
		val compare = s.compare
		val side = if (isLeft) compare.left else compare.right
		val provider = s.providerById(side.providerId)
		if (provider == null) {
			showToast("برای هر دو طرف API انتخاب کن")
			return
		}
		setCompareResult(isLeft) { it.copy(running = true, text = "", error = null) }
		val started = System.currentTimeMillis()
		val message = ChatMessage(role = "user", content = compare.prompt)
		viewModelScope.launch {
			val result = client.complete(
				provider,
				side.model.ifBlank { provider.defaultModel },
				s.settings,
				buildSystemPrompt(),
				listOf(message),
			)
			val elapsed = System.currentTimeMillis() - started
			result.fold(
				onSuccess = { text ->
					val promptTokens = TokenCounter.countMessage(message)
					val completionTokens = TokenCounter.countText(text)
					setCompareResult(isLeft) {
						it.copy(
							running = false,
							text = text,
							tokens = promptTokens + completionTokens,
							cost = TokenCounter.estimateCost(
							provider,
							side.model.ifBlank { provider.defaultModel },
							promptTokens,
							completionTokens,
						),
							elapsedMs = elapsed,
						)
					}
				},
				onFailure = { error ->
					setCompareResult(isLeft) {
						it.copy(running = false, error = error.message ?: "خطا", elapsedMs = elapsed)
					}
				},
			)
		}
	}

	private fun setCompareResult(isLeft: Boolean, transform: (CompareSide) -> CompareSide) {
		val compare = _state.value.compare
		_state.value = _state.value.copy(
			compare = if (isLeft) {
				compare.copy(left = transform(compare.left))
			} else {
				compare.copy(right = transform(compare.right))
			}
		)
	}
}
