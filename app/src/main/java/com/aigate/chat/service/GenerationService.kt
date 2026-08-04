package com.aigate.chat.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.aigate.chat.MainActivity
import com.aigate.chat.R

/**
 * سرویس پیش‌زمینه — تا وقتی مدل در حال پاسخ دادن است، اپ در پس‌زمینه کشته نمی‌شود.
 */
class GenerationService : Service() {

	companion object {
		const val CHANNEL_ID: String = "aigate_generation"
		const val NOTIFICATION_ID: Int = 1001
		const val EXTRA_TITLE: String = "extra_title"
		const val EXTRA_TEXT: String = "extra_text"
		const val ACTION_START: String = "com.aigate.chat.action.START"
		const val ACTION_UPDATE: String = "com.aigate.chat.action.UPDATE"
		const val ACTION_STOP: String = "com.aigate.chat.action.STOP"

		fun start(context: Context, title: String, text: String) {
			val intent = Intent(context, GenerationService::class.java).apply {
				action = ACTION_START
				putExtra(EXTRA_TITLE, title)
				putExtra(EXTRA_TEXT, text)
			}
			try {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
					context.startForegroundService(intent)
				} else {
					context.startService(intent)
				}
			} catch (t: Throwable) {
				// اگر سیستم اجازه نداد، چت باید به کارش ادامه دهد
			}
		}

		fun update(context: Context, title: String, text: String) {
			val intent = Intent(context, GenerationService::class.java).apply {
				action = ACTION_UPDATE
				putExtra(EXTRA_TITLE, title)
				putExtra(EXTRA_TEXT, text)
			}
			try {
				context.startService(intent)
			} catch (t: Throwable) {
				// نادیده گرفته می‌شود
			}
		}

		fun stop(context: Context) {
			val intent = Intent(context, GenerationService::class.java).apply { action = ACTION_STOP }
			try {
				context.startService(intent)
			} catch (t: Throwable) {
				// نادیده گرفته می‌شود
			}
		}
	}

	override fun onBind(intent: Intent?): IBinder? = null

	override fun onCreate() {
		super.onCreate()
		createChannel()
	}

	private fun createChannel() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val manager = getSystemService(NotificationManager::class.java)
			if (manager != null && manager.getNotificationChannel(CHANNEL_ID) == null) {
				val channel = NotificationChannel(
					CHANNEL_ID,
					"پاسخ در حال تولید",
					NotificationManager.IMPORTANCE_LOW,
				)
				channel.description = "وقتی مدل در حال پاسخ دادن است فعال می‌ماند"
				channel.setShowBadge(false)
				manager.createNotificationChannel(channel)
			}
		}
	}

	private fun buildNotification(title: String, text: String): Notification {
		val contentIntent = PendingIntent.getActivity(
			this,
			0,
			Intent(this, MainActivity::class.java).apply {
				flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
			},
			PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
		)
		return NotificationCompat.Builder(this, CHANNEL_ID)
			.setSmallIcon(R.drawable.ic_notification)
			.setContentTitle(title)
			.setContentText(text)
			.setStyle(NotificationCompat.BigTextStyle().bigText(text))
			.setOngoing(true)
			.setSilent(true)
			.setPriority(NotificationCompat.PRIORITY_LOW)
			.setContentIntent(contentIntent)
			.build()
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		val action = intent?.action
		val title = intent?.getStringExtra(EXTRA_TITLE) ?: "AiGate"
		val text = intent?.getStringExtra(EXTRA_TEXT) ?: "در حال دریافت پاسخ…"
		when (action) {
			ACTION_STOP -> {
				stopForegroundCompat()
				stopSelf()
			}

			ACTION_UPDATE -> {
				val manager = getSystemService(NotificationManager::class.java)
				manager?.notify(NOTIFICATION_ID, buildNotification(title, text))
			}

			else -> {
				startForeground(NOTIFICATION_ID, buildNotification(title, text))
			}
		}
		return START_NOT_STICKY
	}

	private fun stopForegroundCompat() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			stopForeground(STOP_FOREGROUND_REMOVE)
		} else {
			@Suppress("DEPRECATION")
			stopForeground(true)
		}
	}

	override fun onDestroy() {
		stopForegroundCompat()
		super.onDestroy()
	}
}
