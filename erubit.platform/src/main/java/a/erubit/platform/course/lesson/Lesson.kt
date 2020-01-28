package a.erubit.platform.course.lesson

import a.erubit.platform.course.Course
import a.erubit.platform.course.Progress
import a.erubit.platform.interaction.InteractionManager
import android.content.Context
import android.view.View
import org.json.JSONObject
import java.util.*


abstract class Lesson(val course: Course) {
	var id: String? = null
	var name: String? = null
	var type: String = ""
	var mProgress: Progress? = null

	enum class Status {
		OK, LESSON_LEARNED, ERROR
	}

	abstract fun fromJson(context: Context, jo: JSONObject): Lesson

	abstract fun hasInteraction(): Boolean

	abstract fun getNextPresentable(context: Context): PresentableDescriptor

	abstract fun getPresentables(context: Context): ArrayList<PresentableDescriptor>

	abstract fun updateProgress(): Progress

	abstract fun getProgress(context: Context): Progress


	abstract class Inflater {
		abstract fun inflate(course: Course): Lesson
		abstract fun createView(context: Context): View
		abstract fun populateView(context: Context, view: View, lesson: Lesson, problem: Any, listener: InteractionManager.InteractionListener)
	}


	class PresentableDescriptor {
		var title: String = ""
		val mStatus: Status
		val mPresentable: Any?

		internal constructor(status: Status) {
			mStatus = status
			mPresentable = null
		}

		constructor(problem: Problem?) {
			mStatus = Status.OK
			mPresentable = problem
		}

		constructor(text: String?) {
			mStatus = Status.OK
			mPresentable = text
		}

		companion object {
			val ERROR = PresentableDescriptor(Status.ERROR)
			val COURSE_LEARNED = PresentableDescriptor(Status.LESSON_LEARNED)
		}
	}


	abstract class Problem(val lesson: Lesson) {
		var mSucceed = true

		fun attempt(solved: Boolean) {
			mSucceed = mSucceed && solved
		}

		abstract fun spied()
		abstract fun treatResult()
	}

}
