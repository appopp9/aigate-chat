package com.aigate.chat.ui

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.aigate.chat.ui.components.NeoBox
import com.aigate.chat.ui.components.NeoButton

/** safheye قفل با اثر انگشت یا رمز دستگاهe dastgah */
@Composable
fun LockScreen(onUnlock: () -> Unit) {
	val context = LocalContext.current

	fun authenticate() {
		val activity = context as? FragmentActivity
		if (activity == null) {
			onUnlock()
			return
		}
		val allowed = BiometricManager.Authenticators.BIOMETRIC_WEAK or
			BiometricManager.Authenticators.DEVICE_CREDENTIAL
		val manager = BiometricManager.from(activity)
		if (manager.canAuthenticate(allowed) != BiometricManager.BIOMETRIC_SUCCESS) {
			onUnlock()
			return
		}
		val executor = ContextCompat.getMainExecutor(activity)
		val prompt = BiometricPrompt(
			activity,
			executor,
			object : BiometricPrompt.AuthenticationCallback() {
				override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
					onUnlock()
				}
			},
		)
		val info = BiometricPrompt.PromptInfo.Builder()
			.setTitle("AiGate")
			.setSubtitle("برای ورود هویتت را تأیید کن")
			.setAllowedAuthenticators(allowed)
			.build()
		prompt.authenticate(info)
	}

	LaunchedEffect(Unit) { authenticate() }

	Column(
		modifier = Modifier
			.fillMaxSize()
			.background(MaterialTheme.colorScheme.background)
			.padding(24.dp),
		verticalArrangement = Arrangement.Center,
		horizontalAlignment = Alignment.CenterHorizontally,
	) {
		NeoBox(background = MaterialTheme.colorScheme.primaryContainer) {
			Text(
				text = "AiGate قفل است",
				modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
				style = MaterialTheme.typography.headlineSmall,
				fontWeight = FontWeight.Bold,
				color = Color(0xFF101010),
			)
		}
		Spacer(Modifier.height(18.dp))
		NeoButton(
			text = "باز کردن با اثر انگشت",
			icon = Icons.Filled.Lock,
			onClick = { authenticate() },
		)
	}
}
