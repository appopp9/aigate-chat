package com.aigate.chat.model

import kotlinx.serialization.Serializable
import java.util.UUID

enum class AttachmentKind { IMAGE, FILE }

/** no'e API: sazegar ba OpenAI ya Anthropic */
enum class ProviderType { OPENAI, ANTHROPIC }

@Serializable
data class Attachment(
	val id: String = UUID.randomUUID().toString(),
	val name: String = "file",
	val mimeType: String = "application/octet-stream",
	val sizeBytes: Long = 0L,
	val kind: AttachmentKind = AttachmentKind.FILE,
	val dataUrl: String? = null,
	val textContent: String? = null,
	val localUri: String? = null,
)

@Serializable
data class ChatMessage(
	val id: String = UUID.randomUUID().toString(),
	val role: String = "user",
	val content: String = "",
	val attachments: List<Attachment> = emptyList(),
	val createdAt: Long = System.currentTimeMillis(),
	val error: String? = null,
	/** nosakhe haye mokhtalefe pasokh (tolide dobare) */
	val variants: List<String> = emptyList(),
	val variantIndex: Int = 0,
	val promptTokens: Int = 0,
	val completionTokens: Int = 0,
	val costUsd: Double = 0.0,
	val modelName: String = "",
	/** payam sanjagh shode */
	val pinned: Boolean = false,
	/** agar pasokh nesfe mande bashad (max_tokens) */
	val truncated: Boolean = false,
) {
	/** matne fa'al ba dar nazar gereftane nosakhe entekhabi */
	val activeText: String
		get() = if (variants.isEmpty()) content else variants.getOrElse(variantIndex) { content }

	val variantCount: Int
		get() = if (variants.isEmpty()) 1 else variants.size
}

/** gheymate har model be dollar baraye har yek million token */
@Serializable
data class ModelPricing(
	val model: String = "",
	val inputPricePerM: Double = 0.0,
	val outputPricePerM: Double = 0.0,
)

/** yek servis dahandeye API */
@Serializable
data class Provider(
	val id: String = UUID.randomUUID().toString(),
	val name: String = "",
	val baseUrl: String = "",
	val authKey: String = "",
	val type: ProviderType = ProviderType.OPENAI,
	val models: List<String> = emptyList(),
	val favoriteModels: List<String> = emptyList(),
	val defaultModel: String = "",
	val colorIndex: Int = 0,
	/** gheymate pishfarz (vaghti model gheymate ekhtesasi nadarad) */
	val inputPricePerM: Double = 0.0,
	val outputPricePerM: Double = 0.0,
	/** gheymate ekhtesasi be ezaye har model */
	val modelPricing: List<ModelPricing> = emptyList(),
	val createdAt: Long = System.currentTimeMillis(),
) {
	fun pricingFor(model: String): ModelPricing {
		val found = modelPricing.firstOrNull { it.model == model }
		return found ?: ModelPricing(model, inputPricePerM, outputPricePerM)
	}
}

enum class ConnectionState { UNKNOWN, TESTING, ONLINE, OFFLINE }

/** vaz'iate ettesal (faghat dar hafeze, zakhire nemishavad) */
data class ProviderStatus(
	val state: ConnectionState = ConnectionState.UNKNOWN,
	val latencyMs: Long = 0L,
	val message: String = "",
)

/** naghsh / shakhsiat */
@Serializable
data class Persona(
	val id: String = UUID.randomUUID().toString(),
	val name: String = "",
	val emoji: String = "🤖",
	val systemPrompt: String = "",
	val providerId: String = "",
	val model: String = "",
	val temperature: Float = -1f,
	val createdAt: Long = System.currentTimeMillis(),
)

/** pramte amade */
@Serializable
data class PromptItem(
	val id: String = UUID.randomUUID().toString(),
	val title: String = "",
	val body: String = "",
	val shortcut: String = "",
	val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
data class Conversation(
	val id: String = UUID.randomUUID().toString(),
	val title: String = "goftogooye jadid",
	val providerId: String = "",
	val model: String = "",
	val messages: List<ChatMessage> = emptyList(),
	val updatedAt: Long = System.currentTimeMillis(),
	val pinned: Boolean = false,
	/** shakhe'i kardane goftogoo */
	val parentId: String? = null,
	val branchedFromMessageId: String? = null,
	/** naghshe entekhab shode baraye in goftogoo */
	val personaId: String = "",
	/** kholaseye sanjagh shode dar bala */
	val summary: String = "",
) {
	val promptTokens: Int get() = messages.sumOf { it.promptTokens }
	val completionTokens: Int get() = messages.sumOf { it.completionTokens }
	val totalTokens: Int get() = promptTokens + completionTokens
	val totalCost: Double get() = messages.sumOf { it.costUsd }
}

@Serializable
data class MemoryItem(
	val id: String = UUID.randomUUID().toString(),
	val text: String = "",
	val enabled: Boolean = true,
)

/** masrafe mahane baraye budje */
@Serializable
data class UsageRecord(
	val month: String = "",
	val providerId: String = "",
	val model: String = "",
	val promptTokens: Int = 0,
	val completionTokens: Int = 0,
	val costUsd: Double = 0.0,
)

enum class FontScale { SMALL, MEDIUM, LARGE }

@Serializable
data class AppSettings(
	val systemPrompt: String = "You are a helpful assistant. Always answer in the user's language.",
	val temperature: Float = 0.7f,
	val streaming: Boolean = true,
	val sendFilesAsBase64: Boolean = true,
	val darkMode: Boolean = false,
	/** indexe teme rangi */
	val themeIndex: Int = 0,
	val hapticsEnabled: Boolean = true,
	val showTokenStats: Boolean = true,
	/** hafezeye bolandmodat */
	val memoryEnabled: Boolean = true,
	val memory: List<MemoryItem> = emptyList(),
	/** dar soorate khata, pishnahade estefade az API digar */
	val askFallback: Boolean = true,
	val maxTokens: Int = 4096,
	/** andazeye font goftogoo */
	val fontScale: FontScale = FontScale.MEDIUM,
	/** ghofle app ba asare angosht */
	val appLockEnabled: Boolean = false,
	/** budje mahane be dollar; sefr = bedoone mahdoodiat */
	val monthlyBudgetUsd: Double = 0.0,
	/** darsadi ke hoshdar dade mishavad */
	val budgetWarnPercent: Int = 80,
	/** vaghti budje tamam shod, ersal motevaghef shavad */
	val blockOverBudget: Boolean = false,
)

@Serializable
data class AppState(
	val providers: List<Provider> = emptyList(),
	val conversations: List<Conversation> = emptyList(),
	val settings: AppSettings = AppSettings(),
	val currentConversationId: String = "",
	val personas: List<Persona> = emptyList(),
	val prompts: List<PromptItem> = emptyList(),
	val usage: List<UsageRecord> = emptyList(),
)
