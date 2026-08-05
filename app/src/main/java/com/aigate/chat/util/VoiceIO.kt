package com.aigate.chat.util

import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * khandan e pasokh ba seda (TTS) va sakhtane intent e goftar be matn (STT).
 * STT az intent e sistem estefade mikonad pas ejaze ye zabt e seda lazem nist.
 */
object VoiceIO {

	private var tts: TextToSpeech? = null
	private var ready = false

	fun init(context: Context) {
		if (tts != null) return
		tts = TextToSpeech(context.applicationContext) { status ->
			ready = status == TextToSpeech.SUCCESS
			val engine = tts
			if (ready && engine != null) {
				val fa = Locale("fa", "IR")
				if (engine.isLanguageAvailable(fa) >= TextToSpeech.LANG_AVAILABLE) {
					engine.language = fa
				} else {
					engine.language = Locale.getDefault()
				}
				engine.setSpeechRate(1.0f)
			}
		}
	}

	fun speak(context: Context, text: String): Boolean {
		init(context)
		val engine = tts ?: return false
		if (!ready) return false
		val clean = stripMarkdown(text)
		if (clean.isBlank()) return false
		engine.stop()
		engine.speak(clean.take(3800), TextToSpeech.QUEUE_FLUSH, null, "aigate")
		return true
	}

	fun stop() {
		tts?.stop()
	}

	fun isSpeaking(): Boolean = tts?.isSpeaking == true

	fun release() {
		val engine = tts
		tts = null
		ready = false
		engine?.stop()
		engine?.shutdown()
	}

	/** matn ra baraye khandan tamiz mikonad. */
	fun stripMarkdown(raw: String): String {
		var out = raw
		out = out.replace(Regex("```[\\s\\S]*?```"), " کد داده شده است. ")
		out = out.replace(Regex("`([^`]*)`"), "$1")
		out = out.replace(Regex("[*_>#|]"), " ")
		out = out.replace(Regex("\\[(.*?)\\]\\((.*?)\\)"), "$1")
		out = out.replace(Regex("[ \\t]+"), " ")
		return out.trim()
	}

	/** intent e tabdil e goftar be matn. */
	fun speechIntent(): Intent {
		val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
		intent.putExtra(
			RecognizerIntent.EXTRA_LANGUAGE_MODEL,
			RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
		)
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
		intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "بگو تا بنویسم…")
		intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
		return intent
	}

	fun textFromResult(data: Intent?): String {
		val list = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
		return list?.firstOrNull().orEmpty()
	}
}
