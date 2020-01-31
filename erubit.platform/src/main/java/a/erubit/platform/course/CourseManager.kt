package a.erubit.platform.course

import a.erubit.platform.R
import a.erubit.platform.course.lesson.Lesson
import android.content.Context
import org.json.JSONArray
import org.json.JSONException
import t.TinyDB
import u.C
import u.U
import java.io.IOException
import java.util.*


class CourseManager private constructor() {
	val courses = ArrayList<Course>()
	private var mActiveCourses = ArrayList<Course>()
	var lessonInflaters: LinkedHashMap<String, Lesson.Inflater> = LinkedHashMap(0)

	fun initialize(context: Context, contentsResourceId: Int) {
		val packageName = context.packageName
		if (contentsResourceId != 0) {
			try {
				val json = U.loadStringResource(context, contentsResourceId)
				val ja = JSONArray(json)
				for (i in 0 until ja.length()) {
					val jo = ja.getJSONObject(i)
					val rid = context.resources.getIdentifier(jo.getString("name"), "raw", packageName)

					val c = fromResourceId(context, rid)
					c.defaultActive = jo.getBoolean("active")
					courses.add(c)
				}
			} catch (e: JSONException) {
				e.printStackTrace()
			} catch (e: IOException) {
				e.printStackTrace()
			}
		}

		// mark all courses as `active` is no saved settings found
		if (!i().hasSavedSettings(context))
			for (course in courses)
				if (course.defaultActive)
					i().setActive(context, course)

		updateActivity(context)
	}

	private fun fromResourceId(context: Context, resourceId: Int): Course {
		return Course().loadFromResource(context, resourceId)
	}

	fun getCourse(id: String?): Course? {
		id ?: return null

		// TODO to map
		for (c in courses)
			if (c.id == id)
				return c

		return null
	}

	fun isActive(course: Course): Boolean {
		return mActiveCourses.contains(course)
	}

	fun setActive(context: Context, course: Course) {
		if (!mActiveCourses.contains(course))
			mActiveCourses.add(course)
		save(context)
	}

	fun setInactive(context: Context, course: Course?) {
		if (mActiveCourses.contains(course))
			mActiveCourses.remove(course)
		save(context)
	}

	private fun save(context: Context) {
		val size = mActiveCourses.size
		val list = ArrayList<String>(size)

		for (k in 0 until size)
			list.add(mActiveCourses[k].id!!)

		TinyDB(context.applicationContext).putListString(C.SP_ACTIVE_COURSES, list)
	}

	private fun hasSavedSettings(context: Context): Boolean {
		return TinyDB(context.applicationContext).getListString(C.SP_ACTIVE_COURSES).size > 0
	}

	private fun updateActivity(context: Context) {
		val list = TinyDB(context.applicationContext).getListString(C.SP_ACTIVE_COURSES)

		mActiveCourses = courses.clone() as ArrayList<Course>

		var k = 0
		while (k < mActiveCourses.size) {
			if (!list.contains(mActiveCourses[k].id)) {
				mActiveCourses.removeAt(k)
				k--
			}
			k++
		}
	}

	fun getNextLesson(): Lesson? {
		for (course in mActiveCourses) {
			for (l in course.lessons.values) {
				if (l.hasInteraction())
					return l
			}
		}

		return null
	}

	fun getNextLesson(course: Course): Lesson? {
		val size = course.lessons.size
		if (size < 1)
			return null

		for (l in course.lessons.values) {
			if (l.hasInteraction())
				return l
		}

		return null
	}

	fun registerInflater(type: String, lessonInflater: Lesson.Inflater) {
		lessonInflaters[type] = lessonInflater
	}


	companion object {
		private val courseManager = CourseManager()

		@JvmStatic
		fun i(): CourseManager {
			return courseManager
		}
	}
}
