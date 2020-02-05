package a.erubit.platform

import a.erubit.platform.course.CourseManager
import a.erubit.platform.course.lesson.Lesson
import a.erubit.platform.interaction.AnalyticsManager
import a.erubit.platform.interaction.InteractionManager
import android.app.Application
import android.content.Intent
import android.content.IntentFilter


abstract class App : Application() {
	override fun onCreate() {
		super.onCreate()

		applicationContext.setTheme(R.style.AppTheme)

		applicationContext.registerReceiver(
			UserPresentBroadcastReceiver(),
				IntentFilter(Intent.ACTION_USER_PRESENT))
	}

	fun initialize(i: Initializer) {
		i.registerLessons()
		CourseManager.i().initialize(applicationContext, i.resolveContentsResource())
		InteractionManager.i().initialize(applicationContext)
		AnalyticsManager.i().initialize(applicationContext)
	}


	abstract inner class Initializer {
		abstract fun resolveContentsResource(): Int

		open fun registerLessons() { }

		protected fun registerInflater(type: String, lessonInflater: Lesson.Inflater) {
			CourseManager.i().registerInflater(type, lessonInflater)
			InteractionManager.i().registerInflater(type, lessonInflater)
		}
	}

}