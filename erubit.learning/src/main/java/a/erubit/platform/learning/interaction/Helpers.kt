package a.erubit.platform.learning.interaction

import a.erubit.platform.learning.R
import a.erubit.platform.learning.lesson.CharacterLesson
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.TextView
import t.FlipLayout.FlipLayout


fun animateMoveWord(container: View, view: View) = run {
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
	anim.setAnimationListener(object : Animation.AnimationListener {
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

fun populateCard(view: View, problem: CharacterLesson.Problem) {
	val card: FlipLayout = view.findViewById(R.id.card)
	card.reset()
	card.setOnFlipListener(object : FlipLayout.OnFlipListener {
		override fun onFlipStart(view: FlipLayout) {}
		override fun onFlipEnd(view: FlipLayout) {
			if (view.isFlipped)
				problem.spied()
		}
	})
	(view.findViewById<View>(R.id.problem) as TextView).text = problem.text
	(view.findViewById<View>(R.id.meaning) as TextView).text = problem.meaning
}


