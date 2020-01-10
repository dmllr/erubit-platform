package a.erubit.platform.android

import a.erubit.platform.R
import a.erubit.platform.interaction.AnalyticsManager
import a.erubit.platform.interaction.InteractionManager
import a.erubit.platform.interaction.InteractionManager.InteractionEvent
import a.erubit.platform.interaction.InteractionManager.InteractionListener
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_MIN
import android.view.View
import android.view.WindowManager
import t.TinyDB
import u.C

class InteractionService : Service(), InteractionListener {
	private val interactionServiceFgId = 9990

	private var mWindowManager: WindowManager? = null
	private var mInteractionView: View? = null
	private var mNextTimeSlot: Long = 0

	override fun onBind(intent: Intent): IBinder? {
		return null
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		val enabled = (TinyDB(applicationContext).getBoolean(C.SP_ENABLED_ON_UNLOCK, true)
				&& System.currentTimeMillis() > mNextTimeSlot && (Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(applicationContext)))

		if (enabled && (mInteractionView == null || mInteractionView!!.windowToken == null)) {
			val interactionView = InteractionManager.i().getInteractionView(applicationContext, this)

			if (interactionView != null) {
				interactionView.findViewById<View>(R.id.quickButtonBar).visibility = View.VISIBLE
				mWindowManager!!.addView(interactionView, interactionView.layoutParams)
				mInteractionView = interactionView

				startForeground()
			}
		}

		return super.onStartCommand(intent, flags, startId)
	}

	override fun onConfigurationChanged(newConfig: Configuration) {
		super.onConfigurationChanged(newConfig)
		// only if interaction window is shown now
		if (mInteractionView != null && mInteractionView!!.windowToken != null) {
			InteractionManager.i().onConfigurationChanged(applicationContext)
			updateView()
		}
	}

	private fun updateView() {
		removeView()
		mInteractionView = InteractionManager.i().getLastInteractionView(applicationContext, this)
		if (mInteractionView != null) {
			mInteractionView!!.findViewById<View>(R.id.quickButtonBar).visibility = View.VISIBLE
			mWindowManager!!.addView(mInteractionView, mInteractionView!!.layoutParams)
			AnalyticsManager.i().reportInteractionScreen()
		}
	}

	private fun startForeground() {
		val channel =
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
				createNotificationChannel()
			else ""

		val notificationBuilder = NotificationCompat.Builder(this, channel)
		val builder = notificationBuilder.setOngoing(true)
				//.setSmallIcon(R.mipmap.ic_launcher)
				.setPriority(PRIORITY_MIN)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
			builder.setCategory(Notification.CATEGORY_SERVICE)

		val notification = builder.build()

		startForeground(interactionServiceFgId, notification)
	}

	@RequiresApi(Build.VERSION_CODES.O)
	private fun createNotificationChannel(): String {
		val id = "erubit-foreground-service"
		val chan = NotificationChannel(
			id,
			"Screen-unlock learning service",
			NotificationManager.IMPORTANCE_NONE)
		chan.lightColor = R.color.color_purple
		chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE

		val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

		service.createNotificationChannel(chan)

		return id
	}

	override fun onCreate() {
		super.onCreate()

		mWindowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
		mInteractionView = null
	}

	override fun onDestroy() {
		super.onDestroy()

		removeView()
	}

	private fun removeView() {
		if (mInteractionView != null && mInteractionView!!.windowToken != null)
			mWindowManager!!.removeView(mInteractionView)

		stopForeground(true)
	}

	override fun onInteraction(event: InteractionEvent) {
		when (event) {
			InteractionEvent.BUSY1 -> mNextTimeSlot = System.currentTimeMillis() + C.TIME_1H
			InteractionEvent.BUSY2 -> mNextTimeSlot = System.currentTimeMillis() + C.TIME_2H
			InteractionEvent.BUSY4 -> mNextTimeSlot = System.currentTimeMillis() + C.TIME_4H
			else -> { }
		}

		if (event !== InteractionEvent.NEGATIVE)
			removeView()
	}
}
