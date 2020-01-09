package a.erubit.platform.android

import a.erubit.platform.R
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.preference.PreferenceFragmentCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

class PreferencesFragment : PreferenceFragmentCompat(), IUxController {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		addPreferencesFromResource(R.xml.preferences)
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		val view = super.onCreateView(inflater, container, savedInstanceState)

		view?.setBackgroundColor(ContextCompat.getColor(context!!, R.color.color_bg_light))

		return view
	}

	override fun onCreatePreferences(savedInstanceState: Bundle, rootKey: String) {}

	override val floatingButtonVisibility: Int
		get() = View.GONE
}
