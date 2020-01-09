package a.erubit.platform.interaction

import android.content.Context


interface IAnalyticsReporter {
	fun initialize(context: Context)
	fun report(event: String, category: String)
}
