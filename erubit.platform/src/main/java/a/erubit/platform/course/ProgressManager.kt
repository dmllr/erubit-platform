package a.erubit.platform.course

import a.erubit.platform.course.lesson.Lesson
import android.content.Context
import com.google.gson.Gson
import t.SelfExpiringHashMap
import t.TinyDB
import u.C


class ProgressManager {
	private val cacheMap = SelfExpiringHashMap<String, String>(1000, 10)

	fun save(context: Context, lesson: Lesson) {
		save(context, lesson.id!!, lesson.updateProgress())
	}

	private fun save(context: Context, id: String, progress: Progress) {
		val json = Gson().toJson(progress)
		cacheMap[id] = json
		TinyDB(context.applicationContext).putString(C.PROGRESS_KEY + id, json)
	}

	fun load(context: Context, id: String?): String {
		val cached = cacheMap[id]
		return cached ?: TinyDB(context.applicationContext).getString(C.PROGRESS_KEY + id)
	}

	companion object {
		private val progressManager = ProgressManager()

		@JvmStatic
		fun i(): ProgressManager {
			return progressManager
		}
	}
}
