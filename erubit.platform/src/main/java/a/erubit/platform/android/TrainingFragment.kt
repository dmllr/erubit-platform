package a.erubit.platform.android

import a.erubit.platform.R
import a.erubit.platform.android.LessonsFragment.OnLessonInteractionListener
import a.erubit.platform.interaction.InteractionManager
import a.erubit.platform.interaction.InteractionManager.InteractionEvent
import a.erubit.platform.interaction.InteractionManager.InteractionListener

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ViewAnimator


abstract class TrainingFragment : Fragment(), InteractionListener, IUxController {
	private var mViewAnimator: ViewAnimator? = null
	private var mListener: OnTrainingInteractionListener? = null

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		val view = inflater.inflate(R.layout.fragment_training, container, false)

		mViewAnimator = view.findViewById(R.id.viewAnimator)

		return view
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		switchView()
	}

	override fun onConfigurationChanged(newConfig: Configuration) {
		super.onConfigurationChanged(newConfig)

		InteractionManager.i().onConfigurationChanged(context!!)
		updateView(context)
	}

	private fun updateView(context: Context?) {
		val v = InteractionManager.i().getLastInteractionView(context!!, this)

		v!!.findViewById<View>(R.id.quickButtonBar).visibility = View.GONE

		val animator = mViewAnimator ?: return
		animator.removeAllViews()
		animator.addView(v)
	}

	override fun onInteraction(event: InteractionEvent) {
		if (event !== InteractionEvent.NEGATIVE)
			switchView()
	}

	override fun onAttach(context: Context) {
		super.onAttach(context)

		if (context is OnLessonInteractionListener)
			mListener = context as OnTrainingInteractionListener
	}

	private fun switchView() {
		val view = nextInteractionView
		if (view == null) {
			mListener!!.onTrainingInteraction(TrainingInteractionAction.FINISHED)
			return
		}

		val parent = view.parent as ViewGroup?
		parent?.removeView(view)

		val animator = mViewAnimator ?: return
		animator.removeAllViews()
		animator.addView(view)
	}

	protected abstract val nextInteractionView: View?

	override val floatingButtonVisibility: Int
		get() = View.GONE


	enum class TrainingInteractionAction {
		FINISHED
	}


	internal interface OnTrainingInteractionListener {
		fun onTrainingInteraction(action: TrainingInteractionAction)
	}

}