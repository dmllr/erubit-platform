package a.erubit.platform.android

import a.erubit.platform.course.CourseManager
import a.erubit.platform.interaction.AnalyticsManager
import a.erubit.platform.interaction.InteractionManager
import android.app.Application
import android.content.Intent
import android.content.IntentFilter


class App : Application() {
	override fun onCreate() {
		super.onCreate()
		CourseManager.i().initialize(applicationContext)
		InteractionManager.i().initialize(applicationContext)
		AnalyticsManager.i().initialize(applicationContext)
		applicationContext.registerReceiver(
				UserPresentBroadcastReceiver(),
				IntentFilter(Intent.ACTION_USER_PRESENT))
	}
}