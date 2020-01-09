package a.erubit.platform.android

import a.erubit.platform.R
import a.erubit.platform.android.CoursesFragment.CourseInteractionAction
import a.erubit.platform.android.CoursesFragment.OnCourseInteractionListener
import a.erubit.platform.android.LessonsFragment.LessonInteractionAction
import a.erubit.platform.android.LessonsFragment.OnLessonInteractionListener
import a.erubit.platform.android.TrainingFragment.OnTrainingInteractionListener
import a.erubit.platform.android.TrainingFragment.TrainingInteractionAction
import a.erubit.platform.course.Course
import a.erubit.platform.course.CourseManager
import a.erubit.platform.course.Lesson
import a.erubit.platform.interaction.AnalyticsManager
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.annotation.RequiresApi
import com.google.android.material.navigation.NavigationView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import android.view.MenuItem
import android.view.View
import android.widget.CompoundButton
import t.TinyDB
import u.C
import u.U


class NavActivity :
	AppCompatActivity(),
	OnCourseInteractionListener,
	OnLessonInteractionListener,
	OnTrainingInteractionListener,
	NavigationView.OnNavigationItemSelectedListener,
	OnSharedPreferenceChangeListener
{
	private var mViewPermissionsWarning: View? = null
	private var mFab: View? = null

	private fun setContentView() {
		setContentView(R.layout.activity_navigation)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		setContentView()

		if (savedInstanceState == null) {
			val fm = supportFragmentManager
			fm.beginTransaction()
					.replace(R.id.fragment, CoursesFragment())
					.commit()
		}

		val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
		setSupportActionBar(toolbar)
		val drawer = findViewById<View>(R.id.drawer_layout) as DrawerLayout
		val toggle = ActionBarDrawerToggle(
				this, drawer, toolbar,
				R.string.navigation_drawer_open, R.string.navigation_drawer_close)
		drawer.addDrawerListener(toggle)
		toggle.syncState()

		PreferenceManager.setDefaultValues(this, R.xml.preferences, false)
		supportFragmentManager.addOnBackStackChangedListener {
			if (supportFragmentManager.backStackEntryCount > 0) { // show back button
				supportActionBar!!.setDisplayHomeAsUpEnabled(true)
				toolbar.setNavigationOnClickListener { v: View? -> onBackPressed() }
			} else { //show hamburger
				supportActionBar!!.setDisplayHomeAsUpEnabled(false)
				toggle.syncState()
				toolbar.setNavigationOnClickListener { v: View? -> drawer.openDrawer(GravityCompat.START) }
			}
		}

		val navigationView = findViewById<View>(R.id.nav_view) as NavigationView
		navigationView.setNavigationItemSelectedListener(this)

		mFab = findViewById(R.id.fab)
		mFab!!.setOnClickListener { trainingButtonTapped() }

		mViewPermissionsWarning = findViewById(R.id.permissionsWarning)
		mViewPermissionsWarning!!.setOnClickListener { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) askForPermission() }

		val switchUnlock = findViewById<View>(R.id.switchEnableOnUnlock) as SwitchCompat
		switchUnlock.isChecked = TinyDB(applicationContext).getBoolean(C.SP_ENABLED_ON_UNLOCK, true)
		switchUnlock.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean -> TinyDB(applicationContext).putBoolean(C.SP_ENABLED_ON_UNLOCK, isChecked) }
	}

	override fun onStart() {
		super.onStart()

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
			checkDrawOverlayPermission()
		else
			permissionGranted()
	}

	override fun onResume() {
		super.onResume()

		TinyDB(this).registerOnSharedPreferenceChangeListener(this)
	}

	override fun onPause() {
		super.onPause()

		TinyDB(this).registerOnSharedPreferenceChangeListener(this)
	}

	@RequiresApi(api = Build.VERSION_CODES.M)
	private fun askForPermission() {
		TinyDB(this).putBoolean(C.SP_PERMISSION_REQUESTED, true)

		val dialogClickListener = DialogInterface.OnClickListener { _: DialogInterface?, which: Int ->
			when (which) {
				DialogInterface.BUTTON_POSITIVE -> {
					val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
					startActivityForResult(intent, ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE)
				}
			}
		}

		val builder = AlertDialog.Builder(this)
		builder.setMessage(R.string.grant_permission_dialog)
				.setPositiveButton(android.R.string.yes, dialogClickListener)
				.setNegativeButton(android.R.string.no, null)
				.show()
	}

	private fun permissionGranted() {
		mViewPermissionsWarning!!.visibility = View.GONE
	}

	@RequiresApi(api = Build.VERSION_CODES.M)
	private fun checkDrawOverlayPermission() {
		if (Settings.canDrawOverlays(this))
			permissionGranted()
		else {
			val permissionRequested = TinyDB(this).getBoolean(C.SP_PERMISSION_REQUESTED, false)

			if (!permissionRequested)
				askForPermission()
		}
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		if (requestCode == ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE) { // check once again if we have permission
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) { // continue here - permission was granted
				permissionGranted()
			}
		}

		super.onActivityResult(requestCode, resultCode, data)
	}

	override fun onNavigationItemSelected(item: MenuItem): Boolean { // Handle navigation view item clicks here.
		val id = item.itemId

		if (id == R.id.nav_share) {
			val message = CourseManager.i().getSharingText(this)
			val share = Intent(Intent.ACTION_SEND)
			share.type = "text/plain"
			share.putExtra(Intent.EXTRA_TEXT, message)
			startActivity(Intent.createChooser(share, getString(R.string.share_my_progress)))
		}

		if (id == R.id.nav_settings) {
			putFragment(PreferencesFragment())
		}

		val drawer = findViewById<View>(R.id.drawer_layout) as DrawerLayout
		drawer.closeDrawer(GravityCompat.START)

		return true
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		val id = item.itemId

		if (id == android.R.id.home) {
			popFragment()

			return true
		}

		return super.onOptionsItemSelected(item)
	}

	private fun trainingButtonTapped() {
		val fm = supportFragmentManager
		val topFragment = fm.findFragmentById(R.id.fragment)
		val trainingFragment: Fragment = CourseTrainingFragment()

		if (topFragment is LessonsFragment || topFragment is ProgressFragment)
			trainingFragment.arguments = topFragment.arguments

		putFragment(trainingFragment)
	}

	override fun onBackPressed() {
		val drawer = findViewById<View>(R.id.drawer_layout) as DrawerLayout
		if (drawer.isDrawerOpen(GravityCompat.START))
			drawer.closeDrawer(GravityCompat.START)
		else
			if (!popFragment()) super.onBackPressed()
	}

	override fun onCourseInteraction(course: Course, action: CourseInteractionAction) {
		val bundle = Bundle()
		bundle.putString("id", course.id)

		val fragment: Fragment

		when (action) {
			CourseInteractionAction.SHOW_LESSONS -> {
				fragment = LessonsFragment()
				fragment.setArguments(bundle)
				putFragment(fragment)
			}
			CourseInteractionAction.SHOW_STATS -> {
				fragment = ProgressFragment()
				fragment.setArguments(bundle)
				putFragment(fragment)
			}
			CourseInteractionAction.SHOW_INFO -> showCourseInfo(course)
			CourseInteractionAction.PRACTICE -> {
				val lesson = CourseManager.i().getNextLesson(this, course)
				fragment = if (lesson == null) LessonsFragment() else CourseTrainingFragment()
				fragment.arguments = bundle
				putFragment(fragment)
			}
		}
	}

	override fun onLessonInteraction(lesson: Lesson?, action: LessonInteractionAction?) {
		val bundle = Bundle()
		bundle.putString("id", lesson!!.course.id)
		bundle.putString("lesson_id", lesson.id)

		val fragment: Fragment

		when (action) {
			LessonInteractionAction.PRACTICE -> {
				fragment = LessonTrainingFragment()
				fragment.setArguments(bundle)
				putFragment(fragment)
			}
		}
	}

	override fun onTrainingInteraction(action: TrainingInteractionAction) {
		when (action) {
			TrainingInteractionAction.FINISHED -> popFragment()
		}
	}

	private fun showCourseInfo(course: Course) {
		val builder = AlertDialog.Builder(this)
		val spanned = U.getSpanned(course.description ?: "")
		builder.setMessage(spanned)
				.setPositiveButton(android.R.string.ok, null)
				.show()
	}

	private fun putFragment(fragment: Fragment) {
		val fm = supportFragmentManager
		fm.beginTransaction()
				.add(R.id.fragment, fragment)
				.addToBackStack(C.BACKSTACK)
				.commit()

		triggerUIUpdates(fm, fragment, +1)

		AnalyticsManager.i().reportFragmentChanged(fragment)
	}

	private fun popFragment(): Boolean {
		val fm = supportFragmentManager
		val c = fm.backStackEntryCount
		if (c == 0)
			return false

		fm.popBackStack()

		if (null != fm.primaryNavigationFragment)
			triggerUIUpdates(fm, fm.primaryNavigationFragment!!, -1)

		AnalyticsManager.i().reportFragmentChanged(fm.primaryNavigationFragment)

		return true
	}

	private fun triggerUIUpdates(fm: FragmentManager, fragment: Fragment, direction: Int) {
		val c = fm.backStackEntryCount + direction

		val actionBar = supportActionBar!!
		actionBar.setDisplayHomeAsUpEnabled(c > 0)
		actionBar.setDisplayShowHomeEnabled(c > 0)

		findViewById<View>(R.id.main_backdrop).visibility = if (c == 0) View.VISIBLE else View.GONE
		findViewById<View>(R.id.onScreenSettings).visibility = if (c == 0) View.VISIBLE else View.GONE

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this))
			mViewPermissionsWarning!!.visibility = if (c == 0) View.VISIBLE else View.GONE

		val fab = mFab ?: return
		if (fragment is IUxController)
			fab.visibility = (fragment as IUxController).floatingButtonVisibility
		else
			fab.visibility = View.VISIBLE
	}

	private fun updatePreferences() {
		val switchUnlock : SwitchCompat = findViewById(R.id.switchEnableOnUnlock) ?: return

		switchUnlock.isChecked = TinyDB(applicationContext).getBoolean(C.SP_ENABLED_ON_UNLOCK, true)
	}

	override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
		if (C.SP_ENABLED_ON_UNLOCK == key)
			updatePreferences()
	}

	companion object {
		private const val ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE = 5432
	}
}
