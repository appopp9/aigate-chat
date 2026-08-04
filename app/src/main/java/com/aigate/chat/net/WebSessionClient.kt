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
 * karbar yek bar dar safheye "vorood be site" login mikonad, cookie ha zakhire mishavand
 * va bad az an har payam be sooratee khodkar dar site type va ersal mishavad
 * va pasokh az DOM khande mishavad.
 *
 * hoshdar: in ravesh be sakhtare HTML site vabaste ast va mitavanad ba har taghire site beshkanad.
 */
object WebSessionClient {

	const val DEFAULT_SITE = "https://chat.deepseek.com/"

	private const val USER_AGENT =
		"Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) " +
			"Chrome/124.0.0.0 Mobile Safari/537.36"

	private var headless: WebView? = null
	private var loadedUrl: String = ""

	@SuppressLint("SetJavaScriptEnabled")
	fun configure(view: WebView) {
		val settings = view.settings
		settings.javaScriptEnabled = true
		settings.domStorageEnabled = true
		settings.databaseEnabled = true
		settings.userAgentString = USER_AGENT
		settings.loadsImagesAutomatically = true
		settings.mediaPlaybackRequiresUserGesture = true
		settings.cacheMode = WebSettings.LOAD_DEFAULT
		settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
		view.webChromeClient = WebChromeClient()
		view.webViewClient = WebViewClient()
		val cookies = CookieManager.getInstance()
		cookies.setAcceptCookie(true)
		cookies.setAcceptThirdPartyCookies(view, true)
	}

	fun persistCookies() {
		CookieManager.getInstance().flush()
	}

	private suspend fun webView(context: Context, url: String): WebView =
		withContext(Dispatchers.Main) {
			val existing = headless
			val view = if (existing != null) {
				existing
			} else {
				val created = WebView(context.applicationContext)
				configure(created)
				headless = created
				created
			}
			if (loadedUrl != url) {
				loadedUrl = url
				view.loadUrl(url)
			}
			view
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

	/** script paye ke tavabe komaki ra roye safhe nasb mikonad. */
	private val BOOTSTRAP = """
(function(){
  function inputEl(){
    return document.querySelector('textarea#chat-input')
      || document.querySelector('textarea')
      || document.querySelector('[contenteditable="true"]');
  }
  function nodes(){
    var list = document.querySelectorAll('div.ds-markdown');
    if (list.length === 0) { list = document.querySelectorAll('div[class*="markdown"]'); }
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
  window.__aigateCount = function(){ return String(nodes().length); };
  window.__aigateText = function(){
    var list = nodes();
    if (list.length === 0){ return ''; }
    return nodeText(list[list.length - 1]);
  };
  window.__aigateBusy = function(){
    var all = document.querySelectorAll('div[role="button"], button, [aria-label]');
    for (var i = 0; i < all.length; i++){
      var label = (all[i].getAttribute('aria-label') || '') + ' ' + (all[i].className || '');
      if (label.toLowerCase().indexOf('stop') >= 0){ return '1'; }
    }
    return '0';
  };
  window.__aigateDeepThink = function(on){
    var all = document.querySelectorAll('div[role="button"], button, span');
    for (var i = 0; i < all.length; i++){
      var text = (all[i].innerText || '').toLowerCase();
      if (text.indexOf('deepthink') >= 0 || text.indexOf('deep think') >= 0){
        var active = (all[i].className || '').toLowerCase().indexOf('active') >= 0
          || all[i].getAttribute('aria-pressed') === 'true';
        if ((on === '1' && !active) || (on === '0' && active)){ all[i].click(); }
        return 'toggled';
      }
    }
    return 'not-found';
  };
  window.__aigateSend = function(text){
    var el = inputEl();
    if (!el){ return 'no-input'; }
    el.focus();
    if (el.tagName === 'TEXTAREA'){
      var desc = Object.getOwnPropertyDescriptor(window.HTMLTextAreaElement.prototype, 'value');
      if (desc && desc.set){ desc.set.call(el, text); } else { el.value = text; }
      el.dispatchEvent(new Event('input', { bubbles: true }));
    } else {
      el.innerText = text;
      el.dispatchEvent(new Event('input', { bubbles: true }));
    }
    setTimeout(function(){
      var clicked = false;
      var btns = document.querySelectorAll('div[role="button"], button');
      for (var i = btns.length - 1; i >= 0; i--){
        var b = btns[i];
        var label = ((b.getAttribute('aria-label') || '') + ' ' + (b.className || '')).toLowerCase();
        if (label.indexOf('send') >= 0 || label.indexOf('submit') >= 0){
          b.click();
          clicked = true;
          break;
        }
      }
      if (!clicked){
        var down = new KeyboardEvent('keydown', { key: 'Enter', code: 'Enter', keyCode: 13, which: 13, bubbles: true });
        el.dispatchEvent(down);
        var up = new KeyboardEvent('keyup', { key: 'Enter', code: 'Enter', keyCode: 13, which: 13, bubbles: true });
        el.dispatchEvent(up);
      }
    }, 150);
    return 'ok';
  };
  return 'installed';
})();
"""

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
			var ready = false
			var waited = 0
			while (waited < 25000) {
				eval(view, BOOTSTRAP)
				if (eval(view, "window.__aigateReady()") == "1") {
					ready = true
					break
				}
				delay(700)
				waited += 700
			}
			if (ready) {
				PingResult(true, System.currentTimeMillis() - start, "نشست وب فعال است")
			} else {
				PingResult(false, System.currentTimeMillis() - start, "وارد حساب نشده‌ای — از دکمه «ورود به سایت» لاگین کن")
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

		// montazer mimanim ta safhe amade shavad
		var ready = false
		var waited = 0
		while (waited < 30000) {
			eval(view, BOOTSTRAP)
			if (eval(view, "window.__aigateReady()") == "1") {
				ready = true
				break
			}
			delay(700)
			waited += 700
		}
		if (!ready) {
			emit(
				StreamEvent.Failure(
					"صفحه‌ی سایت آماده نشد. یا لاگین نکرده‌ای یا کپچا نشان داده شده — " +
						"از بخش API‌ها دکمه «ورود به سایت» را بزن"
				)
			)
			return@flow
		}

		val wantsDeepThink = model.contains("R1", ignoreCase = true) ||
			model.contains("reason", ignoreCase = true) ||
			model.contains("think", ignoreCase = true)
		eval(view, "window.__aigateDeepThink('" + (if (wantsDeepThink) "1" else "0") + "')")
		delay(250)

		val baseCount = eval(view, "window.__aigateCount()").toIntOrNull() ?: 0
		val sendResult = eval(view, "window.__aigateSend(" + jsString(prompt) + ")")
		if (sendResult != "ok") {
			emit(StreamEvent.Failure("کادر نوشتن پیام در سایت پیدا نشد (ساختار سایت تغییر کرده)"))
			return@flow
		}

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
					emit(StreamEvent.Failure("سایت پاسخی تولید نکرد"))
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
			if (!busy && stableFor >= 1800) break
			if (busy) stableFor = 0
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
		val view = headless
		headless = null
		loadedUrl = ""
		view?.destroy()
	}
}
