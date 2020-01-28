package a.erubit.platform.learning.lesson

import a.erubit.platform.course.Course
import a.erubit.platform.course.ProgressManager
import a.erubit.platform.course.lesson.Lesson
import a.erubit.platform.learning.R
import android.content.Context
import com.google.gson.JsonParser
import org.json.JSONException
import org.json.JSONObject
import u.U
import java.io.IOException
import java.util.*


class WelcomeLesson constructor(course: Course) : Lesson(course) {
	private var welcomeText: String? = null

	override fun getNextPresentable(context: Context): PresentableDescriptor {
		val text = welcomeText
		return PresentableDescriptor(text)
	}

	override fun getPresentables(context: Context): ArrayList<PresentableDescriptor> {
		val descriptors = ArrayList<PresentableDescriptor>(1)
		descriptors.add(getNextPresentable(context))

		return descriptors
	}

	private fun loadProgress(context: Context): Progress {
		val json: String = ProgressManager.i().load(context, id)

		val progress = Progress()

		if ("" == json)
			return progress

		val jo = JsonParser().parse(json).asJsonObject
		if (jo.has("interactionDate"))
			progress.interactionDate = jo["interactionDate"].asLong

		return progress
	}

	override fun updateProgress(): a.erubit.platform.course.Progress {
		val progress = mProgress ?: Progress()

		val isShown = progress.interactionDate > 0
		progress.familiarity = when {
			isShown -> 100
			else -> 0
		}
		progress.progress = progress.familiarity

		return progress
	}

	override fun getProgress(context: Context): a.erubit.platform.course.Progress {
		return updateProgress()
	}

	override fun hasInteraction(): Boolean {
		return mProgress!!.interactionDate == 0L
	}

	override fun fromJson(context: Context, jo: JSONObject): WelcomeLesson {
		try {
			id = jo.getString("id")
			name = U.getStringValue(context, jo, "title")
			welcomeText = U.getStringValue(context, jo, "description")
			mProgress = loadProgress(context)
		}
		catch (_: IOException) { }
		catch (_: JSONException) { }

		return this
	}


	private inner class Progress : a.erubit.platform.course.Progress() {
		override fun getExplanation(context: Context): String {
			val r = context.resources
			return r.getString(if (interactionDate == 0L) R.string.unopened else R.string.finished)
		}
	}
}
