package a.erubit.platform.course

import android.content.Context
import org.json.JSONException
import org.json.JSONObject
import u.C
import u.U
import java.io.IOException
import java.util.*


class LanguageCourse : Course() {
	override val progress: Progress
		get() = object : Progress() {}

	fun loadFromResource(context: Context, resourceId: Int) {
		try {
			val json = U.loadStringResource(context, resourceId)
			loadFromString(context, json)
		} catch (ignored: IOException) {
			ignored.printStackTrace()
		}
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
				val ls = ArrayList<Lesson>(size)
				val error = UnsupportedOperationException("`LanguageCourse.loadFromString` cant determine lesson format.")
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
					val lessonType = ljo.getString("type")
					ls.add(when (lessonType) {
						"Welcome" -> WelcomeLesson(this).fromJson(context, ljo)
						"Set" -> SetLesson(this).fromJson(context, ljo)
						"Phrase" -> PhraseLesson(this).fromJson(context, ljo)
						"Character" -> CharacterLesson(this).fromJson(context, ljo)
						"Vocabulary" -> VocabularyLesson(this).fromJson(context, ljo)
						else -> throw UnsupportedOperationException("Lesson type '$lessonType' is not supported yet.")
					})
				}
				lessons = ls
			}
		} catch (_: JSONException) { }
	}
}
