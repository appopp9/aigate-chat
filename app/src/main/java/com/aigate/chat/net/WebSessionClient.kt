package com.aigate.chat.net

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
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
 * v8.1:
 *  - hook e shabake dar onPageStarted tazrigh mishavad (ghabl az ejraye bundle e site),
 *    vagarna app e site reference e ghadimi fetch ra negah midarad va shenood kar nemikonad.
 *  - fetch + XMLHttpRequest + EventSource har se hook mishavand.
 *  - khandane DOM ba textContent anjam mishavad (na innerText) chon innerText be layout
 *    vabaste ast va dar WebView e kuchak khali barmigardad.
 */
object WebSessionClient {

	const val DEFAULT_SITE = "https://chat.deepseek.com/"

	private const val USER_AGENT =
		"Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) " +
			"Chrome/124.0.0.0 Mobile Safari/537.36"

	private var host: WebView? = null
	private var fallbackView: WebView? = null
	private var lastLog: String = ""

	/** adrese goftogooye site baraye chat e feli (bad az ersal khande mishavad) */
	var lastSessionUrl: String = ""
		private set

	fun log(): String = lastLog

	data class WebCheck(val title: String, val ok: Boolean, val detail: String)

	private data class StreamState(
		val status: Int,
		val done: Boolean,
		val frames: Int,
		val calls: Int,
		val error: String,
		val text: String,
	)

	// ---------------------------------------------------------------- JS

	/** hook e shabake; bayad ghabl az script haye site ejra shavad. */
	private val NET_HOOK = """
(function(){
  if (!window.__aigateNet){
    window.__aigateNet = {
      hooked: false, early: 0, calls: 0, urls: [], allUrls: [], status: 0, frames: 0,
      text: '', think: '', done: false, err: '', lastPath: '', xhrBuf: '', xhrSeen: 0
    };
  }
  var N = window.__aigateNet;

  window.__aigateFrame = function(line){
    if (!line){ return; }
    var s = ('' + line).trim();
    if (s.indexOf('data:') === 0){ s = s.slice(5).trim(); }
    if (!s){ return; }
    if (s === '[DONE]'){ N.done = true; return; }
    var j = null;
    try { j = JSON.parse(s); } catch (e){ return; }
    N.frames++;
    if (typeof j.p === 'string' && j.p){ N.lastPath = j.p; }
    if (typeof j.v === 'string'){
      var path = (typeof j.p === 'string' && j.p) ? j.p : N.lastPath;
      if (path && path.indexOf('thinking') >= 0){ N.think += j.v; }
      else { N.text += j.v; }
      return;
    }
    if (j.choices && j.choices[0]){
      var d = j.choices[0].delta || j.choices[0].message;
      if (d){
        if (typeof d.content === 'string'){ N.text += d.content; }
        if (typeof d.reasoning_content === 'string'){ N.think += d.reasoning_content; }
      }
      if (j.choices[0].finish_reason){ N.done = true; }
    }
    if (j.error){ N.err = JSON.stringify(j.error).slice(0, 300); }
  };

  window.__aigateBegin = function(url){
    N.calls++;
    N.urls.push(String(url).slice(0, 140));
    N.text = ''; N.think = ''; N.frames = 0; N.done = false;
    N.err = ''; N.lastPath = ''; N.xhrBuf = ''; N.xhrSeen = 0;
  };

  function track(url){
    var u = String(url || '');
    if (N.allUrls.length < 60){ N.allUrls.push(u.slice(0, 90)); }
    return u.indexOf('completion') >= 0 || u.indexOf('chat/comple') >= 0;
  }

  window.__aigateHook = function(){
    if (N.hooked){ return 'already'; }

    // ---- fetch ----
    if (window.fetch){
      var origFetch = window.fetch;
      window.fetch = function(input, init){
        var url = '';
        try { url = (typeof input === 'string') ? input : ((input && input.url) || ''); } catch (e){ url = ''; }
        var isChat = track(url);
        var promise = origFetch.apply(this, arguments);
        if (!isChat){ return promise; }
        return promise.then(function(res){
          try {
            N.status = res.status;
            window.__aigateBegin(url);
            if (!res.body || !res.body.getReader){ N.done = true; return res; }
            var reader = res.clone().body.getReader();
            var dec = new TextDecoder();
            var buf = '';
            var pump = function(){
              return reader.read().then(function(r){
                if (r.done){ if (buf){ window.__aigateFrame(buf); } N.done = true; return; }
                buf += dec.decode(r.value, { stream: true });
                var idx;
                while ((idx = buf.indexOf('\n')) >= 0){
                  var line = buf.slice(0, idx);
                  buf = buf.slice(idx + 1);
                  window.__aigateFrame(line);
                }
                return pump();
              });
            };
            pump().catch(function(e){ N.err = 'read:' + e; N.done = true; });
          } catch (e){ N.err = 'hook:' + e; }
          return res;
        }).catch(function(e){
          N.err = 'fetch:' + e;
          N.done = true;
          throw e;
        });
      };
    }

    // ---- XMLHttpRequest ----
    var OrigXHR = window.XMLHttpRequest;
    if (OrigXHR && !OrigXHR.__aigate){
      var Patched = function(){
        var x = new OrigXHR();
        var reqUrl = '';
        var origOpen = x.open;
        x.open = function(m, u){
          reqUrl = u || '';
          return origOpen.apply(x, arguments);
        };
        x.addEventListener('readystatechange', function(){
          var isChat = track(reqUrl);
          if (!isChat){ return; }
          if (x.readyState === 2){
            N.status = x.status;
            window.__aigateBegin(reqUrl);
            return;
          }
          if (x.readyState >= 3){
            var raw = '';
            try { raw = x.responseText || ''; } catch (e){ return; }
            var chunk = raw.slice(N.xhrSeen);
            N.xhrSeen = raw.length;
            var buf = N.xhrBuf + chunk;
            var idx;
            while ((idx = buf.indexOf('\n')) >= 0){
              var line = buf.slice(0, idx);
              buf = buf.slice(idx + 1);
              window.__aigateFrame(line);
            }
            N.xhrBuf = buf;
            if (x.readyState === 4){
              if (N.xhrBuf){ window.__aigateFrame(N.xhrBuf); N.xhrBuf = ''; }
              N.done = true;
            }
          }
        });
        return x;
      };
      Patched.__aigate = 1;
      Patched.prototype = OrigXHR.prototype;
      Patched.UNSENT = 0;
      Patched.OPENED = 1;
      Patched.HEADERS_RECEIVED = 2;
      Patched.LOADING = 3;
      Patched.DONE = 4;
      window.XMLHttpRequest = Patched;
    }

    // ---- EventSource ----
    if (window.EventSource && !window.EventSource.__aigate){
      var OrigES = window.EventSource;
      var PatchedES = function(url, cfg){
        var es = new OrigES(url, cfg);
        if (track(url)){
          window.__aigateBegin(url);
          es.addEventListener('message', function(ev){ window.__aigateFrame(ev.data); });
          es.addEventListener('error', function(){ N.done = true; });
        }
        return es;
      };
      PatchedES.__aigate = 1;
      PatchedES.prototype = OrigES.prototype;
      window.EventSource = PatchedES;
    }

    N.hooked = true;
    return 'hooked';
  };

  window.__aigateReset = function(){
    N.text = ''; N.think = ''; N.frames = 0; N.done = false;
    N.err = ''; N.status = 0; N.calls = 0; N.urls = []; N.allUrls = [];
    N.lastPath = ''; N.xhrBuf = ''; N.xhrSeen = 0;
    return 'reset';
  };

  window.__aigateState = function(){
    return 'status=' + N.status
      + '\nhooked=' + (N.hooked ? '1' : '0')
      + '\ndone=' + (N.done ? '1' : '0')
      + '\nframes=' + N.frames
      + '\ncalls=' + N.calls
      + '\nerr=' + (N.err || '')
      + '\n---TEXT---\n' + N.text;
  };

  window.__aigateHook();
  N.early = 1;
  return 'net-hook';
})();
"""

	/** komak haye DOM; bad az bar shodane safhe tazrigh mishavad. */
	private val DOM_HELPERS = """
(function(){
  function inputEl(){
    return document.querySelector('textarea#chat-input')
      || document.querySelector('textarea')
      || document.querySelector('[contenteditable="true"]');
  }
  function txt(el){
    if (!el){ return ''; }
    return el.textContent || '';
  }
  function blocks(){
    var list = document.querySelectorAll('div.ds-markdown');
    if (list.length > 0){ return list; }
    list = document.querySelectorAll('div[class*="markdown"], div[class*="Markdown"]');
    if (list.length > 0){ return list; }
    list = document.querySelectorAll('[class*="message-content"], [class*="messageContent"], [data-message-author-role]');
    if (list.length > 0){ return list; }
    return document.querySelectorAll('[class*="message"] > div, [class*="Message"] > div');
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
        out += '\n' + '```' + lang + '\n' + txt(codeEl || k) + '\n' + '```' + '\n';
      } else if (tag === 'br'){
        out += '\n';
      } else if (tag === 'button' || tag === 'svg' || tag === 'textarea' || tag === 'input'){
        out += '';
      } else {
        out += nodeText(k);
        if (tag === 'p' || tag === 'div' || tag === 'li' || tag === 'tr'
          || tag === 'h1' || tag === 'h2' || tag === 'h3' || tag === 'h4'){
          out += '\n';
        }
      }
    }
    return out;
  }

  window.__aigateReady = function(){ return inputEl() ? '1' : '0'; };
  window.__aigateBodyLen = function(){ return String(txt(document.body).length); };

  window.__aigateMark = function(){
    var list = blocks();
    for (var i = 0; i < list.length; i++){ list[i].setAttribute('data-aigate-seen', '1'); }
    return String(list.length);
  };
  window.__aigateMarkAll = function(){
    var all = document.querySelectorAll('div,p,li,article,section,span');
    for (var i = 0; i < all.length; i++){
      all[i].setAttribute('data-aigate-seen', '1');
      all[i].setAttribute('data-aigate-len', String(txt(all[i]).length));
    }
    return String(all.length);
  };
  window.__aigateNew = function(skip){
    var list = blocks();
    var found = null;
    for (var i = 0; i < list.length; i++){
      var b = list[i];
      if (b.getAttribute('data-aigate-seen') === '1'){ continue; }
      var t = txt(b);
      if (skip && skip.length > 2 && t.indexOf(skip) >= 0 && t.length < skip.length + 30){ continue; }
      found = b;
    }
    return found ? nodeText(found) : '';
  };
  window.__aigateGrown = function(skip){
    var all = document.querySelectorAll('div,p,li,article,section');
    var best = null;
    var bestLen = 0;
    for (var i = 0; i < all.length; i++){
      var e = all[i];
      if (e.querySelector('textarea') || e.querySelector('input')){ continue; }
      var t = txt(e).trim();
      if (t.length < 2 || t.length > 20000){ continue; }
      if (skip && skip.length > 2 && t.indexOf(skip) >= 0){ continue; }
      if (e.getAttribute('data-aigate-seen') === '1'){
        var prev = parseInt(e.getAttribute('data-aigate-len') || '0', 10);
        if (t.length <= prev + 2){ continue; }
      }
      if (t.length > bestLen){ bestLen = t.length; best = e; }
    }
    return best ? nodeText(best) : '';
  };
  window.__aigateAnswer = function(skip){
    var t = window.__aigateNew(skip);
    if (t && t.trim().length > 0){ return t; }
    return window.__aigateGrown(skip);
  };
  window.__aigateBusy = function(){
    var all = document.querySelectorAll('div[role="button"], button, [aria-label], [class*="loading"]');
    for (var i = 0; i < all.length; i++){
      var label = ((all[i].getAttribute('aria-label') || '') + ' ' + (all[i].className || '')).toLowerCase();
      if (label.indexOf('stop') >= 0 || label.indexOf('loading') >= 0){ return '1'; }
    }
    return '0';
  };
  window.__aigateValue = function(){
    var el = inputEl();
    if (!el){ return ''; }
    return (el.tagName === 'TEXTAREA' ? (el.value || '') : txt(el));
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
      el.textContent = text;
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
  function sendScope(){
    var el = inputEl();
    var scope = document;
    if (el){
      var p = el.parentElement;
      for (var d = 0; d < 5 && p; d++){ p = p.parentElement; }
      if (p){ scope = p; }
    }
    return scope;
  }
  window.__aigateFindSend = function(){
    return String(sendScope().querySelectorAll('div[role="button"], button, [aria-disabled]').length);
  };
  window.__aigateClickSend = function(){
    var cands = sendScope().querySelectorAll('div[role="button"], button, [aria-disabled]');
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
  window.__aigateDeepThink = function(on){
    var all = document.querySelectorAll('div[role="button"], button, span');
    for (var i = 0; i < all.length; i++){
      var t = txt(all[i]).toLowerCase();
      if (t.indexOf('deepthink') >= 0 || t.indexOf('deep think') >= 0){
        var active = (all[i].className || '').toLowerCase().indexOf('active') >= 0
          || all[i].getAttribute('aria-pressed') === 'true';
        if ((on === '1' && !active) || (on === '0' && active)){ all[i].click(); }
        return 'toggled';
      }
    }
    return 'not-found';
  };
  window.__aigateTail = function(){
    return txt(document.body).slice(-200).replace(/\s+/g, ' ');
  };
  window.__aigateDiag = function(){
    var N = window.__aigateNet || {};
    var el = inputEl();
    return 'url=' + location.href
      + ' | input=' + (el ? el.tagName : 'none')
      + ' | blocks=' + blocks().length
      + ' | hooked=' + (N.hooked ? '1' : '0')
      + ' | early=' + (N.early || 0)
      + ' | calls=' + (N.calls || 0)
      + ' | frames=' + (N.frames || 0)
      + ' | status=' + (N.status || 0)
      + ' | netLen=' + ((N.text || '').length)
      + ' | bodyLen=' + txt(document.body).length
      + ' | seenUrls=' + ((N.allUrls || []).slice(-6).join(' '))
      + ' | tail=' + window.__aigateTail();
  };

  window.__aigateChecks = function(){
    var N = window.__aigateNet || {};
    var out = [];
    function add(key, ok, detail){
      out.push(key + '|' + (ok ? '1' : '0') + '|' + String(detail).replace(/[|\n]/g, ' '));
    }
    var bodyLen = txt(document.body).length;
    add('page', document.readyState !== 'loading' && bodyLen > 50,
      'readyState=' + document.readyState + ' bodyTextLen=' + bodyLen);
    var href = location.href;
    add('route', href.indexOf('sign_in') < 0 && href.indexOf('login') < 0, 'url=' + href);
    var token = '';
    try {
      for (var i = 0; i < localStorage.length; i++){
        var k = localStorage.key(i);
        if (k && k.toLowerCase().indexOf('token') >= 0){ token += k + ' '; }
      }
    } catch (e){ token = 'err'; }
    add('session', (token && token !== 'err') || document.cookie.length > 20,
      'lsTokens=' + (token || 'none') + ' cookieLen=' + document.cookie.length);
    var el = inputEl();
    add('input', !!el, el ? (el.tagName + ' id=' + (el.id || '-')) : 'not found');
    add('sendbtn', parseInt(window.__aigateFindSend(), 10) > 0, 'candidates=' + window.__aigateFindSend());
    add('hook', !!N.hooked, 'hooked=' + (N.hooked ? '1' : '0') + ' early=' + (N.early || 0));
    add('early', (N.early || 0) === 1 && !!N.hooked,
      'hook ghabl az script haye site: ' + ((N.early || 0) === 1 ? 'yes' : 'no'));
    add('blocks', blocks().length > 0, 'answerBlocks=' + blocks().length);
    add('calls', (N.calls || 0) > 0,
      'chatRequests=' + (N.calls || 0) + ' urls=' + ((N.urls || []).join(' ')));
    add('stream', (N.frames || 0) > 0,
      'frames=' + (N.frames || 0) + ' status=' + (N.status || 0) + ' netLen=' + ((N.text || '').length) + ' err=' + (N.err || '-'));
    add('seen', (N.allUrls || []).length > 0,
      'lastUrls=' + ((N.allUrls || []).slice(-8).join(' ')));
    return out.join('\n');
  };
  return 'dom-helpers';
})();
"""

	private val BOOTSTRAP = NET_HOOK + "\n" + DOM_HELPERS

	// ---------------------------------------------------------------- setup

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
		view.webViewClient = object : WebViewClient() {
			override fun onPageStarted(v: WebView?, url: String?, favicon: Bitmap?) {
				super.onPageStarted(v, url, favicon)
				v?.evaluateJavascript(NET_HOOK, null)
			}

			override fun doUpdateVisitedHistory(v: WebView?, url: String?, isReload: Boolean) {
				super.doUpdateVisitedHistory(v, url, isReload)
				v?.evaluateJavascript(NET_HOOK, null)
			}

			override fun onPageFinished(v: WebView?, url: String?) {
				super.onPageFinished(v, url)
				v?.evaluateJavascript(BOOTSTRAP, null)
				CookieManager.getInstance().flush()
			}
		}
		val cookies = CookieManager.getInstance()
		cookies.setAcceptCookie(true)
		cookies.setAcceptThirdPartyCookies(view, true)
	}

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
				val existing = fallbackView
				if (existing != null) {
					existing
				} else {
					val created = WebView(context.applicationContext)
					configure(created)
					fallbackView = created
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

	/** JSON string e barghashti az evaluateJavascript ra be matn e vaghei tabdil mikonad. */
	private fun unquote(raw: String?): String {
		if (raw == null || raw == "null" || raw == "undefined") return ""
		var body = raw
		if (body.length >= 2 && body.startsWith("\"") && body.endsWith("\"")) {
			body = body.substring(1, body.length - 1)
		}
		val out = StringBuilder(body.length)
		var i = 0
		while (i < body.length) {
			val c = body[i]
			if (c != '\\') {
				out.append(c)
				i++
				continue
			}
			if (i + 1 >= body.length) break
			val next = body[i + 1]
			when (next) {
				'n' -> { out.append('\n'); i += 2 }
				't' -> { out.append('\t'); i += 2 }
				'r' -> { i += 2 }
				'b' -> { i += 2 }
				'f' -> { i += 2 }
				'"' -> { out.append('"'); i += 2 }
				'\'' -> { out.append('\''); i += 2 }
				'\\' -> { out.append('\\'); i += 2 }
				'/' -> { out.append('/'); i += 2 }
				'u' -> {
					if (i + 6 <= body.length) {
						val hex = body.substring(i + 2, i + 6)
						val code = hex.toIntOrNull(16)
						if (code != null) {
							out.append(code.toChar())
							i += 6
						} else {
							out.append(next)
							i += 2
						}
					} else {
						i += 2
					}
				}
				else -> { out.append(next); i += 2 }
			}
		}
		return out.toString()
	}

	private fun jsString(value: String): String {
		val escaped = value
			.replace("\\", "\\\\")
			.replace("\"", "\\\"")
			.replace("\n", "\\n")
			.replace("\r", "")
		return "\"" + escaped + "\""
	}

	private suspend fun install(view: WebView) {
		eval(view, BOOTSTRAP)
	}

	private suspend fun ensureAlive(view: WebView) {
		val alive = eval(
			view,
			"(typeof window.__aigateState === 'function' && window.__aigateNet && window.__aigateNet.hooked) ? '1' : '0'",
		)
		if (alive != "1") install(view)
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

	/**
	 * agar hook bad az ejraye script haye site nasb shode bashad, site reference e
	 * ghadimi fetch ra dar dast darad va shenood kar nemikonad. dar in halat yek bar
	 * safhe ra reload mikonim ta hook dar onPageStarted nasb shavad.
	 */
	private suspend fun ensureEarlyHook(view: WebView): Boolean {
		val early = eval(view, "(window.__aigateNet && window.__aigateNet.early === 1) ? '1' : '0'")
		if (early == "1") return true
		withContext(Dispatchers.Main) { view.reload() }
		delay(1200)
		return waitReady(view, 30000)
	}

	private suspend fun readState(view: WebView): StreamState {
		val raw = eval(view, "window.__aigateState()")
		val marker = "---TEXT---\n"
		val index = raw.indexOf(marker)
		val head = if (index >= 0) raw.substring(0, index) else raw
		val text = if (index >= 0) raw.substring(index + marker.length) else ""
		fun field(name: String): String {
			for (line in head.lines()) {
				if (line.startsWith(name + "=")) return line.removePrefix(name + "=")
			}
			return ""
		}
		return StreamState(
			status = field("status").trim().toIntOrNull() ?: 0,
			done = field("done").trim() == "1",
			frames = field("frames").trim().toIntOrNull() ?: 0,
			calls = field("calls").trim().toIntOrNull() ?: 0,
			error = field("err").trim(),
			text = text,
		)
	}

	private fun promptFor(history: List<ChatMessage>): String {
		val last = history.lastOrNull { it.role == "user" }
		return last?.activeText?.trim().orEmpty()
	}

	suspend fun checkSession(context: Context, provider: Provider): PingResult {
		val start = System.currentTimeMillis()
		val url = provider.baseUrl.ifBlank { DEFAULT_SITE }
		return try {
			val view = webView(context, url)
			val ready = waitReady(view, 30000)
			lastLog = eval(view, "window.__aigateDiag()")
			if (ready) {
				PingResult(true, System.currentTimeMillis() - start, "نشست وب فعال است")
			} else {
				PingResult(
					false,
					System.currentTimeMillis() - start,
					"کادر چت پیدا نشد — احتمالاً لاگین نشده‌ای یا کپچا باز است. " + lastLog,
				)
			}
		} catch (t: Throwable) {
			PingResult(false, System.currentTimeMillis() - start, t.message ?: "خطای وب‌ویو")
		}
	}

	/** ersal payam va khandan e javab; aval az shabake, agar nashod az DOM. */
	fun streamChat(
		context: Context,
		provider: Provider,
		model: String,
		history: List<ChatMessage>,
		sessionUrl: String = "",
	): Flow<StreamEvent> = flow {
		val prompt = promptFor(history)
		if (prompt.isEmpty()) {
			emit(StreamEvent.Failure("پیامی برای ارسال پیدا نشد"))
			return@flow
		}
		val url = provider.baseUrl.ifBlank { DEFAULT_SITE }
		val view = webView(context, url)
		lastSessionUrl = ""

		// har chat e app yek goftogooye jodaye site darad ta hafeze ghati nashavad
		val currentHref = withContext(Dispatchers.Main) { view.url ?: "" }
		val needsNav = if (sessionUrl.isNotBlank()) {
			!currentHref.startsWith(sessionUrl)
		} else {
			currentHref.contains("/a/chat/s/")
		}
		if (needsNav) {
			val target = if (sessionUrl.isNotBlank()) sessionUrl else url
			withContext(Dispatchers.Main) { view.loadUrl(target) }
			delay(1500)
		}

		if (!waitReady(view, 40000)) {
			lastLog = eval(view, "window.__aigateDiag()")
			emit(
				StreamEvent.Failure(
					"صفحه‌ی سایت آماده نشد. از بخش API‌ها «ورود به سایت» را بزن یا چک‌آپ نشست وب را اجرا کن. " + lastLog
				)
			)
			return@flow
		}

		ensureEarlyHook(view)

		val wantsDeepThink = model.contains("R1", ignoreCase = true) ||
			model.contains("reason", ignoreCase = true) ||
			model.contains("think", ignoreCase = true)
		if (wantsDeepThink) {
			eval(view, "window.__aigateDeepThink('1')")
			delay(300)
		}

		eval(view, "window.__aigateReset()")
		eval(view, "window.__aigateMark()")
		eval(view, "window.__aigateMarkAll()")

		val typed = eval(view, "window.__aigateType(" + jsString(prompt) + ")")
		delay(300)
		if (typed != "ok" || eval(view, "window.__aigateValue()").isBlank()) {
			lastLog = eval(view, "window.__aigateDiag()")
			emit(StreamEvent.Failure("نمی‌توانم در کادر چت سایت بنویسم. " + lastLog))
			return@flow
		}

		eval(view, "window.__aigateEnter()")
		delay(700)
		var sent = eval(view, "window.__aigateValue()").isBlank()
		if (!sent) {
			eval(view, "window.__aigateClickSend()")
			delay(900)
			sent = eval(view, "window.__aigateValue()").isBlank()
			if (!sent) {
				eval(view, "window.__aigateEnter()")
				delay(900)
				sent = eval(view, "window.__aigateValue()").isBlank()
			}
		}
		if (!sent) {
			lastLog = eval(view, "window.__aigateDiag()")
			emit(StreamEvent.Failure("دکمه‌ی ارسال سایت پیدا نشد. " + lastLog))
			return@flow
		}

		val skipArg = jsString(prompt.replace(Regex("\\s+"), " ").take(40))
		var emitted = ""
		var elapsed = 0
		var stableFor = 0
		var started = false
		var httpStatus = 0

		while (elapsed < 300000) {
			delay(400)
			elapsed += 400
			ensureAlive(view)

			val state = readState(view)
			if (state.status != 0) httpStatus = state.status
			if (httpStatus >= 400) {
				lastLog = eval(view, "window.__aigateDiag()")
				emit(StreamEvent.Failure("سایت خطا داد: HTTP " + httpStatus + " " + state.error + " | " + lastLog))
				return@flow
			}

			var text = state.text
			if (text.isBlank() && elapsed >= 2000) {
				text = eval(view, "window.__aigateAnswer(" + skipArg + ")")
			}
			text = text.trim()

			if (text.isEmpty()) {
				if (!started && elapsed > 90000) {
					lastLog = eval(view, "window.__aigateDiag()")
					emit(StreamEvent.Failure("پیام ارسال شد ولی پاسخی نیامد. " + lastLog))
					return@flow
				}
				continue
			}

			started = true
			if (text.length > emitted.length && text.startsWith(emitted)) {
				emit(StreamEvent.Delta(text.substring(emitted.length)))
				emitted = text
				stableFor = 0
			} else if (text != emitted) {
				emit(StreamEvent.Replace(text))
				emitted = text
				stableFor = 0
			} else {
				stableFor += 400
			}

			if (state.done && stableFor >= 800) break
			val busy = eval(view, "window.__aigateBusy()") == "1"
			if (!busy && stableFor >= 2400) break
			if (stableFor >= 12000) break
		}

		persistCookies()
		lastSessionUrl = eval(view, "location.href")
		eval(view, "window.__aigateMark()")
		if (!started) {
			lastLog = eval(view, "window.__aigateDiag()")
			emit(StreamEvent.Failure("پاسخی از سایت خوانده نشد. " + lastLog))
		} else {
			emit(StreamEvent.Done)
		}
	}

	suspend fun complete(
		context: Context,
		provider: Provider,
		model: String,
		history: List<ChatMessage>,
		sessionUrl: String = "",
	): Result<String> {
		var text = ""
		var failure: String? = null
		streamChat(context, provider, model, history, sessionUrl).collect { event ->
			when (event) {
				is StreamEvent.Delta -> text += event.text
				is StreamEvent.Replace -> text = event.text
				is StreamEvent.Failure -> failure = event.message
				else -> Unit
			}
		}
		val error = failure
		return if (text.isNotEmpty()) Result.success(text)
		else Result.failure(IllegalStateException(error ?: "پاسخی دریافت نشد"))
	}

	private fun titleFor(key: String): String = when (key) {
		"page" -> "۱. محتوای صفحه در وب‌ویو"
		"route" -> "۲. آدرس صفحه (ریدایرکت لاگین)"
		"session" -> "۳. توکن نشست و کوکی"
		"input" -> "۴. کادر نوشتن پیام"
		"sendbtn" -> "۵. دکمه‌ی ارسال"
		"hook" -> "۶. شنود شبکه (fetch/XHR/SSE)"
		"early" -> "۷. نصب شنود قبل از اسکریپت‌های سایت"
		"blocks" -> "۸. بلوک‌های پاسخ در صفحه"
		"calls" -> "۹. درخواست‌های چت ثبت‌شده"
		"stream" -> "۱۰. فریم‌های استریم پاسخ"
		"seen" -> "۱۱. آدرس‌های شبکه‌ی دیده‌شده"
		else -> key
	}

	/** system e checkup: marahel e mokhtalef ra test mikonad va gozaresh midahad. */
	suspend fun runDiagnostics(
		context: Context,
		provider: Provider,
		liveTest: Boolean = true,
	): List<WebCheck> {
		val results = ArrayList<WebCheck>()
		val url = provider.baseUrl.ifBlank { DEFAULT_SITE }
		val view = try {
			webView(context, url)
		} catch (t: Throwable) {
			results.add(WebCheck("۰. ساخت وب‌ویو", false, t.message ?: "خطا"))
			return results
		}

		results.add(
			WebCheck(
				"۰. وب‌ویو زنده و متصل",
				host != null,
				if (host != null) "وب‌ویوی متصل به صفحه فعال است" else "فقط وب‌ویوی پشتیبان",
			)
		)

		val ready = waitReady(view, 25000)
		if (ready) ensureEarlyHook(view)

		if (liveTest && ready) {
			val probe = listOf(ChatMessage(role = "user", content = "سلام، فقط بنویس: تست"))
			val outcome = try {
				complete(context, provider, provider.defaultModel, probe)
			} catch (t: Throwable) {
				Result.failure<String>(t)
			}
			appendJsChecks(view, results)
			results.add(
				WebCheck(
					"۱۲. تست کامل ارسال و دریافت",
					outcome.isSuccess,
					outcome.fold(
						onSuccess = { "پاسخ گرفته شد: " + it.take(140) },
						onFailure = { it.message ?: "خطا" },
					),
				)
			)
		} else {
			appendJsChecks(view, results)
			if (!ready) {
				results.add(WebCheck("۱۲. تست کامل ارسال و دریافت", false, "چون کادر چت آماده نشد اجرا نشد"))
			}
		}
		persistCookies()
		return results
	}

	private suspend fun appendJsChecks(view: WebView, results: ArrayList<WebCheck>) {
		val raw = eval(view, "window.__aigateChecks()")
		for (line in raw.lines()) {
			val parts = line.split("|")
			if (parts.size < 3) continue
			results.add(WebCheck(titleFor(parts[0]), parts[1] == "1", parts[2]))
		}
	}

	fun release() {
		val view = fallbackView
		fallbackView = null
		view?.destroy()
	}
}
