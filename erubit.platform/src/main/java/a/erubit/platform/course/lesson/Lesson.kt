package a.erubit.platform.course.lesson

import a.erubit.platform.course.Course
import a.erubit.platform.course.Progress
import android.content.Context
import java.util.*


abstract class Lesson internal constructor(val course: Course) {
	var id: String? = null
	var name: String? = null
	var mProgress: Progress? = null

	enum class Status {
		OK, LESSON_LEARNED, ERROR
	}

	abstract fun hasInteraction(): Boolean

	abstract fun getNextPresentable(context: Context): PresentableDescriptor

	abstract fun getPresentables(context: Context): ArrayList<PresentableDescriptor>

	abstract fun updateProgress(): Progress

	abstract fun getProgress(context: Context): Progress


	class PresentableDescriptor {
		val mStatus: Status
		val mPresentable: Any?

		internal constructor(status: Status) {
			mStatus = status
			mPresentable = null
		}

		internal constructor(problem: Problem?) {
			mStatus = Status.OK
			mPresentable = problem
		}

		internal constructor(text: String?) {
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
		abstract fun isSolved(answer: String): Boolean
	}

}
