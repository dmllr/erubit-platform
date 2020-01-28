package a.erubit.platform.learning.lesson

import a.erubit.platform.course.Course
import a.erubit.platform.course.lesson.BunchLesson
import a.erubit.platform.course.lesson.Lesson
import android.content.Context
import org.json.JSONException
import org.json.JSONObject
import u.C
import u.U
import java.io.IOException
import java.util.*


open class CharacterLesson constructor(course: Course) : BunchLesson(course) {
	override val rankFamiliar: Int
		get() = 1
	override val rankLearned: Int
		get() = 2
	override val rankLearnedWell: Int
		get() = 3

	var mNoise: ArrayList<String>? = null

	override fun getPresentable(problemItem: BunchLesson.Item): PresentableDescriptor {
		if (problemItem !is Item)
			return PresentableDescriptor.ERROR

		val random = Random()
		val apxSize = 4 + random.nextInt(4)
		val regex = "\\s+:\\s+"
		val problem = Problem(this, problemItem)
		val words = U.defurigana(problem.meaning).split(regex).toTypedArray()

		problem.variants = arrayOfNulls(words.size + apxSize)

		val variants = ArrayList<String>(problem.variants.size)
		variants.addAll(listOf(*words))

		val set = mSet ?: return PresentableDescriptor.ERROR
		val noise = mNoise ?: return PresentableDescriptor.ERROR

		for (k in 0 until apxSize) {
			val c = set[random.nextInt(set.size)] as Item
			val randomWords = U.defurigana(c.meaning).split(regex).toTypedArray()
			for (s in randomWords)
				if (!variants.contains(s))
					variants.add(s)
		}

		// add noise if has empty slots
		for (k in variants.size until problem.variants.size)
			variants.add(noise[random.nextInt(noise.size)])

		problem.variants = variants.toArray(problem.variants)
		U.shuffleStrArray(problem.variants)

		return PresentableDescriptor(problem)
	}

	override fun fromJson(context: Context, jo: JSONObject): BunchLesson {
		try {
			id = jo.getString("id")
			name = U.getStringValue(context, jo, "title")

			val progress = loadProgress(context)

			val jset = jo.getJSONArray("set")
			val set = ArrayList<BunchLesson.Item>(jset.length())
			for (i in 0 until jset.length()) {
				val jso = jset.getJSONObject(i)
				set.add(Item().fromJsonObject(jso).withProgress(progress))
			}
			mSet = set

			val variants = ArrayList<String>(20)
			val jvar = jo.getJSONArray("noise")
			for (i in 0 until jvar.length()) {
				variants.add(jvar.getString(i))
			}
			mNoise = variants

			mProgress = progress
		} catch (ignored: IOException) {
			ignored.printStackTrace()
		} catch (ignored: JSONException) {
			ignored.printStackTrace()
		}

		return this
	}


	inner class Item : BunchLesson.Item() {
		var character: String = ""
		var meaning: String = ""

		override fun fromJsonObject(jso: JSONObject): BunchLesson.Item {
			character = jso.getString("c")
			meaning = jso.getString("m")

			title = character

			return this
		}
	}


	inner class Problem constructor(lesson: Lesson, item: Item) : BunchLesson.Problem(lesson, item) {
		var text: String = ""
		var meaning: String = ""
		var variants: Array<String?> = arrayOfNulls(C.NUMBER_OF_ANSWERS)

		init {
			text = item.character
			meaning = item.meaning
		}

		fun isSolved(answer: String): Boolean {
			return U.equals(meaning, answer)
		}
	}
}
