package a.erubit.platform.android;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import a.erubit.platform.interaction.InteractionManager;
import a.erubit.platform.R;
import a.erubit.platform.course.Course;
import a.erubit.platform.course.CourseManager;
import a.erubit.platform.course.Lesson;

public class LessonTrainingFragment extends TrainingFragment {
    private Lesson mLesson;
    private int mPresentableIndex;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Course course = CourseManager.i().getCourse(getArguments().getString("id"));
        mLesson = course.getLesson(getArguments().getString("lesson_id"));
        mPresentableIndex = -1;

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    protected View getNextInteractionView() {
        mPresentableIndex++;
        ArrayList<Lesson.PresentableDescriptor> presentables = mLesson.getPresentables();

        if (mPresentableIndex == presentables.size())
            return null;

        Lesson.PresentableDescriptor pd = presentables.get(mPresentableIndex);
        View v = InteractionManager.i().populate(mLesson, pd, this);
        v.findViewById(R.id.quickButtonBar).setVisibility(View.GONE);

        return v;
    }
}
