package a.erubit.platform.tests

import a.erubit.platform.course.CourseManager
import android.content.Context

object Tests {
	fun courseManagerInitialize(context: Context?) {
		CourseManager.i().initialize(context!!)
	}
}