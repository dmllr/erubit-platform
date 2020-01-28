package a.erubit.platform

import a.erubit.platform.course.Course
import a.erubit.platform.course.CourseManager
import a.erubit.platform.interaction.InteractionManager

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup


class CourseTrainingFragment : TrainingFragment() {
	private var mCourse: Course? = null

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		mCourse = if (null == arguments) null else CourseManager.i().getCourse(arguments!!.getString("id"))
		return super.onCreateView(inflater, container, savedInstanceState)
	}

	override val nextInteractionView: View?
		get() {
			val context = this.context ?: return null

			val lesson = (if (mCourse != null) CourseManager.i().getNextLesson(mCourse!!) else CourseManager.i().getNextLesson())
				?: return null

			return InteractionManager.i().getInteractionView(context, lesson, this) ?: return null
		}
}
