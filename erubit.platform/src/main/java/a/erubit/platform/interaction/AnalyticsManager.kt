package a.erubit.platform.interaction

import android.content.Context
import android.support.v4.app.Fragment
import java.util.*

class AnalyticsManager {
	fun initialize(context: Context) {
		mReporterInstance!!.initialize(context)
	}

	fun reportFragmentChanged(fragment: Fragment?) {
		mReporterInstance ?: return

		if (fragment != null)
			mReporterInstance.report(fragment.javaClass.simpleName, "fragment")
		else
			mReporterInstance.report("navigationFragment", "fragment")
	}

	fun reportInteractionScreen() {
		mReporterInstance ?: return
		mReporterInstance.report("Interaction", "WindowManager")
	}

	private object Container {
		var mInstance: IAnalyticsReporter? = null

		init {
			val instance: IAnalyticsReporter
			val loader: Iterator<IAnalyticsReporter> = ServiceLoader.load(IAnalyticsReporter::class.java).iterator()

			instance = if (loader.hasNext())
				loader.next()
			else
				EmptyAnalyticsReporter()

			mInstance = instance
		}
	}


	private class EmptyAnalyticsReporter : IAnalyticsReporter {
		override fun initialize(context: Context) {}
		override fun report(event: String, category: String) {}
	}


	companion object {
		private val mReporterInstance = Container.mInstance
		private val mInstance = AnalyticsManager()

		fun i(): AnalyticsManager {
			return mInstance
		}
	}
}
