package a.erubit.platform.android


import a.erubit.platform.R
import a.erubit.platform.course.Course
import a.erubit.platform.course.CourseManager
import a.erubit.platform.course.Lesson

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.*


class LessonsFragment : Fragment() {
	private var mListView: RecyclerView? = null
	private var mTextHeader: TextView? = null
	private var mTextExplanation: TextView? = null
	private var mListener: OnLessonInteractionListener? = null


	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		val view = inflater.inflate(R.layout.fragment_lessons, container, false)
		val listView : RecyclerView = view.findViewById(android.R.id.list)

		val layoutManager: LinearLayoutManager = object : LinearLayoutManager(listView.context, HORIZONTAL, false) {
			override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
				super.onLayoutChildren(recycler, state)
				updateProgressExplanation()
			}
		}

		listView.itemAnimator = DefaultItemAnimator()
		listView.layoutManager = layoutManager

		val snapHelper: SnapHelper = PagerSnapHelper()
		snapHelper.attachToRecyclerView(listView)

		mListView = listView
		mTextHeader = view.findViewById(android.R.id.text1)
		mTextExplanation = view.findViewById(android.R.id.text2)

		return view
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		val id = arguments!!.getString("id") ?: return
		val course = CourseManager.i().getCourse(id) ?: return
		val listView = mListView ?: return
		val adapter = LessonsListAdapter(course)

		listView.adapter = adapter

		listView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
			override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
				super.onScrollStateChanged(recyclerView, newState)
				updateProgressExplanation()
			}
		})
	}

	override fun onAttach(context: Context) {
		super.onAttach(context)

		if (context is OnLessonInteractionListener)
			mListener = context
	}

	private fun updateProgressExplanation() {
		val listView = mListView ?: return
		val context = this.context ?: return

		val layoutManager = listView.layoutManager as LinearLayoutManager
		val cvi = layoutManager.findFirstCompletelyVisibleItemPosition()
		if (cvi < 0)
			return

		val lesson = (listView.adapter as LessonsListAdapter)[cvi]

		mTextHeader!!.text = lesson.name
		mTextExplanation!!.text = lesson.getProgress(context).getExplanation(context)
	}


	internal inner class LessonsListAdapter(course: Course) : RecyclerView.Adapter<LessonsListAdapter.ViewHolder>() {
		private val mList = course.lessons!!

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
			val inflater = LayoutInflater.from(parent.context)
			val container = inflater.inflate(R.layout.item_nav_lesson, parent, false)
			val listener = mListener ?: return ViewHolder(container)

			container.findViewById<View>(R.id.button_practice).setOnClickListener {
				val viewHolder = container.tag as ViewHolder
				listener.onLessonInteraction(viewHolder.mLesson, LessonInteractionAction.PRACTICE)
			}

			return ViewHolder(container)
		}

		override fun onBindViewHolder(holder: ViewHolder, position: Int) {
			holder.mLesson = mList[position]
			val progress = holder.mLesson!!.mProgress
			holder.textTitle.text = holder.mLesson!!.name
			holder.progressCourse.secondaryProgress = progress!!.familiarity
			holder.btnPractice.visibility = if (progress.interactionDate > 0) View.VISIBLE else View.GONE
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) holder.progressCourse.setProgress(progress.progress, true) else holder.progressCourse.progress = progress.progress
		}

		override fun getItemCount(): Int {
			return mList.size
		}

		operator fun get(i: Int): Lesson {
			return mList[i]
		}

		internal inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
			var mLesson: Lesson? = null
			val textTitle: TextView = itemView.findViewById(android.R.id.text1)
			val progressCourse: ProgressBar = itemView.findViewById(android.R.id.progress)
			val btnPractice: Button = itemView.findViewById(R.id.button_practice)

			init {
				itemView.tag = this
			}
		}

	}

	enum class LessonInteractionAction {
		PRACTICE
	}

	internal interface OnLessonInteractionListener {
		fun onLessonInteraction(lesson: Lesson?, action: LessonInteractionAction?)
	}
}