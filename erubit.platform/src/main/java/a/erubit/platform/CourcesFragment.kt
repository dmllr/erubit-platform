package a.erubit.platform

import a.erubit.platform.course.Course
import a.erubit.platform.course.CourseManager

import t.MorphButton.CheckMorphButton
import t.SwipeLayout

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager

import java.util.*


class CoursesFragment : Fragment() {
	private var mListener : OnCourseInteractionListener? = null
	private var mListView : RecyclerView? = null

	override fun onConfigurationChanged(newConfig:Configuration) {
		super.onConfigurationChanged(newConfig)

		val lv = mListView ?: return

		val observer = lv.viewTreeObserver
		observer.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
			override fun onGlobalLayout() {
				lv.adapter?.notifyDataSetChanged()
				lv.viewTreeObserver.removeOnGlobalLayoutListener(this)
			}
		})
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		val context = this.context ?: return null
		val view = inflater.inflate(R.layout.fragment_nav, container, false)

		val listView : RecyclerView = view.findViewById(android.R.id.list)
		mListView = listView

		val layoutManager = StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL)
		listView.itemAnimator = DefaultItemAnimator()
		listView.layoutManager = layoutManager

		val decorator = DividerItemDecoration(listView.context, layoutManager.orientation)
		val drawable = ResourcesCompat.getDrawable(context.resources, R.drawable.course_list_decorator, null)
		if (drawable != null)
			decorator.setDrawable(drawable)

		listView.addItemDecoration(decorator)

		val mAdapter = CoursesListAdapter(this, mListener)
		listView.adapter = mAdapter

		return view
	}

	override fun onAttach(context:Context) {
		super.onAttach(context)

		if (context is OnCourseInteractionListener)
			mListener = context
	}

	override fun onDetach() {
		super.onDetach()

		mListener = null
	}


	enum class CourseInteractionAction {
		SHOW_LESSONS,
		SHOW_STATS,
		SHOW_INFO,
		PRACTICE
	}


	internal interface OnCourseInteractionListener {
		fun onCourseInteraction(course:Course, action: CourseInteractionAction)
	}


	internal class CoursesListAdapter(private val coursesFragment: CoursesFragment, listener: OnCourseInteractionListener?) : RecyclerView.Adapter<CoursesListAdapter.ViewHolder>() {
		private val mList: ArrayList<Course> = CourseManager.i().courses

		private val mListener: OnCourseInteractionListener? = listener

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
			val inflater = LayoutInflater.from(parent.context)

			val view = inflater.inflate(R.layout.item_nav_course, parent, false)

			val button: CheckMorphButton = view.findViewById(R.id.checkButton)
			button.setOnClickListener { v: View ->
				val holder = view.tag as ViewHolder
				val b = v as CheckMorphButton

				if (b.state == CheckMorphButton.State.AVAILABLE) {
					b.morphToActivated(CheckMorphButton.DURATION)
					CourseManager.i().setActive(coursesFragment.context!!, holder.mCourse!!)
				} else {
					b.morphToAvailable(CheckMorphButton.DURATION)
					CourseManager.i().setInactive(coursesFragment.context!!, holder.mCourse)
				}
			}

			view.findViewById<View>(R.id.courseFace).setOnClickListener {
				if (mListener != null) {
					val holder = view.tag as ViewHolder
					if (null != holder.mCourse)
						mListener.onCourseInteraction(holder.mCourse!!, CourseInteractionAction.SHOW_LESSONS)
				}
			}

			view.findViewById<View>(R.id.train_btn).setOnClickListener {
				if (mListener != null) {
					val holder = view.tag as ViewHolder
					if (null != holder.mCourse)
						mListener.onCourseInteraction(holder.mCourse!!, CourseInteractionAction.PRACTICE)
				}
			}

			view.findViewById<View>(R.id.info_btn).setOnClickListener {
				if (mListener != null) {
					val holder = view.tag as ViewHolder
					if (null != holder.mCourse)
						mListener.onCourseInteraction(holder.mCourse!!, CourseInteractionAction.SHOW_INFO)
				}
			}

			view.findViewById<View>(R.id.stats_btn).setOnClickListener {
				if (mListener != null) {
					val holder = view.tag as ViewHolder
					if (null != holder.mCourse)
						mListener.onCourseInteraction(holder.mCourse!!, CourseInteractionAction.SHOW_STATS)
				}
			}

			val swipeLayout = view as SwipeLayout
			swipeLayout.showMode = SwipeLayout.ShowMode.LayDown
			swipeLayout.addDrag(SwipeLayout.DragEdge.Left, view.findViewById<View>(R.id.bottom_wrapper))

			return ViewHolder(view)
		}

		override fun onBindViewHolder(holder: ViewHolder, position: Int) {
			val course = mList[position]
			holder.mCourse = course
			holder.textTitle.text = course.name
			holder.itemView.post {
				holder.checkButton.initialize(CourseManager.i().isActive(course))
			}
		}

		override fun getItemCount(): Int {
			return mList.size
		}

		internal inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
			var mCourse: Course? = null
			val checkButton: CheckMorphButton = itemView.findViewById(R.id.checkButton)
			val textTitle: TextView = itemView.findViewById(android.R.id.text1)

			init {
				itemView.tag = this
			}
		}

	}

}
