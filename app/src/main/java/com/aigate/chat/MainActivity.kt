package com.aigate.chat

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
						NavHost(navController = navController, startDestination = "chat") {
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
