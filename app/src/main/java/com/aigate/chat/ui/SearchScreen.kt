package com.aigate.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aigate.chat.ui.components.NeoBox
import com.aigate.chat.ui.components.NeoChip
import com.aigate.chat.ui.components.NeoIconButton

/** جست‌وجوی سرتاسری dar hameye goftogooha */
@Composable
fun SearchScreen(viewModel: ChatViewModel, onBack: () -> Unit) {
	val state by viewModel.state.collectAsStateCompat()
	var query by remember { mutableStateOf("") }
	val hits = remember(query, state.conversations) { viewModel.search(query) }

	Column(
		modifier = Modifier
			.fillMaxSize()
			.background(MaterialTheme.colorScheme.background)
			.statusBarsPadding()
			.imePadding()
			.padding(14.dp),
		verticalArrangement = Arrangement.spacedBy(12.dp),
	) {
		Row(verticalAlignment = Alignment.CenterVertically) {
			NeoIconButton(
				icon = Icons.Filled.ArrowBack,
				boxSize = 48.dp,
				onClick = onBack,
				contentDescription = "بازگشت",
			)
			Spacer(Modifier.width(10.dp))
			Text(
				text = "جست‌وجوی سرتاسری",
				style = MaterialTheme.typography.headlineSmall,
				fontWeight = FontWeight.Bold,
				color = MaterialTheme.colorScheme.onBackground,
			)
		}

		NeoTextField(
			value = query,
			onValueChange = { query = it },
			placeholder = "دنبال چه چیزی می‌گردی؟",
			modifier = Modifier.fillMaxWidth(),
		)

		if (query.trim().length < 2) {
			Text(
				text = "حداقل ۲ حرف بنویس",
				style = MaterialTheme.typography.bodyMedium,
				color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
			)
		} else {
			NeoChip(text = hits.size.toString() + " نتیجه")
			LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
				items(hits, key = { it.messageId }) { hit ->
					NeoBox(
						modifier = Modifier.fillMaxWidth(),
						background = MaterialTheme.colorScheme.surface,
						onClick = {
							viewModel.selectConversation(hit.conversationId)
							onBack()
						},
					) {
						Column(modifier = Modifier.padding(12.dp)) {
							Text(
								text = hit.conversationTitle + "  ·  " + hit.role,
								style = MaterialTheme.typography.labelLarge,
								fontWeight = FontWeight.Bold,
								color = MaterialTheme.colorScheme.onSurface,
							)
							Spacer(Modifier.height(4.dp))
							Text(
								text = hit.snippet,
								style = MaterialTheme.typography.bodySmall,
								color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
							)
						}
					}
				}
			}
		}
	}
}
