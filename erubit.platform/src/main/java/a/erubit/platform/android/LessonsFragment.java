package a.erubit.platform.android;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PagerSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SnapHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;

import a.erubit.platform.R;
import a.erubit.platform.course.Course;
import a.erubit.platform.course.CourseManager;
import a.erubit.platform.course.Lesson;
import a.erubit.platform.course.Progress;

public class LessonsFragment extends Fragment {

    private RecyclerView mListView;
    private TextView mTextHeader;
    private TextView mTextExplanation;

    private OnLessonInteractionListener mListener;


    public LessonsFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lessons, container, false);

        mListView = view.findViewById(android.R.id.list);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(mListView.getContext(), LinearLayoutManager.HORIZONTAL, false) {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                super.onLayoutChildren(recycler, state);
                updateProgressExplanation();
            }
        };
        mListView.setItemAnimator(new DefaultItemAnimator());
        mListView.setLayoutManager(layoutManager);

        SnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(mListView);

        mTextHeader = view.findViewById(android.R.id.text1);
        mTextExplanation = view.findViewById(android.R.id.text2);

        return view;
    }

    @Override
    public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final Course course = CourseManager.i().getCourse(getArguments().getString("id"));
        final LessonsListAdapter adapter = new LessonsListAdapter(course);
        mListView.setAdapter(adapter);

        mListView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                updateProgressExplanation();
            }
        });

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof OnLessonInteractionListener)
            mListener = (OnLessonInteractionListener) context;
    }

    private void updateProgressExplanation() {
        LinearLayoutManager layoutManager = ((LinearLayoutManager) mListView.getLayoutManager());
        int cvi = layoutManager.findFirstCompletelyVisibleItemPosition();
        if (cvi < 0)
            return;

        Lesson lesson = ((LessonsListAdapter)mListView.getAdapter()).get(cvi);

        mTextHeader.setText(lesson.name);
        mTextExplanation.setText(lesson.getProgress(getContext()).getExplanation(getContext()));
    }

    class LessonsListAdapter extends RecyclerView.Adapter<LessonsListAdapter.ViewHolder> {
        private final ArrayList<Lesson> mList;

        LessonsListAdapter(Course course) {
            mList = course.getLessons(getContext());
        }

        @Override
        public ViewHolder onCreateViewHolder(final ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            final View container = inflater.inflate(R.layout.item_nav_lesson, parent, false);

            container.findViewById(R.id.button_practice).setOnClickListener(view -> {
                ViewHolder viewHolder = (ViewHolder) container.getTag();
                mListener.onLessonInteraction(viewHolder.mLesson, LessonInteractionAction.PRACTICE);
            });

            return new ViewHolder(container);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            holder.mLesson = mList.get(position);
            Progress progress = holder.mLesson.mProgress;

            holder.textTitle.setText(holder.mLesson.name);
            holder.progressCourse.setSecondaryProgress(progress.familiarity);
            holder.btnPractice.setVisibility(progress.interactionDate > 0 ? View.VISIBLE : View.GONE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                holder.progressCourse.setProgress(progress.progress, true);
            else
                holder.progressCourse.setProgress(progress.progress);
        }

        @Override
        public int getItemCount() {
            return mList.size();
        }

        public Lesson get(int i) {
            return mList.get(i);
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            Lesson mLesson;
            private final TextView textTitle;
            private final ProgressBar progressCourse;
            private final Button btnPractice;

            ViewHolder(View itemView) {
                super(itemView);

                textTitle = itemView.findViewById(android.R.id.text1);
                progressCourse = itemView.findViewById(android.R.id.progress);
                btnPractice = itemView.findViewById(R.id.button_practice);

                itemView.setTag(this);
            }
        }
    }

    enum LessonInteractionAction {
        PRACTICE
    }
    interface OnLessonInteractionListener {
        void onLessonInteraction(Lesson lesson, LessonInteractionAction action);
    }
}
