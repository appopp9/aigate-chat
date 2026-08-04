package com.aigate.chat.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight

data class CodeColors(
	val plain: Color,
	val keyword: Color,
	val type: Color,
	val string: Color,
	val comment: Color,
	val number: Color,
	val punctuation: Color,
)

/** هایلایت سینتکس ساده و چندزبانه */
object CodeHighlighter {

	private val baseKeywords = setOf(
		"abstract", "as", "async", "await", "break", "case", "catch", "class", "const", "continue",
		"companion", "data", "def", "default", "delete", "do", "elif", "else", "enum", "export",
		"extends", "false", "final", "finally", "fn", "for", "from", "fun", "function", "if",
		"implements", "import", "in", "init", "instanceof", "interface", "internal", "is", "lambda",
		"let", "match", "mut", "new", "none", "not", "null", "nil", "object", "open", "operator",
		"or", "override", "package", "pass", "private", "protected", "public", "raise", "return",
		"sealed", "self", "static", "struct", "super", "suspend", "switch", "this", "throw",
		"throws", "trait", "true", "try", "type", "typeof", "use", "val", "var", "when", "where",
		"while", "with", "yield", "and", "elseif", "end", "func", "go", "defer", "select", "echo",
		"select", "insert", "update", "delete", "where", "join",
	)

	private val typeWords = setOf(
		"String", "Int", "Integer", "Long", "Float", "Double", "Boolean", "Char", "Byte", "Short",
		"List", "Map", "Set", "Array", "Any", "Unit", "Void", "void", "int", "long", "float",
		"double", "bool", "boolean", "char", "str", "number", "string", "object", "Object",
		"Result", "Option", "Optional", "Promise", "Flow", "Column", "Row", "Box", "Text",
	)

	private fun isIdentStart(c: Char): Boolean = c.isLetter() || c == '_' || c == '$'
	private fun isIdentPart(c: Char): Boolean = c.isLetterOrDigit() || c == '_' || c == '$'

	fun highlight(code: String, language: String, colors: CodeColors): AnnotatedString {
		val lang = language.lowercase()
		val hashComment = lang.startsWith("py") || lang == "bash" || lang == "sh" || lang == "shell" ||
			lang == "yaml" || lang == "yml" || lang == "ruby" || lang == "rb" || lang == "toml" ||
			lang == "ini" || lang == "perl" || lang == "r" || lang == "dockerfile" || lang == "conf"
		val markupLike = lang == "html" || lang == "xml" || lang == "svg" || lang == "vue"

		return buildAnnotatedString {
			var i = 0
			val n = code.length
			while (i < n) {
				val c = code[i]

				// کامنت تک‌خطی //
				if (!markupLike && c == '/' && i + 1 < n && code[i + 1] == '/') {
					val end = code.indexOf('\n', i).let { if (it == -1) n else it }
					withStyle(SpanStyle(color = colors.comment)) { append(code.substring(i, end)) }
					i = end
					continue
				}
				// کامنت بلوکی /* */
				if (!markupLike && c == '/' && i + 1 < n && code[i + 1] == '*') {
					val close = code.indexOf("*/", i + 2)
					val end = if (close == -1) n else close + 2
					withStyle(SpanStyle(color = colors.comment)) { append(code.substring(i, end)) }
					i = end
					continue
				}
				// کامنت HTML
				if (markupLike && c == '<' && code.startsWith("<!--", i)) {
					val close = code.indexOf("-->", i + 4)
					val end = if (close == -1) n else close + 3
					withStyle(SpanStyle(color = colors.comment)) { append(code.substring(i, end)) }
					i = end
					continue
				}
				// کامنت #
				if (hashComment && c == '#') {
					val end = code.indexOf('\n', i).let { if (it == -1) n else it }
					withStyle(SpanStyle(color = colors.comment)) { append(code.substring(i, end)) }
					i = end
					continue
				}
				// رشته
				if (c == '"' || c == '\'' || c == '`') {
					val quote = c
					var j = i + 1
					while (j < n) {
						val cj = code[j]
						if (cj == '\\') {
							j += 2
							continue
						}
						if (cj == quote) {
							j++
							break
						}
						if (cj == '\n' && quote != '`') {
							break
						}
						j++
					}
					val end = if (j > n) n else j
					withStyle(SpanStyle(color = colors.string)) { append(code.substring(i, end)) }
					i = end
					continue
				}
				// عدد
				if (c.isDigit()) {
					var j = i
					while (j < n && (code[j].isLetterOrDigit() || code[j] == '.')) j++
					withStyle(SpanStyle(color = colors.number)) { append(code.substring(i, j)) }
					i = j
					continue
				}
				// شناسه
				if (isIdentStart(c)) {
					var j = i
					while (j < n && isIdentPart(code[j])) j++
					val word = code.substring(i, j)
					when {
						baseKeywords.contains(word.lowercase()) && word.firstOrNull()?.isLowerCase() != false ->
							withStyle(SpanStyle(color = colors.keyword, fontWeight = FontWeight.Bold)) { append(word) }

						typeWords.contains(word) || (word.first().isUpperCase() && word.length > 1) ->
							withStyle(SpanStyle(color = colors.type)) { append(word) }

						else -> withStyle(SpanStyle(color = colors.plain)) { append(word) }
					}
					i = j
					continue
				}
				// نشانه‌گذاری
				if (!c.isWhitespace() && !c.isLetterOrDigit()) {
					withStyle(SpanStyle(color = colors.punctuation)) { append(c) }
					i++
					continue
				}
				withStyle(SpanStyle(color = colors.plain)) { append(c) }
				i++
			}
		}
	}
}
