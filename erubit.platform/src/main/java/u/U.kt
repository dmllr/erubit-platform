package u

import android.content.Context
import android.os.Build
import android.text.Html
import android.text.Spanned
import android.view.View
import android.view.animation.Animation
import android.view.animation.TranslateAnimation

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

import java.io.*
import java.util.*


object U {
	@Throws(IOException::class)
	fun loadStringResource(context: Context, id: Int): String {
		val writer: Writer = StringWriter()
		val buffer = CharArray(1024)

		context.resources.openRawResource(id).use { s ->
			val reader: Reader = BufferedReader(InputStreamReader(s, "UTF-8"))
			var n: Int
			while (reader.read(buffer).also { n = it } != -1)
				writer.write(buffer, 0, n)
		}

		return writer.toString()
	}

	fun shuffleIntArray(array: IntArray) {
		var index: Int
		val random = Random()

		for (i in array.size - 1 downTo 1) {
			index = random.nextInt(i + 1)
			if (index != i) {
				array[index] = array[index] xor array[i]
				array[i] = array[i] xor array[index]
				array[index] = array[index] xor array[i]
			}
		}
	}

	fun shuffleStrArray(arr: Array<String?>) {
		val rgen = Random()

		for (i in arr.indices) {
			val randPos = rgen.nextInt(arr.size)
			val tmp = arr[i]
			arr[i] = arr[randPos]
			arr[randPos] = tmp
		}
	}

	fun animateNegative(view: View) {
		val animation: Animation = TranslateAnimation((-16).toFloat(), (+16).toFloat(), 0F, 0F)

		animation.duration = 100
		animation.fillAfter = false
		animation.repeatCount = 3
		animation.repeatMode = Animation.REVERSE

		view.startAnimation(animation)
	}

	fun getSpanned(text: String): Spanned {
		return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
			Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY)
		else
			Html.fromHtml(text)
	}

	@JvmStatic
	fun defurigana(text: String): String {
		// Don't trust Lint here, regex is correct
		return text.replace("\\{(.*?):(.*?)\\}".toRegex(), "$1")
	}

	fun equals(s1: String?, s2: String?): Boolean {
		if (s1 == null || s2 == null)
			return false

		val d1 = defurigana(s1).replace('　', ' ')
		val d2 = defurigana(s2).replace('　', ' ')

		return d1 == d2
	}

	fun equalsIndependent(s1: String, s2: String): Boolean {
		val regex = "\\s+:\\s+"

		val w1 = defurigana(s1).split(regex).toTypedArray()
		val w2 = defurigana(s2).split(regex).toTypedArray()

		Arrays.sort(w1)
		Arrays.sort(w2)

		return Arrays.equals(w1, w2)
	}

	@Throws(JSONException::class)
	fun getStringValue(context: Context, jo: JSONObject, property: String?): String {
		var value = ""

		if (jo.has(property)) {
			val djo = jo[property]

			if (djo is JSONArray) {
				val jd = jo.getJSONArray(property)
				for (k in 0 until jd.length()) value += jd.getString(k)
			} else {
				value = djo as String
				if (value.startsWith(C.RESREF)) {
					val resourceName = value.substring(C.RESREF.length)
					val resourceId = context.resources.getIdentifier(resourceName, "raw", context.packageName)
					value = try {
						loadStringResource(context, resourceId)
					} catch (e: IOException) {
						""
					}
				}
			}
		}
		return value
	}
}
