package a.erubit.platform.course

import a.erubit.platform.course.lesson.Lesson
import android.content.Context
import org.json.JSONException
import org.json.JSONObject
import u.C
import u.U
import java.io.IOException
import java.util.*


class Course {
	var id: String? = null
	var name: String? = null
	var description: String? = null
	var defaultActive = false
	var lessons: Map<String, Lesson> = LinkedHashMap(0)

	val progress: Progress
		get() = object : Progress() {}

	fun getLesson(id: String): Lesson? {
		return lessons[id]
	}

	fun loadFromResource(context: Context, resourceId: Int): Course {
		try {
			val json = U.loadStringResource(context, resourceId)
			loadFromString(context, json)
		} catch (ignored: IOException) {
			ignored.printStackTrace()
		}

		return this
	}

	private fun loadFromString(context: Context, json: String) {
		try {
			val jo = JSONObject(json)

			id = jo.getString("id")
			name = U.getStringValue(context, jo, "title")
			description = U.getStringValue(context, jo, "description")

			if (jo.has("lessons")) {
				val jd = jo.getJSONArray("lessons")
				val size = jd.length()
				val ls = LinkedHashMap<String, Lesson>(size)
				val error = UnsupportedOperationException("`Course.loadFromString` cant determine lesson format.")
				for (k in 0 until size) {
					val ljo = when {
						jd[k] is JSONObject -> jd[k] as JSONObject
						jd[k] is String -> {
							val ref = jd[k] as String
							if (ref.startsWith(C.RESREF)) {
								val resourceName = ref.substring(C.RESREF.length)
								val resourceId = context.resources.getIdentifier(resourceName, "raw", context.packageName)
								JSONObject(U.loadStringResource(context, resourceId))
							} else throw error
						}
						else -> throw error
					}
					val id = ljo.getString("id")
					val lessonType = ljo.getString("type")
					ls[id] = CourseManager.i().lessonInflaters[lessonType]?.inflate(this)?.fromJson(context, ljo)
						?: throw UnsupportedOperationException("Lesson Inflater for type '$lessonType' is not registered.")
					ls[id]!!.type = lessonType
				}
				lessons = ls
			}
		} catch (_: JSONException) { }
	}
}
