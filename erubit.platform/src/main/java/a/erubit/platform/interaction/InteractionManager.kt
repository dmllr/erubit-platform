package a.erubit.platform.interaction

import a.erubit.platform.R
import a.erubit.platform.course.*
import a.erubit.platform.course.lesson.*
import a.erubit.platform.course.lesson.Lesson.PresentableDescriptor

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.TranslateAnimation
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import link.fls.swipestack.SwipeStack.*

import t.FlipLayout.FlipLayout
import t.FlipLayout.FlipLayout.OnFlipListener
import t.FlowLayout
import t.SwipeStack
import t.TinyDB
import u.C
import u.U

import java.lang.UnsupportedOperationException


class InteractionManager private constructor() {
	private val defaultLayoutParams: WindowManager.LayoutParams = WindowManager.LayoutParams(
			WindowManager.LayoutParams.MATCH_PARENT,
			WindowManager.LayoutParams.MATCH_PARENT,
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
				WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
			else
				WindowManager.LayoutParams.TYPE_PHONE,
			WindowManager.LayoutParams.FLAG_FULLSCREEN or
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
					WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
				else
					0,
			PixelFormat.TRANSLUCENT
	)
	private var mInteractionViewWelcome: View? = null
	private var mInteractionViewFlipcardCourse: View? = null
	private var mInteractionViewSetCourse: View? = null
	private var mInteractionViewPhraseCourse: View? = null
	private var mAnswerLabels: IntArray = IntArray(0)
	private var mLastViewData: InteractionViewData? = null


	enum class InteractionEvent {
		POSITIVE, NEGATIVE, CLOSE, BUSY1, BUSY2, BUSY4
	}


	interface InteractionListener {
		fun onInteraction(event: InteractionEvent)
	}


	fun initialize(context: Context) {
		createInteractionViews(context)
	}

	private fun createInteractionViews(context: Context) {
		mInteractionViewWelcome = createWelcomeInteractionView(context)
		mInteractionViewFlipcardCourse = createFlipcardInteractionView(context)
		mInteractionViewSetCourse = createChooseInteractionView(context)
		mInteractionViewPhraseCourse = createPhraseInteractionView(context)
	}

	private fun createWelcomeInteractionView(context: Context): View {
		return View.inflate(context, R.layout.view_interaction_welcome, null)
	}

	private fun createFlipcardInteractionView(context: Context): View {
		return View.inflate(context, R.layout.view_interaction_flipcard, null)
	}

	private fun createChooseInteractionView(context: Context): View {
		val view = View.inflate(context, R.layout.view_interaction_choose, null)
		val packageName = context.packageName

		mAnswerLabels = IntArray(C.NUMBER_OF_ANSWERS)
		for (k in 0 until C.NUMBER_OF_ANSWERS)
			mAnswerLabels[k] = context.resources.getIdentifier("a$k", "id", packageName)

		return view
	}

	private fun createPhraseInteractionView(context: Context): View {
		return View.inflate(context, R.layout.view_interaction_phrase, null)
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
		val view: View?

		if (lesson == null || pd.mStatus !== Lesson.Status.OK)
			return null
			// TODO toast

		val lessonType = lesson.javaClass.toString()

		view = when (lessonType) {
			WelcomeLesson::class.java.toString() -> populateWelcome(context, lesson, pd.mPresentable as String, listener)
			FlipcardLesson::class.java.toString() -> populateFlipcardLesson(context, lesson, pd.mPresentable as Lesson.Problem, listener)
			SetLesson::class.java.toString() -> populateSetLesson(context, lesson, pd.mPresentable as Lesson.Problem, listener)
			PhraseLesson::class.java.toString() -> populatePhraseLesson(context, lesson, pd.mPresentable as Lesson.Problem, listener)
			CharacterLesson::class.java.toString() -> populatePhraseLesson(context, lesson, pd.mPresentable as Lesson.Problem, listener)
			VocabularyLesson::class.java.toString() -> populateVocabularyLesson(context, lesson, pd.mPresentable as Lesson.Problem, listener)
			else -> null
		} ?: throw UnsupportedOperationException("LessonType `$lessonType` is not supported.")
		view.layoutParams = defaultLayoutParams

		mLastViewData = InteractionViewData(lesson, pd)

		return view
	}

	private fun populateWelcome(context: Context, lesson: Lesson, text: String, listener: InteractionListener): View {
		val view = mInteractionViewWelcome!!
		val textView = view.findViewById<TextView>(android.R.id.text1)

		textView.text = U.getSpanned(text)

		view.findViewById<View>(android.R.id.button1).setOnClickListener {
			lesson.mProgress!!.interactionDate = System.currentTimeMillis()
			lesson.mProgress!!.trainDate = lesson.mProgress!!.interactionDate
			ProgressManager.i().save(context, lesson)
			onInteraction(listener, InteractionEvent.POSITIVE)
		}

		setupQuickButtons(context, view, lesson, listener)

		return view
	}

	private fun populateFlipcardLesson(context: Context, lesson: Lesson, problem: Lesson.Problem, listener: InteractionListener): View {
		val view = mInteractionViewFlipcardCourse!!
		val prob = problem as FlipcardLesson.Problem

		val fv = view.findViewById<LinearLayout>(R.id.face)
		fv.findViewById<TextView>(R.id.content).text = prob.flipcard.face.content
		fv.findViewById<TextView>(R.id.helper).text = prob.flipcard.face.helper
		fv.findViewById<TextView>(R.id.side).text = prob.flipcard.face.side
		val fva = fv.findViewById<LinearLayout>(R.id.additions)
		fva.removeAllViews()
		for (a in prob.flipcard.face.additions) {
			val tv = TextView(context)
			tv.text = a
			tv.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
			fva.addView(tv)
		}

		view.findViewById<TextView>(R.id.back).text = prob.flipcard.back.content

		view.findViewById<View>(R.id.familiar).setOnClickListener {
			prob.attempt(true)
			prob.treatResult()
			lesson.mProgress!!.trainDate = System.currentTimeMillis()

			ProgressManager.i().save(context, lesson)

			onInteraction(listener, InteractionEvent.POSITIVE)
		}
		view.findViewById<View>(R.id.no_idea).setOnClickListener {
			prob.attempt(false)
			onInteraction(listener, InteractionEvent.NEGATIVE)
		}

		setupQuickButtons(context, view, lesson, listener)

		return view
	}

	private fun populateSetLesson(context: Context, lesson: Lesson, problem: Lesson.Problem, listener: InteractionListener): View {
		val view = mInteractionViewSetCourse!!
		val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
		val prob = problem as CharacterLesson.Problem

		populateCard(view, prob)

		val answerTouchListener = View.OnClickListener { v: View ->
			lesson.mProgress!!.interactionDate = System.currentTimeMillis()

			val answer = (v as TextView).text.toString()
			val solved = prob.isSolved(answer)

			prob.attempt(solved)

			if (solved) {
				lesson.mProgress!!.trainDate = System.currentTimeMillis()

				prob.treatResult()
				ProgressManager.i().save(context, lesson)

				onInteraction(listener, InteractionEvent.POSITIVE)
			} else {
				vibrateAsFail(context)
				U.animateNegative(view.findViewById(R.id.card))

				onInteraction(listener, InteractionEvent.NEGATIVE)
			}
		}

		val swipeStack: SwipeStack = view.findViewById(R.id.swipeStack)
		val viewVariants = inflater.inflate(R.layout.view_card_variants, swipeStack, false)

		for (i in mAnswerLabels.indices) {
			val tv = viewVariants.findViewById<Button>(mAnswerLabels[i])
			tv.text = prob.variants[i]
			tv.setOnClickListener(answerTouchListener)
		}

		viewVariants.findViewById<View>(R.id.no_idea).setOnClickListener {
			lesson.mProgress!!.interactionDate = System.currentTimeMillis()
			prob.attempt(false)
			swipeStack.swipeTopViewToLeft()
		}
		val shadowing : View = viewVariants.findViewById<View>(R.id.shadowing)
		val shadowingEnabled: Boolean = TinyDB(context).getBoolean(C.SP_ENABLED_SHADOWING, true)
		shadowing.visibility = if (shadowingEnabled) View.VISIBLE else View.GONE
		shadowing.setOnClickListener {
			it.visibility = View.GONE
		}

		val viewMeaning = inflater.inflate(R.layout.view_card_explaination, swipeStack, false)
		(viewMeaning.findViewById<View>(R.id.meaning) as TextView).text = prob.meaning

		val problemKnowledge = (prob as BunchLesson.Problem).knowledge
		swipeStack.adapter = when(problemKnowledge) {
			BunchLesson.Knowledge.Untouched -> SetLessonWelcomeStackAdapter(viewMeaning)
			else -> SetLessonFullStackAdapter(viewVariants, viewMeaning)
		}

		swipeStack.setListener(object : SwipeStackListener {
			override fun onViewSwipedToLeft(position: Int) {
				if (problemKnowledge === BunchLesson.Knowledge.Untouched) {
					val progress = lesson.mProgress!!
					progress.trainDate = System.currentTimeMillis()
					progress.interactionDate = progress.trainDate

					prob.attempt(true)
					prob.treatResult()

					ProgressManager.i().save(context, lesson)

					return
				}
			}

			override fun onViewSwipedToRight(position: Int) {}

			override fun onStackEmpty() {
				prob.treatResult()
				ProgressManager.i().save(context, lesson)
				onInteraction(listener, InteractionEvent.POSITIVE)
			}

			override fun onClick() {}
			override fun onLongClick(duration: Long) {}
		})

		swipeStack.resetStack()
		setupQuickButtons(context, view, lesson, listener)

		return view
	}

	private fun populateVocabularyLesson(context: Context, lesson: Lesson, problem: Lesson.Problem, listener: InteractionListener): View {
		val view = mInteractionViewSetCourse!!
		val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
		val prob = problem as CharacterLesson.Problem

		populateCard(view, prob)

		val swipeStack: SwipeStack = view.findViewById(R.id.swipeStack)
		val viewMeaning = inflater.inflate(R.layout.view_card_explaination, swipeStack, false)
		val textMeaning = viewMeaning.findViewById<TextView>(R.id.meaning)

		textMeaning.text = prob.meaning

		swipeStack.adapter = SetLessonWelcomeStackAdapter(viewMeaning)
		swipeStack.setListener(object : SwipeStackListener {
			fun onViewSwiped() {
				val progress = lesson.mProgress!!
				progress.trainDate = System.currentTimeMillis()
				progress.interactionDate = progress.trainDate

				prob.attempt(true)
				prob.treatResult()

				ProgressManager.i().save(context, lesson)
			}

			override fun onViewSwipedToLeft(position: Int) {
				onViewSwiped()
			}

			override fun onViewSwipedToRight(position: Int) {
				onViewSwiped()
			}

			override fun onStackEmpty() {
				prob.treatResult()

				ProgressManager.i().save(context, lesson)

				onInteraction(listener, InteractionEvent.POSITIVE)
			}

			override fun onClick() {}
			override fun onLongClick(duration: Long) {}
		})

		swipeStack.resetStack()
		setupQuickButtons(context, view, lesson, listener)

		return view
	}

	private fun populatePhraseLesson(context: Context, lesson: Lesson, problem: Lesson.Problem, listener: InteractionListener): View {
		val view = mInteractionViewPhraseCourse!!
		val prob = problem as CharacterLesson.Problem

		populateCard(view, prob)

		val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

		val fllPhrase: FlowLayout = view.findViewById(R.id.phrase)
		while (fllPhrase.childCount > 1)
			fllPhrase.removeViewAt(0)

		val fllVariants: FlowLayout = view.findViewById(R.id.variants)
		while (fllVariants.childCount > 1)
			fllVariants.removeViewAt(0)

		for (variant in prob.variants) {
			val tpi = inflater.inflate(R.layout.text_phrase_item, fllVariants, false)
			tpi.setOnClickListener { v: View -> animateMoveWord(view, v) }

			val tv = tpi.findViewById<TextView>(android.R.id.text1)
			tv.text = variant

			fllVariants.addView(tpi, fllVariants.childCount - 1)
		}

		view.findViewById<View>(android.R.id.button1).setOnClickListener {
			val answer = StringBuilder()

			var separator = " "
			if (lesson is CharacterLesson)
				separator = " : "

			for (k in 0 until fllPhrase.childCount - 1) {
				answer
					.append(when {
						answer.isNotEmpty() -> separator
						else -> ""
					})
					.append((fllPhrase.getChildAt(k).findViewById<View>(android.R.id.text1) as TextView).text)
			}

			var solved = false
			if (lesson is PhraseLesson)
				solved = U.equals(prob.meaning, answer.toString())
			if (lesson is CharacterLesson)
				solved = U.equalsIndependent(prob.meaning, answer.toString())

			prob.attempt(solved)

			if (solved) {
				prob.treatResult()
				ProgressManager.i().save(context, lesson)
				onInteraction(listener, InteractionEvent.POSITIVE)
			} else {
				vibrateAsFail(context)
				U.animateNegative(view.findViewById(R.id.card))
			}
		}

		setupQuickButtons(context, view, lesson, listener)

		return view
	}

	private fun populateCard(view: View, problem: CharacterLesson.Problem) {
		val card: FlipLayout = view.findViewById(R.id.card)
		card.reset()
		card.setOnFlipListener(object : OnFlipListener {
			override fun onFlipStart(view: FlipLayout) {}
			override fun onFlipEnd(view: FlipLayout) {
				if (view.isFlipped)
					problem.spied()
			}
		})
		(view.findViewById<View>(R.id.problem) as TextView).text = problem.text
		(view.findViewById<View>(R.id.meaning) as TextView).text = problem.meaning
	}

	private fun animateMoveWord(container: View, view: View) {
		val r0 = Rect()
		val r1 = Rect()

		view.getGlobalVisibleRect(r0)

		val parent = view.parent as ViewGroup
		val dest = container.findViewById<ViewGroup>(when (parent.id) {
			R.id.variants -> R.id.phrase
			else -> R.id.variants
		})
		if (dest.childCount > 0) {
			val lastChild = dest.getChildAt(dest.childCount - 1)
			lastChild.getGlobalVisibleRect(r1)
		} else
			dest.getGlobalVisibleRect(r1)

		val anim = TranslateAnimation(
			0F, (r1.left - r0.left).toFloat(),
			0F, (r1.top - r0.top).toFloat()
		)
		anim.duration = 100
		anim.setAnimationListener(object : AnimationListener {
			override fun onAnimationStart(animation: Animation) {}
			override fun onAnimationRepeat(animation: Animation) {}
			override fun onAnimationEnd(animation: Animation) {
				view.post {
					parent.removeView(view)
					dest.addView(view, dest.childCount - 1)
				}
			}
		})
		view.startAnimation(anim)
	}

	private fun setupQuickButtons(context: Context, view: View, lesson: Lesson, listener: InteractionListener) {
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

	private fun vibrateAsFail(context: Context) {
		val pattern = longArrayOf(0, 200, 100, 200)
		val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
		vibrator.vibrate(pattern, -1)
	}

	fun onConfigurationChanged(context: Context) {
		createInteractionViews(context)
	}


	private class SetLessonFullStackAdapter internal constructor(viewVariants: View, viewExplanation: View) : BaseAdapter() {
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


	private class SetLessonWelcomeStackAdapter internal constructor(private val mViewExplanation: View) : BaseAdapter() {
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
