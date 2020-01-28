package a.erubit.platform.learning

import a.erubit.platform.course.Course
import a.erubit.platform.course.CourseManager
import a.erubit.platform.course.ProgressManager
import a.erubit.platform.course.lesson.BunchLesson
import a.erubit.platform.course.lesson.Lesson
import a.erubit.platform.learning.lesson.*
import a.erubit.platform.interaction.AnalyticsManager
import a.erubit.platform.interaction.InteractionManager
import a.erubit.platform.learning.interaction.animateMoveWord
import a.erubit.platform.learning.interaction.populateCard
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import t.FlowLayout
import t.SwipeStack
import t.TinyDB
import u.C
import u.U


abstract class App : a.erubit.platform.App() {

	fun initialize(i: Initializer) {
		i.registerLessons()
		CourseManager.i().initialize(applicationContext, i.resolveContentsResource())
		InteractionManager.i().initialize(applicationContext)
		AnalyticsManager.i().initialize(applicationContext)
	}


	abstract inner class Initializer : a.erubit.platform.App.Initializer() {
		override fun registerLessons() {
			super.registerLessons()

			registerInflater("Welcome", object : Lesson.Inflater() {
				override fun inflate(course: Course): Lesson {
					return WelcomeLesson(course)
				}

				override fun createView(context: Context): View {
					return View.inflate(context, R.layout.view_interaction_welcome, null)
				}

				override fun populateView(context: Context, view: View, lesson: Lesson, problem: Any, listener: InteractionManager.InteractionListener) {
					val textView = view.findViewById<TextView>(android.R.id.text1)

					textView.text = U.getSpanned(problem as String)

					view.findViewById<View>(android.R.id.button1).setOnClickListener {
						lesson.mProgress!!.interactionDate = System.currentTimeMillis()
						lesson.mProgress!!.trainDate = lesson.mProgress!!.interactionDate
						ProgressManager.i().save(context, lesson)
						listener.onInteraction(InteractionManager.InteractionEvent.POSITIVE)
					}
				}
			})

			registerInflater("Flipcard", object : Lesson.Inflater() {
				override fun inflate(course: Course): Lesson {
					return FlipcardLesson(course)
				}

				override fun createView(context: Context): View {
					return View.inflate(context, R.layout.view_interaction_flipcard, null)
				}

				override fun populateView(context: Context, view: View, lesson: Lesson, problem: Any, listener: InteractionManager.InteractionListener) {
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
						lesson.mProgress!!.interactionDate = System.currentTimeMillis()
						lesson.mProgress!!.trainDate = lesson.mProgress!!.interactionDate

						ProgressManager.i().save(context, lesson)

						listener.onInteraction(InteractionManager.InteractionEvent.POSITIVE)
					}
					view.findViewById<View>(R.id.no_idea).setOnClickListener {
						prob.attempt(false)
						lesson.mProgress!!.interactionDate = System.currentTimeMillis()

						listener.onInteraction(InteractionManager.InteractionEvent.CLOSE)
					}
				}
			})

			registerInflater("Set", object : Lesson.Inflater() {
				private var mAnswerLabels: IntArray = IntArray(0)

				override fun inflate(course: Course): Lesson {
					return SetLesson(course)
				}

				override fun createView(context: Context): View {
					val view = View.inflate(context, R.layout.view_interaction_choose, null)
					val packageName = context.packageName

					mAnswerLabels = IntArray(C.NUMBER_OF_ANSWERS)
					for (k in 0 until C.NUMBER_OF_ANSWERS)
						mAnswerLabels[k] = context.resources.getIdentifier("a$k", "id", packageName)

					return view
				}

				override fun populateView(context: Context, view: View, lesson: Lesson, problem: Any, listener: InteractionManager.InteractionListener) {
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

							listener.onInteraction(InteractionManager.InteractionEvent.POSITIVE)
						} else {
							InteractionManager.i().vibrateAsFail(context)
							U.animateNegative(view.findViewById(R.id.card))

							listener.onInteraction(InteractionManager.InteractionEvent.NEGATIVE)
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
						BunchLesson.Knowledge.Untouched -> InteractionManager.SetLessonWelcomeStackAdapter(viewMeaning)
						else -> InteractionManager.SetLessonFullStackAdapter(viewVariants, viewMeaning)
					}

					swipeStack.setListener(object : link.fls.swipestack.SwipeStack.SwipeStackListener {
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
							listener.onInteraction(InteractionManager.InteractionEvent.POSITIVE)
						}

						override fun onClick() {}
						override fun onLongClick(duration: Long) {}
					})

					swipeStack.resetStack()
				}
			})

			val phraseInflater = object : Lesson.Inflater() {
				override fun inflate(course: Course): Lesson {
					return PhraseLesson(course)
				}

				override fun createView(context: Context): View {
					return View.inflate(context, R.layout.view_interaction_phrase, null)
				}

				override fun populateView(context: Context, view: View, lesson: Lesson, problem: Any, listener: InteractionManager.InteractionListener) {
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
							listener.onInteraction(InteractionManager.InteractionEvent.POSITIVE)
						} else {
							InteractionManager.i().vibrateAsFail(context)
							U.animateNegative(view.findViewById(R.id.card))
						}
					}
				}
			}

			registerInflater("Phrase", phraseInflater)

			registerInflater("Character", phraseInflater)

			registerInflater("Vocabulary", object : Lesson.Inflater() {
				override fun inflate(course: Course): Lesson {
					return VocabularyLesson(course)
				}

				override fun createView(context: Context): View {
					return View.inflate(context, R.layout.view_interaction_choose, null)
				}

				override fun populateView(context: Context, view: View, lesson: Lesson, problem: Any, listener: InteractionManager.InteractionListener) {
					val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
					val prob = problem as CharacterLesson.Problem

					populateCard(view, prob)

					val swipeStack: SwipeStack = view.findViewById(R.id.swipeStack)
					val viewMeaning = inflater.inflate(R.layout.view_card_explaination, swipeStack, false)
					val textMeaning = viewMeaning.findViewById<TextView>(R.id.meaning)

					textMeaning.text = prob.meaning

					swipeStack.adapter = InteractionManager.SetLessonWelcomeStackAdapter(viewMeaning)
					swipeStack.setListener(object : link.fls.swipestack.SwipeStack.SwipeStackListener {
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

							listener.onInteraction(InteractionManager.InteractionEvent.POSITIVE)
						}

						override fun onClick() {}
						override fun onLongClick(duration: Long) {}
					})

					swipeStack.resetStack()
				}
			})
		}
	}
}