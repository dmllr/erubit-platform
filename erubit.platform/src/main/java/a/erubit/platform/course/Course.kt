package a.erubit.platform.course

import android.content.Context
import org.json.JSONException
import org.json.JSONObject
import u.U
import java.io.IOException
import java.util.*


abstract class Course {
	var id: String? = null
	var name: String? = null
	var description: String? = null
	var defaultActive = false
	var lessonResources: ArrayList<String>? = null

	private var mLessons: ArrayList<Lesson>? = null

	abstract val progress: Progress?

	fun getLessons(context: Context): ArrayList<Lesson> {
		if (mLessons != null)
			return mLessons ?: ArrayList(0)

		val lr = lessonResources ?: return ArrayList(0)

		val size = lr.size
		val packageName = context.packageName
		val lessons = ArrayList<Lesson>(size)

		for (k in 0 until size) {
			val resourceId = context.resources.getIdentifier(lr[k], "raw", packageName)
			try {
				val jo = JSONObject(U.loadStringResource(context, resourceId))
				when (jo.getString("type")) {
					"Welcome" -> lessons.add(WelcomeLesson(this).loadFromResource(context, resourceId))
					"Set" -> lessons.add(SetLesson(this).loadFromResource(context, resourceId, false))
					"Phrase" -> lessons.add(PhraseLesson(this).loadFromResource(context, resourceId, false))
					"Character" -> lessons.add(CharacterLesson(this).loadFromResource(context, resourceId, false))
					"Vocabulary" -> lessons.add(VocabularyLesson(this).loadFromResource(context, resourceId, false))
				}
			}
			catch (ignored: JSONException) { }
			catch (ignored: IOException) { }
		}

		return lessons.also { mLessons = it }
	}

	fun getLesson(id: String?): Lesson? {
		id ?: return null

		val lessons = mLessons ?: return null

		for (lesson in lessons)
			if (id == lesson.id)
				return lesson

		return null
	}
}
