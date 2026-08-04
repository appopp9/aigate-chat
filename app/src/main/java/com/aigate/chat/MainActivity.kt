package com.aigate.chat

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aigate.chat.ui.ChatScreen
import com.aigate.chat.ui.ChatViewModel
import com.aigate.chat.ui.CompareScreen
import com.aigate.chat.ui.LockScreen
import com.aigate.chat.ui.ProvidersScreen
import com.aigate.chat.ui.SearchScreen
import com.aigate.chat.ui.SettingsScreen
import com.aigate.chat.ui.collectAsStateCompat
import com.aigate.chat.ui.components.LocalFlatStyle
import com.aigate.chat.ui.components.NeoEasing
import com.aigate.chat.ui.theme.AiGateTheme

class MainActivity : FragmentActivity() {

	private val notificationPermissionLauncher =
		registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()

		if (Build.VERSION.SDK_INT >= 33) {
			notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
		}

		setContent {
			val viewModel: ChatViewModel = viewModel()
			val state by viewModel.state.collectAsStateCompat()

			AiGateTheme(
				darkTheme = state.settings.darkMode,
				themeIndex = state.settings.themeIndex,
			) {
				CompositionLocalProvider(LocalFlatStyle provides !state.settings.neoStyle) {
					if (state.loaded && state.settings.appLockEnabled && !state.unlocked) {
						LockScreen(onUnlock = { viewModel.setUnlocked(true) })
					} else {
						val navController = rememberNavController()
						val flat = !state.settings.neoStyle
						val dur = if (flat) 150 else 300
						val shortDur = if (flat) 110 else 220
						NavHost(
							navController = navController,
							startDestination = "chat",
							enterTransition = {
								slideInHorizontally(
									animationSpec = tween(dur, easing = NeoEasing),
									initialOffsetX = { full -> if (flat) full / 8 else full / 3 },
								) + fadeIn(animationSpec = tween(dur, easing = NeoEasing))
							},
							exitTransition = {
								fadeOut(animationSpec = tween(shortDur, easing = NeoEasing)) +
									scaleOut(targetScale = if (flat) 0.99f else 0.94f, animationSpec = tween(shortDur, easing = NeoEasing))
							},
							popEnterTransition = {
								fadeIn(animationSpec = tween(dur, easing = NeoEasing)) +
									scaleIn(initialScale = if (flat) 0.99f else 0.94f, animationSpec = tween(dur, easing = NeoEasing))
							},
							popExitTransition = {
								slideOutHorizontally(
									animationSpec = tween(shortDur, easing = NeoEasing),
									targetOffsetX = { full -> if (flat) full / 8 else full / 3 },
								) + fadeOut(animationSpec = tween(shortDur, easing = NeoEasing))
							},
						) {
							composable("chat") {
								ChatScreen(
									viewModel = viewModel,
									onOpenProviders = { navController.navigate("providers") },
									onOpenSettings = { navController.navigate("settings") },
									onOpenCompare = { navController.navigate("compare") },
									onOpenSearch = { navController.navigate("search") },
								)
							}
							composable("providers") {
								ProvidersScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
							}
							composable("settings") {
								SettingsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
							}
							composable("compare") {
								CompareScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
							}
							composable("search") {
								SearchScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
							}
						}
					}
				}
			}
		}
	}
}
