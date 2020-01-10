package a.erubit.platform.course

import android.content.Context
import org.json.JSONException
import org.json.JSONObject
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
				val lr = ArrayList<String>(size)
				for (k in 0 until size)
					lr.add(jd.getString(k))
				lessonResources = lr
			}
		} catch (ignored: JSONException) {
			ignored.printStackTrace()
		}
	}
}
