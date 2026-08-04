package com.aigate.chat.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.StateFlow

/** کمکی کوتاه برای خواندن StateFlow در Compose */
@Composable
fun <T> StateFlow<T>.collectAsStateCompat(): State<T> = this.collectAsState()
