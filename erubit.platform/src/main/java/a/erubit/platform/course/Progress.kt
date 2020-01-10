package a.erubit.platform.course

import a.erubit.platform.R
import android.content.Context


abstract class Progress {
	var nextInteractionDate: Long = 0
	var interactionDate: Long = 0
	var trainDate: Long = 0
	var familiarity = 0
	var progress = 0

	open fun getExplanation(context: Context): String {
		val r = context.resources
		return when (interactionDate) {
			0L -> r.getString(R.string.unopened)
			else -> r.getString(
				R.string.progress_explanation,
				progress, familiarity)
		}
	}
}
