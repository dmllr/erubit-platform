package a.erubit.platform.android

import a.erubit.platform.R
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceFragmentCompat

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

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {}

	override val floatingButtonVisibility: Int
		get() = View.GONE
}
