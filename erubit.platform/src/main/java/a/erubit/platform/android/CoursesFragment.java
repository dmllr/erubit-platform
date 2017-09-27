package a.erubit.platform.android;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import java.util.ArrayList;

import a.erubit.platform.R;
import a.erubit.platform.course.Course;
import a.erubit.platform.course.CourseManager;
import t.MorphButton.CheckMorphButton;
import t.SwipeLayout;

public class CoursesFragment extends Fragment {

    private OnCourseInteractionListener mListener;
    private RecyclerView mListView;

    public CoursesFragment() {
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        ViewTreeObserver observer = mListView.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mListView.getAdapter().notifyDataSetChanged();
                mListView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_nav, container, false);

        mListView = view.findViewById(android.R.id.list);
        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL);
        mListView.setItemAnimator(new DefaultItemAnimator());
        mListView.setLayoutManager(layoutManager);
        DividerItemDecoration decorator = new DividerItemDecoration(mListView.getContext(), layoutManager.getOrientation());
        Drawable drawable = ResourcesCompat.getDrawable(getContext().getResources(), R.drawable.course_list_decorator, null);
        if (drawable != null)
            decorator.setDrawable(drawable);
        mListView.addItemDecoration(decorator);

        CoursesListAdapter mAdapter = new CoursesListAdapter(mListener);
        mListView.setAdapter(mAdapter);

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof OnCourseInteractionListener)
            mListener = (OnCourseInteractionListener) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    enum CourseInteractionAction {
        SHOW_LESSONS,
        SHOW_STATS,
        SHOW_INFO,
        PRACTICE
    }
    interface OnCourseInteractionListener {
        void onCourseInteraction(Course course, CourseInteractionAction action);
    }

    static class CoursesListAdapter extends RecyclerView.Adapter<CoursesListAdapter.ViewHolder> {
        private final ArrayList<Course> mList;
        private final OnCourseInteractionListener mListener;

        CoursesListAdapter(OnCourseInteractionListener listener) {
            this.mList = CourseManager.i().getCourses();
            this.mListener = listener;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            final View view = inflater.inflate(R.layout.item_nav_course, parent, false);

            CheckMorphButton button = view.findViewById(R.id.checkButton);
            button.setOnClickListener(v -> {
                ViewHolder holder = (ViewHolder)view.getTag();
                CheckMorphButton b = ((CheckMorphButton) v);
                if (b.state == CheckMorphButton.State.AVAILABLE) {
                    b.morphToActivated(CheckMorphButton.DURATION);
                    CourseManager.i().setActive(holder.mCourse);
                } else {
                    b.morphToAvailable(CheckMorphButton.DURATION);
                    CourseManager.i().setInactive(holder.mCourse);
                }
            });

            view.findViewById(R.id.courseFace).setOnClickListener(v -> {
                if (mListener != null) {
                    ViewHolder holder = (ViewHolder)view.getTag();
                    mListener.onCourseInteraction(holder.mCourse, CourseInteractionAction.SHOW_LESSONS);
                }
            });
            view.findViewById(R.id.train_btn).setOnClickListener(v -> {
                if (mListener != null) {
                    ViewHolder holder = (ViewHolder)view.getTag();
                    mListener.onCourseInteraction(holder.mCourse, CourseInteractionAction.PRACTICE);
                }
            });
            view.findViewById(R.id.stats_btn).setOnClickListener(v -> {
                if (mListener != null) {
                    ViewHolder holder = (ViewHolder)view.getTag();
                    mListener.onCourseInteraction(holder.mCourse, CourseInteractionAction.SHOW_STATS);
                }
            });
            view.findViewById(R.id.infoButton).setOnClickListener(v -> {
                if (mListener != null) {
                    ViewHolder holder = (ViewHolder)view.getTag();
                    mListener.onCourseInteraction(holder.mCourse, CourseInteractionAction.SHOW_INFO);
                }
            });

            SwipeLayout swipeLayout =  (SwipeLayout)view;
            swipeLayout.setShowMode(SwipeLayout.ShowMode.LayDown);
            swipeLayout.addDrag(SwipeLayout.DragEdge.Left, view.findViewById(R.id.bottom_wrapper));

            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            holder.mCourse = mList.get(position);

            holder.textTitle.setText(holder.mCourse.name);
            holder.itemView.post(() -> holder.checkButton.initialize(CourseManager.i().isActive(holder.mCourse)));
        }

        @Override
        public int getItemCount() {
            return mList.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            Course mCourse;
            private final CheckMorphButton checkButton;
            private final TextView textTitle;

            ViewHolder(View itemView) {
                super(itemView);

                textTitle = itemView.findViewById(android.R.id.text1);
                checkButton = itemView.findViewById(R.id.checkButton);

                itemView.setTag(this);
            }
        }
    }
}
