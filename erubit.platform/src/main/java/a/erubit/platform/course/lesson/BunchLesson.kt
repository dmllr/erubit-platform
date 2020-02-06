package a.erubit.platform.course.lesson

import a.erubit.platform.R
import a.erubit.platform.course.Course
import a.erubit.platform.course.ItemProgress
import a.erubit.platform.course.ProgressManager
import android.content.Context
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.json.JSONException
import org.json.JSONObject
import java.util.*
import kotlin.math.max


abstract class BunchLesson(course: Course) : Lesson(course) {
	private val timeToForget = 1000 * 60 * 60 * 24 * 2 // Two days

	open var mSet: ArrayList<Item>? = null

	protected abstract val rankFamiliar: Int
	protected abstract val rankLearned: Int
	protected abstract val rankLearnedWell: Int

	override fun getNextPresentable(context: Context): PresentableDescriptor {
		val set = mSet ?: return PresentableDescriptor.ERROR

		var size = set.size
		if (size == 0)
			return PresentableDescriptor.ERROR

		val availableItems = LinkedList<Item>()
		for (k in 0 until size) {
			val c = set[k]
			val knowledge = c.knowledge
			if (knowledge != Knowledge.Learned && knowledge != Knowledge.LearnedWell) {
				availableItems.add(c)
			}
		}

		size = availableItems.size
		if (size == 0)
			return PresentableDescriptor.COURSE_LEARNED
		availableItems.sortWith(Comparator { c1: Item, c2: Item ->
			when {
				c1.showDate > c2.showDate -> -1
				c1.showDate < c2.showDate -> 1
				else -> 0
			}
		})

		size = availableItems.size
		if (size == 0)
			return PresentableDescriptor.ERROR
		if (size > 2) {
			availableItems.removeFirst()
			availableItems.removeFirst()
			size -= 2
		}

		val problemIndex = Random().nextInt(size)
		val problemItem = availableItems[problemIndex]

		return getPresentable(problemItem)
	}

	protected abstract fun getPresentable(problemItem: Item): PresentableDescriptor

	override fun getPresentables(context: Context): ArrayList<PresentableDescriptor> {
		val set = mSet ?: return ArrayList(0)
		val size = set.size
		val descriptors = ArrayList<PresentableDescriptor>(size)

		for (item in set) {
			val descriptor = getPresentable(item)
			descriptor.title = item.title
			descriptors.add(descriptor)
		}

		return descriptors
	}

	override fun updateProgress(): a.erubit.platform.course.Progress {
		val set = mSet ?: return Progress(0)
		var size = set.size

		val progress = Progress(size)

		var hasUnknown = false
		var hasSomething = false

		for (k in 0 until size) {
			val c = set[k]
			val knowledge = c.knowledge

			hasUnknown = hasUnknown || knowledge != Knowledge.Learned && knowledge != Knowledge.LearnedWell
			hasSomething = hasSomething || knowledge != Knowledge.LearnedWell

			if (hasUnknown)
				break
		}

		progress.nextInteractionDate = -1
		if (hasUnknown)
			progress.nextInteractionDate = 0
		else if (hasSomething)
			progress.nextInteractionDate = System.currentTimeMillis() + timeToForget
		progress.interactionDate = System.currentTimeMillis()
		progress.trainDate = System.currentTimeMillis()

		for (k in 0 until size) {
			val c = mSet!![k]
			val cs = ItemProgress()
			cs.showDate = c.showDate
			cs.failDate = c.failDate
			cs.raiseDate = c.raiseDate
			cs.touchDate = c.touchDate
			cs.knowledgeLevel = c.knowledgeLevel

			progress.append(c.id, cs)
		}

		if (size == 0) {
			progress.familiarity = 0
			progress.progress = 0
			return progress
		}

		var t = 0
		var r = 0
		size = progress.map.size
		for (cs in progress.map.values) {
			if (cs.touchDate > 0)
				t++
			if (cs.knowledgeLevel == rankFamiliar)
				r++
		}
		progress.familiarity = 100 * t / size
		progress.progress = 100 * r / size

		return progress.also { mProgress = it }
	}

	override fun getProgress(context: Context): a.erubit.platform.course.Progress {
		val progress = loadProgress(context)
		mProgress = progress

		return progress
	}

	fun loadProgress(context: Context): Progress {
		val json = ProgressManager.i().load(context, id)

		val progress = Progress()

		if ("" == json)
			return progress

		var jo = JsonParser().parse(json).asJsonObject

		if (jo.has("nextInteractionDate"))
			progress.nextInteractionDate = jo["nextInteractionDate"].asLong
		if (jo.has("interactionDate"))
			progress.interactionDate = jo["interactionDate"].asLong
		if (jo.has("trainDate"))
			progress.trainDate = jo["trainDate"].asLong
		if (jo.has("progress"))
			progress.progress = jo["progress"].asInt
		if (jo.has("familiarity"))
			progress.familiarity = jo["familiarity"].asInt

		if (jo.has("map")) {
			jo = jo["map"].asJsonObject
			for (k in jo.keySet())
				progress.appendFromJson(k.toInt(), jo[k].asJsonObject)
		}

		return progress
	}

	override fun hasInteraction(): Boolean {
		val progress = mProgress ?: return false

		return progress.nextInteractionDate >= 0 && progress.nextInteractionDate <= System.currentTimeMillis()
	}

	fun getKnowledgeText(context: Context, knowledgeLevel: Int): String {
		return when {
			knowledgeLevel in 1 until rankFamiliar -> context.getString(R.string.studying)
			knowledgeLevel in rankFamiliar until rankLearned -> context.getString(R.string.familiar)
			knowledgeLevel in rankLearned until rankLearnedWell -> context.getString(R.string.learned)
			knowledgeLevel >= rankLearnedWell -> context.getString(R.string.learned_well)
			else -> context.getString(R.string.unknown)
		}
	}



	abstract inner class Problem(lesson: Lesson, val item: Item) : Lesson.Problem(lesson) {
		override fun spied() {
			item.fail()
		}

		override fun treatResult() {
			if (mSucceed) item.raise() else item.fail()
			item.showDate = System.currentTimeMillis()
		}

		val knowledge: Knowledge
			get() = item.knowledge
	}


	open inner class Item() {
		var id = 0
		var title = ""

		var knowledgeLevel = 0
		var touchDate: Long = 0
		var raiseDate: Long = 0
		var failDate: Long = 0
		var showDate: Long = 0

		@Throws(JSONException::class)
		open fun fromJsonObject(jso: JSONObject): Item {
			id = jso.getInt("i")

			return this
		}

		fun withProgress(progress: Progress): Item {
			val storage = progress[this.id]

			knowledgeLevel = storage.knowledgeLevel
			touchDate = storage.touchDate
			raiseDate = storage.raiseDate
			failDate = storage.failDate
			showDate = storage.showDate

			return this
		}

		val knowledge: Knowledge
			get() = when {
				knowledgeLevel >= rankLearnedWell -> Knowledge.LearnedWell
				knowledgeLevel in rankLearned until rankLearnedWell -> {
					when {
						System.currentTimeMillis() > raiseDate + timeToForget -> Knowledge.PossibleForgotten
						else -> Knowledge.Learned
					}
				}
				knowledgeLevel in rankFamiliar until rankLearned -> Knowledge.Familiar
				knowledgeLevel < rankFamiliar -> {
					when (touchDate) {
						0L -> Knowledge.Untouched
						else -> Knowledge.Unknown
					}
				}
				else -> Knowledge.Unknown
			}

		fun raise() {
			if (knowledgeLevel < rankLearnedWell)
				knowledgeLevel++
			raiseDate = System.currentTimeMillis()
			touchDate = raiseDate
		}

		fun fail() {
			knowledgeLevel = 0
			failDate = System.currentTimeMillis()
			touchDate = failDate
		}
	}

	inner class Progress : a.erubit.platform.course.Progress {
		val map: HashMap<Int, ItemProgress>
		private val defaultValue = ItemProgress()

		internal constructor() {
			map = HashMap()
		}

		internal constructor(size: Int) {
			map = HashMap(size)
		}

		internal fun append(id: Int, cs: ItemProgress) {
			map.put(id, cs)
		}

		fun appendFromJson(id: Int, jo: JsonObject) {
			val cs = ItemProgress()

			if (jo.has("knowledgeLevel"))
				cs.knowledgeLevel = jo["knowledgeLevel"].asInt
			if (jo.has("touchDate"))
				cs.touchDate = jo["touchDate"].asLong
			if (jo.has("raiseDate"))
				cs.raiseDate = jo["raiseDate"].asLong
			if (jo.has("failDate"))
				cs.failDate = jo["failDate"].asLong
			if (jo.has("showDate"))
				cs.showDate = jo["showDate"].asLong

			append(id, cs)
		}

		internal operator fun get(id: Int): ItemProgress {
			return map.get(id) ?: defaultValue
		}

		override fun getExplanation(context: Context): String {
			var t = 0
			var f = 0
			var l = 0
			var lw = 0
			val size = map.size
			for (cs in map.values) {
				if (cs.touchDate > 0)
					t++
				if (cs.knowledgeLevel == rankFamiliar)
					f++
				if (cs.knowledgeLevel == rankLearned)
					l++
				if (cs.knowledgeLevel == rankLearnedWell)
					lw++
			}

			t = max(0, t - l - lw)

			val r = context.resources
			return when {
				t + f + l + lw == 0 -> r.getString(R.string.unopened)
				lw == size -> r.getString(R.string.finished)
				else -> r.getString(R.string.set_lesson_progress_explanation, t, f, l)
			}

		}
	}

	enum class Knowledge {
		Untouched, Unknown, Familiar, PossibleForgotten, Learned, LearnedWell
	}
}