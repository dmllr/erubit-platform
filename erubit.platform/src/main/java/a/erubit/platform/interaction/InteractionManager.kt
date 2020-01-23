package a.erubit.platform.interaction

import a.erubit.platform.R
import a.erubit.platform.course.CourseManager
import a.erubit.platform.course.ProgressManager
import a.erubit.platform.course.lesson.Lesson
import a.erubit.platform.course.lesson.Lesson.PresentableDescriptor
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Vibrator
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.BaseAdapter
import java.util.*


class InteractionManager private constructor() {
	private var lessonInflaters: LinkedHashMap<String, Lesson.Inflater> = LinkedHashMap(0)
	private var mInteractionViews: LinkedHashMap<String, View> = LinkedHashMap(0)
	private var mLastViewData: InteractionViewData? = null

	private val defaultLayoutParams: WindowManager.LayoutParams = WindowManager.LayoutParams(
			WindowManager.LayoutParams.MATCH_PARENT,
			WindowManager.LayoutParams.MATCH_PARENT,
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
				WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
			else
				WindowManager.LayoutParams.TYPE_PHONE,
			WindowManager.LayoutParams.FLAG_FULLSCREEN or
					WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,
			PixelFormat.TRANSLUCENT
	)

	fun initialize(context: Context) {
		createInteractionViews(context)
	}


	enum class InteractionEvent {
		POSITIVE, NEGATIVE, CLOSE, BUSY1, BUSY2, BUSY4
	}


	interface InteractionListener {
		fun onInteraction(event: InteractionEvent)
	}


	private fun createInteractionViews(context: Context) {
		for (i in lessonInflaters)
			mInteractionViews[i.key] = i.value.createView(context)
	}

	fun getInteractionView(context: Context, listener: InteractionListener): View? {
		val lesson = CourseManager.i().getNextLesson()

		return lesson?.let { getInteractionView(context, it, listener) }
	}

	fun getInteractionView(context: Context, lesson: Lesson, listener: InteractionListener): View? {
		var pd = lesson.getNextPresentable(context)

		if (pd.mStatus === Lesson.Status.LESSON_LEARNED) {
			// We got lesson with interaction enabled, but not presentable,
			// lesson may be learned or familiar at that moment.
			// Recalculate and save it's progress

			ProgressManager.i().save(context, lesson)

			// would take next lesson and presentable
			pd = CourseManager.i().getNextLesson()?.getNextPresentable(context) ?: PresentableDescriptor.ERROR
		}

		if (pd.mStatus === Lesson.Status.OK) {
			val view = i().populate(context, lesson, pd, listener)!!
			val parent = view.parent as ViewGroup?

			parent?.removeView(view)

			return view
		}

		return null
	}

	fun getLastInteractionView(context: Context, listener: InteractionListener): View? {
		return i().populate(context, mLastViewData!!.mLesson, mLastViewData!!.mPresentableDescription, listener)
	}

	private fun onInteraction(listener: InteractionListener, event: InteractionEvent) {
		listener.onInteraction(event)
	}

	fun populate(context: Context, lesson: Lesson?, pd: PresentableDescriptor, listener: InteractionListener): View? {
		if (lesson == null || pd.mStatus !== Lesson.Status.OK)
			return null
			// TODO toast

		val view = mInteractionViews[lesson.type] ?: throw UnsupportedOperationException("Lesson Inflater for type `$lesson.type` is not registered.")

		lessonInflaters[lesson.type]?.populateView(
			context,
			view,
			lesson,
			pd.mPresentable!!,
			listener
		)
		view.layoutParams = defaultLayoutParams

		mLastViewData = InteractionViewData(lesson, pd)

		return view
	}

	fun setupQuickButtons(context: Context, view: View, lesson: Lesson, listener: InteractionListener) {
		view.findViewById<View>(android.R.id.closeButton).setOnClickListener { onInteraction(listener, InteractionEvent.CLOSE) }

		val disableForListener = View.OnClickListener { v: View ->
			var event = InteractionEvent.CLOSE

            when (v.id) {
				R.id.disableFor1h -> event = InteractionEvent.BUSY1
				R.id.disableFor2h -> event = InteractionEvent.BUSY2
				R.id.disableFor4h -> event = InteractionEvent.BUSY4
			}

			onInteraction(listener, event)

			val progress = lesson.mProgress!!
			if (progress.interactionDate == 0L) {
				progress.interactionDate = System.currentTimeMillis()
				ProgressManager.i().save(context, lesson)
			}
		}
		view.findViewById<View>(R.id.disableFor1h).setOnClickListener(disableForListener)
		view.findViewById<View>(R.id.disableFor2h).setOnClickListener(disableForListener)
		view.findViewById<View>(R.id.disableFor4h).setOnClickListener(disableForListener)
	}

	fun vibrateAsFail(context: Context) {
		val pattern = longArrayOf(0, 200, 100, 200)
		val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
		vibrator.vibrate(pattern, -1)
	}

	fun registerInflater(type: String, lessonInflater: Lesson.Inflater) {
		lessonInflaters[type] = lessonInflater
	}

	fun onConfigurationChanged(context: Context) {
		createInteractionViews(context)
	}


	class SetLessonFullStackAdapter(viewVariants: View, viewExplanation: View) : BaseAdapter() {
		private val mViewVariants: View = viewVariants
		private val mViewExplanation: View = viewExplanation

		override fun getCount(): Int {
			return 2
		}

		override fun getItem(position: Int): Any {
			return Any()
		}

		override fun getItemId(position: Int): Long {
			return position.toLong()
		}

		override fun getView(position: Int, view: View?, parent: ViewGroup?): View {
			return when (position) {
				0 -> mViewVariants
				1 -> mViewExplanation
				else -> throw UnsupportedOperationException("In SetLessonFullStackAdapter.getView position has a wrong value `$position`")
			}
		}
	}


	class SetLessonWelcomeStackAdapter(private val mViewExplanation: View) : BaseAdapter() {
		override fun getCount(): Int {
			return 1
		}

		override fun getItem(position: Int): Any {
			return Any()
		}

		override fun getItemId(position: Int): Long {
			return position.toLong()
		}

		override fun getView(position: Int, view: View?, parent: ViewGroup?): View {
			return when (position) {
				0 -> mViewExplanation
				else -> throw UnsupportedOperationException("In SetLessonWelcomeStackAdapter.getView position has a wrong value `$position`")
			}
		}

	}


	private inner class InteractionViewData internal constructor(val mLesson: Lesson, val mPresentableDescription: PresentableDescriptor)


	companion object {
		private val ourInstance = InteractionManager()

		fun i(): InteractionManager {
			return ourInstance
		}
	}

}
