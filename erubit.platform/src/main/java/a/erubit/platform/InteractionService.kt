package a.erubit.platform

import a.erubit.platform.R
import a.erubit.platform.course.CourseManager
import a.erubit.platform.course.ProgressManager
import a.erubit.platform.course.lesson.Lesson
import a.erubit.platform.interaction.InteractionManager
import a.erubit.platform.interaction.InteractionManager.InteractionEvent
import a.erubit.platform.interaction.InteractionManager.InteractionListener
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_MIN
import t.TinyDB
import u.C

class InteractionService : Service(), InteractionListener {
	private val interactionServiceFgId = 9990

	private lateinit var mWindowManager: WindowManager
	private lateinit var mInteractionContainerView: View

	private val defaultLayoutParams: WindowManager.LayoutParams = WindowManager.LayoutParams(
		WindowManager.LayoutParams.MATCH_PARENT,
		WindowManager.LayoutParams.MATCH_PARENT,
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
			WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
		else
			WindowManager.LayoutParams.TYPE_PHONE,
		WindowManager.LayoutParams.FLAG_FULLSCREEN or
			WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,
		PixelFormat.TRANSLUCENT
	)



	private var mNextTimeSlot: Long = 0

	override fun onBind(intent: Intent): IBinder? {
		return null
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		val enabled = (TinyDB(applicationContext).getBoolean(C.SP_ENABLED_ON_UNLOCK, true)
				&& System.currentTimeMillis() > mNextTimeSlot && (Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(applicationContext)))

		if (enabled && mInteractionContainerView.windowToken == null) {
			val lesson = CourseManager.i().getNextLesson() ?: return super.onStartCommand(intent, flags, startId)
			val interactionView = InteractionManager.i().getInteractionView(applicationContext, lesson, this)

			if (interactionView != null) {
				setupQuickButtons(applicationContext, lesson, this)

				val root = mInteractionContainerView.findViewById<ViewGroup>(R.id.content)
				root.removeAllViews()
				root.addView(interactionView)

				mWindowManager.addView(mInteractionContainerView, defaultLayoutParams)

				startForeground()
			}
		}

		return super.onStartCommand(intent, flags, startId)
	}

	private fun startForeground() {
		val channel =
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
				createNotificationChannel()
			else ""

		val notificationBuilder = NotificationCompat.Builder(this, channel)
		val builder = notificationBuilder.setOngoing(true)
				.setSmallIcon(R.drawable.ic_learning)
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
		mInteractionContainerView = View.inflate(applicationContext, R.layout.view_interaction, null)
	}

	override fun onDestroy() {
		super.onDestroy()

		removeView()
	}

	private fun removeView() {
		if (mInteractionContainerView.windowToken != null)
			mWindowManager.removeView(mInteractionContainerView)

		stopForeground(true)
	}

	fun setupQuickButtons(context: Context, lesson: Lesson, listener: InteractionListener) {
		mInteractionContainerView.findViewById<View>(android.R.id.closeButton).setOnClickListener { listener.onInteraction(InteractionEvent.CLOSE) }

		val disableForListener = View.OnClickListener { v: View ->
			var event = InteractionEvent.CLOSE

			when (v.id) {
				R.id.disableFor1h -> event = InteractionEvent.BUSY1
				R.id.disableFor2h -> event = InteractionEvent.BUSY2
				R.id.disableFor4h -> event = InteractionEvent.BUSY4
			}

			listener.onInteraction(event)

			val progress = lesson.mProgress!!
			if (progress.interactionDate == 0L) {
				progress.interactionDate = System.currentTimeMillis()
				ProgressManager.i().save(context, lesson)
			}
		}
		mInteractionContainerView.findViewById<View>(R.id.disableFor1h).setOnClickListener(disableForListener)
		mInteractionContainerView.findViewById<View>(R.id.disableFor2h).setOnClickListener(disableForListener)
		mInteractionContainerView.findViewById<View>(R.id.disableFor4h).setOnClickListener(disableForListener)
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
