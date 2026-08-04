package com.aigate.chat.model

import kotlinx.serialization.Serializable
import java.util.UUID

enum class AttachmentKind { IMAGE, FILE }

/** نوع API: سازگار با OpenAI یا سازگار با Anthropic */
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
	/** نسخه‌های مختلف پاسخ (تولید دوباره) */
	val variants: List<String> = emptyList(),
	val variantIndex: Int = 0,
	val promptTokens: Int = 0,
	val completionTokens: Int = 0,
	val costUsd: Double = 0.0,
	val modelName: String = "",
) {
	/** متن فعالِ نمایش‌داده‌شده با در نظر گرفتن نسخه‌ی انتخابی */
	val activeText: String
		get() = if (variants.isEmpty()) content else variants.getOrElse(variantIndex) { content }

	val variantCount: Int
		get() = if (variants.isEmpty()) 1 else variants.size
}

/** یک سرویس‌دهنده‌ی API */
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
	/** قیمت تقریبی به دلار برای هر یک میلیون توکن */
	val inputPricePerM: Double = 0.0,
	val outputPricePerM: Double = 0.0,
	val createdAt: Long = System.currentTimeMillis(),
)

enum class ConnectionState { UNKNOWN, TESTING, ONLINE, OFFLINE }

/** وضعیت اتصال (فقط در حافظه، ذخیره نمی‌شود) */
data class ProviderStatus(
	val state: ConnectionState = ConnectionState.UNKNOWN,
	val latencyMs: Long = 0L,
	val message: String = "",
)

@Serializable
data class Conversation(
	val id: String = UUID.randomUUID().toString(),
	val title: String = "گفتگوی جدید",
	val providerId: String = "",
	val model: String = "",
	val messages: List<ChatMessage> = emptyList(),
	val updatedAt: Long = System.currentTimeMillis(),
	val pinned: Boolean = false,
	/** شاخه‌ای کردن گفتگو */
	val parentId: String? = null,
	val branchedFromMessageId: String? = null,
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

@Serializable
data class AppSettings(
	val systemPrompt: String = "You are a helpful assistant. Always answer in the user's language.",
	val temperature: Float = 0.7f,
	val streaming: Boolean = true,
	val sendFilesAsBase64: Boolean = true,
	val darkMode: Boolean = false,
	/** ایندکس تم رنگی */
	val themeIndex: Int = 0,
	val hapticsEnabled: Boolean = true,
	val showTokenStats: Boolean = true,
	/** حافظه‌ی بلندمدت */
	val memoryEnabled: Boolean = true,
	val memory: List<MemoryItem> = emptyList(),
	/** در صورت خطا، پیشنهاد استفاده از API دیگر */
	val askFallback: Boolean = true,
	val maxTokens: Int = 4096,
)

@Serializable
data class AppState(
	val providers: List<Provider> = emptyList(),
	val conversations: List<Conversation> = emptyList(),
	val settings: AppSettings = AppSettings(),
	val currentConversationId: String = "",
)
