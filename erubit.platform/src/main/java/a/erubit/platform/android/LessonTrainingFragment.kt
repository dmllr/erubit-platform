package a.erubit.platform.android

import a.erubit.platform.R
import a.erubit.platform.course.CourseManager
import a.erubit.platform.course.lesson.Lesson
import a.erubit.platform.interaction.InteractionManager

import android.os.Bundle
import android.view.View


class LessonTrainingFragment : TrainingFragment() {
	private var mLesson: Lesson? = null
	private var mPresentableIndex = 0

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		arguments ?: return
		val id = arguments!!.getString("id") ?: return
		val course = CourseManager.i().getCourse(id) ?: return
		val lessonId = arguments!!.getString("lesson_id") ?: return

		mLesson = course.getLesson(lessonId)
		mPresentableIndex = -1

		super.onViewCreated(view, savedInstanceState)
	}

	override val nextInteractionView: View?
		get() {
			mPresentableIndex++

			val presentables = mLesson!!.getPresentables(context!!)

			if (mPresentableIndex == presentables.size)
				return null

			val pd = presentables[mPresentableIndex]
			val v = InteractionManager.i().populate(context!!, mLesson, pd, this)
				?: return null
			v.findViewById<View>(R.id.quickButtonBar).visibility = View.GONE

			return v
		}
}
