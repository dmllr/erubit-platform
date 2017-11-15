package a.erubit.platform.android;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import a.erubit.platform.interaction.InteractionManager;
import a.erubit.platform.R;
import a.erubit.platform.course.Course;
import a.erubit.platform.course.CourseManager;
import a.erubit.platform.course.Lesson;

public class CourseTrainingFragment extends TrainingFragment {
    private Course mCourse;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (null == getArguments())
            mCourse = null;
        else
            mCourse = CourseManager.i().getCourse(getArguments().getString("id"));

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public View getNextInteractionView() {
        Lesson lesson;
        if (mCourse != null)
            lesson = CourseManager.i().getNextLesson(getContext(), mCourse);
        else
            lesson = CourseManager.i().getNextLesson(getContext());

        if (lesson == null)
            return null;

        View v = InteractionManager.i().getInteractionView(getContext(), lesson, this);
        v.findViewById(R.id.quickButtonBar).setVisibility(View.GONE);

        return v;
    }
}
