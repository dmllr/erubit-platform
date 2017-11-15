package a.erubit.platform.course;

import android.content.Context;

import java.util.ArrayList;

public abstract class Lesson {
    public String id;
    public String name;

    public final Course course;

    public Progress mProgress;

    public enum Status {
        OK,
        LESSON_LEARNED,
        ERROR
    }

    Lesson(Course course) {
        this.course = course;
    }

    public abstract boolean hasInteraction();

    public abstract PresentableDescriptor getNextPresentable(Context context);

    public abstract ArrayList<PresentableDescriptor> getPresentables(Context context);

    public abstract Progress updateProgress();

    public abstract Progress getProgress(Context context);


    public static class PresentableDescriptor {
        public static final PresentableDescriptor ERROR = new PresentableDescriptor(Status.ERROR);
        static final PresentableDescriptor COURSE_LEARNED = new PresentableDescriptor(Status.LESSON_LEARNED);

        public final Status mStatus;
        public final Object mPresentable;

        PresentableDescriptor(Status status) {
            this.mStatus = status;
            mPresentable = null;
        }

        PresentableDescriptor(Problem problem) {
            this.mStatus = Status.OK;
            this.mPresentable = problem;
        }

        PresentableDescriptor(String text) {
            this.mStatus = Status.OK;
            this.mPresentable = text;
        }
    }


    public abstract static class Problem {
        public String text;
        public String meaning;

        public final Lesson lesson;

        boolean mSucceed = true;

        public Problem(Lesson lesson) {
            this.lesson = lesson;
        }

        public void attempt(boolean solved) {
            mSucceed = mSucceed && solved;
        }
        public abstract void spied();
        public abstract void treatResult();
    }

}
