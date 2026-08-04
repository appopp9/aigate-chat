package com.aigate.chat.net

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.aigate.chat.model.ChatMessage
import com.aigate.chat.model.Provider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * halate sevvom: goftogoo az tarighe neshaste morourgar (WebView).
 *
 * nokteye mohem: WebView bayad be panjere vasl bashad, vagarna Chromium
 * requestAnimationFrame ra ejra nemikonad va site (React) hargez hydrate nemishavad.
 * baraye hamin MainActivity yek WebView makhfi (1dp) misazad va inja sabt mikonad.
 */
object WebSessionClient {

	const val DEFAULT_SITE = "https://chat.deepseek.com/"

	private const val USER_AGENT =
		"Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) " +
			"Chrome/124.0.0.0 Mobile Safari/537.36"

	private var host: WebView? = null
	private var fallback: WebView? = null
	private var lastLog: String = ""

	fun log(): String = lastLog

	@SuppressLint("SetJavaScriptEnabled")
	fun configure(view: WebView) {
		val settings = view.settings
		settings.javaScriptEnabled = true
		settings.domStorageEnabled = true
		settings.databaseEnabled = true
		settings.userAgentString = USER_AGENT
		settings.loadsImagesAutomatically = true
		settings.mediaPlaybackRequiresUserGesture = true
		settings.javaScriptCanOpenWindowsAutomatically = true
		settings.cacheMode = WebSettings.LOAD_DEFAULT
		settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
		view.webChromeClient = WebChromeClient()
		view.webViewClient = WebViewClient()
		val cookies = CookieManager.getInstance()
		cookies.setAcceptCookie(true)
		cookies.setAcceptThirdPartyCookies(view, true)
	}

	/** WebView makhfi va vasl-shode be panjere ra sabt mikonad. */
	fun attachHost(view: WebView) {
		host = view
	}

	fun detachHost(view: WebView) {
		if (host === view) host = null
	}

	fun persistCookies() {
		CookieManager.getInstance().flush()
	}

	private suspend fun webView(context: Context, url: String): WebView =
		withContext(Dispatchers.Main) {
			val attached = host
			val view = if (attached != null) {
				attached
			} else {
				val existing = fallback
				if (existing != null) {
					existing
				} else {
					val created = WebView(context.applicationContext)
					configure(created)
					fallback = created
					created
				}
			}
			val currentUrl = view.url
			val needsLoad = currentUrl.isNullOrBlank() ||
				currentUrl == "about:blank" ||
				!sameHost(currentUrl, url)
			if (needsLoad) view.loadUrl(url)
			view
		}

	private fun sameHost(a: String, b: String): Boolean {
		fun hostOf(value: String): String {
			val withoutScheme = value.substringAfter("//", value)
			return withoutScheme.substringBefore("/").lowercase()
		}
		return hostOf(a) == hostOf(b)
	}

	private suspend fun eval(view: WebView, script: String): String =
		withContext(Dispatchers.Main) {
			suspendCancellableCoroutine { continuation ->
				view.evaluateJavascript(script) { raw ->
					continuation.resume(unquote(raw))
				}
			}
		}

	private fun unquote(raw: String?): String {
		if (raw == null || raw == "null") return ""
		var value = raw
		if (value.length >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
			value = value.substring(1, value.length - 1)
		}
		return value
			.replace("\\n", "\n")
			.replace("\\t", "\t")
			.replace("\\r", "")
			.replace("\\\"", "\"")
			.replace("\\\\", "\\")
			.replace("\\u003C", "<")
			.replace("\\u003E", ">")
			.replace("\\u0026", "&")
	}

	private fun jsString(value: String): String {
		val escaped = value
			.replace("\\", "\\\\")
			.replace("\"", "\\\"")
			.replace("\n", "\\n")
			.replace("\r", "")
		return "\"" + escaped + "\""
	}

	/** tavabe komaki ra roye safhe nasb mikonad. */
	private val BOOTSTRAP = """
(function(){
  function inputEl(){
    return document.querySelector('textarea#chat-input')
      || document.querySelector('textarea')
      || document.querySelector('[contenteditable="true"]');
  }
  function nodes(){
    var list = document.querySelectorAll('div.ds-markdown');
    if (list.length === 0){ list = document.querySelectorAll('div[class*="markdown"]'); }
    return list;
  }
  function nodeText(root){
    var out = '';
    var kids = root.childNodes;
    for (var i = 0; i < kids.length; i++){
      var k = kids[i];
      if (k.nodeType === 3){ out += k.nodeValue; continue; }
      if (k.nodeType !== 1){ continue; }
      var tag = k.tagName.toLowerCase();
      if (tag === 'pre'){
        var codeEl = k.querySelector('code');
        var lang = '';
        if (codeEl && codeEl.className){
          var m = codeEl.className.match(/language-([a-zA-Z0-9+#_-]+)/);
          if (m){ lang = m[1]; }
        }
        var body = codeEl ? codeEl.innerText : k.innerText;
        out += '\n' + '```' + lang + '\n' + body + '\n' + '```' + '\n';
      } else if (tag === 'br'){
        out += '\n';
      } else if (tag === 'button' || tag === 'svg'){
        out += '';
      } else {
        out += nodeText(k);
        if (tag === 'p' || tag === 'div' || tag === 'li' || tag === 'h1' || tag === 'h2' || tag === 'h3' || tag === 'h4' || tag === 'tr'){
          out += '\n';
        }
      }
    }
    return out;
  }
  window.__aigateReady = function(){ return inputEl() ? '1' : '0'; };
  window.__aigateDiag = function(){
    var el = inputEl();
    return 'url=' + location.href
      + ' | input=' + (el ? el.tagName : 'none')
      + ' | blocks=' + nodes().length
      + ' | buttons=' + document.querySelectorAll('div[role="button"], button').length;
  };
  window.__aigateCount = function(){ return String(nodes().length); };
  window.__aigateText = function(){
    var list = nodes();
    if (list.length === 0){ return ''; }
    return nodeText(list[list.length - 1]);
  };
  window.__aigateBusy = function(){
    var all = document.querySelectorAll('div[role="button"], button, [aria-label]');
    for (var i = 0; i < all.length; i++){
      var label = ((all[i].getAttribute('aria-label') || '') + ' ' + (all[i].className || '')).toLowerCase();
      if (label.indexOf('stop') >= 0){ return '1'; }
    }
    return '0';
  };
  window.__aigateValue = function(){
    var el = inputEl();
    if (!el){ return ''; }
    return (el.tagName === 'TEXTAREA' ? (el.value || '') : (el.innerText || ''));
  };
  window.__aigateType = function(text){
    var el = inputEl();
    if (!el){ return 'no-input'; }
    el.focus();
    el.click();
    if (el.tagName === 'TEXTAREA'){
      var desc = Object.getOwnPropertyDescriptor(window.HTMLTextAreaElement.prototype, 'value');
      if (desc && desc.set){ desc.set.call(el, text); } else { el.value = text; }
    } else {
      el.innerText = text;
    }
    el.dispatchEvent(new Event('input', { bubbles: true }));
    el.dispatchEvent(new Event('change', { bubbles: true }));
    return 'ok';
  };
  window.__aigateEnter = function(){
    var el = inputEl();
    if (!el){ return 'no-input'; }
    el.focus();
    var opts = { key: 'Enter', code: 'Enter', keyCode: 13, which: 13, bubbles: true, cancelable: true };
    el.dispatchEvent(new KeyboardEvent('keydown', opts));
    el.dispatchEvent(new KeyboardEvent('keypress', opts));
    el.dispatchEvent(new KeyboardEvent('keyup', opts));
    return 'ok';
  };
  window.__aigateClickSend = function(){
    var el = inputEl();
    var scope = document;
    if (el){
      var p = el.parentElement;
      for (var d = 0; d < 5 && p; d++){ p = p.parentElement; }
      if (p){ scope = p; }
    }
    var cands = scope.querySelectorAll('div[role="button"], button, [aria-disabled]');
    var labelled = null;
    for (var i = 0; i < cands.length; i++){
      var label = ((cands[i].getAttribute('aria-label') || '') + ' ' + (cands[i].className || '')).toLowerCase();
      if (label.indexOf('send') >= 0 || label.indexOf('submit') >= 0){ labelled = cands[i]; }
    }
    if (labelled){ labelled.click(); return 'clicked-label'; }
    var last = null;
    for (var j = 0; j < cands.length; j++){
      var c = cands[j];
      if (c.getAttribute('aria-disabled') === 'true'){ continue; }
      if (c.querySelector('svg') || c.getAttribute('role') === 'button'){ last = c; }
    }
    if (last){ last.click(); return 'clicked-last'; }
    return 'no-button';
  };
  return 'installed';
})();
"""

	private suspend fun install(view: WebView) {
		eval(view, BOOTSTRAP)
	}

	private suspend fun waitReady(view: WebView, timeoutMs: Int): Boolean {
		var waited = 0
		while (waited < timeoutMs) {
			install(view)
			if (eval(view, "window.__aigateReady()") == "1") return true
			delay(600)
			waited += 600
		}
		return false
	}

	private fun promptFor(history: List<ChatMessage>): String {
		val last = history.lastOrNull { it.role == "user" }
		return last?.activeText?.trim().orEmpty()
	}

	/** vaziyate login ra check mikonad. */
	suspend fun checkSession(context: Context, provider: Provider): PingResult {
		val start = System.currentTimeMillis()
		val url = provider.baseUrl.ifBlank { DEFAULT_SITE }
		return try {
			val view = webView(context, url)
			val ready = waitReady(view, 30000)
			val diag = eval(view, "window.__aigateDiag()")
			lastLog = diag
			if (ready) {
				PingResult(true, System.currentTimeMillis() - start, "نشست وب فعال است")
			} else {
				PingResult(
					false,
					System.currentTimeMillis() - start,
					"کادر چت پیدا نشد — احتمالاً لاگین نشده‌ای یا کپچا باز است. " + diag,
				)
			}
		} catch (t: Throwable) {
			PingResult(false, System.currentTimeMillis() - start, t.message ?: "خطای وب‌ویو")
		}
	}

	/** payam ra dar site ersal mikonad va pasokh ra be soorate stream barmigardanad. */
	fun streamChat(
		context: Context,
		provider: Provider,
		model: String,
		history: List<ChatMessage>,
	): Flow<StreamEvent> = flow {
		val prompt = promptFor(history)
		if (prompt.isEmpty()) {
			emit(StreamEvent.Failure("پیامی برای ارسال پیدا نشد"))
			return@flow
		}
		val url = provider.baseUrl.ifBlank { DEFAULT_SITE }
		val view = webView(context, url)

		if (!waitReady(view, 40000)) {
			val diag = eval(view, "window.__aigateDiag()")
			lastLog = diag
			emit(
				StreamEvent.Failure(
					"صفحه‌ی سایت آماده نشد. از بخش API‌ها دکمه‌ی «ورود به سایت» را بزن و مطمئن شو لاگین هستی. " + diag
				)
			)
			return@flow
		}

		val wantsDeepThink = model.contains("R1", ignoreCase = true) ||
			model.contains("reason", ignoreCase = true) ||
			model.contains("think", ignoreCase = true)
		if (wantsDeepThink) {
			eval(view, "window.__aigateDeepThink ? window.__aigateDeepThink('1') : ''")
			delay(250)
		}

		val baseCount = eval(view, "window.__aigateCount()").toIntOrNull() ?: 0

		// 1) neveshtan dar kadr
		val typed = eval(view, "window.__aigateType(" + jsString(prompt) + ")")
		delay(300)
		if (typed != "ok" || eval(view, "window.__aigateValue()").isBlank()) {
			val diag = eval(view, "window.__aigateDiag()")
			lastLog = diag
			emit(StreamEvent.Failure("نمی‌توانم در کادر چت سایت بنویسم. " + diag))
			return@flow
		}

		// 2) Enter, va agar kar nakard click roye dokmeye ersal
		eval(view, "window.__aigateEnter()")
		delay(700)
		var sent = eval(view, "window.__aigateValue()").isBlank()
		if (!sent) {
			val clicked = eval(view, "window.__aigateClickSend()")
			delay(900)
			sent = eval(view, "window.__aigateValue()").isBlank()
			if (!sent) {
				eval(view, "window.__aigateEnter()")
				delay(900)
				sent = eval(view, "window.__aigateValue()").isBlank()
			}
			lastLog = "click=" + clicked + " | " + eval(view, "window.__aigateDiag()")
		}
		if (!sent) {
			val diag = eval(view, "window.__aigateDiag()")
			lastLog = diag
			emit(StreamEvent.Failure("دکمه‌ی ارسال سایت پیدا نشد (ساختار سایت عوض شده). " + diag))
			return@flow
		}

		// 3) khandane pasokh
		var emitted = ""
		var stableFor = 0
		var elapsed = 0
		var started = false
		while (elapsed < 300000) {
			delay(450)
			elapsed += 450
			val count = eval(view, "window.__aigateCount()").toIntOrNull() ?: 0
			if (count <= baseCount) {
				if (elapsed > 60000) {
					lastLog = eval(view, "window.__aigateDiag()")
					emit(StreamEvent.Failure("پیام ارسال شد ولی سایت پاسخی نشان نداد. " + lastLog))
					return@flow
				}
				continue
			}
			val text = eval(view, "window.__aigateText()").trimStart()
			if (text.isEmpty()) continue
			started = true
			if (text.length > emitted.length && text.startsWith(emitted)) {
				emit(StreamEvent.Delta(text.substring(emitted.length)))
				emitted = text
				stableFor = 0
			} else if (text != emitted) {
				emit(StreamEvent.Delta("\n" + text))
				emitted = text
				stableFor = 0
			} else {
				stableFor += 450
			}
			val busy = eval(view, "window.__aigateBusy()") == "1"
			if (busy) {
				stableFor = 0
			} else if (stableFor >= 1800) {
				break
			}
			if (stableFor >= 6000) break
		}
		persistCookies()
		if (!started) {
			emit(StreamEvent.Failure("پاسخی از سایت خوانده نشد"))
		} else {
			emit(StreamEvent.Done)
		}
	}

	suspend fun complete(
		context: Context,
		provider: Provider,
		model: String,
		history: List<ChatMessage>,
	): Result<String> {
		val builder = StringBuilder()
		var failure: String? = null
		streamChat(context, provider, model, history).collect { event ->
			when (event) {
				is StreamEvent.Delta -> builder.append(event.text)
				is StreamEvent.Failure -> failure = event.message
				else -> Unit
			}
		}
		val error = failure
		return if (builder.isNotEmpty()) {
			Result.success(builder.toString())
		} else {
			Result.failure(IllegalStateException(error ?: "پاسخی دریافت نشد"))
		}
	}

	fun release() {
		val view = fallback
		fallback = null
		view?.destroy()
	}
}
